/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import scala.deriving.Mirror

import cats.data.Kleisli
import cats.implicits.*
import cats.effect.Sync

import ldbc.core.{ Table, Column }
import ldbc.sql.*
import ldbc.dsl.ParameterBinder
import ldbc.dsl.logging.{ LogEvent, LogHandler }
import ldbc.dsl.statement.ExpressionSyntax

trait TableSyntax[F[_]: Sync]:

  extension [P <: Product](table: Table[P])(using mirror: Mirror.ProductOf[P])
    def select[T <: Tuple.Union[Tuple.Map[mirror.MirroredElemTypes, Column]] *: NonEmptyTuple](
      columns: T
    ): Select[P, T] =
      val statement = s"SELECT ${ columns.toArray.distinct.mkString(", ") } FROM ${ table._name }"
      Select[P, T](table, statement, columns, Seq.empty)

  private[ldbc] trait Query[T]:

    def queryString: String
    def columns:     T
    def params:      Seq[ParameterBinder[F]]

    private def query[A](consumer: ResultSetConsumer[F, A])(using
      logHandler:                  LogHandler[F]
    ): Kleisli[F, Connection[F], A] =
      Kleisli { connection =>
        for
          statement <- connection.prepareStatement(queryString)
          resultSet <- params.zipWithIndex.traverse {
                         case (param, index) => param.bind(statement, index + 1)
                       } >> statement
                         .executeQuery()
                         .onError(ex =>
                           logHandler.run(LogEvent.ExecFailure(queryString, params.map(_.parameter).toList, ex))
                         )
          result <- consumer
                      .consume(resultSet)
                      .onError(ex =>
                        logHandler.run(LogEvent.ProcessingFailure(queryString, params.map(_.parameter).toList, ex))
                      )
                      <* statement.close()
                      <* logHandler.run(LogEvent.Success(queryString, params.map(_.parameter).toList))
        yield result
      }

    def headOption[A](func: T => Kleisli[F, ResultSet[F], A])(using
      LogHandler[F]
    ): Kleisli[F, Connection[F], Option[A]] =
      given Kleisli[F, ResultSet[F], A] = func(columns)
      query[Option[A]](summon[ResultSetConsumer[F, Option[A]]])

    def toList[A](func: T => Kleisli[F, ResultSet[F], A])(using LogHandler[F]): Kleisli[F, Connection[F], List[A]] =
      given Kleisli[F, ResultSet[F], A] = func(columns)
      query[List[A]](summon[ResultSetConsumer[F, List[A]]])

    def unsafe[A](func: T => Kleisli[F, ResultSet[F], A])(using LogHandler[F]): Kleisli[F, Connection[F], A] =
      given Kleisli[F, ResultSet[F], A] = func(columns)
      query[A](summon[ResultSetConsumer[F, A]])

  private[ldbc] case class Select[P <: Product, T](
    table:       Table[P],
    queryString: String,
    columns:     T,
    params:      Seq[ParameterBinder[F]]
  ) extends Query[T]:

    def where(func: Table[P] => ExpressionSyntax[F]): Where[P, T] =
      val expressionSyntax = func(table)
      val statement        = s" WHERE ${ expressionSyntax.statement }"
      Where[P, T](table, queryString ++ statement, columns, params ++ expressionSyntax.parameter)

  private[ldbc] case class Where[P <: Product, T](
    table:       Table[P],
    queryString: String,
    columns:     T,
    params:      Seq[ParameterBinder[F]]
  ) extends Query[T]:

    private def union[A](label: String, expressionSyntax: ExpressionSyntax[F]): Where[P, T] =
      val statement = s" $label ${ expressionSyntax.statement }"
      Where[P, T](table, queryString ++ statement, columns, params ++ expressionSyntax.parameter)

    def and(func: Table[P] => ExpressionSyntax[F]): Where[P, T] = union("AND", func(table))
    def or(func: Table[P] => ExpressionSyntax[F]):  Where[P, T] = union("OR", func(table))
    def ||(func: Table[P] => ExpressionSyntax[F]):  Where[P, T] = union("||", func(table))
    def xor(func: Table[P] => ExpressionSyntax[F]): Where[P, T] = union("XOR", func(table))
    def &&(func: Table[P] => ExpressionSyntax[F]):  Where[P, T] = union("&&", func(table))
