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
 * AWS credentials provider that loads credentials from JVM system properties.
 * 
 * This provider looks for AWS credentials in the following system properties:
 * - aws.accessKeyId: The AWS access key ID
 * - aws.secretAccessKey: The AWS secret access key
 * - aws.sessionToken: Optional session token for temporary credentials
 * 
 * This provider only checks system properties and does not fallback to environment variables.
 * Customers can use this provider when they want to explicitly source credentials from
 * JVM system properties only.
 * 
 * @tparam F The effect type that supports system property access and error handling
 */
final class SystemPropertyCredentialsProvider[F[_]: SystemProperties: MonadThrow]
  extends SystemSettingsCredentialsProvider[F]:

  /**
   * Loads a setting value from JVM system properties only.
   * 
   * This implementation specifically looks at system properties and does not check
   * environment variables, allowing customers to specify a credentials provider that only
   * uses system properties.
   * 
   * @param setting The system setting to load
   * @return An effect containing the optional setting value from system properties
   */
  override def loadSetting(setting: SdkSystemSetting): F[Option[String]] =
    SystemProperties[F].get(setting.systemProperty)

  /**
   * Returns the provider identifier for business metrics tracking.
   * 
   * @return The business metric feature ID for JVM system property credentials
   */
  override def provider: String = BusinessMetricFeatureId.CREDENTIALS_JVM_SYSTEM_PROPERTIES.code
