/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.scalanative.libc.errno.errno
import scala.scalanative.libc.signal.{ signal, SIG_IGN }
import scala.scalanative.meta.LinktimeInfo
import scala.scalanative.posix.fcntl.{ fcntl, F_GETFL, F_SETFL, O_NONBLOCK }
import scala.scalanative.posix.netdb.{ addrinfo, freeaddrinfo, getaddrinfo }
import scala.scalanative.posix.netinet.in.IPPROTO_TCP
import scala.scalanative.posix.netinet.tcp.TCP_NODELAY
import scala.scalanative.posix.signal.SIGPIPE
import scala.scalanative.posix.sys.socket.{
  connect as cConnect,
  getsockopt,
  recv,
  send,
  setsockopt,
  sockaddr,
  socket,
  socklen_t,
  SOCK_STREAM,
  SOL_SOCKET,
  SO_ERROR,
  SO_KEEPALIVE,
  SO_RCVBUF,
  SO_SNDBUF
}
import scala.scalanative.posix.unistd.close as cClose
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/**
 * A resolved connection target: an already-opened non-blocking `fd` plus the resolved socket
 * address to hand to `connect(2)`. The `addrinfo` result is retained so it can be freed after the
 * connect attempt.
 */
private[net] final class Resolved(val fd: Int, private[net] val info: Ptr[addrinfo]):
  /**
   * `ai_addr`. Scala Native's `addrinfo` CStruct uses the Linux field order (`ai_addr` before
   * `ai_canonname`), but macOS/BSD place `ai_canonname` first, so `ai_addr` is one field later there.
   */
  private[net] def addr: Ptr[sockaddr] =
    if LinktimeInfo.isMac then info._7.asInstanceOf[Ptr[sockaddr]] else info._6
  private[net] def addrLen: socklen_t = info._5
  private[net] def free():  Unit      = freeaddrinfo(info)

/** Thin POSIX interop for the Native [[NativeIoEngine]] (socket, getaddrinfo, recv/send, SIGPIPE). */
private[net] object CInterop:

  /** Ignores SIGPIPE process-wide so a `send` to a closed peer returns EPIPE instead of killing us. */
  def ignoreSigpipe(): Unit =
    signal(SIGPIPE, SIG_IGN)
    ()

  /**
   * Resolves `host`/`port` via blocking `getaddrinfo`, opens a non-blocking TCP socket, and returns
   * the pair. IP literals resolve without contacting DNS. Throws on resolution/socket failure.
   */
  def resolve(host: String, port: Int): Resolved = Zone {
    val hints = alloc[addrinfo]()
    hints._2 = 0           // ai_family = AF_UNSPEC
    hints._3 = SOCK_STREAM // ai_socktype
    val res = alloc[Ptr[addrinfo]]()
    val rc  = getaddrinfo(toCString(host), toCString(port.toString), hints, res)
    if rc != 0 then throw new java.io.IOException(s"getaddrinfo($host:$port) failed (code=$rc)")
    val info = !res
    val fd   = socket(info._2, info._3, info._4)
    if fd < 0 then
      freeaddrinfo(info)
      throw new java.io.IOException(s"socket() failed for $host:$port")
    setNonBlocking(fd)
    new Resolved(fd, info)
  }

  /**
   * Initiates the non-blocking connect and frees the resolved address. Returns `0` if the
   * connection completed immediately, otherwise the `errno` captured right after `connect` (before
   * `freeaddrinfo`, which would clobber it) — typically `EINPROGRESS`.
   */
  def beginConnect(fd: Int, resolved: Resolved): Int =
    val rc  = cConnect(fd, resolved.addr, resolved.addrLen)
    val err = if rc == 0 then 0 else errno
    resolved.free()
    err

  /** Applies the curated [[SocketOptions]] to `fd` via `setsockopt`. */
  def applyOptions(fd: Int, options: SocketOptions): Unit =
    def setInt(level: CInt, name: CInt, value: Int): Unit =
      val v = stackalloc[CInt]()
      !v = value
      setsockopt(fd, level, name, v.asInstanceOf[Ptr[Byte]], sizeof[CInt].toUInt)
      ()
    setInt(IPPROTO_TCP, TCP_NODELAY, if options.noDelay then 1 else 0)
    if options.keepAlive then setInt(SOL_SOCKET, SO_KEEPALIVE, 1)
    options.sendBufferSize.foreach(size => setInt(SOL_SOCKET, SO_SNDBUF, size))
    options.receiveBufferSize.foreach(size => setInt(SOL_SOCKET, SO_RCVBUF, size))

  /** Reads `SO_ERROR`, the pending socket error set after a non-blocking connect completes. */
  def socketError(fd: Int): Int =
    val value = stackalloc[CInt]()
    val len   = stackalloc[socklen_t]()
    !len = sizeof[CInt].toUInt
    getsockopt(fd, SOL_SOCKET, SO_ERROR, value.asInstanceOf[Ptr[Byte]], len)
    !value

  /** Non-blocking `recv` into `buf[0, n)`; returns bytes read, 0 on EOF, or -1 (check errno). */
  def recvInto(fd: Int, buf: Array[Byte], n: Int): Int =
    val r = recv(fd, buf.at(0), n.toUSize, 0)
    r.toInt

  /** Non-blocking `send` of `bytes[off, off+len)`; returns bytes written or -1 (check errno). */
  def sendFrom(fd: Int, bytes: Array[Byte], off: Int, len: Int): Int =
    val w = send(fd, bytes.at(off), len.toUSize, 0)
    w.toInt

  /** Closes a raw fd, ignoring errors. */
  def closeFd(fd: Int): Unit =
    cClose(fd)
    ()

  /** Puts `fd` into non-blocking mode via `fcntl(F_SETFL, O_NONBLOCK)`. */
  private def setNonBlocking(fd: Int): Unit =
    val flags = fcntl(fd, F_GETFL, 0)
    fcntl(fd, F_SETFL, flags | O_NONBLOCK)
    ()

  /** Clears the non-blocking flag on `fd` (used by the TLS layer to drive s2n synchronously). */
  def setBlocking(fd: Int): Unit =
    val flags = fcntl(fd, F_GETFL, 0)
    fcntl(fd, F_SETFL, flags & ~O_NONBLOCK)
    ()

  /** One `recv` into `buf[0, n)` on the (non-blocking) fd; bytes read, 0 on EOF, or -1 (errno set). */
  def recvOnce(fd: Int, buf: Array[Byte], n: Int): Int = recvInto(fd, buf, n)

  /** One `send` of `bytes[0, len)` on the (non-blocking) fd; bytes written or -1 (errno set). */
  def sendOnce(fd: Int, bytes: Array[Byte], len: Int): Int = sendFrom(fd, bytes, 0, len)
