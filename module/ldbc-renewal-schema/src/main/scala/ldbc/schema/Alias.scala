/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import java.time.*
import java.time.Year as JYear

import ldbc.schema.attribute.*

private[ldbc] trait Alias:

  type Column[A] = ldbc.statement.Column[A]

  def COMMENT[T](message: String): Comment[T] = Comment[T](message)

  def VISIBLE[T]: Visible[T] = Visible[T]()

  def INVISIBLE[T]: InVisible[T] = InVisible[T]()

  object COLUMN_FORMAT:
    def FIXED[T]: ColumnFormat.Fixed[T] = ColumnFormat.Fixed[T]()

    def DYNAMIC[T]: ColumnFormat.Dynamic[T] = ColumnFormat.Dynamic[T]()

    def DEFAULT[T]: ColumnFormat.Default[T] = ColumnFormat.Default[T]()

  object STORAGE:
    def DISK[T]: Storage.Disk[T] = Storage.Disk[T]()

    def MEMORY[T]: Storage.Memory[T] = Storage.Memory[T]()

  def AUTO_INCREMENT[T <: Byte | Short | Int | Long | BigInt | Option[Byte | Short | Int | Long | BigInt]]: AutoInc[T] =
    AutoInc[T]()

  def PRIMARY_KEY[T]: PrimaryKey & Attribute[T] = new PrimaryKey with Attribute[T]:
    override def queryString: String = label

  def PRIMARY_KEY(keyPart: Column[?]): PrimaryKey & Index =
    PrimaryKey(None, List(keyPart), None)

  def PRIMARY_KEY(keyPart: Column[?]*): PrimaryKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(None, keyPart.toList, None)

  def PRIMARY_KEY(
                   indexType: Index.Type,
                   keyPart: Column[?]*
                 ): PrimaryKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(Some(indexType), keyPart.toList, None)

  def PRIMARY_KEY(
                   keyPart: List[Column[?]],
                   indexOption: Index.IndexOption
                 ): PrimaryKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(None, keyPart, Some(indexOption))

  def PRIMARY_KEY(
                   indexType: Index.Type,
                   indexOption: Index.IndexOption,
                   keyPart: Column[?]*
                 ): PrimaryKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(Some(indexType), keyPart.toList, Some(indexOption))

  def UNIQUE_KEY[T]: UniqueKey & Attribute[T] = new UniqueKey with Attribute[T]:
    override def indexName: Option[String] = None
    override def queryString: String = label

  def UNIQUE_KEY(keyPart: Column[?]*): UniqueKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(None, None, keyPart.toList, None)

  def UNIQUE_KEY(
                  indexName: String,
                  keyPart: Column[?]*
                ): UniqueKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(Some(indexName), None, keyPart.toList, None)

  def UNIQUE_KEY(
                  indexName: String,
                  indexType: Index.Type,
                  keyPart: Column[?]*
                ): UniqueKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(Some(indexName), Some(indexType), keyPart.toList, None)

  def UNIQUE_KEY(
                  indexName: String,
                  indexType: Index.Type,
                  indexOption: Index.IndexOption,
                  keyPart: Column[?]*
                ): UniqueKey & Index = UniqueKey(Some(indexName), Some(indexType), keyPart.toList, Some(indexOption))

  def UNIQUE_KEY(
                  indexName: Option[String],
                  indexType: Option[Index.Type],
                  indexOption: Option[Index.IndexOption],
                  keyPart: Column[?]*
                ): UniqueKey & Index = UniqueKey(indexName, indexType, keyPart.toList, indexOption)

  type BIT[T <: Byte | Short | Int | Long | Option[Byte | Short | Int | Long]] = DataType.Bit[T]

  type TINYINT[T <: Byte | Short | Option[Byte | Short]] = DataType.Tinyint[T]

  type SMALLINT[T <: Short | Int | Option[Short | Int]] = DataType.Smallint[T]

  type MEDIUMINT[T <: Int | Option[Int]] = DataType.Mediumint[T]

  type INTEGER[T <: Int | Long | Option[Int | Long]] = DataType.Integer[T]

  type BIGINT[T <: Long | BigInt | Option[Long | BigInt]] = DataType.Bigint[T]

  type DECIMAL[T <: BigDecimal | Option[BigDecimal]] = DataType.Decimal[T]

  type FLOAT[T <: Double | Float | Option[Double | Float]] = DataType.CFloat[T]

  type CHAR[T <: String | Option[String]] = DataType.CChar[T]

  type VARCHAR[T <: String | Option[String]] = DataType.Varchar[T]

  type BINARY[T <: Array[Byte] | Option[Array[Byte]]] = DataType.Binary[T]

  type TINYBLOB[T <: Array[Byte] | Option[Array[Byte]]] = DataType.Tinyblob[T]

  type BLOB[T <: Array[Byte] | Option[Array[Byte]]] = DataType.Blob[T]

  type MEDIUMBLOB[T <: Array[Byte] | Option[Array[Byte]]] = DataType.Mediumblob[T]

  type LONGBLOB[T <: Array[Byte] | Option[Array[Byte]]] = DataType.LongBlob[T]

  type TINYTEXT[T <: String | Option[String]] = DataType.TinyText[T]

  type TEXT[T <: String | Option[String]] = DataType.Text[T]

  type MEDIUMTEXT[T <: String | Option[String]] = DataType.MediumText[T]

  type LONGTEXT[T <: String | Option[String]] = DataType.LongText[T]

  type DATE[T <: LocalDate | Option[LocalDate]] = DataType.Date[T]

  type DATETIME[T <: Instant | LocalDateTime | Option[Instant | LocalDateTime]] = DataType.DateTime[T]

  type TIMESTAMP[T <: Instant | LocalDateTime | Option[Instant | LocalDateTime]] = DataType.TimeStamp[T]

  type TIME[T <: LocalTime | Option[LocalTime]] = DataType.Time[T]

  type YEAR[T <: Instant | LocalDate | JYear | Option[Instant | LocalDate | JYear]] = DataType.Year[T]
