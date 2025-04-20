/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.syntax.all.*

import cats.effect.*

import fs2.io.file.*
import fs2.io.net.tls.SecureContext
import fs2.text

class TLSConnectionTest extends FTestPlatform:

  test("Verify that you can connect to MySQL with a TLS connection") {
    assertIO(
      for
        ca   <- Files[IO].readAll(Path("database/ssl/ca.pem")).through(text.utf8.decode).compile.string
        cert <- Files[IO].readAll(Path("database/ssl/client-cert.pem")).through(text.utf8.decode).compile.string
        key  <- Files[IO].readAll(Path("database/ssl/client-key.pem")).through(text.utf8.decode).compile.string
        secureContext = SecureContext(
                          ca   = List(ca.asRight).some,
                          cert = List(cert.asRight).some,
                          key  = List(SecureContext.Key(key.asRight, None)).some
                        )
        result <- ConnectionProvider
                    .default[IO](
                      "127.0.0.1",
                      13306,
                      "ldbc_ssl_user",
                      "securepassword",
                      "world"
                    )
                    .setSSL(SSL.fromSecureContext(secureContext))
                    .use { conn =>
                      for
                        statement <- conn.createStatement()
                        result    <- statement.executeQuery("SELECT 1")
                      yield result.getInt(1)
                    }
      yield result,
      1
    )
  }
