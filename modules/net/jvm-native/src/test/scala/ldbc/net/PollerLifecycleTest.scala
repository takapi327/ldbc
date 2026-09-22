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

  private val names = new AtomicInteger(0)

  private def isolated(): RawIoEngine & PollerDiagnostics =
    PlatformRawEngine.startIsolated(s"ldbc-net-test-${ names.incrementAndGet() }")

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

  private def connectRaw(engine: RawIoEngine, port: Int): RawSocket =
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, SocketOptions.default, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect timed out")
    ref.get().fold(e => fail(s"connect failed: $e"), identity)

  private def await(cond: () => Boolean, millis: Int): Boolean =
    var waited = 0
    while !cond() && waited < millis do { Thread.sleep(25); waited += 25 }
    cond()

  private def parkedRead(
    socket: RawSocket,
    n:      Int
  ): (CountDownLatch, AtomicReference[Either[Throwable, Option[Array[Byte]]]]) =
    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Option[Array[Byte]]]](null)
    socket.read(n, r => { outcome.set(r); settled.countDown() })
    (settled, outcome)

  test("a recoverable error is reported and the loop keeps running"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val thrown           = new AtomicInteger(0)
    engine.injectFault(() => if thrown.getAndIncrement() == 0 then throw new IllegalStateException("boom"))

    val client = connectRaw(engine, port)
    val server = awaitAccepted(accepted)

    assert(await(() => engine.loopErrorCount >= 1L, 10000), "the swallowed error was not counted")
    assert(!engine.isTerminated, "a recoverable error terminated the engine")

    val (settled, outcome) = parkedRead(client, 4)
    server.getOutputStream.write("ABCD".getBytes("UTF-8"))
    server.getOutputStream.flush()

    assert(settled.await(15, TimeUnit.SECONDS), "the loop stopped serving after a recoverable error")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("ABCD")))
    engine.injectFault(null)
    client.close()

  test("a fatal error hands over to a replacement and in-flight work survives"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    val server           = awaitAccepted(accepted)
    val first            = engine.pollerThreadName

    val (settled, outcome) = parkedRead(client, 4)
    Thread.sleep(300)

    val thrown = new AtomicInteger(0)
    engine.injectFault { () =>
      if thrown.getAndIncrement() == 0 then
        engine.injectFault(null)
        throw new StackOverflowError("fatal")
    }

    assert(await(() => engine.pollerThreadName != first, 15000), "no replacement thread was started")
    assert(!engine.isTerminated, "a single fatal error terminated the engine")

    server.getOutputStream.write("WXYZ".getBytes("UTF-8"))
    server.getOutputStream.flush()
    assert(settled.await(15, TimeUnit.SECONDS), "the parked read did not survive the handover")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("WXYZ")))
    client.close()

  test("a fatal error while dispatching settles the socket being served"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    val server           = awaitAccepted(accepted)

    val (settled, outcome) = parkedRead(client, 4)
    Thread.sleep(300)

    val thrown = new AtomicInteger(0)
    engine.injectDispatchFault { () =>
      if thrown.getAndIncrement() == 0 then
        engine.injectDispatchFault(null)
        throw new StackOverflowError("fatal while dispatching")
    }
    server.getOutputStream.write("ABCD".getBytes("UTF-8"))
    server.getOutputStream.flush()

    assert(
      settled.await(15, TimeUnit.SECONDS),
      "the read being dispatched when the thread died was never settled"
    )
    outcome.get() match
      case Left(_: IOException) => ()
      case other                => fail(s"expected an IOException, got $other")
    client.close()

  test("repeated startup failures give up and settle every socket"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    awaitAccepted(accepted)

    val (settled, outcome) = parkedRead(client, 4)
    Thread.sleep(300)

    engine.injectFault(() => throw new StackOverflowError("always fatal"))

    assert(settled.await(20, TimeUnit.SECONDS), "giving up left a parked read unsettled")
    outcome.get() match
      case Left(_: IOException) => ()
      case other                => fail(s"expected an IOException, got $other")
    assert(await(() => engine.isTerminated, 5000), "the engine did not mark itself terminated")

  test("a callback that throws does not stop the engine from giving up"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val hostile          = connectRaw(engine, port)
    val quiet            = connectRaw(engine, port)
    awaitAccepted(accepted)

    hostile.read(4, _ => throw new IllegalStateException("hostile callback"))
    val (settled, outcome) = parkedRead(quiet, 4)
    Thread.sleep(300)

    engine.injectFault(() => throw new StackOverflowError("always fatal"))

    assert(await(() => engine.isTerminated, 20000), "a throwing callback stopped the engine from terminating")
    assert(settled.await(10, TimeUnit.SECONDS), "a throwing callback swallowed the other socket's settlement")
    outcome.get() match
      case Left(_: IOException) => ()
      case other                => fail(s"expected an IOException, got $other")

  test("a read issued after the engine gave up fails immediately"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    awaitAccepted(accepted)

    engine.injectFault(() => throw new StackOverflowError("always fatal"))
    assert(await(() => engine.isTerminated, 20000), "the engine did not terminate")

    val (settled, outcome) = parkedRead(client, 4)
    assert(settled.await(5, TimeUnit.SECONDS), "a read on a terminated engine parked forever")
    outcome.get() match
      case Left(_: IOException) => ()
      case other                => fail(s"expected an IOException, got $other")

  test("a terminated engine is brought back up by the next connect"):
    val engine = isolated()
    engine.injectFault(() => throw new StackOverflowError("always fatal"))
    assert(await(() => engine.isTerminated, 20000), "the engine did not terminate")

    engine.injectFault(null)
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    val server           = awaitAccepted(accepted)

    assertEquals(engine.revivalCount, 1, "the engine was not revived")
    assert(!engine.isTerminated, "the engine is still marked terminated after a revival")

    val (settled, outcome) = parkedRead(client, 4)
    server.getOutputStream.write("LIVE".getBytes("UTF-8"))
    server.getOutputStream.flush()

    assert(settled.await(15, TimeUnit.SECONDS), "the revived engine does not serve reads")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("LIVE")))
    client.close()

  test("a cancelled connect does not leave a socket behind"):
    val engine = isolated()
    val server = new ServerSocket(0, 1)
    val filler = connectRaw(engine, server.getLocalPort)

    assertEquals(engine.liveSocketCount, 1)

    val canceler = engine.connect("198.51.100.1", 9, SocketOptions.default, _ => ())
    canceler.cancel()

    assert(
      await(() => engine.liveSocketCount == 1, 5000),
      s"a cancelled connect stayed tracked (live=${ engine.liveSocketCount })"
    )
    filler.close()
    assert(await(() => engine.liveSocketCount == 0, 5000), "closing did not untrack the socket")
    server.close()

  test("a connect to an unusable address does not leave a socket behind"):
    val engine  = isolated()
    val settled = new CountDownLatch(1)
    engine.connect("", 0, SocketOptions.default, _ => settled.countDown())
    settled.await(5, TimeUnit.SECONDS)
    assert(
      await(() => engine.liveSocketCount == 0, 5000),
      s"a failed connect stayed tracked (live=${ engine.liveSocketCount })"
    )

  test("a connect still in flight when the engine gives up is settled"):
    val engine  = isolated()
    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, RawSocket]](null)

    engine.connect("198.51.100.1", 9, SocketOptions.default, r => { outcome.set(r); settled.countDown() })
    Thread.sleep(400)
    assertEquals(settled.getCount, 1L, "the connect resolved on its own, so this test proved nothing")

    engine.injectFault(() => throw new StackOverflowError("always fatal"))

    assert(await(() => engine.isTerminated, 20000), "the engine did not terminate")
    assert(settled.await(10, TimeUnit.SECONDS), "giving up left an in-flight connect unsettled")
    assert(outcome.get().isLeft, s"expected a failure, got ${ outcome.get() }")

  test("giving up stops tracking the sockets it failed"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val client           = connectRaw(engine, port)
    awaitAccepted(accepted)
    assertEquals(engine.liveSocketCount, 1, "the connected socket was not tracked")

    engine.injectFault(() => throw new StackOverflowError("always fatal"))
    assert(await(() => engine.isTerminated, 20000), "the engine did not terminate")

    assert(
      await(() => engine.liveSocketCount == 0, 5000),
      s"sockets the engine gave up on are still tracked (live=${ engine.liveSocketCount })"
    )

    engine.injectFault(null)
    val (secondPort, secondAccepted) = acceptOne()
    val revived                      = connectRaw(engine, secondPort)
    awaitAccepted(secondAccepted)

    assertEquals(
      engine.liveSocketCount,
      1,
      "the revived engine counts sockets it no longer serves"
    )
    client.close()
    revived.close()

  test("a connection made while the engine is giving up is not swept away"):
    val engine           = isolated()
    val (port, accepted) = acceptOne()
    val victim           = connectRaw(engine, port)
    awaitAccepted(accepted)

    val (secondPort, secondAccepted) = acceptOne()
    val reconnected                  = new AtomicReference[RawSocket](null)
    val reconnectFailed              = new AtomicReference[Throwable](null)
    val reconnectDone                = new CountDownLatch(1)

    victim.read(
      4,
      _ =>
        engine.injectFault(null)
        engine.connect(
          "127.0.0.1",
          secondPort,
          SocketOptions.default,
          {
            case Right(socket) => reconnected.set(socket); reconnectDone.countDown()
            case Left(error)   => reconnectFailed.set(error); reconnectDone.countDown()
          }
        )
        ()
    )
    Thread.sleep(300)

    engine.injectFault(() => throw new StackOverflowError("always fatal"))
    assert(
      reconnectDone.await(30, TimeUnit.SECONDS),
      "the socket never reconnected from inside the sweep, so this test proved nothing"
    )
    assert(engine.revivalCount >= 1, "the reconnect did not have to revive the engine")

    assertEquals(reconnectFailed.get(), null, s"the reconnect was rejected: ${ reconnectFailed.get() }")
    val fresh = reconnected.get()
    assert(fresh != null, "no replacement socket was produced")

    assert(
      await(() => engine.liveSocketCount == 1, 5000),
      s"the socket made during the sweep was dropped from tracking (live=${ engine.liveSocketCount })"
    )

    val server             = awaitAccepted(secondAccepted)
    val (settled, outcome) = parkedRead(fresh, 4)
    server.getOutputStream.write("OKAY".getBytes("UTF-8"))
    server.getOutputStream.flush()
    assert(settled.await(10, TimeUnit.SECONDS), "the socket made during the sweep does not work")
    assertEquals(outcome.get().map(_.map(new String(_, "UTF-8"))), Right(Some("OKAY")))
    fresh.close()
