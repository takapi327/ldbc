/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import ldbc.connector.data.Type

trait NumericCodecs:

  private def safe[A](f: String => A): String => Either[String, A] = s =>
    try Right(f(s))
    catch
      case _: NumberFormatException => Left(s"Invalid: $s")

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def bit(size: Int): Codec[Byte] = Codec.simple(_.toString, safe(_.toByte), Type.bit(size))
  val bit: Codec[Byte] = Codec.simple(_.toString, safe(_.toByte), Type.bit)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def tinyint(size: Int): Codec[Short] = Codec.simple(_.toString, safe(_.toShort), Type.tinyint(size))
  val tinyint: Codec[Short] = Codec.simple(_.toString, safe(_.toShort), Type.tinyint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def smallint(size: Int): Codec[Int] = Codec.simple(_.toString, safe(_.toInt), Type.smallint(size))
  val smallint: Codec[Int] = Codec.simple(_.toString, safe(_.toInt), Type.smallint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def mediumint(size: Int): Codec[Int] = Codec.simple(_.toString, safe(_.toInt), Type.mediumint(size))
  val mediumint: Codec[Int] = Codec.simple(_.toString, safe(_.toInt), Type.mediumint)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def int(size: Int): Codec[Int] = Codec.simple(_.toString, safe(_.toInt), Type.int(size))
  val int: Codec[Int] = Codec.simple(_.toString, safe(_.toInt), Type.int)

  @deprecated(
    "As of MySQL 8.0.17, the display width attribute for integer data types is deprecated. It will no longer be supported in future versions of MySQL.",
    "0.3.0"
  )
  def bigint(size: Int): Codec[Long] = Codec.simple(_.toString, safe(_.toLong), Type.bigint(size))
  val bigint: Codec[Long] = Codec.simple(_.toString, safe(_.toLong), Type.bigint)

  def decimal(accuracy: Int, scale: Int): Codec[BigDecimal] = Codec.simple(_.toString, safe(str => BigDecimal.decimal(str.toDouble)), Type.decimal(accuracy, scale))

  def float(accuracy: Int): Codec[Float] = Codec.simple(_.toString, safe(_.toFloat), Type.float(accuracy))
  def double(accuracy:  Int): Codec[Double] = Codec.simple(_.toString, safe(_.toDouble), Type.float(accuracy))

object numeric extends NumericCodecs
