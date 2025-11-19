package ldbc.amazon.identity

import ldbc.amazon.exception.SdkClientException

trait AwsCredentialsProvider:

  /**
   * Returns [[AwsCredentials]] that can be used to authorize an AWS request. Each implementation of AWSCredentialsProvider
   * can choose its own strategy for loading credentials. For example, an implementation might load credentials from an existing
   * key management system, or load new credentials when credentials are rotated.
   *
   * <p>If an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be
   * raised.</p>
   *
   * @return AwsCredentials which the caller can use to authorize an AWS request.
   */
  def resolveCredentials(): Either[SdkClientException, AwsCredentials]
