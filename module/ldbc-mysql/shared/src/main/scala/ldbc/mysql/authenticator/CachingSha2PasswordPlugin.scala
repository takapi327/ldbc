/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.authenticator

import ldbc.mysql.util.Version

import ldbc.authentication.plugin.*

/**
 * The `caching_sha2_password` authentication plugin. It shares [[Sha256PasswordPlugin]]'s hashing; only
 * the RSA padding used for public-key password exchange differs by server version.
 */
trait CachingSha2PasswordPlugin extends Sha256PasswordPlugin:

  override def name: PluginName = CACHING_SHA2_PASSWORD

object CachingSha2PasswordPlugin:

  def apply(version: Version): CachingSha2PasswordPlugin =
    version.compare(Version(8, 0, 5)) match
      case 1 => new CachingSha2PasswordPlugin {}
      case _ =>
        new CachingSha2PasswordPlugin:
          override def transformation: String = "RSA/ECB/PKCS1Padding"
