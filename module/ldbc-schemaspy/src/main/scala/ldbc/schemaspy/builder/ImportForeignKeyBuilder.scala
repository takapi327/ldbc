/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy.builder

import org.schemaspy.input.dbms.service.helper.ImportForeignKey

import ldbc.core.{ ForeignKey, Reference }

object ImportForeignKeyBuilder:

  def build(
    key:            ForeignKey,
    catalog:        String,
    schema:         String,
    constraintName: Option[String]
  ): Seq[ImportForeignKey] =
    val foreignKeyBuilder = new ImportForeignKey.Builder
    (for
      (keyColumn, keyColumnIndex) <- key.colName.zipWithIndex.toList
      (refColumn, refColumnIndex) <- key.reference.keyPart.zipWithIndex.toList
    yield
      if keyColumnIndex == refColumnIndex then
        Some(
          foreignKeyBuilder
            .withFkName(constraintName.getOrElse(key.indexName.getOrElse(key.label)))
            .withFkColumnName(keyColumn.label)
            .withPkTableCat(catalog)
            .withPkTableSchema(schema)
            .withPkTableName(key.reference.table.name)
            .withPkColumnName(refColumn.label)
            .withUpdateRule(key.reference.onUpdate.getOrElse(Reference.ReferenceOption.RESTRICT).code)
            .withDeleteRule(key.reference.onDelete.getOrElse(Reference.ReferenceOption.RESTRICT).code)
            .build()
        )
      else None).flatten
