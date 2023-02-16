/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql

import java.sql.Types.*

import cats.kernel.Order

/**
 * Enum that defines constants to identify generic SQL types called JDBC types.
 * 
 */
enum JdbcType(val code: Int):
  case Array extends JdbcType(ARRAY)
  case BigInt extends JdbcType(BIGINT)
  case Binary extends JdbcType(BINARY)
  case Bit extends JdbcType(BIT)
  case Blob extends JdbcType(BLOB)
  case Boolean extends JdbcType(BOOLEAN)
  case Char extends JdbcType(CHAR)
  case Clob extends JdbcType(CLOB)
  case DataLink extends JdbcType(DATALINK)
  case Date extends JdbcType(DATE)
  case Decimal extends JdbcType(DECIMAL)
  case Distinct extends JdbcType(DISTINCT)
  case Double extends JdbcType(DOUBLE)
  case Float extends JdbcType(FLOAT)
  case Integer extends JdbcType(INTEGER)
  case JavaObject extends JdbcType(JAVA_OBJECT)
  case LongNVarChar extends JdbcType(LONGNVARCHAR)
  case LongVarBinary extends JdbcType(LONGVARBINARY)
  case LongVarChar extends JdbcType(LONGVARCHAR)
  case NChar extends JdbcType(NCHAR)
  case NClob extends JdbcType(NCLOB)
  case Null extends JdbcType(NULL)
  case Numeric extends JdbcType(NUMERIC)
  case NVarChar extends JdbcType(NVARCHAR)
  case Other extends JdbcType(OTHER)
  case Real extends JdbcType(REAL)
  case Ref extends JdbcType(REF)
  case RefCursor extends JdbcType(REF_CURSOR)
  case RowId extends JdbcType(ROWID)
  case SmallInt extends JdbcType(SMALLINT)
  case SqlXml extends JdbcType(SQLXML)
  case Struct extends JdbcType(STRUCT)
  case Time extends JdbcType(TIME)
  case TimeWithTimezone extends JdbcType(TIME_WITH_TIMEZONE)
  case Timestamp extends JdbcType(TIMESTAMP)
  case TimestampWithTimezone extends JdbcType(TIMESTAMP_WITH_TIMEZONE)
  case TinyInt extends JdbcType(TINYINT)
  case VarBinary extends JdbcType(VARBINARY)
  case VarChar extends JdbcType(VARCHAR)
  case Unknown(int: Int) extends JdbcType(int)

object JdbcType:

  def fromCode(code: Int): JdbcType = code match
    case Array.code => Array
    case BigInt.code => BigInt
    case Binary.code => Binary
    case Bit.code => Bit
    case Blob.code => Blob
    case Boolean.code => Boolean
    case Char.code => Char
    case Clob.code => Clob
    case DataLink.code => DataLink
    case Date.code => Date
    case Decimal.code => Decimal
    case Distinct.code => Distinct
    case Double.code => Double
    case Float.code => Float
    case Integer.code => Integer
    case JavaObject.code => JavaObject
    case LongNVarChar.code => LongNVarChar
    case LongVarBinary.code => LongVarBinary
    case LongVarChar.code => LongVarChar
    case NChar.code => NChar
    case NClob.code => NClob
    case Null.code => Null
    case Numeric.code => Numeric
    case NVarChar.code => NVarChar
    case Other.code => Other
    case Real.code => Real
    case Ref.code => Ref
    case RefCursor.code => RefCursor
    case RowId.code => RowId
    case SmallInt.code => SmallInt
    case SqlXml.code => SqlXml
    case Struct.code => Struct
    case Time.code => Time
    case TimeWithTimezone.code => TimeWithTimezone
    case Timestamp.code => Timestamp
    case TimestampWithTimezone.code => TimestampWithTimezone
    case TinyInt.code => TinyInt
    case VarBinary.code => VarBinary
    case VarChar.code => VarChar
    case n => Unknown(n)

  given Order[JdbcType] = Order.by(_.code)
