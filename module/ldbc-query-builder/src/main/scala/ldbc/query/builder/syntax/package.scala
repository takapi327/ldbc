/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.*

import ldbc.sql.*

import ldbc.dsl.{ Query as DslQuery, SyncSyntax as DslSyntax, * }

import ldbc.query.builder.statement.{ Query, Command }
import ldbc.query.builder.interpreter.Tuples

package object syntax:

  private trait SyncSyntax[F[_]: Temporal] extends QuerySyntax[F], CommandSyntax[F], DslSyntax[F]:

    extension [T](query: Query[T])

      inline def query: DslQuery[F, Tuples.InverseColumnMap[T]] =
        given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[T]] = Kleisli { resultSet =>
          ResultSetReader
            .fold[F, Tuples.InverseColumnMap[T]]
            .toList
            .zipWithIndex
            .traverse {
              case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
            }
            .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[T]])
        }
        DslQuery.Impl[F, Tuples.InverseColumnMap[T]](query.statement, query.params)

      inline def queryTo[P <: Product](using
        mirror: Mirror.ProductOf[P],
        check:  Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes
      ): DslQuery[F, P] =
        given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
          ResultSetReader
            .fold[F, mirror.MirroredElemTypes]
            .toList
            .zipWithIndex
            .traverse {
              case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
            }
            .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
        }

        DslQuery.Impl[F, P](query.statement, query.params)

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

      def returning[T <: String | Int | Long](using reader: ResultSetReader[F, T]): Executor[F, T] =
        given Kleisli[F, ResultSet[F], T] = Kleisli(resultSet => reader.read(resultSet, 1))

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
