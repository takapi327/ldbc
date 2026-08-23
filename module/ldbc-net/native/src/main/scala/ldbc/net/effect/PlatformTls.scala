/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net.effect

import java.util.concurrent.atomic.AtomicBoolean

import scala.scalanative.runtime.Intrinsics
import scala.scalanative.unsafe.*

import ldbc.effect.syntax.*
import ldbc.effect.Async
import ldbc.net.{ FdRawSocket, HostnameMatcher, S2n, S2nBridge, SSL, TrustSource }

/**
 * A [[Socket]] that encrypts through an s2n connection, generic over the effect `F`, driven fully
 * non-blocking (design Phase 3 / P1). This is the effect-generic counterpart of `ldbc.net.S2nTlsSocket`:
 * each s2n call runs on the run loop; when s2n reports `S2N_BLOCKED_ON_READ`/`WRITE`, the fd's readiness is
 * awaited on the engine's poller (no thread is parked) and the call is retried. Reads never return more than
 * `n` bytes; `close()` sends close_notify best-effort, frees the s2n resources exactly once, and closes the
 * underlying socket. `close()` MUST be called (there is no finalizer).
 */
private[net] final class S2nTlsSocketF[F[_]](
  conn:   Ptr[Byte],
  config: Ptr[Byte],
  raw:    FdRawSocket,
  ioId:   Long,
  hostId: Long
)(using F: Async[F])
  extends Socket[F]:

  private val closed = new AtomicBoolean(false)
  private val engine = raw.ioEngine
  private val fd     = raw.fileDescriptor
  private val st     = raw.channelState

  /** `s2n_blocked_status`: 0 = not blocked, 1 = blocked on read, 2 = blocked on write. */
  private inline val BlockedRead  = 1
  private inline val BlockedWrite = 2

  /** Converts a JVM `Long` to a native `Size` (ssize_t). */
  private def toSize(value: Long): Size = Size.valueOf(Intrinsics.castLongToRawSize(value))

  /** Suspends until the fd is readable, then resumes on the poller thread. */
  private def awaitReadable: F[Unit] = F.async { cb =>
    F.delay {
      engine.armRead(fd, st, () => cb(Right(())))
      Some(F.delay { st.readReady = null }): Option[F[Unit]]
    }
  }

  /** Suspends until the fd is writable, then resumes on the poller thread. */
  private def awaitWritable: F[Unit] = F.async { cb =>
    F.delay {
      engine.armWrite(fd, st, () => cb(Right(())))
      Some(F.delay { st.writeReady = null }): Option[F[Unit]]
    }
  }

  /** Awaits readiness for the direction s2n blocked on, or fails if it was a genuine error. */
  private def awaitBlocked(blocked: Int, what: String): F[Unit] =
    blocked match
      case BlockedRead  => awaitReadable
      case BlockedWrite => awaitWritable
      case _            => F.raiseError(new RuntimeException(s"TLS $what failed"))

  /** Runs the handshake to completion, awaiting readiness whenever s2n blocks. */
  private[net] def handshake: F[Unit] =
    F.delay {
      val blocked = stackalloc[CInt]()
      val rc      = S2n.s2n_negotiate(conn, blocked)
      (rc, !blocked)
    }.flatMap { (rc, blocked) =>
      if rc >= 0 then F.unit
      else awaitBlocked(blocked, "handshake").flatMap(_ => handshake)
    }

  /** Frees the expected-host registry entry once the handshake has completed. */
  private[net] def releaseHost(): Unit = S2nBridge.unregister(-1L, hostId)

  override def read(n: Int): F[Option[Array[Byte]]] =
    if n <= 0 then F.pure(Some(Array.emptyByteArray))
    else
      val arr = new Array[Byte](n)
      def attempt: F[Option[Array[Byte]]] =
        F.delay {
          val blocked = stackalloc[CInt]()
          val got     = S2n.s2n_recv(conn, arr.at(0), toSize(n.toLong), blocked).toLong
          (got, !blocked)
        }.flatMap { (got, blocked) =>
          if got > 0 then F.pure(Some(java.util.Arrays.copyOf(arr, got.toInt)))
          else if got == 0 then F.pure(None)
          else awaitBlocked(blocked, "read").flatMap(_ => attempt)
        }
      attempt

  override def write(bytes: Array[Byte]): F[Unit] =
    def attempt(offset: Int): F[Unit] =
      if offset >= bytes.length then F.unit
      else
        F.delay {
          val blocked = stackalloc[CInt]()
          val sent    = S2n.s2n_send(conn, bytes.at(offset), toSize((bytes.length - offset).toLong), blocked).toLong
          (sent, !blocked)
        }.flatMap { (sent, blocked) =>
          if sent > 0 then attempt(offset + sent.toInt)
          else awaitBlocked(blocked, "write").flatMap(_ => attempt(offset))
        }
    attempt(0)

  override def close(): F[Unit] =
    /**
     * A single best-effort `close_notify`. It is deliberately NOT retried on `S2N_BLOCKED_ON_WRITE`: a peer
     * that has already gone away keeps the send blocked forever, so an await/retry loop would hang `close()`
     * (and thus a failed-handshake cleanup) indefinitely. When the peer is alive the 7-byte alert flushes
     * into the empty socket buffer in this one call; when it cannot, the following `raw.close()` still tears
     * the connection down.
     */
    val shutdown: F[Unit] =
      F.delay {
        val blocked = stackalloc[CInt]()
        S2n.s2n_shutdown_send(conn, blocked)
      }.map(_ => ())
    shutdown
      .handleErrorWith(_ => F.unit)
      .flatMap(_ => F.delay(release()))
      .flatMap(_ => F.delay(raw.close()))

  /** Frees the s2n connection/config and the io registry entry exactly once. */
  private def release(): Unit =
    if closed.compareAndSet(false, true) then
      S2n.s2n_connection_free(conn)
      S2n.s2n_config_free(config)
      S2nBridge.unregister(ioId, -1L)
      ()

