/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net.effect

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.KeyStore

import javax.net.ssl.{
  SNIHostName,
  SSLContext,
  SSLEngine,
  SSLEngineResult,
  SSLException,
  TrustManager,
  TrustManagerFactory,
  X509TrustManager
}
import javax.net.ssl.SSLEngineResult.HandshakeStatus
import javax.net.ssl.SSLEngineResult.Status

import scala.jdk.CollectionConverters.*

import ldbc.effect.syntax.*
import ldbc.effect.Sync
import ldbc.net.{ HostnameMatcher, SSL, TrustSource }

/**
 * A [[Socket]] that encrypts through a JSSE [[javax.net.ssl.SSLEngine]] (design §4/§5), generic over the
 * effect `F`. This is the effect-generic counterpart of `ldbc.net.TlsSocket`: the SSLEngine buffer discipline
 * and handshake state machine are identical; only the effect operations are abstracted over `Sync[F]`.
 *
 * Buffer discipline: `netIn` accumulates ciphertext across reads (partial TLS records are kept, and
 * unconsumed trailing bytes are carried over via `compact()`), and `appIn` accumulates decrypted plaintext
 * that `read(n)` drains without ever returning more than `n` bytes.
 */
private[net] final class TlsSocketF[F[_]](engine: SSLEngine, raw: Socket[F])(using F: Sync[F]) extends Socket[F]:

  private val emptyApp: ByteBuffer = ByteBuffer.allocate(0)
  private var netIn:    ByteBuffer = ByteBuffer.allocate(engine.getSession.getPacketBufferSize)
  private var netOut:   ByteBuffer = ByteBuffer.allocate(engine.getSession.getPacketBufferSize)
  private var appIn:    ByteBuffer = ByteBuffer.allocate(engine.getSession.getApplicationBufferSize)

  /** Runs the TLS handshake to completion, driven by wrap/unwrap result statuses (design §4). */
  private[net] def handshake: F[Unit] =
    F.delay(engine.beginHandshake()).flatMap(_ => step(engine.getHandshakeStatus))

  /** One transition of the handshake state machine; recursion is stack-safe under `F`. */
  private def step(status: HandshakeStatus): F[Unit] =
    status match
      case HandshakeStatus.NEED_WRAP =>
        wrapAndFlush(emptyApp).flatMap(result => step(result.getHandshakeStatus))
      case HandshakeStatus.NEED_UNWRAP =>
        unwrapAccumulating.flatMap(result => step(result.getHandshakeStatus))
      case HandshakeStatus.NEED_TASK =>
        runDelegatedTasks.flatMap(_ => step(engine.getHandshakeStatus))
      case _ => F.unit

  /** Wraps `src` once into `netOut` and writes the produced ciphertext, growing `netOut` on overflow. */
  private def wrapAndFlush(src: ByteBuffer): F[SSLEngineResult] =
    F.delay {
      netOut.clear()
      engine.wrap(src, netOut)
    }.flatMap { result =>
      result.getStatus match
        case Status.BUFFER_OVERFLOW =>
          F.delay { netOut = ByteBuffer.allocate(netOut.capacity * 2) }.flatMap(_ => wrapAndFlush(src))
        case _ =>
          F.delay {
            netOut.flip()
            val out = new Array[Byte](netOut.remaining())
            netOut.get(out)
            out
          }.flatMap { out =>
            if out.isEmpty then F.pure(result) else raw.write(out).map(_ => result)
          }
    }

  /**
   * Unwraps from the persistent `netIn` buffer, reading more ciphertext on `BUFFER_UNDERFLOW` while keeping
   * already-buffered partial records, and growing `appIn` on `BUFFER_OVERFLOW`.
   */
  private def unwrapAccumulating: F[SSLEngineResult] =
    F.delay(unwrapOnce()).flatMap { result =>
      result.getStatus match
        case Status.BUFFER_UNDERFLOW =>
          raw.read(engine.getSession.getPacketBufferSize).flatMap {
            case None         => F.raiseError(new SSLException("connection closed during TLS handshake"))
            case Some(cipher) => F.delay(appendNetIn(cipher)).flatMap(_ => unwrapAccumulating)
          }
        case Status.BUFFER_OVERFLOW =>
          F.delay { appIn = growPreserving(appIn) }.flatMap(_ => unwrapAccumulating)
        case _ => F.pure(result)
    }

  /** Performs a single `unwrap`, carrying unconsumed ciphertext over via `compact()`. */
  private def unwrapOnce(): SSLEngineResult =
    netIn.flip()
    val result = engine.unwrap(netIn, appIn)
    netIn.compact()
    result

  /** Appends received ciphertext to `netIn`, growing it (preserving content) when full. */
  private def appendNetIn(bytes: Array[Byte]): Unit =
    if netIn.remaining < bytes.length then
      val bigger = ByteBuffer.allocate(math.max(netIn.capacity * 2, netIn.position() + bytes.length))
      netIn.flip()
      bigger.put(netIn)
      netIn = bigger
    netIn.put(bytes)
    ()

  /** Doubles a write-mode buffer while preserving its accumulated content. */
  private def growPreserving(buffer: ByteBuffer): ByteBuffer =
    val bigger = ByteBuffer.allocate(buffer.capacity * 2)
    buffer.flip()
    bigger.put(buffer)
    bigger

  /** Runs the engine's delegated tasks (certificate path validation etc.) off the run loop. */
  private def runDelegatedTasks: F[Unit] =
    F.blocking {
      var task = engine.getDelegatedTask
      while task != null do
        task.run()
        task = engine.getDelegatedTask
    }

  override def read(n: Int): F[Option[Array[Byte]]] =
    if n <= 0 then F.pure(Some(Array.emptyByteArray))
    else
      F.delay(takePlain(n)).flatMap {
        case Some(bytes) => F.pure(Some(bytes))
        case None        =>
          if engine.isInboundDone then F.pure(None)
          else
            unwrapForData.flatMap { closed =>
              if closed && appIn.position() == 0 then F.pure(None) else read(n)
            }
      }

  /** Drains up to `n` buffered plaintext bytes, or `None` when the plaintext buffer is empty. */
  private def takePlain(n: Int): Option[Array[Byte]] =
    if appIn.position() == 0 then None
    else
      appIn.flip()
      val length = math.min(n, appIn.remaining())
      val out    = new Array[Byte](length)
      appIn.get(out)
      appIn.compact()
      Some(out)

  /**
   * Unwraps until at least one plaintext byte is available, the stream ends, or a post-handshake message
   * requires driving the handshake state machine. Returns `true` when the inbound side is finished (EOF).
   */
  private def unwrapForData: F[Boolean] =
    F.delay(unwrapOnce()).flatMap { result =>
      result.getStatus match
        case Status.BUFFER_UNDERFLOW =>
          raw.read(engine.getSession.getPacketBufferSize).flatMap {
            case None         => F.pure(true)
            case Some(cipher) => F.delay(appendNetIn(cipher)).flatMap(_ => unwrapForData)
          }
        case Status.BUFFER_OVERFLOW =>
          F.delay { appIn = growPreserving(appIn) }.flatMap(_ => unwrapForData)
        case Status.CLOSED => F.pure(true)
        case _             =>
          val hs = result.getHandshakeStatus
          if hs == HandshakeStatus.NEED_WRAP || hs == HandshakeStatus.NEED_TASK then step(hs).map(_ => false)
          else if appIn.position() > 0 then F.pure(false)
          else unwrapForData
    }

  override def write(bytes: Array[Byte]): F[Unit] =
    writeLoop(ByteBuffer.wrap(bytes))

  /** Wraps and sends until `src` is fully consumed, driving any post-handshake statuses en route. */
  private def writeLoop(src: ByteBuffer): F[Unit] =
    if !src.hasRemaining then F.unit
    else
      wrapAndFlush(src).flatMap { result =>
        val hs = result.getHandshakeStatus
        if hs == HandshakeStatus.NEED_UNWRAP || hs == HandshakeStatus.NEED_TASK then
          step(hs).flatMap(_ => writeLoop(src))
        else writeLoop(src)
      }

  override def close(): F[Unit] =
    F.delay(engine.closeOutbound())
      .flatMap(_ => closeNotifyLoop)
      .handleErrorWith(_ => F.unit)
      .flatMap(_ => raw.close())

  /** Flushes close_notify records until the outbound side reports done. */
  private def closeNotifyLoop: F[Unit] =
    if engine.isOutboundDone then F.unit
    else wrapAndFlush(emptyApp).flatMap(_ => closeNotifyLoop)

