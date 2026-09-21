/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.scalajs.js

class NodeErrorTest extends munit.FunSuite:
  private given ExecutionContext = munitExecutionContext

  private lazy val netModule = js.Dynamic.global.require("net")

  private def delay(ms: Int): Future[Unit] =
    val promise = Promise[Unit]()
    js.timers.setTimeout(ms.toDouble)(promise.success(()))
    promise.future

  test("a refused connection carries node's error code") {
    val closedPort = Promise[Int]()
    val server     = netModule.createServer((((_: js.Dynamic) => ())): js.Function1[js.Dynamic, Unit])
    server.listen(
      0,
      "127.0.0.1",
      (() => {
        val port = server.address().port.asInstanceOf[Int]
        server.close((() => { closedPort.success(port); () }): js.Function0[Unit])
        ()
      }): js.Function0[Unit]
    )

    for
      port    <- closedPort.future
      _       <- delay(100)
      outcome <- {
        val settled = Promise[Either[Throwable, RawSocket]]()
        PlatformRawEngine.global.connect("127.0.0.1", port, SocketOptions.default, r => { settled.success(r); () })
        settled.future
      }
    yield outcome match
      case Left(e) =>
        assert(e.isInstanceOf[java.io.IOException], s"expected an IOException, got $e")
        assert(e.getMessage.contains("ECONNREFUSED"), s"node's error code was discarded: ${ e.getMessage }")
      case Right(_) => fail("the connection to a closed port succeeded")
  }

  test("a write to a destroyed socket reports the failure") {
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
      _             = serverSocket.destroy()
      _            <- delay(200)
      outcome       = Promise[Either[Throwable, Unit]]()
      _             = raw.asInstanceOf[NodeRawSocket].underlying.destroy()
      _             = raw.write(new Array[Byte](64), r => { outcome.trySuccess(r); () })
      _            <- delay(500)
      settled       = outcome.future.value
    yield
      server.close()
      settled match
        case Some(scala.util.Success(Left(_: java.io.IOException))) => ()
        case other                                                  => fail(s"expected an IOException, got $other")
  }
