/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import org.typelevel.otel4s.trace.Span
import org.typelevel.otel4s.trace.Tracer

package object protocol:

  def exchange[F[_]: Tracer, A](label: String)(f: Span[F] => F[A])(using
    exchange: Exchange[F]
  ): F[A] = Tracer[F].span(label).use(span => exchange(f(span)))
