/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.KeyStore

import javax.net.ssl.{ SNIHostName, SSLContext, SSLEngine, TrustManager, TrustManagerFactory, X509TrustManager }

import scala.jdk.CollectionConverters.*

import ldbc.fx.Fx

/**
 * JVM TLS engine: drives a JSSE [[javax.net.ssl.SSLEngine]] directly over the plaintext [[Socket]].
 * Because the engine is created with `createSSLEngine(host, port)` and
 * `endpointIdentificationAlgorithm = "HTTPS"`, JSSE performs hostname verification during the
 * handshake itself — no post-handshake manual matching is needed (design §7).
 */
private[net] object PlatformTls:

  /**
   * Wraps `socket` in a TLS client session (see [[Tls.client]]). The handshake runs to completion
   * before the socket is returned; on handshake failure the plaintext socket is closed.
   */
  def client(socket: Socket, host: String, port: Int, ssl: SSL): Fx[Socket] =
    Fx.delay(createEngine(host, port, ssl)).flatMap { engine =>
      val tls = new TlsSocket(engine, socket)
      tls.handshake
        .handleErrorWith { error =>
          socket.close().handleErrorWith(_ => Fx.unit).flatMap(_ => Fx.raiseError(error))
        }
        .map(_ => tls)
    }

  /** Builds a client-mode engine with trust, SNI (DNS hosts only), and hostname verification applied. */
  private def createEngine(host: String, port: Int, ssl: SSL): SSLEngine =
    val context = ssl match
      case SSL.Platform(ctx, _) => ctx
      case _                    =>
        val c = SSLContext.getInstance("TLS")
        c.init(null, trustManagers(ssl), null)
        c
    val engine = context.createSSLEngine(host, port)
    engine.setUseClientMode(true)
    val params = engine.getSSLParameters
    if ssl.verifyHostname then params.setEndpointIdentificationAlgorithm("HTTPS")
    if !HostnameMatcher.isIpLiteral(host) then params.setServerNames(List(new SNIHostName(host)).asJava)
    ssl match
      case SSL.Custom(_, _, _, versions) if versions.nonEmpty => params.setProtocols(versions.toArray)
      case _                                                  => ()
    engine.setSSLParameters(params)
    engine

  /** Resolves the trust managers for `ssl`; `null` selects the JDK default trust store. */
  private def trustManagers(ssl: SSL): Array[TrustManager] =
    ssl match
      case SSL.System                          => null
      case SSL.Trusted                         => Array(trustAll)
      case SSL.Custom(trust, clientAuth, _, _) =>
        if clientAuth.isDefined then
          throw new UnsupportedOperationException("client certificates (mTLS) are not yet supported")
        trust match
          case TrustSource.System            => null
          case TrustSource.InsecureTrustAll  => Array(trustAll)
          case TrustSource.FromPemCerts(pem) => fromPemCerts(pem)
      case SSL.None | SSL.Platform(_, _) =>
        throw new IllegalStateException("unreachable: None is handled by Tls.client and Platform by createEngine")

  /** A trust manager that accepts every certificate (development / self-signed use only). */
  private val trustAll: X509TrustManager = new X509TrustManager:
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ()
    override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ()
    override def getAcceptedIssuers: Array[X509Certificate] = Array.empty

  /** Builds trust managers that trust exactly the PEM-encoded certificates in `pem`. */
  private def fromPemCerts(pem: String): Array[TrustManager] =
    val factory      = CertificateFactory.getInstance("X.509")
    val certificates = factory.generateCertificates(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)))
    val keyStore     = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null, null)
    certificates.asScala.zipWithIndex.foreach {
      case (cert, index) =>
        keyStore.setCertificateEntry(s"trusted-$index", cert)
    }
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(keyStore)
    tmf.getTrustManagers
