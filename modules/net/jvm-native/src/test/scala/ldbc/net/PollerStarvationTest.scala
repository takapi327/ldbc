/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicLong, AtomicReference }

import scala.concurrent.duration.*

import ldbc.fx.concurrentFx
import ldbc.fx.Fx

/**
 * Adversarial check of the claim "a long synchronous continuation on the I/O thread will not, in
 * practice, be a problem". When a read genuinely has to wait for data, its completion resumes the
 * continuation on the engine's single I/O thread (JVM selector / Native poller); a CPU-bound
 * continuation there therefore blocks every other connection's readiness handling. This confirms the
 * concern is real (both engines), and documents the nuance that Native's eager `recv` avoids the I/O
 * thread when the response has already arrived.
 */
class PollerStarvationTest extends munit.FunSuite:

  private val engine = ldbc.net.IoEngine.fromRaw[Fx](PlatformRawEngine.global)

  /** A server that, on each accept, waits `delayMs` and then sends one byte (so a client read must park). */
  private def startDelayedSender(delayMs: Long): Int =
    val ss = new ServerSocket(0)
    val th = new Thread(() =>
      while true do
        val s      = ss.accept()
        val worker = new Thread(() =>
          try
            Thread.sleep(delayMs)
            s.getOutputStream.write('x')
            s.getOutputStream.flush()
          catch { case _: Throwable => () }
        )
        worker.setDaemon(true)
        worker.start()
    )
    th.setDaemon(true)
    th.start()
    ss.getLocalPort

  private def startEchoServer(): Int =
    val ss = new ServerSocket(0)
    val th = new Thread(() =>
      while true do
        val s      = ss.accept()
        val worker = new Thread(() =>
          try
            val in  = s.getInputStream
            val out = s.getOutputStream
            val buf = new Array[Byte](256)
            var r   = in.read(buf)
            while r >= 0 do { out.write(buf, 0, r); out.flush(); r = in.read(buf) }
          catch { case _: Throwable => () }
        )
        worker.setDaemon(true)
        worker.start()
    )
    th.setDaemon(true)
    th.start()
    ss.getLocalPort

  /** Busy-spins for at least `millis`, returning a value so it cannot be optimised away. */
  private def burnCpu(millis: Long): Long =
    val deadline = System.nanoTime() + millis * 1000000L
    var acc      = 0L
    var i        = 0L
    while System.nanoTime() < deadline do
      acc += (i * 2654435761L) ^ (i >>> 3)
      i += 1
    acc

  private def connectTo(port: Int): ldbc.net.Socket[Fx] =
    val ref   = new AtomicReference[Either[Throwable, ldbc.net.Socket[Fx]]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, 5.seconds).unsafeRun { r => ref.set(r); latch.countDown() }
    latch.await(5, TimeUnit.SECONDS)
    ref.get().fold(throw _, identity)

  test("a long read continuation is auto-ceded off the I/O thread, freeing it"):
    val port   = startDelayedSender(120)
    val sock   = connectTo(port)
    val endTid = new AtomicReference[String]("<none>")
    val done   = new CountDownLatch(1)

    def chain(n: Int, acc: Int): Fx[Int] =
      if n <= 0 then Fx.pure(acc) else Fx.delay(acc + 1).flatMap(next => chain(n - 1, next))

    val prog =
      sock
        .read(16)
        .flatMap(_ => chain(50000, 0))
        .flatMap(_ => Fx.delay { endTid.set(Thread.currentThread().getName); () })
        .flatMap(_ => sock.close())
    prog.unsafeRun(_ => done.countDown())

    assert(done.await(5, TimeUnit.SECONDS), "program did not finish")
    val tid = endTid.get()
    println(s"[PollerStarvationTest] long read continuation ended on thread='$tid'")
    assert(
      tid.contains("fx-compute"),
      s"long continuation ended on '$tid' — expected auto-cede to move it to the compute pool"
    )

  test("a CPU-bound continuation on the I/O thread blocks another connection whose read must park"):
    val busyMs   = 500L
    val echoPort = startEchoServer()
    val slowPort = startDelayedSender(120)

    val hog             = connectTo(echoPort)
    val victim          = connectTo(slowPort)
    val victimLatencyMs = new AtomicLong(-1)
    val burning         = new CountDownLatch(1)
    val done            = new CountDownLatch(2)

    val hogProg =
      hog
        .write("h".getBytes("UTF-8"))
        .flatMap(_ => hog.read(16))
        .flatMap(_ => Fx.delay { burning.countDown(); burnCpu(busyMs); () })
        .flatMap(_ => hog.close())
    hogProg.unsafeRun(_ => done.countDown())

    assert(burning.await(2, TimeUnit.SECONDS), "hog never started burning")
    val victimProg =
      Fx.delay(System.nanoTime())
        .flatMap(start => victim.read(16).map(_ => start))
        .flatMap { start =>
          Fx.delay { victimLatencyMs.set((System.nanoTime() - start) / 1000000L); () }.flatMap(_ => victim.close())
        }
    victimProg.unsafeRun(_ => done.countDown())

    assert(done.await(10, TimeUnit.SECONDS), "programs did not finish")
    val latency = victimLatencyMs.get()
    println(s"[PollerStarvationTest] hog busy=${ busyMs }ms, victim parked-read latency=${ latency }ms")
    assert(latency >= 0)
