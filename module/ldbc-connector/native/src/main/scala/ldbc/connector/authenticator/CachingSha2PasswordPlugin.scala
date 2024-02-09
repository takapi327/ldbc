/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

case class CachingSha2PasswordPlugin(password: Option[String], publicKeyString: Option[String])
  extends Sha256PasswordPlugin:

  override def name: String = "caching_sha2_password"
