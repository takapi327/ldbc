/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import org.typelevel.scalaccompat.annotation.*

import scala.scalanative.unsafe.*

@nowarn212
@link("crypto")
@extern
private[ldbc] object Openssl:

  final val EVP_MAX_MD_SIZE = 64

  type EVP_MD
  type EVP_MD_CTX
  type ENGINE

  def EVP_get_digestbyname(name: Ptr[CChar]): Ptr[EVP_MD] = extern

  def EVP_Digest(
    data:   Ptr[Byte],
    count:  CSize,
    md:     Ptr[Byte],
    size:   Ptr[CUnsignedInt],
    `type`: Ptr[EVP_MD],
    impl:   Ptr[ENGINE]
  ): CInt = extern
