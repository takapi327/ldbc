/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.deriving.Mirror
import scala.compiletime.erasedValue

import cats.syntax.all.*

import cats.effect.*

import ldbc.sql.*

import ldbc.dsl.{ Query as DslQuery, SyncSyntax as DslSyntax, * }
import ldbc.dsl.codec.Decoder

import ldbc.statement.{ Query, Command }
import ldbc.statement.syntax.*

package object syntax extends OrderingTable:

  private trait SyncSyntax[F[_]: Temporal] extends QuerySyntax[F], CommandSyntax[F], DslSyntax[F]:

    type TableQuery[P <: Product] = ldbc.statement.TableQuery[Table[P], Table.Opt[P]]

    extension [A, B](query: Query[A, B])

      def query: DslQuery[F, B] = DslQuery.Impl[F, B](query.statement, query.params, query.columns.decoder)

      inline def queryTo[P <: Product](using
        m1:    Mirror.ProductOf[P],
        m2:    Mirror.ProductOf[B],
        check: m1.MirroredElemTypes =:= m2.MirroredElemTypes
      ): DslQuery[F, P] =
        inline erasedValue[P] match
          case _: Tuple => DslQuery.Impl[F, P](query.statement, query.params, Decoder.derivedTuple(m1))
          case _        => DslQuery.Impl[F, P](query.statement, query.params, Decoder.derivedProduct(m1))

    extension (command: Command)
      def update: Executor[F, Int] =
        Executor.Impl[F, Int](
          command.statement,
          command.params,
          connection =>
            for
              prepareStatement <- connection.prepareStatement(command.statement)
              result <- command.params.zipWithIndex.traverse {
                          case (param, index) => param.bind[F](prepareStatement, index + 1)
                        } >> prepareStatement.executeUpdate() <* prepareStatement.close()
            yield result
        )

      def returning[T <: String | Int | Long](using decoder: Decoder.Elem[T]): Executor[F, T] =
        given Decoder[T] = Decoder.one[T]

        Executor.Impl[F, T](
          command.statement,
          command.params,
          connection =>
            for
              prepareStatement <- connection.prepareStatement(command.statement, Statement.RETURN_GENERATED_KEYS)
              resultSet <- command.params.zipWithIndex.traverse {
                             case (param, index) => param.bind[F](prepareStatement, index + 1)
                           } >> prepareStatement.executeUpdate() >> prepareStatement.getGeneratedKeys()
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
  val io: SyncSyntax[IO] = new SyncSyntax[IO] {}
