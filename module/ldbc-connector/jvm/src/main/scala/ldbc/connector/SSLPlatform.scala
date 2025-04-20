/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.nio.file.Path
import java.security.KeyStore

import javax.net.ssl.SSLContext

import cats.*

import cats.effect.Resource

import fs2.io.net.tls.TLSContext
import fs2.io.net.Network

private[ldbc] trait SSLPlatform:

  /** Creates a `SSL` from an `SSLContext`. */
  def fromSSLContext(ctx: SSLContext): SSL =
    new SSL:
      override def tlsContext[F[_]: Network](implicit ev: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
        Resource.pure(Network[F].tlsContext.fromSSLContext(ctx))

  /** Creates an `SSL` from the specified key store file. */
  def fromKeyStoreFile(
    file:          Path,
    storePassword: Array[Char],
    keyPassword:   Array[Char]
  ): SSL =
    new SSL:
      override def tlsContext[F[_]: Network](implicit ev: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
        Resource.eval(Network[F].tlsContext.fromKeyStoreFile(file, storePassword, keyPassword))

  /** Creates an `SSL` from the specified class path resource. */
  def fromKeyStoreResource(
    resource:      String,
    storePassword: Array[Char],
    keyPassword:   Array[Char]
  ): SSL =
    new SSL:
      override def tlsContext[F[_]: Network](implicit ev: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
        Resource.eval(Network[F].tlsContext.fromKeyStoreResource(resource, storePassword, keyPassword))

  /** Creates an `TLSContext` from the specified key store. */
  def fromKeyStore(
    keyStore:    KeyStore,
    keyPassword: Array[Char]
  ): SSL =
    new SSL:
      override def tlsContext[F[_]: Network](implicit ev: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
        Resource.eval(Network[F].tlsContext.fromKeyStore(keyStore, keyPassword))
