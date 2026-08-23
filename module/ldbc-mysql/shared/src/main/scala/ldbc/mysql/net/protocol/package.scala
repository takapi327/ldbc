/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import ldbc.mysql.telemetry.Span
import ldbc.mysql.telemetry.TelemetrySpanName
import ldbc.mysql.telemetry.Tracer

package object protocol:

  /**
   * The size of the header in bytes.
   */
  def parseHeader(headerBytes: Array[Byte]): Int =
    (headerBytes(0) & 0xff) | ((headerBytes(1) & 0xff) << 8) | ((headerBytes(2) & 0xff) << 16)

  def exchange[F[_], A](span: TelemetrySpanName)(f: Span[F] => F[A])(using tracer: Tracer[F], exchange: Exchange[F]): F[A] =
    tracer.span(span.name).use(s => exchange(f(s)))

  def exchange[F[_], A](spanName: String)(f: Span[F] => F[A])(using tracer: Tracer[F], exchange: Exchange[F]): F[A] =
    tracer.span(spanName).use(s => exchange(f(s)))
