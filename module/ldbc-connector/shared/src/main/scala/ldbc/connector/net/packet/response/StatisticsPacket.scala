/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package response

import scodec.*
import scodec.codecs.*

import cats.syntax.option.*

/**
 * StatisticsPacket is a response packet that contains the statistics of the MySQL server.
 *
 * @param uptime
 *   The uptime of the MySQL server.
 * @param threads
 *   The number of threads connected to the MySQL server.
 * @param questions
 *   The number of questions sent to the MySQL server.
 * @param slowQueries
 *   Total number of queries received since the MySQL server started.
 * @param opens
 *   The number of tables opened by the MySQL server.
 * @param flushTables
 *   The number of tables flushed by the MySQL server.
 * @param openTables
 *   Number of open tables currently held in the cache.
 * @param queriesPerSecondAvg
 *   The average number of queries executed by the MySQL server per second.
 */
case class StatisticsPacket(
  uptime: String,
  threads: String,
  questions: String,
  slowQueries: String,
  opens: String,
  flushTables: String,
  openTables: String,
  queriesPerSecondAvg: String
) extends ResponsePacket:

  override def toString: String = "COM_STATISTICS Response Packet"

object StatisticsPacket:

  def decoder: Decoder[StatisticsPacket] =
    for
      _ <- ignore(8 * 8) // "Uptime: " Skip String
      uptime <- spaceDelimitedStringDecoder
      _ <- ignore(8 * 10) // "  Threads: " Skip String
      threads <- spaceDelimitedStringDecoder
      _ <- ignore(8 * 12) // "  Questions: " Skip String
      questions <- spaceDelimitedStringDecoder
      _ <- ignore(8 * 15) // "  Slow queries: " Skip String
      slowQueries <- spaceDelimitedStringDecoder
      _ <- ignore(8 * 8) // "  Opens: " Skip String
      opens <- spaceDelimitedStringDecoder
      _ <- ignore(8 * 15) // "  Flush tables: " Skip String
      flushTables <- spaceDelimitedStringDecoder
      _ <- ignore(8 * 14) // "  Open tables: " Skip String
      openTables <- spaceDelimitedStringDecoder
      _ <- ignore(8 * 25) // " Queries per second avg: " Skip String
      queriesPerSecondAvg <- spaceDelimitedStringDecoder
    yield StatisticsPacket(
      uptime,
      threads.replace("Threads: ", ""),
      questions,
      slowQueries,
      opens,
      flushTables,
      openTables,
      queriesPerSecondAvg
    )
