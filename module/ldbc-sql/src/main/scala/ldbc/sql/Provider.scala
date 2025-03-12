/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import ldbc.sql.logging.LogHandler

trait Provider[F[_]]:
  
  def logHandler: LogHandler[F]

  def use[A](f: Connection[F] => F[A]): F[A]
