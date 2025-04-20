/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scodec.bits.ByteVector

import cats.effect.*

import fs2.io.file.*
import fs2.io.net.tls.{ CertChainAndKey, S2nConfig }

class TLSConnectionTest extends FTestPlatform:

  test("Verify that you can connect to MySQL with a TLS connection") {
    assertIO(
      (for
        cert <- Resource.eval(Files[IO].readAll(Path("database/ssl/ca.pem")).compile.to(ByteVector))
        cfg <- S2nConfig.builder
                 .withCertChainAndKeysToStore(List(CertChainAndKey(cert, ByteVector.empty)))
                 .withPemsToTrustStore(List(cert.decodeAscii.toOption.get))
                 .build[IO]
        connection <- ConnectionProvider
                        .default[IO]("127.0.0.1", 13306, "ldbc_ssl_user", "securepassword", "world")
                        .setSSL(SSL.fromS2nConfig(cfg))
                        .createConnection()
      yield connection).use { conn =>
        for
          statement <- conn.createStatement()
          result    <- statement.executeQuery("SELECT 1")
        yield result.getInt(1)
      },
      1
    )
  }
