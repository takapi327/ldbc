/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core

import java.time.*
import java.time.Year as JYear

import scala.compiletime.{ constValue, erasedValue, error }
import scala.compiletime.ops.int.*
import scala.compiletime.ops.string.*

import ldbc.core.attribute.Attribute
import ldbc.core.interpreter.ExtractOption
import ldbc.core.model.{ Enum as EnumModel, EnumDataType }

/**
 * Trait for representing SQL DataType
 *
 * @tparam T
 *   Scala types that match SQL DataType
 */
sealed trait DataType[T]:
  self =>

  /**
   * Define a TYPE_NAME string for each DataType.
   *
   * @return
   *   SQL TYPE_NAME
   */
  def typeName: String

  /**
   * Value of JdbcType Enum that matches DataType.
   *
   * @return
   *   JdbcType Enum
   */
  def jdbcType: JdbcType

  /**
   * Define SQL query string for each DataType
   *
   * @return
   *   SQL query string
   */
  def queryString: String

  /**
   * Value indicating whether DataType is null-allowed or not.
   *
   * @return
   *   true if NULL is allowed, false if NULL is not allowed
   */
  def isOptional: Boolean

  /**
   * Default value to set for DataType.
   *
   * @return
   *   DataType default value
   */
  def default: Option[Default]

  /**
   * Value to indicate whether NULL is acceptable as a query string in SQL
   */
  protected def nullType: String = if isOptional then "NULL" else "NOT NULL"

  /** Methods for overriding the DataType type with the Option type. */
  def toOption: DataType[Option[ExtractOption[T]]] =
    new DataType[Option[ExtractOption[T]]]:
      override def typeName:    String          = self.typeName
      override def jdbcType:    JdbcType        = self.jdbcType
      override def queryString: String          = self.queryString
      override def isOptional:  Boolean         = true
      override def default:     Option[Default] = self.default

