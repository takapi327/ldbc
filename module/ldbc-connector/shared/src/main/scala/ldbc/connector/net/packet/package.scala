/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import java.time.LocalTime

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

  /**
   * A codec for a local time.
   */
  def time: Decoder[LocalTime] =
    for
      hour        <- uint8
      minute      <- uint8
      second      <- uint8
      microsecond <- uint32L
    yield LocalTime.of(hour, minute, second, microsecond.toInt * 1000)
