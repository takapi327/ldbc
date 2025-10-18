/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import cats.Eq

/**
 * Represents a MySQL data type.
 * 
 * @param name
 *   The name of the type.
 */
final case class Type(name: String)

object Type:
  given EqType: Eq[Type] = Eq.fromUniversalEquals

  def bit(size: Int): Type = Type(s"bit($size)")
  val bit:            Type = Type("bit")

  def tinyint(n: Int): Type = Type(s"tinyint($n)")
  val tinyint:         Type = Type("tinyint")

  def utinyint(n: Int): Type = Type(s"tinyint($n) unsigned")
  val utinyint:         Type = Type("tinyint unsigned")

  def smallint(n: Int): Type = Type(s"smallint($n)")
  val smallint:         Type = Type("smallint")

  def usmallint(n: Int): Type = Type(s"smallint($n) unsigned")
  val usmallint:         Type = Type("smallint unsigned")

  def mediumint(n: Int): Type = Type(s"mediumint($n)")
  val mediumint:         Type = Type("mediumint")

  def umediumint(n: Int): Type = Type(s"mediumint($n) unsigned")
  val umediumint:         Type = Type("mediumint unsigned")

  def int(n: Int): Type = Type(s"int($n)")
  val int:         Type = Type("int")

  def uint(n: Int): Type = Type(s"int($n) unsigned")
  val uint:         Type = Type("int unsigned")

  def bigint(n: Int): Type = Type(s"bigint($n)")
  val bigint:         Type = Type("bigint")

  def ubigint(n: Int): Type = Type(s"bigint($n) unsigned")
  val ubigint:         Type = Type("bigint unsigned")

  def decimal(accuracy: Int = 10, scale: Int = 0): Type = Type(s"decimal($accuracy, $scale)")

  val float:  Type = Type("float")
  val double: Type = Type("double")

  def char(length:    Int): Type = Type(s"char($length)")
  def varchar(length: Int): Type = Type(s"varchar($length)")

  def binary(length:    Int): Type = Type(s"binary($length)")
  def varbinary(length: Int): Type = Type(s"varbinary($length)")

  val tinyblob:   Type = Type("tinyblob")
  val blob:       Type = Type("blob")
  val mediumblob: Type = Type("mediumblob")
  val longblob:   Type = Type("longblob")

  val tinytext:   Type = Type("tinytext")
  val text:       Type = Type("text")
  val mediumtext: Type = Type("mediumtext")
  val longtext:   Type = Type("longtext")

  def `enum`(values: List[String]): Type = Type(s"enum(${ values.mkString(",") })")

  def set(values: List[String]): Type = Type(s"set(${ values.mkString(",") })")

  val json: Type = Type("json")

  val date: Type = Type("date")

  def datetime(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Type = Type(s"datetime($fsp)")
  val datetime:                                 Type = Type("datetime")

  def timestamp(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Type = Type(s"timestamp($fsp)")
  val timestamp:                                 Type = Type("timestamp")

  def time(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Type = Type(s"time($fsp)")
  val time:                                 Type = Type("time")

  def year(digit: 4): Type = Type(s"year($digit)")
  val year:           Type = Type("year")

  val boolean: Type = Type("boolean")
