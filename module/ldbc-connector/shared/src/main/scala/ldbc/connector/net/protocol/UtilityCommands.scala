/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import ldbc.connector.data.EnumMySQLSetOption
import ldbc.connector.net.packet.response.*

/**
 * Utility commands
 * 
 * @tparam F
 *   the effect type
 */
trait UtilityCommands[F[_]]:

  /**
   * Quit the connection
   */
  def comQuit(): F[Unit]

  /**
   * Initialize the database
   * 
   * @param schema
   *   the name of a schema in which to work
   */
  def comInitDB(schema: String): F[Unit]

  /**
   * Get the statistics of the connection
   */
  def comStatistics(): F[StatisticsPacket]

  /**
   * Check if the server is alive.
   */
  def comPing(): F[Boolean]

  /**
   * Reset the connection
   */
  def comResetConnection(): F[Unit]

  /**
   * Set an option
   * 
   * @param optionOperation
   *   the option operation
   */
  def comSetOption(optionOperation: EnumMySQLSetOption): F[Unit]
