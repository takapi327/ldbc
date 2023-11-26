/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.dsl.SQL
import ldbc.dsl.logging.{ LogEvent, LogHandler }

/** Trait for giving database connection information to SQL.
  *
  * @tparam F
  *   The effect type
  */
trait SQLSyntax[F[_]: Sync]:

  implicit class SqlOps(sql: SQL[F]):

    private def connection[A](
      statement: String,
      params: Seq[ParameterBinder[F]],
      consumer: ResultSetConsumer[F, A]
    )(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], A] =
      Kleisli { connection =>
        for
          prepareStatement <- connection.prepareStatement(statement)
          resultSet <- params.zipWithIndex.traverse {
            case (param, index) => param.bind(prepareStatement, index + 1)
          } >> prepareStatement
            .executeQuery()
            .onError(ex =>
              logHandler.run(
                LogEvent.ExecFailure(statement, params.map(_.parameter).toList, ex)
              )
            )
          result <-
            consumer
              .consume(resultSet)
              .onError(ex => logHandler.run(LogEvent.ProcessingFailure(statement, params.map(_.parameter).toList, ex)))
              <* prepareStatement.close()
              <* logHandler.run(LogEvent.Success(statement, params.map(_.parameter).toList))
        yield result
      }

    /** Methods for returning an array of data to be retrieved from the database.
     */
    inline def toList[T <: Tuple]: LogHandler[F] ?=> Kleisli[F, Connection[F], List[T]] =
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, T]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
      }

      connection[List[T]](
        sql.statement,
        sql.params,
        summon[ResultSetConsumer[F, List[T]]]
      )

    inline def toList[P <: Product](using mirror: Mirror.ProductOf[P]): LogHandler[F] ?=> Kleisli[F, Connection[F], List[P]] =
      given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, mirror.MirroredElemTypes]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      }

      connection[List[P]](
        sql.statement,
        sql.params,
        summon[ResultSetConsumer[F, List[P]]]
      )

    /** A method to return the data to be retrieved from the database as Option type. If there are multiple data, the
     * first one is retrieved.
     */
    inline def headOption[T <: Tuple]: LogHandler[F] ?=> Kleisli[F, Connection[F], Option[T]] =
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, T]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
      }

      connection[Option[T]](
        sql.statement,
        sql.params,
        summon[ResultSetConsumer[F, Option[T]]]
      )

    inline def headOption[P <: Product](using mirror: Mirror.ProductOf[P]): LogHandler[F] ?=> Kleisli[F, Connection[F], Option[P]] =
      given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, mirror.MirroredElemTypes]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      }

      connection[Option[P]](
        sql.statement,
        sql.params,
        summon[ResultSetConsumer[F, Option[P]]]
      )

    /** A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
     * raised. Use the [[headOption]] method if you want to retrieve individual data.
     */
    inline def unsafe[T <: Tuple]: LogHandler[F] ?=> Kleisli[F, Connection[F], T] =
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, T]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
      }

      connection[T](
        sql.statement,
        sql.params,
        summon[ResultSetConsumer[F, T]]
      )

    inline def unsafe[P <: Product](using mirror: Mirror.ProductOf[P]): LogHandler[F] ?=> Kleisli[F, Connection[F], P] =
      given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, mirror.MirroredElemTypes]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      }

      connection[P](
        sql.statement,
        sql.params,
        summon[ResultSetConsumer[F, P]]
      )

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
                                           reader: ResultSetReader[F, T]
                                          ): Kleisli[F, Connection[F], T] = Kleisli { connection =>
      given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
        reader.read(resultSet, 1)
      }

      (for
        statement <- connection.prepareStatement(sql.statement, Statement.Generated.RETURN_GENERATED_KEYS)
        resultSet <- sql.params.zipWithIndex.traverse {
          case (param, index) => param.bind(statement, index + 1)
        } >> statement.executeUpdate() >> statement.getGeneratedKeys()
        result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* statement.close()
      yield result)
        .onError(ex => logHandler.run(LogEvent.ExecFailure(sql.statement, sql.params.map(_.parameter).toList, ex)))
        <* logHandler.run(LogEvent.Success(sql.statement, sql.params.map(_.parameter).toList))
    }
