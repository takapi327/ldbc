/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.useragent

/**
 * Enumeration of business metric feature identifiers for AWS SDK user agent tracking.
 * 
 * This enum represents a comprehensive set of short-form codes used to identify specific
 * features, capabilities, and configurations in the AWS SDK user agent string. These
 * metrics help AWS understand which features are being used and how the SDK is configured,
 * enabling better service optimization and support.
 * 
 * The feature identifiers cover several categories:
 *  - SDK features (waiters, paginators, transfer utilities)
 *  - Retry modes (legacy, standard, adaptive)
 *  - Compression and optimization features
 *  - Authentication and credential providers
 *  - Endpoint and account ID handling modes
 *  - Checksum validation methods
 *  - Protocol-specific features
 * 
 * Each feature is assigned a unique single-character or short code that is embedded
 * in the user agent string sent with AWS API requests.
 * 
 * @param code the short identifier code used in the user agent string
 * 
 * @example {{{
 *   // Getting the code for a specific feature
 *   val retryCode = BusinessMetricFeatureId.RETRY_MODE_STANDARD.code  // Returns "E"
 *   
 *   // Using in user agent construction
 *   val features = List(
 *     BusinessMetricFeatureId.PAGINATOR,
 *     BusinessMetricFeatureId.GZIP_REQUEST_COMPRESSION
 *   )
 *   val codes = features.map(_.code).mkString(",")  // "C,L"
 * }}}
 * 
 * @note Unimplemented metrics: I, K
 * @note Unsupported metrics (will never be added): A, H
 * 
 * @see [[ldbc.amazon.client.HttpClient]] for HTTP client implementations that may use these metrics
 */
