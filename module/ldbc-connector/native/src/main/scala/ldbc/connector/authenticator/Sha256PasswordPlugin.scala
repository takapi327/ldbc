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

trait Sha256PasswordPlugin extends AuthenticationPlugin:

  override def name: String = "sha256_password"

  def transformation: String = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val hash1 = sha256(password.getBytes("UTF-8"))
      val hash2 = sha256(hash1)
      val hash3 = sha256(hash2 ++ scramble)
      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

  private def sha256(data: Array[Byte]): Array[Byte] =
    val md     = new Array[Byte](EVP_MAX_MD_SIZE)
    val size   = stackalloc[CUnsignedInt]()
    val `type` = EVP_get_digestbyname(c"SHA256")
    if `type` == null then throw new RuntimeException("EVP_get_digestbyname")
    val input = ByteVector(data)
    if EVP_Digest(input.toArrayUnsafe.atUnsafe(0), input.size.toULong, md.atUnsafe(0), size, `type`, null) != 1 then
      throw new RuntimeException("EVP_Digest")
    ByteVector.view(md, 0, (!size).toInt).toArray

  def encryptPassword(password: String, scramble: Array[Byte], publicKeyString: String): Array[Byte] = ???

object Sha256PasswordPlugin:

  def apply(): Sha256PasswordPlugin = new Sha256PasswordPlugin {}
