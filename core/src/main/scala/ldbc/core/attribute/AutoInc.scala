/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core.attribute

/** Model for specifying an additional attribute AUTO_INCREMENT for DataType.
  */
private[ldbc] case class AutoInc[T <: Byte | Short | Int | Long | BigInt | Option[Byte | Short | Int | Long | BigInt]]()
  extends Attribute[T]:

  override def queryString: String = "AUTO_INCREMENT"

  override def toString: String = queryString
