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

  def AUTO_INCREMENT[T <: Byte | Short | Int | Long | BigInt | Option[Byte | Short | Int | Long | BigInt]]: AutoInc[T] = AutoInc[T]()

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
  ] =
    T match
      case Byte       => DataType.Bit[Byte]
      case Short      => DataType.Bit[Short]
      case Int        => DataType.Bit[Int]
      case Long       => DataType.Bit[Long]
      case Float      => DataType.Bit[Float]
      case Double     => DataType.Bit[Double]
      case BigDecimal => DataType.Bit[BigDecimal]
      case Option[t] =>
        t match
          case Byte       => DataType.BitOpt[Option[Byte]]
          case Short      => DataType.BitOpt[Option[Short]]
          case Int        => DataType.BitOpt[Option[Int]]
          case Long       => DataType.BitOpt[Option[Long]]
          case Float      => DataType.BitOpt[Option[Float]]
          case Double     => DataType.BitOpt[Option[Double]]
          case BigDecimal => DataType.BitOpt[Option[BigDecimal]]

  type TINYINT[T <: Byte | Short | Option[Byte | Short]] = T match
    case Byte          => DataType.Tinyint[Byte]
    case Short         => DataType.Tinyint[Short]
    case Option[Byte]  => DataType.TinyintOpt[Option[Byte]]
    case Option[Short] => DataType.TinyintOpt[Option[Short]]

  type SMALLINT[T <: Short | Int | Option[Short | Int]] = T match
    case Short         => DataType.Smallint[Short]
    case Int           => DataType.Smallint[Int]
    case Option[Short] => DataType.SmallintOpt[Option[Short]]
    case Option[Int]   => DataType.SmallintOpt[Option[Int]]

  type MEDIUMINT[T <: Int | Option[Int]] = T match
    case Int         => DataType.Mediumint[Int]
    case Option[Int] => DataType.MediumintOpt[Option[Int]]

  type INTEGER[T <: Int | Long | Option[Int | Long]] = T match
    case Int          => DataType.Integer[Int]
    case Long         => DataType.Integer[Long]
    case Option[Int]  => DataType.IntegerOpt[Option[Int]]
    case Option[Long] => DataType.IntegerOpt[Option[Long]]

  type BIGINT[T <: Long | BigInt | Option[Long | BigInt]] = T match
    case Long           => DataType.Bigint[Long]
    case BigInt         => DataType.Bigint[BigInt]
    case Option[Long]   => DataType.BigintOpt[Option[Long]]
    case Option[BigInt] => DataType.BigintOpt[Option[BigInt]]

  type DECIMAL[T <: BigDecimal | Option[BigDecimal]] = T match
    case BigDecimal         => DataType.Decimal[BigDecimal]
    case Option[BigDecimal] => DataType.DecimalOpt[Option[BigDecimal]]

  type FLOAT[T <: Double | Float | Option[Double | Float]] = T match
    case Double => DataType.CFloat[Double]
    case Float  => DataType.CFloat[Float]
    case Option[t] =>
      t match
        case Double => DataType.FloatOpt[Option[Double]]
        case Float  => DataType.FloatOpt[Option[Float]]

  type CHAR[T <: String | Option[String]] = T match
    case String         => DataType.CChar[String]
    case Option[String] => DataType.CharOpt[Option[String]]

  type VARCHAR[T <: String | Option[String]] = T match
    case String         => DataType.Varchar[String]
    case Option[String] => DataType.VarcharOpt[Option[String]]

  type BINARY[T <: Array[Byte] | Option[Array[Byte]]] = T match
    case Array[Byte]         => DataType.Binary[Array[Byte]]
    case Option[Array[Byte]] => DataType.BinaryOpt[Option[Array[Byte]]]

  type TINYBLOB[T <: Array[Byte] | Option[Array[Byte]]] = T match
    case Array[Byte]         => DataType.Tinyblob[Array[Byte]]
    case Option[Array[Byte]] => DataType.TinyblobOpt[Option[Array[Byte]]]

  type BLOB[T <: Array[Byte] | Option[Array[Byte]]] = T match
    case Array[Byte]         => DataType.Blob[Array[Byte]]
    case Option[Array[Byte]] => DataType.BlobOpt[Option[Array[Byte]]]

  type MEDIUMBLOB[T <: Array[Byte] | Option[Array[Byte]]] = T match
    case Array[Byte]         => DataType.Mediumblob[Array[Byte]]
    case Option[Array[Byte]] => DataType.MediumblobOpt[Option[Array[Byte]]]

  type LONGBLOB[T <: Array[Byte] | Option[Array[Byte]]] = T match
    case Array[Byte]         => DataType.LongBlob[Array[Byte]]
    case Option[Array[Byte]] => DataType.LongBlobOpt[Option[Array[Byte]]]

  type TINYTEXT[T <: String | Option[String]] = T match
    case String         => DataType.TinyText[String]
    case Option[String] => DataType.TinyTextOpt[Option[String]]

  type TEXT[T <: String | Option[String]] = T match
    case String         => DataType.Text[String]
    case Option[String] => DataType.TextOpt[Option[String]]

  type MEDIUMTEXT[T <: String | Option[String]] = T match
    case String         => DataType.MediumText[String]
    case Option[String] => DataType.MediumTextOpt[Option[String]]

  type LONGTEXT[T <: String | Option[String]] = T match
    case String         => DataType.LongText[String]
    case Option[String] => DataType.LongTextOpt[Option[String]]

  type DATE[T <: LocalDate | Option[LocalDate]] = T match
    case LocalDate         => DataType.Date[LocalDate]
    case Option[LocalDate] => DataType.DateOpt[Option[LocalDate]]

  type DATETIME[T <: Instant | LocalDateTime | Option[Instant | LocalDateTime]] = T match
    case Instant       => DataType.DateTime[Instant]
    case LocalDateTime => DataType.DateTime[LocalDateTime]
    case Option[t] =>
      t match
        case Instant       => DataType.DateTimeOpt[Option[Instant]]
        case LocalDateTime => DataType.DateTimeOpt[Option[LocalDateTime]]

  type TIMESTAMP[T <: Instant | LocalDateTime | Option[Instant | LocalDateTime]] = T match
    case Instant       => DataType.TimeStamp[Instant]
    case LocalDateTime => DataType.TimeStamp[LocalDateTime]
    case Option[t] =>
      t match
        case Instant       => DataType.TimeStampOpt[Option[Instant]]
        case LocalDateTime => DataType.TimeStampOpt[Option[LocalDateTime]]

  type TIME[T <: LocalTime | Option[LocalTime]] = T match
    case LocalTime         => DataType.Time[LocalTime]
    case Option[LocalTime] => DataType.TimeOpt[Option[LocalTime]]

  type YEAR[T <: Instant | LocalDate | JYear | Option[Instant | LocalDate | JYear]] = T match
    case Instant   => DataType.Year[Instant]
    case LocalDate => DataType.Year[LocalDate]
    case JYear     => DataType.Year[JYear]
    case Option[t] =>
      t match
        case Instant   => DataType.YearOpt[Option[Instant]]
        case LocalDate => DataType.YearOpt[Option[LocalDate]]
        case JYear     => DataType.YearOpt[Option[JYear]]
