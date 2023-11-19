/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import org.scalatest.flatspec.AnyFlatSpec

import cats.Id

import ldbc.core.*

class TableQueryTest extends AnyFlatSpec:

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

  case class JoinTest2(p1: Long, p2: String, p3: Option[String])

  private val joinTable2 = Table[JoinTest2]("join_test2")(
    column("p1", BIGINT),
    column("p2", VARCHAR(255)),
    column("p3", VARCHAR(255))
  )

  private val query      = TableQuery[Id, Test](table)
  private val joinQuery  = TableQuery[Id, JoinTest](joinTable)
  private val joinQuery2 = TableQuery[Id, JoinTest2](joinTable2)

  it should "The select query statement generated from Table is equal to the specified query statement." in {
    assert(query.select(_.p1).statement === "SELECT `p1` FROM test")
    assert(query.select(_.p1).where(_.p1 > 1).statement === "SELECT `p1` FROM test WHERE p1 > ?")
    assert(
      query
        .select(_.p1)
        .where(_.p1 >= 1)
        .and(_.p2 === "test")
        .statement === "SELECT `p1` FROM test WHERE p1 >= ? AND p2 = ?"
    )
    assert(
      query
        .select(_.p1)
        .where(v => v.p1 > 1 || v.p2 === "test")
        .statement === "SELECT `p1` FROM test WHERE (p1 > ? OR p2 = ?)"
    )
    assert(
      query
        .select(_.p1)
        .where(v => v.p1 > 1 && v.p2 === "test")
        .or(_.p3 === "test")
        .statement === "SELECT `p1` FROM test WHERE (p1 > ? AND p2 = ?) OR p3 = ?"
    )
    assert(query.select(_.p1).groupBy(p1 => p1).statement === "SELECT `p1` FROM test GROUP BY p1")
    assert(
      query
        .select(_.p1)
        .groupBy(p1 => p1)
        .having(_ < 1)
        .statement === "SELECT `p1` FROM test GROUP BY p1 HAVING p1 < ?"
    )
    assert(query.select(_.p1).orderBy(_.p1.desc).statement === "SELECT `p1` FROM test ORDER BY p1 DESC")
    assert(query.select(_.p1).limit(10).statement === "SELECT `p1` FROM test LIMIT ?")
    assert(query.select(_.p1).limit(10).offset(0).statement === "SELECT `p1` FROM test LIMIT ? OFFSET ?")
    assert(
      query
        .select(_.p1)
        .where(_.p1 === query.select(_.p1).where(_.p1 > 1))
        .statement === "SELECT `p1` FROM test WHERE p1 = (SELECT `p1` FROM test WHERE p1 > ?)"
    )
    assert(
      query
        .select(_.p1)
        .where(v => (v.p1 >= query.select(_.p1).where(_.p1 > 1)) || (v.p2 === query.select(_.p2)))
        .statement === "SELECT `p1` FROM test WHERE (p1 >= (SELECT `p1` FROM test WHERE p1 > ?) OR p2 = (SELECT `p2` FROM test))"
    )
    assert(
      query
        .join(joinQuery)((test, joinTest) => test.p1 === joinTest.p1)
        .select((test, joinTest) => (test.p2, joinTest.p2))
        .statement === "SELECT test.`p2`, join_test.`p2` FROM test JOIN join_test ON test.p1 = join_test.p1"
    )
    assert(
      query
        .leftJoin(joinQuery)((test, joinTest) => test.p1 === joinTest.p1)
        .select((test, joinTest) => (test.p2, joinTest.p2))
        .statement === "SELECT test.`p2`, join_test.`p2` FROM test LEFT JOIN join_test ON test.p1 = join_test.p1"
    )
    assert(
      query
        .rightJoin(joinQuery)((test, joinTest) => test.p1 === joinTest.p1)
        .select((test, joinTest) => (test.p2, joinTest.p2))
        .statement === "SELECT test.`p2`, join_test.`p2` FROM test RIGHT JOIN join_test ON test.p1 = join_test.p1"
    )
    assert(
      query
        .join(joinQuery)((test, joinTest) => test.p1 === joinTest.p1)
        .join(joinQuery2)((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => (test.p2, joinTest.p2, joinTest2.p2))
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test JOIN join_test ON test.p1 = join_test.p1 JOIN join_test2 ON join_test.p1 = join_test2.p1"
    )
    assert(
      query
        .join(joinQuery)((test, joinTest) => test.p1 === joinTest.p1)
        .leftJoin(joinQuery2)((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => (test.p2, joinTest.p2, joinTest2.p2))
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test JOIN join_test ON test.p1 = join_test.p1 LEFT JOIN join_test2 ON join_test.p1 = join_test2.p1"
    )
    assert(
      query
        .join(joinQuery)((test, joinTest) => test.p1 === joinTest.p1)
        .rightJoin(joinQuery2)((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => (test.p2, joinTest.p2, joinTest2.p2))
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test JOIN join_test ON test.p1 = join_test.p1 RIGHT JOIN join_test2 ON join_test.p1 = join_test2.p1"
    )
    assert(
      query
        .leftJoin(joinQuery)((test, joinTest) => test.p1 === joinTest.p1)
        .rightJoin(joinQuery2)((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => (test.p2, joinTest.p2, joinTest2.p2))
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test LEFT JOIN join_test ON test.p1 = join_test.p1 RIGHT JOIN join_test2 ON join_test.p1 = join_test2.p1"
    )
    assert(query.selectAll.statement === "SELECT `p1`, `p2`, `p3` FROM test")
  }

  it should "The insert query statement generated from Table is equal to the specified query statement." in {
    assert(
      query.insert((1L, "p2", Some("p3"))).statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?)"
    )
    assert(
      query
        .insert((1L, "p2", Some("p3")), (2L, "p2", None))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?), (?, ?, ?)"
    )
    assert(
      query
        .insertInto(v => (v.p1, v.p2, v.p3))
        .values((1L, "p2", Some("p3")))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?)"
    )
    assert(
      query
        .insertInto(v => (v.p1, v.p2, v.p3))
        .values(List((1L, "p2", Some("p3")), (2L, "p2", None)))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?), (?, ?, ?)"
    )
    assert(
      (query += Test(1L, "p2", Some("p3"))).statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?)"
    )
    assert(
      (query ++= List(
        Test(1L, "p2", Some("p3")),
        Test(2L, "p2", None)
      )).statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?), (?, ?, ?)"
    )
    assert(
      query
        .insertOrUpdate((1L, "p2", Some("p3")))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`, `p2` = new_test.`p2`, `p3` = new_test.`p3`"
    )
    assert(
      query
        .insertOrUpdates(
          List(
            Test(1L, "p2", Some("p3")),
            Test(2L, "p2", None)
          )
        )
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?), (?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`, `p2` = new_test.`p2`, `p3` = new_test.`p3`"
    )
    assert(
      query
        .insert((1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(_.p1)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`"
    )
    assert(
      query
        .insert((1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(v => (v.p1, v.p2, v.p3))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`, `p2` = new_test.`p2`, `p3` = new_test.`p3`"
    )
    assert(
      (query += Test(1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(_.p1)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`"
    )
    assert(
      (query += Test(1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(v => (v.p1, v.p2, v.p3))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`, `p2` = new_test.`p2`, `p3` = new_test.`p3`"
    )
    assert(
      (query ++= List(
        Test(1L, "p2", Some("p3")),
        Test(2L, "p2", None)
      )).onDuplicateKeyUpdate(_.p1)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?), (?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`"
    )
    assert(
      (query ++= List(
        Test(1L, "p2", Some("p3")),
        Test(2L, "p2", None)
      )).onDuplicateKeyUpdate(v => (v.p1, v.p2, v.p3))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES(?, ?, ?), (?, ?, ?) AS new_test ON DUPLICATE KEY UPDATE `p1` = new_test.`p1`, `p2` = new_test.`p2`, `p3` = new_test.`p3`"
    )
  }

  it should "The update query statement generated from Table is equal to the specified query statement." in {
    assert(
      query
        .update("p1", 1L)
        .set("p2", "p2")
        .set("p3", Some("p3"))
        .where(_.p1 === 1L)
        .statement === "UPDATE test SET p1 = ?, p2 = ?, p3 = ? WHERE p1 = ?"
    )
    assert(
      query
        .update("p1", 1L)
        .set("p2", "p2")
        .where(_.p1 === 1L)
        .statement === "UPDATE test SET p1 = ?, p2 = ? WHERE p1 = ?"
    )
    assert(
      query
        .update(Test(1L, "p2", Some("p3")))
        .where(_.p1 === 1L)
        .statement === "UPDATE test SET p1 = ?, p2 = ?, p3 = ? WHERE p1 = ?"
    )
    assert(
      query
        .update("p1", 1L)
        .set("p2", "p2")
        .set("p3", Some("p3"))
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET p1 = ?, p2 = ?, p3 = ? WHERE p1 = ? LIMIT ?"
    )
    assert(
      query
        .update(Test(1L, "p2", Some("p3")))
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET p1 = ?, p2 = ?, p3 = ? WHERE p1 = ? LIMIT ?"
    )
    assert(
      query
        .update("p1", 1L)
        .set("p2", "p2", false)
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET p1 = ? WHERE p1 = ? LIMIT ?"
    )
    assert(
      query
        .update("p1", 1L)
        .set("p2", "p2", true)
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET p1 = ?, p2 = ? WHERE p1 = ? LIMIT ?"
    )
    assert(
      query
        .update("p1", 1L)
        .set("p2", "p2", false)
        .set("p3", Some("p3"))
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET p1 = ?, p3 = ? WHERE p1 = ? LIMIT ?"
    )
  }

  it should "The delete query statement generated from Table is equal to the specified query statement." in {
    assert(query.delete.statement === "DELETE FROM test")
    assert(query.delete.where(_.p1 === 1L).statement === "DELETE FROM test WHERE p1 = ?")
    assert(query.delete.limit(1).statement === "DELETE FROM test LIMIT ?")
    assert(query.delete.where(_.p1 === 1L).limit(1).statement === "DELETE FROM test WHERE p1 = ? LIMIT ?")
  }
