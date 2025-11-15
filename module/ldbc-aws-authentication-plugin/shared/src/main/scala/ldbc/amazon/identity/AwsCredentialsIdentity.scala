package ldbc.amazon.identity

import ldbc.amazon.identity.internal.DefaultAwsCredentialsIdentity

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
