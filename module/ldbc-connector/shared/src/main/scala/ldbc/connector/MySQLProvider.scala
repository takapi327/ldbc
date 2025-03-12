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

import ldbc.sql.{Provider, DatabaseMetaData}
import ldbc.sql.logging.LogHandler

trait MySQLProvider[F[_], A] extends Provider[F]:

  def setHost(host: String): MySQLProvider[F, A]
  def setPort(port: Int): MySQLProvider[F, A]
  def setUser(user: String): MySQLProvider[F, A]
  def setPassword(password: String): MySQLProvider[F, A]
  def setDatabase(database: String): MySQLProvider[F, A]
  def setDebug(debug: Boolean): MySQLProvider[F, A]
  def setSSL(ssl: SSL): MySQLProvider[F, A]
  def setSocketOptions(socketOptions: List[SocketOption]): MySQLProvider[F, A]
  def setReadTimeout(readTimeout: Duration): MySQLProvider[F, A]
  def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): MySQLProvider[F, A]
  def setDatabaseTerm(databaseTerm: DatabaseMetaData.DatabaseTerm): MySQLProvider[F, A]
  def setLogHandler(handler: LogHandler[F]): MySQLProvider[F, A]
  def setTracer(tracer: Tracer[F]): MySQLProvider[F, A]
  def setBefore[B](before: Connection[F] => F[B]): MySQLProvider[F, B]
  def setAfter(after: (A, Connection[F]) => F[Unit]): MySQLProvider[F, A]
  def setBeforeAfter[B](before: Connection[F] => F[B], after: (B, Connection[F]) => F[Unit]): MySQLProvider[F, B]

  def createConnection(): Resource[F, Connection[F]]
  def use[B](f: Connection[F] => F[B]): F[B]

object MySQLProvider:

  val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))
    
  private case class Impl[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
                                 host:                    String,
                                 port:                    Int,
                                 user:                    String,
                                 logHandler: LogHandler[F],
                                 password:                Option[String] = None,
                                 database:                Option[String] = None,
                                 debug:                   Boolean = false,
                                 ssl:                     SSL = SSL.None,
                                 socketOptions:           List[SocketOption] = defaultSocketOptions,
                                 readTimeout:             Duration = Duration.Inf,
                                 allowPublicKeyRetrieval: Boolean = false,
                                 databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
                                 tracer: Option[Tracer[F]] = None,
                                 before:                  Option[Connection[F] => F[A]] = None,
                                 after:                   Option[(A, Connection[F]) => F[Unit]] = None
                               ) extends MySQLProvider[F, A]:
    given Tracer[F] = tracer.getOrElse(Tracer.noop[F])

    override def setHost(host: String): MySQLProvider[F, A] =
      this.copy(host = host)

    override def setPort(port: Int): MySQLProvider[F, A] =
      this.copy(port = port)

    override def setUser(user: String): MySQLProvider[F, A] =
      this.copy(user = user)

    override def setPassword(password: String): MySQLProvider[F, A] =
      this.copy(password = Some(password))

    override def setDatabase(database: String): MySQLProvider[F, A] =
      this.copy(database = Some(database))

    override def setDebug(debug: Boolean): MySQLProvider[F, A] =
      this.copy(debug = debug)

    override def setSSL(ssl: SSL): MySQLProvider[F, A] =
      this.copy(ssl = ssl)

    override def setSocketOptions(socketOptions: List[SocketOption]): MySQLProvider[F, A] =
      this.copy(socketOptions = socketOptions)

    override def setReadTimeout(readTimeout: Duration): MySQLProvider[F, A] =
      this.copy(readTimeout = readTimeout)

    override def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): MySQLProvider[F, A] =
      this.copy(allowPublicKeyRetrieval = allowPublicKeyRetrieval)

    override def setDatabaseTerm(databaseTerm: DatabaseMetaData.DatabaseTerm): MySQLProvider[F, A] =
      this.copy(databaseTerm = Some(databaseTerm))

    override def setLogHandler(handler: LogHandler[F]): MySQLProvider[F, A] =
      this.copy(logHandler = handler)

    override def setTracer(tracer: Tracer[F]): MySQLProvider[F, A] =
      this.copy(tracer = Some(tracer))
      
    override def setBefore[B](before: Connection[F] => F[B]): MySQLProvider[F, B] =
      Impl(
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
        databaseTerm = databaseTerm,
        logHandler = logHandler,
        before = Some(before),
        after = None
      )
    override def setAfter(after: (A, Connection[F]) => F[Unit]): MySQLProvider[F, A] =
      this.copy(after = Some(after))

    override def setBeforeAfter[B](before: Connection[F] => F[B], after: (B, Connection[F]) => F[Unit]): MySQLProvider[F, B] =
      Impl(
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
        databaseTerm = databaseTerm,
        logHandler = logHandler,
        before = Some(before),
        after = Some(after)
      )

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
            databaseTerm = databaseTerm,
            logHandler = Some(logHandler),
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
            databaseTerm = databaseTerm,
            logHandler = Some(logHandler),
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
            databaseTerm = databaseTerm,
            logHandler = Some(logHandler),
          )

    override def use[B](f: Connection[F] => F[B]): F[B] =
      createConnection().use(f)

  def default[F[_]: Async: Network: Console: Hashing: UUIDGen](
    host:                    String,
    port:                    Int,
    user:                    String,
  ): MySQLProvider[F, Unit] = Impl[F, Unit](host, port, user, Connection.consoleLogger)

  def default[F[_] : Async : Network : Console : Hashing : UUIDGen](
    host: String,
    port: Int,
    user: String,
    password: String,
    database: String
  ): MySQLProvider[F, Unit] =
    default[F](host, port, user)
      .setPassword(password)
      .setDatabase(database)
