/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import java.nio.charset.StandardCharsets
import java.security.{ KeyFactory, PublicKey }
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

import javax.crypto.Cipher

import cats.effect.Concurrent

import fs2.hashing.Hashing

trait Sha256PasswordPlugin[F[_]: Hashing: Concurrent] extends AuthenticationPlugin[F]:

  override def name: String = "sha256_password"

  def transformation: String = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

  def encryptPassword(password: String, scramble: Array[Byte], publicKeyString: String): Array[Byte] =
    val input = if password.nonEmpty then (password + "\u0000").getBytes(StandardCharsets.UTF_8) else Array[Byte](0)
    val mysqlScrambleBuff = xorString(input, scramble, input.length)
    encryptWithRSAPublicKey(
      mysqlScrambleBuff,
      decodeRSAPublicKey(publicKeyString)
    )

  private def xorString(from: Array[Byte], scramble: Array[Byte], length: Int): Array[Byte] =
    val scrambleLength = scramble.length
    (0 until length).map(pos => (from(pos) ^ scramble(pos % scrambleLength)).toByte).toArray

  private def encryptWithRSAPublicKey(input: Array[Byte], key: PublicKey): Array[Byte] =
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    cipher.doFinal(input)

  private def decodeRSAPublicKey(key: String): RSAPublicKey =
    val offset          = key.indexOf("\n") + 1
    val len             = key.indexOf("-----END PUBLIC KEY-----") - offset
    val certificateData = Base64.getMimeDecoder.decode(key.substring(offset, offset + len))
    val spec            = new X509EncodedKeySpec(certificateData)
    val kf              = KeyFactory.getInstance("RSA")
    kf.generatePublic(spec).asInstanceOf[RSAPublicKey]

object Sha256PasswordPlugin:

  def apply[F[_]: Hashing: Concurrent](): Sha256PasswordPlugin[F] = new Sha256PasswordPlugin {}
