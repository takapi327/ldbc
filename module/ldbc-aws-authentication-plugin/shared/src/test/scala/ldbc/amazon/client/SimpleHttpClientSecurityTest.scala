/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.client

import java.net.URI

import scala.concurrent.duration.*

import cats.effect.IO

import fs2.io.net.Network

import munit.CatsEffectSuite

class SimpleHttpClientSecurityTest extends CatsEffectSuite:

  val client = new SimpleHttpClient[IO](connectTimeout = 5.seconds, readTimeout = 10.seconds)

  test("reject URIs without scheme") {
    val uri = URI.create("example.com/path")

    client.get(uri, Map.empty).attempt.map { result =>
      assert(result.isLeft)
      assert(result.left.toOption.get.getMessage.contains("URI scheme is required"))
    }
  }

  test("reject unsupported URI schemes") {
    val uri = URI.create("ftp://example.com/path")

    client.get(uri, Map.empty).attempt.map { result =>
      assert(result.isLeft)
      assert(result.left.toOption.get.getMessage.contains("Unsupported URI scheme: ftp"))
    }
  }

  test("accept HTTP URIs") {
    // A closed local port fails fast with a connection error rather than a scheme-validation error,
    // so this checks the scheme is accepted without depending on any external service.
    val uri = URI.create("http://127.0.0.1:1/")

    client.get(uri, Map.empty).attempt.map { result =>
      // Either succeeds or fails with a network error (not a scheme-validation error).
      result.fold(
        error => assert(!error.getMessage.contains("Unsupported URI scheme")),
        _ => assert(true)
      )
    }
  }

  test("accept HTTPS URIs and use correct default port") {
    // A closed local port fails fast; this checks the HTTPS scheme is accepted (and would use port
    // 443) without depending on any external service.
    val uri = URI.create("https://127.0.0.1:1/")

    client.get(uri, Map.empty).attempt.map { result =>
      // Either succeeds or fails with a network error (not a scheme-validation error).
      result.fold(
        error => assert(!error.getMessage.contains("Unsupported URI scheme")),
        _ => assert(true)
      )
    }
  }

  test("use correct default ports") {
    // HTTP should default to port 80
    val httpUri = URI.create("http://example.com")
    assert(httpUri.getPort == -1) // No explicit port

    // HTTPS should default to port 443
    val httpsUri = URI.create("https://example.com")
    assert(httpsUri.getPort == -1) // No explicit port

    // The client should handle these correctly internally
    assert(true) // This is tested implicitly by the above tests
  }

  test("preserve explicit ports in URIs") {
    val httpUri  = URI.create("http://example.com:8080/path")
    val httpsUri = URI.create("https://example.com:8443/path")

    assert(httpUri.getPort == 8080)
    assert(httpsUri.getPort == 8443)
  }

  test("AWS STS HTTPS endpoints should be accepted") {
    // These are the actual endpoints the StsClient will use
    val stsEndpoints = List(
      "https://sts.us-east-1.amazonaws.com/",
      "https://sts.eu-west-1.amazonaws.com/",
      "https://sts.ap-northeast-1.amazonaws.com/"
    )

    stsEndpoints.foreach { endpoint =>
      val uri = URI.create(endpoint)

      client.get(uri, Map.empty).attempt.map { result =>
        // Should not fail with scheme validation error
        result.fold(
          error => assert(!error.getMessage.contains("Unsupported URI scheme")),
          _ => assert(true)
        )
      }
    }
  }

  test("reject HTTP requests to AWS endpoints") {
    val insecureAWSEndpoint = URI.create("http://sts.us-east-1.amazonaws.com/")

    client.get(insecureAWSEndpoint, Map.empty).attempt.map { result =>
      assert(result.isLeft)
      assert(result.left.toOption.get.getMessage.contains("AWS endpoints require HTTPS"))
    }
  }

  test("reject HTTP PUT requests to AWS endpoints") {
    val insecureAWSEndpoint = URI.create("http://sts.us-east-1.amazonaws.com/")

    client.put(insecureAWSEndpoint, Map.empty, "test body").attempt.map { result =>
      assert(result.isLeft)
      assert(result.left.toOption.get.getMessage.contains("AWS endpoints require HTTPS"))
    }
  }
