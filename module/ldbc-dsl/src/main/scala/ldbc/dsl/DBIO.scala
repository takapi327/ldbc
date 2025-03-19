/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.*
import cats.free.Free
import cats.syntax.all.*

import cats.effect.*
import cats.effect.kernel.Resource.ExitCase

import ldbc.sql.*
import ldbc.sql.logging.LogEvent

import ldbc.dsl.codec.Decoder
import ldbc.dsl.util.FactoryCompat

/**
 * A trait that represents the execution of a query.
 *
 * @tparam A
 *   The result type of the query
 */
sealed trait DBIOA[A]
private object DBIOA:
  /**
   * The priority of given has changed since Scala 3.7. If you use the Free monad, it will conflict with the one provided by cats free. This can be resolved by using implicit instead of given.
   * 
   * @see: https://scala-lang.org/2024/08/19/given-priority-change-3.7.html
   */
  implicit def monadThrowDBIO: MonadThrow[DBIO] = new MonadThrow[DBIO]:
    override def pure[A](a:        A): DBIO[A] = DBIO.liftF(DBIO.Pure(a))
    override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]) = fa.flatMap(f)
    override def tailRecM[A, B](a: A)(f: A => DBIO[Either[A, B]]): DBIO[B] = f(a).flatMap {
      case Left(next)   => tailRecM(next)(f)
      case Right(value) => DBIO.pure(value)
    }
    override def ap[A, B](ff: DBIO[A => B])(fa: DBIO[A]): DBIO[B] =
      for
        a <- fa
        f <- ff
      yield f(a)
    override def raiseError[A](e: Throwable): DBIO[A] =
      Free.liftF(DBIO.RaiseError(e))
    override def handleErrorWith[A](fa: DBIO[A])(f: Throwable => DBIO[A]): DBIO[A] =
      Free.liftF(DBIO.HandleErrorWith(fa, f))

type DBIO[A] = Free[DBIOA, A]

