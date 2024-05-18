/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import scala.collection.immutable.ListMap

import cats.*
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{ Tracer, Span }

import ldbc.sql.ResultSet

import ldbc.connector.LdbcResultSet
import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.Protocol
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

/**
 * <P>The object used for executing a static SQL statement
 * and returning the results it produces.
 * <P>
 * By default, only one <code>ResultSet</code> object per <code>Statement</code>
 * object can be open at the same time. Therefore, if the reading of one
 * <code>ResultSet</code> object is interleaved
 * with the reading of another, each must have been generated by
 * different <code>Statement</code> objects. All execution methods in the
 * <code>Statement</code> interface implicitly close a current
 * <code>ResultSet</code> object of the statement if an open one exists.
 * 
 * @tparam F
 *   The effect type
 */
trait Statement[F[_]]:

  /**
   * Holds batched commands
   */
  def batchedArgs: Ref[F, Vector[String]]

  /**
   * Executes the given SQL statement, which returns a single
   * <code>ResultSet</code> object.
   *<p>
   * <strong>Note:</strong>This method cannot be called on a
   * <code>PreparedStatement</code> or <code>CallableStatement</code>.
   * @param sql an SQL statement to be sent to the database, typically a
   *        static SQL <code>SELECT</code> statement
   * @return a <code>ResultSet</code> object that contains the data produced
   *         by the given query; never <code>null</code>
   */
  def executeQuery(sql: String): F[ResultSet[F]]

  /**
   * Executes the given SQL statement, which may be an <code>INSERT</code>,
   * <code>UPDATE</code>, or <code>DELETE</code> statement or an
   * SQL statement that returns nothing, such as an SQL DDL statement.
   *<p>
   * <strong>Note:</strong>This method cannot be called on a
   * <code>PreparedStatement</code> or <code>CallableStatement</code>.
   * @param sql an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
   * <code>DELETE</code>; or an SQL statement that returns nothing,
   * such as a DDL statement.
   *
   * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
   *         or (2) 0 for SQL statements that return nothing
   */
  def executeUpdate(sql: String): F[Int]

  /**
   * Releases this <code>Statement</code> object's database
   * and JDBC resources immediately instead of waiting for
   * this to happen when it is automatically closed.
   * It is generally good practice to release resources as soon as
   * you are finished with them to avoid tying up database
   * resources.
   * <P>
   * Calling the method <code>close</code> on a <code>Statement</code>
   * object that is already closed has no effect.
   * <P>
   * <B>Note:</B>When a <code>Statement</code> object is
   * closed, its current <code>ResultSet</code> object, if one exists, is
   * also closed.
   */
  def close(): F[Unit]

  /**
   * Executes the given SQL statement, which may return multiple results.
   * In some (uncommon) situations, a single SQL statement may return
   * multiple result sets and/or update counts.  Normally you can ignore
   * this unless you are (1) executing a stored procedure that you know may
   * return multiple results or (2) you are dynamically executing an
   * unknown SQL string.
   * <P>
   * The <code>execute</code> method executes an SQL statement and indicates the
   * form of the first result.  You must then use the methods
   * <code>getResultSet</code> or <code>getUpdateCount</code>
   * to retrieve the result, and <code>getMoreResults</code> to
   * move to any subsequent result(s).
   * <p>
   *<strong>Note:</strong>This method cannot be called on a
   * <code>PreparedStatement</code> or <code>CallableStatement</code>.
   * @param sql any SQL statement
   * @return <code>true</code> if the first result is a <code>ResultSet</code>
   *         object; <code>false</code> if it is an update count or there are
   *         no results
   */
  def execute(sql: String): F[Boolean]

  /**
   *  Retrieves the current result as a <code>ResultSet</code> object.
   *  This method should be called only once per result.
   *
   * @return the current result as a <code>ResultSet</code> object or
   * <code>None</code> if the result is an update count or there are no more results
   */
  def getResultSet(): F[Option[ResultSet[F]]]

  /**
   *  Retrieves the current result as an update count;
   *  if the result is a <code>ResultSet</code> object or there are no more results, -1
   *  is returned. This method should be called only once per result.
   *
   * @return the current result as an update count; -1 if the current result is a
   * <code>ResultSet</code> object or there are no more results
   */
  def getUpdateCount(): F[Int]

  /**
   * Moves to this <code>Statement</code> object's next result, returns
   * <code>true</code> if it is a <code>ResultSet</code> object, and
   * implicitly closes any current <code>ResultSet</code>
   * object(s) obtained with the method <code>getResultSet</code>.
   *
   * <P>There are no more results when the following is true:
   * {{{
   *   ((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
   * }}}
   *
   * @return <code>true</code> if the next result is a <code>ResultSet</code>
   *         object; <code>false</code> if it is an update count or there are
   *         no more results
   */
  def getMoreResults(): F[Boolean]

  /**
   * Adds the given SQL command to the current list of commands for this
   * <code>Statement</code> object. The commands in this list can be
   * executed as a batch by calling the method <code>executeBatch</code>.
   * <P>
   * <strong>Note:</strong>This method cannot be called on a
   * <code>PreparedStatement</code> or <code>CallableStatement</code>.
   * @param sql typically this is a SQL <code>INSERT</code> or
   * <code>UPDATE</code> statement
   */
  def addBatch(sql: String): F[Unit] = batchedArgs.update(_ :+ sql)

  /**
   * Empties this Statement object's current list of SQL commands.
   */
  def clearBatch(): F[Unit] = batchedArgs.set(Vector.empty)

  /**
   * Submits a batch of commands to the database for execution and if all commands execute successfully, returns an array of update counts.
   * The int elements of the array that is returned are ordered to correspond to the commands in the batch, which are ordered according to the order in which they were added to the batch.
   * The elements in the array returned by the method executeBatch may be one of the following:
   *
   * <OL>
   * <LI>A number greater than or equal to zero -- indicates that the
   * command was processed successfully and is an update count giving the
   * number of rows in the database that were affected by the command's
   * execution
   * <LI>A value of <code>SUCCESS_NO_INFO</code> -- indicates that the command was
   * processed successfully but that the number of rows affected is
   * unknown
   * <P>
   * If one of the commands in a batch update fails to execute properly,
   * this method throws a <code>BatchUpdateException</code>, and a JDBC
   * driver may or may not continue to process the remaining commands in
   * the batch.  However, the driver's behavior must be consistent with a
   * particular DBMS, either always continuing to process commands or never
   * continuing to process commands.  If the driver continues processing
   * after a failure, the array returned by the method
   * <code>BatchUpdateException.getUpdateCounts</code>
   * will contain as many elements as there are commands in the batch, and
   * at least one of the elements will be the following:
   *
   * <LI>A value of <code>EXECUTE_FAILED</code> -- indicates that the command failed
   * to execute successfully and occurs only if a driver continues to
   * process commands after a command fails
   * </OL>
   *
   * @return
   *   an array of update counts containing one element for each command in the batch. The elements of the array are ordered according to the order in which commands were added to the batch.
   */
  def executeBatch(): F[List[Int]]

  /**
   * Retrieves any auto-generated keys created as a result of executing this
   * <code>Statement</code> object. If this <code>Statement</code> object did
   * not generate any keys, an empty <code>ResultSet</code>
   * object is returned.
   *
   *<p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
   * the JDBC driver implementation will determine the columns which best represent the auto-generated keys.
   *
   * @return a <code>ResultSet</code> object containing the auto-generated key(s)
   *         generated by the execution of this <code>Statement</code> object
   */
  def getGeneratedKeys(): F[ResultSet[F]]

  /**
   * Executes the given SQL statement and signals the driver with the
   * given flag about whether the
   * auto-generated keys produced by this <code>Statement</code> object
   * should be made available for retrieval.  The driver will ignore the
   * flag if the SQL statement
   * is not an <code>INSERT</code> statement, or an SQL statement able to return
   * auto-generated keys (the list of such statements is vendor-specific).
   *<p>
   * <strong>Note:</strong>This method cannot be called on a
   * <code>PreparedStatement</code> or <code>CallableStatement</code>.
   * @param sql an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
   * <code>DELETE</code>; or an SQL statement that returns nothing,
   * such as a DDL statement.
   *
   * @param autoGeneratedKeys a flag indicating whether auto-generated keys
   *        should be made available for retrieval;
   *         one of the following constants:
   *         <code>Statement.RETURN_GENERATED_KEYS</code>
   *         <code>Statement.NO_GENERATED_KEYS</code>
   * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
   *         or (2) 0 for SQL statements that return nothing
   */
  def executeUpdate(
    sql:               String,
    autoGeneratedKeys: Statement.NO_GENERATED_KEYS | Statement.RETURN_GENERATED_KEYS
  ): F[Int]

  /**
   * Executes the given SQL statement, which may return multiple results,
   * and signals the driver that any
   * auto-generated keys should be made available
   * for retrieval.  The driver will ignore this signal if the SQL statement
   * is not an <code>INSERT</code> statement, or an SQL statement able to return
   * auto-generated keys (the list of such statements is vendor-specific).
   * <P>
   * In some (uncommon) situations, a single SQL statement may return
   * multiple result sets and/or update counts.  Normally you can ignore
   * this unless you are (1) executing a stored procedure that you know may
   * return multiple results or (2) you are dynamically executing an
   * unknown SQL string.
   * <P>
   * The <code>execute</code> method executes an SQL statement and indicates the
   * form of the first result.  You must then use the methods
   * <code>getResultSet</code> or <code>getUpdateCount</code>
   * to retrieve the result, and <code>getMoreResults</code> to
   * move to any subsequent result(s).
   *<p>
   *<strong>Note:</strong>This method cannot be called on a
   * <code>PreparedStatement</code> or <code>CallableStatement</code>.
   * @param sql any SQL statement
   * @param autoGeneratedKeys a constant indicating whether auto-generated
   *        keys should be made available for retrieval using the method
   *        <code>getGeneratedKeys</code>; one of the following constants:
   *        <code>Statement.RETURN_GENERATED_KEYS</code> or
   *        <code>Statement.NO_GENERATED_KEYS</code>
   * @return <code>true</code> if the first result is a <code>ResultSet</code>
   *         object; <code>false</code> if it is an update count or there are
   *         no results
   */
  def execute(sql: String, autoGeneratedKeys: Statement.NO_GENERATED_KEYS | Statement.RETURN_GENERATED_KEYS): F[Boolean]

  /**
   * Retrieves whether this <code>Statement</code> object has been closed. A <code>Statement</code> is closed if the
   * method close has been called on it, or if it is automatically closed.
   * @return true if this <code>Statement</code> object is closed; false if it is still open
   */
  def isClosed(): F[Boolean]

