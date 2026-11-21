/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

import scala.scalanative.runtime.{ fromRawPtr, toRawPtr, Intrinsics }
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

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
