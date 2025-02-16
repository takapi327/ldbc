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
import scala.quoted.*

import ldbc.dsl.codec.Codec

import ldbc.statement.{ AbstractTable, Column }

import ldbc.schema.attribute.Attribute
import ldbc.schema.interpreter.*
import ldbc.schema.macros.DataTypeColumnMacros.*
import ldbc.schema.model.{ Enum as EnumModel, EnumDataType }
import ldbc.schema.DataType.*

trait Table[T](val $name: String) extends AbstractTable[T]:

  export ldbc.statement.Column

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
    DataTypeColumn.numeric(name, Some($name), BIT, Table.isOptional[A])

  protected final inline def bit[A <: Byte | Short | Int | Long | Option[Byte | Short | Int | Long]]
    : () => DataTypeColumn.NumericColumn[A] =
    Table.bit[A]($name)

  /**
   * Create a column with a data type of TINYINT.
   */
  protected final inline def tinyint[A <: Byte | Short | Option[Byte | Short]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, Some($name), TINYINT, Table.isOptional[A])

  protected final inline def tinyint[A <: Byte | Short | Option[Byte | Short]]: () => DataTypeColumn.NumericColumn[A] =
    Table.tinyint[A]($name)

  /**
   * Create a column with a data type of SMALLINT.
   */
  protected final inline def smallint[A <: Short | Int | Option[Short | Int]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, Some($name), SMALLINT, Table.isOptional[A])

  protected final inline def smallint[A <: Short | Int | Option[Short | Int]]: () => DataTypeColumn.NumericColumn[A] =
    Table.smallint[A]($name)

  /**
   * Create a column with a data type of MEDIUMINT.
   */
  protected final inline def mediumint[A <: Int | Option[Int]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, Some($name), MEDIUMINT, Table.isOptional[A])

  protected final inline def mediumint[A <: Int | Option[Int]]: () => DataTypeColumn.NumericColumn[A] =
    Table.mediumint[A]($name)

  /**
   * Create a column with a data type of INT.
   */
  protected final inline def int[A <: Int | Long | Option[Int | Long]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, Some($name), INT, Table.isOptional[A])

  protected final inline def int[A <: Int | Long | Option[Int | Long]]: () => DataTypeColumn.NumericColumn[A] =
    Table.int[A]($name)

  /**
   * Create a column with a data type of BIGINT.
   */
  protected final inline def bigint[A <: Long | BigInt | Option[Long | BigInt]](name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric(name, Some($name), BIGINT, Table.isOptional[A])

  protected final inline def bigint[A <: Long | BigInt | Option[Long | BigInt]]: () => DataTypeColumn.NumericColumn[A] =
    Table.bigint[A]($name)

  /**
   * Create a column with a data type of DECIMAL.
   */
  protected final inline def decimal[A <: BigDecimal | Option[BigDecimal]](
    inline accuracy: Int = 10,
    inline scale:    Int = 0,
    name:            String
  )(using Codec[A]): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric[A](name, Some($name), DECIMAL(accuracy, scale), Table.isOptional[A])

  protected final inline def decimal[A <: BigDecimal | Option[BigDecimal]]
    : (Int, Int) => DataTypeColumn.NumericColumn[A] =
    Table.decimal[A]($name)

  /**
   * Create a column with a data type of FLOAT.
   */
  protected final inline def float[A <: Float | Option[Float]](inline accuracy: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric[A](name, Some($name), FLOAT(accuracy), Table.isOptional[A])

  protected final inline def float[A <: Float | Option[Float]]: Int => DataTypeColumn.NumericColumn[A] =
    Table.float[A]($name)

  /**
   * Create a column with a data type of DOUBLE.
   */
  protected final inline def double[A <: Double | Option[Double]](inline accuracy: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.NumericColumn[A] =
    DataTypeColumn.numeric[A](name, Some($name), DOUBLE(accuracy), Table.isOptional[A])

  protected final inline def double[A <: Double | Option[Double]]: Int => DataTypeColumn.NumericColumn[A] =
    Table.double[A]($name)

  /**
   * Create a column with a data type of CHAR.
   */
  protected final inline def char[A <: String | Option[String]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), CHAR(length), Table.isOptional)

  protected final inline def char[A <: String | Option[String]]: Int => DataTypeColumn.StringColumn[A] =
    Table.char[A]($name)

  /**
   * Create a column with a data type of VARCHAR.
   */
  protected final inline def varchar[A <: String | Option[String]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), VARCHAR(length), Table.isOptional)

  protected final inline def varchar[A <: String | Option[String]]: Int => DataTypeColumn.StringColumn[A] =
    Table.varchar[A]($name)

  /**
   * Create a column with a data type of BINARY.
   */
  protected final inline def binary[A <: Array[Byte] | Option[Array[Byte]]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), BINARY(length), Table.isOptional)

  protected final inline def binary[A <: Array[Byte] | Option[Array[Byte]]]: Int => DataTypeColumn.StringColumn[A] =
    Table.binary[A]($name)

  /**
   * Create a column with a data type of VARBINARY.
   */
  protected final inline def varbinary[A <: Array[Byte] | Option[Array[Byte]]](inline length: Int, name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), VARBINARY(length), Table.isOptional)

  protected final inline def varbinary[A <: Array[Byte] | Option[Array[Byte]]]: Int => DataTypeColumn.StringColumn[A] =
    Table.varbinary[A]($name)

  /**
   * Create a column with a data type of TINYBLOB.
   */
  protected final inline def tinyblob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), TINYBLOB(), Table.isOptional)

  protected final inline def tinyblob[A <: Array[Byte] | Option[Array[Byte]]]: () => DataTypeColumn.StringColumn[A] =
    Table.tinyblob[A]($name)

  /**
   * Create a column with a data type of BLOB.
   */
  protected final inline def blob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), BLOB(), Table.isOptional)

  protected final inline def blob[A <: Array[Byte] | Option[Array[Byte]]]: () => DataTypeColumn.StringColumn[A] =
    Table.blob[A]($name)

  /**
   * Create a column with a data type of MEDIUMBLOB.
   */
  protected final inline def mediumblob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), MEDIUMBLOB(), Table.isOptional)

  protected final inline def mediumblob[A <: Array[Byte] | Option[Array[Byte]]]: () => DataTypeColumn.StringColumn[A] =
    Table.mediumblob[A]($name)

  /**
   * Create a column with a data type of LONGBLOB.
   */
  protected final inline def longblob[A <: Array[Byte] | Option[Array[Byte]]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), LONGBLOB(), Table.isOptional)

  protected final inline def longblob[A <: Array[Byte] | Option[Array[Byte]]]: () => DataTypeColumn.StringColumn[A] =
    Table.longblob[A]($name)

  /**
   * Create a column with a data type of TINYTEXT.
   */
  protected final inline def tinytext[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), TINYTEXT(), Table.isOptional)

  protected final inline def tinytext[A <: String | Option[String]]: () => DataTypeColumn.StringColumn[A] =
    Table.tinytext[A]($name)

  /**
   * Create a column with a data type of TEXT.
   */
  protected final inline def text[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), TEXT(), Table.isOptional)

  protected final inline def text[A <: String | Option[String]]: () => DataTypeColumn.StringColumn[A] =
    Table.text[A]($name)

  /**
   * Create a column with a data type of MEDIUMTEXT.
   */
  protected final inline def mediumtext[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), MEDIUMTEXT(), Table.isOptional)

  protected final inline def mediumtext[A <: String | Option[String]]: () => DataTypeColumn.StringColumn[A] =
    Table.mediumtext[A]($name)

  /**
   * Create a column with a data type of LONGTEXT.
   */
  protected final inline def longtext[A <: String | Option[String]](name: String)(using
    Codec[A]
  ): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), LONGTEXT(), Table.isOptional)

  protected final inline def longtext[A <: String | Option[String]]: () => DataTypeColumn.StringColumn[A] =
    Table.longtext[A]($name)

  /**
   * Create a column with a data type of ENUM.
   */
  protected final inline def `enum`[A <: EnumModel | Option[EnumModel]](
    name: String
  )(using Codec[A], EnumDataType[?]): DataTypeColumn.StringColumn[A] =
    DataTypeColumn.string[A](name, Some($name), ENUM, Table.isOptional)

  /**
   * Create a column with a data type of DATE.
   */
  protected final inline def date[A <: String | LocalDate | Option[String | LocalDate]](name: String)(using
    Codec[A]
  ): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, Some($name), DATE, Table.isOptional)

  protected final inline def date[A <: String | LocalDate | Option[String | LocalDate]]
    : () => DataTypeColumn.TemporalColumn[A] =
    Table.date[A]($name)

  /**
   * Create a column with a data type of DATETIME.
   */
  protected final inline def datetime[
    A <: String | Instant | LocalDateTime | OffsetTime | Option[String | Instant | LocalDateTime | OffsetTime]
  ](name: String)(using Codec[A]): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, Some($name), DATETIME, Table.isOptional)

  protected final inline def datetime[
    A <: String | Instant | LocalDateTime | OffsetTime | Option[String | Instant | LocalDateTime | OffsetTime]
  ]: () => DataTypeColumn.TemporalColumn[A] =
    Table.datetime[A]($name)

  /**
   * Create a column with a data type of TIMESTAMP.
   */
  protected final inline def timestamp[
    A <: String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](name: String)(using Codec[A]): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, Some($name), TIMESTAMP, Table.isOptional)

  protected final inline def timestamp[
    A <: String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ]: () => DataTypeColumn.TemporalColumn[A] =
    Table.timestamp[A]($name)

  /**
   * Create a column with a data type of TIME.
   */
  protected final inline def time[A <: String | LocalTime | Option[String | LocalTime]](name: String)(using
    Codec[A]
  ): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, Some($name), TIME, Table.isOptional)

  protected final inline def time[A <: String | LocalTime | Option[String | LocalTime]]
    : () => DataTypeColumn.TemporalColumn[A] =
    Table.time[A]($name)

  /**
   * Create a column with a data type of YEAR.
   */
  protected final inline def year[A <: Int | Instant | LocalDate | JYear | Option[Int | Instant | LocalDate | JYear]](
    name: String
  )(using Codec[A]): DataTypeColumn.TemporalColumn[A] =
    DataTypeColumn.temporal[A](name, Some($name), YEAR, Table.isOptional)

  protected final inline def year[A <: Int | Instant | LocalDate | JYear | Option[Int | Instant | LocalDate | JYear]]
    : () => DataTypeColumn.TemporalColumn[A] =
    Table.year[A]($name)

  /**
   * Create a column with a data type of SERIAL.
   */
  protected final inline def serial[A <: BigInt](name: String)(using Codec[A]): DataTypeColumn[A] =
    DataTypeColumn[A](name, Some($name), SERIAL, Table.isOptional)

  protected final inline def serial[A <: BigInt]: () => DataTypeColumn[A] =
    Table.serial[A]($name)

  /**
   * Create a column with a data type of BOOLEAN.
   */
  protected final inline def boolean[A <: Boolean | Option[Boolean]](name: String)(using Codec[A]): DataTypeColumn[A] =
    DataTypeColumn[A](name, Some($name), BOOLEAN, Table.isOptional)

  protected final inline def boolean[A <: Boolean | Option[Boolean]]: () => DataTypeColumn[A] =
    Table.boolean[A]($name)

  /**
   * Methods for setting key information for tables.
   */
  def keys: List[Key] = List.empty

  override final def statement: String = $name

  override def toString: String = s"Table($$name)"

