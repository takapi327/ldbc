/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import ldbc.fx.Fx

import ldbc.mysql.telemetry.Span
import ldbc.mysql.telemetry.Tracer

import ldbc.mysql.telemetry.TelemetrySpanName

package object protocol:

  /**
   * The size of the header in bytes.
   */
  def parseHeader(headerBytes: Array[Byte]): Int =
    (headerBytes(0) & 0xff) | ((headerBytes(1) & 0xff) << 8) | ((headerBytes(2) & 0xff) << 16)

  def exchange[A](span: TelemetrySpanName)(f: Span => Fx[A])(using tracer: Tracer, exchange: Exchange): Fx[A] =
    tracer.span(span.name).use(s => exchange(f(s)))

  def exchange[A](spanName: String)(f: Span => Fx[A])(using tracer: Tracer, exchange: Exchange): Fx[A] =
    tracer.span(spanName).use(s => exchange(f(s)))