enum BusinessMetricFeatureId(val code: String):
  /** Indicates usage of SDK waiter functionality for polling operations. */
  case WAITER extends BusinessMetricFeatureId("B")

  /** Indicates usage of SDK paginator functionality for handling paginated API responses. */
  case PAGINATOR extends BusinessMetricFeatureId("C")

  /** Indicates usage of legacy retry mode with basic exponential backoff. */
  case RETRY_MODE_LEGACY extends BusinessMetricFeatureId("D")

  /** Indicates usage of standard retry mode with improved backoff strategies. */
  case RETRY_MODE_STANDARD extends BusinessMetricFeatureId("E")

  /** Indicates usage of adaptive retry mode with dynamic rate adjustment. */
  case RETRY_MODE_ADAPTIVE extends BusinessMetricFeatureId("F")

  /** Indicates usage of S3 transfer manager for optimized file uploads/downloads. */
  case S3_TRANSFER extends BusinessMetricFeatureId("G")

  /** Indicates usage of GZIP compression for request bodies. */
  case GZIP_REQUEST_COMPRESSION extends BusinessMetricFeatureId("L")

  /** Indicates usage of RPC v2 protocol with CBOR encoding. */
  case PROTOCOL_RPC_V2_CBOR extends BusinessMetricFeatureId("M")

  /** Indicates usage of custom endpoint override configuration. */
  case ENDPOINT_OVERRIDE extends BusinessMetricFeatureId("N")

  /** Indicates usage of S3 Express One Zone bucket features. */
  case S3_EXPRESS_BUCKET extends BusinessMetricFeatureId("J")

  /** Indicates account ID endpoint mode is set to preferred. */
  case ACCOUNT_ID_MODE_PREFERRED extends BusinessMetricFeatureId("P")

  /** Indicates account ID endpoint mode is disabled. */
  case ACCOUNT_ID_MODE_DISABLED extends BusinessMetricFeatureId("Q")

  /** Indicates account ID endpoint mode is required. */
  case ACCOUNT_ID_MODE_REQUIRED extends BusinessMetricFeatureId("R")

  /** Indicates usage of Signature Version 4A (SigV4A) signing for multi-region requests. */
  case SIGV4A_SIGNING extends BusinessMetricFeatureId("S")

  /** Indicates that account ID has been resolved for the request. */
  case RESOLVED_ACCOUNT_ID extends BusinessMetricFeatureId("T")

  /** Indicates usage of CRC32 checksum for request validation. */
  case FLEXIBLE_CHECKSUMS_REQ_CRC32 extends BusinessMetricFeatureId("U")

  /** Indicates usage of CRC32C checksum for request validation. */
  case FLEXIBLE_CHECKSUMS_REQ_CRC32C extends BusinessMetricFeatureId("V")

  /** Indicates usage of CRC64 checksum for request validation. */
  case FLEXIBLE_CHECKSUMS_REQ_CRC64 extends BusinessMetricFeatureId("W")

  /** Indicates usage of SHA1 checksum for request validation. */
  case FLEXIBLE_CHECKSUMS_REQ_SHA1 extends BusinessMetricFeatureId("X")

  /** Indicates usage of SHA256 checksum for request validation. */
  case FLEXIBLE_CHECKSUMS_REQ_SHA256 extends BusinessMetricFeatureId("Y")

  /** Indicates flexible request checksums are calculated when supported. */
  case FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED extends BusinessMetricFeatureId("Z")

  /** Indicates flexible request checksums are calculated when required. */
  case FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED extends BusinessMetricFeatureId("a")

  /** Indicates flexible response checksums are validated when supported. */
  case FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED extends BusinessMetricFeatureId("b")

  /** Indicates flexible response checksums are validated when required. */
  case FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED extends BusinessMetricFeatureId("c")

  /** Indicates usage of DynamoDB Object Mapper functionality. */
  case DDB_MAPPER extends BusinessMetricFeatureId("d")

  /** Indicates bearer token credentials loaded from environment variables. */
  case BEARER_SERVICE_ENV_VARS extends BusinessMetricFeatureId("3")

  /** Indicates credentials provided directly in application code. */
  case CREDENTIALS_CODE extends BusinessMetricFeatureId("e")

  /** Indicates credentials loaded from JVM system properties. */
  case CREDENTIALS_JVM_SYSTEM_PROPERTIES extends BusinessMetricFeatureId("f")

  /** Indicates credentials loaded from environment variables. */
  case CREDENTIALS_ENV_VARS extends BusinessMetricFeatureId("g")

  /** Indicates credentials loaded from environment variables with STS web identity token. */
  case CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN extends BusinessMetricFeatureId("h")

  /** Indicates credentials obtained via STS AssumeRole operation. */
  case CREDENTIALS_STS_ASSUME_ROLE extends BusinessMetricFeatureId("i")

  /** Indicates credentials obtained via STS AssumeRoleWithSAML operation. */
  case CREDENTIALS_STS_ASSUME_ROLE_SAML extends BusinessMetricFeatureId("j")

  /** Indicates credentials obtained via STS AssumeRoleWithWebIdentity operation. */
  case CREDENTIALS_STS_ASSUME_ROLE_WEB_ID extends BusinessMetricFeatureId("k")

  /** Indicates credentials obtained via STS GetFederationToken operation. */
  case CREDENTIALS_STS_FEDERATION_TOKEN extends BusinessMetricFeatureId("l")

  /** Indicates credentials obtained via STS GetSessionToken operation. */
  case CREDENTIALS_STS_SESSION_TOKEN extends BusinessMetricFeatureId("m")

  /** Indicates credentials loaded from AWS profile configuration. */
  case CREDENTIALS_PROFILE extends BusinessMetricFeatureId("n")

  /** Indicates credentials loaded from AWS profile with source profile configuration. */
  case CREDENTIALS_PROFILE_SOURCE_PROFILE extends BusinessMetricFeatureId("o")

  /** Indicates credentials loaded from AWS profile with named credential provider. */
  case CREDENTIALS_PROFILE_NAMED_PROVIDER extends BusinessMetricFeatureId("p")

  /** Indicates credentials loaded from AWS profile with STS web identity token. */
  case CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN extends BusinessMetricFeatureId("q")

  /** Indicates credentials loaded from AWS profile with SSO configuration. */
  case CREDENTIALS_PROFILE_SSO extends BusinessMetricFeatureId("r")

  /** Indicates credentials obtained via AWS SSO. */
  case CREDENTIALS_SSO extends BusinessMetricFeatureId("s")

  /** Indicates credentials loaded from AWS profile with legacy SSO configuration. */
  case CREDENTIALS_PROFILE_SSO_LEGACY extends BusinessMetricFeatureId("t")

  /** Indicates credentials obtained via legacy AWS SSO. */
  case CREDENTIALS_SSO_LEGACY extends BusinessMetricFeatureId("u")

  /** Indicates credentials loaded from AWS profile with credential process. */
  case CREDENTIALS_PROFILE_PROCESS extends BusinessMetricFeatureId("v")

  /** Indicates credentials obtained via credential process. */
  case CREDENTIALS_PROCESS extends BusinessMetricFeatureId("w")

  /** Indicates credentials obtained via HTTP credential provider. */
  case CREDENTIALS_HTTP extends BusinessMetricFeatureId("z")

  /** Indicates credentials obtained from EC2 Instance Metadata Service (IMDS). */
  case CREDENTIALS_IMDS extends BusinessMetricFeatureId("0")

  /** Indicates credentials obtained from container metadata service. */
  case CREDENTIALS_CONTAINER extends BusinessMetricFeatureId("1")

  /** Indicates an unknown or unrecognized business metric feature. */
  case UNKNOWN extends BusinessMetricFeatureId("Unknown")
