/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.syntax

import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime }
import java.time.Year as JYear

import ldbc.core.DataType.*

/** Trait for converting DataType type to Option type.
  */
private[ldbc] trait DataTypeConversion:

  given Conversion[Bit[Byte], BitOpt[Option[Byte]]] =
    v => BitOpt(v.length, v.default)
  given optionBitShort: Conversion[Bit[Short], BitOpt[Option[Short]]] =
    v => BitOpt(v.length, v.default)
  given optionBitInt: Conversion[Bit[Int], BitOpt[Option[Int]]] =
    v => BitOpt(v.length, v.default)
  given optionBitLong: Conversion[Bit[Long], BitOpt[Option[Long]]] =
    v => BitOpt(v.length, v.default)
  given optionBitFloat: Conversion[Bit[Float], BitOpt[Option[Float]]] =
    v => BitOpt(v.length, v.default)
  given optionBitDouble: Conversion[Bit[Double], BitOpt[Option[Double]]] =
    v => BitOpt(v.length, v.default)
  given optionBitBigDecimal: Conversion[Bit[BigDecimal], BitOpt[Option[BigDecimal]]] =
    v => BitOpt(v.length, v.default)

  given Conversion[Tinyint[Byte], TinyintOpt[Option[Byte]]] =
    v => TinyintOpt(v.length, v.default)
  given Conversion[Smallint[Short], SmallintOpt[Option[Short]]] =
    v => SmallintOpt(v.length, v.default)
  given Conversion[Mediumint[Int], MediumintOpt[Option[Int]]] =
    v => MediumintOpt(v.length, v.default)
  given Conversion[Integer[Int], IntegerOpt[Option[Int]]] =
    v => IntegerOpt(v.length, v.default)
  given Conversion[Bigint[Long], BigintOpt[Option[Long]]] =
    v => BigintOpt(v.length, v.default)
  given Conversion[Decimal[BigDecimal], DecimalOpt[Option[BigDecimal]]] =
    v => DecimalOpt(v.accuracy, v.scale, v.default)

  given optionFloatDouble: Conversion[CFloat[Double], FloatOpt[Option[Double]]] =
    v => FloatOpt(v.accuracy, v.default)
  given optionFloatFloat: Conversion[CFloat[Float], FloatOpt[Option[Float]]] =
    v => FloatOpt(v.accuracy, v.default)

  given Conversion[CChar[String], CharOpt[Option[String]]] =
    v => CharOpt(v.length, v.default, v.character)
  given Conversion[Varchar[String], VarcharOpt[Option[String]]] =
    v => VarcharOpt(v.length, v.default, v.character)
  given Conversion[Binary[Array[Byte]], BinaryOpt[Option[Array[Byte]]]] =
    v => BinaryOpt(v.length, v.character)
  given Conversion[Varbinary[Array[Byte]], VarbinaryOpt[Option[Array[Byte]]]] =
    v => VarbinaryOpt(v.length, v.character)
  given Conversion[Tinyblob[Array[Byte]], TinyblobOpt[Option[Array[Byte]]]] =
    v => TinyblobOpt(v.character)
  given Conversion[Blob[Array[Byte]], BlobOpt[Option[Array[Byte]]]] =
    v => BlobOpt(v.length, v.character)
  given Conversion[Mediumblob[Array[Byte]], MediumblobOpt[Option[Array[Byte]]]] =
    v => MediumblobOpt(v.character)
  given Conversion[LongBlob[Array[Byte]], LongBlobOpt[Option[Array[Byte]]]] =
    v => LongBlobOpt(v.character)
  given Conversion[TinyText[String], TinyTextOpt[Option[String]]] =
    v => TinyTextOpt(v.character)
  given Conversion[Text[String], TextOpt[Option[String]]] =
    v => TextOpt(v.character)
  given Conversion[MediumText[String], MediumTextOpt[Option[String]]] =
    v => MediumTextOpt(v.character)
  given Conversion[LongText[String], LongTextOpt[Option[String]]] =
    v => LongTextOpt(v.character)

  given Conversion[Date[LocalDate], DateOpt[Option[LocalDate]]] =
    v => DateOpt(v.default)

  given Conversion[DateTime[Instant], DateTimeOpt[Option[Instant]]] =
    v => DateTimeOpt(v.default)
  given optionDateTimeLocalDateTime: Conversion[DateTime[LocalDateTime], DateTimeOpt[Option[LocalDateTime]]] =
    v => DateTimeOpt(v.default)

  given Conversion[TimeStamp[Instant], TimeStampOpt[Option[Instant]]] =
    v => TimeStampOpt(v.default)
  given optionTimeStampLocalDateTime: Conversion[TimeStamp[LocalDateTime], TimeStampOpt[Option[LocalDateTime]]] =
    v => TimeStampOpt(v.default)

  given Conversion[Time[LocalTime], TimeOpt[Option[LocalTime]]] =
    v => TimeOpt(v.default)

  given Conversion[Year[Instant], YearOpt[Option[Instant]]] =
    v => YearOpt(v.default)
  given optionYearLocalDate: Conversion[Year[LocalDate], YearOpt[Option[LocalDate]]] =
    v => YearOpt(v.default)
  given optionYearJYear: Conversion[Year[JYear], YearOpt[Option[JYear]]] =
    v => YearOpt(v.default)
