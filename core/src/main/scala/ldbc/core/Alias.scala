/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import java.time.*
import java.time.Year as JYear

import cats.data.NonEmptyList

import ldbc.core.attribute.{ Attribute, AutoInc }

private[ldbc] trait Alias:

  type TABLE[P <: Product] = Table[P]

  def column[T](
    label:    String,
    dataType: DataType[T]
  ): Column[T] = Column[T](label, dataType)

  def column[T](
    label:    String,
    dataType: DataType[T],
    comment:  String
  ): Column[T] = Column[T](label, dataType, comment)

  def column[T](
    label:      String,
    dataType:   DataType[T],
    attributes: Attribute[T]*
  ): Column[T] = Column[T](label, dataType, attributes: _*)

  def column[T](
    label:      String,
    dataType:   DataType[T],
    comment:    String,
    attributes: Attribute[T]*
  ): Column[T] = Column[T](label, dataType, comment, attributes: _*)

  def AUTO_INCREMENT[T <: Byte | Short | Int | Long | BigInt | Option[Byte | Short | Int | Long | BigInt]]: AutoInc[T] =
    AutoInc[T]()

  def PRIMARY_KEY[T]: PrimaryKey & Attribute[T] = new PrimaryKey with Attribute[T]:
    override def queryString: String = label

  def PRIMARY_KEY(keyPart: Column[?]): PrimaryKey with Index = PrimaryKey(None, NonEmptyList.one(keyPart), None)

  def PRIMARY_KEY(keyPart: NonEmptyList[Column[?]]): PrimaryKey with Index = PrimaryKey(None, keyPart, None)

  def PRIMARY_KEY(
    indexType: Index.Type,
    keyPart:   NonEmptyList[Column[?]]
  ): PrimaryKey with Index = PrimaryKey(Some(indexType), keyPart, None)

  def PRIMARY_KEY(
    keyPart:     NonEmptyList[Column[?]],
    indexOption: Index.IndexOption
  ): PrimaryKey with Index = PrimaryKey(None, keyPart, Some(indexOption))

  def PRIMARY_KEY(
    indexType:   Index.Type,
    keyPart:     NonEmptyList[Column[?]],
    indexOption: Index.IndexOption
  ): PrimaryKey with Index = PrimaryKey(Some(indexType), keyPart, Some(indexOption))

  def UNIQUE_KEY[T]: UniqueKey & Attribute[T] = new UniqueKey with Attribute[T]:
    override def indexName: Option[String] = None

    override def queryString: String = label

  def UNIQUE_KEY(keyPart: Column[?]): UniqueKey with Index = UniqueKey(None, None, NonEmptyList.one(keyPart), None)

  def UNIQUE_KEY(
    indexName: String,
    keyPart:   NonEmptyList[Column[?]]
  ): UniqueKey with Index = UniqueKey(Some(indexName), None, keyPart, None)

  def UNIQUE_KEY(
    indexName: String,
    indexType: Index.Type,
    keyPart:   NonEmptyList[Column[?]]
  ): UniqueKey with Index = UniqueKey(Some(indexName), Some(indexType), keyPart, None)

  def UNIQUE_KEY(
    indexName:   String,
    indexType:   Index.Type,
    keyPart:     NonEmptyList[Column[?]],
    indexOption: Index.IndexOption
  ): UniqueKey with Index = UniqueKey(Some(indexName), Some(indexType), keyPart, Some(indexOption))

  def UNIQUE_KEY(
    indexName:   Option[String],
    indexType:   Option[Index.Type],
    keyPart:     NonEmptyList[Column[?]],
    indexOption: Option[Index.IndexOption]
  ): UniqueKey with Index = UniqueKey(indexName, indexType, keyPart, indexOption)

  def INDEX_KEY(keyPart: Column[?]): IndexKey = IndexKey(None, None, NonEmptyList.one(keyPart), None)

  def INDEX_KEY(
    indexName:   Option[String],
    indexType:   Option[Index.Type],
    keyPart:     NonEmptyList[Column[?]],
    indexOption: Option[Index.IndexOption]
  ): IndexKey = IndexKey(indexName, indexType, keyPart, indexOption)

  def CONSTRAINT(
    symbol: String,
    key:    PrimaryKey | UniqueKey | ForeignKey
  ): Constraint = Constraint(symbol, key)

  def FOREIGN_KEY(
    colName:   Column[?],
    reference: Reference
  ): ForeignKey = ForeignKey(None, NonEmptyList.one(colName), reference)

  def FOREIGN_KEY(
    indexName: Option[String],
    colName:   NonEmptyList[Column[?]],
    reference: Reference
  ): ForeignKey = ForeignKey(indexName, colName, reference)

  def REFERENCE(
    table:   Table[?],
    keyPart: Column[?]
  ): Reference = Reference(table, NonEmptyList.one(keyPart), None, None)

  def REFERENCE(table: Table[?])(columns: Column[?]*): Reference =
    require(
      NonEmptyList.fromList(columns.toList).nonEmpty,
      "For Reference settings, at least one COLUMN must always be specified."
    )
    Reference(table, NonEmptyList.fromListUnsafe(columns.toList), None, None)

  def REFERENCE(
    table:    Table[?],
    keyPart:  NonEmptyList[Column[?]],
    onDelete: Option[Reference.ReferenceOption],
    onUpdate: Option[Reference.ReferenceOption]
  ): Reference = Reference(table, keyPart, onDelete, onUpdate)

  type BIT[
    T <: Byte | Short | Int | Long | Float | Double | BigDecimal |
      Option[Byte | Short | Int | Long | Float | Double | BigDecimal]
  ] = DataType.Bit[T]

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
