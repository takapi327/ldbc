/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.util

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

import ldbc.authentication.plugin.Openssl.*

/**
 * The Scala Native implementation of the cross-platform hashing used by the MySQL authentication
 * plugins, backed by OpenSSL's one-shot `EVP_Digest` (reusing the `libcrypto` binding already linked by
 * the authentication-plugin module). Mirrors fs2's native hashing backend.
 */
private[mysql] object PlatformHash:

  /** Computes the SHA-1 digest of `data`. */
  def sha1(data: Array[Byte]): Array[Byte] = digest(c"sha1", data, 20)

  /** Computes the SHA-256 digest of `data`. */
  def sha256(data: Array[Byte]): Array[Byte] = digest(c"sha256", data, 32)

  private def digest(name: CString, data: Array[Byte], outLen: Int): Array[Byte] =
    Zone.acquire { implicit zone =>
      val md    = EVP_get_digestbyname(name)
      val input = alloc[Byte](if data.length == 0 then 1 else data.length)
      var i     = 0
      while i < data.length do
        input(i) = data(i)
        i += 1
      val out  = alloc[Byte](outLen)
      val size = stackalloc[CUnsignedInt]()
      EVP_Digest(input, data.length.toCSize, out, size, md, null)
      val result = new Array[Byte](outLen)
      var j      = 0
      while j < outLen do
        result(j) = out(j)
        j += 1
      result
    }
