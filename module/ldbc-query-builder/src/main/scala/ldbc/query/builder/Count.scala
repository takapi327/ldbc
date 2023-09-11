/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import ldbc.core.*
import ldbc.sql.ResultSetReader

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

  override private[ldbc] def alias = None

  override def reader: ResultSetReader[F, Int] = ResultSetReader.given_ResultSetReader_F_Int

  override def toString: String = label

object Count:

  def all[F[_]] = Count[F]("*")
