/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import scala.language.dynamics
import scala.deriving.Mirror
import scala.annotation.targetName

import ldbc.sql.interpreter.*

/** Trait for generating SQL table information.
  *
  * @tparam F
  *   The effect type
  * @tparam P
  *   A class that implements a [[Product]] that is one-to-one with the table definition.
  */
private[ldbc] trait Table[F[_], P <: Product] extends Dynamic:

  /** Table name
    */
  private[ldbc] def name: String

  /** Methods for statically accessing column information held by a Table.
    *
    * @param tag
    *   A type with a single instance. Here, Column is passed.
    * @param mirror
    *   product isomorphism map
    * @param index
    *   Position of the specified type in tuple X
    * @tparam Tag
    *   Type with a single instance
    */
  def selectDynamic[Tag <: Singleton](
    tag: Tag
  )(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Column[F, Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]

  /** Method to retrieve an array of column information that a table has.
    */
  @targetName("allColumn") def * : List[Column[F, Any]]

object Table extends Dynamic:

  /** Methods for static Table construction using Dynamic.
    *
    * @param nameApply
    *   The apply method
    * @param mirror
    *   product isomorphism map
    * @param converter
    *   An object that converts a Column's Tuple to a Tuple Map
    * @param name
    *   Table name
    * @param columns
    *   Tuple of columns matching the Product's Elem type
    * @tparam F
    *   The effect type
    * @tparam P
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    */
  def applyDynamic[F[_], P <: Product](using
    mirror:    Mirror.ProductOf[P],
    converter: ColumnTupleConverter[mirror.MirroredElemTypes, F]
  )(nameApply: "apply")(name: String)(columns: ColumnTuples[mirror.MirroredElemTypes, F]): Table[F, P] =
    fromTupleMap[F, P](name, ColumnTupleConverter.convert(columns))

  /** Methods for generating a Table from a Column's Tuple Map.
    *
    * @param mirror
    *   product isomorphism map
    * @param _name
    *   Table name
    * @param columns
    *   Tuple of columns matching the Product's Elem type
    * @tparam F
    *   The effect type
    * @tparam P
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    */
  private def fromTupleMap[F[_], P <: Product](using
    mirror: Mirror.ProductOf[P]
  )(
    _name:   String,
    columns: Tuple.Map[mirror.MirroredElemTypes, [T] =>> Column[F, T]]
  ): Table[F, P] = new Table[F, P]:

    override private[ldbc] def name: String = _name

    override def selectDynamic[Tag <: Singleton](
      tag: Tag
    )(using
      mirror: Mirror.ProductOf[P],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ): Column[F, Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
      columns
        .productElement(index.value)
        .asInstanceOf[Column[F, Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]

    @targetName("allColumn") def * : List[Column[F, Any]] = columns.toList.asInstanceOf[List[Column[F, Any]]]
