/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.syntax.all.*
import cats.effect.Sync

import ldbc.sql.*

import ldbc.dsl.*
import ldbc.dsl.util.FactoryCompat
import ldbc.dsl.logging.LogHandler

import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]: Sync]:

  implicit class QueryOps[T](buildQuery: Query[T])(using Tuples.IsColumnQuery[T] =:= true)
    extends ConnectionProvider[F]:

    inline given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[T]] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, Tuples.InverseColumnMap[T]]
        .toList
        .zipWithIndex
        .traverse {
          case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[T]])
    }

    /**
     * Methods for returning an array of data to be retrieved from the database.
     */
    inline def toList: FactoryCompat[Tuples.InverseColumnMap[T], List[Tuples.InverseColumnMap[T]]] ?=> LogHandler[
      F
    ] ?=> Kleisli[F, Connection[F], List[Tuples.InverseColumnMap[T]]] =
      connectionToList[Tuples.InverseColumnMap[T]](buildQuery.statement, buildQuery.params)

    inline def toList[P <: Product](using
      mirror:  Mirror.ProductOf[P],
      check:   Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes,
      factory: FactoryCompat[P, List[P]]
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], List[P]] =
      given Kleisli[F, ResultSet[F], P] =
        summon[Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[T]]].map(mirror.fromProduct)
      connectionToList[P](buildQuery.statement, buildQuery.params)

    /**
     * A method to return the data to be retrieved from the database as Option type. If there are multiple data, the
     * first one is retrieved.
     */
    inline def headOption: LogHandler[F] ?=> Kleisli[F, Connection[F], Option[Tuples.InverseColumnMap[T]]] =
      connectionToHeadOption[Tuples.InverseColumnMap[T]](buildQuery.statement, buildQuery.params)

    inline def headOption[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], Option[P]] =
      given Kleisli[F, ResultSet[F], P] =
        summon[Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[T]]].map(mirror.fromProduct)
      connectionToHeadOption[P](buildQuery.statement, buildQuery.params)

    /**
     * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
     * raised. Use the [[headOption]] method if you want to retrieve individual data.
     */
    inline def unsafe: LogHandler[F] ?=> Kleisli[F, Connection[F], Tuples.InverseColumnMap[T]] =
      connectionToUnsafe[Tuples.InverseColumnMap[T]](buildQuery.statement, buildQuery.params)

    inline def unsafe[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], P] =
      given Kleisli[F, ResultSet[F], P] =
        summon[Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[T]]].map(mirror.fromProduct)
      connectionToUnsafe[P](buildQuery.statement, buildQuery.params)
