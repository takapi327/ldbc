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

class PollerLifecycleTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(120, TimeUnit.SECONDS)

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

  private def connectRaw(engine: NioRawEngine, port: Int): RawSocket =
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, SocketOptions.default, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect timed out")
    ref.get().fold(e => fail(s"connect failed: $e"), identity)

  private def await(cond: () => Boolean, millis: Int): Boolean =
    var waited = 0
    while !cond() && waited < millis do { Thread.sleep(25); waited += 25 }
    cond()

  test("a recoverable error is reported and the loop keeps running"):
    val engine           = NioRawEngine.start()
    val (port, accepted) = acceptOne()
    val thrown           = new AtomicInteger(0)
    engine.injectFault(() => if thrown.getAndIncrement() == 0 then throw new IllegalStateException("boom"))

    val client = connectRaw(engine, port)
    val server = awaitAccepted(accepted)

    assert(await(() => engine.loopErrorCount >= 1L, 10000), "the swallowed error was not counted")
    assert(!engine.isTerminated, "a recoverable error terminated the engine")

    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    client.read(4, r => { outcome.set(r); settled.countDown() })
    server.getOutputStream.write("ABCD".getBytes("UTF-8"))
    server.getOutputStream.flush()

    assert(settled.await(15, TimeUnit.SECONDS), "the loop stopped serving after a recoverable error")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("ABCD")))
    engine.injectFault(null)
    client.close()

  test("a fatal error hands over to a replacement and in-flight work survives"):
    val engine           = NioRawEngine.start()
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    val server           = awaitAccepted(accepted)

    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    client.read(4, r => { outcome.set(r); settled.countDown() })
    Thread.sleep(300)

    val thrown = new AtomicInteger(0)
    engine.injectFault { () =>
      if thrown.getAndIncrement() == 0 then
        engine.injectFault(null)
        throw new StackOverflowError("fatal")
    }

    assert(
      await(() => Thread.getAllStackTraces.keySet.stream.anyMatch(_.getName == "ldbc-net-nio-raw-2"), 15000),
      "no replacement thread was started"
    )
    assert(!engine.isTerminated, "a single fatal error terminated the engine")

    server.getOutputStream.write("WXYZ".getBytes("UTF-8"))
    server.getOutputStream.flush()
    assert(settled.await(15, TimeUnit.SECONDS), "the parked read did not survive the handover")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("WXYZ")))
    client.close()

  test("repeated startup failures give up and settle every socket"):
    val engine           = NioRawEngine.start()
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    awaitAccepted(accepted)

    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    client.read(4, r => { outcome.set(r); settled.countDown() })
    Thread.sleep(300)

    engine.injectFault(() => throw new StackOverflowError("always fatal"))

    assert(settled.await(20, TimeUnit.SECONDS), "giving up left a parked read unsettled")
    outcome.get() match
      case Left(_: IOException) => ()
      case other                => fail(s"expected an IOException, got $other")
    assert(await(() => engine.isTerminated, 5000), "the engine did not mark itself terminated")

  test("a terminated engine is brought back up by the next connect"):
    val engine = NioRawEngine.start()
    engine.injectFault(() => throw new StackOverflowError("always fatal"))
    assert(await(() => engine.isTerminated, 20000), "the engine did not terminate")

    engine.injectFault(null)
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    val server           = awaitAccepted(accepted)

    assertEquals(engine.revivalCount, 1, "the engine was not revived")
    assert(!engine.isTerminated, "the engine is still marked terminated after a revival")

    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    client.read(4, r => { outcome.set(r); settled.countDown() })
    server.getOutputStream.write("LIVE".getBytes("UTF-8"))
    server.getOutputStream.flush()

    assert(settled.await(15, TimeUnit.SECONDS), "the revived engine does not serve reads")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("LIVE")))
    client.close()
