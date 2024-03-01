/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import cats.Eq
import cats.syntax.all.*

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
  val bit: Type = Type("bit")

  def tinyint(n: Int): Type = Type(s"tinyint($n)")
  val tinyint: Type = Type("tinyint")

  def smallint(n: Int): Type = Type(s"smallint($n)")
  val smallint: Type = Type("smallint")

  def mediumint(n: Int): Type = Type(s"mediumint($n)")
  val mediumint: Type = Type("mediumint")

  def int(n: Int): Type = Type(s"int($n)")
  val int: Type = Type("int")

  def bigint(n: Int): Type = Type(s"bigint($n)")
  val bigint: Type = Type("bigint")

  def decimal(accuracy: Int, scale: Int): Type = Type(s"decimal($accuracy, $scale)")

  def float(accuracy:  Int): Type = Type(s"float($accuracy)")
  def double(accuracy: Int): Type = Type(s"double($accuracy)")

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

  val date: Type = Type("date")

  def datetime(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Type = Type(s"datetime($fsp)")
  val datetime: Type = Type("datetime")

  def timestamp(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Type = Type(s"timestamp($fsp)")
  val timestamp: Type = Type("timestamp")

  def time(fsp: 0 | 1 | 2 | 3 | 4 | 5 | 6): Type = Type(s"time($fsp)")
  val time: Type = Type("time")

  def year(digit: 4): Type = Type(s"year($digit)")
  val year: Type = Type("year")

  val boolean: Type = Type("boolean")
