/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.internal

trait QueryConcat[A, B] extends BinaryTupleOp:
  type Out

object QueryConcat extends LowPriorityTupleConcat2

trait LowPriorityTupleConcat2 extends LowPriorityTupleConcat1:
  given concatTuples[A, B, AB](using
    to: TupleOps.JoinAux[A, B, AB],
    ta: TupleArity[A],
    tb: TupleArity[B]
  ): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity:  Int = ta.arity
      override def rightArity: Int = tb.arity

trait LowPriorityTupleConcat1 extends LowPriorityTupleConcat0:

  given concatSingleAndTuple[A, B, AB](using to: TupleOps.JoinAux[Tuple1[A], B, AB], ta: TupleArity[B]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity:  Int = 1
      override def rightArity: Int = ta.arity

  given concatTupleAndSingle[A, B, AB](using tc: TupleOps.JoinAux[A, Tuple1[B], AB], ta: TupleArity[A]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity:  Int = ta.arity
      override def rightArity: Int = 1

trait LowPriorityTupleConcat0:

  type Aux[A, B, AB] = QueryConcat[A, B] { type Out = AB }

  given concatSingleAndSingle[A, B, AB](using TupleOps.JoinAux[Tuple1[A], Tuple1[B], AB]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity:  Int = 1
      override def rightArity: Int = 1