/**
 * JVM generic TLS entry: drives a JSSE [[javax.net.ssl.SSLEngine]] over the plaintext [[Socket]]. Because
 * the engine is created with `createSSLEngine(host, port)` and `endpointIdentificationAlgorithm = "HTTPS"`,
 * JSSE performs hostname verification during the handshake itself (design §7). The trust / SNI / protocol
 * logic is identical to `ldbc.net.PlatformTls`.
 */
private[net] object PlatformTls:

  /**
   * Wraps `socket` in a TLS client session. The handshake runs to completion before the socket is returned;
   * on handshake failure the plaintext socket is closed.
   */
  def client[F[_]](socket: Socket[F], host: String, port: Int, ssl: SSL)(using F: Sync[F]): F[Socket[F]] =
    F.delay(createEngine(host, port, ssl)).flatMap { engine =>
      val tls = new TlsSocketF[F](engine, socket)
      tls.handshake
        .handleErrorWith { error =>
          socket.close().handleErrorWith(_ => F.unit).flatMap(_ => F.raiseError(error))
        }
        .map(_ => tls)
    }

  /** Builds a client-mode engine with trust, SNI (DNS hosts only), and hostname verification applied. */
  private def createEngine(host: String, port: Int, ssl: SSL): SSLEngine =
    val context = ssl match
      case SSL.Platform(ctx, _) => ctx
      case _                    =>
        val c = SSLContext.getInstance("TLS")
        c.init(null, trustManagers(ssl), null)
        c
    val engine = context.createSSLEngine(host, port)
    engine.setUseClientMode(true)
    val params = engine.getSSLParameters
    if ssl.verifyHostname then params.setEndpointIdentificationAlgorithm("HTTPS")
    if !HostnameMatcher.isIpLiteral(host) then params.setServerNames(List(new SNIHostName(host)).asJava)
    ssl match
      case SSL.Custom(_, _, _, versions) if versions.nonEmpty => params.setProtocols(versions.toArray)
      case _                                                  => ()
    engine.setSSLParameters(params)
    engine

  /** Resolves the trust managers for `ssl`; `null` selects the JDK default trust store. */
  private def trustManagers(ssl: SSL): Array[TrustManager] =
    ssl match
      case SSL.System                          => null
      case SSL.Trusted                         => Array(trustAll)
      case SSL.Custom(trust, clientAuth, _, _) =>
        if clientAuth.isDefined then
          throw new UnsupportedOperationException("client certificates (mTLS) are not yet supported")
        trust match
          case TrustSource.System            => null
          case TrustSource.InsecureTrustAll  => Array(trustAll)
          case TrustSource.FromPemCerts(pem) => fromPemCerts(pem)
      case SSL.None | SSL.Platform(_, _) =>
        throw new IllegalStateException("unreachable: None is handled by TlsUpgrade and Platform by createEngine")

  /** A trust manager that accepts every certificate (development / self-signed use only). */
  private val trustAll: X509TrustManager = new X509TrustManager:
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ()
    override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ()
    override def getAcceptedIssuers: Array[X509Certificate] = Array.empty

  /** Builds trust managers that trust exactly the PEM-encoded certificates in `pem`. */
  private def fromPemCerts(pem: String): Array[TrustManager] =
    val factory      = CertificateFactory.getInstance("X.509")
    val certificates = factory.generateCertificates(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)))
    val keyStore     = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null, null)
    certificates.asScala.zipWithIndex.foreach {
      case (cert, index) =>
        keyStore.setCertificateEntry(s"trusted-$index", cert)
    }
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(keyStore)
    tmf.getTrustManagers
