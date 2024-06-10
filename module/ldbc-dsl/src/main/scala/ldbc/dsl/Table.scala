/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import scala.language.dynamics
import scala.deriving.Mirror
import scala.compiletime.*
import scala.compiletime.ops.int.*
import scala.annotation.targetName
import ldbc.dsl.interpreter.*
import ldbc.dsl.statement.Select

import scala.Tuple.Elem

/**
 * Trait for generating SQL table information.
 *
 * @tparam P
 *   A class that implements a [[Product]] that is one-to-one with the table definition.
 */
trait Table[P] extends Dynamic:

  def name: String
  
  type Columns <: Tuple
  @targetName("all")
  def * : Columns

  transparent inline def selectDynamic[Tag <: Singleton](
    tag: Tag
  )(using
    mirror: Mirror.ProductOf[P],
    index: ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
    *
      .productElement(index.value)
      .asInstanceOf[Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]

  def select[T](func: Table[P] => T): Select[P, T] =
    val columns = func(this)
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v => v
    val statement = s"SELECT $str FROM $name"
    Select(this, statement, columns, Nil)

object Table:

  def apply[P](using t: Table[P]): Table[P] = t

  private inline def buildColumns[NT <: Tuple, T <: Tuple, I <: Int](inline nt: NT, inline xs: List[Column[?]]): Tuple.Map[T, Column] =
    inline nt match
      case nt1: (e *: ts)  =>
        inline nt1.head match
          case h: String =>
            val c = Column.Impl[Tuple.Elem[T, I]](h, None)
            buildColumns[ts, T, I + 1](nt1.tail, xs :+ c)
          case n: (name, _)                 =>
            error("stat " + constValue[name] + " should be a constant integer")
      case _: EmptyTuple => Tuple.fromArray(xs.toArray).asInstanceOf[Tuple.Map[T, Column]]

  inline def derived[P](using m: Mirror.ProductOf[P]): Table[P] =
    val labels = constValueTuple[m.MirroredElemLabels]
    new:
      override def name: String = constValue[m.MirroredLabel]
      override type Columns = Tuple.Map[m.MirroredElemTypes, Column]
      @targetName("all")
      override def * : Columns = buildColumns[m.MirroredElemLabels, m.MirroredElemTypes, 0](labels, Nil)
