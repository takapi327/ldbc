/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

package ldbc.sql

import java.time.*
import java.time.Year as JYear

/**
 * Trait for representing SQL DataType
 *
 * @tparam T
 *   Scala types that match SQL DataType
 */
sealed trait DataType[T]:

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
  def isOptional: Boolean = false

  /**
   * Value to indicate whether NULL is acceptable as a query string in SQL
   */
  protected val nullType: String = if isOptional then "NULL" else "NOT NULL"

object DataType:

  /**
   * Trait for representing numeric data types in SQL DataType
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait IntegerType[T <: Byte | Short | Int | Long | Float | Double | BigDecimal] extends DataType[T]:

    /**
     * Maximum display width of integer data type
     */
    def length: Int

    /**
     * SQL Default values
     */
    def default: Option[Default]

  /**
   * SQL DataType to represent a numeric data type with NULL tolerance trait.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait IntegerOptType[T <: Option[Byte | Short | Int | Long | Float | Double | BigDecimal]] extends DataType[T]:

    /**
     * Maximum display width of integer data type
     */
    def length: Int

    /**
     * SQL Default values
     */
    def default: Option[Default]

    override def isOptional: Boolean = true

  /**
   * SQL DataType to represent a string data type trait.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait StringType[T <: Byte | Array[Byte] | String] extends DataType[T]

  /**
   * SQL DataType to represent a string data type with NULL tolerance trait.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait StringOptType[T <: Option[Byte | Array[Byte] | String]] extends DataType[T]:

    override def isOptional: Boolean = true

  /**
   * SQL DataType to represent BLOB type of string data trait.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait BlobType[T <: Array[Byte]] extends DataType[T]

  /**
   * Trait for representing BLOB type of string data that is NULL-allowed by SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait BlobOptType[T <: Option[Array[Byte]]] extends DataType[T]:
    override def isOptional: Boolean = true

  /**
   * List of Java time data types
   */
  private type DateTypes = Instant | OffsetTime | LocalTime | LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime | JYear

  /**
   * SQL DataType to represent date data types in trait.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait DateType[T <: Instant | OffsetTime | LocalTime | LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime | JYear] extends DataType[T]

  /**
   * SQL DataType trait for representing NULL-allowed date data types.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  sealed trait DateOptType[T <: Option[Instant | OffsetTime | LocalTime | LocalDate | LocalDateTime | OffsetDateTime | ZonedDateTime | JYear]] extends DataType[T]:
    override def isOptional: Boolean = true

  /** List of Numeric Data Types */

  /**
   * Model for representing the Bit data type, which is the numeric data of SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type.
   *   The length of the BIT must be in the range 1 to 64.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Bit[T <: Byte | Short | Int | Long | Float | Double | BigDecimal](
    length:  Int,
    default: Option[Default]
  ) extends IntegerType[T]:

    override def queryString: String = s"BIT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): Bit[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Bit data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param length
   *   Maximum display width of integer data type.
   *   The length of the BIT must be in the range 1 to 64.
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class BitOpt[T <: Option[Byte | Short | Int | Long | Float | Double | BigDecimal]](
    length:  Int,
    default: Option[Default]
  ) extends IntegerOptType[T]:

    override def queryString: String = s"BIT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     *   Value set as the default value for DataType
     */
    def DEFAULT(value: T): BitOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: BitOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Tinyint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the TINYINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Tinyint[T <: Byte](
    length: Int,
    default: Option[Default],
  ) extends IntegerType[T]:

    override def queryString: String = s"TINYINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Tinyint[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Bit data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the TINYINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TinyintOpt[T <: Option[Byte]](
    length: Int,
    default: Option[Default]
  ) extends IntegerOptType[T]:

    override def queryString: String = s"TINYINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): TinyintOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: TinyintOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Smallint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the SMALLINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Smallint[T <: Short](length: Int, default: Option[Default]) extends IntegerType[T]:

    override def queryString: String = s"SMALLINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Smallint[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Smallint data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the SMALLINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class SmallintOpt[T <: Option[Short]](length: Int, default: Option[Default]) extends IntegerOptType[T]:

    override def queryString: String = s"SMALLINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): SmallintOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: SmallintOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Mediumint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the MEDIUMINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Mediumint[T <: Int](length: Int, default: Option[Default]) extends IntegerType[T]:

    override def queryString: String = s"MEDIUMINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Mediumint[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Mediumint data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the MEDIUMINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class MediumintOpt[T <: Option[Int]](length: Int, default: Option[Default]) extends IntegerOptType[T]:

    override def queryString: String = s"MEDIUMINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): MediumintOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: MediumintOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Integer data type, which is the numeric data of SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the INT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Integer[T <: Int](length: Int, default: Option[Default]) extends IntegerType[T]:

    override def queryString: String = s"INT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Integer[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Integer data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the INT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class IntegerOpt[T <: Option[Int]](length: Int, default: Option[Default]) extends IntegerOptType[T]:

    override def queryString: String = s"INT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): IntegerOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: IntegerOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Bigint data type, which is the numeric data of SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the BIGINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Bigint[T <: Long](length: Int, default: Option[Default]) extends IntegerType[T]:

    override def queryString: String = s"BIGINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Bigint[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Bigint data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Maximum display width of integer data type.
   * The length of the BIGINT must be in the range 0 to 255.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class BigintOpt[T <: Option[Long]](length: Int, default: Option[Default]) extends IntegerOptType[T]:

    override def queryString: String = s"BIGINT($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): BigintOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: BigintOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Decimal data type, which is the numeric data of SQL DataType.
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
  private[ldbc] case class Decimal[T <: BigDecimal](accuracy: Int, scale: Int, default: Option[Default]) extends DataType[T]:

    override def queryString: String = s"DECIMAL($accuracy, $scale) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Decimal[T] = this.copy(accuracy, scale, Some(Default.Value(value)))

  /**
   * Model for representing the Decimal data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param accuracy
   * The value of accuracy for DECIMAL must be an integer.
   * @param scale
   * The DECIMAL scale value must be an integer.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class DecimalOpt[T <: Option[BigDecimal]](
    accuracy: Int,
    scale: Int,
    default: Option[Default]
  ) extends DataType[T]:

    override def queryString: String = s"DECIMAL($accuracy, $scale) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    override def isOptional: Boolean = true

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): DecimalOpt[T] = this.copy(accuracy, scale, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: DecimalOpt[T] = this.copy(accuracy, scale, Some(Default.Null))

  /**
   * Model for representing the Float data type, which is the numeric data of SQL DataType.
   *
   * @param accuracy
   * The length of the FLOAT must be in the range 0 to 24.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class CFloat[T <: Double | Float](
    accuracy: Int,
    default: Option[Default]
  ) extends DataType[T]:

    override def queryString: String = s"FLOAT($accuracy) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): CFloat[T] = this.copy(accuracy, Some(Default.Value(value)))

  /**
   * Model for representing the Float data type, which is numeric data with NULL tolerance for SQL DataType.
   *
   * @param accuracy
   * The length of the FLOAT must be in the range 0 to 24.
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class FloatOpt[T <: Option[Double | Float]](
    accuracy: Int,
    default: Option[Default]
  ) extends DataType[T]:

    override def queryString: String = s"FLOAT($accuracy) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    override def isOptional: Boolean = true

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): FloatOpt[T] = this.copy(accuracy, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: FloatOpt[T] = this.copy(accuracy, Some(Default.Null))

  /** List of String Data Types */

  /**
   * Model for representing the Char data type, which is the string data of SQL DataType.
   *
   * @param length
   *   Column character length
   * @param default
   *   SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class CChar[T <: String](
    length: Int,
    default: Option[Default],
  ) extends StringType[T]:

    override def queryString: String = s"CHAR($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): CChar[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Char data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Column character length
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class CharOpt[T <: Option[String]](
    length: Int,
    default: Option[Default],
  ) extends StringOptType[T]:

    override def queryString: String = s"CHAR($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): CharOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: CharOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Varchar data type, which is the string data of SQL DataType.
   *
   * @param length
   * Column character length
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Varchar[T <: String](
    length: Int,
    default: Option[Default],
  ) extends StringType[T]:

    override def queryString: String = s"VARCHAR($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Varchar[T] = this.copy(length, Some(Default.Value(value)))

  /**
   * Model for representing the Varchar data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Column character length
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class VarcharOpt[T <: Option[String]](
    length: Int,
    default: Option[Default],
  ) extends StringOptType[T]:

    override def queryString: String = s"VARCHAR($length) $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): VarcharOpt[T] = this.copy(length, Some(Default.Value(value)))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: VarcharOpt[T] = this.copy(length, Some(Default.Null))

  /**
   * Model for representing the Binary data type, which is the string data of SQL DataType.
   *
   * @param length
   * Column character length
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Binary[T <: Array[Byte]](length: Int) extends StringType[T]:

    override def queryString: String = s"BINARY($length) $nullType"

  /**
   * Model for representing the Binary data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Column character length
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class BinaryOpt[T <: Option[Array[Byte]]](length: Int) extends StringOptType[T]:

    override def queryString: String = s"BINARY($length) $nullType"

  /**
   * Model for representing the Varbinary data type, which is the string data of SQL DataType.
   *
   * @param length
   * Column character length
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Varbinary[T <: Array[Byte]](length: Int) extends StringType[T]:

    override def queryString: String = s"VARBINARY($length) $nullType"

  /**
   * Model for representing the Varbinary data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Column character length
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class VarbinaryOpt[T <: Option[Array[Byte]]](length: Int) extends StringOptType[T]:

    override def queryString: String = s"VARBINARY($length) $nullType"

  /**
   * Model for representing the Tinyblob data type, which is the string data of SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Tinyblob[T <: Array[Byte]]() extends BlobType[T]:

    override def queryString: String = s"TINYBLOB $nullType"

  /**
   * Model for representing the Tinyblob data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TinyblobOpt[T <: Option[Array[Byte]]]() extends BlobOptType[T]:

    override def queryString: String = s"TINYBLOB $nullType"

  /**
   * Model for representing the Blob data type, which is the string data of SQL DataType.
   *
   * @param length
   * Column character length
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Blob[T <: Array[Byte]](length: Long) extends BlobType[T]:

    override def queryString: String = s"BLOB($length) $nullType"

  /**
   * Model for representing the Blob data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @param length
   * Column character length
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class BlobOpt[T <: Option[Array[Byte]]](length: Long) extends BlobOptType[T]:

    override def queryString: String = s"BLOB($length) $nullType"

  /**
   * Model for representing the Mediumblob data type, which is the string data of SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Mediumblob[T <: Array[Byte]]() extends BlobType[T]:

    override def queryString: String = s"MEDIUMBLOB $nullType"

  /**
   * Model for representing the Mediumblob data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class MediumblobOpt[T <: Option[Array[Byte]]]() extends BlobOptType[T]:

    override def queryString: String = s"MEDIUMBLOB $nullType"

  /**
   * Model for representing the LongBlob data type, which is the string data of SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class LongBlob[T <: Array[Byte]]() extends BlobType[T]:

    override def queryString: String = s"LONGBLOB $nullType"

  /**
   * Model for representing the LongBlob data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class LongBlobOpt[T <: Option[Array[Byte]]]() extends BlobOptType[T]:

    override def queryString: String = s"LONGBLOB $nullType"

  /**
   * Model for representing the TinyText data type, which is the string data of SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TinyText[T <: String]() extends StringType[T]:

    override def queryString: String = s"TINYTEXT $nullType"

  /**
   * Model for representing the TinyText data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TinyTextOpt[T <: Option[String]]() extends StringOptType[T]:

    override def queryString: String = s"TINYTEXT $nullType"

  /**
   * Model for representing the Text data type, which is the string data of SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Text[T <: String]() extends StringType[T]:

    override def queryString: String = s"TEXT $nullType"

  /**
   * Model for representing the Text data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TextOpt[T <: Option[String]]() extends StringOptType[T]:

    override def queryString: String = s"TEXT $nullType"

  /**
   * Model for representing the MediumText data type, which is the string data of SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class MediumText[T <: String]() extends StringType[T]:

    override def queryString: String = s"MEDIUMTEXT $nullType"

  /**
   * Model for representing the MediumText data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class MediumTextOpt[T <: Option[String]]() extends StringOptType[T]:

    override def queryString: String = s"MEDIUMTEXT $nullType"

  /**
   * Model for representing the LongText data type, which is the string data of SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class LongText[T <: String]() extends StringType[T]:

    override def queryString: String = s"LONGTEXT $nullType"

  /**
   * Model for representing the LongText data type, which is string data with NULL tolerance for SQL DataType.
   *
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class LongTextOpt[T <: Option[String]]() extends StringOptType[T]:

    override def queryString: String = s"LONGTEXT $nullType"

  /** List of Date Data Types */

  /**
   * This model is used to represent SQL DataType date data.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Date[T <: LocalDate](default: Option[Default]) extends DateType[T]:

    override def queryString: String = s"DATE $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Date[T] = this.copy(Some(Default.Value(value)))

  /**
   * This model is used to represent NULL-allowed date data of SQL DataType.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class DateOpt[T <: Option[LocalDate]](default: Option[Default]) extends DateOptType[T]:

    override def queryString: String = s"DATE $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): DateOpt[T] = this.copy(Some(value.fold(Default.Null)(Default.Value(_))))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: DateOpt[T] = this.copy(Some(Default.Null))

  /**
   * This model is used to represent SQL DataType DateTime data.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class DateTime[T <: Instant | LocalDateTime](default: Option[Default]) extends DateType[T]:

    override def queryString: String = s"DATETIME $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): DateTime[T] = this.copy(Some(Default.Value(value)))

    /**
     * Methods for setting default values for dates.
     *
     * @param onUpdate
     * Value of whether to add settings on update
     */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): DateTime[T] = this.copy(Some(Default.TimeStamp(onUpdate)))

  /**
   * This model is used to represent NULL-allowed DateTime data of SQL DataType.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class DateTimeOpt[T <: Option[Instant | LocalDateTime]](default: Option[Default]) extends DateOptType[T]:

    override def queryString: String = s"DATETIME $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): DateTimeOpt[T] = this.copy(Some(value.fold(Default.Null)(Default.Value(_))))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: DateTimeOpt[T] = this.copy(Some(Default.Null))

    /**
     * Methods for setting default values for dates.
     *
     * @param onUpdate
     * Value of whether to add settings on update
     */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): DateTimeOpt[T] = this.copy(Some(Default.TimeStamp(onUpdate)))

  /**
   * This model is used to represent SQL DataType TimeStamp data.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TimeStamp[T <: Instant | LocalDateTime](default: Option[Default]) extends DateType[T]:

    override def queryString: String = s"TIMESTAMP $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): TimeStamp[T] = this.copy(Some(Default.Value(value)))

    /**
     * Methods for setting default values for dates.
     *
     * @param onUpdate
     * Value of whether to add settings on update
     */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): TimeStamp[T] = this.copy(Some(Default.TimeStamp(onUpdate)))

  /**
   * This model is used to represent NULL-allowed TimeStamp data of SQL DataType.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TimeStampOpt[T <: Option[Instant | LocalDateTime]](default: Option[Default]) extends DateOptType[T]:

    override def queryString: String = s"TIMESTAMP $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): TimeStampOpt[T] = this.copy(Some(value.fold(Default.Null)(Default.Value(_))))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: TimeStampOpt[T] = this.copy(Some(Default.Null))

    /**
     * Methods for setting default values for dates.
     *
     * @param onUpdate
     * Value of whether to add settings on update
     */
    def DEFAULT_CURRENT_TIMESTAMP(onUpdate: Boolean = false): TimeStampOpt[T] = this.copy(Some(Default.TimeStamp(onUpdate)))

  /**
   * This model is used to represent SQL DataType Time data.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Time[T <: LocalTime](default: Option[Default]) extends DateType[T]:

    override def queryString: String = s"TIME $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Time[T] = this.copy(Some(Default.Value(value)))

  /**
   * This model is used to represent NULL-allowed Time data of SQL DataType.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class TimeOpt[T <: Option[LocalTime]](default: Option[Default]) extends DateOptType[T]:

    override def queryString: String = s"TIME $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): TimeOpt[T] = this.copy(Some(value.fold(Default.Null)(Default.Value(_))))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: TimeOpt[T] = this.copy(Some(Default.Null))

  /**
   * This model is used to represent SQL DataType Year data.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class Year[T <: Instant | LocalDate | JYear](default: Option[Default]) extends DateType[T]:

    override def queryString: String = s"YEAR $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): Year[T] = this.copy(Some(Default.Value(value)))

  /**
   * This model is used to represent NULL-allowed Year data of SQL DataType.
   *
   * @param default
   * SQL Default values
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class YearOpt[T <: Option[Instant | LocalDate | JYear]](default: Option[Default]) extends DateOptType[T]:

    override def queryString: String = s"YEAR $nullType" ++ default.fold("")(v => s" ${v.queryString}")

    /**
     * Method for setting Default value to DataType in SQL.
     *
     * @param value
     * Value set as the default value for DataType
     */
    def DEFAULT(value: T): YearOpt[T] = this.copy(Some(value.fold(Default.Null)(Default.Value(_))))

    /**
     * Method to set the Default value to NULL for SQL DataType.
     */
    def DEFAULT_NULL: YearOpt[T] = this.copy(Some(Default.Null))
