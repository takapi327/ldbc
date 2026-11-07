/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.util

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import scodec.bits.ByteVector

/**
 * The Scala.js implementation of the cross-platform hashing used by the MySQL authentication plugins,
 * backed by Node.js's `crypto.createHash`. Mirrors fs2's JS hashing backend.
 */
private[mysql] object PlatformHash:

  private val crypto = js.Dynamic.global.require("crypto")

  /** Computes the SHA-1 digest of `data`. */
  def sha1(data: Array[Byte]): Array[Byte] = digest("sha1", data)

  /** Computes the SHA-256 digest of `data`. */
  def sha256(data: Array[Byte]): Array[Byte] = digest("sha256", data)

  private def digest(algorithm: String, data: Array[Byte]): Array[Byte] =
    val hash = crypto.createHash(algorithm)
    hash.update(ByteVector(data).toUint8Array)
    ByteVector.view(hash.digest().asInstanceOf[Uint8Array]).toArray
