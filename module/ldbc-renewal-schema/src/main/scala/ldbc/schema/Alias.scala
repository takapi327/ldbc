/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.statement.Column
import ldbc.schema.attribute.*

private[ldbc] trait Alias:

  def AUTO_INCREMENT[T <: Byte | Short | Int | Long | BigInt | Option[Byte | Short | Int | Long | BigInt]]: AutoInc[T] =
    AutoInc[T]()

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
                  keyPart: Column[?]*
                ): UniqueKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(Some(indexName), None, keyPart.toList, None)

  def UNIQUE_KEY(
                  indexName: String,
                  indexType: Index.Type,
                  keyPart: Column[?]*
                ): UniqueKey & Index =
    require(
      keyPart.nonEmpty,
      "At least one column must always be specified."
    )
    UniqueKey(Some(indexName), Some(indexType), keyPart.toList, None)

  def UNIQUE_KEY(
                  indexName: String,
                  indexType: Index.Type,
                  indexOption: Index.IndexOption,
                  keyPart: Column[?]*
                ): UniqueKey & Index = UniqueKey(Some(indexName), Some(indexType), keyPart.toList, Some(indexOption))

  def UNIQUE_KEY(
                  indexName: Option[String],
                  indexType: Option[Index.Type],
                  indexOption: Option[Index.IndexOption],
                  keyPart: Column[?]*
                ): UniqueKey & Index = UniqueKey(indexName, indexType, keyPart.toList, indexOption)
