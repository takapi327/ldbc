/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.query.builder.interpreter.Tuples

import ldbc.schema.attribute.*

private[ldbc] trait Alias:

  type Table[P <: Product] = ldbc.query.builder.Table[P]
  type Column[T]          = ldbc.query.builder.Column[T]

  def column[T](
                 label: String,
                 dataType: DataType[T]
               ): Column[T] = Column[T](label, dataType)

  def column[T](
                 label: String,
                 dataType: DataType[T],
                 attributes: Attribute[T]*
               ): Column[T] = Column[T](label, dataType, attributes *)

  def COMMENT[T](message: String): Comment[T] = Comment[T](message)

  def VISIBLE[T]:   Visible[T]   = Visible[T]()
  def INVISIBLE[T]: InVisible[T] = InVisible[T]()

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
    keyPart:   Column[?]*
  ): PrimaryKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(Some(indexType), keyPart.toList, None)

  def PRIMARY_KEY(
    keyPart:     List[Column[?]],
    indexOption: Index.IndexOption
  ): PrimaryKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    PrimaryKey(None, keyPart, Some(indexOption))

  def PRIMARY_KEY(
    indexType:   Index.Type,
    indexOption: Index.IndexOption,
    keyPart:     Column[?]*
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
    keyPart:   Column[?]*
  ): UniqueKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(Some(indexName), None, keyPart.toList, None)

  def UNIQUE_KEY(
    indexName: String,
    indexType: Index.Type,
    keyPart:   Column[?]*
  ): UniqueKey & Index =
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
  ): UniqueKey & Index = UniqueKey(Some(indexName), Some(indexType), keyPart.toList, Some(indexOption))

  def UNIQUE_KEY(
    indexName:   Option[String],
    indexType:   Option[Index.Type],
    indexOption: Option[Index.IndexOption],
    keyPart:     Column[?]*
  ): UniqueKey & Index = UniqueKey(indexName, indexType, keyPart.toList, indexOption)

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
    reference: Reference[Column[T] *: EmptyTuple]
  ): ForeignKey[Column[T] *: EmptyTuple] = ForeignKey[Column[T] *: EmptyTuple](None, column *: EmptyTuple, reference)

  def FOREIGN_KEY[T](
    name:      String,
    column:    Column[T],
    reference: Reference[Column[T] *: EmptyTuple]
  ): ForeignKey[Column[T] *: EmptyTuple] =
    ForeignKey[Column[T] *: EmptyTuple](Some(name), column *: EmptyTuple, reference)

  def FOREIGN_KEY[T <: Tuple](
    columns:   T,
    reference: Reference[T]
  )(using Tuples.IsColumn[T] =:= true): ForeignKey[T] =
    ForeignKey[T](None, columns, reference)

  def FOREIGN_KEY[T <: Tuple](
    name:      String,
    columns:   T,
    reference: Reference[T]
  )(using Tuples.IsColumn[T] =:= true): ForeignKey[T] =
    ForeignKey[T](Some(name), columns, reference)

  def FOREIGN_KEY[T <: Tuple](
    name:      Option[String],
    columns:   T,
    reference: Reference[T]
  )(using Tuples.IsColumn[T] =:= true): ForeignKey[T] =
    ForeignKey[T](name, columns, reference)

  def REFERENCE[T](
    table:  Table[?],
    column: Column[T]
  ): Reference[Column[T] *: EmptyTuple] = Reference[Column[T] *: EmptyTuple](table, column *: EmptyTuple, None, None)

  def REFERENCE[T <: Tuple](
    table:   Table[?],
    columns: T
  )(using Tuples.IsColumn[T] =:= true): Reference[T] =
    Reference[T](table, columns, None, None)