/**
 * Scala Native generic TLS entry over s2n-tls (design Phase 3 / P1). The trust / hostname-verification /
 * connection-setup logic is identical to `ldbc.net.PlatformTls`; only the effect operations are abstracted
 * over `Async[F]`, and the plaintext socket is reached through its underlying [[FdRawSocket]].
 */
private[net] object PlatformTls:

  private val initialised = new AtomicBoolean(false)

  /**
   * Wraps `socket` in a TLS client session. The s2n connection is built without I/O, then the handshake is
   * driven asynchronously over the engine's poller (non-blocking), so no thread is parked during negotiation.
   */
  def client[F[_]](socket: Socket[F], host: String, port: Int, ssl: SSL)(using F: Async[F]): F[Socket[F]] =
    val _ = port
    socket match
      case backed: RawBackedSocket =>
        backed.underlying match
          case raw: FdRawSocket =>
            F.delay(build(raw, host, ssl)).flatMap { tls =>
              tls.handshake
                .map(_ => tls.releaseHost())
                .flatMap(_ => F.pure[Socket[F]](tls))
                .handleErrorWith { error =>
                  tls.close().handleErrorWith(_ => F.unit).flatMap(_ => F.raiseError(error))
                }
            }
          case _ =>
            F.raiseError(new IllegalArgumentException("Native TLS requires a socket produced by the Native IoEngine"))
      case _ =>
        F.raiseError(new IllegalArgumentException("Native TLS requires a socket produced by the Native IoEngine"))

  /** Builds the s2n config + connection and installs callbacks. Pure setup — performs no socket I/O. */
  private def build[F[_]](raw: FdRawSocket, host: String, tlsConfig: SSL)(using F: Async[F]): S2nTlsSocketF[F] =
    if initialised.compareAndSet(false, true) then check(S2n.s2n_init(), "s2n_init")
    val config = S2n.s2n_config_new()
    if config == null then throw new RuntimeException("s2n_config_new returned null")
    var ioId   = -1L
    var hostId = -1L
    try
      hostId = applyTrust(config, host, tlsConfig)
      val conn = S2n.s2n_connection_new(1)
      if conn == null then throw new RuntimeException("s2n_connection_new returned null")
      try
        check(S2n.s2n_connection_set_config(conn, config), "s2n_connection_set_config")
        check(S2n.s2n_connection_set_blinding(conn, 1), "s2n_connection_set_blinding")
        if !HostnameMatcher.isIpLiteral(host) then
          Zone { check(S2n.s2n_set_server_name(conn, toCString(host)), "s2n_set_server_name") }
        ioId = S2nBridge.registerIo(raw.fileDescriptor)
        val ioCtx = S2nBridge.pointerOf(ioId)
        check(S2n.s2n_connection_set_recv_cb(conn, S2nBridge.recvCb), "s2n_connection_set_recv_cb")
        check(S2n.s2n_connection_set_send_cb(conn, S2nBridge.sendCb), "s2n_connection_set_send_cb")
        check(S2n.s2n_connection_set_recv_ctx(conn, ioCtx), "s2n_connection_set_recv_ctx")
        check(S2n.s2n_connection_set_send_ctx(conn, ioCtx), "s2n_connection_set_send_ctx")
        new S2nTlsSocketF[F](conn, config, raw, ioId, hostId)
      catch
        case error: Throwable =>
          S2n.s2n_connection_free(conn)
          throw error
    catch
      case error: Throwable =>
        S2n.s2n_config_free(config)
        S2nBridge.unregister(ioId, hostId)
        throw error

  /**
   * Applies the trust and hostname-verification policy to the config; returns the registry id of the expected
   * host when a verifying callback was installed (or `-1`).
   */
  private def applyTrust(config: Ptr[Byte], host: String, tlsConfig: SSL): Long =
    def installVerifyHost(verify: Boolean): Long =
      if verify then
        val id = S2nBridge.registerHost(host)
        check(
          S2n.s2n_config_set_verify_host_callback(config, S2nBridge.verifyHostCb, S2nBridge.pointerOf(id)),
          "s2n_config_set_verify_host_callback"
        )
        id
      else
        check(
          S2n.s2n_config_set_verify_host_callback(config, S2nBridge.acceptAllCb, S2nBridge.pointerOf(0L)),
          "s2n_config_set_verify_host_callback"
        )
        -1L
    tlsConfig match
      case SSL.System =>
        installVerifyHost(verify = true)
      case SSL.Trusted =>
        check(S2n.s2n_config_disable_x509_verification(config), "s2n_config_disable_x509_verification")
        -1L
      case SSL.Custom(trust, clientAuth, verifyHostname, tlsVersions) =>
        if clientAuth.isDefined then
          throw new UnsupportedOperationException("client certificates (mTLS) are not yet supported")
        if tlsVersions.nonEmpty then
          throw new UnsupportedOperationException("explicit tlsVersions are not yet supported on Native")
        trust match
          case TrustSource.System           => installVerifyHost(verifyHostname)
          case TrustSource.InsecureTrustAll =>
            check(S2n.s2n_config_disable_x509_verification(config), "s2n_config_disable_x509_verification")
            -1L
          case TrustSource.FromPemCerts(pem) =>
            check(S2n.s2n_config_wipe_trust_store(config), "s2n_config_wipe_trust_store")
            Zone {
              check(S2n.s2n_config_add_pem_to_trust_store(config, toCString(pem)), "s2n_config_add_pem_to_trust_store")
            }
            installVerifyHost(verifyHostname)
      case SSL.None           => -1L
      case SSL.Platform(_, _) =>
        throw new UnsupportedOperationException("SSL.Platform (a JVM SSLContext) is not supported on Scala Native")

  /** Raises when an s2n call reports failure. */
  private def check(rc: CInt, what: String): Unit =
    if rc < 0 then throw new RuntimeException(s"$what failed (rc=$rc)")
