/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import com.comcast.ip4s.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.DatabaseMetaData

import ldbc.connector.data.*
import ldbc.connector.exception.*
import ldbc.connector.net.*
import ldbc.connector.net.protocol.*

object Connection:

  private val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  private val defaultCapabilityFlags: Set[CapabilitiesFlags] = Set(
    CapabilitiesFlags.CLIENT_LONG_PASSWORD,
    CapabilitiesFlags.CLIENT_FOUND_ROWS,
    CapabilitiesFlags.CLIENT_LONG_FLAG,
    CapabilitiesFlags.CLIENT_PROTOCOL_41,
    CapabilitiesFlags.CLIENT_TRANSACTIONS,
    CapabilitiesFlags.CLIENT_RESERVED2,
    CapabilitiesFlags.CLIENT_MULTI_RESULTS,
    CapabilitiesFlags.CLIENT_PS_MULTI_RESULTS,
    CapabilitiesFlags.CLIENT_PLUGIN_AUTH,
    CapabilitiesFlags.CLIENT_CONNECT_ATTRS,
    CapabilitiesFlags.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA,
    CapabilitiesFlags.CLIENT_DEPRECATE_EOF,
    CapabilitiesFlags.CLIENT_QUERY_ATTRIBUTES,
    CapabilitiesFlags.MULTI_FACTOR_AUTHENTICATION
  )

  def apply[F[_]: Temporal: Network: Console](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    socketOptions:           List[SocketOption] = defaultSocketOptions,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG)
  ): Tracer[F] ?=> Resource[F, LdbcConnection[F]] =

    val logger: String => F[Unit] = s => Console[F].println(s"TLS: $s")

    for
      sslOp <- ssl.toSSLNegotiationOptions(if debug then logger.some else none)
      connection <- fromSocketGroup(
                      Network[F],
                      host,
                      port,
                      user,
                      password,
                      database,
                      debug,
                      socketOptions,
                      sslOp,
                      readTimeout,
                      allowPublicKeyRetrieval,
                      databaseTerm
                    )
    yield connection

  def fromSockets[F[_]: Temporal: Tracer: Console](
    sockets:                 Resource[F, Socket[F]],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = None
  ): Resource[F, LdbcConnection[F]] =
    val capabilityFlags = defaultCapabilityFlags ++
      (if database.isDefined then Set(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB) else Set.empty) ++
      (if sslOptions.isDefined then Set(CapabilitiesFlags.CLIENT_SSL) else Set.empty)
    val hostInfo = HostInfo(host, port, user, password, database)
    for
      given Exchange[F] <- Resource.eval(Exchange[F])
      protocol <-
        Protocol[F](sockets, hostInfo, debug, sslOptions, allowPublicKeyRetrieval, readTimeout, capabilityFlags)
      _                <- Resource.eval(protocol.startAuthentication(user, password.getOrElse("")))
      serverVariables  <- Resource.eval(protocol.serverVariables())
      readOnly         <- Resource.eval(Ref[F].of[Boolean](false))
      autoCommit       <- Resource.eval(Ref[F].of[Boolean](true))
      connectionClosed <- Resource.eval(Ref[F].of[Boolean](false))
      connection <-
        Resource.make(
          Temporal[F].pure(
            ConnectionImpl[F](
              protocol,
              serverVariables,
              database,
              readOnly,
              autoCommit,
              connectionClosed,
              databaseTerm.getOrElse(DatabaseMetaData.DatabaseTerm.CATALOG)
            )
          )
        )(_.close())
    yield connection

  def fromSocketGroup[F[_]: Tracer: Console](
    socketGroup:             SocketGroup[F],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    socketOptions:           List[SocketOption],
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = None
  )(using ev: Temporal[F]): Resource[F, LdbcConnection[F]] =

    def fail[A](msg: String): Resource[F, A] =
      Resource.eval(ev.raiseError(new SQLClientInfoException(msg)))

    def sockets: Resource[F, Socket[F]] =
      (Hostname.fromString(host), Port.fromInt(port)) match
        case (Some(validHost), Some(validPort)) =>
          socketGroup.client(SocketAddress(validHost, validPort), socketOptions)
        case (None, _) => fail(s"""Hostname: "$host" is not syntactically valid.""")
        case (_, None) => fail(s"Port: $port falls out of the allowed range.")

    fromSockets(
      sockets,
      host,
      port,
      user,
      password,
      database,
      debug,
      sslOptions,
      readTimeout,
      allowPublicKeyRetrieval,
      databaseTerm
    )
