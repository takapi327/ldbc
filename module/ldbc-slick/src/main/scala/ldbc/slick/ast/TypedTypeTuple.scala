/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick.ast

import slick.ast.TypedType

type TypedTypeTuple[T <: Tuple] <: Tuple = T match
  case EmptyTuple => EmptyTuple
  case h *: ts    => TypedType[h] *: TypedTypeTuple[ts]

object TypedTypeTuple:

  inline def infer[T]: TypedType[T] =
    scala.compiletime.summonFrom[TypedType[T]] {
      case typedType: TypedType[T] => typedType
      case _                       => scala.compiletime.error("error")
    }

  inline def fold[T <: Tuple]: TypedTypeTuple[T] =
    inline scala.compiletime.erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (h *: ts)  => infer[h] *: fold[ts]
