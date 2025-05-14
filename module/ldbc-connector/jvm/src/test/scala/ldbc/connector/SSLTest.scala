/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import javax.net.ssl.SNIHostName

import cats.effect.IO

import fs2.io.net.tls.TLSParameters
import fs2.io.net.Network

class SSLTest extends FTestPlatform:

  test("SSL.None should not create a TLSContext") {
    assertIOBoolean(
      SSL.None.tlsContext[IO].attempt.use(result => IO(result.isLeft))
    )
  }

  test("SSL.Trusted should create an insecure TLSContext") {
    // This test verifies that no exception is thrown when creating a trusted context
    assertIOBoolean(
      SSL.Trusted.tlsContext[IO].use(_ => IO(true))
    )
  }

  test("SSL.System should create a system TLSContext") {
    // This test verifies that no exception is thrown when creating a system context
    assertIOBoolean(
      SSL.System.tlsContext[IO].use(_ => IO(true))
    )
  }

  test("withTLSParameters should override tlsParameters") {
    // TLSParameters.Defaultをベースに新しいパラメーターを作成
    val customParams = TLSParameters(
      algorithmConstraints                 = None,
      applicationProtocols                 = None,
      cipherSuites                         = TLSParameters.Default.cipherSuites,
      enableRetransmissions                = None,
      endpointIdentificationAlgorithm      = None,
      maximumPacketSize                    = None,
      protocols                            = TLSParameters.Default.protocols,
      serverNames                          = Some(List(new SNIHostName("test-server"))), // SNIHostNameを使用
      sniMatchers                          = None,
      useCipherSuitesOrder                 = false,
      needClientAuth                       = false,
      wantClientAuth                       = false,
      handshakeApplicationProtocolSelector = None
    )
    val ssl = SSL.Trusted.withTLSParameters(customParams)
    assertEquals(ssl.tlsParameters, customParams)
  }

  test("withFallback should override fallbackOk") {
    val ssl = SSL.Trusted.withFallback(true)
    assertEquals(ssl.fallbackOk, true)
  }

  test("toSSLNegotiationOptions should return None for SSL.None") {
    assertIO(
      SSL.None.toSSLNegotiationOptions[IO](None).use(IO.pure),
      None
    )
  }

  test("toSSLNegotiationOptions should return Some for SSL implementations") {
    assertIOBoolean(
      SSL.Trusted.toSSLNegotiationOptions[IO](None).use(IO.pure).map(_.isDefined)
    )
  }
