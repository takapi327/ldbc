/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

import cats.effect.kernel.Sync

@deprecated(
  "This plugin is deprecated. Please use ldbc.authentication.plugin.MysqlClearPasswordPlugin instead.",
  "0.5.0"
)
class MysqlClearPasswordPlugin[F[_]: Sync] extends AuthenticationPlugin[F]:

  override def name:                                                  String        = "mysql_clear_password"
  override def requiresConfidentiality:                               Boolean       = true
  override def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
    if password.isEmpty then Sync[F].pure(ByteVector.empty)
    else Sync[F].delay(ByteVector(password.getBytes(StandardCharsets.UTF_8)))

object MysqlClearPasswordPlugin:
  @deprecated(
    "This plugin is deprecated. Please use ldbc.authentication.plugin.MysqlClearPasswordPlugin instead.",
    "0.5.0"
  )
  def apply[F[_]: Sync](): MysqlClearPasswordPlugin[F] = new MysqlClearPasswordPlugin[F]()
