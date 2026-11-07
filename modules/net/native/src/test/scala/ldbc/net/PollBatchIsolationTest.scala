/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, AtomicReference }

class PollBatchIsolationTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(60, TimeUnit.SECONDS)

  private def connectRaw(engine: RawIoEngine, port: Int): RawSocket =
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, SocketOptions.default, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect timed out")
    ref.get().fold(e => fail(s"connect failed: $e"), identity)

  test("one failing dispatch does not drop the rest of the poll batch"):
    val engine = PlatformRawEngine.startIsolated("ldbc-net-batch-test")

    val server   = new ServerSocket(0)
    val peers    = new AtomicReference[List[java.net.Socket]](Nil)
    val acceptor = new Thread(() =>
      try
        while true do
          val peer = server.accept()
          peers.updateAndGet(peer :: _)
      catch { case _: Throwable => () }
    )
    acceptor.setDaemon(true)
    acceptor.start()

    val first  = connectRaw(engine, server.getLocalPort)
    val second = connectRaw(engine, server.getLocalPort)

    var waited = 0
    while peers.get().size < 2 && waited < 10000 do { Thread.sleep(25); waited += 25 }
    assertEquals(peers.get().size, 2, "the server did not accept both connections")

    val completed = new AtomicInteger(0)
    first.read(4, _ => completed.incrementAndGet())
    second.read(4, _ => completed.incrementAndGet())
    Thread.sleep(400)
    assertEquals(completed.get(), 0, "a read completed before any data was sent")

    val slept = new AtomicBoolean(false)
    engine.injectFault { () =>
      if slept.compareAndSet(false, true) then Thread.sleep(700)
    }

    val thrown = new AtomicBoolean(false)
    engine.injectDispatchFault { () =>
      if thrown.compareAndSet(false, true) then throw new IllegalStateException("dispatch blew up")
    }

    Thread.sleep(150)
    peers.get().foreach { peer =>
      peer.getOutputStream.write("DATA".getBytes("UTF-8"))
      peer.getOutputStream.flush()
    }

    waited = 0
    while completed.get() == 0 && waited < 5000 do { Thread.sleep(50); waited += 50 }
    val settledWhileOpen = completed.get()

    engine.injectFault(null)
    engine.injectDispatchFault(null)
    first.close()
    second.close()
    server.close()

    assert(thrown.get(), "the dispatch fault never fired, so this test proved nothing")
    assert(
      settledWhileOpen >= 1,
      "a single failing dispatch took the rest of the poll batch with it: neither read ever completed"
    )
