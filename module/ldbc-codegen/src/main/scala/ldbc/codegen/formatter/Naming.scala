/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.codegen.formatter

import scala.annotation.tailrec

/** Enum of naming rules
  */
enum Naming:
  case CAMEL, PASCAL, SNAKE, KEBAB

object Naming:

  def fromString(str: String): Naming =
    str match
      case "CAMEL"  => CAMEL
      case "PASCAL" => PASCAL
      case "SNAKE"  => SNAKE
      case "KEBAB"  => KEBAB
      case unknown =>
        throw new IllegalArgumentException(
          s"$unknown does not match any of the Naming, it must be CAMEL, PASCAL, SNAKE, or KEBAB."
        )

  extension (`case`: Naming)
    def format(name: String): String =
      `case` match
        case CAMEL  => toCamel(name)
        case PASCAL => toPascal(name)
        case SNAKE  => toSnake(name)
        case KEBAB  => toKebab(name)

  /** Converts to camelCase e.g.: PascalCase => pascalCase
    *
    * @param name
    *   name to be converted to camelCase
    * @return
    *   camelCase version of the string passed
    */
  def toCamel(name: String): String =
    toSnake(name).split("_").toList match
      case Nil          => name
      case head :: tail => head + tail.map(v => s"${ v.charAt(0).toUpper }${ v.drop(1) }").mkString

  /** Converts to PascalCase e.g.: camelCase => CamelCase
    *
    * @param name
    *   name to be converted to PascalCase
    * @return
    *   PascalCase version of the string passed
    */
  def toPascal(name: String): String =
    val list = toSnake(name).split("_").toList
    if list.nonEmpty && !(list.size == 1 && list.head == "") then
      list.map(v => s"${ v.charAt(0).toUpper }${ v.drop(1) }").mkString
    else name

  /** Converts to snake_case e.g.: camelCase => camel_case
    *
    * @param name
    *   name to be converted to snake_case
    * @return
    *   snake_case version of the string passed
    */
  def toSnake(name: String): String =
    @tailrec def go(accDone: List[Char], acc: List[Char]): List[Char] = acc match
      case Nil                                                        => accDone
      case a :: b :: c :: tail if a.isUpper && b.isUpper && c.isLower => go(accDone ++ List(a, '_', b, c), tail)
      case a :: b :: tail if a.isLower && b.isUpper                   => go(accDone ++ List(a, '_', b), tail)
      case a :: tail                                                  => go(accDone :+ a, tail)
    go(Nil, name.toList).mkString.toLowerCase.replaceAll("-", "_")

  /** Converts to kebab-case e.g.: camelCase => camel-case
    *
    * @param name
    *   name to be converted to kebab-case
    * @return
    *   kebab-case version of the string passed
    */
  def toKebab(name: String): String =
    @tailrec def go(accDone: List[Char], acc: List[Char]): List[Char] = acc match
      case Nil                                                        => accDone
      case a :: b :: c :: tail if a.isUpper && b.isUpper && c.isLower => go(accDone ++ List(a, '-', b, c), tail)
      case a :: b :: tail if a.isLower && b.isUpper                   => go(accDone ++ List(a, '-', b), tail)
      case a :: tail                                                  => go(accDone :+ a, tail)
    go(Nil, name.toList).mkString.toLowerCase.replaceAll("_", "-")
