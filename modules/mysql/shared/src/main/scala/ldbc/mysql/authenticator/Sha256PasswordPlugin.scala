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
import ldbc.mysql.util.PlatformHash

/**
 * The `sha256_password` authentication plugin: the token is
 * `SHA256(password) XOR SHA256(SHA256(SHA256(password)) ++ scramble)`. RSA public-key encryption of the
 * password (for non-TLS connections) is provided by [[EncryptPasswordPlugin]].
 */
trait Sha256PasswordPlugin[F[_]] extends AuthenticationPlugin[F], EncryptPasswordPlugin:

  /** The effect instance used to suspend the hashing. Concrete instances supply it. */
  protected given effect: Sync[F]

  override def name: PluginName = SHA256_PASSWORD

  override def requiresConfidentiality: Boolean = false

  override def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
    if password.isEmpty then effect.pure(ByteVector.empty)
    else
      effect.delay {
        val hash1 = PlatformHash.sha256(password.getBytes(StandardCharsets.UTF_8))
        val hash2 = PlatformHash.sha256(hash1)
        val hash3 = PlatformHash.sha256(hash2 ++ scramble)
        ByteVector(hash1).xor(ByteVector(hash3))
      }

  def transformation: String = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

object Sha256PasswordPlugin:

  def apply[F[_]](using F: Sync[F]): Sha256PasswordPlugin[F] =
    new Sha256PasswordPlugin[F]:
      override protected given effect: Sync[F] = F
