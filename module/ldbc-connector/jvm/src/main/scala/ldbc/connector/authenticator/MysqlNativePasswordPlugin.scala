/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator


import cats.effect.Concurrent

import fs2.hashing.Hashing

class MysqlNativePasswordPlugin[F[_]: Hashing: Concurrent] extends AuthenticationPlugin[F]:

  override def name: String = "mysql_native_password"

object MysqlNativePasswordPlugin:
  def apply[F[_]: Hashing: Concurrent](): MysqlNativePasswordPlugin[F] = new MysqlNativePasswordPlugin()
