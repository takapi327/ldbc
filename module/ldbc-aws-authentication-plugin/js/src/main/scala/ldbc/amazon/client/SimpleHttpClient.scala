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

import cats.effect.*
import cats.effect.syntax.all.*

import fs2.*
import fs2.io.net.*
import fs2.io.net.tls.*

import ldbc.amazon.exception.*

/**
 * Secure HTTP client that supports both HTTP and HTTPS protocols.
 * 
 * Security Features:
 * - Validates URI schemes and rejects unsupported protocols
 * - Uses TLS for HTTPS connections with proper certificate validation
 * - Defaults to secure ports (443 for HTTPS, 80 for HTTP)
 * - Prevents credentials from being sent over cleartext connections
 * 
 * This addresses the security vulnerability where AWS credentials
 * could be sent over unencrypted HTTP connections.
 */
class SimpleHttpClient[F[_]: Network: Async](
  connectTimeout: Duration,
  readTimeout:    Duration
) extends HttpClient[F]:

  private def isHttps(uri: URI): Boolean =
    Option(uri.getScheme).exists(_.toLowerCase == "https")

  private def getDefaultPort(uri: URI): Int =
    if uri.getPort > 0 then uri.getPort
    else if isHttps(uri) then 443
    else 80

  private def validateScheme(uri: URI): F[Unit] =
    Option(uri.getScheme) match
      case None => Async[F].raiseError(new SdkClientException("URI scheme is required"))
      case Some(scheme) if scheme.toLowerCase == "http" =>
        // Log warning for HTTP usage, but allow it for non-sensitive endpoints
        Async[F].unit
      case Some(scheme) if scheme.toLowerCase == "https" => Async[F].unit
      case Some(unsupported)                             =>
        Async[F].raiseError(
          new SdkClientException(s"Unsupported URI scheme: $unsupported. Only http and https are supported.")
        )

  private def validateSecurityRequirements(uri: URI): F[Unit] =
    // AWS endpoints should always use HTTPS
    if Option(uri.getHost).exists(_.contains(".amazonaws.com")) && !isHttps(uri) then
      Async[F].raiseError(
        new SdkClientException(s"AWS endpoints require HTTPS. Attempted to use: ${ uri.getScheme }://${ uri.getHost }")
      )
    else Async[F].unit

  private def createSocket(address: SocketAddress[Host], isSecure: Boolean, host: String): Resource[F, Socket[F]] =
    if isSecure then
      for
        socket     <- Network[F].client(address)
        tlsContext <- Network[F].tlsContext.systemResource
        tlsSocket  <- tlsContext
                       .clientBuilder(socket)
                       .withParameters(TLSParameters(servername = Some(host)))
                       .build
      yield tlsSocket
    else Network[F].client(address)

  override def get(uri: URI, headers: Map[String, String]): F[HttpResponse] =
    for
      _ <- validateScheme(uri)
      _ <- validateSecurityRequirements(uri)
      host     = uri.getHost
      port     = getDefaultPort(uri)
      isSecure = isHttps(uri)
      path     = Option(uri.getPath).filter(_.nonEmpty).getOrElse("/") +
               Option(uri.getQuery).map("?" + _).getOrElse("")
      address  <- resolveAddress(host, port)
      response <- makeRequest(address, host, port, isSecure, "GET", path, headers, None)
    yield response

  override def put(uri: URI, headers: Map[String, String], body: String): F[HttpResponse] =
    for
      _ <- validateScheme(uri)
      _ <- validateSecurityRequirements(uri)
      host     = uri.getHost
      port     = getDefaultPort(uri)
      isSecure = isHttps(uri)
      path     = Option(uri.getPath).filter(_.nonEmpty).getOrElse("/") +
               Option(uri.getQuery).map("?" + _).getOrElse("")
      address  <- resolveAddress(host, port)
      response <- makeRequest(address, host, port, isSecure, "PUT", path, headers, Some(body))
    yield response

  override def post(uri: URI, headers: Map[String, String], body: String): F[HttpResponse] =
    for
      _ <- validateScheme(uri)
      _ <- validateSecurityRequirements(uri)
      host     = uri.getHost
      port     = getDefaultPort(uri)
      isSecure = isHttps(uri)
      path     = Option(uri.getPath).filter(_.nonEmpty).getOrElse("/") +
               Option(uri.getQuery).map("?" + _).getOrElse("")
      address  <- resolveAddress(host, port)
      response <- makeRequest(address, host, port, isSecure, "POST", path, headers, Some(body))
    yield response

  private def resolveAddress(host: String, port: Int): F[SocketAddress[Host]] =
    for
      h <- Async[F].fromOption(Host.fromString(host), new SdkClientException("Invalid host"))
      p <- Async[F].fromOption(Port.fromInt(port), new SdkClientException("Invalid port"))
    yield SocketAddress(h, p)

  private def sendRequest(
    socket:   Socket[F],
    method:   String,
    host:     String,
    port:     Int,
    isSecure: Boolean,
    path:     String,
    headers:  Map[String, String],
    body:     Option[String]
  ): F[Unit] =
    val defaultPort    = if isSecure then 443 else 80
    val hostHeader     = if port == defaultPort then host else s"$host:$port"
    val contentHeaders = body match {
      case Some(b) => Map("Content-Length" -> b.getBytes("UTF-8").length.toString)
      case None    => Map.empty
    }
    val allHeaders = headers ++ contentHeaders + ("Host" -> hostHeader) + ("Connection" -> "close")

    val requestLine        = s"$method $path HTTP/1.1\r\n"
    val headerLines        = allHeaders.map((k, v) => s"$k: $v\r\n").mkString
    val requestWithHeaders = requestLine + headerLines + "\r\n"
    val fullRequest        = body.map(requestWithHeaders + _).getOrElse(requestWithHeaders)

    Stream
      .emit(fullRequest)
      .through(text.utf8.encode)
      .through(socket.writes)
      .compile
      .drain

  private def parseStatusLine(line: String): F[Int] =
    // "HTTP/1.1 200 OK" -> 200
    line.split(" ").toList match
      case _ :: code :: _ =>
        code.toIntOption match
          case Some(c) => Async[F].pure(c)
          case None    => Async[F].raiseError(new CredentialsFetchError(s"Invalid status code: $code"))
      case _ => Async[F].raiseError(new CredentialsFetchError(s"Invalid status line: $line"))

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

          Async[F].pure(HttpResponse(statusCode, headers, body))
      case _ =>
        Async[F].raiseError(CredentialsFetchError("Empty response"))

  private def receiveResponse(socket: Socket[F]): F[HttpResponse] =
    socket.reads
      .through(text.utf8.decode)
      .compile
      .string
      .flatMap(parseHttpResponse)

  private def makeRequest(
    address:  SocketAddress[Host],
    host:     String,
    port:     Int,
    isSecure: Boolean,
    method:   String,
    path:     String,
    headers:  Map[String, String],
    body:     Option[String]
  ): F[HttpResponse] =
    createSocket(address, isSecure, host)
      .use { socket =>
        for
          _        <- sendRequest(socket, method, host, port, isSecure, path, headers, body)
          response <- receiveResponse(socket)
        yield response
      }
      .timeout(connectTimeout + readTimeout)
