/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

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
  sqlState: String,
  vendorCode: Int,
  message:          String,
  sql:              Option[String] = None,
  detail:           Option[String] = None,
  hint:             Option[String] = None,
  originatedPacket: Option[String] = None
) extends Exception:
  
  override def getMessage: String =
    s"""
       |SQLState: $sqlState
       |Error Code: $vendorCode
       |Message: $message
       |${sql.fold("")(s => s"\nSQL: $s")}
       |${ detail.fold("")(d => s"\nDetail: $d") }
       |${hint.fold("")(h => s"\nHint: $h")}
       |${ originatedPacket.fold("")(p => s"\nPoint of Origin: $p") }
       |""".stripMargin

  /**
   * Retrieves the SQLState for this <code>SQLException</code> object.
   *
   * @return the SQLState value
   */
  def getSQLState: String = sqlState

  /**
   * Retrieves the vendor-specific exception code
   * for this <code>SQLException</code> object.
   *
   * @return the vendor's error code
   */
  def getErrorCode: Int = vendorCode

  /**
   * Summarize error information into attributes.
   */
  def fields: List[Attribute[?]] =
    val builder = List.newBuilder[Attribute[?]]

    builder += Attribute("error.message", message)
    builder += Attribute("error.sqlstate", sqlState)
    builder += Attribute("error.code", vendorCode.toLong)

    sql.foreach(a => builder += Attribute("error.sql", a))
    detail.foreach(a => builder += Attribute("error.detail", a))
    hint.foreach(a => builder += Attribute("error.hint", a))
    originatedPacket.foreach(packet => builder += Attribute("error.originatedPacket", packet))

    builder.result()
