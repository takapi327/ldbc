/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

/**
 * Tests for the run loop's auto-cede: a long synchronous chain must not keep the starting thread
 * forever — after `Fx.autoCedeThreshold` steps it is re-scheduled onto the compute pool. This is what
 * stops a long continuation from monopolising an I/O poller/selector thread (see
 * `ldbc.net.PollerStarvationTest` for the transport-level consequence).
 */
class FxAutoCedeTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 10000): A =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then throw new RuntimeException("timeout")
    ref.get().fold(throw _, identity)

  /** A chain of `n` trivial synchronous steps, then reports the thread it finished on. */
  private def spin(n: Int, acc: Int): Fx[String] =
    if n <= 0 then Fx.delay(Thread.currentThread().getName)
    else Fx.delay(acc + 1).flatMap(next => spin(n - 1, next))

  test("a long synchronous chain is auto-ceded off the starting thread") {
    val startThread = Thread.currentThread().getName
    // The chain far exceeds the threshold, so it must offload to the compute pool at least once.
    val endThread = runSync(spin(Fx.autoCedeThreshold * 4, 0))
    assert(
      endThread != startThread,
      s"chain finished on the starting thread ($endThread) — auto-cede did not offload it"
    )
    assert(
      endThread.contains("fx-compute"),
      s"chain finished on '$endThread', expected the compute pool (fx-compute)"
    )
  }

  test("a short chain stays inline on the starting thread (no needless offload)") {
    val startThread = Thread.currentThread().getName
    val endThread   = runSync(spin(8, 0))
    assertEquals(endThread, startThread, "a short chain must not be offloaded")
  }

  test("auto-cede preserves result correctness across the thread hops") {
    val sum = runSync(spin2(Fx.autoCedeThreshold * 3))
    assertEquals(sum, Fx.autoCedeThreshold * 3)
  }

  /** A chain of `n` steps accumulating a sum, to check correctness survives the offload hops. */
  private def spin2(n: Int): Fx[Int] =
    def go(i: Int, acc: Int): Fx[Int] =
      if i <= 0 then Fx.pure(acc)
      else Fx.delay(acc + 1).flatMap(next => go(i - 1, next))
    go(n, 0)

  test("many concurrent long chains all complete (compute pool does not deadlock)") {
    val n    = 16
    val done = new CountDownLatch(n)
    val ok   = new AtomicInteger(0)
    var i    = 0
    while i < n do
      spin2(Fx.autoCedeThreshold * 2).unsafeRun {
        case Right(v) if v == Fx.autoCedeThreshold * 2 => ok.incrementAndGet(); done.countDown()
        case _                                         => done.countDown()
      }
      i += 1
    assert(done.await(15, TimeUnit.SECONDS), "not all chains completed")
    assertEquals(ok.get(), n)
  }
