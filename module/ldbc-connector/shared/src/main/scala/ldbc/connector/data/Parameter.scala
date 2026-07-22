/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.time.*
import java.util.Arrays.copyOf

import scodec.bits.BitVector
import scodec.codecs.*
import scodec.interop.cats.*

import cats.syntax.all.*

import ldbc.connector.data.Formatter.*

/**
 * A parameter to be used in a prepared statement, modeled as an ADT with one dedicated case per
 * MySQL type.
 *
 * A `Parameter` carries the column type and the binary (server prepared statement) encoding, plus a
 * `toString` that is a `sql_mode`-independent literal for display / diagnostics. It deliberately
 * does NOT expose a context-free SQL literal for strings: rendering a string into a client-side SQL
 * literal depends on the server `sql_mode` and must go through
 * [[ldbc.connector.net.protocol.QueryRenderer]], so escaping can never bypass the sql_mode-aware
 * logic.
 */
sealed trait Parameter:

  /** The column data type of this parameter. */
  def columnDataType: ColumnDataType

  /** The binary representation of this parameter (server prepared statement / binary protocol). */
  def encode: BitVector

object Parameter:

  val none: Parameter = NullParameter

  def boolean(value: Boolean): Parameter = BooleanParameter(value)

  def byte(value: Byte): Parameter = ByteParameter(value)

  def short(value: Short): Parameter = ShortParameter(value)

  def int(value: Int): Parameter = IntParameter(value)

  def long(value: Long): Parameter = LongParameter(value)

  def bigInt(value: BigInt): Parameter = BigIntParameter(value)

  def float(value: Float): Parameter = FloatParameter(value)

  def double(value: Double): Parameter = DoubleParameter(value)

  def bigDecimal(value: BigDecimal): Parameter = BigDecimalParameter(value)

  def string(value: String): Parameter = StringParameter(value)

  def bytes(value: Array[Byte]): Parameter = BytesParameter(value)

  def time(value: LocalTime): Parameter = TimeParameter(value)

  def date(value: LocalDate): Parameter = DateParameter(value)

  def datetime(value: LocalDateTime): Parameter = DateTimeParameter(value)

  def year(value: Year): Parameter = YearParameter(value)

  def parameter(value: String): Parameter = RawParameter(value)

  case object NullParameter extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_NULL
    override def encode:         BitVector      = BitVector.empty
    override def toString:       String         = "NULL"

  private[ldbc] final case class BooleanParameter(value: Boolean) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TINY
    override def encode:         BitVector      = uint8L.encode(if value then 1 else 0).require
    override def toString:       String         = value.toString

  private[ldbc] final case class ByteParameter(value: Byte) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TINY
    override def encode:         BitVector      = BitVector.fromByte(value)
    override def toString:       String         = value.toString

  private[ldbc] final case class ShortParameter(value: Short) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_SHORT
    override def encode:         BitVector      = uint16L.encode(value).require
    override def toString:       String         = value.toString

  private[ldbc] final case class IntParameter(value: Int) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_LONG
    override def encode:         BitVector      = uint32L.encode(value).require
    override def toString:       String         = value.toString

  private[ldbc] final case class LongParameter(value: Long) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_LONGLONG
    override def encode:         BitVector      = int64L.encode(value).require
    override def toString:       String         = value.toString

  private[ldbc] final case class BigIntParameter(value: BigInt) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_STRING
    override def encode: BitVector =
      val bytes = value.toString.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))
    override def toString: String = value.toString

  private[ldbc] final case class FloatParameter(value: Float) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_FLOAT
    override def encode:         BitVector      = floatL.encode(value).require
    override def toString:       String         = value.toString

  private[ldbc] final case class DoubleParameter(value: Double) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_DOUBLE
    override def encode:         BitVector      = doubleL.encode(value).require
    override def toString:       String         = value.toString

  private[ldbc] final case class BigDecimalParameter(value: BigDecimal) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_NEWDECIMAL
    override def encode: BitVector =
      val bytes = value.toString.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))
    override def toString: String = value.toString

  private[ldbc] final case class StringParameter(value: String) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_STRING
    override def encode: BitVector =
      val bytes = value.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))
    override def toString: String = s"'$value'"

  private[ldbc] final case class BytesParameter(value: Array[Byte]) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_VAR_STRING
    override def encode:         BitVector      =
      BitVector(value.length) |+| BitVector(copyOf(value, value.length))
    override def toString: String = "0x" + BitVector.view(value).toHex

  private[ldbc] final case class TimeParameter(value: LocalTime) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TIME
    override def toString:       String         =
      "'" + timeFormatter((value.getNano / 1000).toString.length).format(value) + "'"
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

  private[ldbc] final case class DateParameter(value: LocalDate) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_DATE
    override def toString:       String         = "'" + localDateFormatter.format(value) + "'"
    override def encode: BitVector =
      val year  = value.getYear
      val month = value.getMonthValue
      val day   = value.getDayOfMonth
      (year, month, day) match
        case (0, 0, 0) => BitVector(0)
        case _         =>
          (for
            length <- uint8L.encode(4)
            year   <- uint16L.encode(year)
            month  <- uint8L.encode(month)
            day    <- uint8L.encode(day)
          yield length |+| year |+| month |+| day).require

  private[ldbc] final case class DateTimeParameter(value: LocalDateTime) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_TIMESTAMP
    override def toString:       String         =
      "'" + localDateTimeFormatter((value.getNano / 1000).toString.length).format(value) + "'"
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

  private[ldbc] final case class YearParameter(value: Year) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_SHORT
    override def encode:         BitVector      = uint16L.encode(value.getValue).require
    override def toString:       String         = "'" + value.toString + "'"

  private[ldbc] final case class RawParameter(value: String) extends Parameter:
    override def columnDataType: ColumnDataType = ColumnDataType.MYSQL_TYPE_STRING
    override def encode: BitVector =
      val bytes = value.getBytes
      BitVector(bytes.length) |+| BitVector(copyOf(bytes, bytes.length))
    override def toString: String = value
