/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

class PollerInterruptTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(60, TimeUnit.SECONDS)

  private def await(cond: () => Boolean, millis: Int): Boolean =
    var waited = 0
    while !cond() && waited < millis do { Thread.sleep(25); waited += 25 }
    cond()

  private def pollerThread(name: String): Option[Thread] =
    Thread.getAllStackTraces.keySet.stream
      .filter(t => t.getName == name)
      .findFirst()
      .map[Option[Thread]](t => Some(t))
      .orElse(None)

  test("interrupting the poller hands over instead of spinning"):
    val engine = NioRawEngine.start("ldbc-net-interrupt-test")
    assert(await(() => pollerThread(engine.pollerThreadName).isDefined, 5000), "the poller thread never started")

    val first = engine.pollerThreadName
    pollerThread(first).getOrElse(fail("the poller thread disappeared")).interrupt()

    assert(await(() => engine.pollerThreadName != first, 10000), "the interrupt did not trigger a handover")
    assert(!engine.isTerminated, "a single interrupt terminated the engine")

    val settledCount = engine.loopErrorCount
    Thread.sleep(1500)
    assertEquals(
      engine.loopErrorCount,
      settledCount,
      "the replacement thread is looping on errors, which is what a restored interrupt flag would cause"
    )

    val server   = new ServerSocket(0)
    val accepted = new AtomicReference[java.net.Socket](null)
    val acceptor = new Thread(() =>
      try accepted.set(server.accept())
      catch { case _: Throwable => () }
    )
    acceptor.setDaemon(true)
    acceptor.start()

    val connected = new CountDownLatch(1)
    val ref       = new AtomicReference[Either[Throwable, RawSocket]](null)
    engine.connect("127.0.0.1", server.getLocalPort, SocketOptions.default, r => { ref.set(r); connected.countDown() })
    assert(connected.await(10, TimeUnit.SECONDS), "the engine stopped accepting connections after the interrupt")
    val client = ref.get().fold(e => fail(s"connect failed: $e"), identity)

    assert(await(() => accepted.get() != null, 10000), "the server never accepted")
    val peer = accepted.get()

    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    client.read(4, r => { outcome.set(r); settled.countDown() })
    peer.getOutputStream.write("OKAY".getBytes("UTF-8"))
    peer.getOutputStream.flush()

    assert(settled.await(10, TimeUnit.SECONDS), "the replacement thread does not serve reads")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("OKAY")))
    client.close()
    server.close()
