/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import java.time.*
import java.time.Year as JYear

import ldbc.core.attribute.*

private[ldbc] trait Alias:

  type TABLE[P <: Product] = Table[P]

  def column[T](
    label:    String,
    dataType: DataType[T]
  ): Column[T] = Column[T](label, dataType)

  def column[T](
    label:      String,
    dataType:   DataType[T],
    attributes: Attribute[T]*
  ): Column[T] = Column[T](label, dataType, attributes: _*)

  def COMMENT[T](message: String): Comment[T] = Comment[T](message)

  def VISIBLE[T]:   Visible[T]   = Visible[T]()
  def INVISIBLE[T]: InVisible[T] = InVisible[T]()

  def COLLATE[T <: String | Option[String]](name: String): Collate & Attribute[T] = new Collate(name) with Attribute[T]

  object COLUMN_FORMAT:
    def FIXED[T]:   ColumnFormat.Fixed[T]   = ColumnFormat.Fixed[T]()
    def DYNAMIC[T]: ColumnFormat.Dynamic[T] = ColumnFormat.Dynamic[T]()
    def DEFAULT[T]: ColumnFormat.Default[T] = ColumnFormat.Default[T]()

  object STORAGE:
    def DISK[T]:   Storage.Disk[T]   = Storage.Disk[T]()
    def MEMORY[T]: Storage.Memory[T] = Storage.Memory[T]()

  def AUTO_INCREMENT[T <: Byte | Short | Int | Long | BigInt | Option[Byte | Short | Int | Long | BigInt]]: AutoInc[T] =
    AutoInc[T]()

  def PRIMARY_KEY[T]: PrimaryKey & Attribute[T] = new PrimaryKey with Attribute[T]:
    override def queryString: String = label

  def PRIMARY_KEY(keyPart: Column[?]): PrimaryKey with Index =
    PrimaryKey(None, List(keyPart), None)

  def PRIMARY_KEY(keyPart: Column[?]*): PrimaryKey with Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(None, keyPart.toList, None)

  def PRIMARY_KEY(
    indexType: Index.Type,
    keyPart:   Column[?]*
  ): PrimaryKey with Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(Some(indexType), keyPart.toList, None)

  def PRIMARY_KEY(
    keyPart:     List[Column[?]],
    indexOption: Index.IndexOption
  ): PrimaryKey with Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(None, keyPart, Some(indexOption))

  def PRIMARY_KEY(
    indexType:   Index.Type,
    indexOption: Index.IndexOption,
    keyPart:     Column[?]*
  ): PrimaryKey with Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(Some(indexType), keyPart.toList, Some(indexOption))

  def UNIQUE_KEY[T]: UniqueKey & Attribute[T] = new UniqueKey with Attribute[T]:
    override def indexName: Option[String] = None

    override def queryString: String = label

  def UNIQUE_KEY(keyPart: Column[?]*): UniqueKey with Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(None, None, keyPart.toList, None)

  def UNIQUE_KEY(
    indexName: String,
    keyPart:   Column[?]*
  ): UniqueKey with Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(Some(indexName), None, keyPart.toList, None)

  def UNIQUE_KEY(
    indexName: String,
    indexType: Index.Type,
    keyPart:   Column[?]*
  ): UniqueKey with Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(Some(indexName), Some(indexType), keyPart.toList, None)

  def UNIQUE_KEY(
    indexName:   String,
    indexType:   Index.Type,
    indexOption: Index.IndexOption,
    keyPart:     Column[?]*
  ): UniqueKey with Index = UniqueKey(Some(indexName), Some(indexType), keyPart.toList, Some(indexOption))

  def UNIQUE_KEY(
    indexName:   Option[String],
    indexType:   Option[Index.Type],
    indexOption: Option[Index.IndexOption],
    keyPart:     Column[?]*
  ): UniqueKey with Index = UniqueKey(indexName, indexType, keyPart.toList, indexOption)

  def INDEX_KEY(keyPart: Column[?]*): IndexKey =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    IndexKey(None, None, keyPart.toList, None)

  def INDEX_KEY(
    indexName:   Option[String],
    indexType:   Option[Index.Type],
    indexOption: Option[Index.IndexOption],
    keyPart:     Column[?]*
  ): IndexKey =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    IndexKey(indexName, indexType, keyPart.toList, indexOption)

  def CONSTRAINT(key: PrimaryKey | UniqueKey | ForeignKey[?]): Constraint = Constraint(None, key)

  def CONSTRAINT(
    symbol: String,
    key:    PrimaryKey | UniqueKey | ForeignKey[?]
  ): Constraint = Constraint(Some(symbol), key)

  def FOREIGN_KEY[T](
    column:    Column[T],
    reference: Reference[T *: EmptyTuple]
  ): ForeignKey[T *: EmptyTuple] = ForeignKey[T *: EmptyTuple](None, column *: EmptyTuple, reference)

  def FOREIGN_KEY[T](
    name:      String,
    column:    Column[T],
    reference: Reference[T *: EmptyTuple]
  ): ForeignKey[T *: EmptyTuple] = ForeignKey[T *: EmptyTuple](Some(name), column *: EmptyTuple, reference)

  def FOREIGN_KEY[T <: Tuple](
    columns:   Tuple.Map[T, Column],
    reference: Reference[T]
  ): ForeignKey[T] =
    ForeignKey[T](None, columns, reference)

  def FOREIGN_KEY[T <: Tuple](
    name:      String,
    columns:   Tuple.Map[T, Column],
    reference: Reference[T]
  ): ForeignKey[T] =
    ForeignKey[T](Some(name), columns, reference)

  def FOREIGN_KEY[T <: Tuple](
    name:      Option[String],
    columns:   Tuple.Map[T, Column],
    reference: Reference[T]
  ): ForeignKey[T] =
    ForeignKey[T](name, columns, reference)

  def REFERENCE[T](
    table:  Table[?],
    column: Column[T]
  ): Reference[T *: EmptyTuple] = Reference[T *: EmptyTuple](table, column *: EmptyTuple, None, None)

  def REFERENCE[T <: Tuple](table: Table[?], columns: Tuple.Map[T, Column]): Reference[T] =
    Reference[T](table, columns, None, None)

  def REFERENCES[T <: Tuple](table: Table[?], columns: Tuple.Map[T, Column]): Reference[T] =
    Reference[T](table, columns, None, None)

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
