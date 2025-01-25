/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import cats.effect.kernel.Sync

import fs2.hashing.Hashing

import ldbc.connector.util.Version
trait CachingSha2PasswordPlugin[F[_]: Hashing: Sync] extends Sha256PasswordPlugin[F]:

  override def name: String = "caching_sha2_password"

object CachingSha2PasswordPlugin:
  def apply[F[_]: Hashing: Sync](version: Version): CachingSha2PasswordPlugin[F] =
    version.compare(Version(8, 0, 5)) match
      case 1 => new CachingSha2PasswordPlugin {}
      case _ =>
        new CachingSha2PasswordPlugin:
          override def transformation: String = "RSA/ECB/PKCS1Padding"
