/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import java.nio.charset.StandardCharsets
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.security.{ KeyFactory, MessageDigest, PublicKey }
import java.util.Base64
import javax.crypto.Cipher

trait Sha256PasswordPlugin extends AuthenticationPlugin:

  def password:        Option[String]
  def publicKeyString: Option[String]

  override def name: String = "sha256_password"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val sha256 = MessageDigest.getInstance("SHA-256")
      val hash1  = sha256.digest(password.getBytes("UTF-8"))
      val hash2  = sha256.digest(hash1)
      val hash3  = sha256.digest(hash2 ++ scramble)
      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

  def encryptPassword: Array[Byte] =
    encryptPassword("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")

  def encryptPassword(transformation: String): Array[Byte] =
    val input = password match
      case Some(p) => (p + "\u0000").getBytes(StandardCharsets.UTF_8)
      case None    => Array[Byte](0)
    val mysqlScrambleBuff = new Array[Byte](input.length)
    encryptWithRSAPublicKey(
      input.zip(mysqlScrambleBuff).map { case (a, b) => (b ^ a).toByte },
      decodeRSAPublicKey(publicKeyString.getOrElse(throw new Exception("No public key found."))),
      transformation
    )

  private def encryptWithRSAPublicKey(input: Array[Byte], key: PublicKey, transformation: String): Array[Byte] =
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    cipher.doFinal(input)

  private def decodeRSAPublicKey(key: String): RSAPublicKey =
    val offset          = key.indexOf("\n") + 1
    val len             = key.indexOf("-----END PUBLIC KEY-----") - offset
    val certificateData = Base64.getDecoder.decode(key.substring(offset, offset + len))
    val spec            = new X509EncodedKeySpec(certificateData)
    val kf              = KeyFactory.getInstance("RSA")
    kf.generatePublic(spec).asInstanceOf[RSAPublicKey]

object Sha256PasswordPlugin:

  def apply(_password: String): Sha256PasswordPlugin =
    new Sha256PasswordPlugin:
      override def password:        Option[String] = Some(_password)
      override def publicKeyString: Option[String] = None

  def apply(_password: String, _publicKeyString: String): Sha256PasswordPlugin =
    new Sha256PasswordPlugin:
      override def password:        Option[String] = Some(_password)
      override def publicKeyString: Option[String] = Some(_publicKeyString)
