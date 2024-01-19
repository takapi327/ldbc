/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.core.interpreter

/** Column Type representing the conversion from Tuple to Tuple Map.
  *
  * @tparam Types
  *   Primitive Tuples
  * @tparam F
  *   Column Type
  */
opaque type ColumnTupleConverter[Types <: Tuple, F[_]] =
  ColumnTuples[Types, F] => Tuple.Map[Types, F]

/** An object that converts a Column's Tuple to a Tuple Map
  */
object ColumnTupleConverter:

  /** Implicit value of ColumnTupleConverter according to the number of Tuples. */

  given [T, F[_]]: ColumnTupleConverter[T *: EmptyTuple, F] = (column: F[T]) => column *: EmptyTuple

  given [T1, T2, F[_]]: ColumnTupleConverter[(T1, T2), F] = identity

  given [T1, T2, TN <: NonEmptyTuple, F[_]](using
    converter: ColumnTupleConverter[T2 *: TN, F]
  ): ColumnTupleConverter[T1 *: T2 *: TN, F] =
    (columns: F[T1] *: ColumnTuples[T2 *: TN, F]) => columns.head *: converter(columns.tail)

  /** Method for converting Column Tuple to Tuple Map.
    *
    * @param columnTuples
    *   Tuple of Columns
    * @param converter
    *   An object that converts a Column's Tuple to a Tuple Map
    * @tparam Types
    *   Primitive Tuples
    * @tparam F
    *   The effect type
    */
  def convert[Types <: Tuple, F[_]](columnTuples: ColumnTuples[Types, F])(using
    converter: ColumnTupleConverter[Types, F]
  ): Tuple.Map[Types, F] =
    converter(columnTuples)
