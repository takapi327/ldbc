/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import org.typelevel.otel4s.trace.Span
import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.telemetry.TelemetrySpanName

package object protocol:

  /**
   * The size of the header in bytes.
   */
  def parseHeader(headerBytes: Array[Byte]): Int =
    (headerBytes(0) & 0xff) | ((headerBytes(1) & 0xff) << 8) | ((headerBytes(2) & 0xff) << 16)

  def exchange[F[_]: Tracer, A](span: TelemetrySpanName)(f: Span[F] => F[A])(using
    exchange: Exchange[F]
  ): F[A] = Tracer[F].span(span.name).use(span => exchange(f(span)))

  def exchange[F[_]: Tracer, A](spanName: String)(f: Span[F] => F[A])(using
    exchange: Exchange[F]
  ): F[A] = Tracer[F].span(spanName).use(span => exchange(f(span)))
