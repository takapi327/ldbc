/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The database-agnostic base for SQL exceptions. It carries the standard JDBC-style diagnostic fields
 * (SQL state, vendor error code, the offending SQL, and optional detail/hint) and renders a readable
 * message, without depending on any effect system or wire protocol — so it can be shared by the
 * transport, the connection pool, and every per-database connector. Per-driver flavour is added by
 * subclasses: they map the driver's native errors onto this hierarchy and may override [[vendor]] to
 * brand the rendered message.
 *
 * Bound parameter values are intentionally NOT part of this model: rendering them into a message (or
 * the telemetry [[fields]]) would leak potentially sensitive data into logs and traces.
 *
 * @param message    the human-readable error description
 * @param sqlState   the SQLSTATE code, if known
 * @param vendorCode the vendor-specific error code, if known
 * @param sql        the SQL statement that triggered the error, if applicable
 * @param detail     additional detail about the error
 * @param hint       a hint on how to resolve the error
 * @param vendor     the product name shown in the rendered message (e.g. `"MySQL"`)
 * @param cause      the underlying cause, if any
 */
class SQLException(
  message:        String,
  val sqlState:   Option[String]    = None,
  val vendorCode: Option[Int]       = None,
  val sql:        Option[String]    = None,
  val detail:     Option[String]    = None,
  val hint:       Option[String]    = None,
  vendor:         String            = "SQL",
  cause:          Option[Throwable] = None
) extends Exception(message, cause.orNull):

  /** The SQLSTATE for this exception, or the empty string if unknown. */
  def getSQLState: String = sqlState.getOrElse("")

  /** The vendor-specific error code for this exception, or `0` if unknown. */
  def getErrorCode: Int = vendorCode.getOrElse(0)

  /** Summarizes the error into database-agnostic telemetry attributes. */
  def fields: List[Attribute[?]] =
    val builder = List.newBuilder[Attribute[?]]

    builder += Attribute("error.message", message)

    sqlState.foreach(a => builder += Attribute("error.sqlstate", a))
    vendorCode.foreach(a => builder += Attribute("error.vendorCode", a.toLong))
    sql.foreach(a => builder += Attribute("error.sql", a))
    detail.foreach(a => builder += Attribute("error.detail", a))
    hint.foreach(a => builder += Attribute("error.hint", a))

    builder.result()

  protected def width = 80

  def labeled(label: String, s: String): String =
    if s.isEmpty then ""
    else
      "\n|" +
        label + Console.CYAN + Pretty.wrap(
          width - label.length,
          s,
          s"${ Console.RESET }\n${ Console.CYAN }" + label.map(_ => ' ')
        ) + Console.RESET

  protected def title: String =
    s"$vendor ERROR${ vendorCode.fold("")(code => s" code $code") }${ sqlState.fold("")(state => s" ($state)") }"

  protected def header: String =
    s"""|
        |$title
        |${ labeled("  Problem: ", message) }${ labeled("  Detail: ", detail.getOrElse("")) }${ labeled(
         "     Hint: ",
         hint.getOrElse("")
       ) }
        |
        |""".stripMargin

  protected def statement: String =
    sql.fold("") { sql =>
      s"""|The statement under consideration is
          |
          |  ${ Console.GREEN }$sql${ Console.RESET }
          |
          |""".stripMargin
    }

  protected def sections: List[String] =
    List(header, statement)

  final override def getMessage: String =
    sections.mkString.linesIterator
      .map("🔥  " + _)
      .mkString("\n", "\n", s"\n\n${ getClass.getName }: $message")
