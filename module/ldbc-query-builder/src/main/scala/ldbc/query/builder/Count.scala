/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.query.builder

import ldbc.core.*

/** Model used to obtain the number of specific columns.
  *
  * @param _label
  *   Column name
  * @tparam F
  *   The effect type
  */
case class Count[F[_]](_label: String) extends ColumnQuery[F, Int]:

  override def label: String = s"COUNT($_label)"

  override def dataType: DataType[Int] = DataType.Integer(None, false)

  override def attributes: Seq[attribute.Attribute[Int]] = Seq.empty

  override def alias: Option[String] = None

  override def toString: String = label

object Count:

  def all[F[_]] = Count[F]("*")
