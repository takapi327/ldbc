/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import ldbc.connector.util.Version

trait CachingSha2PasswordPlugin extends Sha256PasswordPlugin:

  override def name: String = "caching_sha2_password"

object CachingSha2PasswordPlugin:
  def apply(version: Version): CachingSha2PasswordPlugin =
    version.compare(Version(8, 0, 5)) match
      case 1 => new CachingSha2PasswordPlugin {}
      case _ =>
        new CachingSha2PasswordPlugin:
          override def transformation: String = "RSA/ECB/PKCS1Padding"
