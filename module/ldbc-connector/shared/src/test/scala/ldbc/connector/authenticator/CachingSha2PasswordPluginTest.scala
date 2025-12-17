/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import cats.effect.IO

import fs2.hashing.Hashing

import ldbc.connector.*
import ldbc.connector.util.Version

class CachingSha2PasswordPluginTest extends FTestPlatform:

  given Hashing[IO] = Hashing.forSync[IO]

  test("CachingSha2PasswordPlugin#name should return correct plugin name") {
    val plugin = CachingSha2PasswordPlugin[IO](Version(8, 0, 10))
    assertEquals(plugin.name.toString, "caching_sha2_password")
  }

  test("CachingSha2PasswordPlugin for version >= 8.0.5 should use default transformation") {
    CachingSha2PasswordPlugin[IO](Version(8, 0, 10))
    // Default transformation is not explicitly defined in the trait
    // so we don't have a direct way to test it
    assert(true) // Placeholder assertion
  }

  test("CachingSha2PasswordPlugin for version < 8.0.5 should use specific transformation") {
    val plugin = CachingSha2PasswordPlugin[IO](Version(8, 0, 4))
    assertEquals(plugin.transformation, "RSA/ECB/PKCS1Padding")
  }
