/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  Year as JYear,
  ZonedDateTime
}

import scala.compiletime.erasedValue
import scala.deriving.Mirror
import scala.language.dynamics

import ldbc.dsl.codec.Codec

import ldbc.statement.{ AbstractTable, Column }

import ldbc.schema.attribute.Attribute
import ldbc.schema.interpreter.*
import ldbc.schema.model.{ Enum as EnumModel, EnumDataType }

trait Table[T](val $name: String) extends AbstractTable[T]:

  export ldbc.statement.Column

  transparent inline private def isOptional[A] = inline erasedValue[A] match
    case _: Option[?] => true
    case _            => false

  protected final def column[A](name: String)(using codec: Codec[A]): Column[A] =
    ColumnImpl[A](s"`$name`", Some(s"${ $name }.`$name`"), codec.asDecoder, codec.asEncoder, None, List.empty)

  protected final def column[A](name: String, dataType: DataType[A])(using codec: Codec[A]): Column[A] =
    ColumnImpl[A](s"`$name`", Some(s"${ $name }.`$name`"), codec.asDecoder, codec.asEncoder, Some(dataType), List.empty)

  protected final def column[A](name: String, dataType: DataType[A], attributes: Attribute[A]*)(using
    codec: Codec[A]
  ): Column[A] =
    ColumnImpl[A](
      s"`$name`",
      Some(s"${ $name }.`$name`"),
      codec.asDecoder,
      codec.asEncoder,
      Some(dataType),
      attributes.toList
    )

  /**
   * Create a column with a data type of BIT.
   */
  protected final inline def bit[A <: Byte | Short | Int | Long | Option[Byte | Short | Int | Long]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, BIT, isOptional[A])

  /**
   * Create a column with a data type of TINYINT.
   */
  protected final inline def tinyint[A <: Byte | Short | Option[Byte | Short]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, TINYINT, isOptional[A])

  /**
   * Create a column with a data type of SMALLINT.
   */
  protected final inline def smallint[A <: Short | Int | Option[Short | Int]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, SMALLINT, isOptional[A])

  /**
   * Create a column with a data type of MEDIUMINT.
   */
  protected final inline def mediumint[A <: Int | Option[Int]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, MEDIUMINT, isOptional[A])

  /**
   * Create a column with a data type of INT.
   */
  protected final inline def int[A <: Int | Long | Option[Int | Long]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, INT, isOptional[A])

  /**
   * Create a column with a data type of BIGINT.
   */
  protected final inline def bigint[A <: Long | BigInt | Option[Long | BigInt]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, BIGINT, isOptional[A])

  /**
   * Create a column with a data type of DECIMAL.
   */
  protected final inline def decimal[A <: BigDecimal | Option[BigDecimal]](
    inline accuracy: Int = 10,
    inline scale:    Int = 0,
    name:            String
  )(using Codec[A]): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric[A](name, DECIMAL(accuracy, scale), isOptional[A])

  /**
   * Create a column with a data type of FLOAT.
   */
  protected final inline def float[A <: Float | Option[Float]](inline accuracy: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric[A](name, FLOAT(accuracy), isOptional[A])

  /**
   * Create a column with a data type of DOUBLE.
   */
  protected final inline def double[A <: Double | Option[Double]](inline accuracy: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric[A](name, DOUBLE(accuracy), isOptional[A])

  /**
   * Create a column with a data type of CHAR.
   */
  protected final inline def char[A <: String | Option[String]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, CHAR(length), isOptional)

  /**
   * Create a column with a data type of VARCHAR.
   */
  protected final inline def varchar[A <: String | Option[String]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, VARCHAR(length), isOptional)

  /**
   * Create a column with a data type of BINARY.
   */
  protected final inline def binary[A <: Array[Byte] | Option[Array[Byte]]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, BINARY(length), isOptional)

  /**
   * Create a column with a data type of VARBINARY.
   */
  protected final inline def varbinary[A <: Array[Byte] | Option[Array[Byte]]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, VARBINARY(length), isOptional)

  /**
   * Create a column with a data type of TINYBLOB.
   */
  protected final inline def tinyblob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, TINYBLOB(), isOptional)

  /**
   * Create a column with a data type of BLOB.
   */
  protected final inline def blob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, BLOB(), isOptional)

  /**
   * Create a column with a data type of MEDIUMBLOB.
   */
  protected final inline def mediumblob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, MEDIUMBLOB(), isOptional)

  /**
   * Create a column with a data type of LONGBLOB.
   */
  protected final inline def longblob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, LONGBLOB(), isOptional)

  /**
   * Create a column with a data type of TINYTEXT.
   */
  protected final inline def tinytext[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, TINYTEXT(), isOptional)

  /**
   * Create a column with a data type of TEXT.
   */
  protected final inline def text[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, TEXT(), isOptional)

  /**
   * Create a column with a data type of MEDIUMTEXT.
   */
  protected final inline def mediumtext[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, MEDIUMTEXT(), isOptional)

  /**
   * Create a column with a data type of LONGTEXT.
   */
  protected final inline def longtext[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, LONGTEXT(), isOptional)

  /**
   * Create a column with a data type of ENUM.
   */
  protected final inline def `enum`[A <: EnumModel | Option[EnumModel]](
    name: String
  )(using Codec[A], EnumDataType[?]): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, ENUM, isOptional)

  /**
   * Create a column with a data type of DATE.
   */
  protected final inline def date[A <: String | LocalDate | Option[String | LocalDate]](name: String)(using
    Codec[A]
  ): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, DATE, isOptional)

  /**
   * Create a column with a data type of DATETIME.
   */
  protected final inline def datetime[
    A <: String | Instant | LocalDateTime | OffsetTime | Option[String | Instant | LocalDateTime | OffsetTime]
  ](name: String)(using Codec[A]): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, DATETIME, isOptional)

  /**
   * Create a column with a data type of TIMESTAMP.
   */
  protected final inline def timestamp[
    A <: String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](name: String)(using Codec[A]): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, TIMESTAMP, isOptional)

  /**
   * Create a column with a data type of TIME.
   */
  protected final inline def time[A <: String | LocalTime | Option[String | LocalTime]](name: String)(using
    Codec[A]
  ): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, TIME, isOptional)

  /**
   * Create a column with a data type of YEAR.
   */
  protected final inline def year[A <: Int | Instant | LocalDate | JYear | Option[Int | Instant | LocalDate | JYear]](
    name: String
  )(using Codec[A]): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, YEAR, isOptional)

  /**
   * Create a column with a data type of SERIAL.
   */
  protected final inline def serial[A <: BigInt](name: String)(using Codec[A]): DataTypeColumn[A] =
    DataTypeColumn[A](name, SERIAL, isOptional)

  /**
   * Create a column with a data type of BOOLEAN.
   */
  protected final inline def boolean[A <: Boolean | Option[Boolean]](name: String)(using Codec[A]): DataTypeColumn[A] =
    DataTypeColumn[A](name, BOOLEAN, isOptional)

  /**
   * Methods for setting key information for tables.
   */
  def keys: List[Key] = List.empty

  /**
   * Create a table statement.
   */
  def createStatement: String =
    val columns = this.*.list.map(_.statement).mkString(",\n  ")
    val keys    = this.keys.map(_.queryString).mkString(",\n  ")
    s"""
       |CREATE TABLE IF NOT EXISTS `${ $name }` (
       |  $columns,
       |  $keys
       |)
       |""".stripMargin

  override final def statement: String = $name

  override def toString: String = s"Table($$name)"

object Table:

  case class Opt[T](
    $name:   String,
    columns: List[Column[?]],
    *      : Column[Option[T]]
  ) extends AbstractTable.Opt[T],
            Dynamic:

    override def statement: String = $name

    transparent inline def selectDynamic[Tag <: Singleton](
      tag: Tag
    )(using
      mirror: Mirror.Of[T],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ): Column[
      Option[ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
    ] =
      columns
        .apply(index.value)
        .asInstanceOf[Column[
          ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
        ]]
        .opt
