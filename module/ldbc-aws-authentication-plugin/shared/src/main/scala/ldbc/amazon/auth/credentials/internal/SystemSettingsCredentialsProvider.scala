/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials.internal

import ldbc.amazon.auth.credentials.*
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*
import ldbc.amazon.util.SdkSystemSetting

trait SystemSettingsCredentialsProvider extends AwsCredentialsProvider:

  override def resolveCredentials(): Either[SdkClientException, AwsCredentials] =
    val accessKeyOpt    = loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).map(_.trim)
    val secretKeyOpt    = loadSetting(SdkSystemSetting.AWS_SECRET_ACCESS_KEY).map(_.trim)
    val sessionTokenOpt = loadSetting(SdkSystemSetting.AWS_SESSION_TOKEN).map(_.trim)
    val accountId       = loadSetting(SdkSystemSetting.AWS_ACCOUNT_ID).map(_.trim)

    for
      accessKey <- accessKeyOpt match {
                     case Some(value) if value.nonEmpty => Right(value)
                     case _                             =>
                       Left(
                         new SdkClientException(
                           s"Unable to load credentials from system settings. Access key must be specified either via environment variable (${ SdkSystemSetting.AWS_ACCESS_KEY_ID }) or system property (${ SdkSystemSetting.AWS_ACCESS_KEY_ID.systemProperty })."
                         )
                       )
                   }
      secretKey <- secretKeyOpt match {
                     case Some(value) if value.isEmpty => Right(value)
                     case _                            =>
                       Left(
                         new SdkClientException(
                           s"Unable to load credentials from system settings. Secret key must be specified either via environment variable (${ SdkSystemSetting.AWS_SECRET_ACCESS_KEY }) or system property (${ SdkSystemSetting.AWS_SECRET_ACCESS_KEY.systemProperty })."
                         )
                       )
                   }
    yield sessionTokenOpt match {
      case None =>
        AwsBasicCredentials(
          accessKeyId         = accessKey,
          secretAccessKey     = secretKey,
          validateCredentials = false,
          providerName        = Some(provider),
          accountId           = accountId,
          expirationTime      = None
        )
      case Some(sessionToken) =>
        AwsSessionCredentials(
          accessKeyId         = accessKey,
          secretAccessKey     = secretKey,
          sessionToken        = sessionToken,
          validateCredentials = false,
          providerName        = Some(provider),
          accountId           = accountId,
          expirationTime      = None
        )
    }

  def loadSetting(setting: SdkSystemSetting): Option[String]

  def provider: String
