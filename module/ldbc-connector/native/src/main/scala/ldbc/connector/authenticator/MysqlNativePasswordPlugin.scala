/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

import scodec.bits.ByteVector

import ldbc.connector.authenticator.Openssl.*

class MysqlNativePasswordPlugin extends AuthenticationPlugin:

  override def name: String = "mysql_native_password"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val hash1 = sha1(password.getBytes("UTF-8"))
      val hash2 = sha1(hash1)
      val hash3 = sha1(scramble ++ hash2)

      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

  private def sha1(data: Array[Byte]): Array[Byte] =
    val md     = new Array[Byte](EVP_MAX_MD_SIZE)
    val size   = stackalloc[CUnsignedInt]()
    val `type` = EVP_get_digestbyname(c"SHA1")
    if `type` == null then throw new RuntimeException("EVP_get_digestbyname")
    val input = ByteVector(data)
    if EVP_Digest(input.toArrayUnsafe.atUnsafe(0), input.size.toULong, md.atUnsafe(0), size, `type`, null) != 1 then
      throw new RuntimeException("EVP_Digest")
    ByteVector.view(md, 0, (!size).toInt).toArray
