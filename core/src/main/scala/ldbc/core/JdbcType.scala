/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import cats.kernel.Order

/** Enum that defines constants to identify generic SQL types called JDBC types.
  */
enum JdbcType(val code: Int):
  case Array                 extends JdbcType(java.sql.Types.ARRAY)
  case BigInt                extends JdbcType(java.sql.Types.BIGINT)
  case Binary                extends JdbcType(java.sql.Types.BINARY)
  case Bit                   extends JdbcType(java.sql.Types.BIT)
  case Blob                  extends JdbcType(java.sql.Types.BLOB)
  case Boolean               extends JdbcType(java.sql.Types.BOOLEAN)
  case Char                  extends JdbcType(java.sql.Types.CHAR)
  case Clob                  extends JdbcType(java.sql.Types.CLOB)
  case DataLink              extends JdbcType(java.sql.Types.DATALINK)
  case Date                  extends JdbcType(java.sql.Types.DATE)
  case Decimal               extends JdbcType(java.sql.Types.DECIMAL)
  case Distinct              extends JdbcType(java.sql.Types.DISTINCT)
  case Double                extends JdbcType(java.sql.Types.DOUBLE)
  case Float                 extends JdbcType(java.sql.Types.FLOAT)
  case Integer               extends JdbcType(java.sql.Types.INTEGER)
  case JavaObject            extends JdbcType(java.sql.Types.JAVA_OBJECT)
  case LongNVarChar          extends JdbcType(java.sql.Types.LONGNVARCHAR)
  case LongVarBinary         extends JdbcType(java.sql.Types.LONGVARBINARY)
  case LongVarChar           extends JdbcType(java.sql.Types.LONGVARCHAR)
  case NChar                 extends JdbcType(java.sql.Types.NCHAR)
  case NClob                 extends JdbcType(java.sql.Types.NCLOB)
  case Null                  extends JdbcType(java.sql.Types.NULL)
  case Numeric               extends JdbcType(java.sql.Types.NUMERIC)
  case NVarChar              extends JdbcType(java.sql.Types.NVARCHAR)
  case Other                 extends JdbcType(java.sql.Types.OTHER)
  case Real                  extends JdbcType(java.sql.Types.REAL)
  case Ref                   extends JdbcType(java.sql.Types.REF)
  case RefCursor             extends JdbcType(java.sql.Types.REF_CURSOR)
  case RowId                 extends JdbcType(java.sql.Types.ROWID)
  case SmallInt              extends JdbcType(java.sql.Types.SMALLINT)
  case SqlXml                extends JdbcType(java.sql.Types.SQLXML)
  case Struct                extends JdbcType(java.sql.Types.STRUCT)
  case Time                  extends JdbcType(java.sql.Types.TIME)
  case TimeWithTimezone      extends JdbcType(java.sql.Types.TIME_WITH_TIMEZONE)
  case Timestamp             extends JdbcType(java.sql.Types.TIMESTAMP)
  case TimestampWithTimezone extends JdbcType(java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
  case TinyInt               extends JdbcType(java.sql.Types.TINYINT)
  case VarBinary             extends JdbcType(java.sql.Types.VARBINARY)
  case VarChar               extends JdbcType(java.sql.Types.VARCHAR)
  case Unknown(int: Int)     extends JdbcType(int)

object JdbcType:

  def fromCode(code: Int): JdbcType = code match
    case Array.code                 => Array
    case BigInt.code                => BigInt
    case Binary.code                => Binary
    case Bit.code                   => Bit
    case Blob.code                  => Blob
    case Boolean.code               => Boolean
    case Char.code                  => Char
    case Clob.code                  => Clob
    case DataLink.code              => DataLink
    case Date.code                  => Date
    case Decimal.code               => Decimal
    case Distinct.code              => Distinct
    case Double.code                => Double
    case Float.code                 => Float
    case Integer.code               => Integer
    case JavaObject.code            => JavaObject
    case LongNVarChar.code          => LongNVarChar
    case LongVarBinary.code         => LongVarBinary
    case LongVarChar.code           => LongVarChar
    case NChar.code                 => NChar
    case NClob.code                 => NClob
    case Null.code                  => Null
    case Numeric.code               => Numeric
    case NVarChar.code              => NVarChar
    case Other.code                 => Other
    case Real.code                  => Real
    case Ref.code                   => Ref
    case RefCursor.code             => RefCursor
    case RowId.code                 => RowId
    case SmallInt.code              => SmallInt
    case SqlXml.code                => SqlXml
    case Struct.code                => Struct
    case Time.code                  => Time
    case TimeWithTimezone.code      => TimeWithTimezone
    case Timestamp.code             => Timestamp
    case TimestampWithTimezone.code => TimestampWithTimezone
    case TinyInt.code               => TinyInt
    case VarBinary.code             => VarBinary
    case VarChar.code               => VarChar
    case n                          => Unknown(n)

  given Order[JdbcType] = Order.by(_.code)
