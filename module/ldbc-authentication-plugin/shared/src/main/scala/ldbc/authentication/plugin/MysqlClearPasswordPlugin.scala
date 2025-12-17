/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.authentication.plugin

import cats.Applicative

import scodec.bits.ByteVector

import java.nio.charset.StandardCharsets

trait MysqlClearPasswordPlugin[F[_]] extends AuthenticationPlugin[F]:

  override final def name:                                                  PluginName        = MYSQL_CLEAR_PASSWORD
  override final def requiresConfidentiality:                               Boolean       = true

object MysqlClearPasswordPlugin:

  private case class Impl[F[_]: Applicative]() extends MysqlClearPasswordPlugin[F]:
    override def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
      val result = if password.isEmpty then ByteVector.empty 
      else ByteVector(password.getBytes(StandardCharsets.UTF_8))
      Applicative[F].pure(result)

  def apply[F[_]: Applicative](): MysqlClearPasswordPlugin[F] = Impl[F]()
