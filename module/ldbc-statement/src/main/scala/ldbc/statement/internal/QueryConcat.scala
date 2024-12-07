/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.internal

trait QueryConcat[A, B]:
  type Out

object QueryConcat extends LowPriorityTupleConcat2

trait LowPriorityTupleConcat2 extends LowPriorityTupleConcat1:
  given concatTuples[A, B, AB](using TupleOps.JoinAux[A, B, AB]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB

trait LowPriorityTupleConcat1 extends LowPriorityTupleConcat0:

  given concatSingleAndTuple[A, B, AB](using TupleOps.JoinAux[Tuple1[A], B, AB]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB

  given concatTupleAndSingle[A, B, AB](using TupleOps.JoinAux[A, Tuple1[B], AB]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB

trait LowPriorityTupleConcat0:

  type Aux[A, B, AB] = QueryConcat[A, B] { type Out = AB }

  given concatSingleAndSingle[A, B, AB](using TupleOps.JoinAux[Tuple1[A], Tuple1[B], AB]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
