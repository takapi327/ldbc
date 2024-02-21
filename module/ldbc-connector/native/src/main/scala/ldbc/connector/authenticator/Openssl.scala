/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import org.typelevel.scalaccompat.annotation.*

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@nowarn212
@link("crypto")
@extern
private[ldbc] object Openssl:

  final val EVP_MAX_MD_SIZE = 64

  type EVP_MD
  type EVP_MD_CTX
  type ENGINE
  type BIO
  type EVP_PKEY
  type EVP_PKEY_CTX

  type pem_password_cb = CFuncPtr4[Ptr[Byte], CInt, CInt, Ptr[Byte], CInt]

  def EVP_get_digestbyname(name: Ptr[CChar]): Ptr[EVP_MD] = extern

  def EVP_Digest(
    data:   Ptr[Byte],
    count:  CSize,
    md:     Ptr[Byte],
    size:   Ptr[CUnsignedInt],
    `type`: Ptr[EVP_MD],
    impl:   Ptr[ENGINE]
  ): CInt = extern

  def BIO_new_mem_buf(buf: Ptr[Byte], len: CInt): Ptr[BIO] = extern

  def PEM_read_bio_PUBKEY(bp: Ptr[BIO], x: Ptr[Ptr[EVP_PKEY]], cb: pem_password_cb, u: Ptr[Byte]): Ptr[EVP_PKEY] = extern

  def EVP_PKEY_CTX_new(pkey: Ptr[EVP_PKEY], e: Ptr[ENGINE]): Ptr[EVP_PKEY_CTX] = extern

  def EVP_PKEY_encrypt_init(ctx: Ptr[EVP_PKEY_CTX]): CInt = extern

  def EVP_PKEY_encrypt(
    ctx: Ptr[EVP_PKEY_CTX],
    out: Ptr[UByte],
    outlen: Ptr[CSize],
    in: Ptr[UByte],
    inlen: CSize
  ): CInt = extern
