/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import ldbc.fx.concurrentFx
import ldbc.fx.Fx

/**
 * Regression test for review finding M4: `connect` to an unreachable host must fail within its
 * timeout rather than hang forever. Uses a non-routable blackhole address that silently drops the
 * SYN; without the timeout the connect would block until the OS default (or never).
 */
class IoEngineConnectTimeoutTest extends munit.FunSuite:

  private val engine = ldbc.net.IoEngine.fromRaw[Fx](PlatformRawEngine.global)

  private def runSync[A](fx: Fx[A], timeoutMs: Long): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("runSync gave up"))
    else ref.get()

  test("connect to an unreachable host fails within the timeout, not hanging"):
    val startNanos = System.nanoTime()
    val result     = runSync(engine.connect("10.255.255.1", 80, 500.millis), 8000)
    val elapsedMs  = (System.nanoTime() - startNanos) / 1000000L
    assert(result.isLeft, s"expected connect to fail, got $result")
    assert(elapsedMs < 5000, s"connect should time out promptly (~500ms), took ${ elapsedMs }ms")

  test("connect to a refused port fails fast with a connection error, not a timeout"):
    val startNanos = System.nanoTime()
    val result     = runSync(engine.connect("127.0.0.1", 1, 5.seconds), 8000)
    val elapsedMs  = (System.nanoTime() - startNanos) / 1000000L
    assert(result.isLeft, s"expected connect to a closed port to fail, got $result")
    assert(
      !result.left.exists(_.isInstanceOf[ConnectTimeoutException]),
      s"a refused connection must surface a connection error, not a timeout: ${ result.left.toOption }"
    )
    assert(elapsedMs < 2000, s"a refused connection should fail promptly (RST), took ${ elapsedMs }ms")

  test("cancelling an in-progress connect returns promptly and never completes the callback"):
    val outcome  = new AtomicReference[Either[Throwable, ldbc.net.Socket[Fx]]](null)
    val canceler = engine.connect("10.255.255.1", 80, 30.seconds).unsafeRun(r => outcome.set(r))
    Thread.sleep(200)
    val startNanos = System.nanoTime()
    canceler.cancel()
    val cancelMs = (System.nanoTime() - startNanos) / 1000000L
    assert(cancelMs < 1000, s"cancel must return promptly, took ${ cancelMs }ms")
    Thread.sleep(500)
    val result = outcome.get()
    assert(result == null, s"a cancelled connect must not complete the callback at all: got $result")
