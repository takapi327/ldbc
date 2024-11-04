/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.internal

trait QueryConcat[A, B] extends BinaryTupleOp:
  type Out

object QueryConcat extends LowPriorityTupleConcat2

/*
object QueryConcat extends LowPriorityTupleConcat4:

  given concatUnitUnit[B]: Aux[Unit, Unit, Unit] =
    new QueryConcat[Unit, Unit]:
      override type Out = Unit
      override def leftArity = 0
      override def rightArity = 0

  given concatNothingNothing[U]: Aux[Nothing, Nothing, Nothing] =
    new QueryConcat[Nothing, Nothing]:
      override type Out = Nothing
      override def leftArity = 0
      override def rightArity = 0

  given concatNothingUnit[B]: Aux[Nothing, Unit, Unit] =
    new QueryConcat[Nothing, Unit]:
      override type Out = Unit
      override def leftArity = 0
      override def rightArity = 0

  given concatUnitNothing[B]: Aux[Unit, Nothing, Unit] =
    new QueryConcat[Unit, Nothing]:
      override type Out = Unit
      override def leftArity = 0
      override def rightArity = 0

trait LowPriorityTupleConcat4 extends LowPriorityTupleConcat3:
  given concatUnitLeft[B](using ta: TupleArity[B]): Aux[Unit, B, B] =
    new QueryConcat[Unit, B]:
      override type Out = B
      override def leftArity: Int = 0
      override def rightArity: Int = ta.arity

  given concatNothingLeft[B](using ta: TupleArity[B]): Aux[Nothing, B, B] =
    new QueryConcat[Nothing, B]:
      override type Out = B
      override def leftArity: Int = 0
      override def rightArity: Int = ta.arity

trait LowPriorityTupleConcat3 extends LowPriorityTupleConcat2:
  given concatUnitRight[A](using ta: TupleArity[A]): Aux[A, Unit, A] =
    new QueryConcat[A, Unit]:
      override type Out = A
      override def leftArity: Int = ta.arity
      override def rightArity: Int = 0

  // for void outputs
  given concatNothingRight[A](using ta: TupleArity[A]): Aux[A, Nothing, A] =
    new QueryConcat[A, Nothing]:
      override type Out = A
      override def leftArity: Int = ta.arity
      override def rightArity: Int = 0
*/

trait LowPriorityTupleConcat2 extends LowPriorityTupleConcat1:
  given concatTuples[A, B, AB](using to: TupleOps.JoinAux[A, B, AB], ta: TupleArity[A], tb: TupleArity[B]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity: Int = ta.arity
      override def rightArity: Int = tb.arity

trait LowPriorityTupleConcat1 extends LowPriorityTupleConcat0:

  given concatSingleAndTuple[A, B, AB](using to: TupleOps.JoinAux[Tuple1[A], B, AB], ta: TupleArity[B]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity: Int = 1
      override def rightArity: Int = ta.arity

  given concatTupleAndSingle[A, B, AB](using tc: TupleOps.JoinAux[A, Tuple1[B], AB], ta: TupleArity[A]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity: Int = ta.arity
      override def rightArity: Int = 1

trait LowPriorityTupleConcat0:

  type Aux[A, B, AB] = QueryConcat[A, B] { type Out = AB }

  given concatSingleAndSingle[A, B, AB](using TupleOps.JoinAux[Tuple1[A], Tuple1[B], AB]): Aux[A, B, AB] =
    new QueryConcat[A, B]:
      override type Out = AB
      override def leftArity: Int = 1
      override def rightArity: Int = 1
