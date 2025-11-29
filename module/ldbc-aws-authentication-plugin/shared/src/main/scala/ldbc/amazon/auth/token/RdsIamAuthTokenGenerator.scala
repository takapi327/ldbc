package ldbc.amazon.auth.token

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import cats.syntax.all.*

import cats.effect.kernel.{Clock, Sync}

import fs2.hashing.{HashAlgorithm, Hashing}
import fs2.{Chunk, Stream}

import ldbc.amazon.identity.AwsCredentials

/**
 * Implementation of AuthTokenGenerator for Amazon RDS IAM database authentication.
 * 
 * This class generates authentication tokens that can be used to connect to Amazon RDS
 * database instances using IAM credentials instead of traditional database passwords.
 * The generated tokens are signed using AWS Signature Version 4 and are valid for
 * 15 minutes from the time of generation.
 * 
 * The tokens enable secure, temporary access to RDS databases based on IAM policies
 * and eliminate the need to store database passwords in application code.
 * 
 * @param hostname The RDS instance hostname or endpoint
 * @param port The database port number (typically 3306 for MySQL)
 * @param username The database username for which to generate the token
 * @param region The AWS region where the RDS instance is located
 * @param clock Clock instance for timestamp generation
 * @tparam F The effect type that wraps the token generation operations
 * 
 * @example
 * {{{
 * import cats.effect.IO
 * import ldbc.amazon.auth.token.RdsIamAuthTokenGenerator
 * import ldbc.amazon.identity.AwsCredentials
 * 
 * val generator = new RdsIamAuthTokenGenerator[IO](
 *   hostname = "my-db-instance.region.rds.amazonaws.com",
 *   port = 3306,
 *   username = "db_user",
 *   region = "us-east-1"
 * )
 * 
 * val credentials = AwsCredentials(
 *   accessKeyId = "AKIAIOSFODNN7EXAMPLE",
 *   secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
 *   sessionToken = None
 * )
 * 
 * val token: IO[String] = generator.generateToken(credentials)
 * }}}
 */
class RdsIamAuthTokenGenerator[F[_]: Hashing](
  hostname: String,
  port: Int,
  username: String,
  region: String
)(using clock: Clock[F]) extends AuthTokenGenerator[F]:

  /** AWS Signature Version 4 algorithm identifier */
  private val ALGORITHM = "AWS4-HMAC-SHA256"
  /** AWS service identifier for RDS database connections */
  private val SERVICE = "rds-db"
  /** Token expiration time in seconds (15 minutes) */
  private val EXPIRES_SECONDS = 900
  /** AWS4 request terminator string */
  private val TERMINATOR = "aws4_request"

  /**
   * Generates a signed authentication token for RDS IAM database authentication.
   * 
   * This method creates a presigned URL-style token that can be used as a password
   * when connecting to an RDS database instance. The token is signed using AWS
   * Signature Version 4 and includes the necessary IAM credentials and metadata.
   * 
   * The generated token follows the format required by MySQL's mysql_clear_password
   * authentication plugin and can be used with SSL/TLS connections to RDS instances.
   * 
   * @param credentials AWS credentials containing access key, secret key, and optional session token
   * @return The signed authentication token that can be used as a database password
   */
  override def generateToken(credentials: AwsCredentials): F[String] =
    for
      now <- clock.realTimeInstant
      dateTime = formatDateTime(now)
      date = dateTime.substring(0, 8)
      credentialScope = s"$date/$region/$SERVICE/$TERMINATOR"
      credential = s"${credentials.accessKeyId}/$credentialScope"
      queryParams = buildQueryParams(credential, dateTime, credentials.sessionToken, username)
      canonicalRequest = buildCanonicalRequest(s"$hostname:$port", queryParams)
      canonicalRequestHash <- sha256Hex(canonicalRequest)
      stringToSign = buildStringToSign(dateTime, credentialScope, canonicalRequestHash)
      signature <- calculateSignature(credentials.secretAccessKey, date, region, stringToSign)
    yield s"${config.hostname}:${config.port}/?$queryParams&X-Amz-Signature=$signature"

  private def formatDateTime(instant: Instant): String =
    DateTimeFormatter
      .ofPattern("yyyyMMdd'T'HHmmss'Z'")
      .withZone(ZoneOffset.UTC)
      .format(instant)

  private def buildQueryParams(
                                credential: String,
                                dateTime: String,
                                sessionToken: String,
                                username: String
                              ): String =
    val params = List(
      "Action" -> "connect",
      "DBUser" -> username,
      "X-Amz-Algorithm" -> ALGORITHM,
      "X-Amz-Credential" -> credential,
      "X-Amz-Date" -> dateTime,
      "X-Amz-Expires" -> EXPIRESSECONDS.toString,
      "X-Amz-Security-Token" -> sessionToken,
      "X-Amz-SignedHeaders" -> "host"
    )
    params
      .sortBy(_._1)
      .map { case (k, v) => s"${urlEncode(k)}=${urlEncode(v)}" }
      .mkString("&")

  private def buildCanonicalRequest(host: String, queryString: String): String =
    val method = "GET"
    val canonicalUri = "/"
    val canonicalHeaders = s"host:$host\n"
    val signedHeaders = "host"
    val payloadHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" // SHA256 of empty string
    s"$method\n$canonicalUri\n$queryString\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

  private def bytesToHex(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString

  private def sha256Hex(data: String): F[String] =
    Stream
      .chunk(Chunk.array(data.getBytes(StandardCharsets.UTF_8)))
      .through(Hashing[F].hash(HashAlgorithm.SHA256))
      .compile
      .lastOrError
      .map(hash => bytesToHex(hash.bytes.toArray))

  private def buildStringToSign(
                                 dateTime: String,
                                 credentialScope: String,
                                 canonicalRequestHash: String
                               ): String =
    s"$Algorithm\n$dateTime\n$credentialScope\n$canonicalRequestHash"

  private def hmacSha256(key: Array[Byte], data: String): F[Array[Byte]] =
    Hashing[F]
      .hmac(HashAlgorithm.SHA256, Chunk.array(key))
      .use { hmac =>
        for
          _ <- hmac.update(Chunk.array(data.getBytes(StandardCharsets.UTF_8)))
          hash <- hmac.hash
        yield hash.bytes.toArray
      }

  private def calculateSignature(
                                  secretKey: String,
                                  date: String,
                                  region: String,
                                  stringToSign: String
                                ): F[String] =
    for
      kDate <- hmacSha256(s"AWS4$secretKey".getBytes(StandardCharsets.UTF_8), date)
      kRegion <- hmacSha256(kDate, region)
      kService <- hmacSha256(kRegion, Service)
      kSigning <- hmacSha256(kService, Terminator)
      sig <- hmacSha256(kSigning, stringToSign)
    yield bytesToHex(sig)

  private def urlEncode(value: String): String =
    URLEncoder.encode(value, "UTF-8")
      .replace("+", "%20")
      .replace("*", "%2A")
      .replace("%7E", "~")
