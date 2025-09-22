/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import fs2.io.file.*
import fs2.io.net.tls.*
import fs2.text

class TLSConnectionTest extends FTestPlatform:

  test("Verify that you can connect to MySQL with a TLS connection") {
    assertIO(
      (for
        ca  <- Resource.eval(Files[IO].readAll(Path("database/ssl/ca.pem")).through(text.utf8.decode).compile.string)
        cfg <- S2nConfig.builder
                 .withPemsToTrustStore(List(ca))
                 .build[IO]
        connection <- MySQLDataSource
                        .build[IO]("127.0.0.1", 13306, "ldbc_ssl_user")
                        .setPassword("securepassword")
                        .setDatabase("world")
                        .setSSL(
                          SSL
                            .fromS2nConfig(cfg)
                            .withTLSParameters(
                              TLSParameters(
                                serverName = Some("MySQL_Server")
                              )
                            )
                        )
                        .getConnection
      yield connection).use { conn =>
        for
          statement <- conn.createStatement()
          result    <- statement.executeQuery("SELECT 1")
          value     <- result.getInt(1)
        yield value
      },
      1
    )
  }
