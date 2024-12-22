/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.interpreter

import scala.compiletime.ops.int.S

/**
 * An object with methods and types that perform processing on one or more informational Tuples.
 */
object Tuples:

  /**
   * Type for obtaining location information inside the Tuple of the specified type.
   */
  type IndexOf[T <: Tuple, E] <: Int = T match
    case E *: _  => 0
    case _ *: es => S[IndexOf[es, E]]
