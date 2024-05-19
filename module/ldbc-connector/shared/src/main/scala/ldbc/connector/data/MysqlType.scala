/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import ldbc.sql.{SQLType, Types}

enum MysqlType(
  val name:         String,
  val jdbcType:     Int,
  val allowedFlags: Int,
  val isDec:        Boolean,
  val precision:    Long,
  val createParams: String
) extends SQLType:

  /**
   * DECIMAL[(M[,D])] [UNSIGNED] [ZEROFILL]
   * A packed "exact" fixed-point number. M is the total number of digits (the precision) and D is the number of digits
   * after the decimal point (the scale). The decimal point and (for negative numbers) the "-" sign are not counted in M.
   * If D is 0, values have no decimal point or fractional part. The maximum number of digits (M) for DECIMAL is 65.
   * The maximum number of supported decimals (D) is 30. If D is omitted, the default is 0. If M is omitted, the default is 10.
   *
   * Protocol: FIELD_TYPE_DECIMAL = 0
   * Protocol: FIELD_TYPE_NEWDECIMAL = 246
   *
   * These types are synonyms for DECIMAL:
   * DEC[(M[,D])] [UNSIGNED] [ZEROFILL],
   * NUMERIC[(M[,D])] [UNSIGNED] [ZEROFILL],
   * FIXED[(M[,D])] [UNSIGNED] [ZEROFILL]
   */
  case DECIMAL
    extends MysqlType(
      "DECIMAL",
      Types.DECIMAL,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      65L,
      "[(M[,D])] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * DECIMAL[(M[,D])] UNSIGNED [ZEROFILL]
   */
  case DECIMAL_UNSIGNED
    extends MysqlType(
      "DECIMAL UNSIGNED",
      Types.DECIMAL,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      65L,
      "[(M[,D])] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * TINYINT[(M)] [UNSIGNED] [ZEROFILL]
   * A very small integer. The signed range is -128 to 127. The unsigned range is 0 to 255.
   *
   * Protocol: FIELD_TYPE_TINY = 1
   */
  case TINYINT
    extends MysqlType(
      "TINYINT",
      Types.TINYINT,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      3L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * TINYINT[(M)] UNSIGNED [ZEROFILL]
   */
  case TINYINT_UNSIGNED
    extends MysqlType(
      "TINYINT UNSIGNED",
      Types.TINYINT,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      3L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * BOOL, BOOLEAN
   * These types are synonyms for TINYINT(1). A value of zero is considered false. Nonzero values are considered true
   *
   * BOOLEAN is converted to TINYINT(1) during DDL execution i.e. it has the same precision=3. Thus we have to
   * look at full data type name and convert TINYINT to BOOLEAN (or BIT) if it has "(1)" length specification.
   *
   * Protocol: FIELD_TYPE_TINY = 1
   */
  case BOOLEAN extends MysqlType("BOOLEAN", Types.BOOLEAN, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 3L, "")

  /**
   * SMALLINT[(M)] [UNSIGNED] [ZEROFILL]
   * A small integer. The signed range is -32768 to 32767. The unsigned range is 0 to 65535.
   *
   * Protocol: FIELD_TYPE_SHORT = 2
   */
  case SMALLINT
    extends MysqlType(
      "SMALLINT",
      Types.SMALLINT,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      5L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * SMALLINT[(M)] UNSIGNED [ZEROFILL]
   *
   * @see MysqlType#SMALLINT
   */
  case SMALLINT_UNSIGNED
    extends MysqlType(
      "SMALLINT UNSIGNED",
      Types.SMALLINT,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      5L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * INT[(M)] [UNSIGNED] [ZEROFILL]
   * A normal-size integer. The signed range is -2147483648 to 2147483647. The unsigned range is 0 to 4294967295.
   *
   * Protocol: FIELD_TYPE_LONG = 3
   *
   * INTEGER[(M)] [UNSIGNED] [ZEROFILL] is a synonym for INT.
   */
  case INT
    extends MysqlType(
      "INT",
      Types.INTEGER,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      10L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * INT[(M)] UNSIGNED [ZEROFILL]
   */
  case INT_UNSIGNED
    extends MysqlType(
      "INT UNSIGNED",
      Types.INTEGER,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      10L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * FLOAT[(M,D)] [UNSIGNED] [ZEROFILL]
   * A small (single-precision) floating-point number. Permissible values are -3.402823466E+38 to -1.175494351E-38, 0,
   * and 1.175494351E-38 to 3.402823466E+38. These are the theoretical limits, based on the IEEE standard. The actual
   * range might be slightly smaller depending on your hardware or operating system.
   *
   * M is the total number of digits and D is the number of digits following the decimal point. If M and D are omitted,
   * values are stored to the limits permitted by the hardware. A single-precision floating-point number is accurate to
   * approximately 7 decimal places.
   *
   * Protocol: FIELD_TYPE_FLOAT = 4
   *
   * Additionally:
   * FLOAT(p) [UNSIGNED] [ZEROFILL]
   * A floating-point number. p represents the precision in bits, but MySQL uses this value only to determine whether
   * to use FLOAT or DOUBLE for the resulting data type. If p is from 0 to 24, the data type becomes FLOAT with no M or D values.
   * If p is from 25 to 53, the data type becomes DOUBLE with no M or D values. The range of the resulting column is the same as
   * for the single-precision FLOAT or double-precision DOUBLE data types.
   */
  case FLOAT
    extends MysqlType(
      "FLOAT",
      Types.REAL,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      12L,
      "[(M,D)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * FLOAT[(M,D)] UNSIGNED [ZEROFILL]
   */
  case FLOAT_UNSIGNED
    extends MysqlType(
      "FLOAT UNSIGNED",
      Types.REAL,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      12L,
      "[(M,D)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * DOUBLE[(M,D)] [UNSIGNED] [ZEROFILL]
   * A normal-size (double-precision) floating-point number. Permissible values are -1.7976931348623157E+308 to
   * -2.2250738585072014E-308, 0, and 2.2250738585072014E-308 to 1.7976931348623157E+308. These are the theoretical limits,
   * based on the IEEE standard. The actual range might be slightly smaller depending on your hardware or operating system.
   *
   * M is the total number of digits and D is the number of digits following the decimal point. If M and D are omitted,
   * values are stored to the limits permitted by the hardware. A double-precision floating-point number is accurate to
   * approximately 15 decimal places.
   *
   * Protocol: FIELD_TYPE_DOUBLE = 5
   *
   * These types are synonyms for DOUBLE:
   * DOUBLE PRECISION[(M,D)] [UNSIGNED] [ZEROFILL],
   * REAL[(M,D)] [UNSIGNED] [ZEROFILL]. Exception: If the REAL_AS_FLOAT SQL mode is enabled, REAL is a synonym for FLOAT rather than DOUBLE.
   */
  case DOUBLE
    extends MysqlType(
      "DOUBLE",
      Types.DOUBLE,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      22L,
      "[(M,D)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * DOUBLE[(M,D)] UNSIGNED [ZEROFILL]
   */
  case DOUBLE_UNSIGNED
    extends MysqlType(
      "DOUBLE UNSIGNED",
      Types.DOUBLE,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      22L,
      "[(M,D)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * FIELD_TYPE_NULL = 6
   */
  case NULL extends MysqlType("NULL", Types.NULL, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 0L, "")

  /**
   * TIMESTAMP[(fsp)]
   * A timestamp. The range is '1970-01-01 00:00:01.000000' UTC to '2038-01-19 03:14:07.999999' UTC.
   * TIMESTAMP values are stored as the number of seconds since the epoch ('1970-01-01 00:00:00' UTC).
   * A TIMESTAMP cannot represent the value '1970-01-01 00:00:00' because that is equivalent to 0 seconds
   * from the epoch and the value 0 is reserved for representing '0000-00-00 00:00:00', the "zero" TIMESTAMP value.
   * An optional fsp value in the range from 0 to 6 may be given to specify fractional seconds precision. A value
   * of 0 signifies that there is no fractional part. If omitted, the default precision is 0.
   *
   * Protocol: FIELD_TYPE_TIMESTAMP = 7
   */
  case TIMESTAMP extends MysqlType("TIMESTAMP", Types.TIMESTAMP, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 26L, "[(fsp)]")

  /**
   * BIGINT[(M)] [UNSIGNED] [ZEROFILL]
   * A large integer. The signed range is -9223372036854775808 to 9223372036854775807. The unsigned range is 0 to 18446744073709551615.
   *
   * Protocol: FIELD_TYPE_LONGLONG = 8
   *
   * SERIAL is an alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE.
   */
  case BIGINT
    extends MysqlType(
      "BIGINT",
      Types.BIGINT,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      19L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * BIGINT[(M)] UNSIGNED [ZEROFILL]
   */
  case BIGINT_UNSIGNED
    extends MysqlType(
      "BIGINT UNSIGNED",
      Types.BIGINT,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      20L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * MEDIUMINT[(M)] [UNSIGNED] [ZEROFILL]
   * A medium-sized integer. The signed range is -8388608 to 8388607. The unsigned range is 0 to 16777215.
   *
   * Protocol: FIELD_TYPE_INT24 = 9
   */
  case MEDIUMINT
    extends MysqlType(
      "MEDIUMINT",
      Types.INTEGER,
      MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      7L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * MEDIUMINT[(M)] UNSIGNED [ZEROFILL]
   */
  case MEDIUMINT_UNSIGNED
    extends MysqlType(
      "MEDIUMINT UNSIGNED",
      Types.INTEGER,
      MysqlTypeVariables.FIELD_FLAG_UNSIGNED | MysqlTypeVariables.FIELD_FLAG_ZEROFILL,
      MysqlTypeVariables.IS_DECIMAL,
      8L,
      "[(M)] [UNSIGNED] [ZEROFILL]"
    )

  /**
   * DATE
   * A date. The supported range is '1000-01-01' to '9999-12-31'. MySQL displays DATE values in 'YYYY-MM-DD' format,
   * but permits assignment of values to DATE columns using either strings or numbers.
   *
   * Protocol: FIELD_TYPE_DATE = 10
   */
  case DATE extends MysqlType("DATE", Types.DATE, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 10L, "")

  /**
   * TIME[(fsp)]
   * A time. The range is '-838:59:59.000000' to '838:59:59.000000'. MySQL displays TIME values in
   * 'HH:MM:SS[.fraction]' format, but permits assignment of values to TIME columns using either strings or numbers.
   * An optional fsp value in the range from 0 to 6 may be given to specify fractional seconds precision. A value
   * of 0 signifies that there is no fractional part. If omitted, the default precision is 0.
   *
   * Protocol: FIELD_TYPE_TIME = 11
   */
  case TIME extends MysqlType("TIME", Types.TIME, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 16L, "[(fsp)]")

  /**
   * DATETIME[(fsp)]
   * A date and time combination. The supported range is '1000-01-01 00:00:00.000000' to '9999-12-31 23:59:59.999999'.
   * MySQL displays DATETIME values in 'YYYY-MM-DD HH:MM:SS[.fraction]' format, but permits assignment of values to
   * DATETIME columns using either strings or numbers.
   * An optional fsp value in the range from 0 to 6 may be given to specify fractional seconds precision. A value
   * of 0 signifies that there is no fractional part. If omitted, the default precision is 0.
   *
   * Protocol: FIELD_TYPE_DATETIME = 12
   */
  case DATETIME extends MysqlType("DATETIME", Types.TIMESTAMP, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 26L, "[(fsp)]")

  /**
   * YEAR[(4)]
   * A year in four-digit format. MySQL displays YEAR values in YYYY format, but permits assignment of
   * values to YEAR columns using either strings or numbers. Values display as 1901 to 2155, and 0000.
   * Protocol: FIELD_TYPE_YEAR = 13
   */
  case YEAR extends MysqlType("YEAR", Types.DATE, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 4L, "[(4)]")

  /**
   * [NATIONAL] VARCHAR(M) [CHARACTER SET charset_name] [COLLATE collation_name]
   * A variable-length string. M represents the maximum column length in characters. The range of M is 0 to 65,535.
   * The effective maximum length of a VARCHAR is subject to the maximum row size (65,535 bytes, which is shared among
   * all columns) and the character set used. For example, utf8 characters can require up to three bytes per character,
   * so a VARCHAR column that uses the utf8 character set can be declared to be a maximum of 21,844 characters.
   *
   * MySQL stores VARCHAR values as a 1-byte or 2-byte length prefix plus data. The length prefix indicates the number
   * of bytes in the value. A VARCHAR column uses one length byte if values require no more than 255 bytes, two length
   * bytes if values may require more than 255 bytes.
   *
   * Note
   * MySQL 5.7 follows the standard SQL specification, and does not remove trailing spaces from VARCHAR values.
   *
   * VARCHAR is shorthand for CHARACTER VARYING. NATIONAL VARCHAR is the standard SQL way to define that a VARCHAR
   * column should use some predefined character set. MySQL 4.1 and up uses utf8 as this predefined character set.
   * NVARCHAR is shorthand for NATIONAL VARCHAR.
   *
   * Protocol: FIELD_TYPE_VARCHAR = 15
   * Protocol: FIELD_TYPE_VAR_STRING = 253
   */
  case VARCHAR
    extends MysqlType(
      "VARCHAR",
      Types.VARCHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      65535L,
      "(M) [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * VARBINARY(M)
   * The VARBINARY type is similar to the VARCHAR type, but stores binary byte strings rather than nonbinary
   * character strings. M represents the maximum column length in bytes.
   *
   * Protocol: FIELD_TYPE_VARCHAR = 15
   * Protocol: FIELD_TYPE_VAR_STRING = 253
   */
  case VARBINARY extends MysqlType("VARBINARY", Types.VARBINARY, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 65535L, "(M)")

  /**
   * BIT[(M)]
   * A bit-field type. M indicates the number of bits per value, from 1 to 64. The default is 1 if M is omitted.
   * Protocol: FIELD_TYPE_BIT = 16
   */
  case BIT extends MysqlType("BIT", Types.BIT, 0, MysqlTypeVariables.IS_DECIMAL, 1L, "[(M)]")

  /**
   * The size of JSON documents stored in JSON columns is limited to the value of the max_allowed_packet system variable (max value 1073741824).
   * (While the server manipulates a JSON value internally in memory, it can be larger; the limit applies when the server stores it.)
   *
   * Protocol: FIELD_TYPE_BIT = 245
   */
  case JSON extends MysqlType("JSON", Types.LONGVARCHAR, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 1073741824L, "")

  /**
   * ENUM('value1','value2',...) [CHARACTER SET charset_name] [COLLATE collation_name]
   * An enumeration. A string object that can have only one value, chosen from the list of values 'value1',
   * 'value2', ..., NULL or the special '' error value. ENUM values are represented internally as integers.
   * An ENUM column can have a maximum of 65,535 distinct elements. (The practical limit is less than 3000.)
   * A table can have no more than 255 unique element list definitions among its ENUM and SET columns considered as a group
   *
   * Protocol: FIELD_TYPE_ENUM = 247
   */
  case ENUM
    extends MysqlType(
      "ENUM",
      Types.CHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      65535L,
      "('value1','value2',...) [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * SET('value1','value2',...) [CHARACTER SET charset_name] [COLLATE collation_name]
   * A set. A string object that can have zero or more values, each of which must be chosen from the list
   * of values 'value1', 'value2', ... SET values are represented internally as integers.
   * A SET column can have a maximum of 64 distinct members. A table can have no more than 255 unique
   * element list definitions among its ENUM and SET columns considered as a group
   *
   * Protocol: FIELD_TYPE_SET = 248
   */
  case SET
    extends MysqlType(
      "SET",
      Types.CHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      64L,
      "('value1','value2',...) [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * TINYBLOB
   * A BLOB column with a maximum length of 255 (28 - 1) bytes. Each TINYBLOB value is stored using a
   * 1-byte length prefix that indicates the number of bytes in the value.
   *
   * Protocol:FIELD_TYPE_TINY_BLOB = 249
   */
  case TINYBLOB extends MysqlType("TINYBLOB", Types.VARBINARY, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 255L, "")

  /**
   * TINYTEXT [CHARACTER SET charset_name] [COLLATE collation_name]
   * A TEXT column with a maximum length of 255 (28 - 1) characters. The effective maximum length
   * is less if the value contains multibyte characters. Each TINYTEXT value is stored using
   * a 1-byte length prefix that indicates the number of bytes in the value.
   *
   * Protocol:FIELD_TYPE_TINY_BLOB = 249
   */
  case TINYTEXT
    extends MysqlType(
      "TINYTEXT",
      Types.VARCHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      255L,
      " [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * MEDIUMBLOB
   * A BLOB column with a maximum length of 16,777,215 (224 - 1) bytes. Each MEDIUMBLOB value is stored
   * using a 3-byte length prefix that indicates the number of bytes in the value.
   *
   * Protocol: FIELD_TYPE_MEDIUM_BLOB = 250
   */
  case MEDIUMBLOB
    extends MysqlType("MEDIUMBLOB", Types.LONGVARBINARY, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 16777215L, "")

  /**
   * MEDIUMTEXT [CHARACTER SET charset_name] [COLLATE collation_name]
   * A TEXT column with a maximum length of 16,777,215 (224 - 1) characters. The effective maximum length
   * is less if the value contains multibyte characters. Each MEDIUMTEXT value is stored using a 3-byte
   * length prefix that indicates the number of bytes in the value.
   *
   * Protocol: FIELD_TYPE_MEDIUM_BLOB = 250
   */
  case MEDIUMTEXT
    extends MysqlType(
      "MEDIUMTEXT",
      Types.LONGVARCHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      16777215L,
      " [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * LONGBLOB
   * A BLOB column with a maximum length of 4,294,967,295 or 4GB (232 - 1) bytes. The effective maximum length
   * of LONGBLOB columns depends on the configured maximum packet size in the client/server protocol and available
   * memory. Each LONGBLOB value is stored using a 4-byte length prefix that indicates the number of bytes in the value.
   *
   * Protocol: FIELD_TYPE_LONG_BLOB = 251
   */
  case LONGBLOB
    extends MysqlType("LONGBLOB", Types.LONGVARBINARY, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 4294967295L, "")

  /**
   * LONGTEXT [CHARACTER SET charset_name] [COLLATE collation_name]
   * A TEXT column with a maximum length of 4,294,967,295 or 4GB (232 - 1) characters. The effective
   * maximum length is less if the value contains multibyte characters. The effective maximum length
   * of LONGTEXT columns also depends on the configured maximum packet size in the client/server protocol
   * and available memory. Each LONGTEXT value is stored using a 4-byte length prefix that indicates
   * the number of bytes in the value.
   *
   * Protocol: FIELD_TYPE_LONG_BLOB = 251
   */
  case LONGTEXT
    extends MysqlType(
      "LONGTEXT",
      Types.LONGVARCHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      4294967295L,
      " [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * BLOB[(M)]
   * A BLOB column with a maximum length of 65,535 (216 - 1) bytes. Each BLOB value is stored using
   * a 2-byte length prefix that indicates the number of bytes in the value.
   * An optional length M can be given for this type. If this is done, MySQL creates the column as
   * the smallest BLOB type large enough to hold values M bytes long.
   *
   * Protocol: FIELD_TYPE_BLOB = 252
   */
  case BLOB extends MysqlType("BLOB", Types.LONGVARBINARY, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 65535L, "[(M)]")

  /**
   * TEXT[(M)] [CHARACTER SET charset_name] [COLLATE collation_name]
   * A TEXT column with a maximum length of 65,535 (216 - 1) characters. The effective maximum length
   * is less if the value contains multibyte characters. Each TEXT value is stored using a 2-byte length
   * prefix that indicates the number of bytes in the value.
   * An optional length M can be given for this type. If this is done, MySQL creates the column as
   * the smallest TEXT type large enough to hold values M characters long.
   *
   * Protocol: FIELD_TYPE_BLOB = 252
   */
  case TEXT
    extends MysqlType(
      "TEXT",
      Types.LONGVARCHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      65535L,
      "[(M)] [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * [NATIONAL] CHAR[(M)] [CHARACTER SET charset_name] [COLLATE collation_name]
   * A fixed-length string that is always right-padded with spaces to the specified length when stored.
   * M represents the column length in characters. The range of M is 0 to 255. If M is omitted, the length is 1.
   * Note
   * Trailing spaces are removed when CHAR values are retrieved unless the PAD_CHAR_TO_FULL_LENGTH SQL mode is enabled.
   * CHAR is shorthand for CHARACTER. NATIONAL CHAR (or its equivalent short form, NCHAR) is the standard SQL way
   * to define that a CHAR column should use some predefined character set. MySQL 4.1 and up uses utf8
   * as this predefined character set.
   *
   * MySQL permits you to create a column of type CHAR(0). This is useful primarily when you have to be compliant
   * with old applications that depend on the existence of a column but that do not actually use its value.
   * CHAR(0) is also quite nice when you need a column that can take only two values: A column that is defined
   * as CHAR(0) NULL occupies only one bit and can take only the values NULL and '' (the empty string).
   *
   * Protocol: FIELD_TYPE_STRING = 254
   */
  case CHAR
    extends MysqlType(
      "CHAR",
      Types.CHAR,
      0,
      MysqlTypeVariables.IS_NOT_DECIMAL,
      255L,
      "[(M)] [CHARACTER SET charset_name] [COLLATE collation_name]"
    )

  /**
   * BINARY(M)
   * The BINARY type is similar to the CHAR type, but stores binary byte strings rather than nonbinary character strings.
   * M represents the column length in bytes.
   *
   * The CHAR BYTE data type is an alias for the BINARY data type.
   */
  case BINARY extends MysqlType("BINARY", Types.BINARY, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 255L, "(M)")

  /**
   * Top class for Spatial Data Types
   *
   * Protocol: FIELD_TYPE_GEOMETRY = 255
   */
  case GEOMETRY extends MysqlType("GEOMETRY", Types.BINARY, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 65535L, "")

  /**
   * Fall-back type for those MySQL data types which c/J can't recognize.
   * Handled the same as BLOB.
   *
   * Has no protocol ID.
   */
  case UNKNOWN extends MysqlType("UNKNOWN", Types.OTHER, 0, MysqlTypeVariables.IS_NOT_DECIMAL, 65535L, "")

  override def getName():             String = name
  override def getVendor():           String = "ldbc"
  override def getVendorTypeNumber(): Int    = jdbcType

object MysqlType:

  /**
   * Get MysqlType matching the full MySQL type name, for example "DECIMAL(5,3) UNSIGNED ZEROFILL".
   * Distinct *_UNSIGNED type will be returned if "UNSIGNED" is present in fullMysqlTypeName.
   *
   * @param fullMysqlTypeName
   *   full MySQL type name
   * @return MysqlType
   */
  def getByName(fullMysqlTypeName: String): MysqlType =
    val typeName =
      if fullMysqlTypeName.indexOf("(") != -1 then fullMysqlTypeName.substring(0, fullMysqlTypeName.indexOf("(")).trim
      else fullMysqlTypeName

    typeName match
      case name
        if name.contains("DECIMAL") | name.contains("DEC") | name.contains("NUMERIC") | name.contains("FIXED") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.DECIMAL_UNSIGNED else MysqlType.DECIMAL
      case name if name.contains("TINYBLOB") => MysqlType.TINYBLOB
      case name if name.contains("TINYTEXT") => MysqlType.TINYTEXT
      case name if name.contains("TINYINT") | name.contains("TINY") | name.contains("INT1") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.TINYINT_UNSIGNED else MysqlType.TINYINT
      case name
        if name.contains("MEDIUMINT") | name.contains("INT24") | name.contains("INT3") | name.contains("MIDDLEINT") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.MEDIUMINT_UNSIGNED else MysqlType.MEDIUMINT
      case name if name.contains("SMALLINT") | name.contains("INT2") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.SMALLINT_UNSIGNED else MysqlType.SMALLINT
      case name if name.contains("BIGINT") | name.contains("SERIAL") | name.contains("INT8") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.BIGINT_UNSIGNED else MysqlType.BIGINT
      case name if name.contains("POINT") => MysqlType.GEOMETRY
      case name if name.contains("INT") | name.contains("INTEGER") | name.contains("INT4") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.INT_UNSIGNED else MysqlType.INT
      case name if name.contains("DOUBLE") | name.contains("REAL") | name.contains("FLOAT8") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.DOUBLE_UNSIGNED else MysqlType.DOUBLE
      case name if name.contains("FLOAT") =>
        if fullMysqlTypeName.contains("UNSIGNED") then MysqlType.FLOAT_UNSIGNED else MysqlType.FLOAT
      case name if name.contains("NULL")                                         => MysqlType.NULL
      case name if name.contains("TIMESTAMP")                                    => MysqlType.TIMESTAMP
      case name if name.contains("DATETIME")                                     => MysqlType.DATETIME
      case name if name.contains("DATE")                                         => MysqlType.DATE
      case name if name.contains("TIME")                                         => MysqlType.TIME
      case name if name.contains("YEAR")                                         => MysqlType.YEAR
      case name if name.contains("LONGBLOB")                                     => MysqlType.LONGBLOB
      case name if name.contains("LONGTEXT")                                     => MysqlType.LONGTEXT
      case name if name.contains("MEDIUMBLOB") | name.contains("LONG VARBINARY") => MysqlType.MEDIUMBLOB
      case name if name.contains("MEDIUMTEXT") | name.contains("LONG VARCHAR") | name.contains("LONG") =>
        MysqlType.MEDIUMTEXT
      case name
        if name.contains("VARCHAR") | name.contains("NVARCHAR") | name
          .contains("NATIONAL VARCHAR") | name.contains("CHARACTER VARYING") =>
        MysqlType.VARCHAR
      case name if name.contains("VARBINARY")                           => MysqlType.VARBINARY
      case name if name.contains("BINARY") | name.contains("CHAR BYTE") => MysqlType.BINARY
      case name if name.contains("LINESTRING")                          => MysqlType.GEOMETRY
      case name
        if name.contains("STRING") | name.contains("CHAR") | name
          .contains("NCHAR") | name.contains("NATIONAL CHAR") | name.contains("CHARACTER") =>
        MysqlType.CHAR
      case name if name.contains("BOOLEAN") | name.contains("BOOL")                          => MysqlType.BOOLEAN
      case name if name.contains("BIT")                                                      => MysqlType.BIT
      case name if name.contains("JSON")                                                     => MysqlType.JSON
      case name if name.contains("ENUM")                                                     => MysqlType.ENUM
      case name if name.contains("SET")                                                      => MysqlType.SET
      case name if name.contains("BLOB")                                                     => MysqlType.BLOB
      case name if name.contains("TEXT")                                                     => MysqlType.TEXT
      case name if name.contains("GEOM") | name.contains("POINT") | name.contains("POLYGON") => MysqlType.GEOMETRY
      case _                                                                                 => MysqlType.UNKNOWN

/**
 * TODO: The Enum property is buggy if you put it inside a MysqlType object.
 * All numeric types are set to 0, and all Boolean types are set to false.
 * {{{
 *   case TINYINT extends(..., MysqlType.FIELD_FLAG_UNSIGNED, ...)" <- This will not work
 *   case TINYINT extends(..., MysqlTypeVariables.FIELD_FLAG_UNSIGNED, ...)" <- This will work
 * }}}
 */
object MysqlTypeVariables:

  val FIELD_FLAG_NOT_NULL:       Int = 1
  val FIELD_FLAG_PRIMARY_KEY:    Int = 2
  val FIELD_FLAG_UNIQUE_KEY:     Int = 4
  val FIELD_FLAG_MULTIPLE_KEY:   Int = 8
  val FIELD_FLAG_BLOB:           Int = 16
  val FIELD_FLAG_UNSIGNED:       Int = 32
  val FIELD_FLAG_ZEROFILL:       Int = 64
  val FIELD_FLAG_BINARY:         Int = 128
  val FIELD_FLAG_AUTO_INCREMENT: Int = 512

  val IS_DECIMAL:     Boolean = true
  val IS_NOT_DECIMAL: Boolean = false

  // Protocol field type numbers
  val FIELD_TYPE_DECIMAL     = 0
  val FIELD_TYPE_TINY        = 1
  val FIELD_TYPE_SHORT       = 2
  val FIELD_TYPE_LONG        = 3
  val FIELD_TYPE_FLOAT       = 4
  val FIELD_TYPE_DOUBLE      = 5
  val FIELD_TYPE_NULL        = 6
  val FIELD_TYPE_TIMESTAMP   = 7
  val FIELD_TYPE_LONGLONG    = 8
  val FIELD_TYPE_INT24       = 9
  val FIELD_TYPE_DATE        = 10
  val FIELD_TYPE_TIME        = 11
  val FIELD_TYPE_DATETIME    = 12
  val FIELD_TYPE_YEAR        = 13
  val FIELD_TYPE_VARCHAR     = 15
  val FIELD_TYPE_BIT         = 16
  val FIELD_TYPE_JSON        = 245
  val FIELD_TYPE_NEWDECIMAL  = 246
  val FIELD_TYPE_ENUM        = 247
  val FIELD_TYPE_SET         = 248
  val FIELD_TYPE_TINY_BLOB   = 249
  val FIELD_TYPE_MEDIUM_BLOB = 250
  val FIELD_TYPE_LONG_BLOB   = 251
  val FIELD_TYPE_BLOB        = 252
  val FIELD_TYPE_VAR_STRING  = 253
  val FIELD_TYPE_STRING      = 254
  val FIELD_TYPE_GEOMETRY    = 255
