/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import cats.effect.*
import cats.effect.std.Console
import cats.effect.std.UUIDGen

import fs2.hashing.Hashing
import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.DataSource
import ldbc.sql.DatabaseMetaData
import ldbc.logging.LogHandler

final case class MySQLDataSource[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
                                                                                   host:                    String,
                                                                                   port:                    Int,
                                                                                   user:                    String,
                                                                                   logHandler:              Option[LogHandler[F]]                 = None,
                                                                                   password:                Option[String]                        = None,
                                                                                   database:                Option[String]                        = None,
                                                                                   debug:                   Boolean                               = false,
                                                                                   ssl:                     SSL                                   = SSL.None,
                                                                                   socketOptions:           List[SocketOption]                    = MySQLConfig.defaultSocketOptions,
                                                                                   readTimeout:             Duration                              = Duration.Inf,
                                                                                   allowPublicKeyRetrieval: Boolean                               = false,
                                                                                   databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
                                                                                   tracer:                  Option[Tracer[F]]                     = None,
                                                                                   useCursorFetch:          Boolean                               = false,
                                                                                   useServerPrepStmts:      Boolean                               = false,
                                                                                   before:                  Option[Connection[F] => F[A]]         = None,
                                                                                   after:                   Option[(A, Connection[F]) => F[Unit]] = None
) extends DataSource[F]:
  given Tracer[F] = tracer.getOrElse(Tracer.noop[F])

  override def createConnection(): Resource[F, Connection[F]] =
    (before, after) match
      case (Some(b), Some(a)) =>
        Connection.withBeforeAfter(
          host = host,
          port = port,
          user = user,
          before = b,
          after = a,
          password = password,
          database = database,
          debug = debug,
          ssl = ssl,
          socketOptions = socketOptions,
          readTimeout = readTimeout,
          allowPublicKeyRetrieval = allowPublicKeyRetrieval,
          useCursorFetch = useCursorFetch,
          useServerPrepStmts = useServerPrepStmts,
          databaseTerm = databaseTerm
        )
      case (Some(b), None) =>
        Connection.withBeforeAfter(
          host = host,
          port = port,
          user = user,
          before = b,
          after = (_, _) => Async[F].unit,
          password = password,
          database = database,
          debug = debug,
          ssl = ssl,
          socketOptions = socketOptions,
          readTimeout = readTimeout,
          allowPublicKeyRetrieval = allowPublicKeyRetrieval,
          useCursorFetch = useCursorFetch,
          useServerPrepStmts = useServerPrepStmts,
          databaseTerm = databaseTerm
        )
      case (None, _) =>
        Connection(
          host = host,
          port = port,
          user = user,
          password = password,
          database = database,
          debug = debug,
          ssl = ssl,
          socketOptions = socketOptions,
          readTimeout = readTimeout,
          allowPublicKeyRetrieval = allowPublicKeyRetrieval,
          useCursorFetch = useCursorFetch,
          useServerPrepStmts = useServerPrepStmts,
          databaseTerm = databaseTerm
        )
        
  def setHost(newHost: String): MySQLDataSource[F, A] = copy(host = newHost)
  def setPort(newPort: Int): MySQLDataSource[F, A] = copy(port = newPort)
  def setUser(newUser: String): MySQLDataSource[F, A] = copy(user = newUser)
  def setPassword(newPassword: String): MySQLDataSource[F, A] = copy(password = Some(newPassword))
  def setDatabase(newDatabase: String): MySQLDataSource[F, A] = copy(database = Some(newDatabase))
  def setDebug(newDebug: Boolean): MySQLDataSource[F, A] = copy(debug = newDebug)
  def setSSL(newSSL: SSL): MySQLDataSource[F, A] = copy(ssl = newSSL)
  def setSocketOptions(newSocketOptions: List[SocketOption]): MySQLDataSource[F, A] =
    copy(socketOptions = newSocketOptions)
  def setReadTimeout(newReadTimeout: Duration): MySQLDataSource[F, A] =
    copy(readTimeout = newReadTimeout)
  def setAllowPublicKeyRetrieval(newAllowPublicKeyRetrieval: Boolean): MySQLDataSource[F, A] =
    copy(allowPublicKeyRetrieval = newAllowPublicKeyRetrieval)
  def setDatabaseTerm(newDatabaseTerm: DatabaseMetaData.DatabaseTerm): MySQLDataSource[F, A] =
    copy(databaseTerm = Some(newDatabaseTerm))
  def setTracer(newTracer: Tracer[F]): MySQLDataSource[F, A] =
    copy(tracer = Some(newTracer))
  def setUseCursorFetch(newUseCursorFetch: Boolean): MySQLDataSource[F, A] =
    copy(useCursorFetch = newUseCursorFetch)
  def setUseServerPrepStmts(newUseServerPrepStmts: Boolean): MySQLDataSource[F, A] =
    copy(useServerPrepStmts = newUseServerPrepStmts)
        
  def withBefore[B](before: Connection[F] => F[B]): MySQLDataSource[F, B] =
    MySQLDataSource(
      host = host,
      port = port,
      user = user,
      logHandler = logHandler,
      password = password,
      database = database,
      debug = debug,
      ssl = ssl,
      socketOptions = socketOptions,
      readTimeout = readTimeout,
      allowPublicKeyRetrieval = allowPublicKeyRetrieval,
      databaseTerm = databaseTerm,
      tracer = tracer,
      useCursorFetch = useCursorFetch,
      useServerPrepStmts = useServerPrepStmts,
      before = Some(before),
      after = None
    )

  def withAfter(after: (A, Connection[F]) => F[Unit]): MySQLDataSource[F, A] =
    copy(after = Some(after))

  def withBeforeAfter[B](
                          before: Connection[F] => F[B],
                          after: (B, Connection[F]) => F[Unit]
                        ): MySQLDataSource[F, B] =
    MySQLDataSource(
      host = host,
      port = port,
      user = user,
      logHandler = logHandler,
      password = password,
      database = database,
      debug = debug,
      ssl = ssl,
      socketOptions = socketOptions,
      readTimeout = readTimeout,
      allowPublicKeyRetrieval = allowPublicKeyRetrieval,
      databaseTerm = databaseTerm,
      tracer = tracer,
      useCursorFetch = useCursorFetch,
      useServerPrepStmts = useServerPrepStmts,
      before = Some(before),
      after = Some(after)
    )

object MySQLDataSource:

  def fromConfig[F[_]: Async: Network: Console: Hashing: UUIDGen](config: MySQLConfig): MySQLDataSource[F, Unit] =
    MySQLDataSource(
      host = config.host,
      port = config.port,
      user = config.user,
      password = config.password,
      database = config.database,
      debug = config.debug,
      ssl = config.ssl,
      socketOptions = config.socketOptions,
      readTimeout = config.readTimeout,
      allowPublicKeyRetrieval = config.allowPublicKeyRetrieval,
      databaseTerm = config.databaseTerm,
      useCursorFetch = config.useCursorFetch,
      useServerPrepStmts = config.useServerPrepStmts
    )
  
  def default[F[_]: Async: Network: Console: Hashing: UUIDGen]: MySQLDataSource[F, Unit] =
    fromConfig(MySQLConfig.default)

  def build[F[_]: Async: Network: Console: Hashing: UUIDGen](
    host: String,
    port: Int,
    user: String
  ): MySQLDataSource[F, Unit] =
    MySQLDataSource(
      host = host,
      port = port,
      user = user,
    )
