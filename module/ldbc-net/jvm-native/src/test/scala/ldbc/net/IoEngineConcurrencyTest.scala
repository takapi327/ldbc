/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ ConcurrentLinkedQueue, CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.*

import ldbc.fx.Fx

/**
 * Concurrency load for the non-blocking engine (JVM NIO selector / Native epoll·kqueue): many
 * connections are multiplexed through the single poller thread at once, each doing several
 * request-response round-trips. This exercises the per-fd interest state, the arm/wakeup path, and
 * the one-shot re-arming under contention — the property that motivates a readiness-based engine
 * (one poller thread regardless of connection count).
 */
class IoEngineConcurrencyTest extends munit.FunSuite:

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

  private lazy val port = startEchoServer()

  /** One connection doing `rounds` sequential echo round-trips; completes `latch` with the outcome. */
  private def session(
    id:     Int,
    rounds: Int,
    ok:     AtomicInteger,
    bad:    ConcurrentLinkedQueue[String],
    latch:  CountDownLatch
  ): Unit =
    def loop(n: Int, sock: Socket): Fx[Unit] =
      if n >= rounds then sock.close()
      else
        val msg = s"c$id-r$n"
        sock
          .write(msg.getBytes("UTF-8"))
          .flatMap(_ => sock.read(256))
          .flatMap { resp =>
            val got = resp.map(bytes => new String(bytes, "UTF-8")).getOrElse("<eof>")
            if got == msg then loop(n + 1, sock)
            else { bad.add(s"expected $msg got $got"); sock.close() }
          }
    val program = IoEngine.global.connect("127.0.0.1", port, 5.seconds).flatMap(loop(0, _))
    program.unsafeRun {
      case Right(_)    => ok.incrementAndGet(); latch.countDown()
      case Left(error) => bad.add(s"c$id: ${ error.getMessage }"); latch.countDown()
    }

  test("many connections are multiplexed concurrently through the single poller"):
    val connections = 40
    val rounds      = 20
    val ok          = new AtomicInteger(0)
    val bad         = new ConcurrentLinkedQueue[String]()
    val latch       = new CountDownLatch(connections)

    val started = System.nanoTime()
    var i       = 0
    while i < connections do
      session(i, rounds, ok, bad, latch)
      i += 1

    val finished   = latch.await(30, TimeUnit.SECONDS)
    val elapsedMs  = (System.nanoTime() - started) / 1000000.0
    val roundTrips = ok.get() * rounds
    println(
      f"[IoEngineConcurrencyTest] $connections conns x $rounds rounds: ok=${ ok.get() } " +
        f"in $elapsedMs%.1f ms (${ roundTrips / (elapsedMs / 1000.0) }%.0f round-trips/s)"
    )

    assert(finished, s"timed out; only ${ connections - latch.getCount } of $connections finished")
    assert(bad.isEmpty, s"failures: ${ bad.toArray.mkString("; ") }")
    assertEquals(ok.get(), connections, "every connection must complete all round-trips")
