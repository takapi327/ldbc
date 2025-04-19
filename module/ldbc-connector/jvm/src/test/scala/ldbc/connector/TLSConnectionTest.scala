/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.ApplicativeError

import cats.effect.*

import fs2.io.net.tls.TLSContext
import fs2.io.net.Network

class TLSConnectionTest extends FTestPlatform:

  object KeyStore extends SSL:
    override def tlsContext[F[_]: Network](using ae: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
      Resource.eval(
        Network[F].tlsContext.fromKeyStoreResource(
          "keystore.jks",
          "password".toCharArray,
          "password".toCharArray
        )
      )

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
        .setSSL(KeyStore)
        .use { conn =>
          for
            statement <- conn.createStatement()
            result    <- statement.executeQuery("SELECT 1")
          yield result.getInt(1)
        },
      1
    )
  }
