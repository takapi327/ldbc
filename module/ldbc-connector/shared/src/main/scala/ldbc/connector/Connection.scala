/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.util.UUID

import scala.concurrent.duration.Duration
import scala.collection.immutable.ListMap

import com.comcast.ip4s.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.data.*
import ldbc.connector.exception.*
import ldbc.connector.net.*
import ldbc.connector.net.protocol.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.DatabaseMetaData.DatabaseTerm
import ldbc.connector.codec.text.text

/**
 * A connection (session) with a specific database. SQL statements are executed and results are returned within the context of a connection.
 * 
 * @tparam F
 *   the effect type
 */
trait Connection[F[_]]:

  /**
   * Creates a <code>Statement</code> object for sending
   * SQL statements to the database.
   * SQL statements without parameters are normally
   * executed using <code>Statement</code> objects. If the same SQL statement
   * is executed many times, it may be more efficient to use a
   * <code>PreparedStatement</code> object.
   * <P>
   * Result sets created using the returned <code>Statement</code>
   * object will by default be type <code>TYPE_FORWARD_ONLY</code>
   * and have a concurrency level of <code>CONCUR_READ_ONLY</code>.
   * The holdability of the created result sets can be determined by
   * calling [[getHoldability]].
   *
   * @return a new default <code>Statement</code> object
   */
  def createStatement(): F[Statement[F]]

  /**
   * Creates a <code>PreparedStatement</code> object for sending
   * parameterized SQL statements to the database.
   * <P>
   * A SQL statement with or without IN parameters can be
   * pre-compiled and stored in a <code>PreparedStatement</code> object. This
   * object can then be used to efficiently execute this statement
   * multiple times.
   *
   * <P><B>Note:</B> This method is optimized for handling
   * parametric SQL statements that benefit from precompilation. If
   * the driver supports precompilation,
   * the method <code>prepareStatement</code> will send
   * the statement to the database for precompilation. Some drivers
   * may not support precompilation. In this case, the statement may
   * not be sent to the database until the <code>PreparedStatement</code>
   * object is executed.  This has no direct effect on users; however, it does
   * affect which methods throw certain <code>SQLException</code> objects.
   * <P>
   * Result sets created using the returned <code>PreparedStatement</code>
   * object will by default be type <code>TYPE_FORWARD_ONLY</code>
   * and have a concurrency level of <code>CONCUR_READ_ONLY</code>.
   * The holdability of the created result sets can be determined by
   * calling [[getHoldability]].
   *
   * @param sql an SQL statement that may contain one or more '?' IN
   * parameter placeholders
   * @return a new default <code>PreparedStatement</code> object containing the
   * pre-compiled SQL statement
   */
  def prepareStatement(sql: String): F[PreparedStatement[F]]

  /**
   * Converts the given SQL statement into the system's native SQL grammar.
   * A driver may convert the JDBC SQL grammar into its system's
   * native SQL grammar prior to sending it. This method returns the
   * native form of the statement that the driver would have sent.
   *
   * @param sql an SQL statement that may contain one or more '?'
   * parameter placeholders
   * @return the native form of this statement
   */
  def nativeSQL(sql: String): F[String]

  /**
   * Sets this connection's auto-commit mode to the given state.
   * If a connection is in auto-commit mode, then all its SQL
   * statements will be executed and committed as individual
   * transactions.  Otherwise, its SQL statements are grouped into
   * transactions that are terminated by a call to either
   * the method <code>commit</code> or the method <code>rollback</code>.
   * By default, new connections are in auto-commit
   * mode.
   * <P>
   * The commit occurs when the statement completes. The time when the statement
   * completes depends on the type of SQL Statement:
   * <ul>
   * <li>For DML statements, such as Insert, Update or Delete, and DDL statements,
   * the statement is complete as soon as it has finished executing.
   * <li>For Select statements, the statement is complete when the associated result
   * set is closed.
   * <li>For <code>CallableStatement</code> objects or for statements that return
   * multiple results, the statement is complete
   * when all of the associated result sets have been closed, and all update
   * counts and output parameters have been retrieved.
   *</ul>
   * <P>
   * <B>NOTE:</B>  If this method is called during a transaction and the
   * auto-commit mode is changed, the transaction is committed.  If
   * <code>setAutoCommit</code> is called and the auto-commit mode is
   * not changed, the call is a no-op.
   *
   * @param autoCommit <code>true</code> to enable auto-commit mode;
   *         <code>false</code> to disable it
   */
  def setAutoCommit(autoCommit: Boolean): F[Unit]

  /**
   * Retrieves the current auto-commit mode for this <code>Connection</code>
   * object.
   *
   * @return the current state of this <code>Connection</code> object's
   *         auto-commit mode
   */
  def getAutoCommit(): F[Boolean]

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
   */
  def commit(): F[Unit]

  /**
   * Undoes all changes made in the current transaction
   * and releases any database locks currently held
   * by this <code>Connection</code> object. This method should be
   * used only when auto-commit mode has been disabled.
   */
  def rollback(): F[Unit]

  /**
   * Releases this <code>Connection</code> object's database and JDBC resources
   * immediately instead of waiting for them to be automatically released.
   * <P>
   * Calling the method <code>close</code> on a <code>Connection</code>
   * object that is already closed is a no-op.
   * <P>
   * It is <b>strongly recommended</b> that an application explicitly
   * commits or rolls back an active transaction prior to calling the
   * <code>close</code> method.  If the <code>close</code> method is called
   * and there is an active transaction, the results are implementation-defined.
   */
  def close(): F[Unit]

  /**
   * Retrieves whether this <code>Connection</code> object has been
   * closed.  A connection is closed if the method <code>close</code>
   * has been called on it or if certain fatal errors have occurred.
   * This method is guaranteed to return <code>true</code> only when
   * it is called after the method <code>Connection.close</code> has
   * been called.
   * <P>
   * This method generally cannot be called to determine whether a
   * connection to a database is valid or invalid.  A typical client
   * can determine that a connection is invalid by catching any
   * exceptions that might be thrown when an operation is attempted.
   *
   * @return <code>true</code> if this <code>Connection</code> object
   *         is closed; <code>false</code> if it is still open
   */
  def isClosed(): F[Boolean]

  /**
   * Retrieves a <code>DatabaseMetaData</code> object that contains
   * metadata about the database to which this
   * <code>Connection</code> object represents a connection.
   * The metadata includes information about the database's
   * tables, its supported SQL grammar, its stored
   * procedures, the capabilities of this connection, and so on.
   *
   * @return a <code>DatabaseMetaData</code> object for this
   *         <code>Connection</code> object
   */
  def getMetaData(): F[DatabaseMetaData[F]]

  /**
   * Puts this connection in read-only mode as a hint to the driver to enable
   * database optimizations.
   *
   * @param isReadOnly
   *   true enables read-only mode; false disables it
   */
  def setReadOnly(isReadOnly: Boolean): F[Unit]

  /**
   * Retrieves whether this Connection object is in read-only mode.
   *
   * @return
   *   true if this Connection object is read-only; false otherwise
   */
  def isReadOnly: F[Boolean]

  /**
   * Attempts to change the transaction isolation level for this Connection object to the one given. The constants defined in the interface Connection are the possible transaction isolation levels.
   *
   * @param level
   *   one of the following Connection constants:
   *   [[Connection.TransactionIsolationLevel.READ_UNCOMMITTED]],
   *   [[Connection.TransactionIsolationLevel.READ_COMMITTED]],
   *   [[Connection.TransactionIsolationLevel.REPEATABLE_READ]], or [[Connection.TransactionIsolationLevel.SERIALIZABLE]]
   */
  def setTransactionIsolation(level: Connection.TransactionIsolationLevel): F[Unit]

  /**
   * Retrieves this Connection object's current transaction isolation level.
   *
   * @return
   *   the current transaction isolation level, which will be one of the following constants:
   *   [[Connection.TransactionIsolationLevel.READ_UNCOMMITTED]],
   *   [[Connection.TransactionIsolationLevel.READ_COMMITTED]],
   *   [[Connection.TransactionIsolationLevel.REPEATABLE_READ]], or [[Connection.TransactionIsolationLevel.SERIALIZABLE]]
   */
  def getTransactionIsolation: F[Connection.TransactionIsolationLevel]

  /**
   * Creates a <code>Statement</code> object that will generate
   * <code>ResultSet</code> objects with the given type and concurrency.
   * This method is the same as the <code>createStatement</code> method
   * above, but it allows the default result set
   * type and concurrency to be overridden.
   * The holdability of the created result sets can be determined by
   * calling {@link #getHoldability}.
   *
   * @param resultSetType a result set type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @param resultSetConcurrency a concurrency type; one of
   *        <code>ResultSet.CONCUR_READ_ONLY</code> or
   *        <code>ResultSet.CONCUR_UPDATABLE</code>
   * @return a new <code>Statement</code> object that will generate
   *         <code>ResultSet</code> objects with the given type and
   *         concurrency
   */
  def createStatement(resultSetType: Int, resultSetConcurrency: Int): F[Statement[F]]

  /**
   * Creates a client-side prepared statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]]

  /**
   * Prepares a statement on the client, using client-side emulation
   * (irregardless of the configuration property 'useServerPrepStmts')
   * with the same semantics as the java.sql.Connection.prepareStatement()
   * method with the same argument types.
   *
   * @param sql
   *   statement
   * @param resultSetType
   *   resultSetType
   * @param resultSetConcurrency
   *   resultSetConcurrency
   * @return prepared statement
   */
  def clientPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int
  ): F[PreparedStatement.Client[F]]

  /**
   * Creates a server prepared statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]]

  /**
   * Prepares a statement on the server (irregardless of the
   * configuration property 'useServerPrepStmts') with the same semantics
   * as the java.sql.Connection.prepareStatement() method with the
   * same argument types.
   *
   * @param sql
   *   statement
   * @param resultSetType
   *   resultSetType
   * @param resultSetConcurrency
   *   resultSetConcurrency
   * @return prepared statement
   */
  def serverPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int
  ): F[PreparedStatement.Server[F]]

  /**
   * Creates an unnamed savepoint in the current transaction and returns the new Savepoint object that represents it.
   * if setSavepoint is invoked outside of an active transaction, a transaction will be started at this newly created savepoint.
   *
   * @return
   *   the new Savepoint object
   */
  def setSavepoint(): F[Savepoint]

  /**
   * Creates a savepoint with the given name in the current transaction and returns the new Savepoint object that represents it.
   * if setSavepoint is invoked outside of an active transaction, a transaction will be started at this newly created savepoint.
   *
   * @param name
   *   a String containing the name of the savepoint
   * @return
   *   the new Savepoint object
   */
  def setSavepoint(name: String): F[Savepoint]

  /**
   * Undoes all changes made after the given Savepoint object was set.
   * This method should be used only when auto-commit has been disabled.
   *
   * @param savepoint
   *   the Savepoint object to roll back to
   */
  def rollback(savepoint: Savepoint): F[Unit]

  /**
   * Removes the specified Savepoint and subsequent Savepoint objects from the current transaction.
   * Any reference to the savepoint after it have been removed will cause an SQLException to be thrown.
   * 
   * @param savepoint
   *   the Savepoint object to release
   */
  def releaseSavepoint(savepoint: Savepoint): F[Unit]

  /**
   * Sets the schema name that will be used for subsequent queries.
   *
   * Calling setSchema has no effect on previously created or prepared Statement objects.
   * It is implementation defined whether a DBMS prepare operation takes place immediately when the Connection method [[statement]] or [[clientPreparedStatement]], [[serverPreparedStatement]] is invoked.
   * For maximum portability, setSchema should be called before a Statement is created or prepared.
   *
   * @param schema
   *   the name of a schema in which to work
   */
  def setSchema(schema: String): F[Unit]

  /**
   * Retrieves this Connection object's current schema name.
   *
   * @return
   *   the current schema name or null if there is none
   */
  def getSchema: F[String]

  /**
   * Retrieves the statistics of this Connection object.
   *
   * @return
   *   the statistics of this Connection object
   */
  def getStatistics: F[StatisticsPacket]

  /**
   * Returns true if the connection has not been closed and is still valid.
   */
  def isValid: F[Boolean]

  /**
   * Resets the server-side state of this connection. 
   */
  def resetServerState: F[Unit]

  /**
   * Changes the user and password for this connection.
   *
   * @param user
   *   the new user name
   * @param password
   *   the new password
   */
  def changeUser(user: String, password: String): F[Unit]

