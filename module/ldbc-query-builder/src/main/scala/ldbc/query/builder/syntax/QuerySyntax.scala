/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.Temporal

import ldbc.sql.*

import ldbc.dsl.*
import ldbc.dsl.Query as DslQuery

import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]: Temporal]:

  implicit class QueryOps[T](query: Query[T])(using Tuples.IsColumn[T] =:= true):

    @scala.annotation.targetName("simpleQuery")
    inline def query: DslQuery[F, Tuples.InverseColumnMap[T]] =
      given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[T]] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, Tuples.InverseColumnMap[T]]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[T]])
      }
      DslQuery.Impl[F, Tuples.InverseColumnMap[T]](query.statement, query.params)

    inline def query[P <: Product](using mirror: Mirror.ProductOf[P], check: Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes): DslQuery[F, P] =
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
