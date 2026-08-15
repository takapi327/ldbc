/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration.*
import scala.scalajs.js

import ldbc.fx.Fx

/**
 * Node-transport abnormal-case coverage (the JS counterparts of the `jvm-native` tests, whose
 * `close`/TLS internals differ per platform): a peer reset surfaces as a read error, a read issued
 * after `close` fails promptly instead of hanging, and a TLS handshake against a plaintext peer
 * fails rather than hanging.
 */
class NodeSocketAbnormalTest extends munit.FunSuite:

  private given ExecutionContext = munitExecutionContext

  private lazy val netModule = js.Dynamic.global.require("net")

  private def toFuture[A](fx: Fx[A]): Future[A] =
    val promise = Promise[A]()
    fx.unsafeRun(result => promise.complete(result.toTry))
    promise.future

  /** Resolves to `f`'s outcome, or to `onTimeout` if `f` has not completed within `ms` (hang guard). */
  private def raceTimeout[A](f: Future[A], ms: Int, onTimeout: => A): Future[A] =
    val timed = Promise[A]()
    js.timers.setTimeout(ms.toDouble)(if !timed.isCompleted then { timed.success(onTimeout); () })
    f.onComplete(t => if !timed.isCompleted then timed.complete(t))
    timed.future

  /** Starts a plaintext echo server; returns a Future of its port. */
  private def startEchoServer(): Future[(Int, js.Dynamic)] =
    val ready  = Promise[Int]()
    val server = netModule.createServer(((sock: js.Dynamic) => {
      sock.on("data", ((chunk: js.Dynamic) => { sock.write(chunk); () }): js.Function1[js.Dynamic, Unit])
      ()
    }): js.Function1[js.Dynamic, Unit])
    server.listen(0, (() => ready.success(server.address().port.asInstanceOf[Int])): js.Function0[Unit])
    ready.future.map(port => (port, server))

  test("a peer reset surfaces as a read error, not as EOF"):
    val ready  = Promise[Int]()
    val server = netModule.createServer(((sock: js.Dynamic) => {
      // Abort the connection so the client observes ECONNRESET (an 'error') rather than a graceful FIN.
      sock.resetAndDestroy()
      ()
    }): js.Function1[js.Dynamic, Unit])
    server.listen(0, (() => ready.success(server.address().port.asInstanceOf[Int])): js.Function0[Unit])
    ready.future.flatMap { port =>
      val prog =
        for
          sock <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
          r    <- sock.read(64)
        yield r
      raceTimeout(
        toFuture(prog).transform(t => scala.util.Success(t)),
        4000,
        scala.util.Failure(new RuntimeException("hang"))
      )
        .map { outcome =>
          server.close()
          assert(outcome.isFailure, s"a reset connection must surface a read error, not EOF/None: got $outcome")
        }
    }

  test("read after close fails promptly and does not hang"):
    startEchoServer().flatMap {
      case (port, server) =>
        val prog =
          for
            sock <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
            _    <- sock.close()
            r    <- sock.read(16)
          yield r
        raceTimeout(
          toFuture(prog).transform(t => scala.util.Success(t)),
          3000,
          scala.util.Failure(new RuntimeException("hang"))
        )
          .map { outcome =>
            server.close()
            assert(outcome.isFailure, s"read after close must fail promptly, not hang or succeed: got $outcome")
          }
    }

  test("a TLS handshake against a plaintext peer fails and does not hang"):
    val ready  = Promise[Int]()
    val server = netModule.createServer(((sock: js.Dynamic) => {
      sock.write("HTTP/1.1 400 Bad Request\r\n\r\n")
      sock.end()
      ()
    }): js.Function1[js.Dynamic, Unit])
    server.listen(0, (() => ready.success(server.address().port.asInstanceOf[Int])): js.Function0[Unit])
    ready.future.flatMap { port =>
      val prog =
        for
          plain <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
          tls   <- Tls.client(plain, "localhost", port, SSL.Trusted)
        yield tls
      raceTimeout(
        toFuture(prog).transform(t => scala.util.Success(t)),
        5000,
        scala.util.Failure(new RuntimeException("hang"))
      )
        .map { outcome =>
          server.close()
          assert(outcome.isFailure, s"a TLS handshake against a plaintext peer must fail, got $outcome")
        }
    }
