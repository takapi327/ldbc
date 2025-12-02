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
 * [[AwsCredentialsProvider]] implementation that loads credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and
 * AWS_SESSION_TOKEN environment variables.
 */
final class EnvironmentVariableCredentialsProvider[F[_]: Env: MonadThrow] extends SystemSettingsCredentialsProvider[F]:

  // Customers should be able to specify a credentials provider that only looks at the system properties,
  // but not the environment variables. For that reason, we're only checking the system properties here.
  override def loadSetting(setting: SdkSystemSetting): F[Option[String]] = Env[F].get(setting.toString)

  override def provider: String = BusinessMetricFeatureId.CREDENTIALS_ENV_VARS.code
