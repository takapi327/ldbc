/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.authenticator

import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

import ldbc.authentication.plugin.*
import ldbc.effect.Sync

/**
 * The `mysql_clear_password` authentication plugin: the password is sent to the server as cleartext
 * UTF-8 bytes. Because no hashing protects it in transit, the plugin demands a confidential channel
 * ([[requiresConfidentiality]] is `true`), so it must only be used over TLS.
 */
class MysqlClearPasswordPlugin[F[_]](using F: Sync[F]) extends AuthenticationPlugin[F]:

  override def name: PluginName = MYSQL_CLEAR_PASSWORD

  override def requiresConfidentiality: Boolean = true

  override def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
    if password.isEmpty then F.pure(ByteVector.empty)
    else F.pure(ByteVector(password.getBytes(StandardCharsets.UTF_8)))

object MysqlClearPasswordPlugin:
  def apply[F[_]](using F: Sync[F]): MysqlClearPasswordPlugin[F] = new MysqlClearPasswordPlugin[F]()
