/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import munit.FunSuite

import ldbc.fx.{ Fx, FxRuntime }
import ldbc.fx.concurrentFx // given Concurrent[Fx]

import ldbc.net.IoEngine

/**
 * Step 3 check: the generic `ldbc.net.Socket[F]` over the effect-free [[NioRawEngine]] runs at
 * `F = Fx` (via `concurrentFx`) against a real MySQL server — no `Fx`-native `Socket` involved.
 */
class GenericSocketFxTest extends FunSuite:

  private def runFx[A](fx: Fx[A]): A =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]]()
    fx.unsafeRun { r => ref.set(r); latch.countDown() }(using FxRuntime.global)
    latch.await()
    ref.get().fold(throw _, identity)

  test("generic Socket[Fx] over NioRawEngine reads the MySQL handshake (proto v10)") {
    val engine: IoEngine[Fx] = IoEngine.fromRaw[Fx](NioRawEngine.global)
    val program: Fx[Byte] =
      engine.connect("127.0.0.1", 13306, 5.seconds).flatMap { sock =>
        sock.read(4).flatMap {
          case Some(header) =>
            val len = (header(0) & 0xff) | ((header(1) & 0xff) << 8) | ((header(2) & 0xff) << 16)
            sock.read(len).flatMap {
              case Some(payload) => sock.close().map(_ => payload(0))
              case None          => Fx.raiseError(new RuntimeException("eof reading payload"))
            }
          case None => Fx.raiseError(new RuntimeException("eof reading header"))
        }
      }
    assertEquals(runFx(program), 0x0a.toByte)
  }
