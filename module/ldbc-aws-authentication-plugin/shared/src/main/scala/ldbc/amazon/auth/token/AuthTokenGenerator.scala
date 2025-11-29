package ldbc.amazon.auth.token

import ldbc.amazon.identity.AwsCredentials

/**
 * A trait for generating authentication tokens using AWS credentials.
 * 
 * This trait defines the contract for creating authentication tokens that can be used
 * for various AWS services that support IAM-based authentication. The generated tokens
 * provide temporary access based on IAM credentials and policies, eliminating the need
 * to store long-term credentials in application code.
 * 
 * @tparam F The effect type that wraps the token generation operations
 */
trait AuthTokenGenerator[F[_]]:

  /**
   * Generates an authentication token using the provided AWS credentials.
   * 
   * The generated token provides temporary access to AWS services that support
   * IAM-based authentication. The token is typically valid for a limited time
   * period and provides access based on the IAM permissions associated with
   * the provided credentials.
   * 
   * @param credentials The AWS credentials containing access key ID, secret access key,
   *                   and optional session token for temporary credentials
   * @return The authentication token wrapped in the effect type F, ready to be used
   *         for service authentication
   */
  def generateToken(credentials: AwsCredentials): F[String]
