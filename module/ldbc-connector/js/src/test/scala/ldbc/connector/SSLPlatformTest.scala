/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import fs2.io.net.Network
import fs2.io.net.tls.{ SecureContext, TLSParameters }

class SSLPlatformTest extends FTestPlatform:

  test("SSL.fromSecureContext with empty secure context") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
    val result = ssl.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }

  test("SSL.fromSecureContext should return SSL instance") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
    assert(ssl.isInstanceOf[SSL])
  }

  test("SSL.fromSecureContext with TLS parameters") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
    val sslWithParams = ssl.withTLSParameters(TLSParameters.Default)
    assertEquals(sslWithParams.tlsParameters, TLSParameters.Default)
    val result = sslWithParams.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }

  test("SSL.fromSecureContext with fallback") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
    val sslWithFallback = ssl.withFallback(true)
    assertEquals(sslWithFallback.fallbackOk, true)
    val result = sslWithFallback.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }

  test("SSL.fromSecureContext default fallback should be false") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
    assertEquals(ssl.fallbackOk, false)
  }

  test("SSL.fromSecureContext toSSLNegotiationOptions") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
    val logger: String => IO[Unit] = msg => IO.unit
    
    ssl.toSSLNegotiationOptions[IO](Some(logger))(Network[IO], implicitly).use { options =>
      IO {
        assert(options.isDefined)
        options.foreach { opt =>
          assertEquals(opt.tlsParameters, ssl.tlsParameters)
          assertEquals(opt.fallbackOk, false)
        }
      }
    }
  }

  test("SSL.fromSecureContext toSSLNegotiationOptions without logger") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
    
    ssl.toSSLNegotiationOptions[IO](None)(Network[IO], implicitly).use { options =>
      IO {
        assert(options.isDefined)
      }
    }
  }

  test("Multiple SSL instances from different SecureContexts") {
    val secureContext1 = SecureContext()
    val secureContext2 = SecureContext()
    
    val ssl1 = SSL.fromSecureContext(secureContext1)
    val ssl2 = SSL.fromSecureContext(secureContext2)
    
    val result1 = ssl1.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(1))
    val result2 = ssl2.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(2))
    
    for {
      _ <- assertIO(result1, 1)
      _ <- assertIO(result2, 2)
    } yield ()
  }

  test("SSL.fromSecureContext with modified parameters chain") {
    val secureContext = SecureContext()
    val ssl = SSL.fromSecureContext(secureContext)
      .withTLSParameters(TLSParameters.Default)
      .withFallback(true)
    
    assertEquals(ssl.tlsParameters, TLSParameters.Default)
    assertEquals(ssl.fallbackOk, true)
    
    val result = ssl.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure("success"))
    assertIO(result, "success")
  }

  test("SSL.fromSecureContext should preserve original after modification") {
    val secureContext = SecureContext()
    val original = SSL.fromSecureContext(secureContext)
    val modified = original.withFallback(true)
    
    assertEquals(original.fallbackOk, false)
    assertEquals(modified.fallbackOk, true)
  }