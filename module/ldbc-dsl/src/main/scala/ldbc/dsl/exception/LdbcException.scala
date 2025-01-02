/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.exception

import cats.syntax.all.*

/**
 * The superclass of all exceptions thrown by the ldbc DSL.
 */
class LdbcException(
  message: String,
  detail:  Option[String] = None,
  hint:    Option[String] = None
) extends Exception:

  def title: String = "ldbc Exception"

  protected def width = 80 // wrap here
  private def wrap(w: Int, s: String, delim: String = "\n"): String =
    if w >= s.length then s
    else
      s.lastIndexWhere(_ == ' ', w) match
        case -1 => wrap(w + 1, s, delim)
        case n =>
          val (s1, s2) = s.splitAt(n)
          s1 + delim + wrap(w, s2.trim, delim)

  protected def labeled(label: String, s: String): String =
    if s.isEmpty then ""
    else
      "\n|" +
        label + Console.CYAN + wrap(
          width - label.length,
          s,
          s"${ Console.RESET }\n${ Console.CYAN }" + label.map(_ => ' ')
        ) + Console.RESET

  protected def header: String = title

  protected def body: String =
    s"""
       |${ labeled("  Problem: ", message) }${ detail.map(str => "\n|  Detail: " + str).getOrElse("") }${ hint
        .map(str => "\n|    Hint: " + str)
        .getOrElse("") }
       |""".stripMargin

  protected def sections: List[String] =
    List(header, body)

  final override def getMessage: String =
    sections.combineAll.linesIterator
      .map("🔥  " + _)
      .mkString("\n", "\n", s"\n\n${ getClass.getName }: $message")
