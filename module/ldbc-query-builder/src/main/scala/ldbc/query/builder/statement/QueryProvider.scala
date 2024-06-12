/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import cats.*
import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.Temporal

import ldbc.sql.ResultSet
import ldbc.dsl.*
import ldbc.query.builder.*
import ldbc.query.builder.interpreter.*

trait QueryProvider[T] extends SQL:

  inline def query[F[_]: Temporal]: Query[F, Tuples.InverseColumnMap[T]] =
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
    Query.Impl[F, Tuples.InverseColumnMap[T]](statement, params)
