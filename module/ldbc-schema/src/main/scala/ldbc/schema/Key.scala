/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.statement.Column

/**
 * Key to be set for the table
 */
private[ldbc] trait Key:

  /** Unique label for key */
  def label: String

  /**
   * Define SQL query string for each Column
   *
   * @return
   *   SQL query string
   */
  def queryString: String

/**
 * Trait for generating SQL keys that will become Indexes
 */
private[ldbc] trait Index[T] extends Key:

  /** Value that is the type of Index */
  def indexType: Option[Index.Type]

  /** List of columns for which the Index key is set */
  def columns: Column[T]

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
   *   Value to associate the parser plugin with an index when special handling is required for full-text indexing and
   *   search operations
   * @param comment
   *   Index comment
   * @param engineAttribute
   *   Value to specify the index attribute for the primary storage engines.
   * @param secondaryEngineAttribute
   *   Value to specify the index attribute for the secondary storage engines.
   */
  case class IndexOption(
    keyBlockSize:             Option[1 | 2 | 4 | 8 | 16],
    indexType:                Option[Type],
    parserName:               Option[String],
    comment:                  Option[String],
    engineAttribute:          Option[String],
    secondaryEngineAttribute: Option[String]
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
 * @param columns
 *   List of columns for which the Index key is set
 * @param indexOption
 *   Additional indexing options
 */
private[ldbc] case class IndexKey[T](
  indexName:   Option[String],
  indexType:   Option[Index.Type],
  columns:     Column[T],
  indexOption: Option[Index.IndexOption]
) extends Index[T]:

  override def label: String = "INDEX"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${ columns.name })"
      + indexType.fold("")(index => s" USING $index")
      + indexOption.fold("")(option => s"${ option.queryString }")

/**
 * A model representing SQL Fulltext Index key information.
 *
 * @param indexName
 *   Unique name for key
 * @param columns
 *   List of columns for which the Index key is set
 * @param indexOption
 *   Additional indexing options
 */
private[ldbc] case class Fulltext[T](
  indexName:   Option[String],
  columns:     Column[T],
  indexOption: Option[Index.IndexOption]
) extends Key:

  override def label: String = "FULLTEXT"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${ columns.name })"
      + indexOption.fold("")(option => s"${ option.queryString }")

/** Trait for representing SQL primary key information. */
private[ldbc] trait PrimaryKey:

  val label: String = "PRIMARY KEY"

  def queryString: String

object PrimaryKey:
  def apply[T](
    _indexType:   Option[Index.Type],
    _columns:     Column[T],
    _indexOption: Option[Index.IndexOption]
  ): PrimaryKey & Index[T] = new PrimaryKey with Index[T]:

    override def indexType: Option[Index.Type] = _indexType

    override def columns: Column[T] = _columns

    override def indexOption: Option[Index.IndexOption] = _indexOption

    override def queryString: String =
      label
        + s" (${ columns.name })"
        + indexType.fold("")(index => s" USING $index")
        + indexOption.fold("")(option => s"${ option.queryString }")

/** Trait for representing SQL unique key information. */
private[ldbc] trait UniqueKey:

  val label: String = "UNIQUE KEY"

  /** Unique name for key */
  def indexName: Option[String]

  def queryString: String

object UniqueKey:

  def apply[T](
    _indexName:   Option[String],
    _indexType:   Option[Index.Type],
    _columns:     Column[T],
    _indexOption: Option[Index.IndexOption]
  ): UniqueKey & Index[T] = new UniqueKey with Index[T]:

    override def indexName: Option[String] = _indexName

    override def indexType: Option[Index.Type] = _indexType

    override def columns: Column[T] = _columns

    override def indexOption: Option[Index.IndexOption] = _indexOption

    override def queryString: String =
      label
        + indexName.fold("")(str => s" `$str`")
        + s" (${ columns.name })"
        + indexType.fold("")(index => s" USING $index")
        + indexOption.fold("")(option => s"${ option.queryString }")

/**
 * A model representing SQL Foreign key information.
 *
 * @param indexName
 *   Unique name for key
 * @param columns
 *   List of columns for which the Index key is set
 * @param reference
 *   A model for setting reference options used for foreign key constraints, etc.
 */
private[ldbc] case class ForeignKey[T](
  indexName: Option[String],
  columns:   Column[T],
  reference: Reference[T]
) extends Key:

  override val label: String = "FOREIGN KEY"

  override def queryString: String =
    label
      + indexName.fold("")(str => s" `$str`")
      + s" (${ columns.name })"
      + s" ${ reference.queryString }"

/**
 * A model for setting constraints on keys.
 *
 * @param symbol
 *   Unique name of the constraint
 * @param key
 *   Types of keys for which constraints can be set
 */
private[ldbc] case class Constraint(
  symbol: Option[String],
  key:    PrimaryKey | UniqueKey | ForeignKey[?]
) extends Key:

  def label: String = "CONSTRAINT"

  private val keyQueryString: String = key match
    case p: PrimaryKey    => p.queryString
    case u: UniqueKey     => u.queryString
    case f: ForeignKey[?] => f.queryString

  def queryString: String =
    symbol.fold(s"$label $keyQueryString")(str => s"$label `$str` $keyQueryString")
