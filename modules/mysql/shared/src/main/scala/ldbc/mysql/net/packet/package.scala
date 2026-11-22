/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import java.time.*

import scala.annotation.tailrec

import scodec.*
import scodec.bits.BitVector
import scodec.codecs.*

import ldbc.mysql.data.ColumnDataType

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
   * A length encoded string is a string that is prefixed with length encoded integer describing the length of the string.
   * 
   * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_strings.html#sect_protocol_basic_dt_string_var
   * @return
   *   A codec for a length encoded string.
   */
  /**
   * Reads a length-encoded integer from `bytes` at `offset`.
   *
   * A first byte below `0xFC` is the value itself, and `0xFC`, `0xFD` and `0xFE` are followed by a 2-, 3- and 8-byte
   * little-endian integer, as MySQL Connector/J reads it. The 8-byte form is what the server sends for values of 2^24
   * and above, such as a large affected-row count or AUTO_INCREMENT value in an OK packet.
   *
   * @see https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_integers.html#sect_protocol_basic_dt_int_le
   * @return
   *   The value and the offset just after it.
   */
  private[ldbc] def readLengthEncodedInt(bytes: Array[Byte], offset: Int): (Long, Int) =
    (bytes(offset) & 0xff) match
      case 0xfc  => (readLittleEndian(bytes, offset + 1, 2), offset + 3)
      case 0xfd  => (readLittleEndian(bytes, offset + 1, 3), offset + 4)
      case 0xfe  => (readLittleEndian(bytes, offset + 1, 8), offset + 9)
      case first => (first.toLong, offset + 1)

  /**
   * Reads a `length`-byte little-endian integer from `bytes` at `offset`.
   */
  private def readLittleEndian(bytes: Array[Byte], offset: Int, length: Int): Long =
    (0 until length).foldLeft(0L) { (value, index) =>
      value | ((bytes(offset + index) & 0xffL) << (8 * index))
    }

  def lengthEncodedIntDecoder: Decoder[Long] =
    uint8.flatMap {
      case len if len <= 251 =>
        Decoder.pure(len)
      case 252 =>
        uint16L.map(_.toLong)
      case 253 =>
        uint24L.map(_.toLong)
      case 254 =>
        int64L
      case int =>
        fail(Err("Invalid length encoded integer: " + int))
    }

  val spaceDelimitedStringDecoder: Decoder[String] = (bits: BitVector) => {
    @tailrec
    def readUntilSpace(acc: Vector[Byte], remaining: BitVector): (Vector[Byte], BitVector) =
      if remaining.isEmpty then (acc, BitVector.empty)
      else
        val (nextByte, rest) = remaining.splitAt(8)
        if nextByte.bytes.head == 0x20 then (acc, rest) // Check space bytes
        else readUntilSpace(acc :+ nextByte.bytes.head, rest)

    val (collectedBytes, rest) = readUntilSpace(Vector.empty, bits)
    Attempt.successful(DecodeResult(new String(collectedBytes.toArray), rest))
  }

  /**
   * NULL bitmap, length = (num_params + 7) / 8
   *
   * @param columns
   *   The list of column data types.
   */
  def nullBitmap(columns: List[ColumnDataType]): BitVector =
    val bits = columns.map {
      case ColumnDataType.MYSQL_TYPE_NULL => true
      case _                              => false
    }
    val paddedBits = bits.padTo(((bits.length + 7) / 8) * 8, false)
    BitVector.bits(paddedBits.reverse)

  def time8: Decoder[LocalTime] =
    for
      isNegative <- uint8L
      days       <- uint32L
      hour       <- uint8L
      minute     <- uint8L
      second     <- uint8L
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
