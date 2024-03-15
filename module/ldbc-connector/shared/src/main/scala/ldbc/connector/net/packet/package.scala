/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import java.time.*

import scodec.*
import scodec.codecs.*
import scodec.bits.BitVector

import ldbc.connector.data.ColumnDataType

package object packet:

  /**
   * A codec for a null-terminated string.
   */
  def nullTerminatedStringCodec: Codec[String] = new Codec[String]:
    def sizeBound: SizeBound = SizeBound.unknown

    def encode(value: String): Attempt[BitVector] =
      Attempt.successful(BitVector(value.getBytes(java.nio.charset.StandardCharsets.UTF_8) :+ 0.toByte))

    def decode(bits: BitVector): Attempt[DecodeResult[String]] =
      val bytes     = bits.bytes.takeWhile(_ != 0)
      val string    = new String(bytes.toArray, java.nio.charset.StandardCharsets.UTF_8)
      val remainder = bits.drop((bytes.size + 1) * 8) // +1 is a null character, so *8 is a byte to bit
      Attempt.successful(DecodeResult(string, remainder))

  /**
   * NULL bitmap, length = (num_params + 7) / 8
   *
   * @param columns
   *   The list of column data types.
   */
  def nullBitmap(columns: List[ColumnDataType]): BitVector =
    if columns.nonEmpty then
      val bitmap = columns.foldLeft(0) { (bitmap, param) =>
        (bitmap << 1) | (
          param match
            case ColumnDataType.MYSQL_TYPE_NULL => 1
            case _                              => 0
        )
      }
      uint8.encode(bitmap).require
    else BitVector.empty

  def time8: Decoder[LocalTime] =
    for
      isNegative  <- uint8L
      days        <- uint32L
      hour        <- uint8L
      minute      <- uint8L
      second      <- uint8L
    yield LocalTime.of(hour, minute, second)
  
  def time12: Decoder[LocalTime] =
    for
      isNegative  <- uint8L
      days        <- uint32L
      hour        <- uint8L
      minute      <- uint8L
      second      <- uint8L
      microsecond <- uint32L
    yield LocalTime.of(hour, minute, second, microsecond.toInt * 1000)

  /**
   * A codec for a local time.
   */
  def time: Decoder[Option[LocalTime]] =
    uint8L.flatMap {
      case 0  => Decoder.pure(None)
      case 8  => time8.map(Some(_))
      case 12 => time12.map(Some(_))
      case _  => throw new IllegalArgumentException("Invalid time length")
    }

  def timestamp4: Decoder[LocalDateTime] =
    for
      year  <- uint16L
      month <- uint8L
      day   <- uint8L
    yield LocalDateTime.of(year, month, day, 0, 0, 0, 0)

  def timestamp7: Decoder[LocalDateTime] =
    for
      year   <- uint16L
      month  <- uint8L
      day    <- uint8L
      hour   <- uint8L
      minute <- uint8L
      second <- uint8L
    yield LocalDateTime.of(year, month, day, hour, minute, second, 0)

  def timestamp11: Decoder[LocalDateTime] =
    for
      year        <- uint16L
      month       <- uint8L
      day         <- uint8L
      hour        <- uint8L
      minute      <- uint8L
      second      <- uint8L
      microsecond <- uint32L
    yield LocalDateTime.of(year, month, day, hour, minute, second, microsecond.toInt * 1000)

  def timestamp: Decoder[Option[LocalDateTime]] =
    uint8L.flatMap {
      case 0  => Decoder.pure(None)
      case 4  => timestamp4.map(Some(_))
      case 7  => timestamp7.map(Some(_))
      case 11 => timestamp11.map(Some(_))
      case _  => throw new IllegalArgumentException("Invalid timestamp length")
    }
