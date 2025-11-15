package ldbc.amazon.identity

import java.time.Instant

/**
 * Interface to represent <b>who</b> is using the SDK, i.e., the identity of the caller, used for authentication.
 *
 * Examples include [[AwsCredentialsIdentity]] and [[TokenIdentity]].
 */
trait Identity:

  /**
   * The time after which this identity will no longer be valid. If this is empty,
   * an expiration time is not known (but the identity may still expire at some
   * time in the future).
   */
  def expirationTime: Option[Instant]

  /**
   * The source that resolved this identity, normally an identity provider. Note that
   * this string value would be set by an identity provider implementation and is
   * intended to be used for for tracking purposes. Avoid building logic on its value.
   */
  def providerName: Option[String]
