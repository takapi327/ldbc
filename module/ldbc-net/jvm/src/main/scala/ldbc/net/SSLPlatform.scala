/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.FileInputStream
import java.nio.file.Path

import java.security.KeyStore

import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }

/**
 * JVM-only factories for building an [[SSL]] from platform-native trust material (a JSSE `SSLContext`
 * or a Java `KeyStore`). Mixed into the [[SSL]] companion on the JVM; the JS and Native companions
 * expose no such factories, since those platforms have no `SSLContext`/`KeyStore`.
 */
private[net] trait SSLPlatform:

  /**
   * Builds an [[SSL]] backed by the given JSSE `SSLContext`.
   *
   * @param context the pre-configured TLS context
   */
  def fromSSLContext(context: SSLContext): SSL = SSL.Platform(context)

  /**
   * Builds an [[SSL]] whose trust and key material come from the given `KeyStore`.
   *
   * @param keyStore    the key store providing trusted CAs (and optionally a client key)
   * @param keyPassword the password protecting the client key entry
   */
  def fromKeyStore(keyStore: KeyStore, keyPassword: Array[Char]): SSL =
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(keyStore, keyPassword)
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(keyStore)
    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, tmf.getTrustManagers, null)
    SSL.Platform(context)

  /**
   * Builds an [[SSL]] from a key store loaded from the given file.
   *
   * @param file          the key store file
   * @param storePassword the password protecting the key store
   * @param keyPassword   the password protecting the client key entry
   */
  def fromKeyStoreFile(file: Path, storePassword: Array[Char], keyPassword: Array[Char]): SSL =
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    val in       = new FileInputStream(file.toFile)
    try keyStore.load(in, storePassword)
    finally in.close()
    fromKeyStore(keyStore, keyPassword)

  /**
   * Builds an [[SSL]] from a key store loaded from the given classpath resource.
   *
   * @param resource      the classpath resource path of the key store
   * @param storePassword the password protecting the key store
   * @param keyPassword   the password protecting the client key entry
   */
  def fromKeyStoreResource(resource: String, storePassword: Array[Char], keyPassword: Array[Char]): SSL =
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    val in       = getClass.getClassLoader.getResourceAsStream(resource)
    try keyStore.load(in, storePassword)
    finally in.close()
    fromKeyStore(keyStore, keyPassword)
