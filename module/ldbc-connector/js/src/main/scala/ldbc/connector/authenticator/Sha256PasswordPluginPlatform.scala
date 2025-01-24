/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator
import java.nio.charset.StandardCharsets

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import scodec.bits.ByteVector
trait Sha256PasswordPluginPlatform[F[_]] { self: Sha256PasswordPlugin[F] =>
  private val crypto = js.Dynamic.global.require("crypto")

  def encryptPassword(password: String, scramble: Array[Byte], publicKeyString: String): Array[Byte] =
    val input = if password.nonEmpty then (password + "\u0000").getBytes(StandardCharsets.UTF_8) else Array[Byte](0)
    val mysqlScrambleBuff = xorString(input, scramble, input.length)
    encryptWithRSAPublicKey(
      mysqlScrambleBuff,
      publicKeyString
    )

  private def encryptWithRSAPublicKey(input: Array[Byte], publicKey: String): Array[Byte] =
    val encrypted = crypto.publicEncrypt(
      publicKey,
      ByteVector(input).toUint8Array
    )
    ByteVector.view(encrypted.asInstanceOf[Uint8Array]).toArray
}
