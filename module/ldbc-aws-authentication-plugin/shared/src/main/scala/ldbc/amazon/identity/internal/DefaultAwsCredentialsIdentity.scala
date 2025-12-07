/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.identity.internal

import java.time.Instant
import java.util.Objects

import ldbc.amazon.identity.AwsCredentialsIdentity

/**
 * Default implementation of AWS credentials identity.
 * 
 * This implementation provides AWS credentials with complete identity information
 * including access keys, account ID, expiration time, and provider name. It serves
 * as the concrete implementation used throughout the AWS authentication plugin.
 * 
 * The class implements proper equals/hashCode semantics for credential comparison
 * and provides a secure toString representation that does not expose sensitive
 * information like secret access keys.
 * 
 * @param accessKeyId The AWS access key ID used for authentication
 * @param secretAccessKey The AWS secret access key used for signing requests
 * @param accountId Optional AWS account ID associated with these credentials
 * @param expirationTime Optional expiration time for temporary credentials
 * @param providerName Optional name of the credentials provider that created these credentials
 */
final case class DefaultAwsCredentialsIdentity(
  accessKeyId:     String,
  secretAccessKey: String,
  accountId:       Option[String],
  expirationTime:  Option[Instant],
  providerName:    Option[String]
) extends AwsCredentialsIdentity:

  /**
   * Returns a string representation of these credentials without exposing sensitive information.
   * 
   * The secret access key is intentionally omitted from the string representation for security.
   * Only the access key ID, provider name, and account ID are included.
   * 
   * @return A secure string representation of the credentials identity
   */
  override def toString: String =
    val builder = new StringBuilder()
    builder.append("AwsCredentialsIdentity(")
    builder.append(s"accessKeyId=$accessKeyId")
    providerName.foreach(v => builder.append(s", providerName=$v"))
    accountId.foreach(v => builder.append(s", accountId=$v"))
    builder.append(")")

    builder.result()

  /**
   * Compares this credentials identity with another object for equality.
   * 
   * Two credentials identities are considered equal if they have the same
   * access key ID, secret access key, and account ID. The expiration time
   * and provider name are not considered for equality comparison.
   * 
   * @param obj The object to compare with this credentials identity
   * @return true if the objects are equal, false otherwise
   */
  override def equals(obj: Any): Boolean =
    obj match
      case that: DefaultAwsCredentialsIdentity =>
        (this eq that) ||
        (Objects.equals(accessKeyId, that.accessKeyId) &&
          Objects.equals(secretAccessKey, that.secretAccessKey) &&
          Objects.equals(accountId, that.accountId))
      case _ => false

  /**
   * Returns a hash code value for this credentials identity.
   * 
   * The hash code is computed based on the access key ID, secret access key,
   * and account ID to ensure consistency with the equals method. The expiration
   * time and provider name are not included in the hash code calculation.
   * 
   * @return A hash code value for this credentials identity
   */
  override def hashCode(): Int =
    var hashCode = 1
    hashCode = 31 * hashCode + Objects.hashCode(accessKeyId)
    hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey)
    hashCode = 31 * hashCode + Objects.hashCode(accountId)
    hashCode
