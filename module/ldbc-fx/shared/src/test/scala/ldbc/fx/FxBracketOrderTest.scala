/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.collection.mutable.ListBuffer

/**
 * Regression test for review finding B6: when a run with nested `bracket`s is cancelled, the
 * finalizers must run in LIFO order (inner resource released before the outer one), matching normal
 * unwinding — not in acquire order.
 */
class FxBracketOrderTest extends munit.FunSuite:

  test("nested bracket finalizers run in LIFO order on cancel"):
    val order = ListBuffer.empty[String]

    val program: Fx[Unit] =
      Fx.bracket(Fx.pure("outer"))(_ =>
        Fx.bracket(Fx.pure("inner"))(_ => Fx.async[Unit](_ => Fx.Canceler.noop))(_ =>
          Fx.delay { order += "inner"; () }
        )
      )(_ => Fx.delay { order += "outer"; () })

    val canceler = program.unsafeRun(_ => ())
    canceler.cancel()

    assertEquals(order.toList, List("inner", "outer"))
