/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import scala.collection.immutable.ListMap

import cats.syntax.all.*

import org.typelevel.otel4s.Attribute

import ldbc.connector.data.Parameter
import ldbc.connector.util.Pretty

/**
 * <P>An exception that provides information on a database access
 * error or other errors.
 *
 * <P>Each <code>SQLException</code> provides several kinds of information:
 * <UL>
 *   <LI> a string describing the error.  This is used as the Java Exception
 *       message, available via the method <code>getMessage</code>.
 *   <LI> a "SQLstate" string, which follows either the XOPEN SQLstate conventions
 *        or the SQL:2003 conventions.
 *       The values of the SQLState string are described in the appropriate spec.
 *       The <code>DatabaseMetaData</code> method <code>getSQLStateType</code>
 *       can be used to discover whether the driver returns the XOPEN type or
 *       the SQL:2003 type.
 *   <LI> an integer error code that is specific to each vendor.  Normally this will
 *       be the actual error code returned by the underlying database.
 *   <LI> a chain to a next Exception.  This can be used to provide additional
 *       error information.
 *   <LI> the causal relationship, if any for this <code>SQLException</code>.
 * </UL>
 */
class SQLException(
  message:          String,
  sqlState:         Option[String] = None,
  vendorCode:       Option[Int]    = None,
  sql:              Option[String] = None,
  detail:           Option[String] = None,
  hint:             Option[String] = None,
  params:          ListMap[Int, Parameter] = ListMap.empty,
) extends Exception:

  /**
   * Summarize error information into attributes.
   */
  def fields: List[Attribute[?]] =
    val builder = List.newBuilder[Attribute[?]]

    builder += Attribute("error.message", message)

    sqlState.foreach(a => builder += Attribute("error.sqlstate", a))
    vendorCode.foreach(a => builder += Attribute("error.vendorCode", a.toLong))
    sql.foreach(a => builder += Attribute("error.sql", a))
    detail.foreach(a => builder += Attribute("error.detail", a))
    hint.foreach(a => builder += Attribute("error.hint", a))

    params.foreach { case (i, p) =>
      builder += Attribute(s"error.parameter.$i.type", p.columnDataType.name)
      builder += Attribute(s"error.parameter.$i.value", p.toString)
    }

    builder.result()

  protected def width = 80 // wrap here

  def labeled(label: String, s: String): String =
    if s.isEmpty then ""
    else
      "\n|" +
        label + Console.CYAN + Pretty.wrap(
        width - label.length,
        s,
        s"${Console.RESET}\n${Console.CYAN}" + label.map(_ => ' ')
      ) + Console.RESET

  protected def title: String = s"MySQL ERROR${vendorCode.fold("")(code => s" code $code")}${sqlState.fold("")(state => s" ($state)")}"

  protected def header: String =
    s"""|
          |$title
          |${labeled("  Problem: ", message)}${labeled("  Detail: ", detail.orEmpty)}${labeled("     Hint: ", hint.orEmpty)}
          |
          |""".stripMargin

  protected def statement: String =
    sql.foldMap { sql =>
      s"""|The statement under consideration is
          |
          |  ${Console.GREEN}$sql${Console.RESET}
          |
          |""".stripMargin
    }

  protected def args: String =

    def formatValue(s: String) =
      s"${Console.GREEN}$s${Console.RESET}"

    if params.isEmpty then ""
    else
      s"""|and the arguments were
          |
          |  ${params.map { case (i, p) => f"$$$i ${p.columnDataType.name}%-10s ${formatValue(p.toString)}" }.mkString("\n|  ")}
          |
          |""".stripMargin

  protected def sections: List[String] =
    List(header, statement, args)

  final override def getMessage =
    sections
      .combineAll
      .linesIterator
      .map("🔥  " + _)
      .mkString("\n", "\n", s"\n\n${getClass.getName}: $message")
