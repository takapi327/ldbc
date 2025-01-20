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

import cats.effect.Concurrent

import fs2.hashing.Hashing
trait Sha256PasswordPlugin[F[_]: Hashing: Concurrent] extends AuthenticationPlugin[F]:

  override def name: String = "sha256_password"

  def transformation: String = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

  private val crypto = js.Dynamic.global.require("crypto")

  def encryptPassword(password: String, scramble: Array[Byte], publicKeyString: String): Array[Byte] =
    val input = if password.nonEmpty then (password + "\u0000").getBytes(StandardCharsets.UTF_8) else Array[Byte](0)
    val mysqlScrambleBuff = xorString(input, scramble, input.length)
    encryptWithRSAPublicKey(
      mysqlScrambleBuff,
      publicKeyString
    )

  private def xorString(from: Array[Byte], scramble: Array[Byte], length: Int): Array[Byte] =
    val scrambleLength = scramble.length
    (0 until length).map(pos => (from(pos) ^ scramble(pos % scrambleLength)).toByte).toArray

  private def encryptWithRSAPublicKey(input: Array[Byte], publicKey: String): Array[Byte] =
    val encrypted = crypto.publicEncrypt(
      publicKey,
      ByteVector(input).toUint8Array
    )
    ByteVector.view(encrypted.asInstanceOf[Uint8Array]).toArray

object Sha256PasswordPlugin:

  def apply[F[_]: Hashing: Concurrent](): Sha256PasswordPlugin[F] = new Sha256PasswordPlugin {}
