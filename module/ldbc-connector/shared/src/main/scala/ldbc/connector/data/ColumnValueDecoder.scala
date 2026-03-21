/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import java.time.*

/**
 * Decodes a column's raw byte data to the target type.
 *
 * Implementations receive the raw field bytes as sent over the MySQL wire protocol
 * and convert them directly to the requested JVM type without intermediate String conversion.
 *
 * Contract:
 *   - `bytes` is never empty due to a NULL value; NULL columns are handled as `Option` at the
 *     call site and these methods are only invoked for non-NULL values.
 *   - `bytes` contains data only, without any length-encoded prefix
 *     (equivalent to the output of `BinaryColumnValueDecoder.extractBinaryFieldData`).
 *   - Implementations may return `null` for date/time types when the server sends a
 *     zero-date (`0000-00-00`) encoded as a 0-length field.
 */
private[ldbc] trait ColumnValueDecoder:

  /**
   * Decodes the field bytes as a `String`.
   *
   * For text protocol, the bytes are decoded using the given `charset`.
   * For binary protocol, numeric types are formatted to their decimal string representation.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation (e.g. `"UTF-8"`)
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded string value
   */
  def decodeString(bytes: Array[Byte], charset: String, columnType: ColumnDataType): String

  /**
   * Decodes the field bytes as a `Boolean`.
   *
   * For `MYSQL_TYPE_BOOL`, interprets the single byte as `true` if non-zero.
   * For string-encoded values (text protocol), `"true"` and `"1"` map to `true`.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded boolean value
   */
  def decodeBoolean(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Boolean

  /**
   * Decodes the field bytes as a `Byte`.
   *
   * For `MYSQL_TYPE_TINY`, returns the raw byte value.
   * For single-character strings (text protocol), returns the first byte of the encoded character.
   * For numeric strings, parses and narrows to `Byte`.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded byte value
   */
  def decodeByte(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Byte

  /**
   * Decodes the field bytes as a `Short`.
   *
   * For `MYSQL_TYPE_SHORT` / `MYSQL_TYPE_YEAR`, reads 2 bytes as a little-endian signed short.
   * For `MYSQL_TYPE_TINY`, widens the single unsigned byte.
   * For string-encoded values, parses the decimal representation.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded short value
   */
  def decodeShort(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Short

  /**
   * Decodes the field bytes as an `Int`.
   *
   * Fixed-width integer types are read as little-endian values and narrowed or widened as needed:
   *   - `MYSQL_TYPE_TINY` (1 byte unsigned)
   *   - `MYSQL_TYPE_SHORT` / `MYSQL_TYPE_YEAR` (2 bytes LE signed)
   *   - `MYSQL_TYPE_LONG` / `MYSQL_TYPE_INT24` (4 bytes LE signed)
   *   - `MYSQL_TYPE_LONGLONG` (8 bytes LE signed, narrowed to `Int`)
   * For string-encoded values, parses the decimal representation.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded int value
   */
  def decodeInt(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Int

  /**
   * Decodes the field bytes as a `Long`.
   *
   * Fixed-width integer types are read as little-endian values and widened as needed:
   *   - `MYSQL_TYPE_TINY` (1 byte unsigned)
   *   - `MYSQL_TYPE_SHORT` / `MYSQL_TYPE_YEAR` (2 bytes LE signed)
   *   - `MYSQL_TYPE_LONG` / `MYSQL_TYPE_INT24` (4 bytes LE signed)
   *   - `MYSQL_TYPE_LONGLONG` (8 bytes LE signed)
   * For string-encoded values, parses the decimal representation.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded long value
   */
  def decodeLong(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Long

  /**
   * Decodes the field bytes as a `Float`.
   *
   * For `MYSQL_TYPE_FLOAT`, reads 4 bytes as an IEEE 754 single-precision little-endian float.
   * For string-encoded values, parses the decimal representation.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded float value
   */
  def decodeFloat(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Float

  /**
   * Decodes the field bytes as a `Double`.
   *
   * For `MYSQL_TYPE_DOUBLE`, reads 8 bytes as an IEEE 754 double-precision little-endian double.
   * For `MYSQL_TYPE_FLOAT`, reads 4 bytes and widens to `Double`.
   * For string-encoded values, parses the decimal representation.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded double value
   */
  def decodeDouble(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Double

  /**
   * Decodes the field bytes as a `BigDecimal`.
   *
   * Converts the field bytes to a string using the given charset, then constructs a `BigDecimal`.
   * This is used for `MYSQL_TYPE_DECIMAL` / `MYSQL_TYPE_NEWDECIMAL` columns whose wire
   * representation is always text (even in the binary protocol).
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded BigDecimal value
   */
  def decodeBigDecimal(bytes: Array[Byte], charset: String, columnType: ColumnDataType): BigDecimal

  /**
   * Returns the raw field bytes as-is, or re-encodes the string representation for text protocol.
   *
   * For binary protocol, the bytes are returned directly without copying.
   * For text protocol, the bytes are decoded to a string and re-encoded using the given charset,
   * which is the same as the original bytes for most charsets.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the field bytes
   */
  def decodeBytes(bytes: Array[Byte], charset: String, columnType: ColumnDataType): Array[Byte]

  /**
   * Decodes the field bytes as a `LocalDate`.
   *
   * For binary protocol, interprets the bytes using the MySQL binary date format:
   *   - 0 bytes → `null` (zero-date `0000-00-00`)
   *   - 4 bytes → `year(2 bytes LE) + month(1) + day(1)`
   * For text protocol, parses the `yyyy-MM-dd` formatted string.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded `LocalDate`, or `null` for zero-date values
   */
  def decodeDate(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalDate

  /**
   * Decodes the field bytes as a `LocalTime`.
   *
   * For binary protocol, interprets the bytes using the MySQL binary time format:
   *   - 0 bytes → `null`
   *   - 8 bytes → `isNeg(1) + days(4 LE) + hour(1) + minute(1) + second(1)`
   *   - 12 bytes → same as 8 bytes + `microsecond(4 LE)`
   *
   * Note: the `days` field (for TIME values exceeding 24 hours) is ignored; only
   * `hour`, `minute`, `second`, and `microsecond` are used.
   * For text protocol, parses the `HH:mm:ss[.SSSSSS]` formatted string.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded `LocalTime`, or `null` for zero-time values
   */
  def decodeTime(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalTime

  /**
   * Decodes the field bytes as a `LocalDateTime`.
   *
   * For binary protocol, interprets the bytes using the MySQL binary datetime format:
   *   - 0 bytes → `null` (zero-datetime `0000-00-00 00:00:00`)
   *   - 4 bytes → `year(2 LE) + month(1) + day(1)` (time defaults to midnight)
   *   - 7 bytes → same as 4 bytes + `hour(1) + minute(1) + second(1)`
   *   - 11 bytes → same as 7 bytes + `microsecond(4 LE)`
   * For text protocol, parses the `yyyy-MM-dd HH:mm:ss[.SSSSSS]` formatted string.
   *
   * @param bytes      raw field bytes (no length prefix)
   * @param charset    Java charset name derived from the column's collation
   * @param columnType MySQL column type reported in the column definition packet
   * @return the decoded `LocalDateTime`, or `null` for zero-datetime values
   */
  def decodeTimestamp(bytes: Array[Byte], charset: String, columnType: ColumnDataType): LocalDateTime

  /**
   * Extracts the raw data bytes for the column at `index` (0-based) from a row's raw bytes.
   *
   * Implementations handle their respective wire-protocol format:
   *   - Text protocol: length-encoded strings, NULL represented as 0xFB
   *   - Binary protocol: fixed or variable-width fields after the null bitmap, NULL indicated by null bitmap
   *
   * The returned bytes contain field data only, without any length-encoded prefix.
   * This method is called before any `decode*` method; decode methods are only invoked
   * on non-NULL values (when `Some` is returned here).
   *
   * @param bytes       raw bytes of the entire row packet (no protocol framing)
   * @param index       0-based column index
   * @param columnTypes column types for all columns in the result set (used by binary protocol for field widths)
   * @return `None` for NULL columns, `Some(fieldBytes)` for non-NULL columns
   */
  def extractColumn(bytes: Array[Byte], index: Int, columnTypes: Vector[ColumnDataType]): Option[Array[Byte]]
