/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import java.sql.DatabaseMetaData.*

/**
 * A model for setting reference options used for foreign key constraints, etc.
 *
 * @param table
 *   Referenced table model
 * @param columns
 *   List of columns for which the Index key is set
 * @param onDelete
 *   Reference action on delete
 * @param onUpdate
 *   Reference action on update
 */
case class Reference[T](
  table:    Table[?],
  columns:  Column[T],
  onDelete: Option[Reference.ReferenceOption],
  onUpdate: Option[Reference.ReferenceOption]
):

  private val label: String = "REFERENCES"

  def queryString: String =
    s"$label `${ table.$name }` (${ columns.name })"
      + onDelete.fold("")(v => s" ON DELETE ${ v.label }")
      + onUpdate.fold("")(v => s" ON UPDATE ${ v.label }")

  def onDelete(option: Reference.ReferenceOption): Reference[T] =
    this.copy(onDelete = Some(option))

  def onUpdate(option: Reference.ReferenceOption): Reference[T] =
    this.copy(onUpdate = Some(option))

object Reference:

  enum ReferenceOption(val label: String, val code: Int):
    case RESTRICT    extends ReferenceOption("RESTRICT", importedKeyRestrict)
    case CASCADE     extends ReferenceOption("CASCADE", importedKeyCascade)
    case SET_NULL    extends ReferenceOption("SET NULL", importedKeySetNull)
    case NO_ACTION   extends ReferenceOption("NO ACTION", importedKeyNoAction)
    case SET_DEFAULT extends ReferenceOption("SET DEFAULT", importedKeySetDefault)
