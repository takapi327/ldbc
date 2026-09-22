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
 * counterpart of the former `Fx` node engine that the generic `ldbc.net.IoEngine[F]` wraps once with
 * `F.async`, so every effect (`IO` / `Task` / `Fx`) drives the same node socket natively.
 *
 * The connect timeout is applied at the `F` layer ([[ldbc.net.IoEngine.fromRaw]]), so `connect`
 * here only registers the `connect` / `error` events and hands back a [[Canceler]] that destroys the socket.
 */
private[net] object NodeRawEngine:
  private lazy val netModule = js.Dynamic.global.require("net")

  /**
   * Turns a node error object into an [[java.io.IOException]], keeping what node reported.
   *
   * `code` (`ECONNREFUSED`, `EHOSTUNREACH`, `ETIMEDOUT`, …) and `message` are the only way to tell
   * these failures apart, and discarding them would leave Scala.js with far less diagnostic detail
   * than the JVM and Native engines. `IOException` rather than `RuntimeException` so the type
   * matches those engines too.
   */
  private[net] def nodeError(op: String, err: js.Dynamic): Throwable =
    def field(name: String): Option[String] =
      if err == null || js.isUndefined(err) then None
      else
        val value = err.selectDynamic(name)
        if value == null || js.isUndefined(value) then None else Some(value.toString)
    val code    = field("code").fold("")(c => s" ($c)")
    val message = field("message").fold("")(m => s": $m")
    new java.io.IOException(s"$op failed$code$message")

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
          (err: js.Dynamic) => if done.compareAndSet(false, true) then cb(Left(NodeRawEngine.nodeError("connect", err)))
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

  private var pendingWrite: Either[Throwable, Unit] => Unit = null

  /**
   * Settles the outstanding write, if there is one.
   *
   * Node only invokes a write callback once the chunk is flushed, so a peer that has stopped reading
   * leaves it pending indefinitely. Closing has to settle it, or the completion contract in
   * [[RawSocket]] would hold on the JVM and Native engines but not here. Clearing the slot first
   * keeps a late callback from node harmless.
   */
  private def settleWrite(result: Either[Throwable, Unit]): Unit =
    val waiting = pendingWrite
    pendingWrite = null
    if waiting != null then waiting(result)

  sock.on("data", ((chunk: Uint8Array) => buffer.onData(toBytes(chunk))): js.Function1[Uint8Array, Unit])
  sock.on(
    "error",
    ((err: js.Dynamic) => buffer.onError(NodeRawEngine.nodeError("socket", err))): js.Function1[js.Dynamic, Unit]
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

  /**
   * Writes `bytes` in full, completing on node's write callback.
   *
   * Node hands that callback an error as its first argument when the write fails, so the argument is
   * inspected rather than ignored: reporting success for a write that never reached the peer would
   * break the completion contract in [[RawSocket]] and hide the failure until the next read.
   */
  override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit): Canceler =
    val u8 = new Uint8Array(bytes.length)
    var i  = 0
    while i < bytes.length do { u8(i) = (bytes(i) & 0xff).toShort; i += 1 }
    if pendingWrite != null then
      cb(Left(new IllegalStateException("another write is already in progress on this socket")))
      return Canceler.noop
    pendingWrite = cb
    sock.write(
      u8,
      (
        (err: js.Dynamic) =>
          if err == null || js.isUndefined(err) then settleWrite(Right(()))
          else settleWrite(Left(NodeRawEngine.nodeError("write", err)))
      ): js.Function1[js.Dynamic, Unit]
    )
    Canceler.noop

  override def close(): Unit =
    sock.end()
    buffer.onClose()
    settleWrite(Left(new java.io.IOException("socket closed")))
