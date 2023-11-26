/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.dsl.ConnectionProvider
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.ColumnQuery
import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]: Sync]:

  implicit class QueryOps[T](buildQuery: Query[F, T])(using Tuples.IsColumnQuery[F, T] =:= true) extends ConnectionProvider[F]:

    /** Methods for returning an array of data to be retrieved from the database.
      */
    def toList: LogHandler[F] ?=> Kleisli[F, Connection[F], List[Tuples.InverseColumnMap[F, T]]] =
      given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]] = (buildQuery.columns match
        case h *: t => h *: t
        case h      => h *: EmptyTuple
      ).toList
        .asInstanceOf[List[ColumnQuery[F, ?]]]
        .traverse {
          case reader: ColumnQuery[F, ?] => reader.read
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[F, T]])

      connectionToList[Tuples.InverseColumnMap[F, T]](buildQuery.statement, buildQuery.params)

    def toList[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], List[P]] =
      given Kleisli[F, ResultSet[F], P] = (buildQuery.columns match
        case h *: t => h *: t
        case h      => h *: EmptyTuple
      ).toList
        .asInstanceOf[List[ColumnQuery[F, ?]]]
        .traverse {
          case reader: ColumnQuery[F, ?] => reader.read
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))

      connectionToList[P](buildQuery.statement, buildQuery.params)

    /** A method to return the data to be retrieved from the database as Option type. If there are multiple data, the
      * first one is retrieved.
      */
    def headOption: LogHandler[F] ?=> Kleisli[F, Connection[F], Option[Tuples.InverseColumnMap[F, T]]] =
      given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]] = (buildQuery.columns match
        case h *: t => h *: t
        case h      => h *: EmptyTuple
      ).toList
        .asInstanceOf[List[ColumnQuery[F, ?]]]
        .traverse {
          case reader: ColumnQuery[F, ?] => reader.read
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[F, T]])

      connectionToHeadOption[Tuples.InverseColumnMap[F, T]](buildQuery.statement, buildQuery.params)

    def headOption[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], Option[P]] =
      given Kleisli[F, ResultSet[F], P] = (buildQuery.columns match
        case h *: t => h *: t
        case h      => h *: EmptyTuple
      ).toList
        .asInstanceOf[List[ColumnQuery[F, ?]]]
        .traverse {
          case reader: ColumnQuery[F, ?] => reader.read
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))

      connectionToHeadOption[P](buildQuery.statement, buildQuery.params)

    /** A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
      * raised. Use the [[headOption]] method if you want to retrieve individual data.
      */
    def unsafe: LogHandler[F] ?=> Kleisli[F, Connection[F], Tuples.InverseColumnMap[F, T]] =
      given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]] = (buildQuery.columns match
        case h *: t => h *: t
        case h      => h *: EmptyTuple
      ).toList
        .asInstanceOf[List[ColumnQuery[F, ?]]]
        .traverse {
          case reader: ColumnQuery[F, ?] => reader.read
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[F, T]])

      connectionToUnsafe[Tuples.InverseColumnMap[F, T]](buildQuery.statement, buildQuery.params)

    def unsafe[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], P] =
      given Kleisli[F, ResultSet[F], P] = (buildQuery.columns match
        case h *: t => h *: t
        case h      => h *: EmptyTuple
      ).toList
        .asInstanceOf[List[ColumnQuery[F, ?]]]
        .traverse {
          case reader: ColumnQuery[F, ?] => reader.read
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))

      connectionToUnsafe[P](buildQuery.statement, buildQuery.params)
