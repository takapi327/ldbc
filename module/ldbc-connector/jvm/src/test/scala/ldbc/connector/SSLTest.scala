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

  test("Bug #718: withTLSParameters should preserve fallbackOk when chained after withFallback") {
    // withFallback(true).withTLSParameters(customParams) silently resets fallbackOk to false
    // because withTLSParameters creates a new anonymous SSL that only overrides tlsParameters
    // and falls through to the trait default (false) for fallbackOk instead of outer.fallbackOk.
    val customParams = TLSParameters(serverNames = Some(List(new SNIHostName("test-server"))))
    val ssl          = SSL.Trusted.withFallback(true).withTLSParameters(customParams)
    assertEquals(ssl.tlsParameters, customParams)
    assertEquals(ssl.fallbackOk, true, "fallbackOk should be preserved after chaining withTLSParameters")
  }

  test("Bug #718: withFallback should preserve tlsParameters when chained after withTLSParameters") {
    // withTLSParameters(customParams).withFallback(true) silently resets tlsParameters to Default
    // because withFallback creates a new anonymous SSL that only overrides fallbackOk
    // and falls through to the trait default (TLSParameters.Default) for tlsParameters.
    val customParams = TLSParameters(serverNames = Some(List(new SNIHostName("test-server"))))
    val ssl          = SSL.Trusted.withTLSParameters(customParams).withFallback(true)
    assertEquals(ssl.fallbackOk, true)
    assertEquals(ssl.tlsParameters, customParams, "tlsParameters should be preserved after chaining withFallback")
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
