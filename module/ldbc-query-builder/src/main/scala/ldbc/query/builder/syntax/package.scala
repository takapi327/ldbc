/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import cats.effect.*

import ldbc.dsl.SyncSyntax as DslSyntax

package object syntax:

  private trait SyncSyntax[F[_]: Sync] extends QuerySyntax[F], CommandSyntax[F], ConnectionSyntax[F], DslSyntax[F]

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.query.builder.syntax.io.*
   * }}}
   */
  val io: SyncSyntax[IO] = new SyncSyntax[IO] {}
