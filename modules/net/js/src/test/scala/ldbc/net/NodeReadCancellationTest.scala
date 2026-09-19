/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

/**
 * The JS counterpart of `RawSocketCancellationTest`: cancelling a read must not swallow bytes that
 * arrive afterwards. Here the buffering lives in [[ReadBuffer]] rather than in an OS readiness
 * registration, so the mechanism differs entirely from JVM/Native and needs its own cover.
 */
class NodeReadCancellationTest extends munit.FunSuite:

  private given ExecutionContext = munitExecutionContext

  private lazy val netModule = js.Dynamic.global.require("net")

  private def toUint8(bytes: Array[Byte]): Uint8Array =
    val out = new Uint8Array(bytes.length)
    var i   = 0
    while i < bytes.length do { out(i) = (bytes(i) & 0xff).toShort; i += 1 }
    out

  private def delay(ms: Int): Future[Unit] =
    val promise = Promise[Unit]()
    js.timers.setTimeout(ms.toDouble)(promise.success(()))
    promise.future

  test("cancelling a read leaves the bytes for the next read") {
    val serverReady = Promise[Int]()
    val peer        = Promise[js.Dynamic]()

    val server = netModule.createServer(((socket: js.Dynamic) => {
      peer.success(socket); ()
    }): js.Function1[js.Dynamic, Unit])

    server.listen(
      0,
      "127.0.0.1",
      (() => { serverReady.success(server.address().port.asInstanceOf[Int]); () }): js.Function0[Unit]
    )

    for
      port <- serverReady.future
      raw  <- {
        val connected = Promise[RawSocket]()
        PlatformRawEngine.global.connect(
          "127.0.0.1",
          port,
          SocketOptions.default,
          {
            case Right(socket) => connected.success(socket); ()
            case Left(error)   => connected.failure(error); ()
          }
        )
        connected.future
      }
      serverSocket <- peer.future

      firstCalled = Promise[String]()
      canceler    = raw.read(4, r => { if !firstCalled.isCompleted then firstCalled.success(s"called: $r"); () })
      _ <- delay(200)
      _ = assert(!firstCalled.isCompleted, "データが無いのに1回目の read が完了している")

      _ = canceler.cancel()
      _ <- delay(100)

      _ = serverSocket.write(toUint8("AAAABBBB".getBytes("UTF-8")))
      _ <- delay(400)

      got <- {
        val second = Promise[String]()
        raw.read(
          8,
          {
            case Right(Some(bytes)) => second.success(new String(bytes, "UTF-8")); ()
            case other              => second.success(s"unexpected: $other"); ()
          }
        )
        second.future
      }
    yield
      raw.close()
      server.close()
      assertEquals(got, "AAAABBBB", s"キャンセルした read がバイトを捨てている（読めたのは '$got'）")
  }
