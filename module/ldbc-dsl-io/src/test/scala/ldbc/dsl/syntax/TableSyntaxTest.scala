/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import org.scalatest.flatspec.AnyFlatSpec

import cats.Id
import ldbc.core.*

class TableSyntaxTest extends AnyFlatSpec, TableSyntax[Id], ColumnSyntax[Id]:

  case class Test(p1: Long, p2: String, p3: Option[String])
  private val table = Table[Test]("test")(
    column("p1", BIGINT),
    column("p2", VARCHAR(255)),
    column("p3", VARCHAR(255))
  )

  it should "The query statement generated from Table is equal to the specified query statement." in {
    assert(table.select[Column[Long]](_.p1).statement === "SELECT `p1` FROM test")
    assert(table.select[Column[Long]](_.p1).where(_.p1 > 1).statement === "SELECT `p1` FROM test WHERE p1 > ?")
    assert(table.select[Column[Long]](_.p1).where(_.p1 >= 1).and(_.p2 === "test").statement === "SELECT `p1` FROM test WHERE p1 >= ? AND p2 = ?")
    assert(table.select[Column[Long]](_.p1).where(v => v.p1 > 1 || v.p2 === "test").statement === "SELECT `p1` FROM test WHERE (p1 > ? OR p2 = ?)")
    assert(table.select[Column[Long]](_.p1).where(v => v.p1 > 1 && v.p2 === "test").or(_.p3 === "test").statement === "SELECT `p1` FROM test WHERE (p1 > ? AND p2 = ?) OR p3 = ?")
    assert(table.select[Column[Long]](_.p1).groupBy(p1 => p1).statement === "SELECT `p1` FROM test GROUP BY p1")
    assert(table.select[Column[Long]](_.p1).groupBy(p1 => p1).having(_ < 1).statement === "SELECT `p1` FROM test GROUP BY p1 HAVING p1 < ?")
    assert(table.select[Column[Long]](_.p1).orderBy(_.p1.desc).statement === "SELECT `p1` FROM test ORDER BY p1 DESC")
    assert(table.select[Column[Long]](_.p1).limit(10).statement === "SELECT `p1` FROM test LIMIT ?")
    assert(table.select[Column[Long]](_.p1).limit(10).offset(0).statement === "SELECT `p1` FROM test LIMIT ? OFFSET ?")
    assert(table.select[Column[Long]](_.p1).where(_.p1 === table.select[Column[Long]](_.p1).where(_.p1 > 1)).statement === "SELECT `p1` FROM test WHERE p1 = (SELECT `p1` FROM test WHERE p1 > ?)")
    assert(table.select[Column[Long]](_.p1).where(v => (v.p1 >= table.select[Column[Long]](_.p1).where(_.p1 > 1)) || (v.p2 === table.select[Column[String]](_.p2))).statement === "SELECT `p1` FROM test WHERE (p1 >= (SELECT `p1` FROM test WHERE p1 > ?) OR p2 = (SELECT `p2` FROM test))")
  }
