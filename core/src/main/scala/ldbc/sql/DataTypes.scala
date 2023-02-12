/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql

import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime }
import java.time.Year as JYear

import scala.compiletime.error
import scala.annotation.targetName

import ldbc.sql.DataType.*

/**
 * A set of methods for constructing DataType
 */
object DataTypes:

  /**
   * ===============================
   *  List of Numeric Data Types
   * ===============================
   */

  inline def BIT[T <: Byte | Short | Int | Long | Float | Double | BigDecimal](inline length: Int): Bit[T] =
    inline if length < 1 || length > 64 then error("The length of the BIT must be in the range 1 to 64.")
    else Bit(length, None)

  inline def TINYINT[T <: Byte](inline length: Int): Tinyint[T] =
    inline if length < 0 || length > 255 then error("The length of the TINYINT must be in the range 0 to 255.")
    else Tinyint(length, None)

  inline def SMALLINT[T <: Short](inline length: Int): Smallint[T] =
    inline if length < 0 || length > 255 then error("The length of the SMALLINT must be in the range 0 to 255.")
    else Smallint(length, None)

  inline def MEDIUMINT[T <: Int](inline length: Int): Mediumint[T] =
    inline if length < 0 || length > 255 then error("The length of the MEDIUMINT must be in the range 0 to 255.")
    else Mediumint(length, None)

  inline def INT[T <: Int](inline length: Int): Integer[T] =
    inline if length < 0 || length > 255 then error("The length of the INT must be in the range 0 to 255.")
    else Integer(length, None)

  inline def BIGINT[T <: Long](inline length: Int): Bigint[T] =
    inline if length < 0 || length > 255 then error("The length of the BIGINT must be in the range 0 to 255.")
    else Bigint(length, None)

  inline def DECIMAL[T <: BigDecimal](inline accuracy: Int = 10, inline scale: Int = 0): Decimal[T] =
    inline if accuracy < 0 then error("The value of accuracy for DECIMAL must be an integer.")
    inline if scale < 0 then error("The DECIMAL scale value must be an integer.")
    inline if accuracy > 65 then error("The maximum number of digits for DECIMAL is 65.")
    else Decimal(accuracy, scale, None)

  inline def FLOAT[T <: Float](inline accuracy: Int): CFloat[T] =
    inline if accuracy < 0 || accuracy > 24 then error("The length of the FLOAT must be in the range 0 to 24.")
    else CFloat(accuracy, None)

  inline def DOUBLE[T <: Double](inline accuracy: Int): CFloat[T] =
    inline if accuracy < 24 || accuracy > 53 then error("The length of the DOUBLE must be in the range 24 to 53.")
    else CFloat(accuracy, None)

  /**
   * ===============================
   *  List of String Data Types
   * ===============================
   */

  inline def CHAR[T <: String](inline length: Int): CChar[T] =
    inline if length < 0 || length > 255 then error("The length of the CHAR must be in the range 0 to 255.")
    else CChar(length, None, None)

  inline def VARCHAR[T <: String](inline length: Int): Varchar[T] =
    inline if length < 0 || length > 255 then error("The length of the VARCHAR must be in the range 0 to 255.")
    else Varchar(length, None, None)

  inline def BINARY[T <: Array[Byte]](inline length: Int): Binary[T] =
    inline if length < 0 || length > 255 then error("The length of the BINARY must be in the range 0 to 255.")
    else Binary(length, None)

  inline def VARBINARY[T <: Array[Byte]](inline length: Int): Varbinary[T] =
    inline if length < 0 || length > 255 then error("The length of the VARBINARY must be in the range 0 to 255.")
    else Varbinary(length, None)

  inline def TINYBLOB[T <: Array[Byte]](): Tinyblob[T] = Tinyblob(None)

  inline def BLOB[T <: Array[Byte]](inline length: Long): Blob[T] =
    inline if length < 0 || length > 4294967295L then error("The length of the BLOB must be in the range 0 to 4294967295.")
    else Blob(length, None)

  inline def MEDIUMBLOB[T <: Array[Byte]](): Mediumblob[T] = Mediumblob(None)

  inline def LONGBLOB[T <: Array[Byte]](): LongBlob[T] = LongBlob(None)

  inline def TINYTEXT[T <: String](): TinyText[T] = TinyText(None)

  inline def TEXT[T <: String](): Text[T] = Text(None)

  inline def MEDIUMTEXT[T <: String](): MediumText[T] = MediumText(None)

  inline def LONGTEXT[T <: String](): LongText[T] = LongText(None)

  /**
   * ===============================
   *  List of Date Data Types
   * ===============================
   */

  inline def DATE[T <: LocalDate]: Date[T] = Date(None)

  inline def DATETIME[T <: Instant | LocalDateTime]: DateTime[T] = DateTime(None)

  inline def TIMESTAMP[T <: Instant | LocalDateTime]: TimeStamp[T] = TimeStamp(None)

  inline def TIME[T <: LocalTime]: Time[T] = Time(None)

  inline def YEAR[T <: Instant | LocalDate | JYear]: Year[T] = Year(None)
