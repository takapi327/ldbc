/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.InputStream
import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, AtomicReference }

class RawSocketCancellationTest extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(60, TimeUnit.SECONDS)

  private val engine = PlatformRawEngine.global

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

  private def connectRaw(port: Int): RawSocket =
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, SocketOptions.default, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect timed out")
    ref.get().fold(e => fail(s"connect failed: $e"), identity)

  test("cancelling a read leaves the bytes for the next read"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    val server           = awaitAccepted(accepted)

    val firstCallback = new AtomicReference[String]("not called")
    val canceler      = client.read(4, r => firstCallback.set(s"called: $r"))
    Thread.sleep(300)
    assertEquals(firstCallback.get(), "not called", "the first read completed even though no data had arrived")

    canceler.cancel()
    Thread.sleep(300)

    server.getOutputStream.write("AAAABBBB".getBytes("UTF-8"))
    server.getOutputStream.flush()

    Thread.sleep(800)

    val secondRef   = new AtomicReference[String]("not called")
    val secondLatch = new CountDownLatch(1)
    client.read(
      8,
      {
        case Right(Some(bytes)) => secondRef.set(new String(bytes, "UTF-8")); secondLatch.countDown()
        case other              => secondRef.set(s"unexpected: $other"); secondLatch.countDown()
      }
    )
    val got = if secondLatch.await(5, TimeUnit.SECONDS) then secondRef.get() else "timed out"
    client.close()

    assertEquals(got, "AAAABBBB", s"a cancelled read consumed and discarded bytes (only '$got' was left)")

  test("cancelling a write still transfers every byte"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    val server           = awaitAccepted(accepted)

    val size    = 8 * 1024 * 1024
    val payload = Array.tabulate(size)(i => (i % 251).toByte)

    val writeDone = new AtomicBoolean(false)
    val canceler  = client.write(payload, _ => writeDone.set(true))

    Thread.sleep(500)
    assert(!writeDone.get(), "the payload was too small to force a partial write, so this test proves nothing")

    canceler.cancel()

    val received = new AtomicInteger(0)
    val reader   = new Thread(() =>
      try
        val in: InputStream = server.getInputStream
        val buf  = new Array[Byte](64 * 1024)
        var read = in.read(buf)
        while read >= 0 && received.get() < size do
          received.addAndGet(read)
          read = if received.get() < size then in.read(buf) else -1
      catch { case _: Throwable => () }
    )
    reader.setDaemon(true)
    reader.start()
    reader.join(30000)

    client.close()

    assertEquals(
      received.get(),
      size,
      s"a cancelled write stopped midway (${ received.get() } of $size bytes arrived)"
    )
