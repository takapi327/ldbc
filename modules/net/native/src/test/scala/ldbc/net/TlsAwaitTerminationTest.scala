/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import ldbc.fx.Fx

class TlsAwaitTerminationTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(90, TimeUnit.SECONDS)

  private def await(cond: () => Boolean, millis: Int): Boolean =
    var waited = 0
    while !cond() && waited < millis do { Thread.sleep(25); waited += 25 }
    cond()

  test("a TLS-style readiness wait is settled when the engine gives up"):
    val engine = FdRawEngine.start("ldbc-net-tls-await-test")

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
    assert(connected.await(10, TimeUnit.SECONDS), "connect timed out")
    val socket = ref.get().fold(e => fail(s"connect failed: $e"), identity).asInstanceOf[FdRawSocket]
    assert(await(() => accepted.get() != null, 10000), "the server never accepted")

    val settled = new CountDownLatch(1)
    val outcome = new AtomicReference[Either[Throwable, Unit]](null)
    val awaiting = Fx.async[Unit] { cb =>
      engine.armRead(socket.fileDescriptor, socket.channelState, () => cb(Right(())), e => cb(Left(e)))
      new Fx.Canceler:
        override def cancel(): Unit = socket.channelState.readReady = null
    }
    awaiting.unsafeRun { r => outcome.set(r.map(_ => ())); settled.countDown() }

    Thread.sleep(400)
    assertEquals(settled.getCount, 1L, "the wait resolved on its own, so this test proved nothing")

    engine.injectFault(() => throw new StackOverflowError("always fatal"))
    assert(await(() => engine.isTerminated, 30000), "the engine did not terminate")

    val wasSettled = settled.await(10, TimeUnit.SECONDS)
    socket.close()
    server.close()

    assert(wasSettled, "giving up left a TLS-style readiness wait unsettled")
    assert(outcome.get().isLeft, s"expected a failure, got ${ outcome.get() }")
