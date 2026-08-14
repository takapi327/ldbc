/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * TLS client configuration, secure by default: [[SSL.System]] performs both certificate-chain
 * validation and hostname verification. [[SSL.None]] disables TLS entirely.
 *
 * The cross-platform variants ([[SSL.None]], [[SSL.System]], [[SSL.Trusted]], [[SSL.Custom]]) work on
 * every platform. Platform-specific trust material — a JSSE `SSLContext` or a Java `KeyStore` — is
 * expressed via [[SSL.Platform]], which is only constructible on the JVM through the factories on the
 * [[SSL]] companion (`fromSSLContext`, `fromKeyStore`, `fromKeyStoreFile`, `fromKeyStoreResource`).
 */
sealed trait SSL:
  /** Whether the server's identity (hostname) is verified against its certificate. */
  def verifyHostname: Boolean

object SSL extends SSLPlatform:

  /** Disables TLS; the socket is used in the clear. */
  case object None extends SSL:
    override val verifyHostname: Boolean = false

  /** Certificate-chain validation against the platform trust store plus hostname verification (production default). */
  case object System extends SSL:
    override val verifyHostname: Boolean = true

  /** Trusts every certificate and verifies nothing. Development / self-signed use only — never production. */
  case object Trusted extends SSL:
    override val verifyHostname: Boolean = false

  /**
   * Detailed configuration with a custom trust source. Trust material is restricted to cross-platform
   * representations (see [[TrustSource]]); platform-specific trust objects are expressed via
   * [[Platform]] instead.
   *
   * @param trust          the source of trusted CA certificates
   * @param clientAuth     an optional client certificate for mTLS, in PEM form (not yet supported)
   * @param verifyHostname whether to verify the server hostname against its certificate
   * @param tlsVersions    permitted protocol versions (empty = platform default)
   */
  final case class Custom(
    trust:          TrustSource,
    clientAuth:     Option[ClientCertificatePem] = scala.None,
    verifyHostname: Boolean                      = true,
    tlsVersions:    List[String]                 = Nil
  ) extends SSL

  /**
   * TLS driven by a platform-native context (a JSSE `SSLContext` on the JVM). Constructible only on the
   * JVM via the companion's `fromSSLContext` / `fromKeyStore*` factories; the JS and Native engines do
   * not support it.
   *
   * @param context        the platform-native TLS context
   * @param verifyHostname whether to verify the server hostname against its certificate
   */
  final case class Platform(
    context:        PlatformSSLContext,
    verifyHostname: Boolean = true
  ) extends SSL

/**
 * A source of trusted CA certificates, limited to forms that every platform (JSSE / node / s2n) can
 * consume, so it can live in shared code.
 */
sealed trait TrustSource

object TrustSource:

  /** The platform's default trust store (OS / JDK / node bundled CAs). */
  case object System extends TrustSource

  /** Trusts every certificate without validation (equivalent to [[SSL.Trusted]]). */
  case object InsecureTrustAll extends TrustSource

  /**
   * Trusts exactly the CA certificates contained in `pem` (one or more PEM-encoded certificates).
   *
   * @param pem the PEM text of the trusted certificate(s)
   */
  final case class FromPemCerts(pem: String) extends TrustSource

/**
 * A client certificate and its private key in PEM form, for mTLS.
 *
 * @param certPem the PEM-encoded client certificate (chain)
 * @param keyPem  the PEM-encoded PKCS#8 private key
 */
final case class ClientCertificatePem(certPem: String, keyPem: String)
