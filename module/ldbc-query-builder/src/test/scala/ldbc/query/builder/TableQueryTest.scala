/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import cats.data.NonEmptyList

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.codec.Codec

import ldbc.query.builder.*

class TableQueryTest extends AnyFlatSpec:

  case class Test(p1: Long, p2: String, p3: Option[String]) derives Table
  case class JoinTest(p1: Long, p2: String, p3: Option[String]) derives Table
  case class JoinTest2(p1: Long, p2: String, p3: Option[String]) derives Table
  given Codec[Test]      = Codec.derived[Test]
  given Codec[JoinTest]  = Codec.derived[JoinTest]
  given Codec[JoinTest2] = Codec.derived[JoinTest2]

  private val query      = TableQuery[Test]
  private val joinQuery  = TableQuery[JoinTest]
  private val joinQuery2 = TableQuery[JoinTest2]

  it should "The select query statement generated from Table is equal to the specified query statement." in {
    assert(query.select(_.p1).statement === "SELECT test.`p1` FROM test")
    assert(query.select(_.p1).where(_.p1 > 1).statement === "SELECT test.`p1` FROM test WHERE test.`p1` > ?")
    assert(
      query
        .select(_.p1)
        .where(_.p1 >= 1)
        .and(_.p2 === "test")
        .statement === "SELECT test.`p1` FROM test WHERE test.`p1` >= ? AND test.`p2` = ?"
    )
    assert(
      query
        .select(_.p1)
        .whereOpt(test => Some(1).map(v => test.p1 >= v))
        .and(_.p2 === "test")
        .statement === "SELECT test.`p1` FROM test WHERE test.`p1` >= ? AND test.`p2` = ?"
    )
    val opt: Option[Int] = None
    assert(
      query
        .select(_.p1)
        .whereOpt(test => opt.map(v => test.p1 orMore v))
        .and(_.p2 === "test")
        .statement === "SELECT test.`p1` FROM test WHERE test.`p2` = ?"
    )
    assert(
      query
        .select(_.p1)
        .whereOpt(Some(1))((test, value) => test.p1 orMore value)
        .and(_.p2 === "test")
        .statement === "SELECT test.`p1` FROM test WHERE test.`p1` >= ? AND test.`p2` = ?"
    )
    assert(
      query
        .select(_.p1)
        .whereOpt[Int](None)((test, value) => test.p1 orMore value)
        .and(_.p2 === "test")
        .statement === "SELECT test.`p1` FROM test WHERE test.`p2` = ?"
    )
    assert(
      query
        .select(_.p1)
        .where(_.p1 >= 1)
        .and(_.p2 === "test", false)
        .statement === "SELECT test.`p1` FROM test WHERE test.`p1` >= ?"
    )
    assert(
      query
        .select(_.p1)
        .where(_.p1 >= 1)
        .andOpt(Some("test"))((test, value) => test.p2 _equals value)
        .statement === "SELECT test.`p1` FROM test WHERE test.`p1` >= ? AND test.`p2` = ?"
    )
    assert(
      query
        .select(_.p1)
        .where(_.p1 >= 1)
        .andOpt[String](None)((test, value) => test.p2 _equals value)
        .statement === "SELECT test.`p1` FROM test WHERE test.`p1` >= ?"
    )
    assert(
      query
        .select(_.p1)
        .whereOpt[Int](None)((test, value) => test.p1 orMore value)
        .andOpt[String](None)((test, value) => test.p2 _equals value)
        .statement === "SELECT test.`p1` FROM test"
    )
    assert(
      query
        .select(_.p1)
        .where(v => v.p1 > 1 || v.p2 === "test")
        .statement === "SELECT test.`p1` FROM test WHERE (test.`p1` > ? OR test.`p2` = ?)"
    )
    assert(
      query
        .select(_.p1)
        .where(v => v.p1 > 1 && v.p2 === "test")
        .or(_.p3 === "test")
        .statement === "SELECT test.`p1` FROM test WHERE (test.`p1` > ? AND test.`p2` = ?) OR test.`p3` = ?"
    )
    assert(
      query
        .select(_.p1)
        .where(v => v.p1 > 1 && v.p2 === "test")
        .or(_.p3 === "test", false)
        .statement === "SELECT test.`p1` FROM test WHERE (test.`p1` > ? AND test.`p2` = ?)"
    )
    assert(query.select(_.p1).groupBy(_.p1).statement === "SELECT test.`p1` FROM test GROUP BY `p1`")
    assert(
      query
        .select(_.p1)
        .groupBy(_.p1)
        .having(_.p1 < 1)
        .statement === "SELECT test.`p1` FROM test GROUP BY `p1` HAVING test.`p1` < ?"
    )
    assert(query.select(_.p1).orderBy(_.p1.desc).statement === "SELECT test.`p1` FROM test ORDER BY test.`p1` DESC")
    assert(query.select(_.p1).limit(10).statement === "SELECT test.`p1` FROM test LIMIT ?")
    assert(query.select(_.p1).limit(10).offset(0).statement === "SELECT test.`p1` FROM test LIMIT ? OFFSET ?")
    assert(
      query
        .select(_.p1)
        .where(_.p1 === query.select(_.p1).where(_.p1 > 1))
        .statement === "SELECT test.`p1` FROM test WHERE test.`p1` = (SELECT test.`p1` FROM test WHERE test.`p1` > ?)"
    )
    assert(
      query
        .select(_.p1)
        .where(v => (v.p1 >= query.select(_.p1).where(_.p1 > 1)) || (v.p2 === query.select(_.p2)))
        .statement === "SELECT test.`p1` FROM test WHERE (test.`p1` >= (SELECT test.`p1` FROM test WHERE test.`p1` > ?) OR test.`p2` = (SELECT test.`p2` FROM test))"
    )
    assert(
      query
        .join(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .select((test, joinTest) => test.p2 *: joinTest.p2)
        .statement === "SELECT test.`p2`, join_test.`p2` FROM test JOIN join_test ON test.`p1` = join_test.`p1`"
    )
    assert(
      query
        .leftJoin(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .select((test, joinTest) => test.p2 *: joinTest.p2)
        .statement === "SELECT test.`p2`, join_test.`p2` FROM test LEFT JOIN join_test ON test.`p1` = join_test.`p1`"
    )
    assert(
      query
        .rightJoin(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .select((test, joinTest) => test.p2 *: joinTest.p2)
        .statement === "SELECT test.`p2`, join_test.`p2` FROM test RIGHT JOIN join_test ON test.`p1` = join_test.`p1`"
    )
    assert(
      query
        .join(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .join(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test JOIN join_test ON test.`p1` = join_test.`p1` JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(
      query
        .join(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .leftJoin(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test JOIN join_test ON test.`p1` = join_test.`p1` LEFT JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(
      query
        .join(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .rightJoin(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test JOIN join_test ON test.`p1` = join_test.`p1` RIGHT JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(
      query
        .leftJoin(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .join(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test LEFT JOIN join_test ON test.`p1` = join_test.`p1` JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(
      query
        .rightJoin(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .join(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test RIGHT JOIN join_test ON test.`p1` = join_test.`p1` JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(
      query
        .rightJoin(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .leftJoin(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test RIGHT JOIN join_test ON test.`p1` = join_test.`p1` LEFT JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(
      query
        .leftJoin(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .leftJoin(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test LEFT JOIN join_test ON test.`p1` = join_test.`p1` LEFT JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(
      query
        .leftJoin(joinQuery)
        .on((test, joinTest) => test.p1 === joinTest.p1)
        .rightJoin(joinQuery2)
        .on((_, joinTest, joinTest2) => joinTest.p1 === joinTest2.p1)
        .select((test, joinTest, joinTest2) => test.p2 *: joinTest.p2 *: joinTest2.p2)
        .statement === "SELECT test.`p2`, join_test.`p2`, join_test2.`p2` FROM test LEFT JOIN join_test ON test.`p1` = join_test.`p1` RIGHT JOIN join_test2 ON join_test.`p1` = join_test2.`p1`"
    )
    assert(query.selectAll.statement === "SELECT test.`p1`, test.`p2`, test.`p3` FROM test")
  }

  it should "The insert query statement generated from Table is equal to the specified query statement." in {
    assert(
      query.insert((1L, "p2", Some("p3"))).statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?)"
    )
    assert(
      query
        .insert((1L, "p2", Some("p3")), (2L, "p2", None))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?),(?,?,?)"
    )
    assert(
      query
        .insertInto(v => v.p1 *: v.p2 *: v.p3)
        .values((1L, "p2", Some("p3")))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?)"
    )
    assert(
      query
        .insertInto(v => v.p1 *: v.p2 *: v.p3)
        .values(
          (1L, "p2", Some("p3")),
          (2L, "p2", None)
        )
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?),(?,?,?)"
    )
    assert(
      (query += Test(1L, "p2", Some("p3"))).statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?)"
    )
    assert(
      (query
        .++=(
          NonEmptyList.of(
            Test(1L, "p2", Some("p3")),
            Test(2L, "p2", None)
          )
        ))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?),(?,?,?)"
    )
    assert(
      query
        .insert((1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(t => t.p1 *: t.p2 *: t.p3)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE `p1` = VALUES(test.`p1`), `p2` = VALUES(test.`p2`), `p3` = VALUES(test.`p3`)"
    )
    assert(
      query
        .insert((1L, "p2", Some("p3")), (2L, "p2", None))
        .onDuplicateKeyUpdate(t => t.p1 *: t.p2 *: t.p3)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?),(?,?,?) ON DUPLICATE KEY UPDATE `p1` = VALUES(test.`p1`), `p2` = VALUES(test.`p2`), `p3` = VALUES(test.`p3`)"
    )
    assert(
      query
        .insert((1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(_.p1)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE `p1` = VALUES(test.`p1`)"
    )
    assert(
      (query += Test(1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(_.p1)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE `p1` = VALUES(test.`p1`)"
    )
    assert(
      (query += Test(1L, "p2", Some("p3")))
        .onDuplicateKeyUpdate(v => v.p1 *: v.p2 *: v.p3)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE `p1` = VALUES(test.`p1`), `p2` = VALUES(test.`p2`), `p3` = VALUES(test.`p3`)"
    )
    assert(
      (query ++= NonEmptyList.of(
        Test(1L, "p2", Some("p3")),
        Test(2L, "p2", None)
      )).onDuplicateKeyUpdate(_.p1)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?),(?,?,?) ON DUPLICATE KEY UPDATE `p1` = VALUES(test.`p1`)"
    )
    assert(
      (query ++= NonEmptyList.of(
        Test(1L, "p2", Some("p3")),
        Test(2L, "p2", None)
      )).onDuplicateKeyUpdate(v => v.p1 *: v.p2 *: v.p3)
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) VALUES (?,?,?),(?,?,?) ON DUPLICATE KEY UPDATE `p1` = VALUES(test.`p1`), `p2` = VALUES(test.`p2`), `p3` = VALUES(test.`p3`)"
    )
    assert(
      query
        .insertInto(v => v.p1 *: v.p2 *: v.p3)
        .select(joinQuery.select(v => v.p1 *: v.p2 *: v.p3))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) SELECT join_test.`p1`, join_test.`p2`, join_test.`p3` FROM join_test"
    )
    assert(
      query
        .insertInto(v => v.p1 *: v.p2 *: v.p3)
        .select(joinQuery.select(v => v.p1 *: v.p2 *: v.p3).where(_.p1 > 1))
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) SELECT join_test.`p1`, join_test.`p2`, join_test.`p3` FROM join_test WHERE join_test.`p1` > ?"
    )
    assert(
      query
        .insertInto(v => v.p1 *: v.p2 *: v.p3)
        .select(
          query
            .join(joinQuery)
            .on((t, j) => t.p1 === j.p1)
            .select((t, j) => t.p1 *: t.p2 *: j.p3)
            .where((_, j) => j.p1 > 1)
        )
        .statement === "INSERT INTO test (`p1`, `p2`, `p3`) SELECT test.`p1`, test.`p2`, join_test.`p3` FROM test JOIN join_test ON test.`p1` = join_test.`p1` WHERE join_test.`p1` > ?"
    )
  }

  it should "The update query statement generated from Table is equal to the specified query statement." in {
    assert(
      query
        .update(q => q.p1 *: q.p2 *: q.p3)(
          (1L, "p2", Some("p3"))
        )
        .statement === "UPDATE test SET `p1` = ?, `p2` = ?, `p3` = ?"
    )
    assert(
      query
        .update(q => q.p1 *: q.p2)(
          (1L, "p2")
        )
        .where(_.p1 === 1L)
        .statement === "UPDATE test SET `p1` = ?, `p2` = ? WHERE test.`p1` = ?"
    )
    assert(
      query
        .update(Test(1L, "p2", Some("p3")))
        .where(_.p1 === 1L)
        .statement === "UPDATE test SET `p1` = ?, `p2` = ?, `p3` = ? WHERE test.`p1` = ?"
    )
    assert(
      query
        .update(Test(1L, "p2", Some("p3")))
        .whereOpt(Some(1L))((test, value) => test.p1 === value)
        .statement === "UPDATE test SET `p1` = ?, `p2` = ?, `p3` = ? WHERE test.`p1` = ?"
    )
    assert(
      query
        .update(Test(1L, "p2", Some("p3")))
        .whereOpt[Long](None)((test, value) => test.p1 === value)
        .statement === "UPDATE test SET `p1` = ?, `p2` = ?, `p3` = ?"
    )
    assert(
      query
        .update(q => q.p1 *: q.p2 *: q.p3)(
          (1L, "p2", Some("p3"))
        )
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET `p1` = ?, `p2` = ?, `p3` = ? WHERE test.`p1` = ? LIMIT ?"
    )
    assert(
      query
        .update(Test(1L, "p2", Some("p3")))
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET `p1` = ?, `p2` = ?, `p3` = ? WHERE test.`p1` = ? LIMIT ?"
    )
    assert(
      query
        .update(_.p1)(1L)
        .set(_.p2, "p2", false)
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET `p1` = ? WHERE test.`p1` = ? LIMIT ?"
    )
    assert(
      query
        .update(_.p1)(1L)
        .set(_.p2, "p2", true)
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET `p1` = ?, `p2` = ? WHERE test.`p1` = ? LIMIT ?"
    )
    assert(
      query
        .update(q => q.p1 *: q.p3)(
          (1L, Some("p3"))
        )
        .set(_.p2, "p2", false)
        .where(_.p1 === 1L)
        .limit(1)
        .statement === "UPDATE test SET `p1` = ?, `p3` = ? WHERE test.`p1` = ? LIMIT ?"
    )
  }

  it should "The delete query statement generated from Table is equal to the specified query statement." in {
    assert(query.delete.statement === "DELETE FROM test")
    assert(query.delete.where(_.p1 === 1L).statement === "DELETE FROM test WHERE test.`p1` = ?")
    assert(
      query.delete
        .whereOpt(Some(1L))((test, value) => test.p1 === value)
        .statement === "DELETE FROM test WHERE test.`p1` = ?"
    )
    assert(query.delete.whereOpt[Long](None)((test, value) => test.p1 === value).statement === "DELETE FROM test")
    assert(query.delete.limit(1).statement === "DELETE FROM test LIMIT ?")
    assert(query.delete.where(_.p1 === 1L).limit(1).statement === "DELETE FROM test WHERE test.`p1` = ? LIMIT ?")
  }
