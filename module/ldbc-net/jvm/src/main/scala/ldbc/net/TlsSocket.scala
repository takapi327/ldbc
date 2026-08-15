/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.ByteBuffer

import javax.net.ssl.{ SSLEngine, SSLEngineResult, SSLException }
import javax.net.ssl.SSLEngineResult.HandshakeStatus
import javax.net.ssl.SSLEngineResult.Status

import ldbc.fx.Fx

/**
 * A [[Socket]] that encrypts through a JSSE [[javax.net.ssl.SSLEngine]] (design §4/§5).
 *
 * Buffer discipline: `netIn` accumulates ciphertext across reads (partial TLS records are kept, and
 * unconsumed trailing bytes are carried over via `compact()`), and `appIn` accumulates decrypted
 * plaintext that `read(n)` drains without ever returning more than `n` bytes. Reads must not overlap
 * other reads, nor writes other writes (a read and a write may run concurrently — `wrap`/`unwrap`
 * use disjoint buffers); [[Tls.client]] wraps this in a [[SerializedSocket]] to enforce that
 * structurally, so callers never rely on the contract by convention alone.
 */
private[net] final class TlsSocket(engine: SSLEngine, raw: Socket) extends Socket:

  private val emptyApp: ByteBuffer = ByteBuffer.allocate(0)
  private var netIn:    ByteBuffer = ByteBuffer.allocate(engine.getSession.getPacketBufferSize)
  private var netOut:   ByteBuffer = ByteBuffer.allocate(engine.getSession.getPacketBufferSize)
  private var appIn:    ByteBuffer = ByteBuffer.allocate(engine.getSession.getApplicationBufferSize)

  /** Runs the TLS handshake to completion, driven by wrap/unwrap result statuses (design §4). */
  private[net] def handshake: Fx[Unit] =
    Fx.delay(engine.beginHandshake()).flatMap(_ => step(engine.getHandshakeStatus))

  /** One transition of the handshake state machine; recursion is stack-safe under Fx. */
  private def step(status: HandshakeStatus): Fx[Unit] =
    status match
      case HandshakeStatus.NEED_WRAP =>
        wrapAndFlush(emptyApp).flatMap(result => step(result.getHandshakeStatus))
      case HandshakeStatus.NEED_UNWRAP =>
        unwrapAccumulating.flatMap(result => step(result.getHandshakeStatus))
      case HandshakeStatus.NEED_TASK =>
        runDelegatedTasks.flatMap(_ => step(engine.getHandshakeStatus))
      case _ => Fx.unit

  /** Wraps `src` once into `netOut` and writes the produced ciphertext, growing `netOut` on overflow. */
  private def wrapAndFlush(src: ByteBuffer): Fx[SSLEngineResult] =
    Fx.delay {
      netOut.clear()
      engine.wrap(src, netOut)
    }.flatMap { result =>
      result.getStatus match
        case Status.BUFFER_OVERFLOW =>
          Fx.delay { netOut = ByteBuffer.allocate(netOut.capacity * 2) }.flatMap(_ => wrapAndFlush(src))
        case _ =>
          Fx.delay {
            netOut.flip()
            val out = new Array[Byte](netOut.remaining())
            netOut.get(out)
            out
          }.flatMap { out =>
            if out.isEmpty then Fx.pure(result) else raw.write(out).map(_ => result)
          }
    }

  /**
   * Unwraps from the persistent `netIn` buffer, reading more ciphertext on `BUFFER_UNDERFLOW` while
   * keeping already-buffered partial records, and growing `appIn` on `BUFFER_OVERFLOW`.
   */
  private def unwrapAccumulating: Fx[SSLEngineResult] =
    Fx.delay(unwrapOnce()).flatMap { result =>
      result.getStatus match
        case Status.BUFFER_UNDERFLOW =>
          raw.read(engine.getSession.getPacketBufferSize).flatMap {
            case None         => Fx.raiseError(new SSLException("connection closed during TLS handshake"))
            case Some(cipher) => Fx.delay(appendNetIn(cipher)).flatMap(_ => unwrapAccumulating)
          }
        case Status.BUFFER_OVERFLOW =>
          Fx.delay { appIn = growPreserving(appIn) }.flatMap(_ => unwrapAccumulating)
        case _ => Fx.pure(result)
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
  private def runDelegatedTasks: Fx[Unit] =
    Fx.blocking {
      var task = engine.getDelegatedTask
      while task != null do
        task.run()
        task = engine.getDelegatedTask
    }

  override def read(n: Int): Fx[Option[Array[Byte]]] =
    if n <= 0 then Fx.pure(Some(Array.emptyByteArray))
    else
      Fx.delay(takePlain(n)).flatMap {
        case Some(bytes) => Fx.pure(Some(bytes))
        case None        =>
          if engine.isInboundDone then Fx.pure(None)
          else
            unwrapForData.flatMap { closed =>
              if closed && appIn.position() == 0 then Fx.pure(None) else read(n)
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
   * Unwraps until at least one plaintext byte is available, the stream ends, or a post-handshake
   * message requires driving the handshake state machine. Returns `true` when the inbound side is
   * finished (EOF).
   */
  private def unwrapForData: Fx[Boolean] =
    Fx.delay(unwrapOnce()).flatMap { result =>
      result.getStatus match
        case Status.BUFFER_UNDERFLOW =>
          raw.read(engine.getSession.getPacketBufferSize).flatMap {
            case None         => Fx.pure(true)
            case Some(cipher) => Fx.delay(appendNetIn(cipher)).flatMap(_ => unwrapForData)
          }
        case Status.BUFFER_OVERFLOW =>
          Fx.delay { appIn = growPreserving(appIn) }.flatMap(_ => unwrapForData)
        case Status.CLOSED => Fx.pure(true)
        case _             =>
          val hs = result.getHandshakeStatus
          if hs == HandshakeStatus.NEED_WRAP || hs == HandshakeStatus.NEED_TASK then step(hs).map(_ => false)
          else if appIn.position() > 0 then Fx.pure(false)
          else unwrapForData
    }

  override def write(bytes: Array[Byte]): Fx[Unit] =
    writeLoop(ByteBuffer.wrap(bytes))

  /** Wraps and sends until `src` is fully consumed, driving any post-handshake statuses en route. */
  private def writeLoop(src: ByteBuffer): Fx[Unit] =
    if !src.hasRemaining then Fx.unit
    else
      wrapAndFlush(src).flatMap { result =>
        val hs = result.getHandshakeStatus
        if hs == HandshakeStatus.NEED_UNWRAP || hs == HandshakeStatus.NEED_TASK then
          step(hs).flatMap(_ => writeLoop(src))
        else writeLoop(src)
      }

  override def close(): Fx[Unit] =
    Fx.delay(engine.closeOutbound())
      .flatMap(_ => closeNotifyLoop)
      .handleErrorWith(_ => Fx.unit)
      .flatMap(_ => raw.close())

  /** Flushes close_notify records until the outbound side reports done. */
  private def closeNotifyLoop: Fx[Unit] =
    if engine.isOutboundDone then Fx.unit
    else wrapAndFlush(emptyApp).flatMap(_ => closeNotifyLoop)
