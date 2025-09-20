/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import cats.syntax.all.*

import ldbc.connector.*

class TypeTest extends FTestPlatform:

  test("bit type creation") {
    assertEquals(Type.bit.name, "bit")
    assertEquals(Type.bit(1).name, "bit(1)")
    assertEquals(Type.bit(8).name, "bit(8)")
    assertEquals(Type.bit(64).name, "bit(64)")
  }

  test("tinyint type creation") {
    assertEquals(Type.tinyint.name, "tinyint")
    assertEquals(Type.tinyint(3).name, "tinyint(3)")
    assertEquals(Type.tinyint(5).name, "tinyint(5)")

    assertEquals(Type.utinyint.name, "tinyint unsigned")
    assertEquals(Type.utinyint(3).name, "tinyint(3) unsigned")
    assertEquals(Type.utinyint(5).name, "tinyint(5) unsigned")
  }

  test("smallint type creation") {
    assertEquals(Type.smallint.name, "smallint")
    assertEquals(Type.smallint(5).name, "smallint(5)")
    assertEquals(Type.smallint(10).name, "smallint(10)")

    assertEquals(Type.usmallint.name, "smallint unsigned")
    assertEquals(Type.usmallint(5).name, "smallint(5) unsigned")
    assertEquals(Type.usmallint(10).name, "smallint(10) unsigned")
  }

  test("mediumint type creation") {
    assertEquals(Type.mediumint.name, "mediumint")
    assertEquals(Type.mediumint(7).name, "mediumint(7)")
    assertEquals(Type.mediumint(9).name, "mediumint(9)")

    assertEquals(Type.umediumint.name, "mediumint unsigned")
    assertEquals(Type.umediumint(7).name, "mediumint(7) unsigned")
    assertEquals(Type.umediumint(9).name, "mediumint(9) unsigned")
  }

  test("int type creation") {
    assertEquals(Type.int.name, "int")
    assertEquals(Type.int(10).name, "int(10)")
    assertEquals(Type.int(11).name, "int(11)")

    assertEquals(Type.uint.name, "int unsigned")
    assertEquals(Type.uint(10).name, "int(10) unsigned")
    assertEquals(Type.uint(11).name, "int(11) unsigned")
  }

  test("bigint type creation") {
    assertEquals(Type.bigint.name, "bigint")
    assertEquals(Type.bigint(19).name, "bigint(19)")
    assertEquals(Type.bigint(20).name, "bigint(20)")

    assertEquals(Type.ubigint.name, "bigint unsigned")
    assertEquals(Type.ubigint(19).name, "bigint(19) unsigned")
    assertEquals(Type.ubigint(20).name, "bigint(20) unsigned")
  }

  test("decimal type creation") {
    assertEquals(Type.decimal().name, "decimal(10, 0)")
    assertEquals(Type.decimal(5).name, "decimal(5, 0)")
    assertEquals(Type.decimal(10, 2).name, "decimal(10, 2)")
    assertEquals(Type.decimal(38, 10).name, "decimal(38, 10)")
  }

  test("floating point type creation") {
    assertEquals(Type.float.name, "float")
    assertEquals(Type.double.name, "double")
  }

  test("char and varchar type creation") {
    assertEquals(Type.char(1).name, "char(1)")
    assertEquals(Type.char(10).name, "char(10)")
    assertEquals(Type.char(255).name, "char(255)")

    assertEquals(Type.varchar(1).name, "varchar(1)")
    assertEquals(Type.varchar(100).name, "varchar(100)")
    assertEquals(Type.varchar(65535).name, "varchar(65535)")
  }

  test("binary and varbinary type creation") {
    assertEquals(Type.binary(1).name, "binary(1)")
    assertEquals(Type.binary(10).name, "binary(10)")
    assertEquals(Type.binary(255).name, "binary(255)")

    assertEquals(Type.varbinary(1).name, "varbinary(1)")
    assertEquals(Type.varbinary(100).name, "varbinary(100)")
    assertEquals(Type.varbinary(65535).name, "varbinary(65535)")
  }

  test("blob type creation") {
    assertEquals(Type.tinyblob.name, "tinyblob")
    assertEquals(Type.blob.name, "blob")
    assertEquals(Type.mediumblob.name, "mediumblob")
    assertEquals(Type.longblob.name, "longblob")
  }

  test("text type creation") {
    assertEquals(Type.tinytext.name, "tinytext")
    assertEquals(Type.text.name, "text")
    assertEquals(Type.mediumtext.name, "mediumtext")
    assertEquals(Type.longtext.name, "longtext")
  }

  test("enum type creation") {
    assertEquals(Type.`enum`(List("'a'")).name, "enum('a')")
    assertEquals(Type.`enum`(List("'a'", "'b'")).name, "enum('a','b')")
    assertEquals(Type.`enum`(List("'small'", "'medium'", "'large'")).name, "enum('small','medium','large')")
    assertEquals(Type.`enum`(List.empty).name, "enum()")
  }

  test("set type creation") {
    assertEquals(Type.set(List("'a'")).name, "set('a')")
    assertEquals(Type.set(List("'a'", "'b'")).name, "set('a','b')")
    assertEquals(Type.set(List("'read'", "'write'", "'execute'")).name, "set('read','write','execute')")
    assertEquals(Type.set(List.empty).name, "set()")
  }

  test("json type creation") {
    assertEquals(Type.json.name, "json")
  }

  test("date type creation") {
    assertEquals(Type.date.name, "date")
  }

  test("datetime type creation") {
    assertEquals(Type.datetime.name, "datetime")
    assertEquals(Type.datetime(0).name, "datetime(0)")
    assertEquals(Type.datetime(1).name, "datetime(1)")
    assertEquals(Type.datetime(2).name, "datetime(2)")
    assertEquals(Type.datetime(3).name, "datetime(3)")
    assertEquals(Type.datetime(4).name, "datetime(4)")
    assertEquals(Type.datetime(5).name, "datetime(5)")
    assertEquals(Type.datetime(6).name, "datetime(6)")
  }

  test("timestamp type creation") {
    assertEquals(Type.timestamp.name, "timestamp")
    assertEquals(Type.timestamp(0).name, "timestamp(0)")
    assertEquals(Type.timestamp(1).name, "timestamp(1)")
    assertEquals(Type.timestamp(2).name, "timestamp(2)")
    assertEquals(Type.timestamp(3).name, "timestamp(3)")
    assertEquals(Type.timestamp(4).name, "timestamp(4)")
    assertEquals(Type.timestamp(5).name, "timestamp(5)")
    assertEquals(Type.timestamp(6).name, "timestamp(6)")
  }

  test("time type creation") {
    assertEquals(Type.time.name, "time")
    assertEquals(Type.time(0).name, "time(0)")
    assertEquals(Type.time(1).name, "time(1)")
    assertEquals(Type.time(2).name, "time(2)")
    assertEquals(Type.time(3).name, "time(3)")
    assertEquals(Type.time(4).name, "time(4)")
    assertEquals(Type.time(5).name, "time(5)")
    assertEquals(Type.time(6).name, "time(6)")
  }

  test("year type creation") {
    assertEquals(Type.year.name, "year")
    assertEquals(Type.year(4).name, "year(4)")
  }

  test("boolean type creation") {
    assertEquals(Type.boolean.name, "boolean")
  }

  test("Type case class basic functionality") {
    val customType = Type("custom_type")
    assertEquals(customType.name, "custom_type")

    // Test toString
    assertEquals(customType.toString, "Type(custom_type)")
  }

  test("Type equality using Eq instance") {
    val type1 = Type("int")
    val type2 = Type("int")
    val type3 = Type("varchar(255)")

    // Test Eq instance
    assert(type1 === type2)
    assert(type1 =!= type3)

    // Test with factory methods
    assert(Type.int === Type.int)
    assert(Type.int =!= Type.uint)
    assert(Type.varchar(10) === Type.varchar(10))
    assert(Type.varchar(10) =!= Type.varchar(20))

    // Test equality with same parameters
    assert(Type.decimal(10, 2) === Type.decimal(10, 2))
    assert(Type.decimal(10, 2) =!= Type.decimal(10, 3))
  }

  test("Type with special characters") {
    // Test with quotes in enum/set
    val enumType = Type.`enum`(List("'option1'", "'option 2'", "'option-3'"))
    assertEquals(enumType.name, "enum('option1','option 2','option-3')")

    val setType = Type.set(List("'flag_1'", "'flag 2'", "'flag-3'"))
    assertEquals(setType.name, "set('flag_1','flag 2','flag-3')")
  }

  test("Type edge cases") {
    // Large display width
    assertEquals(Type.int(255).name, "int(255)")
    assertEquals(Type.bigint(255).name, "bigint(255)")

    // Maximum decimal precision
    assertEquals(Type.decimal(65, 30).name, "decimal(65, 30)")

    // Empty collections
    assertEquals(Type.`enum`(Nil).name, "enum()")
    assertEquals(Type.set(Nil).name, "set()")

    // Single element collections
    assertEquals(Type.`enum`(List("'only'")).name, "enum('only')")
    assertEquals(Type.set(List("'only'")).name, "set('only')")
  }

  test("Type composition and complex scenarios") {
    // Creating types that might be used in real scenarios
    val idType          = Type.bigint(20)
    val nameType        = Type.varchar(100)
    val statusType      = Type.`enum`(List("'active'", "'inactive'", "'pending'"))
    val permissionsType = Type.set(List("'read'", "'write'", "'delete'"))
    val createdAtType   = Type.timestamp(6)
    val metadataType    = Type.json

    assertEquals(idType.name, "bigint(20)")
    assertEquals(nameType.name, "varchar(100)")
    assertEquals(statusType.name, "enum('active','inactive','pending')")
    assertEquals(permissionsType.name, "set('read','write','delete')")
    assertEquals(createdAtType.name, "timestamp(6)")
    assertEquals(metadataType.name, "json")
  }
