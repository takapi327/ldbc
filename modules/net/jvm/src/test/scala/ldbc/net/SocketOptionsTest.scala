/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.{ ServerSocket, StandardSocketOptions }
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

class SocketOptionsTest extends munit.FunSuite:

  private val engine = PlatformRawEngine.global

  private def acceptingServer(): Int =
    val server = new ServerSocket(0)
    val thread = new Thread(() =>
      try while true do server.accept()
      catch { case _: Throwable => () }
    )
    thread.setDaemon(true)
    thread.start()
    server.getLocalPort

  private lazy val port = acceptingServer()

  private def connectWith(options: SocketOptions): NioRawSocket =
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, options, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect がタイムアウトした")
    ref.get().fold(e => fail(s"connect 失敗: $e"), _.asInstanceOf[NioRawSocket])

  test("TCP_NODELAY and SO_KEEPALIVE follow the given options"):
    val on  = connectWith(SocketOptions(noDelay = true, keepAlive = true))
    val off = connectWith(SocketOptions(noDelay = false, keepAlive = false))
    try
      assertEquals(on.channel.getOption(StandardSocketOptions.TCP_NODELAY), java.lang.Boolean.TRUE)
      assertEquals(on.channel.getOption(StandardSocketOptions.SO_KEEPALIVE), java.lang.Boolean.TRUE)
      assertEquals(off.channel.getOption(StandardSocketOptions.TCP_NODELAY), java.lang.Boolean.FALSE)
      assertEquals(off.channel.getOption(StandardSocketOptions.SO_KEEPALIVE), java.lang.Boolean.FALSE)
    finally
      on.close()
      off.close()

  test("sendBufferSize and receiveBufferSize reach the socket"):
    val requested = 1024 * 1024
    val control   = connectWith(SocketOptions.default)
    val tuned     = connectWith(
      SocketOptions(sendBufferSize = Some(requested), receiveBufferSize = Some(requested))
    )
    try
      val controlSend = control.channel.getOption(StandardSocketOptions.SO_SNDBUF).intValue
      val controlRecv = control.channel.getOption(StandardSocketOptions.SO_RCVBUF).intValue
      val tunedSend   = tuned.channel.getOption(StandardSocketOptions.SO_SNDBUF).intValue
      val tunedRecv   = tuned.channel.getOption(StandardSocketOptions.SO_RCVBUF).intValue
      assert(
        tunedSend > controlSend,
        s"sendBufferSize がソケットに届いていない（既定 $controlSend / 指定後 $tunedSend）"
      )
      assert(
        tunedRecv > controlRecv,
        s"receiveBufferSize がソケットに届いていない（既定 $controlRecv / 指定後 $tunedRecv）"
      )
    finally
      control.close()
      tuned.close()

  test("leaving the buffer sizes unset does not touch them"):
    val control = connectWith(SocketOptions.default)
    try
      assert(control.channel.getOption(StandardSocketOptions.SO_SNDBUF).intValue > 0)
      assert(control.channel.getOption(StandardSocketOptions.SO_RCVBUF).intValue > 0)
    finally control.close()
