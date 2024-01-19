/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.schemaspy.builder

import org.schemaspy.input.dbms.service.helper.ImportForeignKey

import ldbc.core.{ Column, ForeignKey, Reference }

/** Object to generate the ImportForeignKey model for SchemaSpy.
  */
object ImportForeignKeyBuilder:

  /** Methods for constructing an ImportForeignKey array based on a ForeignKey.
    *
    * @param key
    *   A model representing SQL Foreign key information.
    * @param catalog
    *   Database Catalog
    * @param schema
    *   Database Schema
    * @param constraintName
    *   Unique name of the constraint
    */
  def build(
    key:            ForeignKey[?],
    catalog:        String,
    schema:         String,
    constraintName: Option[String]
  ): Seq[ImportForeignKey] =
    val foreignKeyBuilder = new ImportForeignKey.Builder
    (for
      (keyColumn, keyColumnIndex) <- key.columns.toList.asInstanceOf[List[Column[?]]].zipWithIndex
      (refColumn, refColumnIndex) <- key.reference.keyPart.toList.asInstanceOf[List[Column[?]]].zipWithIndex
    yield
      if keyColumnIndex == refColumnIndex then
        Some(
          foreignKeyBuilder
            .withFkName(constraintName.getOrElse(key.indexName.getOrElse(key.label)))
            .withFkColumnName(keyColumn.label)
            .withPkTableCat(catalog)
            .withPkTableSchema(schema)
            .withPkTableName(key.reference.table._name)
            .withPkColumnName(refColumn.label)
            .withUpdateRule(key.reference.onUpdate.getOrElse(Reference.ReferenceOption.RESTRICT).code)
            .withDeleteRule(key.reference.onDelete.getOrElse(Reference.ReferenceOption.RESTRICT).code)
            .build()
        )
      else None).flatten
