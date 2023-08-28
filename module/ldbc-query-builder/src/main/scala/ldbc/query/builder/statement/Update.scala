/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import scala.deriving.Mirror

import ldbc.sql.*
import ldbc.core.interpreter.Tuples as CoreTuples

/** A model for constructing UPDATE statements in MySQL.
  *
  * @param table
  *   Trait for generating SQL table information.
  * @param columns
  *   Column name list
  * @param params
  *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
  *   only.
  * @tparam F
  *   The effect type
  * @tparam P
  *   Base trait for all products
  */
case class Update[F[_], P <: Product](
  table:   Table[P],
  columns: List[String],
  params:  Seq[ParameterBinder[F]]
) extends Command[F],
          Update.LimitProvider[F]:

  private val values = columns.map(column => s"$column = ?")

  override def statement: String = s"UPDATE ${ table._name } SET ${ values.mkString(", ") }"

  /** A method that sets additional values to be updated in the query model that updates specific columns defined in the
    * table.
    *
    * @param tag
    *   A type with a single instance. Here, Column is passed.
    * @param value
    *   A value of type T to be inserted into the specified column.
    * @param mirror
    *   product isomorphism map
    * @param index
    *   Position of the specified type in tuple X
    * @param check
    *   A value to verify that the specified type matches the type of the specified column that the Table has.
    * @tparam Tag
    *   Type with a single instance
    * @tparam T
    *   Scala types that match SQL DataType
    */
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

  /** A method that sets additional values to be updated in the query model that updates specific columns defined in the
    * table.
    *
    * @param tag
    *   A type with a single instance. Here, Column is passed.
    * @param value
    *   A value of type T to be inserted into the specified column.
    * @param mirror
    *   product isomorphism map
    * @param index
    *   Position of the specified type in tuple X
    * @param check
    *   A value to verify that the specified type matches the type of the specified column that the Table has.
    * @tparam Tag
    *   Type with a single instance
    * @tparam T
    *   Scala types that match SQL DataType
    */
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

  /** A method for setting the WHERE condition in a UPDATE statement.
    *
    * @param func
    *   Function to construct an expression using the columns that Table has.
    */
  def where(func: Table[P] => ExpressionSyntax[F]): Update.Where[F] =
    val expressionSyntax = func(table)
    Update.Where[F](
      _statement       = statement,
      expressionSyntax = expressionSyntax,
      params           = params ++ expressionSyntax.parameter
    )

object Update:

  /** A model for constructing WHERE statements in MySQL.
    *
    * @param _statement
    *   SQL statement string
    * @param expressionSyntax
    *   Trait for the syntax of expressions available in MySQL.
    * @param params
    *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
    *   only.
    * @tparam F
    *   The effect type
    */
  case class Where[F[_]](
    _statement:       String,
    expressionSyntax: ExpressionSyntax[F],
    params:           Seq[ParameterBinder[F]]
  ) extends Command[F],
            LimitProvider[F]:

    override def statement: String = _statement ++ s" WHERE ${ expressionSyntax.statement }"

  /** @param _statement
    *   SQL statement string
    * @param params
    *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
    *   only.
    * @tparam F
    *   The effect type
    */
  case class Limit[F[_]](
    _statement: String,
    params:     Seq[ParameterBinder[F]]
  ) extends Command[F]:

    override def statement: String = _statement ++ " LIMIT ?"

  /** Transparent Trait to provide limit method.
    *
    * @tparam F
    *   The effect type
    */
  private[ldbc] transparent trait LimitProvider[F[_]]:
    self: Command[F] =>

    /** A method for setting the LIMIT condition in a statement.
      *
      * @param length
      *   Upper limit to be updated
      */
    def limit(length: Long): Parameter[F, Long] ?=> Update.Limit[F] =
      Update.Limit[F](
        _statement = statement,
        params     = params :+ ParameterBinder(length)
      )
