/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.*
import ldbc.dsl.logging.{ LogEvent, LogHandler }
import ldbc.query.builder.ColumnReader
import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]: Sync]:

  extension [T <: Tuple](query: Query[F, T])

    private def connection[A](consumer: ResultSetConsumer[F, A])(using
      logHandler:                       LogHandler[F]
    ): Kleisli[F, Connection[F], A] =
      Kleisli { connection =>
        for
          statement <- connection.prepareStatement(query.statement)
          resultSet <- query.params.zipWithIndex.traverse {
                         case (param, index) => param.bind(statement, index + 1)
                       } >> statement
                         .executeQuery()
                         .onError(ex =>
                           logHandler.run(
                             LogEvent.ExecFailure(query.statement, query.params.map(_.parameter).toList, ex)
                           )
                         )
          result <-
            consumer
              .consume(resultSet)
              .onError(ex =>
                logHandler.run(LogEvent.ProcessingFailure(query.statement, query.params.map(_.parameter).toList, ex))
              )
              <* statement.close()
              <* logHandler.run(LogEvent.Success(query.statement, query.params.map(_.parameter).toList))
        yield result
      }

    def headOption[A](func: T => Kleisli[F, ResultSet[F], A])(using
      LogHandler[F]
    ): Kleisli[F, Connection[F], Option[A]] =
      given Kleisli[F, ResultSet[F], A] = func(query.columns)
      connection[Option[A]](summon[ResultSetConsumer[F, Option[A]]])

    def headOption(using LogHandler[F]): Kleisli[F, Connection[F], Option[Tuples.InverseColumnMap[F, T]]] =
      given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]] = query.columns.productIterator.toList
        .asInstanceOf[List[ColumnReader[F, ?]]]
        .traverse {
          case x: ColumnReader[F, ?] => x.read
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[F, T]])
      connection[Option[Tuples.InverseColumnMap[F, T]]](
        summon[ResultSetConsumer[F, Option[Tuples.InverseColumnMap[F, T]]]]
      )

    def headOption[P <: Product](using
      mirror: scala.deriving.Mirror.ProductOf[P],
      i:      Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes,
      log:    LogHandler[F]
    ): Kleisli[F, Connection[F], Option[P]] =
      given Kleisli[F, ResultSet[F], P] = query.columns.toList
        .asInstanceOf[List[ColumnReader[F, ?]]]
        .traverse {
          case x: ColumnReader[F, ?] => x.read
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      connection[Option[P]](summon[ResultSetConsumer[F, Option[P]]])

    def toList[A](func: T => Kleisli[F, ResultSet[F], A])(using LogHandler[F]): Kleisli[F, Connection[F], List[A]] =
      given Kleisli[F, ResultSet[F], A] = func(query.columns)
      connection[List[A]](summon[ResultSetConsumer[F, List[A]]])

    def toList(using LogHandler[F]): Kleisli[F, Connection[F], List[Tuples.InverseColumnMap[F, T]]] =
      given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[F, T]] = query.columns.productIterator.toList
        .asInstanceOf[List[ColumnReader[F, ?]]]
        .traverse {
          case x: ColumnReader[F, ?] => x.read
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[F, T]])
      connection[List[Tuples.InverseColumnMap[F, T]]](summon[ResultSetConsumer[F, List[Tuples.InverseColumnMap[F, T]]]])

    def toList[P <: Product](using
      mirror: scala.deriving.Mirror.ProductOf[P],
      i:      Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes,
      log:    LogHandler[F]
    ): Kleisli[F, Connection[F], List[P]] =
      given Kleisli[F, ResultSet[F], P] = query.columns.toList
        .asInstanceOf[List[ColumnReader[F, ?]]]
        .traverse {
          case x: ColumnReader[F, ?] => x.read
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      connection[List[P]](summon[ResultSetConsumer[F, List[P]]])

    def unsafe[A](func: T => Kleisli[F, ResultSet[F], A])(using LogHandler[F]): Kleisli[F, Connection[F], A] =
      given Kleisli[F, ResultSet[F], A] = func(query.columns)
      connection[A](summon[ResultSetConsumer[F, A]])

    def unsafe[P <: Product](using
      mirror: scala.deriving.Mirror.ProductOf[P],
      i:      Tuples.InverseColumnMap[F, T] =:= mirror.MirroredElemTypes,
      log:    LogHandler[F]
    ): Kleisli[F, Connection[F], P] =
      given Kleisli[F, ResultSet[F], P] = query.columns.toList
        .asInstanceOf[List[ColumnReader[F, ?]]]
        .traverse {
          case x: ColumnReader[F, ?] => x.read
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      connection[P](summon[ResultSetConsumer[F, P]])
