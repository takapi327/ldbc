/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import java.time.*
import java.time.Year as JYear

/** Trait for representing SQL DataType
  *
  * @tparam T
  *   Scala types that match SQL DataType
  */
sealed trait DataType[T]:

  /** Define a TYPE_NAME string for each DataType.
    *
    * @return
    *   SQL TYPE_NAME
    */
  def typeName: String

  /** Value of JdbcType Enum that matches DataType.
    *
    * @return
    *   JdbcType Enum
    */
  def jdbcType: JdbcType

  /** Define SQL query string for each DataType
    *
    * @return
    *   SQL query string
    */
  def queryString: String

  /** Value indicating whether DataType is null-allowed or not.
    *
    * @return
    *   true if NULL is allowed, false if NULL is not allowed
    */
  def isOptional: Boolean

  /** Value to indicate whether NULL is acceptable as a query string in SQL
    */
  protected def nullType: String = if isOptional then "NULL" else "NOT NULL"

object DataType:

  /** Methods for mapping specific types to DataType.
    *
    * @tparam D
    *   Trait for representing SQL DataType
    * @tparam T
    *   Scala types that match SQL DataType
    */
  def mapping[D <: DataType[?], T]: Conversion[D, DataType[T]] =
    v =>
      new DataType[T]:
        override def typeName: String = v.typeName

        override def jdbcType: JdbcType = v.jdbcType

        override def queryString: String = v.queryString

        override def isOptional: Boolean = v.isOptional

  /** Trait for representing numeric data types in SQL DataType
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] trait IntegerType[
    T <: Byte | Short | Int | Long | Float | Double | BigDecimal | BigInt |
      Option[Byte | Short | Int | Long | Float | Double | BigDecimal | BigInt]
  ] extends DataType[T]:

    /** Maximum display width of integer data type
      */
    def length: Int

    /** SQL Default values
      */
    def default: Option[Default]

  /** SQL DataType to represent a string data type trait.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  sealed trait StringType[T <: Byte | Array[Byte] | String | Option[Byte | Array[Byte] | String]] extends DataType[T]:

    def character: Option[Character]

  /** SQL DataType to represent BLOB type of string data trait.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  sealed trait BlobType[T <: Array[Byte] | Option[Array[Byte]]] extends DataType[T]:

    def character: Option[Character]

  /** SQL DataType to represent date data types in trait.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  sealed trait DateType[
    T <: Instant | OffsetTime | LocalTime | LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime | JYear |
      Option[Instant | OffsetTime | LocalTime | LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime | JYear]
  ] extends DataType[T]

  /** ===== List of Numeric Data Types ===== */

  /** Model for representing the Bit data type, which is the numeric data of SQL DataType.
    *
    * @param length
    *   Maximum display width of integer data type. The length of the BIT must be in the range 1 to 64.
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Bit[
    T <: Byte | Short | Int | Long | Float | Double | BigDecimal |
      Option[Byte | Short | Int | Long | Float | Double | BigDecimal]
  ](
    length:     Int,
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = s"BIT($length)"

    override def jdbcType: JdbcType = JdbcType.Bit

    override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Bit[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

  /** Model for representing the Tinyint data type, which is the numeric data of SQL DataType.
    *
    * @param length
    *   Maximum display width of integer data type. The length of the TINYINT must be in the range 0 to 255.
    * @param isUnSigned
    *   Flag to set data type to unsigned
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Tinyint[T <: Byte | Short | Option[Byte | Short]](
    length:     Int,
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = s"TINYINT($length)"

    override def jdbcType: JdbcType = JdbcType.TinyInt

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Tinyint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting data type to unsigned.
      */
    def UNSIGNED: Tinyint[T] = this.copy(isUnSigned = true)

    /** Method for setting data type to zerofill.
      */
    def ZEROFILL: Tinyint[T] = this.copy(isZerofill = true)

  /** Model for representing the Smallint data type, which is the numeric data of SQL DataType.
    *
    * @param length
    *   Maximum display width of integer data type. The length of the SMALLINT must be in the range 0 to 255.
    * @param isUnSigned
    *   Flag to set data type to unsigned
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Smallint[T <: Short | Int | Option[Short | Int]](
    length:     Int,
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = s"SMALLINT($length)"

    override def jdbcType: JdbcType = JdbcType.SmallInt

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Smallint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting data type to unsigned.
      */
    def UNSIGNED: Smallint[T] = this.copy(isUnSigned = true)

    /** Method for setting data type to zerofill.
      */
    def ZEROFILL: Smallint[T] = this.copy(isZerofill = true)

  /** Model for representing the Mediumint data type, which is the numeric data of SQL DataType.
    *
    * @param length
    *   Maximum display width of integer data type. The length of the MEDIUMINT must be in the range 0 to 255.
    * @param isUnSigned
    *   Flag to set data type to unsigned
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Mediumint[T <: Int | Option[Int]](
    length:     Int,
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = s"MEDIUMINT($length)"

    override def jdbcType: JdbcType = JdbcType.Integer

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Mediumint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting data type to unsigned.
      */
    def UNSIGNED: Mediumint[T] = this.copy(isUnSigned = true)

    /** Method for setting data type to zerofill.
      */
    def ZEROFILL: Mediumint[T] = this.copy(isZerofill = true)

  /** Model for representing the Integer data type, which is the numeric data of SQL DataType.
    *
    * @param length
    *   Maximum display width of integer data type. The length of the INT must be in the range 0 to 255.
    * @param isUnSigned
    *   Flag to set data type to unsigned
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Integer[T <: Int | Long | Option[Int | Long]](
    length:     Int,
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = s"INT($length)"

    override def jdbcType: JdbcType = JdbcType.Integer

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Integer[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting data type to unsigned.
      */
    def UNSIGNED: Integer[T] = this.copy(isUnSigned = true)

    /** Method for setting data type to zerofill.
      */
    def ZEROFILL: Integer[T] = this.copy(isZerofill = true)

  /** Model for representing the Bigint data type, which is the numeric data of SQL DataType.
    *
    * @param length
    *   Maximum display width of integer data type. The length of the BIGINT must be in the range 0 to 255.
    * @param isUnSigned
    *   Flag to set data type to unsigned
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Bigint[T <: Long | BigInt | Option[Long | BigInt]](
    length:     Int,
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = s"BIGINT($length)"

    override def jdbcType: JdbcType = JdbcType.BigInt

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Bigint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting data type to unsigned.
      */
    def UNSIGNED: Bigint[T] = this.copy(isUnSigned = true)

    /** Method for setting data type to zerofill.
      */
    def ZEROFILL: Bigint[T] = this.copy(isZerofill = true)

  /** Model for representing the Decimal data type, which is the numeric data of SQL DataType.
    *
    * @param accuracy
    *   The value of accuracy for DECIMAL must be an integer.
    * @param scale
    *   The DECIMAL scale value must be an integer.
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Decimal[T <: BigDecimal | Option[BigDecimal]](
    accuracy:   Int,
    scale:      Int,
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = s"DECIMAL($accuracy, $scale)"

    override def jdbcType: JdbcType = JdbcType.Decimal

    override def queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Decimal[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting data type to unsigned.
      */
    def UNSIGNED: Decimal[T] = this.copy(isUnSigned = true)

    /** Method for setting data type to zerofill.
      */
    def ZEROFILL: Decimal[T] = this.copy(isZerofill = true)

  /** Model for representing the Float data type, which is the numeric data of SQL DataType.
    *
    * @param accuracy
    *   The length of the FLOAT must be in the range 0 to 24.
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class CFloat[T <: Double | Float | Option[Double | Float]](
    accuracy:   Int,
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = s"FLOAT($accuracy)"

    override def jdbcType: JdbcType = JdbcType.Float

    override def queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): CFloat[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting data type to unsigned.
      */
    def UNSIGNED: CFloat[T] = this.copy(isUnSigned = true)

    /** Method for setting data type to zerofill.
      */
    def ZEROFILL: CFloat[T] = this.copy(isZerofill = true)

  /** ===== List of String Data Types ===== */

  /** Model for representing the Char data type, which is the string data of SQL DataType.
    *
    * @param length
    *   Column character length
    * @param default
    *   SQL Default values
    * @param character
    *   Character Set and Collation
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class CChar[T <: String | Option[String]](
    length:     Int,
    isOptional: Boolean,
    default:    Option[Default]   = None,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = s"CHAR($length)"

    override def jdbcType: JdbcType = JdbcType.Char

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType" ++ default.fold("")(v =>
        s" ${ v.queryString }"
      )

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): CChar[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): CChar[T] = this.copy(character = Some(character))

  /** Model for representing the Varchar data type, which is the string data of SQL DataType.
    *
    * @param length
    *   Column character length
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Varchar[T <: String | Option[String]](
    length:     Int,
    isOptional: Boolean,
    default:    Option[Default]   = None,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = s"VARCHAR($length)"

    override def jdbcType: JdbcType = JdbcType.VarChar

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType" ++ default.fold("")(v =>
        s" ${ v.queryString }"
      )

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Varchar[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): Varchar[T] = this.copy(character = Some(character))

  /** Model for representing the Binary data type, which is the string data of SQL DataType.
    *
    * @param length
    *   Column character length
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Binary[T <: Array[Byte] | Option[Array[Byte]]](
    length:     Int,
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = s"BINARY($length)"

    override def jdbcType: JdbcType = JdbcType.Binary

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): Binary[T] = this.copy(character = Some(character))

  /** Model for representing the Varbinary data type, which is the string data of SQL DataType.
    *
    * @param length
    *   Column character length
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Varbinary[T <: Array[Byte] | Option[Array[Byte]]](
    length:     Int,
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = s"VARBINARY($length)"

    override def jdbcType: JdbcType = JdbcType.VarBinary

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): Varbinary[T] = this.copy(character = Some(character))

  /** Model for representing the Tinyblob data type, which is the string data of SQL DataType.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Tinyblob[T <: Array[Byte] | Option[Array[Byte]]](isOptional: Boolean) extends DataType[T]:

    override def typeName: String = "TINYBLOB"

    override def jdbcType: JdbcType = JdbcType.VarBinary

    override def queryString: String = s"$typeName $nullType"

  /** Model for representing the Blob data type, which is the string data of SQL DataType.
    *
    * @param length
    *   Column character length
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Blob[T <: Array[Byte] | Option[Array[Byte]]](
    length:     Option[Long],
    isOptional: Boolean
  ) extends DataType[T]:

    override def typeName: String = length.fold("BLOB")(n => s"BLOB($n)")

    override def jdbcType: JdbcType = JdbcType.Blob

    override def queryString: String = s"$typeName $nullType"

  /** Model for representing the Mediumblob data type, which is the string data of SQL DataType.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Mediumblob[T <: Array[Byte] | Option[Array[Byte]]](
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends BlobType[T]:

    override def typeName: String = "MEDIUMBLOB"

    override def jdbcType: JdbcType = JdbcType.LongVarBinary

    override def queryString: String = typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): Mediumblob[T] = this.copy(character = Some(character))

  /** Model for representing the LongBlob data type, which is the string data of SQL DataType.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class LongBlob[T <: Array[Byte] | Option[Array[Byte]]](
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends BlobType[T]:

    override def typeName: String = "LONGBLOB"

    override def jdbcType: JdbcType = JdbcType.LongVarBinary

    override def queryString: String = typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): LongBlob[T] = this.copy(character = Some(character))

  /** Model for representing the TinyText data type, which is the string data of SQL DataType.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class TinyText[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = "TINYTEXT"

    override def jdbcType: JdbcType = JdbcType.VarChar

    override def queryString: String = typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): TinyText[T] = this.copy(character = Some(character))

  /** Model for representing the Text data type, which is the string data of SQL DataType.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Text[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = "TEXT"

    override def jdbcType: JdbcType = JdbcType.LongVarChar

    override def queryString: String = typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): Text[T] = this.copy(character = Some(character))

  /** Model for representing the MediumText data type, which is the string data of SQL DataType.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class MediumText[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = "MEDIUMTEXT"

    override def jdbcType: JdbcType = JdbcType.LongVarChar

    override def queryString: String = typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): MediumText[T] = this.copy(character = Some(character))

  /** Model for representing the LongText data type, which is the string data of SQL DataType.
    *
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class LongText[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character] = None
  ) extends StringType[T]:

    override def typeName: String = "LONGTEXT"

    override def jdbcType: JdbcType = JdbcType.LongVarChar

    override def queryString: String = typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ s" $nullType"

    /** Method for setting Character Set and Collation to DataType in SQL.
      *
      * @param character
      *   Character Set and Collation
      */
    def CHARACTER_SET(character: Character): LongText[T] = this.copy(character = Some(character))

  /** ===== List of Date Data Types ===== */

  /** This model is used to represent SQL DataType date data.
    *
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Date[T <: LocalDate | Option[LocalDate]](
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DateType[T]:

    override def typeName: String = "DATE"

    override def jdbcType: JdbcType = JdbcType.Date

    override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Date[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

  /** This model is used to represent SQL DataType DateTime data.
    *
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class DateTime[
    T <: Instant | LocalDateTime | OffsetTime | Option[Instant | LocalDateTime | OffsetTime]
  ](
    fsp:        Option[Int],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DateType[T]:

    override def typeName: String = "DATETIME"

    override def jdbcType: JdbcType = JdbcType.Timestamp

    override def queryString: String = fsp.fold(typeName)(n => s"$typeName($n)") ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): DateTime[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Methods for setting default values for dates.
      *
      * @param onUpdate
      *   Value of whether to add settings on update
      */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): DateTime[T] =
      this.copy(default = Some(Default.TimeStamp(onUpdate)))

  /** This model is used to represent SQL DataType TimeStamp data.
    *
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class TimeStamp[
    T <: Instant | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[Instant | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
                                         fsp:        Option[Int],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DateType[T]:

    override def typeName: String = "TIMESTAMP"

    override def jdbcType: JdbcType = JdbcType.Timestamp

    override def queryString: String = fsp.fold(typeName)(n => s"$typeName($n)") ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): TimeStamp[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /** Methods for setting default values for dates.
      *
      * @param onUpdate
      *   Value of whether to add settings on update
      */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): TimeStamp[T] =
      this.copy(default = Some(Default.TimeStamp(onUpdate)))

  /** This model is used to represent SQL DataType Time data.
    *
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Time[T <: LocalTime | Option[LocalTime]](
                                                                     fsp:        Option[Int],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DateType[T]:

    override def typeName: String = "TIME"

    override def jdbcType: JdbcType = JdbcType.Time

    override def queryString: String = fsp.fold(typeName)(n => s"$typeName($n)") ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Time[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

  /** This model is used to represent SQL DataType Year data.
    *
    * @param default
    *   SQL Default values
    * @tparam T
    *   Scala types that match SQL DataType
    */
  private[ldbc] case class Year[T <: Instant | LocalDate | JYear | Option[Instant | LocalDate | JYear]](
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DateType[T]:

    override def typeName: String = "YEAR"

    override def jdbcType: JdbcType = JdbcType.Date

    override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /** Method for setting Default value to DataType in SQL.
      *
      * @param value
      *   Value set as the default value for DataType
      */
    def DEFAULT(value: T): Year[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))
