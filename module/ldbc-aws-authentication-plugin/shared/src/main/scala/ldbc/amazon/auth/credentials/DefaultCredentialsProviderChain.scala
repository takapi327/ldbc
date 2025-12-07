/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import scala.concurrent.duration.*

import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.{ Env, SystemProperties, UUIDGen }

import fs2.io.file.Files
import fs2.io.net.*

import ldbc.amazon.client.*
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*

/**
 * Default AWS credentials provider chain that matches AWS SDK v2 behavior.
 * 
 * The provider chain attempts to resolve credentials from the following sources in order:
 * 1. Java system properties (aws.accessKeyId and aws.secretAccessKey)
 * 2. Environment variables (AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY)
 * 3. Web identity token file (AWS_WEB_IDENTITY_TOKEN_FILE and AWS_ROLE_ARN)
 * 4. AWS credentials profile files (~/.aws/credentials)
 * 5. Container credentials provider (ECS/EKS)
 * 6. Instance profile credentials provider (EC2)
 * 
 * The first provider in the chain that successfully provides credentials will be used,
 * and the search will stop there. If no provider can provide credentials, an exception is thrown.
 * 
 * Usage example:
 * ```scala
 * import cats.effect.IO
 * 
 * val credentialsProvider = DefaultCredentialsProviderChain[IO]()
 * val credentials: IO[AwsCredentials] = credentialsProvider.resolveCredentials()
 * ```
 * 
 * @tparam F The effect type
 */
class DefaultCredentialsProviderChain[F[_]: Files: Env: SystemProperties: UUIDGen: Concurrent](
  httpClient: HttpClient[F],
  region:     String
) extends AwsCredentialsProvider[F]:

  /**
   * Lazily initialized list of AWS credential providers in order of precedence.
   * 
   * This method creates the standard AWS SDK credential provider chain that matches
   * the behavior of AWS SDK for Java v2. The providers are ordered by precedence,
   * with more specific/explicit credential sources taking priority over implicit ones.
   * 
   * Provider chain order:
   * 1. SystemPropertyCredentialsProvider - Java system properties (aws.accessKeyId, aws.secretAccessKey)
   * 2. EnvironmentVariableCredentialsProvider - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
   * 3. WebIdentityTokenFileCredentialsProvider - OIDC token authentication (Kubernetes IRSA)
   * 4. ProfileCredentialsProvider - AWS credentials file (~/.aws/credentials)
   * 5. ContainerCredentialsProvider - ECS task roles and EKS pod identity
   * 6. InstanceProfileCredentialsProvider - EC2 instance profile (IMDS)
   * 
   * Each provider is only consulted if the previous providers fail to provide credentials.
   * This ordering ensures that explicit credentials (environment variables, system properties)
   * take precedence over implicit credentials (instance profiles, container roles).
   * 
   * Performance considerations:
   * - ProfileCredentialsProvider and InstanceProfileCredentialsProvider require initialization
   * - The list is lazily computed to avoid unnecessary initialization overhead
   * - Failed providers are cached to avoid repeated initialization attempts
   * 
   * @return F[List[AwsCredentialsProvider[F]]] The ordered list of credential providers
   */
  private lazy val providers: F[List[AwsCredentialsProvider[F]]] =
    for
      profileProvider                    <- ProfileCredentialsProvider.default[F]()
      instanceProfileCredentialsProvider <- InstanceProfileCredentialsProvider.create[F](httpClient)
    yield List(
      new SystemPropertyCredentialsProvider[F](),
      new EnvironmentVariableCredentialsProvider[F](),
      WebIdentityTokenFileCredentialsProvider.default[F](httpClient, region),
      profileProvider,
      ContainerCredentialsProvider.create[F](httpClient),
      instanceProfileCredentialsProvider
    )

  /**
   * Resolves AWS credentials by trying providers in the default chain order.
   * 
   * This method implements the AWS SDK v2 default credential provider chain behavior,
   * attempting to resolve credentials from each provider in sequence until one succeeds
   * or all providers are exhausted.
   * 
   * Resolution process:
   * 1. Initialize the provider list (lazy evaluation)
   * 2. Try each provider in order, starting with highest precedence
   * 3. Return credentials from the first successful provider
   * 4. If all providers fail, throw an exception with aggregated error messages
   * 
   * The first successful provider "wins" and the chain stops there. This behavior
   * ensures predictable credential resolution and prevents unexpected credential
   * source changes during application runtime.
   * 
   * Common resolution scenarios:
   * - Development: Environment variables or system properties
   * - CI/CD: Web Identity tokens or environment variables
   * - ECS: Container credentials provider
   * - EKS: Web Identity tokens (IRSA) or container credentials
   * - EC2: Instance profile credentials
   * - Local AWS CLI: Profile credentials from ~/.aws/credentials
   * 
   * @return F[AwsCredentials] The resolved AWS credentials from the first successful provider
   * @throws SdkClientException if no provider in the chain can provide valid credentials
   */
  override def resolveCredentials(): F[AwsCredentials] =
    for
      providerList <- providers
      credentials  <- tryProvidersInOrder(providerList, Nil)
    yield credentials

  /**
   * Attempts to resolve credentials from providers in order, handling failures gracefully.
   * 
   * This method implements the recursive credential resolution logic for the provider chain.
   * It tries each provider sequentially and accumulates error messages for debugging purposes.
   * 
   * Error handling strategy:
   * - Each provider failure is caught and logged for debugging
   * - Failures are expected and normal behavior (e.g., no ~/.aws/credentials file)
   * - Only when all providers fail is an exception raised
   * - Error messages from all providers are included in the final exception
   * 
   * The method maintains a list of error messages to provide comprehensive debugging
   * information when credential resolution completely fails. This helps developers
   * understand why credential resolution failed across all providers.
   * 
   * Example error scenarios:
   * - SystemPropertyCredentialsProvider: "aws.accessKeyId system property not set"
   * - EnvironmentVariableCredentialsProvider: "AWS_ACCESS_KEY_ID environment variable not set"
   * - ProfileCredentialsProvider: "Unable to load credentials from profile 'default'"
   * - InstanceProfileCredentialsProvider: "Unable to retrieve credentials from IMDS"
   * 
   * @param providers Remaining providers to try in the chain
   * @param exceptions Accumulated error messages from failed providers
   * @return F[AwsCredentials] The credentials from the first successful provider
   * @throws SdkClientException if all providers in the list fail to provide credentials
   */
  private def tryProvidersInOrder(
    providers:  List[AwsCredentialsProvider[F]],
    exceptions: List[String]
  ): F[AwsCredentials] =
    providers match
      case Nil =>
        Concurrent[F].raiseError(
          new SdkClientException(
            s"Unable to load AWS credentials from any provider in the chain: ${ exceptions.mkString(", ") }"
          )
        )

      case provider :: remainingProviders =>
        provider.resolveCredentials().recoverWith { ex =>
          val errorMsg = s"${ provider.getClass.getSimpleName }: ${ ex.getMessage }"
          tryProvidersInOrder(remainingProviders, exceptions :+ errorMsg)
        }

object DefaultCredentialsProviderChain:

  /**
 * Creates a new default credentials provider chain.
 * 
 * @tparam F The effect type
 * @return A new DefaultCredentialsProviderChain instance
 */
  def default[F[_]: Files: Env: SystemProperties: Network: UUIDGen: Async](
    region: String
  ): DefaultCredentialsProviderChain[F] =
    val httpClient = new SimpleHttpClient[F](
      connectTimeout = 1.second,
      readTimeout    = 2.seconds
    )
    new DefaultCredentialsProviderChain[F](httpClient, region)