object Table:

  transparent private[ldbc] inline def isOptional[A] = inline erasedValue[A] match
    case _: Option[?] => true
    case _            => false

  inline def bit[A <: Byte | Short | Int | Long | Option[Byte | Short | Int | Long]](
    alias: String
  ): () => DataTypeColumn.NumericColumn[A] =
    ${ namedNumericColumnImpl[A]('{ Some(alias) }, 'BIT, '{ isOptional[A] }) }

  inline def tinyint[A <: Byte | Short | Option[Byte | Short]](alias: String): () => DataTypeColumn.NumericColumn[A] =
    ${ namedNumericColumnImpl[A]('{ Some(alias) }, 'TINYINT, '{ isOptional[A] }) }

  inline def smallint[A <: Short | Int | Option[Short | Int]](alias: String): () => DataTypeColumn.NumericColumn[A] =
    ${ namedNumericColumnImpl[A]('{ Some(alias) }, 'SMALLINT, '{ isOptional[A] }) }

  inline def mediumint[A <: Int | Option[Int]](alias: String): () => DataTypeColumn.NumericColumn[A] =
    ${ namedNumericColumnImpl[A]('{ Some(alias) }, 'MEDIUMINT, '{ isOptional[A] }) }

  inline def int[A <: Int | Long | Option[Int | Long]](alias: String): () => DataTypeColumn.NumericColumn[A] =
    ${ namedNumericColumnImpl[A]('{ Some(alias) }, 'INT, '{ isOptional[A] }) }

  inline def bigint[A <: Long | BigInt | Option[Long | BigInt]](alias: String): () => DataTypeColumn.NumericColumn[A] =
    ${ namedNumericColumnImpl[A]('{ Some(alias) }, 'BIGINT, '{ isOptional[A] }) }

  inline def decimal[A <: BigDecimal | Option[BigDecimal]](
    alias: String
  ): (Int, Int) => DataTypeColumn.NumericColumn[A] =
    ${
      namedDecimalColumnImpl[A](
        '{ Some(alias) },
        '{ (accuracy: Int, scale: Int) => Decimal(accuracy, scale, isOptional[A]) },
        '{ isOptional[A] }
      )
    }

  inline def float[A <: Float | Option[Float]](alias: String): Int => DataTypeColumn.NumericColumn[A] =
    ${
      namedDoubleColumnImpl[A](
        '{ Some(alias) },
        '{ (accuracy: Int) => CFloat(accuracy, isOptional[A]) },
        '{ isOptional[A] }
      )
    }

  inline def double[A <: Double | Option[Double]](alias: String): Int => DataTypeColumn.NumericColumn[A] =
    ${
      namedDoubleColumnImpl[A](
        '{ Some(alias) },
        '{ (accuracy: Int) => CFloat(accuracy, isOptional[A]) },
        '{ isOptional[A] }
      )
    }

  inline def char[A <: String | Option[String]](alias: String): Int => DataTypeColumn.StringColumn[A] =
    ${
      namedStringLengthColumnImpl[A](
        '{ Some(alias) },
        '{ (length: Int) => CChar(length, isOptional[A]) },
        '{ isOptional[A] }
      )
    }

  inline def varchar[A <: String | Option[String]](alias: String): Int => DataTypeColumn.StringColumn[A] =
    ${
      namedStringLengthColumnImpl[A](
        '{ Some(alias) },
        '{ (length: Int) => Varchar(length, isOptional[A]) },
        '{ isOptional[A] }
      )
    }

  inline def binary[A <: Array[Byte] | Option[Array[Byte]]](alias: String): Int => DataTypeColumn.StringColumn[A] =
    ${
      namedStringLengthColumnImpl[A](
        '{ Some(alias) },
        '{ (length: Int) => Binary(length, isOptional[A]) },
        '{ isOptional[A] }
      )
    }

  inline def varbinary[A <: Array[Byte] | Option[Array[Byte]]](alias: String): Int => DataTypeColumn.StringColumn[A] =
    ${
      namedStringLengthColumnImpl[A](
        '{ Some(alias) },
        '{ (length: Int) => Varbinary(length, isOptional[A]) },
        '{ isOptional[A] }
      )
    }

  inline def tinyblob[A <: Array[Byte] | Option[Array[Byte]]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ TINYBLOB() }, '{ isOptional[A] }) }

  inline def blob[A <: Array[Byte] | Option[Array[Byte]]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ BLOB() }, '{ isOptional[A] }) }

  inline def mediumblob[A <: Array[Byte] | Option[Array[Byte]]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ MEDIUMBLOB() }, '{ isOptional[A] }) }

  inline def longblob[A <: Array[Byte] | Option[Array[Byte]]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ LONGBLOB() }, '{ isOptional[A] }) }

  inline def tinytext[A <: String | Option[String]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ TINYTEXT() }, '{ isOptional[A] }) }

  inline def text[A <: String | Option[String]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ TEXT() }, '{ isOptional[A] }) }

  inline def mediumtext[A <: String | Option[String]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ MEDIUMTEXT() }, '{ isOptional[A] }) }

  inline def longtext[A <: String | Option[String]](alias: String): () => DataTypeColumn.StringColumn[A] =
    ${ namedStringColumnImpl[A]('{ Some(alias) }, '{ LONGTEXT() }, '{ isOptional[A] }) }

  inline def date[A <: String | LocalDate | Option[String | LocalDate]](
    alias: String
  ): () => DataTypeColumn.TemporalColumn[A] =
    ${ namedTemporalColumnImpl[A]('{ Some(alias) }, 'DATE, '{ isOptional[A] }) }

  inline def datetime[
    A <: String | Instant | LocalDateTime | OffsetTime | Option[String | Instant | LocalDateTime | OffsetTime]
  ](alias: String): () => DataTypeColumn.TemporalColumn[A] =
    ${ namedTemporalColumnImpl[A]('{ Some(alias) }, 'DATETIME, '{ isOptional[A] }) }

  inline def timestamp[
    A <: String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](alias: String): () => DataTypeColumn.TemporalColumn[A] =
    ${ namedTemporalColumnImpl[A]('{ Some(alias) }, 'TIMESTAMP, '{ isOptional[A] }) }

  inline def time[A <: String | LocalTime | Option[String | LocalTime]](
    alias: String
  ): () => DataTypeColumn.TemporalColumn[A] =
    ${ namedTemporalColumnImpl[A]('{ Some(alias) }, 'TIME, '{ isOptional[A] }) }

  inline def year[A <: Int | Instant | LocalDate | JYear | Option[Int | Instant | LocalDate | JYear]](
    alias: String
  ): () => DataTypeColumn.TemporalColumn[A] =
    ${ namedTemporalColumnImpl[A]('{ Some(alias) }, 'YEAR, '{ isOptional[A] }) }

  inline def serial[A <: BigInt](alias: String): () => DataTypeColumn[A] =
    ${ namedColumnImpl[A]('{ Some(alias) }, 'SERIAL, '{ isOptional[A] }) }

  inline def boolean[A <: Boolean | Option[Boolean]](alias: String): () => DataTypeColumn[A] =
    ${ namedColumnImpl[A]('{ Some(alias) }, 'BOOLEAN, '{ isOptional[A] }) }

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
