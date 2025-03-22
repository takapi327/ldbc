/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
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

import ldbc.sql.{ DatabaseMetaData, Provider }
import ldbc.sql.logging.LogHandler

trait ConnectionProvider[F[_], A] extends Provider[F]:

  /**
   * Update the host information of the database to be connected.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setHost("127.0.0.1")
   * }}}
   *
   * @param host
   *   Host information of the database to be connected
   */
  def setHost(host: String): ConnectionProvider[F, A]

  /**
   * Update the port information of the database to be connected.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setPort(3306)
   * }}}
   *
   * @param port
   *   Port information of the database to be connected
   */
  def setPort(port: Int): ConnectionProvider[F, A]

  /**
   * Update the user information of the database to be connected.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setUser("root")
   * }}}
   *
   * @param user
   *   User information of the database to be connected
   */
  def setUser(user: String): ConnectionProvider[F, A]

  /**
   * Update the password information of the database to be connected.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setPassword("password")
   * }}}
   *
   * @param password
   *   Password information of the database to be connected
   */
  def setPassword(password: String): ConnectionProvider[F, A]

  /**
   * Update the database to be connected.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setDatabase("database")
   * }}}
   *
   * @param database
   *   Database name to connect to
   */
  def setDatabase(database: String): ConnectionProvider[F, A]

  /**
   * Update the setting of whether or not to output the log of packet communications for connection processing.
   *
   * Default is false.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setDebug(true)
   * }}}
   *
   * @param debug
   *   Whether packet communication logs are output or not
   */
  def setDebug(debug: Boolean): ConnectionProvider[F, A]

  /**
   * Update whether SSL communication is used.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setSSL(SSL.Trusted)
   * }}}
   *
   * @param ssl
   *   SSL set value. Changes the way certificates are operated, etc.
   */
  def setSSL(ssl: SSL): ConnectionProvider[F, A]

  /**
   * Update socket options for TCP/UDP sockets.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .addSocketOption(SocketOption.noDelay(true))
   * }}}
   *
   * @param socketOption
   *   Socket options for TCP/UDP sockets
   */
  def addSocketOption(socketOption: SocketOption): ConnectionProvider[F, A]

  /**
   * Update socket options for TCP/UDP sockets.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setSocketOptions(List(SocketOption.noDelay(true)))
   * }}}
   *
   * @param socketOptions
   *   List of socket options for TCP/UDP sockets
   */
  def setSocketOptions(socketOptions: List[SocketOption]): ConnectionProvider[F, A]

  /**
   * Update the read timeout value.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setReadTimeout(Duration.Inf)
   * }}}
   *
   * @param socketOptions
   *   Read timeout value
   */
  def setReadTimeout(readTimeout: Duration): ConnectionProvider[F, A]

  /**
   * Update the setting of whether or not to replace the public key.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setAllowPublicKeyRetrieval(true)
   * }}}
   *
   * @param allowPublicKeyRetrieval
   *   Whether to replace the public key
   */
  def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): ConnectionProvider[F, A]

  /**
   * Update whether the JDBC term “catalog” or “schema” is used to refer to the database in the application.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setDatabaseTerm(DatabaseMetaData.DatabaseTerm.SCHEMA)
   * }}}
   *
   * @param databaseTerm
   *   The JDBC terms [[DatabaseMetaData.DatabaseTerm.CATALOG]] and [[DatabaseMetaData.DatabaseTerm.SCHEMA]] are used to refer to the database.
   */
  def setDatabaseTerm(databaseTerm: DatabaseMetaData.DatabaseTerm): ConnectionProvider[F, A]

  /**
   * Update handler to output execution log of processes using connections.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setLogHandler(consoleLogger)
   * }}}
   *
   * @param handler
   *   Handler for outputting logs of process execution using connections.
   */
  def setLogHandler(handler: LogHandler[F]): ConnectionProvider[F, A]

  /**
   * Update tracers to output metrics.
   *
   * {{{
   *   ConnectionProvider
   *     ...
   *     .setTracer(Tracer.noop[IO])
   * }}}
   *
   * @param handler
   *   Tracer to output metrics
   */
  def setTracer(tracer: Tracer[F]): ConnectionProvider[F, A]

  /**
   * Add an optional process to be executed immediately after connection is established.
   *
   * {{{
   *   val before = ???
   *
   *   ConnectionProvider
   *     ...
   *     .withBefore(before)
   * }}}
   *
   * @param before
   *   Arbitrary processing to be performed immediately after connection is established
   * @tparam B
   *   Value returned after the process is executed. This value can be passed to the After process.
   */
  def withBefore[B](before: Connection[F] => F[B]): ConnectionProvider[F, B]

  /**
   * Add any process to be performed before disconnecting the connection.
   *
   * {{{
   *   val after = ???
   *
   *   ConnectionProvider
   *     ...
   *     .withAfter(after)
   * }}}
   *
   * @param after
   *   Arbitrary processing to be performed before disconnecting
   */
  def withAfter(after: (A, Connection[F]) => F[Unit]): ConnectionProvider[F, A]

  /**
   * Add optional processing to be performed immediately after establishing a connection and before disconnecting.
   *
   * The order of processing is as follows.
   *
   * {{{
   *   1. connection establishment
   *   2. before operation
   *   3. Processing using connections. Any processing used primarily in the operation of an application.
   *   4. after operation
   *   5. Disconnection
   * }}}
   *
   * {{{
   *   val before = ???
   *   val after = ???
   *
   *   ConnectionProvider
   *     ...
   *     .withBeforeAfter(before, after)
   * }}}
   *
   * @param before
   *   Arbitrary processing to be performed immediately after connection is established
   * @param after
   *   Arbitrary processing to be performed before disconnecting
   * @tparam B
   *   Value returned after the process is executed. This value can be passed to the After process.
   */
  def withBeforeAfter[B](before: Connection[F] => F[B], after: (B, Connection[F]) => F[Unit]): ConnectionProvider[F, B]

  /**
   * Create a connection managed by Resource.
   * 
   * {{{
   *   provider.createConnection().user { connection =>
   *     ???
   *   }
   * }}}
   */
  def createConnection(): Resource[F, Connection[F]]

