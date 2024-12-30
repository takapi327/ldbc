/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.deriving.Mirror

import cats.effect.*

import cats.syntax.all.*

import ldbc.sql.*

import ldbc.dsl.{ Query as DslQuery, SyncSyntax as DslSyntax, * }
import ldbc.dsl.codec.Decoder

import ldbc.statement.{ Command, Query }
import ldbc.statement.syntax.*

package object syntax:

  private trait SyncSyntax[F[_]: Temporal] extends QuerySyntax[F], CommandSyntax[F], DslSyntax[F], ParamBinder[F]:

    extension [A, B](query: Query[A, B])

      def query: DslQuery[F, B] = DslQuery.Impl[F, B](query.statement, query.params, query.columns.decoder)

      def queryTo[P <: Product](using
        m1:    Mirror.ProductOf[P],
        m2:    Mirror.ProductOf[B],
        check: m1.MirroredElemTypes =:= m2.MirroredElemTypes,
                                        decoder: Decoder[P]
      ): DslQuery[F, P] =
        DslQuery.Impl[F, P](query.statement, query.params, decoder)

    extension (command: Command)
      def update: DBIO[Int] =
        DBIO.Impl[F, Int](
          command.statement,
          command.params,
          connection =>
            for
              prepareStatement <- connection.prepareStatement(command.statement)
              result <-
                paramBind(prepareStatement, command.params) >> prepareStatement.executeUpdate() <* prepareStatement
                  .close()
            yield result
        )

      def returning[T <: String | Int | Long](using Decoder[T]): DBIO[T] =
        DBIO.Impl[F, T](
          command.statement,
          command.params,
          connection =>
            for
              prepareStatement <- connection.prepareStatement(command.statement, Statement.RETURN_GENERATED_KEYS)
              resultSet <-
                paramBind(prepareStatement, command.params) >> prepareStatement.executeUpdate() >> prepareStatement
                  .getGeneratedKeys()
              result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* prepareStatement.close()
            yield result
        )

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.schema.syntax.io.*
   * }}}
   */
  val io: SyncSyntax[IO] & Alias & DataTypes = new SyncSyntax[IO] with Alias with DataTypes {}
