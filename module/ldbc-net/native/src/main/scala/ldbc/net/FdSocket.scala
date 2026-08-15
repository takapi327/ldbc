/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.atomic.AtomicBoolean

import scala.scalanative.libc.errno.errno
import scala.scalanative.posix.errno.{ EAGAIN, EWOULDBLOCK }

import ldbc.fx.Fx

/** A [[Socket]] over a raw non-blocking fd, driven by [[NativeIoEngine]]'s poller. */
private[net] final class FdSocket(fd: Int, st: ChannelState, engine: NativeIoEngine) extends Socket:

  private val closed = new AtomicBoolean(false)

  /** The raw fd, used by the Native TLS layer to drive s2n directly. */
  private[net] def fileDescriptor: Int = fd

  /** The per-fd interest/continuation state, used by the Native TLS layer to await readiness. */
  private[net] def channelState: ChannelState = st

  /** The owning engine, used by the Native TLS layer to arm read/write readiness on the poller. */
  private[net] def ioEngine: NativeIoEngine = engine

  override def read(n: Int): Fx[Option[Array[Byte]]] =
    if n <= 0 then Fx.pure(Some(Array.emptyByteArray))
    else
      Fx.async { cb =>
        def attempt(): Unit =
          val buf = new Array[Byte](n)
          val r   = CInterop.recvInto(fd, buf, n)
          if r > 0 then cb(Right(Some(java.util.Arrays.copyOf(buf, r))))
          else if r == 0 then cb(Right(None))
          else if errno == EAGAIN || errno == EWOULDBLOCK then engine.armRead(fd, st, () => attempt())
          else cb(Left(new java.io.IOException(s"read failed (errno=$errno)")))
        attempt()
        new Fx.Canceler { override def cancel(): Unit = st.readReady = null }
      }

  override def write(bytes: Array[Byte]): Fx[Unit] =
    Fx.async { cb =>
      val off = new java.util.concurrent.atomic.AtomicInteger(0)
      def attempt(): Unit =
        var blocked = false
        while off.get() < bytes.length && !blocked do
          val w = CInterop.sendFrom(fd, bytes, off.get(), bytes.length - off.get())
          if w > 0 then off.addAndGet(w)
          else if w < 0 && (errno == EAGAIN || errno == EWOULDBLOCK) then
            engine.armWrite(fd, st, () => attempt()); blocked = true
          else { cb(Left(new java.io.IOException(s"write failed (errno=$errno)"))); return }
        if !blocked && off.get() >= bytes.length then cb(Right(()))
      attempt()
      new Fx.Canceler { override def cancel(): Unit = st.writeReady = null }
    }

  override def close(): Fx[Unit] = Fx.delay {
    if closed.compareAndSet(false, true) then
      engine.deregister(fd)
      CInterop.closeFd(fd)
  }