object DataType:

  /**
   * Methods for mapping specific types to DataType.
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

        override def default: Option[Default] = v.default

  /**
   * Trait for representing numeric data types in SQL DataType
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] trait IntegerType[
    T <: Byte | Short | Int | Long | Float | Double | BigDecimal | BigInt |
      Option[Byte | Short | Int | Long | Float | Double | BigDecimal | BigInt]
  ] extends DataType[T]:

    /**
     * Maximum display width of integer data type
     */
    def length: Option[Int]

    /**
     * Method for generating a TypeName based on the presence or absence of the display width attribute.
     *
     * @param name
     *   Data type name
     */
    protected def buildTypeName(name: String): String = length.fold(name)(n => s"$name($n)")

  /**
   * SQL DataType to represent a string data type trait.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait StringType[T <: Byte | Array[Byte] | String | Option[Byte | Array[Byte] | String]] extends DataType[T]:

    def character: Option[Character]

    def collate: Option[Collate[T]]

  /**
   * SQL DataType to represent BLOB type of string data trait.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait BlobType[T <: Array[Byte] | Option[Array[Byte]]] extends DataType[T]:

    def character: Option[Character]

  /** ===== List of Numeric Data Types ===== */

  /**
   * Model for representing the Bit data type, which is the numeric data of SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type. The length of the BIT must be in the range 1 to 64.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Bit[T <: Byte | Short | Int | Long | Option[Byte | Short | Int | Long]](
    length:     Option[Int],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = buildTypeName("BIT")

    override def jdbcType: JdbcType = JdbcType.Bit

    override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Bit[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

  /**
   * Model for representing the Tinyint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type. The length of the TINYINT must be in the range 0 to 255.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param isUnSigned
   *   Flag to set data type to unsigned
   * @param isZerofill
   *   Flag to set data type to zerofill
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Tinyint[T <: Byte | Short | Option[Byte | Short]](
    length:     Option[Int],
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = buildTypeName("TINYINT")

    override def jdbcType: JdbcType = JdbcType.TinyInt

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Tinyint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting data type to unsigned.
     */
    def UNSIGNED: Tinyint[T] = this.copy(isUnSigned = true)

    /**
     * Method for setting data type to zerofill.
     */
    @deprecated(
      """
      |As of MySQL 8.0.17, the ZEROFILL attribute is deprecated for numeric data types.
      |It will no longer be supported in future versions of MySQL.
      |Consider using an alternative method to produce the effect of this attribute.
      |For example, an application could use the LPAD() function to zero-fill a number to the required width or to store a formatted number in a CHAR column.
      |""".stripMargin,
      "Ldbc-Core 0.1.0"
    )
    def ZEROFILL: Tinyint[T] = this.copy(isZerofill = true)

  /**
   * Model for representing the Smallint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type. The length of the SMALLINT must be in the range 0 to 255.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param isUnSigned
   *   Flag to set data type to unsigned
   * @param isZerofill
   *   Flag to set data type to zerofill
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Smallint[T <: Short | Int | Option[Short | Int]](
    length:     Option[Int],
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = buildTypeName("SMALLINT")

    override def jdbcType: JdbcType = JdbcType.SmallInt

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Smallint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting data type to unsigned.
     */
    def UNSIGNED: Smallint[T] = this.copy(isUnSigned = true)

    /**
     * Method for setting data type to zerofill.
     */
    @deprecated(
      """
        |As of MySQL 8.0.17, the ZEROFILL attribute is deprecated for numeric data types.
        |It will no longer be supported in future versions of MySQL.
        |Consider using an alternative method to produce the effect of this attribute.
        |For example, an application could use the LPAD() function to zero-fill a number to the required width or to store a formatted number in a CHAR column.
        |""".stripMargin,
      "Ldbc-Core 0.1.0"
    )
    def ZEROFILL: Smallint[T] = this.copy(isZerofill = true)

  /**
   * Model for representing the Mediumint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type. The length of the MEDIUMINT must be in the range 0 to 255.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param isUnSigned
   *   Flag to set data type to unsigned
   * @param isZerofill
   *   Flag to set data type to zerofill
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Mediumint[T <: Int | Option[Int]](
    length:     Option[Int],
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = buildTypeName("MEDIUMINT")

    override def jdbcType: JdbcType = JdbcType.Integer

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Mediumint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting data type to unsigned.
     */
    def UNSIGNED: Mediumint[T] = this.copy(isUnSigned = true)

    /**
     * Method for setting data type to zerofill.
     */
    @deprecated(
      """
        |As of MySQL 8.0.17, the ZEROFILL attribute is deprecated for numeric data types.
        |It will no longer be supported in future versions of MySQL.
        |Consider using an alternative method to produce the effect of this attribute.
        |For example, an application could use the LPAD() function to zero-fill a number to the required width or to store a formatted number in a CHAR column.
        |""".stripMargin,
      "Ldbc-Core 0.1.0"
    )
    def ZEROFILL: Mediumint[T] = this.copy(isZerofill = true)

  /**
   * Model for representing the Integer data type, which is the numeric data of SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type. The length of the INT must be in the range 0 to 255.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param isUnSigned
   *   Flag to set data type to unsigned
   * @param isZerofill
   *   Flag to set data type to zerofill
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Integer[T <: Int | Long | Option[Int | Long]](
    length:     Option[Int],
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = buildTypeName("INT")

    override def jdbcType: JdbcType = JdbcType.Integer

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Integer[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting data type to unsigned.
     */
    def UNSIGNED: Integer[T] = this.copy(isUnSigned = true)

    /**
     * Method for setting data type to zerofill.
     */
    @deprecated(
      """
        |As of MySQL 8.0.17, the ZEROFILL attribute is deprecated for numeric data types.
        |It will no longer be supported in future versions of MySQL.
        |Consider using an alternative method to produce the effect of this attribute.
        |For example, an application could use the LPAD() function to zero-fill a number to the required width or to store a formatted number in a CHAR column.
        |""".stripMargin,
      "Ldbc-Core 0.1.0"
    )
    def ZEROFILL: Integer[T] = this.copy(isZerofill = true)

  /**
   * Model for representing the Bigint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type. The length of the BIGINT must be in the range 0 to 255.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param isUnSigned
   *   Flag to set data type to unsigned
   * @param isZerofill
   *   Flag to set data type to zerofill
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Bigint[T <: Long | BigInt | Option[Long | BigInt]](
    length:     Option[Int],
    isOptional: Boolean,
    isUnSigned: Boolean         = false,
    isZerofill: Boolean         = false,
    default:    Option[Default] = None
  ) extends IntegerType[T]:

    override def typeName: String = buildTypeName("BIGINT")

    override def jdbcType: JdbcType = JdbcType.BigInt

    override val queryString: String =
      (isUnSigned, isZerofill) match
        case (true, true)   => s"$typeName UNSIGNED ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (true, false)  => s"$typeName UNSIGNED $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, true)  => s"$typeName ZEROFILL $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
        case (false, false) => s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Bigint[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting data type to unsigned.
     */
    def UNSIGNED: Bigint[T] = this.copy(isUnSigned = true)

    /**
     * Method for setting data type to zerofill.
     */
    @deprecated(
      """
        |As of MySQL 8.0.17, the ZEROFILL attribute is deprecated for numeric data types.
        |It will no longer be supported in future versions of MySQL.
        |Consider using an alternative method to produce the effect of this attribute.
        |For example, an application could use the LPAD() function to zero-fill a number to the required width or to store a formatted number in a CHAR column.
        |""".stripMargin,
      "Ldbc-Core 0.1.0"
    )
    def ZEROFILL: Bigint[T] = this.copy(isZerofill = true)

  /**
   * Model for representing the Decimal data type, which is the numeric data of SQL DataType.
   *
   * @param accuracy
   *   The value of accuracy for DECIMAL must be an integer.
   * @param scale
   *   The DECIMAL scale value must be an integer.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param isUnSigned
   *   Flag to set data type to unsigned
   * @param isZerofill
   *   Flag to set data type to zerofill
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

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Decimal[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting data type to unsigned.
     */
    def UNSIGNED: Decimal[T] = this.copy(isUnSigned = true)

    /**
     * Method for setting data type to zerofill.
     */
    @deprecated(
      """
        |As of MySQL 8.0.17, the ZEROFILL attribute is deprecated for numeric data types.
        |It will no longer be supported in future versions of MySQL.
        |Consider using an alternative method to produce the effect of this attribute.
        |For example, an application could use the LPAD() function to zero-fill a number to the required width or to store a formatted number in a CHAR column.
        |""".stripMargin,
      "Ldbc-Core 0.1.0"
    )
    def ZEROFILL: Decimal[T] = this.copy(isZerofill = true)

  /**
   * Model for representing the Float data type, which is the numeric data of SQL DataType.
   *
   * @param accuracy
   *   The length of the FLOAT must be in the range 0 to 24.
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param isUnSigned
   *   Flag to set data type to unsigned
   * @param isZerofill
   *   Flag to set data type to zerofill
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

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: Double | Float | Option[Double | Float]): CFloat[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting data type to unsigned.
     */
    def UNSIGNED: CFloat[T] = this.copy(isUnSigned = true)

    /**
     * Method for setting data type to zerofill.
     */
    @deprecated(
      """
        |As of MySQL 8.0.17, the ZEROFILL attribute is deprecated for numeric data types.
        |It will no longer be supported in future versions of MySQL.
        |Consider using an alternative method to produce the effect of this attribute.
        |For example, an application could use the LPAD() function to zero-fill a number to the required width or to store a formatted number in a CHAR column.
        |""".stripMargin,
      "Ldbc-Core 0.1.0"
    )
    def ZEROFILL: CFloat[T] = this.copy(isZerofill = true)

  /** ===== List of String Data Types ===== */

  /**
   * Model for representing the Char data type, which is the string data of SQL DataType.
   *
   * @param length
   *   Column character length
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class CChar[T <: String | Option[String]](
    length:     Int,
    isOptional: Boolean,
    default:    Option[Default]    = None,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None
  ) extends StringType[T]:

    override def typeName: String = s"CHAR($length)"

    override def jdbcType: JdbcType = JdbcType.Char

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): CChar[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): CChar[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): CChar[T] = this.copy(collate = Some(collate))

  /**
   * Model for representing the Varchar data type, which is the string data of SQL DataType.
   *
   * @param length
   *   Column character length
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Varchar[T <: String | Option[String]](
    length:     Int,
    isOptional: Boolean,
    default:    Option[Default]    = None,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None
  ) extends StringType[T]:

    override def typeName: String = s"VARCHAR($length)"

    override def jdbcType: JdbcType = JdbcType.VarChar

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Varchar[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): Varchar[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): Varchar[T] = this.copy(collate = Some(collate))

  /**
   * Model for representing the Binary data type, which is the string data of SQL DataType.
   *
   * @param length
   *   Column character length
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Binary[T <: Array[Byte] | Option[Array[Byte]]](
    length:     Int,
    isOptional: Boolean,
    default:    Option[Default]    = None,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None
  ) extends StringType[T]:

    override def typeName: String = s"BINARY($length)"

    override def jdbcType: JdbcType = JdbcType.Binary

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Binary[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): Binary[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): Binary[T] = this.copy(collate = Some(collate))

  /**
   * Model for representing the Varbinary data type, which is the string data of SQL DataType.
   *
   * @param length
   *   Column character length
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Varbinary[T <: Array[Byte] | Option[Array[Byte]]](
    length:     Int,
    isOptional: Boolean,
    default:    Option[Default]    = None,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None
  ) extends StringType[T]:

    override def typeName: String = s"VARBINARY($length)"

    override def jdbcType: JdbcType = JdbcType.VarBinary

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Varbinary[T] = value match
      case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
      case v            => this.copy(default = Some(Default.Value(v)))

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): Varbinary[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): Varbinary[T] = this.copy(collate = Some(collate))

  /**
   * Model for representing the Tinyblob data type, which is the string data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Tinyblob[T <: Array[Byte] | Option[Array[Byte]]](
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "TINYBLOB"

    override def jdbcType: JdbcType = JdbcType.VarBinary

    override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): Tinyblob[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the Blob data type, which is the string data of SQL DataType.
   *
   * @param length
   *   Column character length
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Blob[T <: Array[Byte] | Option[Array[Byte]]](
    length:     Option[Long],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = length.fold("BLOB")(n => s"BLOB($n)")

    override def jdbcType: JdbcType = JdbcType.Blob

    override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): Blob[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the Mediumblob data type, which is the string data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Mediumblob[T <: Array[Byte] | Option[Array[Byte]]](
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "MEDIUMBLOB"

    override def jdbcType: JdbcType = JdbcType.LongVarBinary

    override def queryString: String = typeName ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): Mediumblob[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the LongBlob data type, which is the string data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class LongBlob[T <: Array[Byte] | Option[Array[Byte]]](
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "LONGBLOB"

    override def jdbcType: JdbcType = JdbcType.LongVarBinary

    override def queryString: String = typeName ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): LongBlob[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the TinyText data type, which is the string data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TinyText[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None,
    default:    Option[Default]    = None
  ) extends StringType[T]:

    override def typeName: String = "TINYTEXT"

    override def jdbcType: JdbcType = JdbcType.VarChar

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): TinyText[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): TinyText[T] = this.copy(collate = Some(collate))

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): TinyText[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the Text data type, which is the string data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Text[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None,
    default:    Option[Default]    = None
  ) extends StringType[T]:

    override def typeName: String = "TEXT"

    override def jdbcType: JdbcType = JdbcType.LongVarChar

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): Text[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): Text[T] = this.copy(collate = Some(collate))

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): Text[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the MediumText data type, which is the string data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class MediumText[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None,
    default:    Option[Default]    = None
  ) extends StringType[T]:

    override def typeName: String = "MEDIUMTEXT"

    override def jdbcType: JdbcType = JdbcType.LongVarChar

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): MediumText[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): MediumText[T] = this.copy(collate = Some(collate))

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): MediumText[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the LongText data type, which is the string data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param character
   *   Character Set
   * @param collate
   *   Collation
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class LongText[T <: String | Option[String]](
    isOptional: Boolean,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None,
    default:    Option[Default]    = None
  ) extends StringType[T]:

    override def typeName: String = "LONGTEXT"

    override def jdbcType: JdbcType = JdbcType.LongVarChar

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): LongText[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): LongText[T] = this.copy(collate = Some(collate))

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): LongText[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case v: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case _          => error(s"$typeName can be set to NULL as the default value only if NULL is allowed.")

  /**
   * Model for representing the Enum data type, which is the enum data of SQL DataType.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Enum[T <: EnumModel | Option[EnumModel]](
    isOptional: Boolean,
    character:  Option[Character]  = None,
    collate:    Option[Collate[T]] = None,
    default:    Option[Default]    = None
  )(using enumDataType: EnumDataType[?])
    extends DataType[T]:

    override def typeName: String = s"ENUM(${ enumDataType.values.map(v => s"'$v'").mkString(",") })"

    override def jdbcType: JdbcType = JdbcType.Char

    override def queryString: String =
      typeName ++ character.fold("")(v => s" ${ v.queryString }") ++ collate.fold("")(v =>
        s" ${ v.queryString }"
      ) ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     *   Character Set
     */
    def CHARACTER_SET(character: Character): Enum[T] = this.copy(character = Some(character))

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     *   Collation
     */
    def COLLATE(collate: Collate[T]): Enum[T] = this.copy(collate = Some(collate))

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T): Enum[T] = inline value match
      case None       => this.copy(default = Some(Default.Null))
      case _: Some[?] => error(s"$typeName cannot have a default value other than NULL.")
      case v          => this.copy(default = Some(Default.Value(v)))

  /** ===== List of Date Data Types ===== */

  /**
   * This model is used to represent SQL DataType date data.
   *
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Date[T <: String | LocalDate | Option[String | LocalDate]](
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "DATE"

    override def jdbcType: JdbcType = JdbcType.Date

    override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T | String | 0 | Some[Int]): Date[T] =
      inline erasedValue[value.type] match
        case _: Option[?] =>
          this.copy(default = Some(value.asInstanceOf[Option[?]].fold(Default.Null)(Default.Value(_))))
        case v: String =>
          inline if constValue[
              Matches[v.type, """^(1000|100[0-9]|[1-9][0-9]{3})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$"""]
            ]
          then this.copy(default = Some(Default.Value(value)))
          else
            error(
              "The DATE type must be passed a string in the format YYYY-MM-DD, ranging from '1000-01-01' to '9999-12-31'."
            )
        case _: 0         => this.copy(default = Some(Default.Value(value)))
        case _: Some[Int] => this.copy(default = Some(Default.Value(value.asInstanceOf[Some[Int]].get)))
        case _: LocalDate => this.copy(default = Some(Default.Value(value)))

    /**
     * Methods for setting default values for dates.
     */
    def DEFAULT_CURRENT_DATE(): Date[T] = this.copy(default = Some(Default.Date()))

  /**
   * This model is used to represent SQL DataType DateTime data.
   *
   * @param fsp
   *   Accuracy to microseconds (6 digits)
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class DateTime[
    T <: String | Instant | LocalDateTime | OffsetTime | Option[String | Instant | LocalDateTime | OffsetTime]
  ](
    fsp:        Option[0 | 1 | 2 | 3 | 4 | 5 | 6],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "DATETIME"

    override def jdbcType: JdbcType = JdbcType.Timestamp

    override def queryString: String =
      fsp.fold(typeName)(n => s"$typeName($n)") ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T | String | 0 | Some[Int]): DateTime[T] =
      inline erasedValue[value.type] match
        case _: Option[?] =>
          this.copy(default = Some(value.asInstanceOf[Option[?]].fold(Default.Null)(Default.Value(_))))
        case v: String =>
          inline if constValue[
              Matches[
                v.type,
                """^(1000|100[0-9]|[1-9][0-9]{3})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]) (0[0-9]|1[0-9]|2[0-3]):([0-5]\d):([0-5]\d)$"""
              ]
            ]
          then this.copy(default = Some(Default.Value(value)))
          else
            error(
              "The DATETIME type must be passed a string in the format YYYY-MM-DD hh:mm:ss, ranging from '1000-01-01 00:00:00' to '9999-12-31 23:59:59'."
            )
        case _: 0         => this.copy(default = Some(Default.Value(value)))
        case _: Some[Int] => this.copy(default = Some(Default.Value(value.asInstanceOf[Some[Int]].get)))
        case _: (Instant | LocalDateTime | OffsetTime) => this.copy(default = Some(Default.Value(value)))

    /**
     * Methods for setting default values for dates.
     *
     * @param onUpdate
     *   Value of whether to add settings on update
     */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): DateTime[T] =
      this.copy(default = Some(Default.TimeStamp(fsp, onUpdate)))

  /**
   * This model is used to represent SQL DataType TimeStamp data.
   *
   * @param fsp
   *   Accuracy to microseconds (6 digits)
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TimeStamp[
    T <: String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime |
      Option[String | Instant | LocalDateTime | OffsetDateTime | ZonedDateTime]
  ](
    fsp:        Option[0 | 1 | 2 | 3 | 4 | 5 | 6],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "TIMESTAMP"

    override def jdbcType: JdbcType = JdbcType.Timestamp

    override def queryString: String =
      fsp.fold(typeName)(n => s"$typeName($n)") ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T | String | 0 | Some[Int]): TimeStamp[T] =
      inline erasedValue[value.type] match
        case _: Option[?] =>
          this.copy(default = Some(value.asInstanceOf[Option[?]].fold(Default.Null)(Default.Value(_))))
        case v: String =>
          inline if constValue[
              Matches[
                v.type,
                """^(1000|100[0-9]|[1-9][0-9]{3})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]) (0[0-9]|1[0-9]|2[0-3]):([0-5]\d):([0-5]\d)$"""
              ]
            ]
          then this.copy(default = Some(Default.Value(value)))
          else
            error(
              "The TIMESTAMP type must be passed a string in the format YYYY-MM-DD hh:mm:ss, ranging from '1000-01-01 00:00:00' to '9999-12-31 23:59:59'."
            )
        case _: 0         => this.copy(default = Some(Default.Value(value)))
        case _: Some[Int] => this.copy(default = value.asInstanceOf[Some[Int]].map(Default.Value(_)))
        case _: (Instant | LocalDateTime | OffsetDateTime | ZonedDateTime) =>
          this.copy(default = Some(Default.Value(value)))

    /**
     * Methods for setting default values for dates.
     *
     * @param onUpdate
     *   Value of whether to add settings on update
     */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): TimeStamp[T] =
      this.copy(default = Some(Default.TimeStamp(fsp, onUpdate)))

  /**
   * This model is used to represent SQL DataType Time data.
   *
   * @param fsp
   *   Accuracy to microseconds (6 digits)
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Time[T <: String | LocalTime | Option[String | LocalTime]](
    fsp:        Option[0 | 1 | 2 | 3 | 4 | 5 | 6],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "TIME"

    override def jdbcType: JdbcType = JdbcType.Time

    override def queryString: String =
      fsp.fold(typeName)(n => s"$typeName($n)") ++ s" $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T | String | 0 | Some[Int]): Time[T] =
      inline erasedValue[value.type] match
        case _: Option[?] =>
          this.copy(default = Some(value.asInstanceOf[Option[?]].fold(Default.Null)(Default.Value(_))))
        case v: String =>
          inline if constValue[
              Matches[v.type, """^(-?8[0-3][0-8]|-?[0-7][0-9]{2}|0?[0-9]{1,2}):([0-5]\d):([0-5]\d)$"""]
            ]
          then this.copy(default = Some(Default.Value(value)))
          else
            error(
              "A string in hh:mm:ss or hhh:mm:ss format and in the range from '-838:59:59' to '838:59:59' must be passed to the TIME type."
            )
        case _: 0         => this.copy(default = Some(Default.Value(value)))
        case _: Some[Int] => this.copy(default = value.asInstanceOf[Some[Int]].map(Default.Value(_)))
        case _: LocalTime => this.copy(default = Some(Default.Value(value)))

  /**
   * This model is used to represent SQL DataType Year data.
   *
   * @param digit
   *   Display width
   * @param isOptional
   *   Value indicating whether DataType is null-allowed or not.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Year[T <: Int | Instant | LocalDate | JYear | Option[Int | Instant | LocalDate | JYear]](
    digit:      Option[4],
    isOptional: Boolean,
    default:    Option[Default] = None
  ) extends DataType[T]:

    override def typeName: String = "YEAR"

    override def jdbcType: JdbcType = JdbcType.Date

    override def queryString: String =
      digit.fold(s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }"))(n =>
        s"$typeName($n) $nullType" ++ default.fold("")(v => s" ${ v.queryString }")
      )

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    inline def DEFAULT(value: T | Int): Year[T] =
      inline erasedValue[value.type] match
        case _: Option[?] =>
          this.copy(default = Some(value.asInstanceOf[Option[?]].fold(Default.Null)(Default.Value(_))))
        case v: Int =>
          // TODO: Scalafmt removes extra parentheses that wrap the entire condition.
          //       It detects it as extra parentheses, but if you remove those parentheses, the if ... then writing method results in an error
          //       Therefore, the variable is used to avoid the error.
          inline val bool = (constValue[v.type >= 0] & constValue[v.type <= 0]) | (constValue[
            v.type >= 1901
          ] & constValue[v.type <= 2155])
          inline if bool then this.copy(default = Some(Default.Value(value)))
          else error("Only values in the range 0 or 1901 to 2155 can be passed to the YEAR type.")
        case _ => this.copy(default = Some(Default.Value(value)))

  /**
   * Alias for DataType
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] trait Alias[T] extends DataType[T]:

    /**
     * Extra attribute of column
     */
    def attributes: Seq[Attribute[T]]

  private[ldbc] object Alias:

    /**
     * Alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE
     *
     * @tparam T
     *   Scala types that match SQL DataType
     */
    case class Serial[T <: BigInt]() extends Alias[T]:

      override def typeName: String = "BIGINT"

      override def jdbcType: JdbcType = JdbcType.BigInt

      override def isOptional: Boolean = false

      override val queryString: String =
        s"$typeName UNSIGNED $nullType"

      override def default: Option[Default] = None

      override def attributes: Seq[Attribute[T]] = Seq(
        AUTO_INCREMENT,
        UNIQUE_KEY
      )

    /**
     * Alias for TINYINT(1)
     *
     * @param isOptional
     *   Value indicating whether DataType is null-allowed or not.
     * @param default
     *   SQL Default values
     * @tparam T
     *   Scala types that match SQL DataType
     */
    case class Bool[T <: Boolean | Option[Boolean]](
      isOptional: Boolean,
      default:    Option[Default] = None
    ) extends Alias[T]:

      override def typeName: String = "BOOLEAN"

      override def jdbcType: JdbcType = JdbcType.Boolean

      override def queryString: String = s"$typeName $nullType" ++ default.fold("")(v => s" ${ v.queryString }")

      override def attributes: Seq[Attribute[T]] = Seq.empty

      /**
       * Method for setting Default value to DataType in SQL.
       *
       * @param value
       *   Value set as the default value for DataType
       */
      def DEFAULT(value: T | Int | Option[Int]): Bool[T] = value match
        case v: Option[?] => this.copy(default = Some(v.fold(Default.Null)(Default.Value(_))))
        case v            => this.copy(default = Some(Default.Value(v)))
