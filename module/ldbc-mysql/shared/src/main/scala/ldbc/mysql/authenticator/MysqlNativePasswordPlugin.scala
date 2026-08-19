/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.authenticator

import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

import ldbc.authentication.plugin.*
import ldbc.fx.Fx
import ldbc.mysql.util.PlatformHash

/**
 * The `mysql_native_password` authentication plugin: the token is
 * `SHA1(password) XOR SHA1(scramble ++ SHA1(SHA1(password)))`.
 */
class MysqlNativePasswordPlugin extends AuthenticationPlugin[Fx]:

  override def name: PluginName = MYSQL_NATIVE_PASSWORD

  override def requiresConfidentiality: Boolean = false

  override def hashPassword(password: String, scramble: Array[Byte]): Fx[ByteVector] =
    if password.isEmpty then Fx.pure(ByteVector.empty)
    else
      Fx.delay {
        val hash1 = PlatformHash.sha1(password.getBytes(StandardCharsets.UTF_8))
        val hash2 = PlatformHash.sha1(hash1)
        val hash3 = PlatformHash.sha1(scramble ++ hash2)
        ByteVector(hash1).xor(ByteVector(hash3))
      }

object MysqlNativePasswordPlugin:
  def apply(): MysqlNativePasswordPlugin = new MysqlNativePasswordPlugin()
