/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.StandardSocketOptions
import java.nio.channels.SocketChannel

class SocketOptionsTest extends munit.FunSuite:

  private def applied[A](options: SocketOptions)(read: SocketChannel => A): A =
    val ch = SocketChannel.open()
    try
      NioRawEngine.withOptions(ch, options)
      read(ch)
    finally ch.close()

  test("TCP_NODELAY and SO_KEEPALIVE follow the given options"):
    applied(SocketOptions(noDelay = true, keepAlive = true)): ch =>
      assertEquals(ch.getOption(StandardSocketOptions.TCP_NODELAY), java.lang.Boolean.TRUE)
      assertEquals(ch.getOption(StandardSocketOptions.SO_KEEPALIVE), java.lang.Boolean.TRUE)
    applied(SocketOptions(noDelay = false, keepAlive = false)): ch =>
      assertEquals(ch.getOption(StandardSocketOptions.TCP_NODELAY), java.lang.Boolean.FALSE)
      assertEquals(ch.getOption(StandardSocketOptions.SO_KEEPALIVE), java.lang.Boolean.FALSE)

  test("sendBufferSize and receiveBufferSize reach the socket"):
    val requested = 1024 * 1024
    val default   = applied(SocketOptions.default): ch =>
      (
        ch.getOption(StandardSocketOptions.SO_SNDBUF).intValue,
        ch.getOption(StandardSocketOptions.SO_RCVBUF).intValue
      )
    applied(SocketOptions(sendBufferSize = Some(requested), receiveBufferSize = Some(requested))): ch =>
      val send = ch.getOption(StandardSocketOptions.SO_SNDBUF).intValue
      val recv = ch.getOption(StandardSocketOptions.SO_RCVBUF).intValue
      assert(send > default._1, s"sendBufferSize がソケットに届いていない（既定 ${ default._1 } / 指定後 $send）")
      assert(recv > default._2, s"receiveBufferSize がソケットに届いていない（既定 ${ default._2 } / 指定後 $recv）")

  test("leaving the buffer sizes unset keeps the platform defaults"):
    val untouched = SocketChannel.open()
    try
      val send = untouched.getOption(StandardSocketOptions.SO_SNDBUF)
      val recv = untouched.getOption(StandardSocketOptions.SO_RCVBUF)
      applied(SocketOptions.default): ch =>
        assertEquals(ch.getOption(StandardSocketOptions.SO_SNDBUF), send)
        assertEquals(ch.getOption(StandardSocketOptions.SO_RCVBUF), recv)
    finally untouched.close()
