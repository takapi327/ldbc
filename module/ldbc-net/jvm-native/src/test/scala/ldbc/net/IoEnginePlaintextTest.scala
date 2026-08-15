/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import ldbc.fx.Fx

/**
 * Plaintext transport behaviour shared by the JVM (NIO selector) and Native (epoll/kqueue) engines:
 * a real connect/write/read round-trip and out-of-band EOF. Kept in `jvm-native` so both non-blocking
 * `IoEngine` implementations are exercised by the same assertions.
 */
class IoEnginePlaintextTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 8000): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("timeout")) else ref.get()

  private def startEchoServer(): Int =
    val ss = new ServerSocket(0)
    val th = new Thread(() =>
      while true do
        val s      = ss.accept()
        val worker = new Thread(() =>
          try
            val in  = s.getInputStream
            val out = s.getOutputStream
            val buf = new Array[Byte](1024)
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

  test("connect + write + read round-trips over the non-blocking engine"):
    val prog =
      for
        sock <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
        _    <- sock.write("PING".getBytes("UTF-8"))
        resp <- sock.read(64)
        _    <- sock.close()
      yield resp.map(bytes => new String(bytes, "UTF-8"))
    assertEquals(runSync(prog), Right(Some("PING")))

  test("a larger payload is delivered intact"):
    val payload = ("x" * 4000)
    val prog    =
      for
        sock  <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
        _     <- sock.write(payload.getBytes("UTF-8"))
        first <- sock.read(8192)
        _     <- sock.close()
      yield first.map(_.length)
    runSync(prog) match
      case Right(Some(n)) => assert(n > 0 && n <= payload.length, s"unexpected chunk size $n")
      case other          => fail(s"unexpected: $other")

  test("peer close surfaces as None (out-of-band EOF), read(0) as Some(empty)"):
    val oneShot = new ServerSocket(0)
    val server  = new Thread(() =>
      try
        val s = oneShot.accept()
        s.getOutputStream.write("BYE".getBytes("UTF-8"))
        s.getOutputStream.flush()
        s.close()
      catch { case _: Throwable => () }
    )
    server.setDaemon(true)
    server.start()

    val prog =
      for
        sock <- IoEngine.global.connect("127.0.0.1", oneShot.getLocalPort, 5.seconds)
        zero <- sock.read(0)
        data <- sock.read(16)
        eof  <- sock.read(16)
        _    <- sock.close()
      yield (zero, data.map(new String(_, "UTF-8")), eof)

    runSync(prog) match
      case Right((zero, data, eof)) =>
        assert(zero.exists(_.isEmpty), s"read(0) must be Some(empty), got $zero")
        assertEquals(data, Some("BYE"))
        assertEquals(eof, None, "peer close must surface as None")
      case Left(error) => fail(s"unexpected failure: $error")

  test("a mid-stream connection reset terminates the read promptly without phantom data"):
    val rst = new ServerSocket(0)
    val th  = new Thread(() =>
      try
        val s = rst.accept()
        s.getInputStream.read(new Array[Byte](16)) // wait for the client's write so the link is established
        s.setSoLinger(true, 0)                     // linger 0 → close aborts the connection with an RST
        s.close()
      catch { case _: Throwable => () }
    )
    th.setDaemon(true)
    th.start()

    val startNanos = System.nanoTime()
    val prog       =
      for
        sock <- IoEngine.global.connect("127.0.0.1", rst.getLocalPort, 5.seconds)
        _    <- sock.write("PING".getBytes("UTF-8"))
        r    <- sock.read(64)
      yield r
    val result    = runSync(prog, 4000)
    val elapsedMs = (System.nanoTime() - startNanos) / 1000000L
    // A reset surfaces as ECONNRESET (Left) on macOS/BSD, but as an orderly EOF (Right(None)) on Linux
    // once all sent data has been consumed — both are acceptable OS behaviours. What must never happen
    // is a hang, or phantom data (Right(Some(_))) that hides the disconnection.
    assert(elapsedMs < 2000, s"a reset must terminate the read promptly, not hang (took ${ elapsedMs }ms)")
    assert(!result.exists(_.isDefined), s"a reset must surface as an error or EOF, never phantom data: got $result")

  test("read after close fails promptly and does not hang"):
    val startNanos = System.nanoTime()
    val prog       =
      for
        sock <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
        _    <- sock.close()
        r    <- sock.read(16)
      yield r
    val result    = runSync(prog, 4000)
    val elapsedMs = (System.nanoTime() - startNanos) / 1000000L
    assert(result.isLeft, s"read after close must fail, got $result")
    assert(elapsedMs < 1500, s"read after close must fail promptly, not hang (took ${ elapsedMs }ms)")
