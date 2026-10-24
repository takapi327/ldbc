/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.util

import java.security.MessageDigest

/**
 * The JVM implementation of the cross-platform hashing used by the MySQL authentication plugins,
 * backed by `java.security.MessageDigest`. Mirrors fs2's JVM hashing backend.
 */
private[mysql] object PlatformHash:

  /** Computes the SHA-1 digest of `data`. */
  def sha1(data: Array[Byte]): Array[Byte] = MessageDigest.getInstance("SHA-1").digest(data)

  /** Computes the SHA-256 digest of `data`. */
  def sha256(data: Array[Byte]): Array[Byte] = MessageDigest.getInstance("SHA-256").digest(data)
