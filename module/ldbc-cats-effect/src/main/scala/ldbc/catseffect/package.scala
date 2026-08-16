/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.catseffect

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS, NANOSECONDS }

import cats.Applicative
import cats.free.Free
import cats.syntax.all.*

import cats.effect.kernel.{ CancelScope, Poll, Sync }

import ldbc.*
import ldbc.free.*
import ldbc.logging.LogEvent

import ldbc.dsl.*
import ldbc.dsl.codec.Decoder
import ldbc.dsl.exception.DecodeFailureException

/**
 * Cats Effect streaming boundary for the shared `DBIO` DSL.
 *
 * `ldbc-dsl` intentionally stays free of fs2 and any `Sync[DBIO]` instance, so streaming is provided
 * here as a boundary extension. Importing `ldbc.catseffect.*` brings a `Sync[DBIO]` into scope and a
 * `Query[T].stream` extension that yields an `fs2.Stream[DBIO, T]`, allowing the familiar
 * `query.stream.compile.toList.readOnly(connector)` composition. This overlaps deliberately with what
 * `ldbc-cats-effect` provides for the Fx-native path.
 */

/**
 * A `Sync[DBIO]` instance backing fs2 streaming over `DBIO`.
 *
 * `DBIO` is a `Free[ConnectionOp]` program with no genuine cancellation, so `MonadCancel` operations
 * degrade to their uncancelable identities (`rootCancelScope = Uncancelable`). Effect suspension is
 * expressed with the algebra's `ConnectionOp.Suspend`, which the interpreter evaluates lazily.
 */
implicit val syncDBIO: Sync[DBIO] =
  new Sync[DBIO]:
    private val monad = Free.catsFreeMonadForFree[ConnectionOp]
    override val applicative:     Applicative[DBIO] = monad
    override val rootCancelScope: CancelScope       = CancelScope.Uncancelable
    override def pure[A](x:        A):                                   DBIO[A] = monad.pure(x)
    override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]):            DBIO[B] = monad.flatMap(fa)(f)
    override def tailRecM[A, B](a: A)(f:       A => DBIO[Either[A, B]]): DBIO[B] = monad.tailRecM(a)(f)
    override def raiseError[A](e: Throwable): DBIO[A] = ConnectionIO.raiseError(e)
    override def handleErrorWith[A](fa: DBIO[A])(f: Throwable => DBIO[A]): DBIO[A] =
      ConnectionIO.handleErrorWith(fa)(f)
    override def monotonic: DBIO[FiniteDuration] = ConnectionIO.suspend(FiniteDuration(System.nanoTime(), NANOSECONDS))
    override def realTime:  DBIO[FiniteDuration] =
      ConnectionIO.suspend(FiniteDuration(System.currentTimeMillis(), MILLISECONDS))
    override def suspend[A](hint: Sync.Type)(thunk: => A): DBIO[A]    = ConnectionIO.suspend(thunk)
    override def forceR[A, B](fa: DBIO[A])(fb: DBIO[B]):   DBIO[B]    = flatMap(attempt(fa))(_ => fb)
    override def canceled:                                 DBIO[Unit] = unit
    override def uncancelable[A](body: Poll[DBIO] => DBIO[A]): DBIO[A] =
      body(new Poll[DBIO]:
        override def apply[B](fb: DBIO[B]): DBIO[B] = fb)
    override def onCancel[A](fa: DBIO[A], fin: DBIO[Unit]): DBIO[A] = fa

/**
 * Streams the rows of a [[ldbc.dsl.Query]] as an `fs2.Stream[DBIO, T]`.
 *
 * The stream prepares the statement, applies the fetch size, binds parameters, executes the query and
 * pulls one decoded row at a time; the prepared statement is closed when the stream terminates. Compose
 * and run it via a connector, e.g. `query.stream.compile.toList.readOnly(connector)`.
 */
extension [T](query: Query[T])

  def stream: fs2.Stream[DBIO, T] = stream(1)

  def stream(fetchSize: Int): fs2.Stream[DBIO, T] =
    query match
      case impl: Query.Impl[T] @unchecked =>
        if fetchSize <= 0 then
          fs2.Stream.raiseError[DBIO](new IllegalArgumentException(s"fetchSize must be positive, but was: $fetchSize"))
        else streamImpl(impl.statement, impl.params, impl.decoder, fetchSize)
      case _ =>
        fs2.Stream.raiseError[DBIO](new IllegalStateException("Streaming is only supported for the default Query."))

private def streamImpl[A](
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
                                            _         <- DBIO.paramBind(params)
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
                    case true =>
                      decoder
                        .decode(1, statement)
                        .flatMap {
                          case Right(value) => ResultSetIO.pure(value)
                          case Left(error)  =>
                            ResultSetIO.raiseError(
                              new DecodeFailureException(error.message, decoder.offset, statement, error.cause)
                            )
                        }
                        .map(value => Some((value, rs)))
                    case false => ResultSetIO.pure(None)
                  }
                )
              }
  yield result).onError { ex =>
    fs2.Stream.eval(ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex)))
  } <*
    fs2.Stream.eval(ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value))))
