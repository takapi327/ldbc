/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.client

import java.net.URI

import scala.concurrent.duration.*

import com.comcast.ip4s.*

import cats.syntax.all.*
import cats.MonadThrow

import cats.effect.*
import cats.effect.syntax.all.*

import fs2.*
import fs2.io.net.*

import ldbc.amazon.exception.*

class SimpleHttpClient[F[_]: Network: Async](
  connectTimeout: Duration,
  readTimeout:    Duration
)(using ev: MonadThrow[F])
  extends HttpClient[F]:

  override def get(uri: URI, headers: Map[String, String]): F[HttpResponse] =
    val host = uri.getHost
    val port = if uri.getPort > 0 then uri.getPort else 80
    val path = Option(uri.getPath).filter(_.nonEmpty).getOrElse("/") +
      Option(uri.getQuery).map("?" + _).getOrElse("")

    for
      address  <- resolveAddress(host, port)
      response <- makeRequest(address, host, port, path, headers)
    yield response

  private def resolveAddress(host: String, port: Int): F[SocketAddress[Host]] =
    for
      h <- ev.fromOption(Host.fromString(host), new SdkClientException("Invalid host"))
      p <- ev.fromOption(Port.fromInt(port), new SdkClientException("Invalid port"))
    yield SocketAddress(h, p)

  private def sendRequest(
    socket:  Socket[F],
    host:    String,
    port:    Int,
    path:    String,
    headers: Map[String, String]
  ): F[Unit] =
    val hostHeader = if port == 80 then host else s"$host:$port"
    val allHeaders = headers + ("Host" -> hostHeader) + ("Connection" -> "close")

    val requestLine = s"GET $path HTTP/1.1\r\n"
    val headerLines = allHeaders.map((k, v) => s"$k: $v\r\n").mkString
    val request     = requestLine + headerLines + "\r\n"

    Stream
      .emit(request)
      .through(text.utf8.encode)
      .through(socket.writes)
      .compile
      .drain

  private def parseStatusLine(line: String): F[Int] =
    // "HTTP/1.1 200 OK" -> 200
    line.split(" ").toList match
      case _ :: code :: _ =>
        code.toIntOption match
          case Some(c) => ev.pure(c)
          case None    => ev.raiseError(new CredentialsFetchError(s"Invalid status code: $code"))
      case _ => ev.raiseError(new CredentialsFetchError(s"Invalid status line: $line"))

  private def parseHeaderLine(line: String): Option[(String, String)] =
    line.split(": ", 2).toList match
      case key :: value :: Nil => Some(key -> value)
      case _                   => None

  private def parseHttpResponse(raw: String): F[HttpResponse] =
    val lines = raw.split("\r\n").toList

    lines match
      case statusLine :: rest =>
        parseStatusLine(statusLine).flatMap: statusCode =>
          val (headerLines, bodyLines) = rest.span(_.nonEmpty)
          val headers                  = headerLines.flatMap(parseHeaderLine).toMap
          val body                     = bodyLines.drop(1).mkString("\r\n") // drop empty line

          ev.pure(HttpResponse(statusCode, headers, body))
      case _ =>
        ev.raiseError(CredentialsFetchError("Empty response"))

  private def receiveResponse(socket: Socket[F]): F[HttpResponse] =
    socket.reads
      .through(text.utf8.decode)
      .compile
      .string
      .flatMap(parseHttpResponse)

  private def makeRequest(
    address: SocketAddress[Host],
    host:    String,
    port:    Int,
    path:    String,
    headers: Map[String, String]
  ): F[HttpResponse] =
    Network[F]
      .client(address)
      .use { socket =>
        for
          _        <- sendRequest(socket, host, port, path, headers)
          response <- receiveResponse(socket)
        yield response
      }
      .timeout(connectTimeout + readTimeout)
