/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql.interpreter

import ldbc.sql.Column

/**
 * Column Type representing the conversion from Tuple to Tuple Map.
 *
 * @tparam Types
 *   Primitive Tuples
 * @tparam F
 *   The effect type
 */
opaque type ColumnTupleConverter[Types <: Tuple, F[_]] = ColumnTuples[Types, F] => Tuple.Map[Types, [T] =>> Column[F, T]]

/**
 * An object that converts a Column's Tuple to a Tuple Map
 */
object ColumnTupleConverter:

  /** Implicit value of ColumnTupleConverter according to the number of Tuples. */

  given [T, F[_]]: ColumnTupleConverter[T *: EmptyTuple, F] = (column: Column[F, T]) => column *: EmptyTuple
  given [T1, T2, F[_]]: ColumnTupleConverter[(T1, T2), F] = identity
  given [T1, T2, TN <: NonEmptyTuple, F[_]](
    using converter: ColumnTupleConverter[T2 *: TN, F]
  ): ColumnTupleConverter[T1 *: T2 *: TN, F] =
    (columns: Column[F, T1] *: ColumnTuples[T2 *: TN, F]) =>
      columns.head *: converter(columns.tail).asInstanceOf[Column[F, T2] *: Tuple.Map[TN, [T] =>> Column[F, T]]]

  /**
   * Method for converting Column Tuple to Tuple Map.
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
  def convert[Types <: Tuple, F[_]](columnTuples: ColumnTuples[Types, F])(
    using converter: ColumnTupleConverter[Types, F]
  ): Tuple.Map[Types, [T] =>> Column[F, T]] = converter(columnTuples)
