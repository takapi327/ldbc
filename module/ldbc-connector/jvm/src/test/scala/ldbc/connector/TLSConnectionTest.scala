/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

class TLSConnectionTest extends FTestPlatform:

  test("Verify that you can connect to MySQL with a TLS connection") {
    assertIO(
      ConnectionProvider
        .default[IO](
          "127.0.0.1",
          13306,
          "ldbc_ssl_user",
          "securepassword",
          "world"
        )
        .setSSL(SSL.fromKeyStoreResource("keystore.jks", "password".toCharArray, "password".toCharArray))
        .use { conn =>
          for
            statement <- conn.createStatement()
            result    <- statement.executeQuery("SELECT 1")
            value     <- result.getInt(1)
          yield value
        },
      1
    )
  }
