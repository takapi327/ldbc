package ldbc.amazon.identity.internal

import java.time.Instant
import java.util.Objects

import ldbc.amazon.identity.AwsCredentialsIdentity

final case class DefaultAwsCredentialsIdentity(
  accessKeyId: String,
  secretAccessKey: String,
  accountId: Option[String],
  expirationTime: Option[Instant],
  providerName: Option[String]
) extends AwsCredentialsIdentity:

  override def toString: String =
    val builder = new StringBuilder()
    builder.append("AwsCredentialsIdentity(")
    builder.append(s"accessKeyId=$accessKeyId")
    providerName.foreach(v => builder.append(s"providerName=$v"))
    accountId.foreach(v => builder.append(s"accountId=$v"))

    builder.result()

  override def equals(obj: Any): Boolean =
    if this == obj then
      true
    else if obj == null || getClass != obj.getClass then
      false
    else obj match
      case that: AwsCredentialsIdentity =>
        Objects.equals(accessKeyId, that.accessKeyId) &&
          Objects.equals(secretAccessKey, that.secretAccessKey) &&
          Objects.equals(accountId, that.accountId)
      case _ => false

  override def hashCode(): Int =
    var hashCode = 1
    hashCode = 31 * hashCode + Objects.hashCode(accessKeyId)
    hashCode = 31 * hashCode + Objects.hashCode(secretAccessKey)
    hashCode = 31 * hashCode + Objects.hashCode(accountId)
    hashCode
