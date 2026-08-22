/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.protocol

import ldbc.fx.Fx
import ldbc.mysql.data.EnumMySQLSetOption
import ldbc.mysql.net.packet.response.*

/**
 * Utility commands
 * 
 * @tparam F
 *   the effect type
 */
trait UtilityCommands:

  /**
   * Quit the connection
   */
  def comQuit(): Fx[Unit]

  /**
   * Initialize the database
   * 
   * @param schema
   *   the name of a schema in which to work
   */
  def comInitDB(schema: String): Fx[Unit]

  /**
   * Get the statistics of the connection
   */
  def comStatistics(): Fx[StatisticsPacket]

  /**
   * Check if the server is alive.
   */
  def comPing(): Fx[Boolean]

  /**
   * Reset the connection
   */
  def comResetConnection(): Fx[Unit]

  /**
   * Set an option
   * 
   * @param optionOperation
   *   the option operation
   */
  def comSetOption(optionOperation: EnumMySQLSetOption): Fx[Unit]
