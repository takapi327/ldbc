package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

/**
 * The subclass of {@link SQLException} thrown when an error
 * occurs during a batch update operation.  In addition to the
 * information provided by {@link SQLException}, a
 * <code>BatchUpdateException</code> provides the update
 * counts for all commands that were executed successfully during the
 * batch update, that is, all commands that were executed before the error
 * occurred.  The order of elements in an array of update counts
 * corresponds to the order in which commands were added to the batch.
 * <P>
 * After a command in a batch update fails to execute properly
 * and a <code>BatchUpdateException</code> is thrown, the driver
 * may or may not continue to process the remaining commands in
 * the batch.  If the driver continues processing after a failure,
 * the array returned by the method
 * <code>BatchUpdateException.getUpdateCounts</code> will have
 * an element for every command in the batch rather than only
 * elements for the commands that executed successfully before
 * the error.  In the case where the driver continues processing
 * commands, the array element for any command
 * that failed is <code>Statement.EXECUTE_FAILED</code>.
 */
class BatchUpdateException(
                            sqlState: String,
                            vendorCode: Int,
                            message:          String,
                            updateCounts:     List[Int],
                            sql:              Option[String] = None,
                            detail:           Option[String] = None,
                            hint:             Option[String] = None,
                            originatedPacket: Option[String] = None
                          ) extends SQLException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket):

  override def getMessage: String =
    s"""
       |SQLState: $sqlState
       |Error Code: $vendorCode
       |Message: $message
       |Update Counts: [${updateCounts.mkString(",")}]
       |${sql.fold("")(s => s"\nSQL: $s")}
       |${ detail.fold("")(d => s"\nDetail: $d") }
       |${hint.fold("")(h => s"\nHint: $h")}
       |${ originatedPacket.fold("")(p => s"\nPoint of Origin: $p") }
       |""".stripMargin

  /**
   * Summarize error information into attributes.
   */
  override def fields: List[Attribute[?]] =
    val builder = List.newBuilder[Attribute[?]]

    builder += Attribute("error.message", message)
    builder += Attribute("error.sqlstate", sqlState)
    builder += Attribute("error.code", vendorCode.toLong)
    builder += Attribute("error.updateCounts", s"[${updateCounts.mkString(",")}]")

    sql.foreach(a => builder += Attribute("error.sql", a))
    detail.foreach(a => builder += Attribute("error.detail", a))
    hint.foreach(a => builder += Attribute("error.hint", a))
    originatedPacket.foreach(packet => builder += Attribute("error.originatedPacket", packet))

    builder.result()
