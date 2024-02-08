/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import scala.math.pow

/**
 * The status flags are a bit-field.
 * 
 * @param code
 *  The numeric value of the bit-field.
 */
enum ServerStatusFlags(val code: Long):
  case SERVER_STATUS_IN_TRANS extends ServerStatusFlags(1L << 0)
  case SERVER_STATUS_AUTOCOMMIT extends ServerStatusFlags(1L << 1)
  case SERVER_MORE_RESULTS_EXISTS extends ServerStatusFlags(1L << 3)
  case SERVER_STATUS_NO_GOOD_INDEX_USED extends ServerStatusFlags(1L << 4)
  case SERVER_STATUS_NO_INDEX_USED extends ServerStatusFlags(1L << 5)
  case SERVER_STATUS_CURSOR_EXISTS extends ServerStatusFlags(1L << 6)
  case SERVER_STATUS_LAST_ROW_SENT extends ServerStatusFlags(1L << 7)
  case SERVER_STATUS_DB_DROPPED extends ServerStatusFlags(1L << 8)
  case SERVER_STATUS_NO_BACKSLASH_ESCAPES extends ServerStatusFlags(1L << 9)
  case SERVER_STATUS_METADATA_CHANGED extends ServerStatusFlags(1L << 10)
  case SERVER_QUERY_WAS_SLOW extends ServerStatusFlags(1L << 11)
  case SERVER_PS_OUT_PARAMS extends ServerStatusFlags(1L << 12)
  case SERVER_STATUS_IN_TRANS_READONLY extends ServerStatusFlags(1L << 13)
  case SERVER_SESSION_STATE_CHANGED extends ServerStatusFlags(1L << 14)

object ServerStatusFlags:

  /** Get bitset objects from numeric bitset. */
  def apply(bitset: Long): Seq[ServerStatusFlags] =
    toEnumSeq(bitset)

  /** Get bitset objects from numeric bitsets. */
  def apply(bitset: Seq[Short]): Seq[ServerStatusFlags] =
    bitset.flatMap(b => toEnumSeq(toCode(b)))

  /** Convert bitNum to BitFlag numbers */
  def toCode(bitNum: Short): Long =
    pow(2, bitNum.toDouble).toLong

  /** Calculate bitset as numeric */
  def toBitset(bitset: Seq[ServerStatusFlags]): Long =
    bitset.foldLeft(0L)((code, cur) => code | cur.code)

  /** Calculate bitset as bit flags */
  def toEnumSeq(bitset: Long): Seq[ServerStatusFlags] =
    ServerStatusFlags.values.filter(p => (p.code & bitset) == p.code).toSeq

  /** Check to whether has a bit flag. */
  def hasBitFlag(bitset: Seq[ServerStatusFlags], flag: ServerStatusFlags): Boolean =
    (toBitset(bitset) & flag.code) == flag.code

  def hasBitFlag(bitset: Seq[ServerStatusFlags], code: Long): Boolean = (toBitset(bitset) & code) == code

  def hasBitFlag(bitset: Long, flag: ServerStatusFlags): Boolean = (bitset & flag.code) == flag.code

  def hasBitFlag(bitset: Long, code: Long): Boolean = (bitset & code) == code

  /** Set a specified bit flag. */
  def setBitFlag(bitset: Seq[ServerStatusFlags], flag: ServerStatusFlags): Seq[ServerStatusFlags] = apply(
    toBitset(bitset) | flag.code
  )

  def setBitFlag(bitset: Seq[ServerStatusFlags], code: Long): Seq[ServerStatusFlags] = apply(
    toBitset(bitset) | code
  )

  def setBitFlag(bitset: Long, flag: ServerStatusFlags): Long = bitset | flag.code

  def setBitFlag(bitset: Long, code: Long): Long = bitset | code
