/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.deriving.Mirror

import ldbc.dsl.{ Query as DslQuery, SyncSyntax as DslSyntax, * }
import ldbc.dsl.codec.Decoder

import ldbc.statement.{ Command, Query }
import ldbc.statement.syntax.*
import ldbc.statement.Schema

package object syntax:

  private trait SyncSyntax extends QuerySyntax, CommandSyntax, DslSyntax, ParamBinder:

    extension [A, B](query: Query[A, B])

      def query: DslQuery[B] = DslQuery.Impl[B](query.statement, query.params, query.columns.decoder)

      def queryTo[P <: Product](using
        m1:      Mirror.ProductOf[P],
        m2:      Mirror.ProductOf[B],
        check:   m1.MirroredElemTypes =:= m2.MirroredElemTypes,
        decoder: Decoder[P]
      ): DslQuery[P] =
        DslQuery.Impl[P](query.statement, query.params, decoder)

    extension (command: Command)
      def update: DBIO[Int] = DBIO.update(command.statement, command.params)

      def returning[T <: String | Int | Long](using decoder: Decoder[T]): DBIO[T] =
        DBIO.returning(command.statement, command.params, decoder)

    implicit final def schemaDDLOps(ddl: Schema.DDL): DBIO[Array[Int]] =
      DBIO.sequence(ddl.statements)

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.schema.syntax.io.*
   * }}}
   */
  val io: SyncSyntax = new SyncSyntax {}
