/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql

import cats.data.NonEmptyList

import ldbc.sql.free.Column

/**
 * Key to be set for the table
 */
private[ldbc] trait Key:

  /** Unique label for key */
  def label: String

  /** Define SQL query string for each Column
   *
   * @return
   * SQL query string
   */
  def queryString: String

/**
 * Trait for generating SQL keys that will become Indexes
 */
private[ldbc] trait Index extends Key:

  /** Value that is the type of Index */
  def indexType: Option[Index.Type]

  /** List of columns for which the Index key is set */
  def keyPart: NonEmptyList[Column[?]]

  /** Additional indexing options */
  def indexOption: Option[Index.IndexOption]

object Index:

  /** Value that is the type of Index */
  enum Type:
    case BTREE
    case HASH

  /**
   * Additional indexing options
   * 
   * @param keyBlockSize
   *   Value to specify the size in bytes to be used for the index key block
   * @param indexType
   *   Value that is the type of Index
   * @param parserName
   *   Value to associate the parser plugin with an index when special handling is required for full-text indexing and search operations
   * @param comment
   *   Index comment
   * @param engineAttribute
   *   Value to specify the index attribute for the primary storage engines.
   * @param secondaryEngineAttribute
   *   Value to specify the index attribute for the secondary storage engines.
   */
  case class IndexOption(
    keyBlockSize: Option[1 | 2 | 4 | 8 | 16],
    indexType: Option[Type],
    parserName: Option[String],
    comment: Option[String],
    engineAttribute: Option[String],
    secondaryEngineAttribute: Option[String],
  ):

    def queryString: String =
      keyBlockSize.fold("")(size => s" KEY_BLOCK_SIZE = $size")
        + indexType.fold("")(index => s" USING $index")
        + parserName.fold("")(name => s" WITH PARSER $name")
        + comment.fold("")(str => s" COMMENT '$str'")
        + engineAttribute.fold("")(engine => s" ENGINE_ATTRIBUTE = '$engine'")
        + secondaryEngineAttribute.fold("")(secondary => s" SECONDARY_ENGINE_ATTRIBUTE = '$secondary'")

/**
 * A model representing SQL Index key information.
 * 
 * @param indexName
 *   Unique name for key
 * @param indexType
 *   Value that is the type of Index
 * @param keyPart
 *   List of columns for which the Index key is set
 * @param indexOption
 *   Additional indexing options
 */
private[ldbc] case class IndexKey(
  indexName:   Option[String],
  indexType:   Option[Index.Type],
  keyPart:     NonEmptyList[Column[?]],
  indexOption: Option[Index.IndexOption]
) extends Index:

  override def label: String = "INDEX"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${keyPart.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + indexType.fold("")(index => s" USING $index")
      + indexOption.fold("")(option => s"${option.queryString}")

/**
 * A model representing SQL Fulltext Index key information.
 * 
 * @param indexName
 *   Unique name for key
 * @param keyPart 
 *   List of columns for which the Index key is set
 * @param indexOption
 *   Additional indexing options
 */
private[ldbc] case class Fulltext(
  indexName:   Option[String],
  keyPart:     NonEmptyList[Column[?]],
  indexOption: Option[Index.IndexOption]
) extends Key:

  override def label: String = "FULLTEXT"

  override def queryString: String = 
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${keyPart.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + indexOption.fold("")(option => s"${option.queryString}")

/**
 * A model representing SQL Primary key information.
 *
 * @param indexType
 *   Value that is the type of Index
 * @param keyPart
 *   List of columns for which the Index key is set
 * @param indexOption
 *   Additional indexing options
 */
private[ldbc] case class PrimaryKey(
  indexType:   Option[Index.Type],
  keyPart:     NonEmptyList[Column[?]],
  indexOption: Option[Index.IndexOption]
) extends Index:

  override def label: String = "PRIMARY KEY"

  override def queryString: String =
    label
      + s" (${keyPart.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + indexType.fold("")(index => s" USING $index")
      + indexOption.fold("")(option => s"${option.queryString}")

/**
 * A model representing SQL Unique key information.
 *
 * @param indexName
 *   Unique name for key
 * @param indexType
 *   Value that is the type of Index
 * @param keyPart
 *   List of columns for which the Index key is set
 * @param indexOption
 *   Additional indexing options
 */
private[ldbc] case class UniqueKey(
  indexName:   Option[String],
  indexType:   Option[Index.Type],
  keyPart:     NonEmptyList[Column[?]],
  indexOption: Option[Index.IndexOption]
) extends Index:

  override def label: String = "UNIQUE KEY"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
    + s" (${keyPart.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + indexType.fold("")(index => s" USING $index")
      + indexOption.fold("")(option => s"${option.queryString}")

/**
 * A model representing SQL Foreign key information.
 * 
 * @param indexName
 *   Unique name for key
 * @param colName
 *   List of columns for which the Index key is set
 * @param reference
 *   A model for setting reference options used for foreign key constraints, etc.
 */
private[ldbc] case class ForeignKey(
  indexName: Option[String],
  colName:   NonEmptyList[Column[?]],
  reference: Reference
) extends Key:

  override def label: String = "FOREIGN KEY"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${colName.map(column => s"`${column.label}`").toList.mkString(", ")})"
      + s" ${reference.queryString}"

/**
 * A model for setting constraints on keys.
 * 
 * @param symbol
 *   Unique name of the constraint
 * @param key
 *   Types of keys for which constraints can be set
 */
private[ldbc] case class Constraint(
  symbol: String,
  key: PrimaryKey | UniqueKey | ForeignKey
) extends Key:

  def label: String = "CONSTRAINT"

  private val keyQueryString: String = key match
    case p: PrimaryKey => p.queryString
    case u: UniqueKey  => u.queryString
    case f: ForeignKey => f.queryString

  def queryString: String = s"$label `$symbol` $keyQueryString"
