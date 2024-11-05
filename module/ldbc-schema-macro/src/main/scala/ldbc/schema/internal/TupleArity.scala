/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.internal

trait TupleArity[A]:
  def arity: Int

object TupleArity extends LowPriorityTupleArity:
  implicit def tupleArity2[A1, A2]: TupleArity[(A1, A2)] = new TupleArity[(A1, A2)]:
    def arity: Int = 2
  implicit def tupleArity3[A1, A2, A3]: TupleArity[(A1, A2, A3)] = new TupleArity[(A1, A2, A3)]:
    def arity: Int = 3
  implicit def tupleArity4[A1, A2, A3, A4]: TupleArity[(A1, A2, A3, A4)] = new TupleArity[(A1, A2, A3, A4)]:
    def arity: Int = 4
  implicit def tupleArity5[A1, A2, A3, A4, A5]: TupleArity[(A1, A2, A3, A4, A5)] = new TupleArity[(A1, A2, A3, A4, A5)]:
    def arity: Int = 5
  implicit def tupleArity6[A1, A2, A3, A4, A5, A6]: TupleArity[(A1, A2, A3, A4, A5, A6)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6)]:
      def arity: Int = 6
  implicit def tupleArity7[A1, A2, A3, A4, A5, A6, A7]: TupleArity[(A1, A2, A3, A4, A5, A6, A7)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7)]:
      def arity: Int = 7
  implicit def tupleArity8[A1, A2, A3, A4, A5, A6, A7, A8]: TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8)]:
      def arity: Int = 8
  implicit def tupleArity9[A1, A2, A3, A4, A5, A6, A7, A8, A9]: TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9)]:
      def arity: Int = 9
  implicit def tupleArity10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)]:
      def arity: Int = 10
  implicit def tupleArity11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)]:
      def arity: Int = 11
  implicit def tupleArity12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)]:
      def arity: Int = 12
  implicit def tupleArity13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)]:
      def arity: Int = 13
  implicit def tupleArity14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)]:
      def arity: Int = 14
  implicit def tupleArity15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)]:
      def arity: Int = 15
  implicit def tupleArity16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)]:
      def arity: Int = 16
  implicit def tupleArity17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)]:
      def arity: Int = 17
  implicit def tupleArity18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)]:
      def arity: Int = 18
  implicit def tupleArity19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)]:
      def arity: Int = 19
  implicit def tupleArity20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20]
    : TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)]:
      def arity: Int = 20
  implicit def tupleArity21[
    A1,
    A2,
    A3,
    A4,
    A5,
    A6,
    A7,
    A8,
    A9,
    A10,
    A11,
    A12,
    A13,
    A14,
    A15,
    A16,
    A17,
    A18,
    A19,
    A20,
    A21
  ]: TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)] =
    new TupleArity[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)]:
      def arity: Int = 21

trait LowPriorityTupleArity:
  implicit def tupleArity1[A]: TupleArity[A] = new TupleArity[A]:
    def arity: Int = 1
