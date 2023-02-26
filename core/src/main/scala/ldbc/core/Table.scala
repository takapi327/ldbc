/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import scala.language.dynamics
import scala.deriving.Mirror

import ldbc.core.interpreter.*

/** Trait for generating SQL table information.
  *
  * @tparam P
  *   A class that implements a [[Product]] that is one-to-one with the table definition.
  */
private[ldbc] trait Table[P <: Product] extends Dynamic:

  /** Table name */
  private[ldbc] def name: String

  /** Table Key definitions */
  private[ldbc] def keyDefinitions: Seq[Key]

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
  ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]

  /** Method to retrieve an array of column information that a table has.
    */
  def selectDynamic(label: "*"): List[Tuple.Union[Tuple.Map[Any *: NonEmptyTuple, Column]]]

  def keys(func: Table[P] => Seq[Key]): Table[P]

object Table extends Dynamic:

  private case class Impl[P <: Product, T <: Tuple](
    name:           String,
    columns:        Tuple.Map[T, Column],
    keyDefinitions: Seq[Key]
  ) extends Table[P]:

    override def selectDynamic[Tag <: Singleton](
      tag: Tag
    )(using
      mirror: Mirror.ProductOf[P],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
      columns
        .productElement(index.value)
        .asInstanceOf[Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]

    override def selectDynamic(label: "*"): List[Tuple.Union[Tuple.Map[Any *: NonEmptyTuple, Column]]] =
      columns.toList.asInstanceOf[List[Tuple.Union[Tuple.Map[Any *: NonEmptyTuple, Column]]]]

    override def keys(func: Table[P] => Seq[Key]): Table[P] = this.copy(keyDefinitions = func(this))

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
    * @tparam P
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    */
  def applyDynamic[P <: Product](using
    mirror:    Mirror.ProductOf[P],
    converter: ColumnTupleConverter[mirror.MirroredElemTypes, Column]
  )(nameApply: "apply")(name: String)(columns: ColumnTuples[mirror.MirroredElemTypes, Column]): Table[P] =
    fromTupleMap[P](name, ColumnTupleConverter.convert(columns))

  /** Methods for generating a Table from a Column's Tuple Map.
    *
    * @param mirror
    *   product isomorphism map
    * @param _name
    *   Table name
    * @param columns
    *   Tuple of columns matching the Product's Elem type
    * @tparam P
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    */
  private def fromTupleMap[P <: Product](using
    mirror: Mirror.ProductOf[P]
  )(
    _name:   String,
    columns: Tuple.Map[mirror.MirroredElemTypes, Column]
  ): Table[P] = Impl[P, mirror.MirroredElemTypes](_name, columns, Seq.empty)
