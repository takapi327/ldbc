/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.testkit.munit

import cats.effect.IO

import munit.CatsEffectSuite

class AssertRowsUnorderedTest extends CatsEffectSuite:

  private object Suite extends LdbcSuite:
    override def dataSource = throw new UnsupportedOperationException("no DB needed")

  // assertRowsUnordered

  test("assertRowsUnordered passes when lists contain the same elements in different order") {
    Suite.assertRowsUnordered(IO.pure(List(3, 1, 2)), List(1, 2, 3))
  }

  test("assertRowsUnordered fails when actual and expected have different elements") {
    Suite.assertRowsUnordered(IO.pure(List(1, 2, 4)), List(1, 2, 3)).attempt.map { result =>
      assert(result.isLeft, "should fail when elements differ")
    }
  }

  test("assertRowsUnordered correctly fails when actual has different duplicates than expected") {
    // actual   = List(1, 2, 2)  →  Map(1->1, 2->2)
    // expected = List(1, 1, 2)  →  Map(1->2, 2->1)
    Suite.assertRowsUnordered(IO.pure(List(1, 2, 2)), List(1, 1, 2)).attempt.map { result =>
      assert(result.isLeft, "should fail when duplicate counts differ")
    }
  }

  test("assertRowsUnordered correctly fails when expected has more duplicates than actual") {
    // actual   = List(1, 2)       →  Map(1->1, 2->1)
    // expected = List(1, 1, 2, 2) →  Map(1->2, 2->2)
    Suite.assertRowsUnordered(IO.pure(List(1, 2)), List(1, 1, 2, 2)).attempt.map { result =>
      assert(result.isLeft, "should fail when element counts differ")
    }
  }

  test("assertRowsUnordered passes with duplicates when counts match") {
    Suite.assertRowsUnordered(IO.pure(List(1, 2, 2, 3)), List(2, 3, 1, 2))
  }

  // assertRowsOrdered

  test("assertRowsOrdered passes when lists are identical") {
    Suite.assertRowsOrdered(IO.pure(List(1, 2, 3)), List(1, 2, 3))
  }

  test("assertRowsOrdered fails when order differs") {
    Suite.assertRowsOrdered(IO.pure(List(3, 1, 2)), List(1, 2, 3)).attempt.map { result =>
      assert(result.isLeft, "should fail when order differs")
    }
  }

  test("assertRowsOrdered fails when elements differ") {
    Suite.assertRowsOrdered(IO.pure(List(1, 2, 4)), List(1, 2, 3)).attempt.map { result =>
      assert(result.isLeft, "should fail when elements differ")
    }
  }
