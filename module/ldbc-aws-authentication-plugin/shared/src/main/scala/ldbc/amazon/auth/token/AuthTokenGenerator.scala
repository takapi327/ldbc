package ldbc.amazon.auth.token

import ldbc.amazon.identity.AwsCredentials

trait AuthTokenGenerator[F[_]]:
  def generateToken(credentials: AwsCredentials): F[String]
