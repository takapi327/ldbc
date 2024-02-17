/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import java.security.MessageDigest

trait Sha256PasswordPlugin extends AuthenticationPlugin:

  override def name: String = "sha256_password"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val sha256 = MessageDigest.getInstance("SHA-256")
      val hash1  = sha256.digest(password.getBytes("UTF-8"))
      val hash2  = sha256.digest(hash1)
      val hash3  = sha256.digest(hash2 ++ scramble)
      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

object Sha256PasswordPlugin:

  def apply(): Sha256PasswordPlugin = new Sha256PasswordPlugin {}