object Connection:

  private val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  private val defaultCapabilityFlags: List[CapabilitiesFlags] = List(
    CapabilitiesFlags.CLIENT_LONG_PASSWORD,
    CapabilitiesFlags.CLIENT_FOUND_ROWS,
    CapabilitiesFlags.CLIENT_LONG_FLAG,
    CapabilitiesFlags.CLIENT_PROTOCOL_41,
    CapabilitiesFlags.CLIENT_TRANSACTIONS,
    CapabilitiesFlags.CLIENT_RESERVED2,
    CapabilitiesFlags.CLIENT_MULTI_RESULTS,
    CapabilitiesFlags.CLIENT_PS_MULTI_RESULTS,
    CapabilitiesFlags.CLIENT_PLUGIN_AUTH,
    CapabilitiesFlags.CLIENT_CONNECT_ATTRS,
    CapabilitiesFlags.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA,
    CapabilitiesFlags.CLIENT_DEPRECATE_EOF,
    CapabilitiesFlags.CLIENT_QUERY_ATTRIBUTES,
    CapabilitiesFlags.MULTI_FACTOR_AUTHENTICATION
  )

  /**
   * The possible transaction isolation levels.
   */
  enum TransactionIsolationLevel(val name: String):
    /**
     * A constant indicating that dirty reads, non-repeatable reads and phantom reads can occur.
     * This level allows a row changed by one transaction to be read by another transaction before any changes in that row have been committed (a "dirty read").
     * If any of the changes are rolled back, the second transaction will have retrieved an invalid row.
     */
    case READ_UNCOMMITTED extends TransactionIsolationLevel("READ UNCOMMITTED")

    /**
     * A constant indicating that dirty reads are prevented; non-repeatable reads and phantom reads can occur.
     * This level only prohibits a transaction from reading a row with uncommitted changes in it.
     */
    case READ_COMMITTED extends TransactionIsolationLevel("READ COMMITTED")

    /**
     * A constant indicating that dirty reads and non-repeatable reads are prevented; phantom reads can occur.
     * This level prohibits a transaction from reading a row with uncommitted changes in it,
     * and it also prohibits the situation where one transaction reads a row, a second transaction alters the row,
     * and the first transaction rereads the row, getting different values the second time (a "non-repeatable read").
     */
    case REPEATABLE_READ extends TransactionIsolationLevel("REPEATABLE READ")

    /**
     * A constant indicating that dirty reads, non-repeatable reads and phantom reads are prevented.
     * This level includes the prohibitions in TRANSACTION_REPEATABLE_READ and further prohibits the situation where one transaction reads all rows that satisfy a WHERE condition,
     * a second transaction inserts a row that satisfies that WHERE condition, and the first transaction rereads for the same condition, retrieving the additional "phantom" row in the second read.
     */
    case SERIALIZABLE extends TransactionIsolationLevel("SERIALIZABLE")

  /**
   * A constant indicating that transactions are not supported.
   */
  val TRANSACTION_NONE: Int = 0

  /**
   * A constant indicating that
   * dirty reads, non-repeatable reads and phantom reads can occur.
   * This level allows a row changed by one transaction to be read
   * by another transaction before any changes in that row have been
   * committed (a "dirty read").  If any of the changes are rolled back,
   * the second transaction will have retrieved an invalid row.
   */
  val TRANSACTION_READ_UNCOMMITTED: Int = 1

  /**
   * A constant indicating that
   * dirty reads are prevented; non-repeatable reads and phantom
   * reads can occur.  This level only prohibits a transaction
   * from reading a row with uncommitted changes in it.
   */
  val TRANSACTION_READ_COMMITTED: Int = 2

  /**
   * A constant indicating that
   * dirty reads and non-repeatable reads are prevented; phantom
   * reads can occur.  This level prohibits a transaction from
   * reading a row with uncommitted changes in it, and it also
   * prohibits the situation where one transaction reads a row,
   * a second transaction alters the row, and the first transaction
   * rereads the row, getting different values the second time
   * (a "non-repeatable read").
   */
  val TRANSACTION_REPEATABLE_READ: Int = 4

  /**
   * A constant indicating that
   * dirty reads, non-repeatable reads and phantom reads are prevented.
   * This level includes the prohibitions in
   * <code>TRANSACTION_REPEATABLE_READ</code> and further prohibits the
   * situation where one transaction reads all rows that satisfy
   * a <code>WHERE</code> condition, a second transaction inserts a row that
   * satisfies that <code>WHERE</code> condition, and the first transaction
   * rereads for the same condition, retrieving the additional
   * "phantom" row in the second read.
   */
  val TRANSACTION_SERIALIZABLE: Int = 8

  private[ldbc] case class ConnectionImpl[F[_]: Temporal: Tracer: Console: Exchange](
    protocol:        Protocol[F],
    serverVariables: Map[String, String],
    database:        Option[String],
    readOnly:        Ref[F, Boolean],
    isAutoCommit:    Ref[F, Boolean],
    closed:          Ref[F, Boolean],
    databaseTerm:    Option[DatabaseTerm] = None
  )(using ev: MonadError[F, Throwable])
    extends Connection[F]:

    override def createStatement(): F[Statement[F]] =
      createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

    override def prepareStatement(sql: String): F[PreparedStatement[F]] =
      for
        params      <- Ref[F].of(ListMap.empty[Int, Parameter])
        batchedArgs <- Ref[F].of(Vector.empty[String])
      yield PreparedStatement.Client[F](
        protocol,
        sql,
        params,
        batchedArgs,
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )

    override def nativeSQL(sql: String): F[String] = ev.pure(sql)

    override def setAutoCommit(autoCommit: Boolean): F[Unit] =
      isAutoCommit.update(_ => autoCommit) *>
        createStatement()
          .flatMap(_.executeQuery("SET autocommit=" + (if autoCommit then "1" else "0")))
          .void

    override def getAutoCommit(): F[Boolean] = isAutoCommit.get

    override def commit(): F[Unit] = isAutoCommit.get.flatMap { autoCommit =>
      if !autoCommit then createStatement().flatMap(_.executeQuery("COMMIT")).void
      else ev.raiseError(new SQLNonTransientException("Can't call commit when autocommit=true"))
    }

    override def rollback(): F[Unit] = isAutoCommit.get.flatMap { autoCommit =>
      if !autoCommit then createStatement().flatMap(_.executeQuery("ROLLBACK")).void
      else ev.raiseError(new SQLNonTransientException("Can't call rollback when autocommit=true"))
    }

    override def close(): F[Unit] = getAutoCommit().flatMap { autoCommit =>
      (if !autoCommit then createStatement().flatMap(_.executeQuery("ROLLBACK")).void
       else ev.unit) *> protocol.resetSequenceId *> protocol.comQuit() *> closed.update(_ => true)
    }

    override def isClosed(): F[Boolean] = closed.get

    override def getMetaData(): F[DatabaseMetaData[F]] =
      isClosed().map {
        case true => throw new SQLException("Connection is closed")
        case false =>
          DatabaseMetaData[F](protocol, serverVariables, database, databaseTerm)
      }

    override def setReadOnly(isReadOnly: Boolean): F[Unit] =
      readOnly.update(_ => isReadOnly) *>
        createStatement()
          .flatMap(_.executeQuery("SET SESSION TRANSACTION READ " + (if isReadOnly then "ONLY" else "WRITE")))
          .void

    override def isReadOnly: F[Boolean] = readOnly.get

    override def setTransactionIsolation(level: TransactionIsolationLevel): F[Unit] =
      createStatement().flatMap(_.executeQuery(s"SET SESSION TRANSACTION ISOLATION LEVEL ${ level.name }")).void

    override def getTransactionIsolation: F[Connection.TransactionIsolationLevel] =
      for
        statement            <- createStatement()
        result               <- statement.executeQuery("SELECT @@session.transaction_isolation")
        transactionIsolation <- result.getString(1)
      yield transactionIsolation match
        case Some("READ-UNCOMMITTED") => Connection.TransactionIsolationLevel.READ_UNCOMMITTED
        case Some("READ-COMMITTED")   => Connection.TransactionIsolationLevel.READ_COMMITTED
        case Some("REPEATABLE-READ")  => Connection.TransactionIsolationLevel.REPEATABLE_READ
        case Some("SERIALIZABLE")     => Connection.TransactionIsolationLevel.SERIALIZABLE
        case Some(unknown) => throw new SQLFeatureNotSupportedException(s"Unknown transaction isolation level $unknown")
        case None          => throw new SQLFeatureNotSupportedException("Unknown transaction isolation level")

    override def createStatement(resultSetType: Int, resultSetConcurrency: Int): F[Statement[F]] =
      Ref[F]
        .of(Vector.empty[String])
        .map(batchedArgs =>
          Statement[F](
            protocol,
            batchedArgs,
            resultSetType,
            resultSetConcurrency
          )
        )

    override def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]] =
      clientPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
    override def clientPreparedStatement(
      sql:                  String,
      resultSetType:        Int,
      resultSetConcurrency: Int
    ): F[PreparedStatement.Client[F]] =
      for
        params      <- Ref[F].of(ListMap.empty[Int, Parameter])
        batchedArgs <- Ref[F].of(Vector.empty[String])
      yield PreparedStatement
        .Client[F](
          protocol,
          sql,
          params,
          batchedArgs,
          resultSetType,
          resultSetConcurrency
        )

    override def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]] =
      serverPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
    override def serverPreparedStatement(
      sql:                  String,
      resultSetType:        Int,
      resultSetConcurrency: Int
    ): F[PreparedStatement.Server[F]] =
      for
        result <- protocol.resetSequenceId *> protocol.send(ComStmtPreparePacket(sql)) *>
                    protocol.receive(ComStmtPrepareOkPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
                      case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                      case ok: ComStmtPrepareOkPacket => ev.pure(ok)
                    }
        _ <- protocol.repeatProcess(
               result.numParams,
               ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
             )
        _ <- protocol.repeatProcess(
               result.numColumns,
               ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
             )
        params      <- Ref[F].of(ListMap.empty[Int, Parameter])
        batchedArgs <- Ref[F].of(Vector.empty[String])
      yield PreparedStatement
        .Server[F](
          protocol,
          result.statementId,
          sql,
          params,
          batchedArgs,
          resultSetType,
          resultSetConcurrency
        )

    override def setSavepoint(): F[Savepoint] = setSavepoint(UUID.randomUUID().toString)

    override def setSavepoint(name: String): F[Savepoint] =
      for
        statement <- createStatement()
        _         <- statement.executeQuery(s"SAVEPOINT `$name`")
      yield new Savepoint:
        override def getSavepointName: String = name

    override def rollback(savepoint: Savepoint): F[Unit] =
      createStatement().flatMap(_.executeQuery(s"ROLLBACK TO SAVEPOINT `${ savepoint.getSavepointName }`")).void

    override def releaseSavepoint(savepoint: Savepoint): F[Unit] =
      createStatement().flatMap(_.executeQuery(s"RELEASE SAVEPOINT `${ savepoint.getSavepointName }`")).void

    override def setSchema(schema: String): F[Unit] = protocol.resetSequenceId *> protocol.comInitDB(schema)

    override def getSchema: F[String] =
      for
        statement <- createStatement()
        result    <- statement.executeQuery("SELECT DATABASE()")
        decoded   <- result.decode(text)
      yield decoded.headOption.getOrElse("")

    override def getStatistics: F[StatisticsPacket] = protocol.resetSequenceId *> protocol.comStatistics()

    override def isValid: F[Boolean] = protocol.resetSequenceId *> protocol.comPing()

    override def resetServerState: F[Unit] =
      protocol.resetSequenceId *> protocol.resetConnection *> createStatement().flatMap { statement =>
        statement.executeQuery("SET NAMES utf8mb4") *>
          statement.executeQuery("SET character_set_results = NULL") *>
          statement.executeQuery("SET autocommit=1") *>
          isAutoCommit.update(_ => true)
      }

    override def changeUser(user: String, password: String): F[Unit] =
      protocol.resetSequenceId *> protocol.changeUser(user, password)

  def apply[F[_]: Temporal: Network: Console](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    socketOptions:           List[SocketOption] = Connection.defaultSocketOptions,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseTerm] = None
  ): Tracer[F] ?=> Resource[F, Connection[F]] =

    val logger: String => F[Unit] = s => Console[F].println(s"TLS: $s")

    for
      sslOp <- ssl.toSSLNegotiationOptions(if debug then logger.some else none)
      connection <- fromSocketGroup(
                      Network[F],
                      host,
                      port,
                      user,
                      password,
                      database,
                      debug,
                      socketOptions,
                      sslOp,
                      readTimeout,
                      allowPublicKeyRetrieval,
                      databaseTerm
                    )
    yield connection

  def fromSockets[F[_]: Temporal: Tracer: Console](
    sockets:                 Resource[F, Socket[F]],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseTerm] = None
  ): Resource[F, Connection[F]] =
    val capabilityFlags = defaultCapabilityFlags ++
      (if database.isDefined then List(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB) else List.empty) ++
      (if sslOptions.isDefined then List(CapabilitiesFlags.CLIENT_SSL) else List.empty)
    val hostInfo = HostInfo(host, port, user, password, database)
    for
      given Exchange[F] <- Resource.eval(Exchange[F])
      protocol <-
        Protocol[F](sockets, hostInfo, debug, sslOptions, allowPublicKeyRetrieval, readTimeout, capabilityFlags)
      _               <- Resource.eval(protocol.startAuthentication(user, password.getOrElse("")))
      serverVariables <- Resource.eval(protocol.serverVariables())
      readOnly        <- Resource.eval(Ref[F].of[Boolean](false))
      autoCommit      <- Resource.eval(Ref[F].of[Boolean](true))
      closed          <- Resource.eval(Ref[F].of[Boolean](false))
      connection <-
        Resource.make(
          Temporal[F].pure(
            ConnectionImpl[F](protocol, serverVariables, database, readOnly, autoCommit, closed, databaseTerm)
          )
        )(_.close())
    yield connection

  def fromSocketGroup[F[_]: Tracer: Console](
    socketGroup:             SocketGroup[F],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    socketOptions:           List[SocketOption],
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseTerm] = None
  )(using ev: Temporal[F]): Resource[F, Connection[F]] =

    def fail[A](msg: String): Resource[F, A] =
      Resource.eval(ev.raiseError(new SQLClientInfoException(msg)))

    def sockets: Resource[F, Socket[F]] =
      (Hostname.fromString(host), Port.fromInt(port)) match
        case (Some(validHost), Some(validPort)) =>
          socketGroup.client(SocketAddress(validHost, validPort), socketOptions)
        case (None, _) => fail(s"""Hostname: "$host" is not syntactically valid.""")
        case (_, None) => fail(s"Port: $port falls out of the allowed range.")

    fromSockets(
      sockets,
      host,
      port,
      user,
      password,
      database,
      debug,
      sslOptions,
      readTimeout,
      allowPublicKeyRetrieval,
      databaseTerm
    )