object DBIO extends ParamBinder:
  final case class QueryA[A](statement: String, params: List[Parameter.Dynamic], decoder: Decoder[A]) extends DBIOA[A]
  final case class QueryTo[G[_], A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A],
    factory:   FactoryCompat[A, G[A]]
  ) extends DBIOA[G[A]]
  final case class Update(statement: String, params: List[Parameter.Dynamic]) extends DBIOA[Int]
  final case class Returning[A](statement: String, params: List[Parameter.Dynamic], decoder: Decoder[A])
    extends DBIOA[A]
  final case class Sequence(statements: List[String])                       extends DBIOA[Array[Int]]
  final case class Pure[A](value: A)                                        extends DBIOA[A]
  final case class RaiseError[A](e: Throwable)                              extends DBIOA[A]
  final case class HandleErrorWith[A](fa: DBIO[A], f: Throwable => DBIO[A]) extends DBIOA[A]

  private[ldbc] class Ops[A](dbio: DBIO[A]):
    /**
     * The function that actually executes the query.
     *
     * @return
     *   The result of the query
     */
    def run[F[_]: MonadCancelThrow](connection: Connection[F]): F[A] =
      dbio.foldMap(connection.interpreter)

    /**
     * Functions for managing the processing of connections in a read-only manner.
     */
    def readOnly[F[_]: MonadCancelThrow](connection: Connection[F]): F[A] =
      connection.setReadOnly(true) *> run(connection) <* connection.setReadOnly(false)

    /**
     * Functions to manage the processing of connections for writing.
     */
    def commit[F[_]: MonadCancelThrow](connection: Connection[F]): F[A] =
      connection.setReadOnly(false) *> connection.setAutoCommit(true) *> run(connection)

    /**
     * Functions to manage the processing of connections, always rolling back.
     */
    def rollback[F[_]: MonadCancelThrow](connection: Connection[F]): F[A] =
      connection.setReadOnly(false) *> connection.setAutoCommit(false) *> run(connection) <* connection
        .rollback() <* connection.setAutoCommit(true)

    /**
     * Functions to manage the processing of connections in a transaction.
     */
    def transaction[F[_]: MonadCancelThrow](connection: Connection[F]): F[A] =
      val acquire = connection.setReadOnly(false) *> connection.setAutoCommit(false) *> MonadThrow[F].pure(connection)

      val release = (connection: Connection[F], exitCase: ExitCase) =>
        (exitCase match
          case ExitCase.Errored(_) | ExitCase.Canceled => connection.rollback()
          case _                                       => connection.commit()
        )
          *> connection.setAutoCommit(true)

      Resource
        .makeCase(acquire)(release)
        .use(run)

  def liftF[A](dbio: DBIOA[A]): DBIO[A] = Free.liftF(dbio)

  def queryA[A](statement: String, params: List[Parameter.Dynamic], decoder: Decoder[A]): DBIO[A] =
    liftF(QueryA(statement, params, decoder))
  def queryTo[G[_], A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A],
    factory:   FactoryCompat[A, G[A]]
  ): DBIO[G[A]] =
    liftF(QueryTo(statement, params, decoder, factory))
  def update(statement: String, params: List[Parameter.Dynamic]): DBIO[Int] =
    liftF(Update(statement, params))
  def returning[A](statement: String, params: List[Parameter.Dynamic], decoder: Decoder[A]): DBIO[A] =
    liftF(Returning(statement, params, decoder))
  def sequence(statements: List[String]): DBIO[Array[Int]] = liftF(Sequence(statements))
  def pure[A](value:       A):            DBIO[A]          = Free.pure(value)
  def raiseError[A](e:     Throwable):    DBIO[A]          = Free.liftF(RaiseError(e))
  def sequence[A](dbios: DBIO[A]*): DBIO[List[A]] =
    dbios.toList.sequence

  extension [F[_]: MonadCancelThrow](connection: Connection[F])
    def interpreter: DBIOA ~> F =
      new (DBIOA ~> F):
        override def apply[A](fa: DBIOA[A]): F[A] =
          fa match
            case QueryA(statement, params, decoder) =>
              given Decoder[A] = decoder
              (for
                prepareStatement <- connection.prepareStatement(statement)
                resultSet        <- paramBind(prepareStatement, params) >> prepareStatement.executeQuery()
                result <- summon[ResultSetConsumer[F, A]].consume(resultSet, statement) <* prepareStatement.close()
              yield result)
                .onError(ex =>
                  connection.logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
                ) <*
                connection.logHandler.run(LogEvent.Success(statement, params.map(_.value)))
            case QueryTo(statement, params, decoder, factory) =>
              (for
                prepareStatement <- connection.prepareStatement(statement)
                resultSet        <- paramBind(prepareStatement, params) >> prepareStatement.executeQuery()
                result <- {
                  val builder = factory.newBuilder
                  while resultSet.next() do
                    decoder.decode(resultSet, 1) match
                      case Right(value) => builder += value
                      case Left(error) =>
                        throw new ldbc.dsl.exception.DecodeFailureException(
                          error.message,
                          decoder.offset,
                          statement,
                          error.cause
                        )
                  MonadCancelThrow[F].pure(builder.result())
                }
                _ <- prepareStatement.close()
              yield result)
                .onError(ex =>
                  connection.logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
                ) <*
                connection.logHandler.run(LogEvent.Success(statement, params.map(_.value)))

            case Update(statement, params) =>
              (for
                prepareStatement <- connection.prepareStatement(statement)
                result <-
                  paramBind(prepareStatement, params) >> prepareStatement.executeUpdate() <* prepareStatement
                    .close()
              yield result)
                .onError(ex =>
                  connection.logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
                ) <*
                connection.logHandler.run(LogEvent.Success(statement, params.map(_.value)))
            case Returning(statement, params, decoder) =>
              given Decoder[A] = decoder
              (for
                prepareStatement <- connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
                resultSet <- paramBind(prepareStatement, params) >> prepareStatement.executeUpdate() >> prepareStatement
                               .getGeneratedKeys()
                result <- summon[ResultSetConsumer[F, A]].consume(resultSet, statement) <* prepareStatement.close()
              yield result)
                .onError(ex =>
                  connection.logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
                ) <*
                connection.logHandler.run(LogEvent.Success(statement, params.map(_.value)))
            case Sequence(statements) =>
              (for
                statement <- connection.createStatement()
                _         <- statements.map(statement.addBatch).sequence
                result    <- statement.executeBatch()
              yield result)
                .onError(ex =>
                  connection.logHandler.run(LogEvent.ProcessingFailure(statements.mkString("\n"), List.empty, ex))
                ) <*
                connection.logHandler.run(LogEvent.Success(statements.mkString("\n"), List.empty))
            case Pure(value)            => MonadCancelThrow[F].pure(value)
            case RaiseError(e)          => MonadCancelThrow[F].raiseError(e)
            case HandleErrorWith(fa, f) => fa.foldMap(interpreter).handleErrorWith(e => f(e).foldMap(interpreter))
