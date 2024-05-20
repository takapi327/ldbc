/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.effect.*

import ldbc.sql.*

package object connector:

  private trait StringContextSyntax[F[_]: Temporal]:

    extension (sc: StringContext)
      inline def sql(inline args: ParameterBinder[F]*): SQL[F] =
        val strings     = sc.parts.iterator
        val expressions = args.iterator
        Mysql[F](strings.mkString("?"), expressions.toList)

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.connector.io.*
   * }}}
   */
  val io: StringContextSyntax[IO] = new StringContextSyntax[IO] {}
