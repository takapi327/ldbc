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

class IoEngineTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 5000): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("timeout")) else ref.get()

  private def startEchoServer(): Int =
    val ss   = new ServerSocket(0)
    val port = ss.getLocalPort
    val th = new Thread(() =>
      while true do
        val s = ss.accept()
        val t2 = new Thread(() =>
          try
            val in  = s.getInputStream
            val out = s.getOutputStream
            val buf = new Array[Byte](1024)
            var r   = in.read(buf)
            while r >= 0 do { out.write(buf, 0, r); out.flush(); r = in.read(buf) }
          catch { case _: Throwable => () }
        )
        t2.setDaemon(true)
        t2.start()
    )
    th.setDaemon(true)
    th.start()
    port

  private lazy val port = startEchoServer()

  private def echo(msg: String): Fx[String] =
    for
      sock <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
      _    <- sock.write(msg.getBytes("UTF-8"))
      resp <- sock.read(64)
      _    <- sock.close()
    yield new String(resp.getOrElse(Array.emptyByteArray), "UTF-8")

  test("connect + write + read round-trips over real NIO socket"):
    assertEquals(runSync(echo("PING")), Right("PING"))

  test("multiple bytes are serialized correctly"):
    assertEquals(runSync(echo("HELLO-WORLD-123")), Right("HELLO-WORLD-123"))

  test("read cancellation does not hang (no data arrives)"):
    val pending = IoEngine.global.connect("127.0.0.1", port, 5.seconds).flatMap(_.read(16))
    val latch   = new CountDownLatch(1)
    val canc    = pending.unsafeRun(_ => latch.countDown())
    val completedBeforeCancel = latch.await(200, TimeUnit.MILLISECONDS)
    canc.cancel()
    assert(!completedBeforeCancel)

  test("10 concurrent connections are multiplexed by a single selector"):
    val results = (1 to 10).map(i => runSync(echo(s"C$i"))).toSet
    assertEquals(results, (1 to 10).map(i => Right(s"C$i")).toSet)

  test("two concurrent reads on one socket are serialized and both complete (no lost wakeup)"):
    runSync(IoEngine.global.connect("127.0.0.1", port, 5.seconds)) match
      case Left(error) => fail(s"connect failed: $error")
      case Right(sock) =>
        val firstBytes = new AtomicReference[Array[Byte]](null)
        val secondByte = new AtomicReference[Array[Byte]](null)
        val done       = new CountDownLatch(2)
        sock.read(4).unsafeRun { r => firstBytes.set(r.toOption.flatten.orNull); done.countDown() }
        sock.read(4).unsafeRun { r => secondByte.set(r.toOption.flatten.orNull); done.countDown() }
        runSync(sock.write("ABCDEFGH".getBytes("UTF-8")))
        val completed = done.await(8, TimeUnit.SECONDS)
        runSync(sock.close())
        assert(completed, "a concurrent read was lost (hang): only one callback fired")
        val a = Option(firstBytes.get()).getOrElse(Array.emptyByteArray)
        val b = Option(secondByte.get()).getOrElse(Array.emptyByteArray)
        assertEquals(
          (a ++ b).sorted.toList,
          "ABCDEFGH".getBytes("UTF-8").sorted.toList,
          "both reads must complete and together receive all bytes"
        )

  test("EOF is None (out of band) and read(0) is Some(empty) on a live connection"):
    val oneShot = new ServerSocket(0)
    val server = new Thread(() =>
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
        sock  <- IoEngine.global.connect("127.0.0.1", oneShot.getLocalPort, 5.seconds)
        zero  <- sock.read(0)
        data  <- sock.read(16)
        eof   <- sock.read(16)
        _     <- sock.close()
      yield (zero, data.map(new String(_, "UTF-8")), eof)

    runSync(prog) match
      case Right((zero, data, eof)) =>
        assert(zero.exists(_.isEmpty), s"read(0) must be Some(empty), got $zero")
        assertEquals(data, Some("BYE"), "data must arrive as Some")
        assertEquals(eof, None, "peer close must surface as None, not an empty array")
      case Left(error) => fail(s"unexpected failure: $error")
