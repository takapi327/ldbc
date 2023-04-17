/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import java.sql.DatabaseMetaData.{ importedKeyCascade, importedKeyRestrict, importedKeySetNull, importedKeyNoAction, importedKeySetDefault }

import cats.data.NonEmptyList

/** A model for setting reference options used for foreign key constraints, etc.
  *
  * @param table
  *   Referenced table model
  * @param keyPart
  *   List of columns for which the Index key is set
  * @param onDelete
  *   Reference action on delete
  * @param onUpdate
  *   Reference action on update
  */
case class Reference(
  table:    Table[?],
  keyPart:  NonEmptyList[Column[?]],
  onDelete: Option[Reference.ReferenceOption],
  onUpdate: Option[Reference.ReferenceOption]
):

  def label: String = "REFERENCES"

  def queryString: String =
    s"$label `${ table.name }` (${ keyPart.toList.map(column => s"`${ column.label }`").mkString(", ") })"
      + onDelete.fold("")(v => s" ON DELETE ${ v.label }")
      + onUpdate.fold("")(v => s" ON UPDATE ${ v.label }")

object Reference:

  enum ReferenceOption(val label: String, val code: Int):
    case RESTRICT    extends ReferenceOption("RESTRICT", importedKeyRestrict)
    case CASCADE     extends ReferenceOption("CASCADE", importedKeyCascade)
    case SET_NULL    extends ReferenceOption("SET NULL", importedKeySetNull)
    case NO_ACTION   extends ReferenceOption("NO ACTION", importedKeyNoAction)
    case SET_DEFAULT extends ReferenceOption("SET DEFAULT", importedKeySetDefault)

  def apply(table: Table[?])(columns: Column[?]*): Reference =
    require(
      NonEmptyList.fromList(columns.toList).nonEmpty,
      "For Reference settings, at least one COLUMN must always be specified."
    )
    Reference(table, NonEmptyList.fromListUnsafe(columns.toList), None, None)
