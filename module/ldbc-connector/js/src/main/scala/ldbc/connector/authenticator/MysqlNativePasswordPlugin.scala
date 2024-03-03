/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import scodec.bits.ByteVector

class MysqlNativePasswordPlugin extends AuthenticationPlugin:

  private val crypto = js.Dynamic.global.require("crypto")

  override def name: String = "mysql_native_password"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val hash1 = sha1(password.getBytes("UTF-8"))
      val hash2 = sha1(hash1)
      val hash3 = sha1(scramble ++ hash2)

      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

  private def sha1(data: Array[Byte]): Array[Byte] =
    val hash = crypto.createHash("sha1")
    hash.update(ByteVector(data).toUint8Array)
    ByteVector.view(hash.digest().asInstanceOf[Uint8Array]).toArray
