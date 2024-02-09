/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import java.security.MessageDigest

class MysqlNativePasswordPlugin extends AuthenticationPlugin:

  override def name: String = "mysql_native_password"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val sha1 = MessageDigest.getInstance("SHA-1")

      val hash1 = sha1.digest(password.getBytes("UTF-8"))
      val hash2 = sha1.digest(hash1)
      val hash3 = sha1.digest(scramble ++ hash2)

      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }
