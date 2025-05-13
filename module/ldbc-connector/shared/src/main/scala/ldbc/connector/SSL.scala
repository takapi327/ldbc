/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.*
import cats.syntax.all.*

import cats.effect.*

import fs2.io.net.*
import fs2.io.net.tls.*

import ldbc.connector.net.SSLNegotiation

trait SSL:

  outer =>

  def tlsParameters: TLSParameters = TLSParameters.Default

  def fallbackOk: Boolean = false

  def tlsContext[F[_]: Network](using ae: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]]

  def withTLSParameters(_tlsParameters: TLSParameters): SSL =
    new SSL:
      override def tlsParameters: TLSParameters = _tlsParameters
      override def tlsContext[F[_]: Network](using ev: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
        outer.tlsContext

  def withFallback(_fallbackOk: Boolean): SSL =
    new SSL:
      override def fallbackOk: Boolean = _fallbackOk
      override def tlsContext[F[_]: Network](implicit ev: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
        outer.tlsContext

  def toSSLNegotiationOptions[F[_]: Network](logger: Option[String => F[Unit]])(using
    ev: ApplicativeError[F, Throwable]
  ): Resource[F, Option[SSLNegotiation.Options[F]]] =
    this match
      case SSL.None => Resource.pure(None)
      case _        => tlsContext.map(SSLNegotiation.Options(_, tlsParameters, fallbackOk, logger).some)

object SSL extends SSLPlatform:

  /** `SSL` which indicates that SSL is not to be used. */
  object None extends SSL:
    def tlsContext[F[_]: Network](using ae: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
      Resource.eval(ae.raiseError(new Exception("SSL.None: cannot create a TLSContext.")))

  /** `SSL` which trusts all certificates. */
  object Trusted extends SSL:
    def tlsContext[F[_]: Network](using ae: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
      Network[F].tlsContext.insecureResource

  /** `SSL` from the system default `SSLContext`. */
  object System extends SSL:
    def tlsContext[F[_]: Network](using ae: ApplicativeError[F, Throwable]): Resource[F, TLSContext[F]] =
      Network[F].tlsContext.systemResource
