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
opaque type ServerStatusFlags = Int

object ServerStatusFlags:

  val SERVER_STATUS_IN_TRANS:             ServerStatusFlags = 1 << 0
  val SERVER_STATUS_AUTOCOMMIT:           ServerStatusFlags = 1 << 1
  val SERVER_MORE_RESULTS_EXISTS:         ServerStatusFlags = 1 << 3
  val SERVER_STATUS_NO_GOOD_INDEX_USED:   ServerStatusFlags = 1 << 4
  val SERVER_STATUS_NO_INDEX_USED:        ServerStatusFlags = 1 << 5
  val SERVER_STATUS_CURSOR_EXISTS:        ServerStatusFlags = 1 << 6
  val SERVER_STATUS_LAST_ROW_SENT:        ServerStatusFlags = 1 << 7
  val SERVER_STATUS_DB_DROPPED:           ServerStatusFlags = 1 << 8
  val SERVER_STATUS_NO_BACKSLASH_ESCAPES: ServerStatusFlags = 1 << 9
  val SERVER_STATUS_METADATA_CHANGED:     ServerStatusFlags = 1 << 10
  val SERVER_QUERY_WAS_SLOW:              ServerStatusFlags = 1 << 11
  val SERVER_PS_OUT_PARAMS:               ServerStatusFlags = 1 << 12
  val SERVER_STATUS_IN_TRANS_READONLY:    ServerStatusFlags = 1 << 13
  val SERVER_SESSION_STATE_CHANGED:       ServerStatusFlags = 1 << 14

  val values: Set[ServerStatusFlags] = Set(
    SERVER_STATUS_IN_TRANS,
    SERVER_STATUS_AUTOCOMMIT,
    SERVER_MORE_RESULTS_EXISTS,
    SERVER_STATUS_NO_GOOD_INDEX_USED,
    SERVER_STATUS_NO_INDEX_USED,
    SERVER_STATUS_CURSOR_EXISTS,
    SERVER_STATUS_LAST_ROW_SENT,
    SERVER_STATUS_DB_DROPPED,
    SERVER_STATUS_NO_BACKSLASH_ESCAPES,
    SERVER_STATUS_METADATA_CHANGED,
    SERVER_QUERY_WAS_SLOW,
    SERVER_PS_OUT_PARAMS,
    SERVER_STATUS_IN_TRANS_READONLY,
    SERVER_SESSION_STATE_CHANGED
  )

  /** Get bitset objects from numeric bitset. */
  def apply(bitset: Long): Set[ServerStatusFlags] =
    toEnumSet(bitset)

  /** Get bitset objects from numeric bitsets. */
  def apply(bitset: Set[Short]): Set[ServerStatusFlags] =
    bitset.flatMap(b => toEnumSet(toCode(b)))

  /** Convert bitNum to BitFlag numbers */
  def toCode(bitNum: Short): Long =
    pow(2, bitNum.toDouble).toLong

  /** Calculate bitset as numeric */
  def toBitset(bitset: Set[ServerStatusFlags]): Long =
    bitset.foldLeft(0L)((code, cur) => code | cur)

  /** Calculate bitset as bit flags */
  def toEnumSet(bitset: Long): Set[ServerStatusFlags] =
    ServerStatusFlags.values.filter(p => (bitset & p) == p).toSet

  /** Check to whether has a bit flag. */
  def hasBitFlag(bitset: Set[ServerStatusFlags], flag: ServerStatusFlags): Boolean =
    (toBitset(bitset) & flag) == flag

  def hasBitFlag(bitset: Long, flag: ServerStatusFlags): Boolean = (bitset & flag) == flag

  /** Set a specified bit flag. */
  def setBitFlag(bitset: Set[ServerStatusFlags], flag: ServerStatusFlags): Set[ServerStatusFlags] = apply(
    toBitset(bitset) | flag
  )

  def setBitFlag(bitset: Long, flag: ServerStatusFlags): Long = bitset | flag
