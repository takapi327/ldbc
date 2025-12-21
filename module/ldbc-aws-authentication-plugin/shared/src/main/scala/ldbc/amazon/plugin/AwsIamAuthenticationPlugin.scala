/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.plugin

import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

import cats.syntax.all.*
import cats.Monad

import cats.effect.*
import cats.effect.std.{ Env, SystemProperties, UUIDGen }

import fs2.io.file.Files
import fs2.io.net.*

import ldbc.amazon.auth.credentials.DefaultCredentialsProviderChain
import ldbc.amazon.auth.token.{ AuthTokenGenerator, RdsIamAuthTokenGenerator }
import ldbc.amazon.identity.{ AwsCredentials, AwsCredentialsProvider }
import ldbc.authentication.plugin.MysqlClearPasswordPlugin

/**
 * AWS IAM authentication plugin for connecting to MySQL databases using IAM credentials.
 * 
 * This plugin enables secure database connections to AWS RDS MySQL instances using AWS IAM
 * credentials instead of traditional username/password authentication. It extends the
 * `MysqlClearPasswordPlugin` and generates temporary authentication tokens using AWS credentials.
 * 
 * The plugin automatically:
 * - Resolves AWS credentials from various sources (environment variables, profiles, IAM roles, etc.)
 * - Generates time-limited authentication tokens signed with AWS credentials
 * - Handles token refresh and credential rotation transparently
 * 
 * Security benefits:
 * - No hardcoded database passwords in application code
 * - Leverages existing AWS IAM policies and roles
 * - Tokens are automatically time-limited (15 minutes)
 * - Supports AWS credential rotation and temporary credentials
 * 
 * Usage requirements:
 * - AWS RDS instance must have IAM authentication enabled
 * - Database user must be created with IAM authentication
 * - Application must have appropriate IAM permissions
 * 
 * @tparam F The effect type that wraps authentication operations
 * @param provider The AWS credentials provider for obtaining authentication credentials
 * @param generator The token generator for creating RDS IAM authentication tokens
 * 
 * @see [[https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html AWS RDS IAM Database Authentication]]
 * @since 1.0.0
 */
final class AwsIamAuthenticationPlugin[F[_]: Monad](
  provider:  AwsCredentialsProvider[F],
  generator: AuthTokenGenerator[F]
) extends MysqlClearPasswordPlugin[F]:

  /**
   * Generates an AWS IAM authentication token instead of processing a traditional password.
   * 
   * This method overrides the standard password hashing behavior to generate a temporary
   * authentication token using AWS IAM credentials. The process involves:
   * 
   * 1. Resolving AWS credentials from the configured provider
   * 2. Generating a signed authentication token using the RDS IAM authentication protocol
   * 3. Converting the token to bytes for transmission to the MySQL server
   * 
   * The generated token is a pre-signed URL that contains:
   * - The RDS endpoint hostname and port
   * - The database username
   * - AWS signature version 4 (SigV4) authentication parameters
   * - A 15-minute expiration time
   * 
   * @param password The original password parameter (ignored in IAM authentication)
   * @param scramble The server's challenge bytes (ignored in IAM authentication)
   * @return The AWS IAM authentication token as UTF-8 encoded bytes wrapped in the effect type F
   * 
   * @note The password and scramble parameters are inherited from the parent trait but are not
   *       used in AWS IAM authentication since the token generation process uses AWS credentials
   *       and cryptographic signing instead of password-based authentication.
   */
  override def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector] =
    for
      credentials <- provider.resolveCredentials()
      token       <- generator.generateToken(credentials)
    yield ByteVector(token.getBytes(StandardCharsets.UTF_8))

object AwsIamAuthenticationPlugin:

  /**
   * Creates a default AWS IAM authentication plugin with standard credential resolution.
   * 
   * This factory method constructs an `AwsIamAuthenticationPlugin` with the default AWS
   * credential provider chain and RDS IAM token generator. The credential provider chain
   * attempts to resolve AWS credentials from multiple sources in the following order:
   * 
   * 1. System properties (aws.accessKeyId, aws.secretAccessKey, aws.sessionToken)
   * 2. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)
   * 3. Web identity token file (for EKS service accounts, Lambda, etc.)
   * 4. AWS profile configuration files (~/.aws/credentials, ~/.aws/config)
   * 5. Amazon EC2 instance profile credentials
   * 6. ECS container credentials
   * 
   * The token generator creates pre-signed authentication tokens using AWS Signature Version 4
   * that are valid for 15 minutes and specific to the provided RDS endpoint and database user.
   * 
   * @tparam F The effect type that must support file operations, environment access, system
   *           properties, network operations, UUID generation, and asynchronous operations
   * @param region The AWS region where the RDS instance is located (e.g., "us-east-1")
   * @param hostname The RDS instance endpoint hostname (e.g., "mydb.abc123.us-east-1.rds.amazonaws.com")
   * @param username The database username configured for IAM authentication
   * @param port The database port number (default: 3306 for MySQL)
   * @return A configured `AwsIamAuthenticationPlugin` instance ready for database authentication
   * 
   * @example {{{
   * import cats.effect.IO
   * import ldbc.amazon.plugin.AwsIamAuthenticationPlugin
   * 
   * val plugin = AwsIamAuthenticationPlugin.default[IO](
   *   region = "us-east-1",
   *   hostname = "mydb.abc123.us-east-1.rds.amazonaws.com",
   *   username = "myuser",
   *   port = 3306
   * )
   * }}}
   * 
   * @see [[ldbc.amazon.auth.credentials.DefaultCredentialsProviderChain]] for credential resolution details
   * @see [[ldbc.amazon.auth.token.RdsIamAuthTokenGenerator]] for token generation implementation
   * @since 1.0.0
   */
  def default[F[_]: Files: Env: SystemProperties: Network: UUIDGen: Async](
    region:   String,
    hostname: String,
    username: String,
    port:     Int = 3306
  ): AwsIamAuthenticationPlugin[F] =
    new AwsIamAuthenticationPlugin[F](
      DefaultCredentialsProviderChain.default[F](region),
      new RdsIamAuthTokenGenerator[F](hostname, port, username, region)
    )
