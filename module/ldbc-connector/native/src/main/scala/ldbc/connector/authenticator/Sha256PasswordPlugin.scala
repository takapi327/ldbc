/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import java.nio.charset.StandardCharsets

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

import cats.effect.Concurrent

import fs2.hashing.Hashing

import ldbc.connector.authenticator.Openssl.*

trait Sha256PasswordPlugin[F[_]: Hashing: Concurrent] extends AuthenticationPlugin[F]:

  override def name: String = "sha256_password"

  def transformation: String = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"

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
    Zone { implicit zone =>
      val publicKeyCStr = toCString(publicKey)
      val bio           = BIO_new_mem_buf(publicKeyCStr, publicKey.length)
      if bio == null then throw new RuntimeException("Failed to create a new memory BIO.")

      val evpPkey = PEM_read_bio_PUBKEY(bio, null, null, null)
      if evpPkey == null then throw new RuntimeException("Failed to load public key.")

      val ctx = EVP_PKEY_CTX_new(evpPkey, null)
      if EVP_PKEY_encrypt_init(ctx) <= 0 then throw new RuntimeException("Failed to initialize encryption context.")

      if EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_OAEP_PADDING) != 1 then {
        throw new RuntimeException("Failed to set RSA padding.")
      }

      val sha1Md = EVP_get_digestbyname(c"sha1")
      if EVP_PKEY_CTX_set_rsa_oaep_md(ctx, sha1Md) != 1 then {
        throw new RuntimeException("Failed to set OAEP hash function.")
      }

      if EVP_PKEY_CTX_set_rsa_mgf1_md(ctx, sha1Md) != 1 then {
        throw new RuntimeException("Failed to set MGF1 hash function.")
      }

      val inputBuf = alloc[UByte](input.length.toULong)
      for i <- input.indices do !(inputBuf + i) = input(i).toUByte

      val outLen = stackalloc[CSize]()
      !outLen = 0.toULong

      if EVP_PKEY_encrypt(ctx, null, outLen, inputBuf, input.length.toULong) <= 0 then
        throw new RuntimeException("Failed to obtain the output buffer size required for encryption.")

      val encryptedBuf = alloc[UByte](!outLen)
      if EVP_PKEY_encrypt(ctx, encryptedBuf, outLen, inputBuf, input.length.toULong) <= 0 then
        throw new RuntimeException("Encryption failed.")

      val result = Array.fill[Byte]((!outLen).toInt)(0)
      for i <- 0 until (!outLen).toInt do result(i) = (!(encryptedBuf + i)).toInt.toByte

      result
    }

object Sha256PasswordPlugin:

  def apply[F[_]: Hashing: Concurrent](): Sha256PasswordPlugin[F] = new Sha256PasswordPlugin {}
