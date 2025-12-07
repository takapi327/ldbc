/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import cats.MonadThrow

import cats.effect.std.Env

import ldbc.amazon.auth.credentials.internal.SystemSettingsCredentialsProvider
import ldbc.amazon.useragent.BusinessMetricFeatureId
import ldbc.amazon.util.SdkSystemSetting

/**
 * AWS credentials provider that loads credentials from environment variables.
 * 
 * This provider looks for AWS credentials in the following environment variables:
 * - AWS_ACCESS_KEY_ID: The AWS access key ID
 * - AWS_SECRET_ACCESS_KEY: The AWS secret access key  
 * - AWS_SESSION_TOKEN: Optional session token for temporary credentials
 * 
 * This provider only checks environment variables and does not fallback to system properties.
 * Customers can use this provider when they want to explicitly source credentials from
 * environment variables only.
 * 
 * @tparam F The effect type that supports environment variable access and error handling
 */
final class EnvironmentVariableCredentialsProvider[F[_]: Env: MonadThrow] extends SystemSettingsCredentialsProvider[F]:

  /**
   * Loads a setting value from environment variables only.
   * 
   * This implementation specifically looks at environment variables and does not check
   * system properties, allowing customers to specify a credentials provider that only
   * uses environment variables.
   * 
   * @param setting The system setting to load
   * @return An effect containing the optional setting value from environment variables
   */
  override def loadSetting(setting: SdkSystemSetting): F[Option[String]] = Env[F].get(setting.toString)

  /**
   * Returns the provider identifier for business metrics tracking.
   * 
   * @return The business metric feature ID for environment variable credentials
   */
  override def provider: String = BusinessMetricFeatureId.CREDENTIALS_ENV_VARS.code
