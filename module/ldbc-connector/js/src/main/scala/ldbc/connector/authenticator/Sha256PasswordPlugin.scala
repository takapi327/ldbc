/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import scodec.bits.ByteVector

trait Sha256PasswordPlugin extends AuthenticationPlugin:

  override def name: String = "sha256_password"

  def transformation: String = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

  private val crypto = js.Dynamic.global.require("crypto")

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val hash1 = sha256(password.getBytes("UTF-8"))
      val hash2 = sha256(hash1)
      val hash3 = sha256(hash2 ++ scramble)

      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

  private def sha256(data: Array[Byte]): Array[Byte] =
    val hash = crypto.createHash("sha256")
    hash.update(ByteVector(data).toUint8Array)
    ByteVector.view(hash.digest().asInstanceOf[Uint8Array]).toArray

  def encryptPassword(password: String, scramble: Array[Byte], publicKeyString: String): Array[Byte] = ??? // TODO

object Sha256PasswordPlugin:

  def apply(): Sha256PasswordPlugin = new Sha256PasswordPlugin {}
