/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy.builder

import org.schemaspy.model.TableColumn
import org.schemaspy.model.Table as SchemaspyTable

import ldbc.core.{ Column, DataType }

/** Object to generate a TableColumn model for SchemaSpy.
  */
object TableColumnBuilder:

  /** Methods for constructing a TableColumn from a Column.
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
    _column.dataType match
      case v: DataType.IntegerType[?]    => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.Decimal[?]        => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.CFloat[?]         => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.CChar[?]          => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.Varchar[?]        => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.Date[?]           => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.DateTime[?]       => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.TimeStamp[?]      => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.Time[?]           => v.default.map(_.value) foreach column.setDefaultValue
      case v: DataType.Year[?]           => v.default.map(_.value) foreach column.setDefaultValue
      case unknown =>
        throw new IllegalArgumentException(s"The $unknown in the ${ _column.label } column is not a DataType type.")

    _column.comment foreach column.setComments
    column.setId(index)
    column
