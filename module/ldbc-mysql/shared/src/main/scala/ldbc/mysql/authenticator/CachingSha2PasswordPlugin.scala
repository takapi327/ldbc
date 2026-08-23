/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.authenticator

import ldbc.authentication.plugin.*
import ldbc.effect.Sync
import ldbc.mysql.util.Version

/**
 * The `caching_sha2_password` authentication plugin. It shares [[Sha256PasswordPlugin]]'s hashing; only
 * the RSA padding used for public-key password exchange differs by server version.
 */
trait CachingSha2PasswordPlugin[F[_]] extends Sha256PasswordPlugin[F]:

  override def name: PluginName = CACHING_SHA2_PASSWORD

object CachingSha2PasswordPlugin:

  def apply[F[_]](version: Version)(using F: Sync[F]): CachingSha2PasswordPlugin[F] =
    version.compare(Version(8, 0, 5)) match
      case 1 =>
        new CachingSha2PasswordPlugin[F]:
          override protected given effect: Sync[F] = F
      case _ =>
        new CachingSha2PasswordPlugin[F]:
          override protected given effect: Sync[F]        = F
          override def transformation:     String          = "RSA/ECB/PKCS1Padding"
