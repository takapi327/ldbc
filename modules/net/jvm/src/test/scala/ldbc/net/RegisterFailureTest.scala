/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.channels.{ SelectionKey, SocketChannel }
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

class RegisterFailureTest extends munit.FunSuite:

  test("a registration that cannot be placed settles the caller"):
    val engine = NioRawEngine.start()
    val ch     = SocketChannel.open()
    ch.configureBlocking(false)
    ch.close()

    val settled = new CountDownLatch(1)
    val failure = new AtomicReference[Throwable](null)
    val fired   = new AtomicReference[String]("not fired")

    engine.register(
      null,
      ch,
      SelectionKey.OP_READ,
      () => fired.set("fired"),
      e => { failure.set(e); settled.countDown() }
    )

    assert(settled.await(5, TimeUnit.SECONDS), "the failed registration was dropped silently")
    assert(
      failure.get().isInstanceOf[java.nio.channels.ClosedChannelException],
      s"expected a ClosedChannelException, got ${ failure.get() }"
    )
    assertEquals(fired.get(), "not fired", "the readiness callback ran despite the registration failing")
