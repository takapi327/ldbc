/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import java.time.{ LocalDate, LocalDateTime, LocalTime }

/**
 * Synchronous, in-memory access to a single buffered row (no effect).
 *
 * A [[BufferedResultSet]] hands one of these to the per-row `step` inside [[BufferedResultSet.foldRowsSync]] so
 * that a fully materialized result set can be decoded without wrapping every column read in `F`. Column indices
 * are 1-based, matching [[ResultSet]]. `wasNull` reports the null-status of the **last column read on this row**,
 * so callers must read a column before consulting it (same contract as JDBC / [[ResultSet.wasNull]]).
 */
trait SyncRow:
  def getString(columnIndex:     Int): String
  def getBoolean(columnIndex:    Int): Boolean
  def getByte(columnIndex:       Int): Byte
  def getShort(columnIndex:      Int): Short
  def getInt(columnIndex:        Int): Int
  def getLong(columnIndex:       Int): Long
  def getFloat(columnIndex:      Int): Float
  def getDouble(columnIndex:     Int): Double
  def getBytes(columnIndex:      Int): Array[Byte]
  def getDate(columnIndex:       Int): LocalDate
  def getTime(columnIndex:       Int): LocalTime
  def getTimestamp(columnIndex:  Int): LocalDateTime
  def getBigDecimal(columnIndex: Int): BigDecimal
  def wasNull():                       Boolean
