/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.zio

import ldbc.sql.{ Connection, PreparedStatement, ResultSet }

import ldbc.dsl.*
import ldbc.dsl.codec.Decoder
import ldbc.dsl.exception.DecodeFailureException

import ldbc.free.*
import ldbc.logging.{ LogEvent, LogHandler }

import zio.{ Scope, Task, ZIO }
import zio.stream.ZStream

/**
 * ZIO streaming boundary for the shared `DBIO` DSL — the `zio.stream.ZStream` counterpart of the
 * `fs2.Stream` support in `ldbc.catseffect`.
 *
 * Unlike fs2, a `ZStream` is always over ZIO, so it cannot be built over the effect-agnostic `DBIO` monad
 * and later specialised by a connector. Instead the stream acquires a real connection from a
 * `DataSource[Task]`, holds it for the stream's lifetime, and pulls one decoded row at a time in `Task`
 * (each step interpreting a small `DBIO` against the held connection). The prepared statement and the
 * connection are released when the stream terminates.
 *
 * `connector` is the ordinary `ldbc.Connector[Task]` used for `readOnly`/`commit`. The one produced by
 * [[ldbc.zio.Connector.fromDataSource]] privately carries the `DataSource` the connection is acquired from,
 * which this method recovers internally — so callers pass one plain connector for both queries and streams,
 * and no ZIO-specific connector type is exposed. A connector without that capability yields a failed stream.
 *
 * {{{
 * val connector: ldbc.Connector[Task] = ldbc.zio.Connector.fromDataSource(MySQLDataSource.build[Task](...))
 * val rows: ZStream[Any, Throwable, City] = sql"SELECT ...".query[City].stream(connector, fetchSize = 100)
 * }}}
 */
extension [T](query: Query[T])

  def stream(connector: ldbc.Connector[Task]): ZStream[Any, Throwable, T] = stream(connector, 1)

  def stream(connector: ldbc.Connector[Task], fetchSize: Int): ZStream[Any, Throwable, T] =
    connector match
      case source: StreamableConnector =>
        query match
          case impl: Query.Impl[T] @unchecked =>
            if fetchSize <= 0 then
              ZStream.fail(new IllegalArgumentException(s"fetchSize must be positive, but was: $fetchSize"))
            else streamImpl(source.scopedConnection, impl.statement, impl.params, impl.decoder, fetchSize)
          case _ =>
            ZStream.fail(new IllegalStateException("Streaming is only supported for the default Query."))
      case _ =>
        ZStream.fail(
          new IllegalStateException(
            "This connector does not support streaming; build it with ldbc.zio.Connector.fromConnection/fromDataSource."
          )
        )

private def streamImpl[A](
  scoped:    ZIO[Scope, Throwable, Connection[Task]],
  statement: String,
  params:    List[Parameter.Dynamic],
  decoder:   Decoder[A],
  fetchSize: Int
): ZStream[Any, Throwable, A] =
  val logHandler: LogHandler[Task] = (_: LogEvent) => ZIO.unit
  val interpreter = new KleisliInterpreter[Task](logHandler)

  def runOn[X](connection: Connection[Task], dbio: ConnectionIO[X]): Task[X] =
    dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

  val execute: Connection[Task] => Task[(PreparedStatement[?], ResultSet[?])] = connection =>
    runOn(connection, ConnectionIO.prepareStatement(statement)).flatMap { preparedStatement =>
      runOn(
        connection,
        ConnectionIO.embed(
          preparedStatement,
          PreparedStatementIO
            .setFetchSize(fetchSize)
            .flatMap(_ => DBIO.paramBind(params))
            .flatMap(_ => PreparedStatementIO.executeQuery())
        )
      ).map(resultSet => (preparedStatement, resultSet))
    }

  def pull(connection: Connection[Task], resultSet: ResultSet[?]): Task[Option[A]] =
    runOn(
      connection,
      ConnectionIO.embed(
        resultSet,
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
              .map(Some(_))
          case false => ResultSetIO.pure(None)
        }
      )
    )

  ZStream.scoped(scoped).flatMap { connection =>
    ZStream
      .acquireReleaseWith(execute(connection)) {
        case (preparedStatement, _) =>
          runOn(connection, ConnectionIO.embed(preparedStatement, PreparedStatementIO.close())).orDie
      }
      .flatMap {
        case (_, resultSet) =>
          ZStream.unfoldZIO(())(_ => pull(connection, resultSet).map(_.map(value => (value, ()))))
      }
  }
