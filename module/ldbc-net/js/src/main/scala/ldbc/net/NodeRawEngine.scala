/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.atomic.AtomicBoolean

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

/**
 * Scala.js [[RawIoEngine]] over node's async `net` module (event loop, non-blocking): the effect-free
 * counterpart of the former `Fx` node engine that the generic `ldbc.net.effect.IoEngine[F]` wraps once with
 * `F.async`, so every effect (`IO` / `Task` / `Fx`) drives the same node socket natively.
 *
 * The connect timeout is applied at the `F` layer ([[ldbc.net.effect.IoEngine.fromRaw]]), so `connect`
 * here only registers the `connect` / `error` events and hands back a [[Canceler]] that destroys the socket.
 */
private[net] object NodeRawEngine:
  private lazy val netModule = js.Dynamic.global.require("net")

  lazy val global: RawIoEngine = new RawIoEngine:
    override def connect(
      host:    String,
      port:    Int,
      options: SocketOptions,
      cb:      Either[Throwable, RawSocket] => Unit
    ): Canceler =
      val sock = netModule.connect(port.asInstanceOf[js.Any], host.asInstanceOf[js.Any])
      sock.setNoDelay(options.noDelay.asInstanceOf[js.Any])
      if options.keepAlive then sock.setKeepAlive(true.asInstanceOf[js.Any])
      val done = new AtomicBoolean(false)
      sock.on(
        "connect",
        ((() => if done.compareAndSet(false, true) then cb(Right(new NodeRawSocket(sock)))): js.Function0[Unit])
      )
      sock.on(
        "error",
        (
          (_: js.Dynamic) => if done.compareAndSet(false, true) then cb(Left(new RuntimeException("connect error")))
        ): js.Function1[js.Dynamic, Unit]
      )
      new Canceler:
        override def cancel(): Unit = { sock.destroy(); () }

/**
 * [[RawSocket]] over a node `net` socket. Incoming `data` / `end` / `error` events feed a [[ReadBuffer]]
 * that `read` drains; `write` copies bytes into a `Uint8Array` and completes on node's write callback.
 */
private[net] final class NodeRawSocket(sock: js.Dynamic) extends RawSocket:
  private val buffer = new ReadBuffer

  sock.on("data", ((chunk: Uint8Array) => buffer.onData(toBytes(chunk))): js.Function1[Uint8Array, Unit])
  sock.on(
    "error",
    ((_: js.Dynamic) => buffer.onError(new RuntimeException("socket error"))): js.Function1[js.Dynamic, Unit]
  )
  sock.on("end", ((() => buffer.onEof())): js.Function0[Unit])

  private def toBytes(chunk: Uint8Array): Array[Byte] =
    val arr = new Array[Byte](chunk.length)
    var i   = 0
    while i < chunk.length do { arr(i) = chunk(i).toByte; i += 1 }
    arr

  /** The raw node socket, exposed for the generic TLS layer and its tests. */
  private[net] def underlying: js.Dynamic = sock

  /**
   * Detaches this wrapper from its underlying node socket for a transport upgrade (STARTTLS): all event
   * listeners are removed and any pre-read bytes are drained and returned so the new layer can `unshift`
   * them back onto the stream. This wrapper must not be used afterwards.
   *
   * @return the raw node socket and the bytes read ahead of the upgrade point
   */
  private[net] def detachForUpgrade(): (js.Dynamic, Array[Byte]) =
    sock.removeAllListeners("data")
    sock.removeAllListeners("error")
    sock.removeAllListeners("end")
    (sock, buffer.drainPending())

  override def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit): Canceler =
    val cancelRead = buffer.read(n, cb)
    new Canceler:
      override def cancel(): Unit = cancelRead()

  override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit): Canceler =
    val u8 = new Uint8Array(bytes.length)
    var i  = 0
    while i < bytes.length do { u8(i) = (bytes(i) & 0xff).toShort; i += 1 }
    sock.write(u8, ((() => cb(Right(())))): js.Function0[Unit])
    Canceler.noop

  override def close(): Unit = { sock.end(); buffer.onClose(); () }
