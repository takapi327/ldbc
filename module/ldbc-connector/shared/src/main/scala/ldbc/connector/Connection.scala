/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import com.comcast.ip4s.*

import cats.*
import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.net.*
import ldbc.connector.util.*
import ldbc.connector.exception.MySQLException

trait Connection[F[_]]

object Connection:

  private val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  object Recyclers:

    /**
     * Ensure the session is idle, then remove all channel listeners and reset all variables to
     * system defaults. Note that this is usually more work than you need to do. If your application
     * isn't running arbitrary statements then `minimal` might be more efficient.
     */
    def full[F[_]: Monad]: Recycler[F, Connection[F]] =
      Recycler.success[F, Connection[F]]

  def single[F[_]: Temporal: Tracer: Network: Console](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  ): Resource[F, Connection[F]] =
    singleTracer(host, port, user, password, debug, ssl, readTimeout, allowPublicKeyRetrieval).apply(Tracer[F])

  def singleTracer[F[_]: Temporal: Network: Console](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  ): Tracer[F] => Resource[F, Connection[F]] =
    Kleisli((_: Tracer[F]) =>
      pooled[F](
        host                    = host,
        port                    = port,
        user                    = user,
        password                = password,
        max                     = 1,
        debug                   = debug,
        ssl                     = ssl,
        readTimeout             = readTimeout,
        allowPublicKeyRetrieval = allowPublicKeyRetrieval
      )
    )
      .flatMap(f => Kleisli { implicit T: Tracer[F] => f(T) })
      .run

  def fromSockets[F[_]: Temporal: Tracer: Console](
    sockets:                 Resource[F, Socket[F]],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    debug:                   Boolean = false,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  ): Resource[F, Connection[F]] =
    for
      protocol <- MySQLProtocol[F](sockets, debug, sslOptions, readTimeout)
      _ <- Resource.eval(
             protocol.authenticate(user, password.getOrElse(""), sslOptions.isDefined, allowPublicKeyRetrieval)
           )
    yield new Connection[F] {}

  def fromSocketGroup[F[_]: Tracer: Console](
    socketGroup:             SocketGroup[F],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    debug:                   Boolean = false,
    socketOptions:           List[SocketOption],
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  )(using ev: Temporal[F]): Resource[F, Connection[F]] =

    def fail[A](msg: String): Resource[F, A] =
      Resource.eval(ev.raiseError(new MySQLException(sql = None, message = msg)))

    def sockets: Resource[F, Socket[F]] =
      (Hostname.fromString(host), Port.fromInt(port)) match
        case (Some(validHost), Some(validPort)) =>
          socketGroup.client(SocketAddress(validHost, validPort), socketOptions)
        case (None, _) => fail(s"""Hostname: "$host" is not syntactically valid.""")
        case (_, None) => fail(s"Port: $port falls out of the allowed range.")

    fromSockets(sockets, host, port, user, password, debug, sslOptions, readTimeout, allowPublicKeyRetrieval)

  def pooled[F[_]: Temporal: Network: Console](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    max:                     Int,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    socketOptions:           List[SocketOption] = Connection.defaultSocketOptions,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  ): Resource[F, Tracer[F] => Resource[F, Connection[F]]] =

    val logger: String => F[Unit] = s => Console[F].println(s"TLS: $s")

    def connection(socketGroup: SocketGroup[F], sslOp: Option[SSLNegotiation.Options[F]])(using
      Tracer[F]
    ): Resource[F, Connection[F]] =
      fromSocketGroup(
        socketGroup,
        host,
        port,
        user,
        password,
        debug,
        socketOptions,
        sslOp,
        readTimeout,
        allowPublicKeyRetrieval
      )

    for
      sslOp <- ssl.toSSLNegotiationOptions(if debug then logger.some else none)
      pool  <- Pool.ofF({ implicit T: Tracer[F] => connection(Network[F], sslOp) }, max)(Recyclers.full)
    yield pool
