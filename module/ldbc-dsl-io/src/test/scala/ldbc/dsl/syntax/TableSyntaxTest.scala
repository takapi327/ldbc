/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.syntax

import org.specs2.mutable.Specification

import cats.Id
import ldbc.core.*

object TableSyntaxTest extends Specification, TableSyntax[Id], ColumnSyntax[Id]:

  case class Test(p1: Long, p2: String, p3: Option[String])
  private val table = Table[Test]("test")(
    column("p1", BIGINT),
    column("p2", VARCHAR(255)),
    column("p3", VARCHAR(255))
  )

  "TableSyntax Test" should {
    "The query statement generated from Table is equal to the specified query statement." in {
      table.select[Column[Long]](_.p1).statement === "SELECT `p1` FROM test" and
        table.select[Column[Long]](_.p1).where(_.p1 > 1).statement === "SELECT `p1` FROM test WHERE p1 > ?" and
        table.select[Column[Long]](_.p1).where(_.p1 >= 1).and(_.p2 === "test").statement === "SELECT `p1` FROM test WHERE p1 >= ? AND p2 = ?" and
        table.select[Column[Long]](_.p1).where(v => v.p1 > 1 || v.p2 === "test").statement === "SELECT `p1` FROM test WHERE (p1 > ? OR p2 = ?)" and
        table.select[Column[Long]](_.p1).where(v => v.p1 > 1 && v.p2 === "test").or(_.p3 === "test").statement === "SELECT `p1` FROM test WHERE (p1 > ? AND p2 = ?) OR p3 = ?" and
        table.select[Column[Long]](_.p1).groupBy(p1 => p1).statement === "SELECT `p1` FROM test GROUP BY p1" and
        table.select[Column[Long]](_.p1).groupBy(p1 => p1).having(_ < 1).statement === "SELECT `p1` FROM test GROUP BY p1 HAVING p1 < ?" and
        table.select[Column[Long]](_.p1).orderBy(_.p1.desc).statement === "SELECT `p1` FROM test ORDER BY p1 DESC" and
        table.select[Column[Long]](_.p1).limit(10).statement === "SELECT `p1` FROM test LIMIT ?" and
        table.select[Column[Long]](_.p1).limit(10).offset(0).statement === "SELECT `p1` FROM test LIMIT ? OFFSET ?"
    }
  }
