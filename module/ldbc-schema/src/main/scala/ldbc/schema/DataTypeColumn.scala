/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.dsl.codec.*

import ldbc.statement.Column

import ldbc.schema.attribute.{ Attribute, AutoInc }

sealed trait DataTypeColumn[T] extends Column[T]:

  /**
   * Data type to be set for the column
   */
  def dataType: DataType[T]

  /**
   * List of attributes to be set for the column
   */
  def attributes: List[Attribute[T]]

  /**
   * Default value to be set for the column
   */
  def defaultValue: Option[Default]

  /**
   * Method for setting the default value to the current time.
   */
  def default(value: T): DataTypeColumn[T]

  /**
   * Method for setting the default value to NULL.
   */
  def defaultNull: DataTypeColumn[T]

  /**
   * Method for setting the attributes to the column.
   */
  def setAttributes(attributes: Attribute[T]*): DataTypeColumn[T]

  /**
   * Value indicating whether DataType is null-allowed or not.
   *
   * @return
   * true if NULL is allowed, false if NULL is not allowed
   */
  def isOptional: Boolean

  /**
   * Value to indicate whether NULL is acceptable as a query string in SQL
   */
  protected def nullType: String = if isOptional then "NULL" else "NOT NULL"

object DataTypeColumn:

  sealed trait NumericColumn[T] extends DataTypeColumn[T]:

    /**
     * Method for setting data type to unsigned.
     */
    def unsigned: NumericColumn[T]

    /**
     * Method for setting the default value to AUTO_INCREMENT.
     */
    def autoIncrement: NumericColumn[T]

  sealed trait StringColumn[T] extends DataTypeColumn[T]:

    /**
     * Method for setting Character Set to DataType in SQL.
     *
     * @param character
     * Character Set
     */
    def charset(character: Character): StringColumn[T]

    /**
     * Method for setting Collation to DataType in SQL.
     *
     * @param collate
     * Collation
     */
    def collate(collate: Collate[T]): StringColumn[T]

  sealed trait TemporalColumn[T] extends DataTypeColumn[T]:

    /**
     * Method for setting the default value to the current time.
     */
    def defaultCurrentTimestamp(onUpdate: Boolean = false): TemporalColumn[T]

    /**
     * Method for setting the default value to the current date.
     */
    def defaultCurrentDate: TemporalColumn[T]

  private[ldbc] case class NumericColumnImpl[T](
    name:         String,
    alias:        Option[String],
    decoder:      Decoder[T],
    encoder:      Encoder[T],
    dataType:     DataType[T],
    isUnsigned:   Boolean            = false,
    isOptional:   Boolean            = true,
    defaultValue: Option[Default]    = None,
    attributes:   List[Attribute[T]] = List.empty[Attribute[T]]
  ) extends NumericColumn[T]:

    override def as(name: String): Column[T] =
      this.copy(alias = Some(name))

    override def statement: String =
      (List(
        Some(name),
        Some(dataType.typeName),
        if isUnsigned then Some("UNSIGNED") else None,
        Some(nullType),
        defaultValue.map(_.queryString)
      ).flatten ++ attributes.map(_.queryString)).mkString(" ")
    override def updateStatement:             String = s"$name = ?"
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

    override def default(value: T): DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Value(value)))
    override def defaultNull:       DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Null))

    override def setAttributes(attributes: Attribute[T]*): DataTypeColumn[T] = this.copy(attributes = attributes.toList)
    override def autoIncrement: NumericColumn[T] = this.copy(attributes = AutoInc[T]() :: attributes)

    override def unsigned: NumericColumn[T] = this.copy(isUnsigned = true)

  private[ldbc] case class StringColumnImpl[T](
    name:         String,
    alias:        Option[String],
    decoder:      Decoder[T],
    encoder:      Encoder[T],
    dataType:     DataType[T],
    isOptional:   Boolean            = true,
    attributes:   List[Attribute[T]] = List.empty[Attribute[T]],
    defaultValue: Option[Default]    = None,
    character:    Option[Character]  = None,
    collate:      Option[Collate[T]] = None
  ) extends StringColumn[T]:

    override def as(name: String): Column[T] =
      this.copy(alias = Some(name))

    override def statement: String =
      (List(
        Some(name),
        Some(dataType.typeName),
        character.map(_.queryString),
        collate.map(_.queryString),
        Some(nullType),
        defaultValue.map(_.queryString)
      ).flatten ++ attributes.map(_.queryString)).mkString(" ")
    override def updateStatement:             String = s"$name = ?"
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

    override def default(value: T): DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Value(value)))
    override def defaultNull:       DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Null))

    override def setAttributes(attributes: Attribute[T]*): DataTypeColumn[T] = this.copy(attributes = attributes.toList)

    override def charset(character: Character):  StringColumn[T] = this.copy(character = Some(character))
    override def collate(collate:   Collate[T]): StringColumn[T] = this.copy(collate = Some(collate))

  private[ldbc] case class TemporalColumnImpl[T](
    name:         String,
    alias:        Option[String],
    decoder:      Decoder[T],
    encoder:      Encoder[T],
    dataType:     DataType[T],
    isOptional:   Boolean            = true,
    attributes:   List[Attribute[T]] = List.empty[Attribute[T]],
    defaultValue: Option[Default]    = None
  ) extends TemporalColumn[T]:

    override def as(name: String): Column[T] =
      this.copy(alias = Some(name))

    override def statement: String =
      (List(
        Some(name),
        Some(dataType.typeName),
        Some(nullType),
        defaultValue.map(_.queryString)
      ).flatten ++ attributes.map(_.queryString)).mkString(" ")
    override def updateStatement:             String = s"$name = ?"
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

    override def default(value: T): DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Value(value)))
    override def defaultNull:       DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Null))

    override def setAttributes(attributes: Attribute[T]*): DataTypeColumn[T] = this.copy(attributes = attributes.toList)

    override def defaultCurrentTimestamp(onUpdate: Boolean = false): TemporalColumn[T] =
      this.copy(defaultValue = Some(Default.TimeStamp(None, onUpdate)))
    override def defaultCurrentDate: TemporalColumn[T] = this.copy(defaultValue = Some(Default.Date()))

  private[ldbc] case class Impl[T](
    name:         String,
    alias:        Option[String],
    decoder:      Decoder[T],
    encoder:      Encoder[T],
    dataType:     DataType[T],
    isOptional:   Boolean            = true,
    attributes:   List[Attribute[T]] = List.empty[Attribute[T]],
    defaultValue: Option[Default]    = None
  ) extends DataTypeColumn[T]:

    override def as(name: String): Column[T] =
      this.copy(alias = Some(name))

    override def statement: String =
      (List(
        Some(name),
        Some(dataType.typeName),
        Some(nullType),
        defaultValue.map(_.queryString)
      ).flatten ++ attributes.map(_.queryString)).mkString(" ")
    override def updateStatement:             String = s"$name = ?"
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

    override def default(value: T): DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Value(value)))
    override def defaultNull:       DataTypeColumn[T] = this.copy(defaultValue = Some(Default.Null))

    override def setAttributes(attributes: Attribute[T]*): DataTypeColumn[T] = this.copy(attributes = attributes.toList)

  def apply[T](name: String, alias: Option[String], dataType: DataType[T], isOptional: Boolean)(using
    codec: Codec[T]
  ): DataTypeColumn[T] =
    Impl[T](
      s"`$name`",
      alias.map(v => s"$v.`$name`"),
      codec.asDecoder,
      codec.asEncoder,
      dataType,
      isOptional = isOptional
    )

  def numeric[T](name: String, alias: Option[String], dataType: DataType[T], isOptional: Boolean)(using
    codec: Codec[T]
  ): NumericColumn[T] =
    NumericColumnImpl[T](
      s"`$name`",
      alias.map(v => s"$v.`$name`"),
      codec.asDecoder,
      codec.asEncoder,
      dataType,
      isOptional = isOptional
    )

  def string[T](name: String, alias: Option[String], dataType: DataType[T], isOptional: Boolean)(using
    codec: Codec[T]
  ): StringColumn[T] =
    StringColumnImpl[T](
      s"`$name`",
      alias.map(v => s"$v.`$name`"),
      codec.asDecoder,
      codec.asEncoder,
      dataType,
      isOptional = isOptional
    )

  def temporal[T](name: String, alias: Option[String], dataType: DataType[T], isOptional: Boolean)(using
    codec: Codec[T]
  ): TemporalColumn[T] =
    TemporalColumnImpl[T](
      s"`$name`",
      alias.map(v => s"$v.`$name`"),
      codec.asDecoder,
      codec.asEncoder,
      dataType,
      isOptional = isOptional
    )
