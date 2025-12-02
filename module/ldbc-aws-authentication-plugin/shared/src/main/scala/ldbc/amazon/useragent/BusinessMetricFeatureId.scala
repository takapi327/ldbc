/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.useragent

/**
 * An enum class representing a short form of identity providers to record in the UA string.
 *
 * Unimplemented metrics: I,K
 * Unsupported metrics (these will never be added): A,H
 */
enum BusinessMetricFeatureId(val code: String):
  case WAITER                                extends BusinessMetricFeatureId("B")
  case PAGINATOR                             extends BusinessMetricFeatureId("C")
  case RETRY_MODE_LEGACY                     extends BusinessMetricFeatureId("D")
  case RETRY_MODE_STANDARD                   extends BusinessMetricFeatureId("E")
  case RETRY_MODE_ADAPTIVE                   extends BusinessMetricFeatureId("F")
  case S3_TRANSFER                           extends BusinessMetricFeatureId("G")
  case GZIP_REQUEST_COMPRESSION              extends BusinessMetricFeatureId("L")
  case PROTOCOL_RPC_V2_CBOR                  extends BusinessMetricFeatureId("M")
  case ENDPOINT_OVERRIDE                     extends BusinessMetricFeatureId("N")
  case S3_EXPRESS_BUCKET                     extends BusinessMetricFeatureId("J")
  case ACCOUNT_ID_MODE_PREFERRED             extends BusinessMetricFeatureId("P")
  case ACCOUNT_ID_MODE_DISABLED              extends BusinessMetricFeatureId("Q")
  case ACCOUNT_ID_MODE_REQUIRED              extends BusinessMetricFeatureId("R")
  case SIGV4A_SIGNING                        extends BusinessMetricFeatureId("S")
  case RESOLVED_ACCOUNT_ID                   extends BusinessMetricFeatureId("T")
  case FLEXIBLE_CHECKSUMS_REQ_CRC32          extends BusinessMetricFeatureId("U")
  case FLEXIBLE_CHECKSUMS_REQ_CRC32C         extends BusinessMetricFeatureId("V")
  case FLEXIBLE_CHECKSUMS_REQ_CRC64          extends BusinessMetricFeatureId("W")
  case FLEXIBLE_CHECKSUMS_REQ_SHA1           extends BusinessMetricFeatureId("X")
  case FLEXIBLE_CHECKSUMS_REQ_SHA256         extends BusinessMetricFeatureId("Y")
  case FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED extends BusinessMetricFeatureId("Z")
  case FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED  extends BusinessMetricFeatureId("a")
  case FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED extends BusinessMetricFeatureId("b")
  case FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED  extends BusinessMetricFeatureId("c")
  case DDB_MAPPER                            extends BusinessMetricFeatureId("d")
  case BEARER_SERVICE_ENV_VARS               extends BusinessMetricFeatureId("3")
  case CREDENTIALS_CODE                      extends BusinessMetricFeatureId("e")
  case CREDENTIALS_JVM_SYSTEM_PROPERTIES     extends BusinessMetricFeatureId("f")
  case CREDENTIALS_ENV_VARS                  extends BusinessMetricFeatureId("g")
  case CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN extends BusinessMetricFeatureId("h")
  case CREDENTIALS_STS_ASSUME_ROLE           extends BusinessMetricFeatureId("i")
  case CREDENTIALS_STS_ASSUME_ROLE_SAML      extends BusinessMetricFeatureId("j")
  case CREDENTIALS_STS_ASSUME_ROLE_WEB_ID    extends BusinessMetricFeatureId("k")
  case CREDENTIALS_STS_FEDERATION_TOKEN      extends BusinessMetricFeatureId("l")
  case CREDENTIALS_STS_SESSION_TOKEN         extends BusinessMetricFeatureId("m")
  case CREDENTIALS_PROFILE                   extends BusinessMetricFeatureId("n")
  case CREDENTIALS_PROFILE_SOURCE_PROFILE    extends BusinessMetricFeatureId("o")
  case CREDENTIALS_PROFILE_NAMED_PROVIDER    extends BusinessMetricFeatureId("p")
  case CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN  extends BusinessMetricFeatureId("q")
  case CREDENTIALS_PROFILE_SSO               extends BusinessMetricFeatureId("r")
  case CREDENTIALS_SSO                       extends BusinessMetricFeatureId("s")
  case CREDENTIALS_PROFILE_SSO_LEGACY        extends BusinessMetricFeatureId("t")
  case CREDENTIALS_SSO_LEGACY                extends BusinessMetricFeatureId("u")
  case CREDENTIALS_PROFILE_PROCESS           extends BusinessMetricFeatureId("v")
  case CREDENTIALS_PROCESS                   extends BusinessMetricFeatureId("w")
  case CREDENTIALS_HTTP                      extends BusinessMetricFeatureId("z")
  case CREDENTIALS_IMDS                      extends BusinessMetricFeatureId("0")
  case CREDENTIALS_WEB_IDENTITY_TOKEN        extends BusinessMetricFeatureId("k")
  case UNKNOWN                               extends BusinessMetricFeatureId("Unknown")
