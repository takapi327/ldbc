/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schemaspy.builder

import org.schemaspy.model.Table as SchemaspyTable
import org.schemaspy.model.TableColumn

import ldbc.core.attribute.Comment
import ldbc.core.Column

/**
 * Object to generate a TableColumn model for SchemaSpy.
 */
object TableColumnBuilder:

  /**
   * Methods for constructing a TableColumn from a Column.
   *
   * @param table
   *   A Table is one of the basic building blocks of SchemaSpy that knows everything about the database table's
   *   metadata.
   * @param _column
   *   Trait for representing SQL Column
   * @param index
   *   The index number of the column set in the table.
   */
  def build(table: SchemaspyTable, _column: Column[?], index: Int): TableColumn =
    val column = new TableColumn(table)
    column.setName(_column.label)
    column.setTypeName(_column.dataType.typeName)
    column.setType(_column.dataType.jdbcType.code)
    column.setNullable(_column.dataType.isOptional)
    _column.dataType.default.map(_.value) foreach column.setDefaultValue

    _column.attributes.foreach {
      case comment: Comment[?] => column.setComments(comment.message)
      case _                   =>
    }
    column.setId(index)
    column
