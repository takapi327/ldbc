package ldbc.amazon.auth.credentials

import java.time.Instant

import ldbc.amazon.identity.AwsCredentials

final case class AwsSessionCredentials(
                                      accessKeyId: String,
                                      secretAccessKey: String,
                                      sessionToken: String,
                                      validateCredentials: Boolean,
                                      providerName: Option[String],
                                      accountId: Option[String],
                                      expirationTime: Option[Instant],
                                    ) extends AwsCredentials
