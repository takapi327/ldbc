/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

class PendingCallbackTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(60, TimeUnit.SECONDS)

  private val engine = PlatformRawEngine.global

  private def acceptOne(): (Int, AtomicReference[java.net.Socket]) =
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

  test("closing while a read is parked settles it with a failure"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    awaitAccepted(accepted)

    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    client.read(16, r => { outcome.set(r); settled.countDown() })
    Thread.sleep(300)
    assertEquals(settled.getCount, 1L, "the read completed even though no data had arrived")

    client.close()

    assert(settled.await(5, TimeUnit.SECONDS), "close left the parked read unsettled")
    outcome.get() match
      case Left(e: IOException) => ()
      case other                => fail(s"expected an IOException, got $other")

  test("closing while a write is parked settles it with a failure"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    awaitAccepted(accepted)

    val size    = 64 * 1024 * 1024
    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Unit]](null)
    client.write(new Array[Byte](size), r => { outcome.set(r); settled.countDown() })
    Thread.sleep(500)
    assertEquals(settled.getCount, 1L, "the payload was too small to force a partial write")

    client.close()

    assert(settled.await(5, TimeUnit.SECONDS), "close left the parked write unsettled")
    outcome.get() match
      case Left(e: IOException) => ()
      case other                => fail(s"expected an IOException, got $other")

  test("a settled read is not settled again by close"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    val server           = awaitAccepted(accepted)

    val calls   = new AtomicInteger(0)
    val settled = new CountDownLatch(1)
    client.read(4, _ => { calls.incrementAndGet(); settled.countDown() })
    server.getOutputStream.write("ABCD".getBytes("UTF-8"))
    server.getOutputStream.flush()
    assert(settled.await(5, TimeUnit.SECONDS), "the read never completed")

    client.close()
    Thread.sleep(300)
    assertEquals(calls.get(), 1, "the callback was invoked more than once")

  test("a cancelled read is not settled again by close"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    awaitAccepted(accepted)

    val calls    = new AtomicInteger(0)
    val canceler = client.read(16, _ => { calls.incrementAndGet(); () })
    Thread.sleep(200)
    canceler.cancel()
    client.close()
    Thread.sleep(300)
    assertEquals(calls.get(), 0, "the callback of a cancelled read was invoked")
