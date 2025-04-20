/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.ApplicativeError

import cats.effect.*

import fs2.io.net.tls.{ SecureContext, TLSContext }
import fs2.io.net.Network

private[ldbc] trait SSLPlatform:

  /** Creates an `TLSContext` from the secure context. */
  def fromSecureContext(
    secureContext: SecureContext
  ): SSL = new SSL:
    override def tlsContext[F[_]: Network](using ae: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
      Resource.pure(
        Network[F].tlsContext.fromSecureContext(
          secureContext
        )
      )
