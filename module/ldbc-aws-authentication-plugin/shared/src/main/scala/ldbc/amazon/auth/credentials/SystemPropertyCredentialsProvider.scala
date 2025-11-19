package ldbc.amazon.auth.credentials

import ldbc.amazon.auth.credentials.internal.SystemSettingsCredentialsProvider
import ldbc.amazon.util.SdkSystemSetting
import ldbc.amazon.useragent.BusinessMetricFeatureId

/**
 * [[AwsCredentialsProvider]] implementation that loads credentials from the aws.accessKeyId, aws.secretAccessKey and
 * aws.sessionToken system properties.
 */
final class SystemPropertyCredentialsProvider extends SystemSettingsCredentialsProvider:

  // Customers should be able to specify a credentials provider that only looks at the system properties,
  // but not the environment variables. For that reason, we're only checking the system properties here.
  override def loadSetting(setting: SdkSystemSetting): Option[String] = Option(System.getProperty(setting.systemProperty))

  override def provider: String = BusinessMetricFeatureId.CREDENTIALS_JVM_SYSTEM_PROPERTIES.code
