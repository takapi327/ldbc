/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.scalajs.js

class PendingCallbackJsTest extends munit.FunSuite:
  private given ExecutionContext = munitExecutionContext

  private lazy val netModule = js.Dynamic.global.require("net")

  private def delay(ms: Int): Future[Unit] =
    val promise = Promise[Unit]()
    js.timers.setTimeout(ms.toDouble)(promise.success(()))
    promise.future

  test("closing while a read is parked settles it with a failure") {
    val serverReady = Promise[Int]()
    val peer        = Promise[js.Dynamic]()
    val server      =
      netModule.createServer(((socket: js.Dynamic) => { peer.success(socket); () }): js.Function1[js.Dynamic, Unit])
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
      _ <- peer.future
      outcome = Promise[Either[Throwable, Option[Array[Byte]]]]()
      _       = raw.read(16, r => { outcome.trySuccess(r); () })
      _ <- delay(200)
      pending = outcome.future.value
      _       = assert(pending.isEmpty, "the read completed even though no data had arrived")
      _       = raw.close()
      _ <- delay(500)
      settled = outcome.future.value
    yield
      server.close()
      settled match
        case Some(scala.util.Success(Left(_: java.io.IOException))) => ()
        case other                                                  => fail(s"expected an IOException, got $other")
  }

  test("closing while a write is parked settles it") {
    val serverReady = Promise[Int]()
    val peer        = Promise[js.Dynamic]()
    val server      =
      netModule.createServer(((socket: js.Dynamic) => { peer.success(socket); () }): js.Function1[js.Dynamic, Unit])
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
      _       = serverSocket.pause()
      outcome = Promise[Either[Throwable, Unit]]()
      _       = raw.write(new Array[Byte](64 * 1024 * 1024), r => { outcome.trySuccess(r); () })
      _ <- delay(300)
      parked = outcome.future.value
      _      = assert(parked.isEmpty, "the payload was too small to force a partial write")
      _      = raw.close()
      _ <- delay(1000)
      settled = outcome.future.value
    yield
      server.close()
      assert(settled.isDefined, "close left the parked write unsettled")
  }
