/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.dsl.Command
import ldbc.query.builder.ColumnQuery
import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]: Sync]:

  implicit class QueryOps[T](buildQuery: Query[F, T])(using Tuples.IsColumnQuery[F, T] =:= true):

    def query: Command[F, Tuples.InverseColumnMap[F, T]] =
      Command(
        buildQuery.statement,
        buildQuery.params,
        (buildQuery.columns match
          case h *: t => h *: t
          case h      => h *: EmptyTuple
        ).toList
          .asInstanceOf[List[ColumnQuery[F, ?]]]
          .traverse {
            case reader: ColumnQuery[F, ?] => reader.read
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[F, T]])
      )

    def query[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes
    ): Command[F, P] =
      Command(
        buildQuery.statement,
        buildQuery.params,
        (buildQuery.columns match
          case h *: t => h *: t
          case h      => h *: EmptyTuple
        ).toList
          .asInstanceOf[List[ColumnQuery[F, ?]]]
          .traverse {
            case reader: ColumnQuery[F, ?] => reader.read
          }
          .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      )

    def query[A](func: T => Kleisli[F, ResultSet[F], A]): Command[F, A] =
      Command(
        buildQuery.statement,
        buildQuery.params,
        func(buildQuery.columns)
      )
