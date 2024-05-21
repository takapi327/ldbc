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
import ldbc.sql.logging.LogEvent
import ldbc.dsl.{ SQL, ConnectionProvider }
import ldbc.dsl.logging.LogHandler

/**
 * Trait for giving database connection information to SQL.
 *
 * @tparam F
 *   The effect type
 */
trait SQLSyntax[F[_]: Sync]:

  implicit class SqlOps(sql: SQL[F]) extends ConnectionProvider[F]:

    /**
     * Methods for returning an array of data to be retrieved from the database.
     */
    inline def toList[T <: Tuple]()
      : FactoryCompat[T, List[T]] ?=> LogHandler[F] ?=> Kleisli[F, Connection[F], List[T]] =
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, T]
          .toList
          .zipWithIndex
          .traverse {
            case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
      }

      connectionToList[T](sql.statement, sql.params)

    inline def toList[P <: Product](using
      mirror:     Mirror.ProductOf[P],
      logHandler: LogHandler[F],
      factory:    FactoryCompat[P, List[P]]
    ): Kleisli[F, Connection[F], List[P]] =
      given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, mirror.MirroredElemTypes]
          .toList
          .zipWithIndex
          .traverse {
            case (reader: ResultSetReader[F, ?], index) => reader.read(resultSet, index + 1)
          }
          .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      }

      connectionToList[P](sql.statement, sql.params)

    /**
     * A method to return the data to be retrieved from the database as Option type. If there are multiple data, the
     * first one is retrieved.
     */
    inline def headOption[T <: Tuple]: LogHandler[F] ?=> Kleisli[F, Connection[F], Option[T]] =
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, T]
          .toList
          .zipWithIndex
          .traverse {
            case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
      }

      connectionToHeadOption[T](sql.statement, sql.params)

    inline def headOption[P <: Product](using
      mirror: Mirror.ProductOf[P]
    ): LogHandler[F] ?=> Kleisli[F, Connection[F], Option[P]] =
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

      connectionToHeadOption[P](sql.statement, sql.params)

    /**
     * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
     * raised. Use the [[headOption]] method if you want to retrieve individual data.
     */
    inline def unsafe[T <: Tuple]: LogHandler[F] ?=> Kleisli[F, Connection[F], T] =
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, T]
          .toList
          .zipWithIndex
          .traverse {
            case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
      }

      connectionToUnsafe[T](sql.statement, sql.params)

    inline def unsafe[P <: Product](using mirror: Mirror.ProductOf[P]): LogHandler[F] ?=> Kleisli[F, Connection[F], P] =
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

      connectionToUnsafe[P](sql.statement, sql.params)

    def update(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], Int] = Kleisli { connection =>
      (for
        statement <- connection.prepareStatement(sql.statement)
        result <- sql.params.zipWithIndex.traverse {
                    case (param, index) => param.bind(statement, index + 1)
                  } >> statement.executeUpdate() <* statement.close()
      yield result)
        .onError(ex => logHandler.run(LogEvent.ExecFailure(sql.statement, sql.params.map(_.parameter).toList, ex)))
        <* logHandler.run(LogEvent.Success(sql.statement, sql.params.map(_.parameter).toList))
    }

    def updateReturningAutoGeneratedKey[T](using
      logHandler: LogHandler[F],
      reader:     ResultSetReader[F, T]
    ): Kleisli[F, Connection[F], T] = Kleisli { connection =>
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        reader.read(resultSet, 1)
      }

      (for
        statement <- connection.prepareStatement(sql.statement, Statement.RETURN_GENERATED_KEYS)
        resultSet <- sql.params.zipWithIndex.traverse {
                       case (param, index) => param.bind(statement, index + 1)
                     } >> statement.executeUpdate() >> statement.getGeneratedKeys()
        result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* statement.close()
      yield result)
        .onError(ex => logHandler.run(LogEvent.ExecFailure(sql.statement, sql.params.map(_.parameter).toList, ex)))
        <* logHandler.run(LogEvent.Success(sql.statement, sql.params.map(_.parameter).toList))
    }
