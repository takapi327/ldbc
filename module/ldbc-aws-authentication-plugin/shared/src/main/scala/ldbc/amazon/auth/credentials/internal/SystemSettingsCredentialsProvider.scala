/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials.internal

import cats.syntax.all.*
import cats.MonadThrow

import ldbc.amazon.auth.credentials.*
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*
import ldbc.amazon.util.SdkSystemSetting

trait SystemSettingsCredentialsProvider[F[_]](using ev: MonadThrow[F]) extends AwsCredentialsProvider[F]:

  override def resolveCredentials(): F[AwsCredentials] =
    for
      accessKeyOpt    <- loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).map(_.map(_.trim))
      secretKeyOpt    <- loadSetting(SdkSystemSetting.AWS_SECRET_ACCESS_KEY).map(_.map(_.trim))
      sessionTokenOpt <- loadSetting(SdkSystemSetting.AWS_SESSION_TOKEN).map(_.map(_.trim))
      accountId       <- loadSetting(SdkSystemSetting.AWS_ACCOUNT_ID).map(_.map(_.trim))
      accessKey       <- ev.fromOption(
                     accessKeyOpt,
                     new SdkClientException(
                       s"Unable to load credentials from system settings. Access key must be specified either via environment variable (${ SdkSystemSetting.AWS_ACCESS_KEY_ID }) or system property (${ SdkSystemSetting.AWS_ACCESS_KEY_ID.systemProperty })."
                     )
                   )
      secretKey <- ev.fromOption(
                     secretKeyOpt,
                     new SdkClientException(
                       s"Unable to load credentials from system settings. Secret key must be specified either via environment variable (${ SdkSystemSetting.AWS_SECRET_ACCESS_KEY }) or system property (${ SdkSystemSetting.AWS_SECRET_ACCESS_KEY.systemProperty })."
                     )
                   )
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

  def loadSetting(setting: SdkSystemSetting): F[Option[String]]

  def provider: String
