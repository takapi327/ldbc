/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql.logging

/**
 * copied from doobie:
 * https://github.com/tpolecat/doobie/blob/main/modules/free/src/main/scala/doobie/util/log.scala#L42
 *
 * Provides additional processing for Doobie `LogEvent`s.
 */
trait LogHandler[F[_]]:

  def run(logEvent: LogEvent): F[Unit]
