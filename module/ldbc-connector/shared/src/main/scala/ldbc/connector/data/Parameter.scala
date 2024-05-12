/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.util.Arrays.copyOf
import java.time.*

import cats.syntax.all.*

import scodec.bits.BitVector
import scodec.codecs.*
import scodec.interop.cats.*

import ldbc.connector.data.Formatter.*

/**
 * A parameter to be used in a prepared statement.
 */
trait Parameter:

  /**
   * The column data type of this parameter.
   */
  def columnDataType: ColumnDataType

  /**
   * The SQL representation of this parameter.
   */
  def sql: Array[Char]

  /**
   * The binary representation of this parameter.
   */
  def encode: BitVector

  override def toString: String = new String(sql)

object Parameter:

  val none: Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_NULL
    override def sql:            Array[Char]    = "NULL".toCharArray
    override def encode:         BitVector      = BitVector.empty

  def boolean(value: Boolean): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TINY
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode:         BitVector      = uint8L.encode(if value then 1 else 0).require

  def byte(value: Byte): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TINY
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode:         BitVector      = uint8L.encode(value).require

  def short(value: Short): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_SHORT
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode:         BitVector      = uint16L.encode(value).require

  def int(value: Int): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_LONG
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode:         BitVector      = uint32L.encode(value).require

  def long(value: Long): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_LONGLONG
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode:         BitVector      = int64L.encode(value).require

  def bigInt(value: BigInt): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_STRING
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode: BitVector =
      val bytes = value.toString.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))

  def float(value: Float): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_FLOAT
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode:         BitVector      = floatL.encode(value).require

  def double(value: Double): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_DOUBLE
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode:         BitVector      = doubleL.encode(value).require

  def bigDecimal(value: BigDecimal): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_NEWDECIMAL
    override def sql:            Array[Char]    = value.toString.toCharArray
    override def encode: BitVector =
      val bytes = value.toString.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))

  def string(value: String): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_STRING
    override def sql:            Array[Char]    = ("'" + value + "'").toCharArray
    override def encode: BitVector =
      val bytes = value.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))

  def bytes(value: Array[Byte]): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_VAR_STRING
    override def sql:            Array[Char]    = ("0x" + BitVector.view(value).toHex).toCharArray
    override def encode: BitVector =
      BitVector(value.length) |+| BitVector(copyOf(value, value.length))

  def time(value: LocalTime): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TIME
    override def sql: Array[Char] =
      ("'" + timeFormatter((value.getNano / 1000).toString.length).format(value) + "'").toCharArray
    override def encode: BitVector =
      val hour   = value.getHour
      val minute = value.getMinute
      val second = value.getSecond
      val micro  = value.getNano / 1000
      (hour, minute, second, micro) match
        case (0, 0, 0, 0) => BitVector(0)
        case (_, _, _, 0) =>
          (for
            length     <- uint8L.encode(8)
            isNegative <- uint8L.encode(0)
            days       <- uint32L.encode(0)
            hour       <- uint8L.encode(hour)
            minute     <- uint8L.encode(minute)
            second     <- uint8L.encode(second)
          yield length |+| isNegative |+| days |+| hour |+| minute |+| second).require
        case _ =>
          (for
            length     <- uint8L.encode(12)
            isNegative <- uint8L.encode(0)
            days       <- uint32L.encode(0)
            hour       <- uint8L.encode(hour)
            minute     <- uint8L.encode(minute)
            second     <- uint8L.encode(second)
            nano       <- uint32L.encode(micro)
          yield length |+| isNegative |+| days |+| hour |+| minute |+| second |+| nano).require

  def date(value: LocalDate): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_DATE
    override def sql:            Array[Char]    = ("'" + localDateFormatter.format(value) + "'").toCharArray
    override def encode: BitVector =
      val year  = value.getYear
      val month = value.getMonthValue
      val day   = value.getDayOfMonth
      (year, month, day) match
        case (0, 0, 0) => BitVector(0)
        case _ =>
          (for
            length <- uint8L.encode(4)
            year   <- uint16L.encode(year)
            month  <- uint8L.encode(month)
            day    <- uint8L.encode(day)
          yield length |+| year |+| month |+| day).require

  def datetime(value: LocalDateTime): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TIMESTAMP
    override def sql: Array[Char] =
      ("'" + localDateTimeFormatter((value.getNano / 1000).toString.length).format(value) + "'").toCharArray
    override def encode: BitVector =
      val year   = value.getYear
      val month  = value.getMonthValue
      val day    = value.getDayOfMonth
      val hour   = value.getHour
      val minute = value.getMinute
      val second = value.getSecond
      val micro  = value.getNano / 1000
      (year, month, day, hour, minute, second, micro) match
        case (0, 0, 0, 0, 0, 0, 0) => BitVector(0)
        case (_, _, _, 0, 0, 0, 0) =>
          (for
            length <- uint8L.encode(4)
            year   <- uint16L.encode(year)
            month  <- uint8L.encode(month)
            day    <- uint8L.encode(day)
          yield length |+| year |+| month |+| day).require
        case (_, _, _, _, _, _, 0) =>
          (for
            length <- uint8L.encode(7)
            year   <- uint16L.encode(year)
            month  <- uint8L.encode(month)
            day    <- uint8L.encode(day)
            hour   <- uint8L.encode(hour)
            minute <- uint8L.encode(minute)
            second <- uint8L.encode(second)
          yield length |+| year |+| month |+| day |+| hour |+| minute |+| second).require
        case _ =>
          (for
            length <- uint8L.encode(11)
            year   <- uint16L.encode(year)
            month  <- uint8L.encode(month)
            day    <- uint8L.encode(day)
            hour   <- uint8L.encode(hour)
            minute <- uint8L.encode(minute)
            second <- uint8L.encode(second)
            micro  <- uint32L.encode(micro)
          yield length |+| year |+| month |+| day |+| hour |+| minute |+| second |+| micro).require

  def year(value: Year): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_SHORT
    override def sql:            Array[Char]    = ("'" + value.toString + "'").toCharArray
    override def encode:         BitVector      = uint16L.encode(value.getValue).require

  def parameter(value: String): Parameter = new Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_STRING
    override def sql:            Array[Char]    = value.toCharArray
    override def encode: BitVector =
      val bytes = value.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))
