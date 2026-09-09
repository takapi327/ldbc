/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.scalanative.bsd.kevent.*
import scala.scalanative.posix.stdint.{ int16_t, intptr_t, uint16_t, uint32_t, uintptr_t }
import scala.scalanative.posix.unistd.{ pipe, read as cRead, write as cWrite }
import scala.scalanative.runtime.Intrinsics
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/**
 * macOS/BSD readiness multiplexer using `kqueue`. Each direction is armed with `EV_ADD | EV_ONESHOT`
 * so it auto-removes after one event (one-shot, matching the JVM selector). Wakeup uses a self-pipe
 * whose read end is registered persistently.
 */
private[net] final class KqueuePoller extends Poller:

  private val kq        = kqueue()
  private val wakePipe  = Array.fill(2)(0)
  private val maxEvents = 64
  private val evSize    = scalanative_kevent_size().toInt
  private val eventList: Ptr[Byte] = stdlibMalloc(evSize * maxEvents)

  locally {
    val fds = stackalloc[CInt](2)
    pipe(fds)
    wakePipe(0) = fds(0)
    wakePipe(1) = fds(1)
    registerWake()
  }

  override def add(fd: Int): Unit = ()

  override def arm(fd: Int, read: Boolean, write: Boolean): Unit =
    if read then change(fd, EVFILT_READ, EV_ADD | EV_ONESHOT)
    if write then change(fd, EVFILT_WRITE, EV_ADD | EV_ONESHOT)

  override def remove(fd: Int): Unit =
    change(fd, EVFILT_READ, EV_DELETE)
    change(fd, EVFILT_WRITE, EV_DELETE)

  override def poll(onEvent: (Int, Boolean, Boolean, Boolean) => Unit): Unit =
    val n = kevent(kq, null, 0, eventList.asInstanceOf[CVoidPtr], maxEvents, null)
    var i = 0
    while i < n do
      val ident  = stackalloc[uintptr_t]()
      val filter = stackalloc[int16_t]()
      val flags  = stackalloc[uint16_t]()
      val fflags = stackalloc[uint32_t]()
      val data   = stackalloc[intptr_t]()
      val udata  = stackalloc[CVoidPtr]()
      scalanative_kevent_get(eventList.asInstanceOf[CVoidPtr], i, ident, filter, flags, fflags, data, udata)
      val fd    = (!ident).toInt
      val filt  = (!filter).toInt
      val isEof = ((!flags).toInt & EV_EOF) != 0
      val isErr = ((!flags).toInt & EV_ERROR) != 0
      if fd == wakePipe(0) then drainWake()
      else
        val readable = filt == EVFILT_READ
        val writable = filt == EVFILT_WRITE
        onEvent(fd, readable, writable, isErr || isEof)
      i += 1

  override def wakeup(): Unit =
    val one = stackalloc[Byte]()
    !one = 1.toByte
    cWrite(wakePipe(1), one.asInstanceOf[CVoidPtr], 1.toUSize)
    ()

  private def registerWake(): Unit =
    change(wakePipe(0), EVFILT_READ, EV_ADD)

  private def drainWake(): Unit =
    val buf = stackalloc[Byte](64)
    cRead(wakePipe(0), buf.asInstanceOf[CVoidPtr], 64.toUSize)
    ()

  /** Applies a single kevent change immediately (changelist of one, no wait). */
  private def change(fd: Int, filter: Int, flags: Int): Unit =
    val chg = stdlibMalloc(evSize)
    scalanative_kevent_set(
      chg.asInstanceOf[CVoidPtr],
      0,
      fd.toUSize,
      filter.toShort,
      flags.toUShort,
      0.toUInt,
      Size.valueOf(Intrinsics.castLongToRawSize(0L)),
      null
    )
    kevent(kq, chg.asInstanceOf[CVoidPtr], 1, null, 0, null)
    stdlibFree(chg)

  private def stdlibMalloc(size: Int): Ptr[Byte] =
    scala.scalanative.libc.stdlib.malloc(size.toUSize).asInstanceOf[Ptr[Byte]]

  private def stdlibFree(p: Ptr[Byte]): Unit =
    scala.scalanative.libc.stdlib.free(p.asInstanceOf[CVoidPtr])