object Statement:

  /**
   * The constant indicating that a batch statement executed successfully but that no count of the number of rows it affected is available.
   */
  val SUCCESS_NO_INFO: Int = -2

  /**
   * The constant indicating that an error occurred while executing a batch statement.
   */
  val EXECUTE_FAILED: Int = -3

  /**
   * The constant indicating that generated keys should be made
   * available for retrieval.
   */
  val RETURN_GENERATED_KEYS: Int = 1
  type RETURN_GENERATED_KEYS = RETURN_GENERATED_KEYS.type

  /**
   * The constant indicating that generated keys should not be made
   * available for retrieval.
   */
  val NO_GENERATED_KEYS: Int = 2
  type NO_GENERATED_KEYS = NO_GENERATED_KEYS.type

  private[ldbc] trait ShareStatement[F[_]: Temporal](using ev: MonadError[F, Throwable]) extends Statement[F]:

    def statementClosed:   Ref[F, Boolean]
    def connectionClosed:  Ref[F, Boolean]
    def currentResultSet:  Ref[F, Option[ResultSet[F]]]
    def updateCount:       Ref[F, Int]
    def moreResults:       Ref[F, Boolean]
    def autoGeneratedKeys: Ref[F, NO_GENERATED_KEYS | RETURN_GENERATED_KEYS]
    def lastInsertId:      Ref[F, Int]

    override def getResultSet():   F[Option[ResultSet[F]]] = checkClosed() *> currentResultSet.get
    override def getUpdateCount(): F[Int]                  = checkClosed() *> updateCount.get
    override def getMoreResults(): F[Boolean]              = checkClosed() *> moreResults.get

    override def executeUpdate(sql: String, autoGeneratedKeys: NO_GENERATED_KEYS | RETURN_GENERATED_KEYS): F[Int] =
      this.autoGeneratedKeys.set(autoGeneratedKeys) *> executeUpdate(sql)

    override def execute(sql: String, autoGeneratedKeys: NO_GENERATED_KEYS | RETURN_GENERATED_KEYS): F[Boolean] =
      this.autoGeneratedKeys.set(autoGeneratedKeys) *> execute(sql)

    override def isClosed(): F[Boolean] =
      for
        connClosed <- connectionClosed.get
        stmtClosed <- statementClosed.get
      yield connClosed || stmtClosed

    protected def checkClosed(): F[Unit] =
      isClosed().ifM(
        close() *> ev.raiseError(new SQLException("No operations allowed after statement closed.")),
        ev.unit
      )

    protected def checkNullOrEmptyQuery(sql: String): F[Unit] =
      if sql.isEmpty then ev.raiseError(new SQLException("Can not issue empty query."))
      else if sql == null then ev.raiseError(new SQLException("Can not issue NULL query."))
      else ev.unit

  private[ldbc] case class Impl[F[_]: Temporal: Exchange: Tracer](
    protocol:             Protocol[F],
    serverVariables:      Map[String, String],
    batchedArgs:          Ref[F, Vector[String]],
    connectionClosed:     Ref[F, Boolean],
    statementClosed:      Ref[F, Boolean],
    resultSetClosed:      Ref[F, Boolean],
    currentResultSet:     Ref[F, Option[ResultSet[F]]],
    updateCount:          Ref[F, Int],
    moreResults:          Ref[F, Boolean],
    autoGeneratedKeys:    Ref[F, NO_GENERATED_KEYS | RETURN_GENERATED_KEYS],
    lastInsertId:         Ref[F, Int],
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
  )(using ev: MonadError[F, Throwable])
    extends ShareStatement[F]:

    private val attributes = protocol.initialPacket.attributes ++ List(Attribute("type", "Statement"))

    override def executeQuery(sql: String): F[ResultSet[F]] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, ResultSet[F]]("statement") { (span: Span[F]) =>
        span.addAttributes((attributes ++ List(Attribute("execute", "query"), Attribute("sql", sql)))*) *>
          protocol.resetSequenceId *>
          protocol.send(ComQueryPacket(sql, protocol.initialPacket.capabilityFlags, ListMap.empty)) *>
          protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
            case _: OKPacket =>
              for
                isResultSetClosed      <- Ref[F].of(false)
                lastColumnReadNullable <- Ref[F].of(true)
                resultSetCurrentCursor <- Ref[F].of(0)
                resultSetCurrentRow    <- Ref[F].of[Option[ResultSetRowPacket]](None)
                resultSet = LdbcResultSet
                              .empty(
                                serverVariables,
                                protocol.initialPacket.serverVersion,
                                isResultSetClosed,
                                lastColumnReadNullable,
                                resultSetCurrentCursor,
                                resultSetCurrentRow
                              )
                _ <- currentResultSet.set(Some(resultSet))
              yield resultSet
            case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
            case result: ColumnsNumberPacket =>
              for
                columnDefinitions <-
                  protocol.repeatProcess(
                    result.size,
                    ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
                  )
                resultSetRow <-
                  protocol.readUntilEOF[ResultSetRowPacket](
                    ResultSetRowPacket.decoder(protocol.initialPacket.capabilityFlags, columnDefinitions),
                    Vector.empty
                  )
                lastColumnReadNullable <- Ref[F].of(true)
                resultSetCurrentCursor <- Ref[F].of(0)
                resultSetCurrentRow    <- Ref[F].of(resultSetRow.headOption)
                resultSet = LdbcResultSet(
                              columnDefinitions,
                              resultSetRow,
                              serverVariables,
                              protocol.initialPacket.serverVersion,
                              resultSetClosed,
                              lastColumnReadNullable,
                              resultSetCurrentCursor,
                              resultSetCurrentRow,
                              resultSetType,
                              resultSetConcurrency
                            )
                _ <- currentResultSet.set(Some(resultSet))
              yield resultSet
          }
      }

    override def executeUpdate(sql: String): F[Int] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, Int]("statement") { (span: Span[F]) =>
        span.addAttributes(
          (attributes ++ List(Attribute("execute", "update"), Attribute("sql", sql)))*
        ) *> protocol.resetSequenceId *> (
          protocol.send(ComQueryPacket(sql, protocol.initialPacket.capabilityFlags, ListMap.empty)) *>
            protocol.receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
              case result: OKPacket =>
                lastInsertId.set(result.lastInsertId) *> updateCount.updateAndGet(_ => result.affectedRows)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
            }
        )
      }

    override def close(): F[Unit] = statementClosed.set(true) *> resultSetClosed.set(true)

    override def execute(sql: String): F[Boolean] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> (
        if sql.toUpperCase.startsWith("SELECT") then
          executeQuery(sql).flatMap {
            case resultSet: LdbcResultSet[F] => resultSet.hasRows()
            case _                           => ev.pure(false)
          }
        else executeUpdate(sql).map(_ => false)
      )

    override def executeBatch(): F[List[Int]] =
      checkClosed() *> protocol.resetSequenceId *>
        protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON) *>
        exchange[F, List[Int]]("statement") { (span: Span[F]) =>
          batchedArgs.get.flatMap { args =>
            span.addAttributes(
              (attributes ++ List(
                Attribute("execute", "batch"),
                Attribute("size", args.length.toLong),
                Attribute("sql", args.toArray.toSeq)
              ))*
            ) *> (
              if args.isEmpty then ev.pure(List.empty)
              else
                protocol.resetSequenceId *>
                  protocol.send(
                    ComQueryPacket(args.mkString(";"), protocol.initialPacket.capabilityFlags, ListMap.empty)
                  ) *>
                  args
                    .foldLeft(ev.pure(Vector.empty[Int])) { ($acc, _) =>
                      for
                        acc <- $acc
                        result <-
                          protocol
                            .receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags))
                            .flatMap {
                              case result: OKPacket =>
                                lastInsertId.set(result.lastInsertId) *> ev.pure(acc :+ result.affectedRows)
                              case error: ERRPacket =>
                                ev.raiseError(error.toException("Failed to execute batch", acc))
                              case _: EOFPacket => ev.raiseError(new SQLException("Unexpected EOF packet"))
                            }
                      yield result
                    }
                    .map(_.toList)
            )
          }
        } <* protocol.resetSequenceId <* protocol.comSetOption(
          EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF
        ) <* clearBatch()

    override def getGeneratedKeys(): F[ResultSet[F]] =
      autoGeneratedKeys.get.flatMap {
        case Statement.RETURN_GENERATED_KEYS =>
          for
            isResultSetClosed      <- Ref[F].of(false)
            lastColumnReadNullable <- Ref[F].of(true)
            resultSetCurrentCursor <- Ref[F].of(0)
            resultSetCurrentRow    <- Ref[F].of[Option[ResultSetRowPacket]](None)
            lastInsertId           <- lastInsertId.get
            resultSet = LdbcResultSet(
                          Vector(new ColumnDefinitionPacket:
                            override def table:      String                     = ""
                            override def name:       String                     = "GENERATED_KEYS"
                            override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_LONGLONG
                            override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
                          ),
                          Vector(ResultSetRowPacket(List(Some(lastInsertId.toString)))),
                          serverVariables,
                          protocol.initialPacket.serverVersion,
                          isResultSetClosed,
                          lastColumnReadNullable,
                          resultSetCurrentCursor,
                          resultSetCurrentRow
                        )
            _ <- currentResultSet.set(Some(resultSet))
          yield resultSet
        case Statement.NO_GENERATED_KEYS =>
          ev.raiseError(
            new SQLException(
              "Generated keys not requested. You need to specify Statement.RETURN_GENERATED_KEYS to Statement.executeUpdate(), Statement.executeLargeUpdate() or Connection.prepareStatement()."
            )
          )
      }

  def apply[F[_]: Temporal: Exchange: Tracer](
    protocol:             Protocol[F],
    serverVariables:      Map[String, String],
    batchedArgsRef:       Ref[F, Vector[String]],
    connectionClosed:     Ref[F, Boolean],
    statementClosed:      Ref[F, Boolean],
    resultSetClosed:      Ref[F, Boolean],
    currentResultSet:     Ref[F, Option[ResultSet[F]]],
    updateCount:          Ref[F, Int],
    moreResults:          Ref[F, Boolean],
    autoGeneratedKeys:    Ref[F, NO_GENERATED_KEYS | RETURN_GENERATED_KEYS],
    lastInsertId:         Ref[F, Int],
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
  )(using ev: MonadError[F, Throwable]): Statement[F] =
    Impl(
      protocol,
      serverVariables,
      batchedArgsRef,
      connectionClosed,
      statementClosed,
      resultSetClosed,
      currentResultSet,
      updateCount,
      moreResults,
      autoGeneratedKeys,
      lastInsertId,
      resultSetType,
      resultSetConcurrency
    )
