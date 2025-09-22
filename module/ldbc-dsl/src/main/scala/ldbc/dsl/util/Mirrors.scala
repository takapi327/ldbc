/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.util

import scala.compiletime.{ constValue, erasedValue, error, summonInline }
import scala.deriving.Mirror

object Mirrors:

  /** A type function to pull a type parameter it has from a type with one type parameter. */
  type Extract[T] = T match
    case Option[t] => Extract[t]
    case Array[t]  => Extract[t]
    case List[t]   => Extract[t]
    case Seq[t]    => Extract[t]
    case Set[t]    => Extract[t]
    case _         => T

  /** A type function that derives its type from the type parameters that Option has. */
  type ExtractOption[T] = T match
    case Option[t] => Extract[t]
    case _         => T

  inline def summonLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t)   => constValue[h].toString :: summonLabels[t]

  inline def summonEnumCases[T <: Tuple, A](inline typeName: String): List[A] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t)   =>
        inline summonInline[Mirror.Of[h]] match
          case m: Mirror.Singleton => m.fromProduct(EmptyTuple).asInstanceOf[A] :: summonEnumCases[t, A](typeName)
          case m: Mirror           =>
            error(
              s"Cannot summon enum cases for type $typeName: ${ constValue[m.MirroredLabel] } is not a singleton mirror."
            )
