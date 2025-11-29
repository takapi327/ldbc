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

  /**
   * Formats an Instant to ISO 8601 basic format string required by AWS Signature Version 4.
   * 
   * Converts a timestamp to the format "yyyyMMddTHHmmssZ" in UTC timezone,
   * which is required for AWS authentication requests.
   * 
   * @param instant The timestamp to format
   * @return Formatted datetime string in AWS SigV4 format (e.g., "20230101T120000Z")
   */
  private def formatDateTime(instant: Instant): String =
    DateTimeFormatter
      .ofPattern("yyyyMMdd'T'HHmmss'Z'")
      .withZone(ZoneOffset.UTC)
      .format(instant)

  /**
   * Builds the query parameters for the RDS authentication request.
   * 
   * Creates a URL-encoded query string containing all the required AWS Signature Version 4
   * parameters for RDS IAM authentication. The parameters are sorted alphabetically
   * as required by the AWS signing process.
   * 
   * @param credential The AWS credential string in format "accessKeyId/scope"
   * @param dateTime The formatted timestamp for the request
   * @param sessionToken The AWS session token (for temporary credentials)
   * @param username The database username to authenticate as
   * @return URL-encoded query string with all required authentication parameters
   */
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

  /**
   * Constructs the canonical request string according to AWS Signature Version 4 specification.
   * 
   * The canonical request is a standardized representation of the HTTP request that will be
   * used for signature calculation. It includes the HTTP method, URI path, query string,
   * headers, signed headers list, and payload hash.
   * 
   * @param host The database host including port (e.g., "host.rds.amazonaws.com:3306")
   * @param queryString The URL-encoded query parameters
   * @return The canonical request string formatted according to AWS SigV4 requirements
   */
  private def buildCanonicalRequest(host: String, queryString: String): String =
    val method = "GET"
    val canonicalUri = "/"
    val canonicalHeaders = s"host:$host\n"
    val signedHeaders = "host"
    val payloadHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" // SHA256 of empty string
    s"$method\n$canonicalUri\n$queryString\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

  /**
   * Converts a byte array to lowercase hexadecimal string representation.
   * 
   * Used for converting hash outputs to the hex format required by AWS signatures.
   * Each byte is formatted as a two-character lowercase hex string.
   * 
   * @param bytes The byte array to convert
   * @return Lowercase hexadecimal string representation
   */
  private def bytesToHex(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString

  /**
   * Computes the SHA-256 hash of a string and returns it as a lowercase hex string.
   * 
   * Uses fs2's streaming hash functionality to compute the SHA-256 hash of the input
   * string encoded as UTF-8 bytes. The result is converted to lowercase hexadecimal
   * format as required by AWS Signature Version 4.
   * 
   * @param data The string to hash
   * @return The SHA-256 hash as a lowercase hexadecimal string
   */
  private def sha256Hex(data: String): F[String] =
    Stream
      .chunk(Chunk.array(data.getBytes(StandardCharsets.UTF_8)))
      .through(Hashing[F].hash(HashAlgorithm.SHA256))
      .compile
      .lastOrError
      .map(hash => bytesToHex(hash.bytes.toArray))

  /**
   * Creates the "String to Sign" for AWS Signature Version 4.
   * 
   * The string to sign is a formatted string that combines the algorithm identifier,
   * timestamp, credential scope, and canonical request hash. This string will be
   * signed using the AWS signing key to produce the final signature.
   * 
   * @param dateTime The ISO 8601 formatted timestamp
   * @param credentialScope The credential scope (date/region/service/terminator)
   * @param canonicalRequestHash The SHA-256 hash of the canonical request
   * @return The formatted string to be signed
   */
  private def buildStringToSign(
                                 dateTime: String,
                                 credentialScope: String,
                                 canonicalRequestHash: String
                               ): String =
    s"$Algorithm\n$dateTime\n$credentialScope\n$canonicalRequestHash"

  /**
   * Computes HMAC-SHA256 hash using the provided key and data.
   * 
   * Uses fs2's streaming HMAC functionality to compute the HMAC-SHA256 of the input
   * data using the provided key. This is a core cryptographic operation used in
   * AWS Signature Version 4 key derivation and final signature calculation.
   * 
   * @param key The cryptographic key as byte array
   * @param data The data to authenticate as string (will be UTF-8 encoded)
   * @return The HMAC-SHA256 result as byte array
   */
  private def hmacSha256(key: Array[Byte], data: String): F[Array[Byte]] =
    Hashing[F]
      .hmac(HashAlgorithm.SHA256, Chunk.array(key))
      .use { hmac =>
        for
          _ <- hmac.update(Chunk.array(data.getBytes(StandardCharsets.UTF_8)))
          hash <- hmac.hash
        yield hash.bytes.toArray
      }

  /**
   * Derives the AWS Signature Version 4 signing key and calculates the final signature.
   * 
   * Implements the AWS SigV4 key derivation algorithm by computing a series of
   * HMAC-SHA256 operations to derive the signing key, then signs the string-to-sign
   * with that key. The process follows AWS specifications for signature calculation.
   * 
   * Key derivation steps:
   * 1. kDate = HMAC-SHA256("AWS4" + SecretKey, Date)
   * 2. kRegion = HMAC-SHA256(kDate, Region)
   * 3. kService = HMAC-SHA256(kRegion, Service)
   * 4. kSigning = HMAC-SHA256(kService, "aws4_request")
   * 5. signature = HMAC-SHA256(kSigning, StringToSign)
   * 
   * @param secretKey The AWS secret access key
   * @param date The date in YYYYMMDD format
   * @param region The AWS region
   * @param stringToSign The formatted string to be signed
   * @return The final signature as lowercase hexadecimal string
   */
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

  /**
   * URL encodes a string according to AWS requirements.
   * 
   * Performs URL encoding with specific character replacements required by AWS:
   * - Spaces are encoded as %20 (not +)
   * - Asterisks (*) are encoded as %2A
   * - Tildes (~) remain unencoded
   * 
   * This encoding is required for query parameter values in AWS authentication.
   * 
   * @param value The string to URL encode
   * @return The URL encoded string with AWS-specific character handling
   */
  private def urlEncode(value: String): String =
    URLEncoder.encode(value, "UTF-8")
      .replace("+", "%20")
      .replace("*", "%2A")
      .replace("%7E", "~")
