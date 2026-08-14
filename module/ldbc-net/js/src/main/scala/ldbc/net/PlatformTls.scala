/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.atomic.AtomicBoolean

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.util.control.NonFatal

import ldbc.fx.Fx

/**
 * Scala.js TLS engine over node's `tls.connect` (design Phase 2). The plaintext [[NodeSocket]] is
 * detached (listeners removed, pre-read bytes `unshift`ed back onto the stream) and the underlying
 * node socket is handed to `tls.connect`, which performs the handshake, chain validation
 * (`rejectUnauthorized`) and — via `servername`/`host` — node's built-in hostname verification. The
 * resulting `TLSSocket` is wrapped in a fresh [[NodeSocket]], so reads and writes carry plaintext.
 */
private[net] object PlatformTls:

  private lazy val tlsModule = js.Dynamic.global.require("tls")

  /** Wraps `socket` in a TLS client session (see [[Tls.client]]). */
  def client(socket: Socket, host: String, port: Int, ssl: SSL): Fx[Socket] =
    val _ = port
    socket match
      case node: NodeSocket => upgrade(node, host, ssl)
      case _ =>
        Fx.raiseError(new IllegalArgumentException("JS TLS requires a socket produced by the node IoEngine"))

  /** Detaches the plaintext wrapper, replays pre-read bytes, and starts the TLS handshake. */
  private def upgrade(node: NodeSocket, host: String, ssl: SSL): Fx[Socket] =
    Fx.async { cb =>
      val done = new AtomicBoolean(false)
      try
        val (raw, pending) = node.detachForUpgrade()
        if pending.nonEmpty then raw.unshift(toUint8Array(pending))
        val options = buildOptions(host, ssl)
        options.socket = raw
        val tlsSock = tlsModule.connect(options)
        tlsSock.on(
          "secureConnect",
          ((() => if done.compareAndSet(false, true) then cb(Right(new NodeSocket(tlsSock)))): js.Function0[Unit])
        )
        tlsSock.on(
          "error",
          ((err: js.Dynamic) =>
            if done.compareAndSet(false, true) then cb(Left(new RuntimeException(s"TLS handshake failed: $err")))
          ): js.Function1[js.Dynamic, Unit]
        )
        new Fx.Canceler { override def cancel(): Unit = { tlsSock.destroy(); () } }
      catch
        case NonFatal(error) =>
          if done.compareAndSet(false, true) then cb(Left(error))
          Fx.Canceler.noop
    }

  /** Maps a [[SSL]] to node `tls.connect` options (trust, identity, protocol versions). */
  private def buildOptions(host: String, ssl: SSL): js.Dynamic =
    val options = js.Dynamic.literal()
    ssl match
      case SSL.System  => ()
      case SSL.Trusted => options.rejectUnauthorized = false
      case SSL.Custom(trust, clientAuth, verifyHostname, tlsVersions) =>
        if clientAuth.isDefined then
          throw new UnsupportedOperationException("client certificates (mTLS) are not yet supported")
        trust match
          case TrustSource.System            => ()
          case TrustSource.InsecureTrustAll  => options.rejectUnauthorized = false
          case TrustSource.FromPemCerts(pem) => options.ca = pem
        if !verifyHostname then
          options.checkServerIdentity =
            ((_: js.Dynamic, _: js.Dynamic) => js.undefined): js.Function2[js.Dynamic, js.Dynamic, js.Any]
        if tlsVersions.nonEmpty then
          options.minVersion = tlsVersions.min
          options.maxVersion = tlsVersions.max
      case SSL.None => ()
      case SSL.Platform(_, _) =>
        throw new UnsupportedOperationException("SSL.Platform (a JVM SSLContext) is not supported on Scala.js")
    options.host = host
    if !HostnameMatcher.isIpLiteral(host) then options.servername = host
    options

  /** Converts bytes to a `Uint8Array` for `stream.unshift`. */
  private def toUint8Array(bytes: Array[Byte]): Uint8Array =
    val out = new Uint8Array(bytes.length)
    var i   = 0
    while i < bytes.length do
      out(i) = (bytes(i) & 0xff).toShort
      i += 1
    out
