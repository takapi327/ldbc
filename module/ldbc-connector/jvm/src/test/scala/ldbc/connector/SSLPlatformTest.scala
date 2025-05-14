/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.nio.file.Paths
import java.security.KeyStore
import java.io.FileInputStream

import javax.net.ssl.SSLContext

import cats.effect.*

import fs2.io.net.Network

class SSLPlatformTest extends FTestPlatform:

  test("SSL.Trusted") {
    // Just test that SSL.Trusted can be used to create a TLSContext
    val result = SSL.Trusted.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }

  test("SSL.fromSSLContext") {
    val sslContext = SSLContext.getDefault
    val ssl        = SSL.fromSSLContext(sslContext)
    val result     = ssl.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }

  test("SSL.fromKeyStoreFile") {
    val classLoader = getClass.getClassLoader
    val resource = classLoader.getResource("keystore.jks")
    val keyStorePath = Paths.get(resource.toURI)
    val storePassword = "password".toCharArray
    val keyPassword = "password".toCharArray

    val ssl = SSL.fromKeyStoreFile(keyStorePath, storePassword, keyPassword)
    val result = ssl.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }

  test("SSL.fromKeyStoreResource") {
    val resource      = "keystore.jks"
    val storePassword = "password".toCharArray
    val keyPassword   = "password".toCharArray

    val ssl    = SSL.fromKeyStoreResource(resource, storePassword, keyPassword)
    val result = ssl.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }

  test("SSL.fromKeyStore") {
    val classLoader = getClass.getClassLoader
    val resource = classLoader.getResource("keystore.jks")
    val keyStore = KeyStore.getInstance("JKS")
    val storePassword = "password".toCharArray
    val keyPassword = "password".toCharArray
    
    // Initialize the KeyStore
    val inputStream = new FileInputStream(resource.getFile)
    try {
      keyStore.load(inputStream, storePassword)
    } finally {
      inputStream.close()
    }

    val ssl = SSL.fromKeyStore(keyStore, keyPassword)
    val result = ssl.tlsContext[IO](Network[IO], implicitly).use(_ => IO.pure(true))
    assertIOBoolean(result)
  }
