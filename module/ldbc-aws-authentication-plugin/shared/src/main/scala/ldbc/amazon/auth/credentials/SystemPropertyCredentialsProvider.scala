/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import cats.MonadThrow

import cats.effect.std.SystemProperties

import ldbc.amazon.auth.credentials.internal.SystemSettingsCredentialsProvider
import ldbc.amazon.useragent.BusinessMetricFeatureId
import ldbc.amazon.util.SdkSystemSetting

/**
 * [[AwsCredentialsProvider]] implementation that loads credentials from the aws.accessKeyId, aws.secretAccessKey and
 * aws.sessionToken system properties.
 */
final class SystemPropertyCredentialsProvider[F[_]: SystemProperties: MonadThrow]
  extends SystemSettingsCredentialsProvider[F]:

  // Customers should be able to specify a credentials provider that only looks at the system properties,
  // but not the environment variables. For that reason, we're only checking the system properties here.
  override def loadSetting(setting: SdkSystemSetting): F[Option[String]] =
    SystemProperties[F].get(setting.systemProperty)

  override def provider: String = BusinessMetricFeatureId.CREDENTIALS_JVM_SYSTEM_PROPERTIES.code
