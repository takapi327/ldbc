/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.client

/**
 * Represents an HTTP response from AWS API calls or other HTTP operations.
 * 
 * This case class encapsulates the essential components of an HTTP response including
 * the status code, response headers, and response body. It is designed to be immutable
 * and type-safe, providing a clean abstraction over HTTP responses in the AWS
 * authentication plugin.
 * 
 * The response is typically created by HTTP client implementations and consumed by
 * AWS service clients for processing API responses, credential retrieval, and
 * authentication operations.
 * 
 * @param statusCode the HTTP status code indicating the result of the request (e.g., 200, 404, 500)
 * @param headers a map of HTTP response headers with case-insensitive keys
 * @param body the response body content as a string, may be empty for certain responses
 * 
 * @example {{{
 *   // Creating a successful response
 *   val response = HttpResponse(
 *     statusCode = 200,
 *     headers = Map(
 *       "Content-Type" -> "application/json",
 *       "Content-Length" -> "123"
 *     ),
 *     body = """{"access_token": "abc123", "expires_in": 3600}"""
 *   )
 *   
 *   // Checking response status
 *   if (response.statusCode >= 200 && response.statusCode < 300) {
 *     // Process successful response
 *     println(s"Success: ${response.body}")
 *   }
 *   
 *   // Accessing specific headers
 *   response.headers.get("Content-Type") match {
 *     case Some("application/json") => // Parse JSON response
 *     case _ => // Handle other content types
 *   }
 * }}}
 * 
 * @note HTTP header names should be treated as case-insensitive according to RFC 7230.
 *       Implementations should normalize header names appropriately.
 * 
 * @see [[HttpClient]] for the interface that produces HttpResponse instances
 * @see [[ldbc.amazon.useragent.BusinessMetricFeatureId]] for tracking HTTP client metrics
 */
final case class HttpResponse(
  statusCode: Int,
  headers:    Map[String, String],
  body:       String
)
