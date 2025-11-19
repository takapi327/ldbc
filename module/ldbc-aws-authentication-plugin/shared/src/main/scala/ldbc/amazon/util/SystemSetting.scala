/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.util

trait SystemSetting

enum SdkSystemSetting(val systemProperty: String, val defaultValue: Option[String]):
  /**
   * Configure the AWS access key ID.
   *
   * This value will not be ignored if the [[AWS_SECRET_ACCESS_KEY]] is not specified.
   */
  case AWS_ACCESS_KEY_ID extends SdkSystemSetting("aws.accessKeyId", None)

  /**
   * Configure the AWS secret access key.
   *
   * This value will not be ignored if the [[AWS_ACCESS_KEY_ID]] is not specified.
   */
  case AWS_SECRET_ACCESS_KEY extends SdkSystemSetting("aws.secretAccessKey", None)

  /**
   * Configure the AWS session token.
   */
  case AWS_SESSION_TOKEN extends SdkSystemSetting("aws.sessionToken", None)

  /**
   * Configure the AWS account id associated with credentials supplied through system properties.
   */
  case AWS_ACCOUNT_ID extends SdkSystemSetting("aws.accountId", None)

  /**
   * Configure the AWS web identity token file path.
   */
  case AWS_WEB_IDENTITY_TOKEN_FILE extends SdkSystemSetting("aws.webIdentityTokenFile", None)

  /**
   * Configure the AWS role arn.
   */
  case AWS_ROLE_ARN extends SdkSystemSetting("aws.roleArn", None)

  /**
   * Configure the session name for a role.
   */
  case AWS_ROLE_SESSION_NAME extends SdkSystemSetting("aws.roleSessionName", None)

  /**
   * Configure the default region.
   */
  case AWS_REGION extends SdkSystemSetting("aws.region", None)

  /**
   * Whether to load information such as credentials, regions from EC2 Metadata instance service.
   */
  case AWS_EC2_METADATA_DISABLED extends SdkSystemSetting("aws.disableEc2Metadata", Some("false"))

  /**
   * Whether to disable fallback to insecure EC2 Metadata instance service v1 on errors or timeouts.
   */
  case AWS_EC2_METADATA_V1_DISABLED extends SdkSystemSetting("aws.disableEc2MetadataV1", None)

  /**
   * The EC2 instance metadata service endpoint.
   *
   * This allows a service running in EC2 to automatically load its credentials and region without needing to configure them
   * in the SdkClientBuilder.
   */
  case AWS_EC2_METADATA_SERVICE_ENDPOINT
    extends SdkSystemSetting("aws.ec2MetadataServiceEndpoint", Some("http://169.254.169.254"))

  case AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE
    extends SdkSystemSetting("aws.ec2MetadataServiceEndpointMode", Some("IPv4"))

  /**
   * The number of seconds (either as an integer or double) before a connection to the instance
   * metadata service should time out. This value is applied to both the socket connect and read timeouts.
   *
   * The timeout can be configured using the system property "aws.ec2MetadataServiceTimeout". If not set,
   * a default timeout is used. This setting is crucial for ensuring timely responses from the instance
   * metadata service in environments with varying network conditions.
   */
  case AWS_METADATA_SERVICE_TIMEOUT extends SdkSystemSetting("aws.ec2MetadataServiceTimeout", Some("1"))

  /**
   * The elastic container metadata service endpoint that should be called by the ContainerCredentialsProvider
   * when loading data from the container metadata service.
   *
   * This allows a service running in an elastic container to automatically load its credentials without needing to configure
   * them in the SdkClientBuilder.
   *
   * This is not used if the [[AWS_CONTAINER_CREDENTIALS_RELATIVE_URI]] is not specified.
   */
  case AWS_CONTAINER_SERVICE_ENDPOINT
    extends SdkSystemSetting("aws.containerServiceEndpoint", Some("http://169.254.170.2"))

  /**
   * The elastic container metadata service path that should be called by the ContainerCredentialsProvider when
   * loading credentials form the container metadata service. If this is not specified, credentials will not be automatically
   * loaded from the container metadata service.
   *
   * @see #AWS_CONTAINER_SERVICE_ENDPOINT
   */
  case AWS_CONTAINER_CREDENTIALS_RELATIVE_URI extends SdkSystemSetting("aws.containerCredentialsPath", None)

  /**
   * The full URI path to a localhost metadata service to be used.
   */
  case AWS_CONTAINER_CREDENTIALS_FULL_URI extends SdkSystemSetting("aws.containerCredentialsFullUri", None)

  /**
   * An authorization token to pass to a container metadata service, only used when [[AWS_CONTAINER_CREDENTIALS_FULL_URI]]
   * is specified.
   *
   * @see #AWS_CONTAINER_CREDENTIALS_FULL_URI
   */
  case AWS_CONTAINER_AUTHORIZATION_TOKEN extends SdkSystemSetting("aws.containerAuthorizationToken", None)

  /**
   * The absolute file path containing the authorization token in plain text to pass to a container metadata
   * service, only used when [[AWS_CONTAINER_CREDENTIALS_FULL_URI]] is specified.
   *
   * @see #AWS_CONTAINER_CREDENTIALS_FULL_URI
   */
  case AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE extends SdkSystemSetting("aws.containerAuthorizationTokenFile", None)

  /**
   * Explicitly identify the default synchronous HTTP implementation the SDK will use. Useful
   * when there are multiple implementations on the classpath or as a performance optimization
   * since implementation discovery requires classpath scanning.
   */
  case SYNC_HTTP_SERVICE_IMPL extends SdkSystemSetting("software.amazon.awssdk.http.service.impl", None)

  /**
   * Explicitly identify the default Async HTTP implementation the SDK will use. Useful
   * when there are multiple implementations on the classpath or as a performance optimization
   * since implementation discovery requires classpath scanning.
   */
  case ASYNC_HTTP_SERVICE_IMPL extends SdkSystemSetting("software.amazon.awssdk.http.async.service.impl", None)

  /**
   * Whether CBOR optimization should automatically be used if its support is found on the classpath and the service supports
   * CBOR-formatted JSON.
   */
  case CBOR_ENABLED extends SdkSystemSetting("aws.cborEnabled", Some("true"))

  /**
   * Whether binary ION representation optimization should automatically be used if the service supports ION.
   */
  case BINARY_ION_ENABLED extends SdkSystemSetting("aws.binaryIonEnabled", Some("true"))

  /**
   * The execution environment of the SDK user. This is automatically set in certain environments by the underlying AWS service.
   * For example, AWS Lambda will automatically specify a runtime indicating that the SDK is being used within Lambda.
   */
  case AWS_EXECUTION_ENV extends SdkSystemSetting("aws.executionEnvironment", None)

  /**
   * Whether endpoint discovery should be enabled.
   */
  case AWS_ENDPOINT_DISCOVERY_ENABLED extends SdkSystemSetting("aws.endpointDiscoveryEnabled", None)

  /**
   * Which [[RetryMode]] to use for the default [[RetryPolicy]], when one is not specified at the client level.
   */
  case AWS_RETRY_MODE extends SdkSystemSetting("aws.retryMode", None)

  /**
   * Which DefaultsMode to use, case insensitive
   */
  case AWS_DEFAULTS_MODE extends SdkSystemSetting("aws.defaultsMode", None)

  /**
   * Which AccountIdEndpointMode to use, case insensitive
   */
  case AWS_ACCOUNT_ID_ENDPOINT_MODE extends SdkSystemSetting("aws.accountIdEndpointMode", None)

  /**
   * Defines whether dualstack endpoints should be resolved during default endpoint resolution instead of non-dualstack
   * endpoints.
   */
  case AWS_USE_DUALSTACK_ENDPOINT extends SdkSystemSetting("aws.useDualstackEndpoint", None)

  /**
   * Defines whether fips endpoints should be resolved during default endpoint resolution instead of non-fips endpoints.
   */
  case AWS_USE_FIPS_ENDPOINT extends SdkSystemSetting("aws.useFipsEndpoint", None)

  /**
   * Whether request compression is disabled for operations marked with the RequestCompression trait. The default value is
   * false, i.e., request compression is enabled.
   */
  case AWS_DISABLE_REQUEST_COMPRESSION extends SdkSystemSetting("aws.disableRequestCompression", None)

  /**
   * Defines the minimum compression size in bytes, inclusive, for a request to be compressed. The default value is 10_240.
   * The value must be non-negative and no greater than 10_485_760.
   */
  case AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES extends SdkSystemSetting("aws.requestMinCompressionSizeBytes", None)

  /**
   * Defines a file path from which partition metadata should be loaded. If this isn't specified, the partition
   * metadata deployed with the SDK client will be used instead.
   */
  case AWS_PARTITIONS_FILE extends SdkSystemSetting("aws.partitionsFile", None)

  /**
   * The request checksum calculation setting. The default value is WHEN_SUPPORTED.
   */
  case AWS_REQUEST_CHECKSUM_CALCULATION extends SdkSystemSetting("aws.requestChecksumCalculation", None)

  /**
   * The response checksum validation setting. The default value is WHEN_SUPPORTED.
   */
  case AWS_RESPONSE_CHECKSUM_VALIDATION extends SdkSystemSetting("aws.responseChecksumValidation", None)

  /**
   * Configure an optional identification value to be appended to the user agent header.
   * The value should be less than 50 characters in length and is None by default.
   */
  case AWS_SDK_UA_APP_ID extends SdkSystemSetting("sdk.ua.appId", None)

  /**
   * Configure the SIGV4A signing region set.
   * This is a non-empty, comma-delimited list of AWS region names used during signing.
   */
  case AWS_SIGV4A_SIGNING_REGION_SET extends SdkSystemSetting("aws.sigv4a.signing.region.set", None)

  /**
   * Configure the preferred auth scheme to use.
   * This is a comma-delimited list of AWS auth scheme names used during signing.
   */
  case AWS_AUTH_SCHEME_PREFERENCE extends SdkSystemSetting("aws.authSchemePreference", None)
