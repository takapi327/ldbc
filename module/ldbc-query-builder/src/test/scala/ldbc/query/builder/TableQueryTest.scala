/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import org.scalatest.flatspec.AnyFlatSpec

import cats.Id

import ldbc.core.*
import ldbc.query.builder.syntax.ColumnSyntax

class TableQueryTest extends AnyFlatSpec, ColumnSyntax[Id]:

  case class Test(p1: Long, p2: String, p3: Option[String])
  private val table = Table[Test]("test")(
    column("p1", BIGINT),
    column("p2", VARCHAR(255)),
    column("p3", VARCHAR(255))
  )

  case class JoinTest(p1: Long, p2: String, p3: Option[String])
  private val joinTable = Table[JoinTest]("join_test")(
    column("p1", BIGINT),
    column("p2", VARCHAR(255)),
    column("p3", VARCHAR(255))
  )

  private val query     = TableQuery[Id, Test](table)
  private val joinQuery = TableQuery[Id, JoinTest](joinTable)

  it should "The query statement generated from Table is equal to the specified query statement." in {
    assert(query.select[Long](_.p1).statement === "SELECT `p1` FROM test")
    assert(query.select[Long](_.p1).where(_.p1 > 1).statement === "SELECT `p1` FROM test WHERE p1 > ?")
    assert(
      query
        .select[Long](_.p1)
        .where(_.p1 >= 1)
        .and(_.p2 === "test")
        .statement === "SELECT `p1` FROM test WHERE p1 >= ? AND p2 = ?"
    )
    assert(
      query
        .select[Long](_.p1)
        .where(v => v.p1 > 1 || v.p2 === "test")
        .statement === "SELECT `p1` FROM test WHERE (p1 > ? OR p2 = ?)"
    )
    assert(
      query
        .select[Long](_.p1)
        .where(v => v.p1 > 1 && v.p2 === "test")
        .or(_.p3 === "test")
        .statement === "SELECT `p1` FROM test WHERE (p1 > ? AND p2 = ?) OR p3 = ?"
    )
    assert(query.select[Long](_.p1).groupBy(p1 => p1).statement === "SELECT `p1` FROM test GROUP BY p1")
    assert(
      query
        .select[Long](_.p1)
        .groupBy(p1 => p1)
        .having(_ < 1)
        .statement === "SELECT `p1` FROM test GROUP BY p1 HAVING p1 < ?"
    )
    assert(query.select[Long](_.p1).orderBy(_.p1.desc).statement === "SELECT `p1` FROM test ORDER BY p1 DESC")
    assert(query.select[Long](_.p1).limit(10).statement === "SELECT `p1` FROM test LIMIT ?")
    assert(query.select[Long](_.p1).limit(10).offset(0).statement === "SELECT `p1` FROM test LIMIT ? OFFSET ?")
    assert(
      query
        .select[Long](_.p1)
        .where(_.p1 === query.select[Long](_.p1).where(_.p1 > 1))
        .statement === "SELECT `p1` FROM test WHERE p1 = (SELECT `p1` FROM test WHERE p1 > ?)"
    )
    assert(
      query
        .select[Long](_.p1)
        .where(v => (v.p1 >= query.select[Long](_.p1).where(_.p1 > 1)) || (v.p2 === query.select[String](_.p2)))
        .statement === "SELECT `p1` FROM test WHERE (p1 >= (SELECT `p1` FROM test WHERE p1 > ?) OR p2 = (SELECT `p2` FROM test))"
    )
    assert(
      query
        .join(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .select[(String, String)]((test, joinTest) => (test.p2, joinTest.p2))
        .statement === "SELECT x1.`p2`, x2.`p2` FROM test AS x1 JOIN join_test AS x2 ON x1.p1 = x2.p1"
    )
    assert(query.selectAll.statement === "SELECT `p1`, `p2`, `p3` FROM test")
  }
