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

/**
 * The cancellation contract of [[RawSocket]], asserted against the real platform engine. Kept in
 * `jvm-native` so the NIO selector and the epoll/kqueue poller are held to the same behaviour by one
 * source — they used to differ, in opposite directions for read and write.
 *
 * `IoEngineTest` also has a cancellation case, but it only checks that a cancelled read does not
 * hang; it asserts nothing about what `cancel()` actually does, so it passes even with a no-op
 * canceler. These tests cover the effect of cancelling.
 *
 * Both rely on a peer that does nothing until told to. The read case waits until no data can
 * possibly have arrived, so the read has to park; the write case sends far more than any send
 * buffer can absorb and leaves the peer unread, so the write has to park part-way through — that
 * mid-transfer state is the one whose cancellation matters. The peer only starts draining after
 * the cancel, which is what makes the final byte count meaningful.
 */
class RawSocketCancellationTest extends munit.FunSuite:

  override val munitTimeout = scala.concurrent.duration.Duration(60, TimeUnit.SECONDS)

  private val engine = PlatformRawEngine.global

  /** A server that accepts one connection and then does only what the test tells it to. */
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
    assert(socket != null, "サーバが accept できなかった")
    socket

  private def connectRaw(port: Int): RawSocket =
    val ref   = new AtomicReference[Either[Throwable, RawSocket]](null)
    val latch = new CountDownLatch(1)
    engine.connect("127.0.0.1", port, SocketOptions.default, r => { ref.set(r); latch.countDown() })
    assert(latch.await(10, TimeUnit.SECONDS), "connect がタイムアウトした")
    ref.get().fold(e => fail(s"connect 失敗: $e"), identity)

  test("cancelling a read leaves the bytes for the next read"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    val server           = awaitAccepted(accepted)

    val firstCallback = new AtomicReference[String]("not called")
    val canceler      = client.read(4, r => firstCallback.set(s"called: $r"))
    Thread.sleep(300)
    assertEquals(firstCallback.get(), "not called", "データが無いのに1回目の read が完了している")

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

    assertEquals(got, "AAAABBBB", s"キャンセルした read がバイトを消費して捨てている（読めたのは '$got'）")

  test("cancelling a write still transfers every byte"):
    val (port, accepted) = acceptOne()
    val client           = connectRaw(port)
    val server           = awaitAccepted(accepted)

    val size    = 8 * 1024 * 1024
    val payload = Array.tabulate(size)(i => (i % 251).toByte)

    val writeDone = new AtomicBoolean(false)
    val canceler  = client.write(payload, _ => writeDone.set(true))

    Thread.sleep(500)
    assert(!writeDone.get(), "ペイロードが小さすぎて部分書き込みが起きていない（テストの前提が崩れている）")

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
      s"キャンセルされた write が途中で打ち切られている（${ received.get() } / $size バイトしか届いていない）"
    )
