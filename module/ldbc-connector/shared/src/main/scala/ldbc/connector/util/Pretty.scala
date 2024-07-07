/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

/**
 * copied from skunk:
 * https://github.com/typelevel/skunk/blob/main/modules/core/shared/src/main/scala/util/Pretty.scala
 */
object Pretty:

  def wrap(w: Int, s: String, delim: String = "\n"): String =
    if w >= s.length then s
    else
      s.lastIndexWhere(_ == ' ', w) match
        case -1 => wrap(w + 1, s, delim)
        case n =>
          val (s1, s2) = s.splitAt(n)
          s1 + delim + wrap(w, s2.trim, delim)
