/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.deriving.Mirror
import scala.compiletime.erasedValue

import cats.syntax.all.*

import cats.effect.*

import ldbc.sql.*

import ldbc.dsl.{ Query as DslQuery, SyncSyntax as DslSyntax, * }
import ldbc.dsl.codec.Decoder

import ldbc.query.builder.statement.{ Query, Command }
import ldbc.query.builder.interpreter.Tuples

package object syntax:

  private trait SyncSyntax[F[_]: Temporal] extends QuerySyntax[F], CommandSyntax[F], DslSyntax[F]:

    extension [T](query: Query[T])

      inline def query(using mirror: Mirror.ProductOf[Tuples.InverseColumnMap[T]]): DslQuery[F, Tuples.InverseColumnMap[T]] =
        DslQuery.Impl[F, Tuples.InverseColumnMap[T]](query.statement, query.params, Decoder.derivedTuple(mirror))

      inline def queryTo[P <: Product](using
        mirror: Mirror.ProductOf[P],
        check:  Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes
      ): DslQuery[F, P] =
        inline erasedValue[P] match
          case _: Tuple => DslQuery.Impl[F, P](query.statement, query.params, Decoder.derivedTuple(mirror))
          case _ => DslQuery.Impl[F, P](query.statement, query.params, Decoder.derivedProduct(mirror))

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
   *   import ldbc.query.builder.syntax.io.*
   * }}}
   */
  val io: SyncSyntax[IO] = new SyncSyntax[IO] {}
