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

  override def resolveCredentials(): F[AwsCredentials] =
    for
      providerList <- providers
      credentials  <- tryProvidersInOrder(providerList, Nil)
    yield credentials

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
  def default[F[_]: Files: Env: SystemProperties: Network: UUIDGen: Async](region: String): DefaultCredentialsProviderChain[F] =
    val httpClient = new SimpleHttpClient[F](
      connectTimeout = 1.second,
      readTimeout    = 2.seconds
    )
    new DefaultCredentialsProviderChain[F](httpClient, region)
