/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.authenticator

import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

import ldbc.fx.Fx

import ldbc.authentication.plugin.*

/**
 * The `mysql_clear_password` authentication plugin: the password is sent to the server as cleartext
 * UTF-8 bytes. Because no hashing protects it in transit, the plugin demands a confidential channel
 * ([[requiresConfidentiality]] is `true`), so it must only be used over TLS.
 */
class MysqlClearPasswordPlugin extends AuthenticationPlugin[Fx]:

  override def name: PluginName = MYSQL_CLEAR_PASSWORD

  override def requiresConfidentiality: Boolean = true

  override def hashPassword(password: String, scramble: Array[Byte]): Fx[ByteVector] =
    if password.isEmpty then Fx.pure(ByteVector.empty)
    else Fx.pure(ByteVector(password.getBytes(StandardCharsets.UTF_8)))

object MysqlClearPasswordPlugin:
  def apply(): MysqlClearPasswordPlugin = new MysqlClearPasswordPlugin()
