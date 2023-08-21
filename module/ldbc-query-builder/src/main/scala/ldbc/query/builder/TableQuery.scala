/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import scala.deriving.Mirror
import scala.compiletime.*
import scala.annotation.targetName

import ldbc.core.*
import ldbc.core.interpreter.Tuples as CoreTuples
import ldbc.sql.*
import ldbc.query.builder.statement.*
import ldbc.query.builder.interpreter.Tuples

case class TableQuery[F[_], P <: Product](table: Table[P]):

  private inline def inferResultSetReader[T]: ResultSetReader[F, T] =
    summonFrom[ResultSetReader[F, T]] {
      case reader: ResultSetReader[F, T] => reader
      case _                             => error("ResultSetReader cannot be inferred")
    }

  private inline def foldResultSetReader[T <: Tuple]: Tuples.MapToResultSetReader[F, T] =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (h *: t)   => inferResultSetReader[h] *: foldResultSetReader[t]

  inline def selectAll(using mirror: Mirror.ProductOf[P]): Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]] =
    val columns = table.*.zip(foldResultSetReader[mirror.MirroredElemTypes])
      .map(
        [t] =>
          (x: t) =>
            val (column, reader) = x.asInstanceOf[(Column[t], ResultSetReader[F, t])]
            ColumnReader(column, reader)
      )
      .asInstanceOf[Tuples.ToColumn[F, mirror.MirroredElemTypes]]
    val statement = s"SELECT ${ table.*.toList.mkString(", ") } FROM ${ table._name }"
    new Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]](table, statement, columns, Seq.empty)

  def select[T](func: Table[P] => Tuples.ToColumn[F, T]): Select[F, P, Tuples.ToColumn[F, T]] =
    val columns = func(table)
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    val statement = s"SELECT $str FROM ${ table._name }"
    Select[F, P, Tuples.ToColumn[F, T]](table, statement, columns, Seq.empty)

  def join[O <: Product](other: Table[O]):         Join[F, P, O] = Join(table.as("x1"), other.as("x2"))
  def join[O <: Product](other: TableQuery[F, O]): Join[F, P, O] = Join(table.as("x1"), other.table.as("x2"))

  // TODO: In the following implementation, Warning occurs at the time of Compile, so it is cast by asInstanceOf.
  // case (value: Any, parameter: Parameter[F, Any]) => ???
  inline def insert(using mirror: Mirror.ProductOf[P])(values: mirror.MirroredElemTypes*): Insert[F, P] =
    val tuples = values.map(Tuple.fromProduct)
    val parameterBinders = tuples.flatMap(_.zip(Parameter.fold[F, mirror.MirroredElemTypes]).toArray.map {
      case (value: Any, parameter: Any) =>
        ParameterBinder[F, Any](value)(using parameter.asInstanceOf[Parameter[F, Any]])
    })
    new Insert.Multi[F, P, Tuple](table, tuples.toList, parameterBinders.toList)

  def selectInsert[T <: Tuple](func: Table[P] => Tuple.Map[T, Column]): Insert.Select[F, P, T] =
    Insert.Select[F, P, T](table, func(table))

  @targetName("insertProduct")
  inline def +=(value: P)(using mirror: Mirror.ProductOf[P]): Insert[F, P] =
    val tuples = Tuple.fromProduct(value)
    val parameterBinders = tuples.zip(Parameter.fold[F, mirror.MirroredElemTypes]).toArray.map {
      case (value: Any, parameter: Any) =>
        ParameterBinder[F, Any](value)(using parameter.asInstanceOf[Parameter[F, Any]])
    }
    new Insert.Single[F, P, Tuple](table, tuples, parameterBinders.toList)

  @targetName("insertProducts")
  inline def ++=(values: List[P])(using mirror: Mirror.ProductOf[P]): Insert[F, P] =
    val tuples = values.map(Tuple.fromProduct)
    val parameterBinders = tuples.flatMap(_.zip(Parameter.fold[F, mirror.MirroredElemTypes]).toArray.map {
      case (value: Any, parameter: Any) =>
        ParameterBinder[F, Any](value)(using parameter.asInstanceOf[Parameter[F, Any]])
    })
    new Insert.Multi[F, P, Tuple](table, tuples, parameterBinders)

  inline def update[Tag <: Singleton, T](tag: Tag, value: T)(using
    mirror:                                   Mirror.ProductOf[P],
    index:                                    ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check: T =:= Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[F, P] =
    new Update[F, P](
      table   = table,
      columns = List(table.selectDynamic[Tag](tag).label),
      params = (value *: EmptyTuple).zip(Parameter.fold[F, T *: EmptyTuple]).toList.map {
        case (value: Any, parameter: Any) =>
          ParameterBinder[F, Any](value)(using parameter.asInstanceOf[Parameter[F, Any]])
      }
    )
