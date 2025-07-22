/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import java.time.*

import scala.concurrent.duration.FiniteDuration

import cats.*
import cats.data.NonEmptyList
import cats.free.Free
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.{ CancelScope, Poll, Sync }

import ldbc.sql.*
import ldbc.logging.LogEvent

import ldbc.dsl.codec.*
import ldbc.dsl.exception.*
import ldbc.dsl.util.FactoryCompat

import ldbc.*
import ldbc.free.*

object DBIO:

  private def paramBind(params: List[Parameter.Dynamic]): PreparedStatementIO[Unit] =
    val encoded = params.foldLeft(PreparedStatementIO.pure(List.empty[Encoder.Supported])) {
      case (acc, param) =>
        for
          acc$  <- acc
          value <- param match
                     case Parameter.Dynamic.Success(value)  => PreparedStatementIO.pure(value)
                     case Parameter.Dynamic.Failure(errors) =>
                       PreparedStatementIO.raiseError(new IllegalArgumentException(errors.mkString(", ")))
        yield acc$ :+ value
    }

    for
      encodes <- encoded
      _       <- encodes.zipWithIndex.foldLeft(PreparedStatementIO.pure[Unit](())) {
             case (acc, (value, index)) =>
               acc *> (value match
                 case value: Boolean       => PreparedStatementIO.setBoolean(index + 1, value)
                 case value: Byte          => PreparedStatementIO.setByte(index + 1, value)
                 case value: Short         => PreparedStatementIO.setShort(index + 1, value)
                 case value: Int           => PreparedStatementIO.setInt(index + 1, value)
                 case value: Long          => PreparedStatementIO.setLong(index + 1, value)
                 case value: Float         => PreparedStatementIO.setFloat(index + 1, value)
                 case value: Double        => PreparedStatementIO.setDouble(index + 1, value)
                 case value: BigDecimal    => PreparedStatementIO.setBigDecimal(index + 1, value)
                 case value: String        => PreparedStatementIO.setString(index + 1, value)
                 case value: Array[Byte]   => PreparedStatementIO.setBytes(index + 1, value)
                 case value: LocalDate     => PreparedStatementIO.setDate(index + 1, value)
                 case value: LocalTime     => PreparedStatementIO.setTime(index + 1, value)
                 case value: LocalDateTime => PreparedStatementIO.setTimestamp(index + 1, value)
                 case None                 => PreparedStatementIO.setNull(index + 1, ldbc.sql.Types.NULL))
           }
    yield ()

  private def unique[T](
    statement: String,
    decoder:   Decoder[T]
  ): ResultSetIO[T] =
    ResultSetIO.next().flatMap {
      case true  => decoder.decode(1, statement)
      case false => ResultSetIO.raiseError(new UnexpectedContinuation("Expected ResultSet to have at least one row."))
    }

  private def whileM[G[_], T](
    statement:     String,
    decoder:       Decoder[T],
    factoryCompat: FactoryCompat[T, G[T]]
  ): ResultSetIO[G[T]] =
    val builder = factoryCompat.newBuilder

    def loop(acc: collection.mutable.Builder[T, G[T]]): ResultSetIO[collection.mutable.Builder[T, G[T]]] =
      ResultSetIO.next().flatMap {
        case true  => decoder.decode(1, statement).flatMap(v => loop(acc += v))
        case false => ResultSetIO.pure(acc)
      }

    loop(builder).map(_.result())

  private def nel[A](
    statement: String,
    decoder:   Decoder[A]
  ): ResultSetIO[NonEmptyList[A]] =
    whileM[List, A](statement, decoder, summon[FactoryCompat[A, List[A]]]).flatMap { results =>
      if results.isEmpty then ResultSetIO.raiseError(new UnexpectedEnd("No results found"))
      else ResultSetIO.pure(NonEmptyList.fromListUnsafe(results))
    }

  def queryA[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[A] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, unique(statement, decoder))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  def queryTo[G[_], A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A],
    factory:   FactoryCompat[A, G[A]]
  ): DBIO[G[A]] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, whileM(statement, decoder, factory))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  def queryOption[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[Option[A]] =
    val decoded: ResultSetIO[Option[A]] =
      for
        data <- ResultSetIO.next().flatMap {
                  case true  => decoder.decode(1, statement).map(Option(_))
                  case false => ResultSetIO.pure(None)
                }
        next   <- ResultSetIO.next()
        result <- if next then {
                    ResultSetIO.raiseError(
                      new UnexpectedContinuation(
                        "Expected ResultSet exhaustion, but more rows were available."
                      )
                    )
                  } else ResultSetIO.pure(data)
      yield result
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, decoded)
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  def queryNel[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[NonEmptyList[A]] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, nel(statement, decoder))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  def update(
    statement: String,
    params:    List[Parameter.Dynamic]
  ): DBIO[Int] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      result           <- ConnectionIO.embed(
                  prepareStatement,
                  paramBind(params) *> PreparedStatementIO.executeUpdate()
                )
      _ <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  def returning[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[A] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeUpdate() *> PreparedStatementIO
                       .getGeneratedKeys()
                   )
      result <- ConnectionIO.embed(resultSet, unique(statement, decoder))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  def sequence(
    statements: List[String]
  ): DBIO[Array[Int]] =
    (for
      statement <- ConnectionIO.createStatement()
      _         <- ConnectionIO.embed(statement, statements.map(statement => StatementIO.addBatch(statement)).sequence)
      result    <- ConnectionIO.embed(statement, StatementIO.executeBatch())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statements.mkString("\n"), List.empty, ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statements.mkString("\n"), List.empty))

  def stream[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A],
    fetchSize: Int
  ): fs2.Stream[DBIO, A] =
    (for
      preparedStatement              <- fs2.Stream.eval(ConnectionIO.prepareStatement(statement))
      (preparedStatement, resultSet) <- fs2.Stream.bracket {
                                          ConnectionIO.embed(
                                            preparedStatement,
                                            for
                                              _         <- PreparedStatementIO.setFetchSize(fetchSize)
                                              _         <- paramBind(params)
                                              resultSet <- PreparedStatementIO.executeQuery()
                                            yield (preparedStatement, resultSet)
                                          )
                                        }((preparedStatement, _) =>
                                          ConnectionIO.embed(preparedStatement, PreparedStatementIO.close())
                                        )
      result <- fs2.Stream.unfoldEval(resultSet) { rs =>
                  ConnectionIO.embed(
                    rs,
                    ResultSetIO.next().flatMap {
                      case true  => decoder.decode(1, statement).map(name => Some((name, rs)))
                      case false => ResultSetIO.pure(None)
                    }
                  )
                }
    yield result).onError { ex =>
      fs2.Stream.eval(ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex)))
    } <*
      fs2.Stream.eval(ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value))))

  def sequence[A](dbios: DBIO[A]*): DBIO[List[A]] =
    dbios.toList.sequence

  def pure[A](value:   A):         DBIO[A] = Free.pure(value)
  def raiseError[A](e: Throwable): DBIO[A] = ConnectionIO.raiseError(e)

  implicit val syncDBIO: Sync[DBIO] =
    new Sync[DBIO]:
      val monad = Free.catsFreeMonadForFree[ConnectionOp]
      override val applicative:                                            Applicative[DBIO] = monad
      override val rootCancelScope:                                        CancelScope       = CancelScope.Cancelable
      override def pure[A](x:        A):                                   DBIO[A]           = monad.pure(x)
      override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]):            DBIO[B]           = monad.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f:       A => DBIO[Either[A, B]]): DBIO[B]           = monad.tailRecM(a)(f)
      override def raiseError[A](e: Throwable):                              DBIO[A] = ConnectionIO.raiseError(e)
      override def handleErrorWith[A](fa: DBIO[A])(f: Throwable => DBIO[A]): DBIO[A] =
        ConnectionIO.handleErrorWith(fa)(f)
      override def monotonic:                                   DBIO[FiniteDuration] = ConnectionIO.monotonic
      override def realTime:                                    DBIO[FiniteDuration] = ConnectionIO.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A):    DBIO[A]              = ConnectionIO.suspend(hint)(thunk)
      override def forceR[A, B](fa: DBIO[A])(fb:      DBIO[B]): DBIO[B]              = ConnectionIO.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[DBIO] => DBIO[A]):    DBIO[A]    = ConnectionIO.uncancelable(body)
      override def canceled:                                        DBIO[Unit] = ConnectionIO.canceled
      override def onCancel[A](fa:       DBIO[A], fin: DBIO[Unit]): DBIO[A]    = ConnectionIO.onCancel(fa, fin)

  private[ldbc] class Ops[A](dbio: DBIO[A]):

    def run[F[_]](connector: Connector[F]): F[A] = connector.run(dbio)

    def readOnly[F[_]](connector: Connector[F]): F[A] =
      connector.run(ConnectionIO.setReadOnly(true) *> dbio <* ConnectionIO.setReadOnly(false))

    def commit[F[_]](connector: Connector[F]): F[A] =
      connector.run(ConnectionIO.setReadOnly(false) *> ConnectionIO.setAutoCommit(true) *> dbio)

    def rollback[F[_]](connector: Connector[F]): F[A] =
      connector.run(
        ConnectionIO.setReadOnly(false) *>
          ConnectionIO.setAutoCommit(false) *>
          dbio <*
          ConnectionIO.rollback() <*
          ConnectionIO.setAutoCommit(true)
      )

    def transaction[F[_]](connector: Connector[F]): F[A] =
      connector.run(
        (ConnectionIO.setReadOnly(false) *> ConnectionIO.setAutoCommit(false) *> dbio)
          .onError { _ =>
            ConnectionIO.rollback()
          } <* ConnectionIO.commit() <* ConnectionIO.setAutoCommit(true)
      )
