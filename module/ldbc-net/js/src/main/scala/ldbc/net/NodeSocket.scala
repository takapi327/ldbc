/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import ldbc.fx.Fx

/**
 * Scala.js [[Socket]] over a node `net` socket (event loop, non-blocking): incoming `data`/`end`/
 * `error` events feed a [[ReadBuffer]] that `read` drains, and `write` copies bytes into a
 * `Uint8Array`.
 * NOTE: compile-verified; runtime testing on node is a follow-up (requires a JS test run).
 */
private[net] final class NodeSocket(sock: js.Dynamic) extends Socket:
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

  override def read(n: Int): Fx[Option[Array[Byte]]] = Fx.async { cb =>
    val cancelRead = buffer.read(n, cb)
    new Fx.Canceler { override def cancel(): Unit = cancelRead() }
  }

  /** The raw node socket, exposed for the TLS layer and its tests. */
  private[net] def underlying: js.Dynamic = sock

  /**
   * Detaches this wrapper from its underlying node socket for a transport upgrade (STARTTLS): all
   * event listeners are removed and any pre-read bytes are drained and returned so the new layer can
   * `unshift` them back onto the stream. This wrapper must not be used afterwards.
   *
   * @return the raw node socket and the bytes read ahead of the upgrade point
   */
  private[net] def detachForUpgrade(): (js.Dynamic, Array[Byte]) =
    sock.removeAllListeners("data")
    sock.removeAllListeners("error")
    sock.removeAllListeners("end")
    (sock, buffer.drainPending())

  override def write(bytes: Array[Byte]): Fx[Unit] = Fx.async { cb =>
    val u8 = new Uint8Array(bytes.length)
    var i  = 0
    while i < bytes.length do { u8(i) = (bytes(i) & 0xff).toShort; i += 1 }
    sock.write(u8, ((() => cb(Right(())))): js.Function0[Unit])
    Fx.Canceler.noop
  }

  override def close(): Fx[Unit] = Fx.delay { sock.end(); buffer.onClose(); () }
