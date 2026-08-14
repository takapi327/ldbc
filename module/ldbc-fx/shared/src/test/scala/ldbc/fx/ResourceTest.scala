/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.collection.mutable.ListBuffer

/**
 * Cross-platform (JVM / JS / Native) tests for [[Resource]]: `use` releases on success/error,
 * `flatMap` composes with LIFO release order, and an acquire failure of a later resource releases the
 * earlier ones. Written as `Fx`-valued tests via [[FxSuite]] so they run on JS too.
 */
class ResourceTest extends FxSuite:

  private def tracked(log: ListBuffer[String], name: String): Resource[String] =
    Resource.make(Fx.delay { log += s"acquire-$name"; name })(_ => Fx.delay { log += s"release-$name"; () })

  test("use acquires, runs the body, and releases on success") {
    val log = ListBuffer.empty[String]
    tracked(log, "A").use(a => Fx.delay { log += s"use-$a"; a.length }).map { out =>
      assertEquals(out, 1)
      assertEquals(log.toList, List("acquire-A", "use-A", "release-A"))
    }
  }

  test("use releases even when the body fails") {
    val log  = ListBuffer.empty[String]
    val boom = new RuntimeException("boom")
    interceptFx[RuntimeException](tracked(log, "A").use(_ => Fx.raiseError[Int](boom))).map { e =>
      assertEquals(e.getMessage, "boom")
      assertEquals(log.toList, List("acquire-A", "release-A"))
    }
  }

  test("flatMap composes resources and releases in LIFO order") {
    val log = ListBuffer.empty[String]
    val res = for
      a <- tracked(log, "A")
      b <- tracked(log, "B")
    yield s"$a$b"
    res.use(v => Fx.delay { log += s"use-$v"; () }).map { _ =>
      assertEquals(log.toList, List("acquire-A", "acquire-B", "use-AB", "release-B", "release-A"))
    }
  }

  test("an acquire failure of a later resource releases the earlier ones") {
    val log  = ListBuffer.empty[String]
    val boom = new RuntimeException("acquire-B-failed")
    val res = for
      a <- tracked(log, "A")
      _ <- Resource.make(Fx.raiseError[String](boom))(_ => Fx.delay { log += "release-B"; () })
    yield a
    interceptFx[RuntimeException](res.use(_ => Fx.unit)).map { e =>
      assertEquals(e.getMessage, "acquire-B-failed")
      assertEquals(log.toList, List("acquire-A", "release-A"), "B never acquired; A must be released")
    }
  }

  test("eval and pure carry no release") {
    val log = ListBuffer.empty[String]
    val res = for
      _ <- Resource.eval(Fx.delay { log += "eval"; 1 })
      _ <- Resource.pure(2)
    yield ()
    res.use(_ => Fx.unit).map(_ => assertEquals(log.toList, List("eval")))
  }

  test("a failing release surfaces its error, and earlier releases still run (LIFO)") {
    val log     = ListBuffer.empty[String]
    val relBoom = new RuntimeException("release-B-failed")
    val res = for
      _ <- tracked(log, "A")
      _ <- Resource.make(Fx.delay { log += "acquire-B"; "B" })(_ => Fx.raiseError[Unit](relBoom))
    yield ()
    interceptFx[RuntimeException](res.use(_ => Fx.delay { log += "use"; () })).map { e =>
      assertEquals(e.getMessage, "release-B-failed")
      // B's release ran (and failed); A's release must still run afterwards (LIFO).
      assertEquals(log.toList, List("acquire-A", "acquire-B", "use", "release-A"))
    }
  }

  test("allocated returns the value and a release action to run later") {
    val log = ListBuffer.empty[String]
    tracked(log, "A").allocated
      .flatMap { (value, release) =>
        Fx.delay { log += s"between-$value"; () }.flatMap(_ => release)
      }
      .map(_ => assertEquals(log.toList, List("acquire-A", "between-A", "release-A")))
  }
