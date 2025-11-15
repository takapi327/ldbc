package ldbc.amazon.identity

import ldbc.amazon.identity.internal.DefaultAwsCredentialsIdentity

/**
 * Provides access to the AWS credentials used for accessing services: AWS access key ID and secret access key. These
 * credentials are used to securely sign requests to services (e.g., AWS services) that use them for authentication.
 *
 * <p>For more details on AWS access keys, see:
 * <a href="https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys">
 * https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys</a></p>
 */
trait AwsCredentialsIdentity extends Identity:

  /**
   * Retrieve the AWS access key, used to identify the user interacting with services.
   */
  def accessKeyId: String

  /**
   * Retrieve the AWS secret access key, used to authenticate the user interacting with services.
   */
  def secretAccessKey: String

  /**
   * Retrieve the AWS account id associated with this credentials identity, if found.
   */
  def accountId: Option[String]

object AwsCredentialsIdentity:

  /**
   * Constructs a new credentials object, with the specified AWS access key and AWS secret key.
   *
   * @param accessKeyId     The AWS access key, used to identify the user interacting with services.
   * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with services.
   */
  def create(accessKeyId: String, secretAccessKey: String): AwsCredentialsIdentity = DefaultAwsCredentialsIdentity(
    accessKeyId = accessKeyId,
    secretAccessKey = secretAccessKey,
    accountId = None,
    expirationTime = None,
    providerName = None
  )