object ConnectionProvider:

  val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  private case class Impl[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    host:                    String,
    port:                    Int,
    user:                    String,
    logHandler:              Option[LogHandler[F]]                 = None,
    password:                Option[String]                        = None,
    database:                Option[String]                        = None,
    debug:                   Boolean                               = false,
    ssl:                     SSL                                   = SSL.None,
    socketOptions:           List[SocketOption]                    = defaultSocketOptions,
    readTimeout:             Duration                              = Duration.Inf,
    allowPublicKeyRetrieval: Boolean                               = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    tracer:                  Option[Tracer[F]]                     = None,
    before:                  Option[Connection[F] => F[A]]         = None,
    after:                   Option[(A, Connection[F]) => F[Unit]] = None
  ) extends ConnectionProvider[F, A]:
    given Tracer[F] = tracer.getOrElse(Tracer.noop[F])

    override def setHost(host: String): ConnectionProvider[F, A] =
      this.copy(host = host)

    override def setPort(port: Int): ConnectionProvider[F, A] =
      this.copy(port = port)

    override def setUser(user: String): ConnectionProvider[F, A] =
      this.copy(user = user)

    override def setPassword(password: String): ConnectionProvider[F, A] =
      this.copy(password = Some(password))

    override def setDatabase(database: String): ConnectionProvider[F, A] =
      this.copy(database = Some(database))

    override def setDebug(debug: Boolean): ConnectionProvider[F, A] =
      this.copy(debug = debug)

    override def setSSL(ssl: SSL): ConnectionProvider[F, A] =
      this.copy(ssl = ssl)

    override def addSocketOption(socketOption: SocketOption): ConnectionProvider[F, A] =
      this.copy(socketOptions = socketOptions.::(socketOption))

    override def setSocketOptions(socketOptions: List[SocketOption]): ConnectionProvider[F, A] =
      this.copy(socketOptions = socketOptions)

    override def setReadTimeout(readTimeout: Duration): ConnectionProvider[F, A] =
      this.copy(readTimeout = readTimeout)

    override def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): ConnectionProvider[F, A] =
      this.copy(allowPublicKeyRetrieval = allowPublicKeyRetrieval)

    override def setDatabaseTerm(databaseTerm: DatabaseMetaData.DatabaseTerm): ConnectionProvider[F, A] =
      this.copy(databaseTerm = Some(databaseTerm))

    override def setLogHandler(handler: LogHandler[F]): ConnectionProvider[F, A] =
      this.copy(logHandler = Some(handler))

    override def setTracer(tracer: Tracer[F]): ConnectionProvider[F, A] =
      this.copy(tracer = Some(tracer))

    override def withBefore[B](before: Connection[F] => F[B]): ConnectionProvider[F, B] =
      Impl(
        host                    = host,
        port                    = port,
        user                    = user,
        password                = password,
        database                = database,
        debug                   = debug,
        ssl                     = ssl,
        socketOptions           = socketOptions,
        readTimeout             = readTimeout,
        allowPublicKeyRetrieval = allowPublicKeyRetrieval,
        databaseTerm            = databaseTerm,
        logHandler              = logHandler,
        before                  = Some(before),
        after                   = None
      )
    override def withAfter(after: (A, Connection[F]) => F[Unit]): ConnectionProvider[F, A] =
      this.copy(after = Some(after))

    override def withBeforeAfter[B](
      before: Connection[F] => F[B],
      after:  (B, Connection[F]) => F[Unit]
    ): ConnectionProvider[F, B] =
      Impl(
        host                    = host,
        port                    = port,
        user                    = user,
        password                = password,
        database                = database,
        debug                   = debug,
        ssl                     = ssl,
        socketOptions           = socketOptions,
        readTimeout             = readTimeout,
        allowPublicKeyRetrieval = allowPublicKeyRetrieval,
        databaseTerm            = databaseTerm,
        logHandler              = logHandler,
        before                  = Some(before),
        after                   = Some(after)
      )

    override def createConnection(): Resource[F, Connection[F]] =
      (before, after) match
        case (Some(b), Some(a)) =>
          Connection.withBeforeAfter(
            host                    = host,
            port                    = port,
            user                    = user,
            before                  = b,
            after                   = a,
            password                = password,
            database                = database,
            debug                   = debug,
            ssl                     = ssl,
            socketOptions           = socketOptions,
            readTimeout             = readTimeout,
            allowPublicKeyRetrieval = allowPublicKeyRetrieval,
            databaseTerm            = databaseTerm,
            logHandler              = logHandler
          )
        case (Some(b), None) =>
          Connection.withBeforeAfter(
            host                    = host,
            port                    = port,
            user                    = user,
            before                  = b,
            after                   = (_, _) => Async[F].unit,
            password                = password,
            database                = database,
            debug                   = debug,
            ssl                     = ssl,
            socketOptions           = socketOptions,
            readTimeout             = readTimeout,
            allowPublicKeyRetrieval = allowPublicKeyRetrieval,
            databaseTerm            = databaseTerm,
            logHandler              = logHandler
          )
        case (None, _) =>
          Connection(
            host                    = host,
            port                    = port,
            user                    = user,
            password                = password,
            database                = database,
            debug                   = debug,
            ssl                     = ssl,
            socketOptions           = socketOptions,
            readTimeout             = readTimeout,
            allowPublicKeyRetrieval = allowPublicKeyRetrieval,
            databaseTerm            = databaseTerm,
            logHandler              = logHandler
          )

    override def use[B](f: Connection[F] => F[B]): F[B] =
      createConnection().use(f)

  def default[F[_]: Async: Network: Console: Hashing: UUIDGen](
    host: String,
    port: Int,
    user: String
  ): ConnectionProvider[F, Unit] = Impl[F, Unit](host, port, user)

  def default[F[_]: Async: Network: Console: Hashing: UUIDGen](
    host:     String,
    port:     Int,
    user:     String,
    password: String,
    database: String
  ): ConnectionProvider[F, Unit] =
    default[F](host, port, user)
      .setPassword(password)
      .setDatabase(database)
