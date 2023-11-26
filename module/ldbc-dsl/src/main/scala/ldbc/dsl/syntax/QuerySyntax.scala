/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.dsl.logging.{ LogEvent, LogHandler }
import ldbc.query.builder.ColumnQuery
import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]: Sync]:

  implicit class QueryOps[T](buildQuery: Query[F, T])(using Tuples.IsColumnQuery[F, T] =:= true):
    private def connection[A](
      statement:        String,
      params:           Seq[ParameterBinder[F]],
      consumer:         ResultSetConsumer[F, A]
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

      connection[List[Tuples.InverseColumnMap[F, T]]](
        buildQuery.statement,
        buildQuery.params,
        summon[ResultSetConsumer[F, List[Tuples.InverseColumnMap[F, T]]]]
      )

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

      connection[List[P]](
        buildQuery.statement,
        buildQuery.params,
        summon[ResultSetConsumer[F, List[P]]]
      )

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

      connection[Option[Tuples.InverseColumnMap[F, T]]](
        buildQuery.statement,
        buildQuery.params,
        summon[ResultSetConsumer[F, Option[Tuples.InverseColumnMap[F, T]]]]
      )

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

      connection[Option[P]](
        buildQuery.statement,
        buildQuery.params,
        summon[ResultSetConsumer[F, Option[P]]]
      )

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

      connection[Tuples.InverseColumnMap[F, T]](
        buildQuery.statement,
        buildQuery.params,
        summon[ResultSetConsumer[F, Tuples.InverseColumnMap[F, T]]]
      )

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

      connection[P](
        buildQuery.statement,
        buildQuery.params,
        summon[ResultSetConsumer[F, P]]
      )
