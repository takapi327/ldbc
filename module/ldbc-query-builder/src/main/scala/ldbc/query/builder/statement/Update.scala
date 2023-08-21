/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import scala.deriving.Mirror

import ldbc.sql.*
import ldbc.core.interpreter.Tuples as CoreTuples

case class Update[F[_], P <: Product](
  table:   Table[P],
  columns: List[String],
  params:  Seq[ParameterBinder[F]]
) extends Command[F]:

  private val values = columns.map(column => s"$column = ?")

  override def statement: String = s"UPDATE ${ table._name } SET ${ values.mkString(", ") }"

  inline def set[Tag <: Singleton, T](tag: Tag, value: T)(using
    mirror:                                Mirror.ProductOf[P],
    index:                                 ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check: T =:= Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[F, P] =
    type Param = Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val param = ParameterBinder[F, Param](check(value))(using Parameter.infer[F, Param])
    this.copy(
      columns = columns :+ table.selectDynamic[Tag](tag).label,
      params  = params :+ param
    )

  inline def set[Tag <: Singleton, T](tag: Tag, value: Option[T])(using
    mirror:                                Mirror.ProductOf[P],
    index:                                 ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check: Option[T] =:= Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[F, P] =
    type Param = Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val param = ParameterBinder[F, Param](check(value))(using Parameter.infer[F, Param])
    this.copy(
      columns = columns :+ table.selectDynamic[Tag](tag).label,
      params  = params :+ param
    )

  def where(func: Table[P] => ExpressionSyntax[F]): Update.Where[F] =
    val expressionSyntax = func(table)
    Update.Where[F](
      _statement       = statement,
      expressionSyntax = expressionSyntax,
      params           = params ++ expressionSyntax.parameter
    )

object Update:

  case class Where[F[_]](
    _statement:       String,
    expressionSyntax: ExpressionSyntax[F],
    params:           Seq[ParameterBinder[F]]
  ) extends Command[F]:

    override def statement: String = _statement ++ s" WHERE ${ expressionSyntax.statement }"
