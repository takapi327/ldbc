/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.sql.util.FactoryCompat
import ldbc.sql.logging.LogHandler
import ldbc.dsl.ConnectionProvider
import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]: Sync]:

  implicit class QueryOps[T](buildQuery: Query[F, T])(using Tuples.IsColumnQuery[F, T] =:= true)
    extends ConnectionProvider[F]:

    inline given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, Tuples.InverseColumnMap[F, T]]
        .toList
        .zipWithIndex
        .traverse {
          case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[F, T]])
    }

    /**
     * Methods for returning an array of data to be retrieved from the database.
     */
    inline def toList: FactoryCompat[Tuples.InverseColumnMap[F, T], List[Tuples.InverseColumnMap[F, T]]] ?=> LogHandler[
      F
    ] ?=> Kleisli[F, Connection[F], List[Tuples.InverseColumnMap[F, T]]] =
      connectionToList[Tuples.InverseColumnMap[F, T]](buildQuery.statement, buildQuery.params)

    inline def toList[P <: Product](using
      mirror:  Mirror.ProductOf[P],
      check:   Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes,
      factory: FactoryCompat[P, List[P]]
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], List[P]] =
      given Kleisli[F, ResultSet[F], P] =
        summon[Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]]].map(mirror.fromProduct)
      connectionToList[P](buildQuery.statement, buildQuery.params)

    /**
     * A method to return the data to be retrieved from the database as Option type. If there are multiple data, the
     * first one is retrieved.
     */
    inline def headOption: LogHandler[F] ?=> Kleisli[F, Connection[F], Option[Tuples.InverseColumnMap[F, T]]] =
      connectionToHeadOption[Tuples.InverseColumnMap[F, T]](buildQuery.statement, buildQuery.params)

    inline def headOption[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], Option[P]] =
      given Kleisli[F, ResultSet[F], P] =
        summon[Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]]].map(mirror.fromProduct)
      connectionToHeadOption[P](buildQuery.statement, buildQuery.params)

    /**
     * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
     * raised. Use the [[headOption]] method if you want to retrieve individual data.
     */
    inline def unsafe: LogHandler[F] ?=> Kleisli[F, Connection[F], Tuples.InverseColumnMap[F, T]] =
      connectionToUnsafe[Tuples.InverseColumnMap[F, T]](buildQuery.statement, buildQuery.params)

    inline def unsafe[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], P] =
      given Kleisli[F, ResultSet[F], P] =
        summon[Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]]].map(mirror.fromProduct)
      connectionToUnsafe[P](buildQuery.statement, buildQuery.params)
