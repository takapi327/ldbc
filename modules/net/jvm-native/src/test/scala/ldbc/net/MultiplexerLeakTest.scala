/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

class MultiplexerLeakTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(180, TimeUnit.SECONDS)

  private def await(cond: () => Boolean, millis: Int): Boolean =
    var waited = 0
    while !cond() && waited < millis do { Thread.sleep(25); waited += 25 }
    cond()

  private def connectAndClose(engine: RawIoEngine): Unit =
    val server   = new ServerSocket(0)
    val accepted = new AtomicReference[java.net.Socket](null)
    val acceptor = new Thread(() =>
      try accepted.set(server.accept())
      catch { case _: Throwable => () }
    )
    acceptor.setDaemon(true)
    acceptor.start()

    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    engine.connect("127.0.0.1", server.getLocalPort, SocketOptions.default, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect timed out")
    ref.get().foreach(_.close())
    assert(await(() => accepted.get() != null, 10000), "the server never accepted")
    accepted.get().close()
    server.close()

  private def forceRevival(engine: RawIoEngine & PollerDiagnostics): Unit =
    engine.injectFault(() => throw new StackOverflowError("always fatal"))
    assert(await(() => engine.isTerminated, 30000), "the engine did not terminate")
    engine.injectFault(null)
    connectAndClose(engine)

  test("reviving the engine releases the multiplexer it replaced"):
    val engine = PlatformRawEngine.startIsolated("ldbc-net-leak-test")
    connectAndClose(engine)

    val revivals = 5
    for _ <- 1 to revivals do forceRevival(engine)

    assertEquals(engine.revivalCount, revivals, "the engine was not revived as many times as expected")
    assertEquals(
      engine.releasedMultiplexers,
      revivals,
      "a revival replaced the multiplexer without releasing the one it displaced"
    )
