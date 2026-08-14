/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.ConcurrentHashMap

import scala.scalanative.linux.epoll.*
import scala.scalanative.linux.eventfd.{ eventfd, EFD_NONBLOCK }
import scala.scalanative.posix.stdint.{ uint32_t, uint64_t }
import scala.scalanative.posix.unistd.{ read as cRead, write as cWrite }
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/**
 * Linux readiness multiplexer using `epoll`. Each fd is registered once and armed one-shot with
 * `EPOLLONESHOT`, so after one event it is disabled until re-armed via `EPOLL_CTL_MOD` (matching the
 * JVM selector's one-shot behaviour). Wakeup uses an `eventfd` registered persistently.
 */
private[net] final class EpollPoller extends Poller:

  private val epfd             = epoll_create1(0)
  private val wakeFd           = eventfd(0.toUInt, EFD_NONBLOCK)
  private val maxEvents        = 64
  private val evSize           = scalanative_epoll_event_size().toInt
  private val eventList: Ptr[Byte] = malloc(evSize * maxEvents)
  private val registered       = ConcurrentHashMap.newKeySet[Int]()

  locally {
    ctl(EPOLL_CTL_ADD, wakeFd, EPOLLIN.toUInt, wakeFd)
  }

  override def add(fd: Int): Unit =
    if registered.add(fd) then ctl(EPOLL_CTL_ADD, fd, EPOLLONESHOT.toUInt, fd)

  override def arm(fd: Int, read: Boolean, write: Boolean): Unit =
    var events = EPOLLONESHOT.toUInt
    if read then events |= EPOLLIN.toUInt
    if write then events |= EPOLLOUT.toUInt
    if registered.contains(fd) then ctl(EPOLL_CTL_MOD, fd, events, fd)
    else { registered.add(fd); ctl(EPOLL_CTL_ADD, fd, events, fd) }

  override def remove(fd: Int): Unit =
    if registered.remove(fd) then ctl(EPOLL_CTL_DEL, fd, 0.toUInt, fd)

  override def poll(onEvent: (Int, Boolean, Boolean, Boolean) => Unit): Unit =
    val n = epoll_wait(epfd, eventList.asInstanceOf[CVoidPtr], maxEvents, -1)
    var i = 0
    while i < n do
      val events = stackalloc[uint32_t]()
      val data   = stackalloc[uint64_t]()
      scalanative_epoll_event_get(eventList.asInstanceOf[CVoidPtr], i, events, data)
      val fd  = (!data).toInt
      val ev  = (!events).toInt
      if fd == wakeFd then drainWake()
      else
        val readable = (ev & EPOLLIN.toInt) != 0
        val writable = (ev & EPOLLOUT.toInt) != 0
        val error    = (ev & (EPOLLERR.toInt | EPOLLHUP.toInt)) != 0
        onEvent(fd, readable, writable, error)
      i += 1

  override def wakeup(): Unit =
    val one = stackalloc[uint64_t]()
    !one = 1.toULong
    cWrite(wakeFd, one.asInstanceOf[CVoidPtr], 8.toUSize)
    ()

  private def drainWake(): Unit =
    val buf = stackalloc[uint64_t]()
    cRead(wakeFd, buf.asInstanceOf[CVoidPtr], 8.toUSize)
    ()

  private def ctl(op: Int, fd: Int, events: UInt, data: Int): Unit =
    val ev = malloc(evSize)
    scalanative_epoll_event_set(ev.asInstanceOf[CVoidPtr], 0, events, data.toULong)
    epoll_ctl(epfd, op, fd, ev.asInstanceOf[CVoidPtr])
    free(ev)

  private def malloc(size: Int): Ptr[Byte] =
    scala.scalanative.libc.stdlib.malloc(size.toUSize).asInstanceOf[Ptr[Byte]]

  private def free(p: Ptr[Byte]): Unit =
    scala.scalanative.libc.stdlib.free(p.asInstanceOf[CVoidPtr])
