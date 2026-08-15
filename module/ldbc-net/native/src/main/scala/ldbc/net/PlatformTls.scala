/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicLong }
import java.util.concurrent.ConcurrentHashMap

import scala.scalanative.runtime.{ fromRawPtr, toRawPtr, Intrinsics }
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

import ldbc.fx.Fx

/**
 * Scala Native TLS engine over s2n-tls (design Phase 3 / P1). The s2n connection drives IO through
 * custom recv/send callbacks that perform non-blocking `recv`/`send` on the underlying [[FdSocket]]'s
 * fd; when s2n reports it blocked, the driver awaits fd readiness on the engine's poller and retries,
 * so no thread is parked during handshake or encrypted IO. Hostname verification uses s2n's
 * `verify_host` callback — which delivers one certificate name per call — backed by the shared
 * [[HostnameMatcher]] (RFC 6125).
 */
private[net] object PlatformTls:

  private val initialised = new AtomicBoolean(false)

  /**
   * Wraps `socket` in a TLS client session (see [[Tls.client]]). The s2n connection is built without
   * I/O, then the handshake is driven asynchronously over the engine's poller (non-blocking, using
   * s2n's blocked-status), so no thread is parked during negotiation.
   */
  def client(socket: Socket, host: String, port: Int, ssl: SSL): Fx[Socket] =
    val _ = port
    socket match
      case raw: FdSocket =>
        Fx.delay(build(raw, host, ssl)).flatMap { tls =>
          tls.handshake
            .map(_ => tls.releaseHost())
            .flatMap(_ => Fx.pure[Socket](tls))
            .handleErrorWith { error =>
              tls.close().handleErrorWith(_ => Fx.unit).flatMap(_ => Fx.raiseError(error))
            }
        }
      case _ =>
        Fx.raiseError(new IllegalArgumentException("Native TLS requires a socket produced by the Native IoEngine"))

  /** Builds the s2n config + connection and installs callbacks. Pure setup — performs no socket I/O. */
  private def build(raw: FdSocket, host: String, tlsConfig: SSL): S2nTlsSocket =
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
        new S2nTlsSocket(conn, config, raw, ioId, hostId)
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
   * Applies the trust and hostname-verification policy to the config; returns the registry id of
   * the expected host when a verifying callback was installed (or `-1`).
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

/**
 * Static callback bridge between s2n's C callbacks and Scala objects: contexts are passed as
 * integer ids encoded in the `void*` pointer and resolved through concurrent registries (C function
 * pointers cannot capture Scala state).
 */
private[net] object S2nBridge:

  private val ids     = new AtomicLong(1L)
  private val sockets = new ConcurrentHashMap[Long, Integer]()
  private val hosts   = new ConcurrentHashMap[Long, String]()

  /** Registers the fd the recv/send callbacks will read/write (non-blocking); returns its id. */
  def registerIo(fd: Int): Long =
    val id = ids.getAndIncrement()
    sockets.put(id, Integer.valueOf(fd))
    id

  /** Registers the expected hostname for the verify-host callback; returns its id. */
  def registerHost(host: String): Long =
    val id = ids.getAndIncrement()
    hosts.put(id, host)
    id

  /** Removes registry entries for a finished connection (negative ids are ignored). */
  def unregister(ioId: Long, hostId: Long): Unit =
    if ioId >= 0 then sockets.remove(ioId)
    if hostId >= 0 then hosts.remove(hostId)
    ()

  /** Number of live io registry entries, exposed for leak tests. */
  private[net] def registeredIo: Int = sockets.size

  /** Number of live expected-host registry entries, exposed for leak tests. */
  private[net] def registeredHosts: Int = hosts.size

  /** Encodes a registry id as the opaque `void*` context pointer. */
  def pointerOf(id: Long): Ptr[Byte] = fromRawPtr[Byte](Intrinsics.castLongToRawPtr(id))

  /** Decodes the opaque `void*` context pointer back to a registry id. */
  private def idOf(ctx: Ptr[Byte]): Long = Intrinsics.castRawPtrToLong(toRawPtr(ctx))

  /** s2n recv callback: non-blocking read of up to `len` bytes; `0` = EOF, `-1` = would-block/error (errno set). */
  val recvCb: CFuncPtr3[Ptr[Byte], Ptr[Byte], CUnsignedInt, CInt] =
    CFuncPtr3.fromScalaFunction { (ctx: Ptr[Byte], buf: Ptr[Byte], len: CUnsignedInt) =>
      val fd = sockets.get(idOf(ctx))
      if fd == null then -1
      else
        try
          val max  = len.toInt
          val arr  = new Array[Byte](max)
          val read = CInterop.recvOnce(fd.intValue, arr, max)
          if read <= 0 then read
          else
            var i = 0
            while i < read do
              buf(i) = arr(i)
              i += 1
            read
        catch case _: Throwable => -1
    }

  /** s2n send callback: non-blocking write of `len` bytes; `-1` = would-block/error (errno set). */
  val sendCb: CFuncPtr3[Ptr[Byte], Ptr[Byte], CUnsignedInt, CInt] =
    CFuncPtr3.fromScalaFunction { (ctx: Ptr[Byte], buf: Ptr[Byte], len: CUnsignedInt) =>
      val fd = sockets.get(idOf(ctx))
      if fd == null then -1
      else
        try
          val size = len.toInt
          val arr  = new Array[Byte](size)
          var i    = 0
          while i < size do
            arr(i) = buf(i)
            i += 1
          CInterop.sendOnce(fd.intValue, arr, size)
        catch case _: Throwable => -1
    }

  /** s2n verify-host callback: matches one certificate name against the registered expected host. */
  val verifyHostCb: CFuncPtr3[CString, CLong, Ptr[Byte], UByte] =
    CFuncPtr3.fromScalaFunction { (name: CString, nameLen: CLong, data: Ptr[Byte]) =>
      val host = hosts.get(idOf(data))
      if host == null || name == null then 0.toUByte
      else
        val length = nameLen.toInt
        val arr    = new Array[Byte](length)
        var i      = 0
        while i < length do
          arr(i) = name(i)
          i += 1
        val certName = new String(arr, StandardCharsets.US_ASCII)
        if HostnameMatcher.matchesName(certName, host) then 1.toUByte else 0.toUByte
    }

  /** s2n verify-host callback that accepts every name (used when `verifyHostname = false`). */
  val acceptAllCb: CFuncPtr3[CString, CLong, Ptr[Byte], UByte] =
    CFuncPtr3.fromScalaFunction { (_: CString, _: CLong, _: Ptr[Byte]) => 1.toUByte }
