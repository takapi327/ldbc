/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

class ConcurrentOperationTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(60, TimeUnit.SECONDS)

  private val engine = PlatformRawEngine.global

  private def silentPeer(): (Int, AtomicReference[java.net.Socket]) =
    val server   = new ServerSocket(0)
    val accepted = new AtomicReference[java.net.Socket](null)
    val thread   = new Thread(() =>
      try accepted.set(server.accept())
      catch { case _: Throwable => () }
    )
    thread.setDaemon(true)
    thread.start()
    (server.getLocalPort, accepted)

  private def awaitAccepted(accepted: AtomicReference[java.net.Socket]): java.net.Socket =
    var waited = 0
    while accepted.get() == null && waited < 10000 do { Thread.sleep(25); waited += 25 }
    val socket = accepted.get()
    assert(socket != null, "the server never accepted a connection")
    socket

  private def connectRaw(port: Int): RawSocket =
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, SocketOptions.default, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect timed out")
    ref.get().fold(e => fail(s"connect failed: $e"), identity)

  test("a second read while one is parked does not orphan the first callback"):
    val (port, accepted) = silentPeer()
    val client           = connectRaw(port)
    awaitAccepted(accepted)

    val firstSettled  = new CountDownLatch(1)
    val secondSettled = new CountDownLatch(1)
    val firstResult   = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    val secondResult  = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)

    client.read(16, r => { firstResult.set(r); firstSettled.countDown() })
    Thread.sleep(200)
    client.read(16, r => { secondResult.set(r); secondSettled.countDown() })
    Thread.sleep(200)

    client.close()

    assert(secondSettled.await(5, TimeUnit.SECONDS), "the second read was never settled")
    assert(
      firstSettled.await(5, TimeUnit.SECONDS),
      "the first read was orphaned by the second one and never settled"
    )
    assert(firstResult.get().isLeft, s"the first read should have failed, got ${ firstResult.get() }")
    assert(secondResult.get().isLeft, s"the second read should have failed, got ${ secondResult.get() }")

  test("a second write while one is parked does not orphan the first callback"):
    val (port, accepted) = silentPeer()
    val client           = connectRaw(port)
    awaitAccepted(accepted)

    val size          = 64 * 1024 * 1024
    val firstSettled  = new CountDownLatch(1)
    val secondSettled = new CountDownLatch(1)
    val firstResult   = new AtomicReference[Either[Throwable, Unit]](null)
    val secondResult  = new AtomicReference[Either[Throwable, Unit]](null)

    client.write(new Array[Byte](size), r => { firstResult.set(r); firstSettled.countDown() })
    Thread.sleep(500)
    assertEquals(firstSettled.getCount, 1L, "the payload was too small to force a partial write")

    client.write(new Array[Byte](16), r => { secondResult.set(r); secondSettled.countDown() })
    Thread.sleep(200)

    client.close()

    assert(secondSettled.await(5, TimeUnit.SECONDS), "the second write was never settled")
    assert(
      firstSettled.await(5, TimeUnit.SECONDS),
      "the first write was orphaned by the second one and never settled"
    )
