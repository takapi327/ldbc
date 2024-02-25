/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import scala.math.pow

/**
 * ColumnDefinitionFlags is a bitset of column definition flags.
 *
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/group__group__cs__column__definition__flags.html
 */
enum ColumnDefinitionFlags(val code: Long):
  case NOT_NULL_FLAG         extends ColumnDefinitionFlags(1L << 0)
  case PRI_KEY_FLAG          extends ColumnDefinitionFlags(1L << 1)
  case UNIQUE_KEY_FLAG       extends ColumnDefinitionFlags(1L << 2)
  case MULTIPLE_KEY_FLAG     extends ColumnDefinitionFlags(1L << 3)
  case BLOB_FLAG             extends ColumnDefinitionFlags(1L << 4)
  case UNSIGNED_FLAG         extends ColumnDefinitionFlags(1L << 5)
  case ZEROFILL_FLAG         extends ColumnDefinitionFlags(1L << 6)
  case BINARY_FLAG           extends ColumnDefinitionFlags(1L << 7)
  case ENUM_FLAG             extends ColumnDefinitionFlags(1L << 8)
  case AUTO_INCREMENT_FLAG   extends ColumnDefinitionFlags(1L << 9)
  case TIMESTAMP_FLAG        extends ColumnDefinitionFlags(1L << 10)
  case SET_FLAG              extends ColumnDefinitionFlags(1L << 11)
  case NO_DEFAULT_VALUE_FLAG extends ColumnDefinitionFlags(1L << 12)
  case ON_UPDATE_NOW_FLAG    extends ColumnDefinitionFlags(1L << 13)
  case PART_KEY_FLAG         extends ColumnDefinitionFlags(1L << 14)
  case NUM_FLAG              extends ColumnDefinitionFlags(1L << 15)
  case UNIQUE_FLAG                    extends ColumnDefinitionFlags(1L << 16)
  case BINCMP_FLAG                    extends ColumnDefinitionFlags(1L << 17)
  case GET_FIXED_FIELDS_FLAG          extends ColumnDefinitionFlags(1L << 18)
  case FIELD_IN_PART_FUNC_FLAG        extends ColumnDefinitionFlags(1L << 19)
  case FIELD_IN_ADD_INDEX             extends ColumnDefinitionFlags(1L << 20)
  case FIELD_IS_RENAMED               extends ColumnDefinitionFlags(1L << 21)
  case FIELD_FLAGS_STORAGE_MEDIA      extends ColumnDefinitionFlags(22)
  case FIELD_FLAGS_STORAGE_MEDIA_MASK extends ColumnDefinitionFlags(3L << 22)
  case FIELD_FLAGS_COLUMN_FORMAT      extends ColumnDefinitionFlags(24)
  case FIELD_FLAGS_COLUMN_FORMAT_MASK extends ColumnDefinitionFlags(3L << 24)
  case FIELD_IS_DROPPED               extends ColumnDefinitionFlags(1L << 26)
  case EXPLICIT_NULL_FLAG             extends ColumnDefinitionFlags(1L << 27)
  case NOT_SECONDARY_FLAG             extends ColumnDefinitionFlags(1L << 29)
  case FIELD_IS_INVISIBLE             extends ColumnDefinitionFlags(1L << 30)

object ColumnDefinitionFlags:

  /** Get bitset objects from numeric bitset. */
  def apply(bitset: Long): Seq[ColumnDefinitionFlags] =
    toEnumSeq(bitset)

  /** Get bitset objects from numeric bitsets. */
  def apply(bitset: Seq[Short]): Seq[ColumnDefinitionFlags] =
    bitset.flatMap(b => toEnumSeq(toCode(b)))

  /** Convert bitNum to BitFlag numbers */
  def toCode(bitNum: Short): Long =
    pow(2, bitNum.toDouble).toLong

  /** Calculate bitset as numeric */
  def toBitset(bitset: Seq[ColumnDefinitionFlags]): Long =
    bitset.foldLeft(0L)((code, cur) => code | cur.code)

  /** Calculate bitset as bit flags */
  def toEnumSeq(bitset: Long): Seq[ColumnDefinitionFlags] =
    ColumnDefinitionFlags.values.filter(p => (p.code & bitset) == p.code).toSeq

  /** Check to whether has a bit flag. */
  def hasBitFlag(bitset: Seq[ColumnDefinitionFlags], flag: ColumnDefinitionFlags): Boolean =
    (toBitset(bitset) & flag.code) == flag.code

  def hasBitFlag(bitset: Seq[ColumnDefinitionFlags], code: Long): Boolean = (toBitset(bitset) & code) == code

  def hasBitFlag(bitset: Long, flag: ColumnDefinitionFlags): Boolean = (bitset & flag.code) == flag.code

  def hasBitFlag(bitset: Long, code: Long): Boolean = (bitset & code) == code

  /** Set a specified bit flag. */
  def setBitFlag(bitset: Seq[ColumnDefinitionFlags], flag: ColumnDefinitionFlags): Seq[ColumnDefinitionFlags] = apply(
    toBitset(bitset) | flag.code
  )

  def setBitFlag(bitset: Seq[ColumnDefinitionFlags], code: Long): Seq[ColumnDefinitionFlags] = apply(
    toBitset(bitset) | code
  )

  def setBitFlag(bitset: Long, flag: ColumnDefinitionFlags): Long = bitset | flag.code

  def setBitFlag(bitset: Long, code: Long): Long = bitset | code
