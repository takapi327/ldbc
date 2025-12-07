/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.client

import java.net.URI

/**
 * Abstract HTTP client interface for making HTTP requests in the AWS authentication plugin.
 * 
 * This trait defines a generic HTTP client abstraction that can be implemented for different
 * effect types (F[_]) and HTTP libraries. It provides the core HTTP operations needed for
 * AWS API communication including GET, PUT, and POST requests.
 * 
 * The client is designed to work with functional effect systems like cats-effect, allowing
 * for composable and type-safe HTTP operations. Each method returns the HTTP response
 * wrapped in the effect type F[_].
 * 
 * Implementations of this trait should handle:
 *  - HTTP connection management and pooling
 *  - Request/response serialization
 *  - Error handling and retries
 *  - SSL/TLS configuration for HTTPS
 *  - Timeout configuration
 * 
 * @tparam F the effect type that wraps the HTTP response (e.g., IO, Future, Task)
 * 
 * @example {{{
 *   import cats.effect.IO
 *   import java.net.URI
 *   
 *   // Usage with cats-effect IO
 *   def makeRequest(client: HttpClient[IO]): IO[HttpResponse] = {
 *     val uri = URI.create("https://api.amazonaws.com/endpoint")
 *     val headers = Map("Authorization" -> "Bearer token")
 *     
 *     for {
 *       response <- client.get(uri, headers)
 *     } yield response
 *   }
 * }}}
 * 
 * @see [[HttpResponse]] for the response type returned by HTTP operations
 * @see [[ldbc.amazon.useragent.BusinessMetricFeatureId]] for user agent metrics tracking
 */
trait HttpClient[F[_]]:

  /**
   * Performs an HTTP GET request to the specified URI.
   * 
   * This method executes a GET request with the provided headers and returns
   * the HTTP response wrapped in the effect type F[_]. GET requests should be
   * idempotent and safe, typically used for retrieving data without side effects.
   * 
   * @param uri the target URI for the GET request, must be a valid HTTP/HTTPS URI
   * @param headers a map of HTTP headers to include in the request
   * @return an effect containing the HTTP response
   * 
   * @example {{{
   *   val uri = URI.create("https://sts.amazonaws.com/")
   *   val headers = Map(
   *     "Accept" -> "application/json",
   *     "User-Agent" -> "ldbc-aws-plugin/1.0"
   *   )
   *   client.get(uri, headers)
   * }}}
   */
  def get(uri: URI, headers: Map[String, String]): F[HttpResponse]

  /**
   * Performs an HTTP PUT request to the specified URI with a request body.
   * 
   * This method executes a PUT request with the provided headers and body content.
   * PUT requests are typically used for creating or updating resources and should
   * be idempotent.
   * 
   * @param uri the target URI for the PUT request, must be a valid HTTP/HTTPS URI
   * @param headers a map of HTTP headers to include in the request
   * @param body the request body content as a string
   * @return an effect containing the HTTP response
   * 
   * @example {{{
   *   val uri = URI.create("https://api.amazonaws.com/resource/123")
   *   val headers = Map(
   *     "Content-Type" -> "application/json",
   *     "Authorization" -> "AWS4-HMAC-SHA256 ..."
   *   )
   *   val body = """{"key": "value"}"""
   *   client.put(uri, headers, body)
   * }}}
   */
  def put(uri: URI, headers: Map[String, String], body: String): F[HttpResponse]

  /**
   * Performs an HTTP POST request to the specified URI with a request body.
   * 
   * This method executes a POST request with the provided headers and body content.
   * POST requests are typically used for creating new resources or submitting data
   * and may have side effects.
   * 
   * @param uri the target URI for the POST request, must be a valid HTTP/HTTPS URI
   * @param headers a map of HTTP headers to include in the request
   * @param body the request body content as a string
   * @return an effect containing the HTTP response
   * 
   * @example {{{
   *   val uri = URI.create("https://sts.amazonaws.com/")
   *   val headers = Map(
   *     "Content-Type" -> "application/x-amz-json-1.1",
   *     "X-Amz-Target" -> "AWSSecurityTokenServiceV20110615.AssumeRole"
   *   )
   *   val body = """{"RoleArn": "arn:aws:iam::123456789012:role/MyRole"}"""
   *   client.post(uri, headers, body)
   * }}}
   */
  def post(uri: URI, headers: Map[String, String], body: String): F[HttpResponse]
