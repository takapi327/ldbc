/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy.builder

import org.schemaspy.model.TableColumn
import org.schemaspy.model.Table as SchemaspyTable

import ldbc.core.{ Column, DataType }

object TableColumnBuilder:

  def build(table: SchemaspyTable, _column: Column[?], index: Int): TableColumn =
    val column = new TableColumn(table)
    column.setName(_column.label)
    column.setTypeName(_column.dataType.typeName)
    column.setType(_column.dataType.jdbcType.code)
    // column.setLength(255)
    // column.setDecimalDigits(???)
    column.setNullable(_column.dataType.isOptional)
    _column match
      case v: DataType.IntegerType[?]  => v.default foreach column.setDefaultValue
      case v: DataType.Decimal[?]      => v.default foreach column.setDefaultValue
      case v: DataType.DecimalOpt[?]   => v.default foreach column.setDefaultValue
      case v: DataType.CFloat[?]       => v.default foreach column.setDefaultValue
      case v: DataType.FloatOpt[?]     => v.default foreach column.setDefaultValue
      case v: DataType.CChar[?]        => v.default foreach column.setDefaultValue
      case v: DataType.CharOpt[?]      => v.default foreach column.setDefaultValue
      case v: DataType.Varchar[?]      => v.default foreach column.setDefaultValue
      case v: DataType.VarcharOpt[?]   => v.default foreach column.setDefaultValue
      case v: DataType.Date[?]         => v.default foreach column.setDefaultValue
      case v: DataType.DateOpt[?]      => v.default foreach column.setDefaultValue
      case v: DataType.DateTime[?]     => v.default foreach column.setDefaultValue
      case v: DataType.DateTimeOpt[?]  => v.default foreach column.setDefaultValue
      case v: DataType.TimeStamp[?]    => v.default foreach column.setDefaultValue
      case v: DataType.TimeStampOpt[?] => v.default foreach column.setDefaultValue
      case v: DataType.Time[?]         => v.default foreach column.setDefaultValue
      case v: DataType.TimeOpt[?]      => v.default foreach column.setDefaultValue
      case v: DataType.Year[?]         => v.default foreach column.setDefaultValue
      case v: DataType.YearOpt[?]      => v.default foreach column.setDefaultValue
      case _                           =>

    _column.comment foreach column.setComments
    column.setId(index)
    column
