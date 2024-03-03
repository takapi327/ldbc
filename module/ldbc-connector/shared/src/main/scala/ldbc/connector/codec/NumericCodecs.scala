/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import ldbc.connector.data.Type

trait NumericCodecs:

  private def safe[A](`type`: Type)(f: String => A): String => Either[String, A] = s =>
    try Right(f(s))
    catch case ex: NumberFormatException => Left(s"Invalid ${`type`.name} $s ${ ex.getMessage }")

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def bit(size: Int): Codec[Byte] = Codec.simple(_.toString, safe(Type.bit)(_.toByte), Type.bit(size))
  val bit: Codec[Byte] = Codec.simple(_.toString, safe(Type.bit)(_.toByte), Type.bit)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def tinyint(size: Int): Codec[Byte] = Codec.simple(_.toString, safe(Type.tinyint)(_.toByte), Type.tinyint(size))
  val tinyint: Codec[Byte] = Codec.simple(_.toString, safe(Type.tinyint)(_.toByte), Type.tinyint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def utinyint(size: Int): Codec[Short] = Codec.simple(_.toString, safe(Type.utinyint)(_.toShort), Type.utinyint(size))
  val utinyint: Codec[Short] = Codec.simple(_.toString, safe(Type.utinyint)(str =>
    val short = str.toShort
    if 0 <= short && short <= 255 then short
    else throw new NumberFormatException("can only handle the range 0 ~ 255")
  ), Type.utinyint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def smallint(size: Int): Codec[Short] = Codec.simple(_.toString, safe(Type.smallint)(_.toShort), Type.smallint(size))
  val smallint: Codec[Short] = Codec.simple(_.toString, safe(Type.smallint)(_.toShort), Type.smallint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def usmallint(size: Int): Codec[Int] = Codec.simple(_.toString, safe(Type.usmallint)(_.toInt), Type.usmallint(size))
  val usmallint: Codec[Int] = Codec.simple(_.toString, safe(Type.usmallint)(str => 
    val int = str.toInt
    if 0 <= int && int <= 65535 then int
    else throw new NumberFormatException("can only handle the range 0 ~ 65535")
  ), Type.usmallint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def mediumint(size: Int): Codec[Int] = Codec.simple(_.toString, safe(Type.mediumint)(_.toInt), Type.mediumint(size))
  val mediumint: Codec[Int] = Codec.simple(_.toString, safe(Type.mediumint)(_.toInt), Type.mediumint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def int(size: Int): Codec[Int] = Codec.simple(_.toString, safe(Type.int)(_.toInt), Type.int(size))
  val int: Codec[Int] = Codec.simple(_.toString, safe(Type.int)(_.toInt), Type.int)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def uint(size: Int): Codec[Long] = Codec.simple(_.toString, safe(Type.uint)(_.toLong), Type.uint(size))
  val uint: Codec[Long] = Codec.simple(_.toString, safe(Type.uint)(_.toLong), Type.uint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def bigint(size: Int): Codec[Long] = Codec.simple(_.toString, safe(Type.bigint)(_.toLong), Type.bigint(size))
  val bigint: Codec[Long] = Codec.simple(_.toString, safe(Type.bigint)(_.toLong), Type.bigint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def ubigint(size: Int): Codec[BigInt] = Codec.simple(_.toString, safe(Type.ubigint)(BigInt(_)), Type.ubigint(size))
  val ubigint: Codec[BigInt] = Codec.simple(_.toString, safe(Type.ubigint)(BigInt(_)), Type.ubigint)

  def decimal(accuracy: Int, scale: Int): Codec[BigDecimal] =
    Codec.simple(_.toString, safe(Type.decimal(accuracy, scale))(str => BigDecimal.decimal(str.toDouble)), Type.decimal(accuracy, scale))

  def float(accuracy:  Int): Codec[Float]  = Codec.simple(_.toString, safe(Type.float(accuracy))(_.toFloat), Type.float(accuracy))
  def double(accuracy: Int): Codec[Double] = Codec.simple(_.toString, safe(Type.float(accuracy))(_.toDouble), Type.float(accuracy))

object numeric extends NumericCodecs
