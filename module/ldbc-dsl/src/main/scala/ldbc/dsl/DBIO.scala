/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import scala.concurrent.duration.FiniteDuration

import cats.*
import cats.data.NonEmptyList
import cats.free.Free
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.{ CancelScope, Poll, Sync }

import ldbc.sql.*
import ldbc.sql.logging.LogEvent

import ldbc.dsl.codec.Decoder
import ldbc.dsl.exception.UnexpectedContinuation
import ldbc.dsl.free.*
import ldbc.dsl.util.FactoryCompat

type DBIO[A] = Free[ConnectionOp, A]

object DBIO:

  def queryA[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[A] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     PreparedStatementIO.paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, ResultSetIO.unique(statement, decoder))
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
                     PreparedStatementIO.paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, ResultSetIO.whileM(statement, decoder, factory))
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
                     PreparedStatementIO.paramBind(params) *> PreparedStatementIO.executeQuery()
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
                     PreparedStatementIO.paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, ResultSetIO.nel(statement, decoder))
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
                  PreparedStatementIO.paramBind(params) *> PreparedStatementIO.executeUpdate()
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
                     PreparedStatementIO.paramBind(params) *> PreparedStatementIO.executeUpdate() *> PreparedStatementIO
                       .getGeneratedKeys()
                   )
      result <- ConnectionIO.embed(resultSet, ResultSetIO.unique(statement, decoder))
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

  def stream[A](statement: String, params: List[Parameter.Dynamic], decoder: Decoder[A]): fs2.Stream[DBIO, A] =
    (for
      preparedStatement              <- fs2.Stream.eval(ConnectionIO.prepareStatement(statement))
      (preparedStatement, resultSet) <- fs2.Stream.bracket {
                                          ConnectionIO.embed(
                                            preparedStatement,
                                            for
                                              _         <- PreparedStatementIO.setFetchSize(1)
                                              _         <- PreparedStatementIO.paramBind(params)
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

    def run[F[_]: Sync](connection: Connection[F]): F[A] =
      dbio
        .foldMap(new KleisliInterpreter[F](connection.logHandler).ConnectionInterpreter)
        .run(connection)

    def readOnly[F[_]: Sync](connection: Connection[F]): F[A] =
      (ConnectionIO.setReadOnly(true) *> dbio <* ConnectionIO.setReadOnly(false))
        .foldMap(new KleisliInterpreter[F](connection.logHandler).ConnectionInterpreter)
        .run(connection)

    def commit[F[_]: Sync](connection: Connection[F]): F[A] =
      (ConnectionIO.setReadOnly(false) *> ConnectionIO.setAutoCommit(true) *> dbio)
        .foldMap(new KleisliInterpreter[F](connection.logHandler).ConnectionInterpreter)
        .run(connection)

    def rollback[F[_]: Sync](connection: Connection[F]): F[A] =
      (ConnectionIO.setReadOnly(false) *> ConnectionIO.setAutoCommit(false) *> dbio <* ConnectionIO
        .rollback() <* ConnectionIO.setAutoCommit(true))
        .foldMap(new KleisliInterpreter[F](connection.logHandler).ConnectionInterpreter)
        .run(connection)

    def transaction[F[_]: Sync](connection: Connection[F]): F[A] =
      val interpreter = new KleisliInterpreter[F](connection.logHandler).ConnectionInterpreter
      ((ConnectionIO.setReadOnly(false) *> ConnectionIO.setAutoCommit(false) *> dbio).onError {
        _ => ConnectionIO.rollback()
      } <* ConnectionIO.commit() <* ConnectionIO.setAutoCommit(true))
        .foldMap(interpreter)
        .run(connection)
