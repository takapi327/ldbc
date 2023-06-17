/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime, OffsetTime, OffsetDateTime, ZonedDateTime }
import java.time.Year as JYear

import scala.compiletime.{ error, erasedValue }

import ldbc.core.DataType.*

/** A set of methods for constructing DataType
  */
trait DataTypes:

  transparent inline private def isOptional[T] = inline erasedValue[T] match
    case _: Option[?] => true
    case _ => false

  /** ===== List of Numeric Data Types ===== */

  inline def BIT[
    T <: Byte | Short | Int | Long | Float | Double | BigDecimal |
      Option[Byte | Short | Int | Long | Float | Double | BigDecimal]
  ](inline length: Int): Bit[T] =
    inline if length < 1 || length > 64 then error("The length of the BIT must be in the range 1 to 64.")
    else Bit(length, isOptional[T])

  inline def TINYINT[T <: Byte | Short | Option[Byte | Short]](inline length: Int): Tinyint[T] =
    inline if length < 0 || length > 255 then error("The length of the TINYINT must be in the range 0 to 255.")
    else Tinyint(length, isOptional[T])

  inline def SMALLINT[T <: Short | Int | Option[Short | Int]](inline length: Int): Smallint[T] =
    inline if length < 0 || length > 255 then error("The length of the SMALLINT must be in the range 0 to 255.")
    else Smallint(length, isOptional[T])

  inline def MEDIUMINT[T <: Int | Option[Int]](inline length: Int): Mediumint[T] =
    inline if length < 0 || length > 255 then error("The length of the MEDIUMINT must be in the range 0 to 255.")
    else Mediumint(length, isOptional[T])

  inline def INT[T <: Int | Long | Option[Int | Long]](inline length: Int): Integer[T] =
    inline if length < 0 || length > 255 then error("The length of the INT must be in the range 0 to 255.")
    else Integer(length, isOptional[T])

  inline def BIGINT[T <: Long | BigInt | Option[Long | BigInt]](inline length: Int): Bigint[T] =
    inline if length < 0 || length > 255 then error("The length of the BIGINT must be in the range 0 to 255.")
    else Bigint(length, isOptional[T])

  inline def DECIMAL[T <: BigDecimal | Option[BigDecimal]](inline accuracy: Int = 10, inline scale: Int = 0): Decimal[T] =
    inline if accuracy < 0 then error("The value of accuracy for DECIMAL must be an integer.")
    inline if scale < 0 then error("The DECIMAL scale value must be an integer.")
    inline if accuracy > 65 then error("The maximum number of digits for DECIMAL is 65.")
    else Decimal(accuracy, scale, isOptional[T])

  inline def FLOAT[T <: Float | Option[Float]](inline accuracy: Int): CFloat[T] =
    inline if accuracy < 0 || accuracy > 24 then error("The length of the FLOAT must be in the range 0 to 24.")
    else CFloat(accuracy, isOptional[T])

  inline def DOUBLE[T <: Double | Option[Double]](inline accuracy: Int): CFloat[T] =
    inline if accuracy < 24 || accuracy > 53 then error("The length of the DOUBLE must be in the range 24 to 53.")
    else CFloat(accuracy, isOptional[T])

  /** ===== List of String Data Types ===== */

  inline def CHAR[T <: String | Option[String]](inline length: Int): CChar[T] =
    inline if length < 0 || length > 255 then error("The length of the CHAR must be in the range 0 to 255.")
    else CChar(length, isOptional[T])

  inline def VARCHAR[T <: String | Option[String]](inline length: Int): Varchar[T] =
    inline if length < 0 || length > 255 then error("The length of the VARCHAR must be in the range 0 to 255.")
    else Varchar(length, isOptional[T])

  inline def BINARY[T <: Array[Byte] | Option[Array[Byte]]](inline length: Int): Binary[T] =
    inline if length < 0 || length > 255 then error("The length of the BINARY must be in the range 0 to 255.")
    else Binary(length, isOptional[T])

  inline def VARBINARY[T <: Array[Byte] | Option[Array[Byte]]](inline length: Int): Varbinary[T] =
    inline if length < 0 || length > 255 then error("The length of the VARBINARY must be in the range 0 to 255.")
    else Varbinary(length, isOptional[T])

  inline def TINYBLOB[T <: Array[Byte] | Option[Array[Byte]]](): Tinyblob[T] = Tinyblob(isOptional[T])

  inline def BLOB[T <: Array[Byte] | Option[Array[Byte]]](inline length: Long): Blob[T] =
    inline if length < 0 || length > 4294967295L then
      error("The length of the BLOB must be in the range 0 to 4294967295.")
    else Blob(length, isOptional[T])

  inline def MEDIUMBLOB[T <: Array[Byte] | Option[Array[Byte]]](): Mediumblob[T] = Mediumblob(isOptional[T])

  inline def LONGBLOB[T <: Array[Byte] | Option[Array[Byte]]](): LongBlob[T] = LongBlob(isOptional[T])

  inline def TINYTEXT[T <: String | Option[String]](): TinyText[T] = TinyText(isOptional[T])

  inline def TEXT[T <: String | Option[String]](): Text[T] = Text(isOptional[T])

  inline def MEDIUMTEXT[T <: String | Option[String]](): MediumText[T] = MediumText(isOptional[T])

  inline def LONGTEXT[T <: String | Option[String]](): LongText[T] = LongText(isOptional[T])

  /** ===== List of Date Data Types ===== */

  inline def DATE[T <: LocalDate | Option[LocalDate]]: Date[T] = Date(isOptional[T])

  inline def DATETIME[T <: Instant | LocalDateTime | OffsetTime | Option[Instant | LocalDateTime | OffsetTime]]: DateTime[T] = DateTime(isOptional[T])

  inline def TIMESTAMP[T <: Instant | LocalDateTime | OffsetDateTime | ZonedDateTime | Option[Instant | LocalDateTime | OffsetDateTime | ZonedDateTime]]: TimeStamp[T] = TimeStamp(isOptional[T])

  inline def TIME[T <: LocalTime | Option[LocalTime]]: Time[T] = Time(isOptional[T])

  inline def YEAR[T <: Instant | LocalDate | JYear | Option[Instant | LocalDate | JYear]]: Year[T] = Year(isOptional[T])
