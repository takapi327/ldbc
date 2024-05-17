/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * A connection (session) with a specific database. SQL statements are executed and results are returned within the
 * context of a connection.
 *
 * A Connection object's database is able to provide information describing its tables, its supported SQL grammar, its
 * stored procedures, the capabilities of this connection, and so on. This information is obtained with the getMetaData
 * method.
 *
 * Note: When configuring a Connection, JDBC applications should use the appropriate Connection method such as
 * setAutoCommit or setTransactionIsolation. Applications should not invoke SQL commands directly to change the
 * connection's configuration when there is a JDBC method available. By default a Connection object is in auto-commit
 * mode, which means that it automatically commits changes after executing each statement. If auto-commit mode has been
 * disabled, the method commit must be called explicitly in order to commit changes; otherwise, database changes will
 * not be saved.
 *
 * A new Connection object created using the JDBC 2.1 core API has an initially empty type map associated with it. A
 * user may enter a custom mapping for a UDT in this type map. When a UDT is retrieved from a data source with the
 * method ResultSet.getObject, the getObject method will check the connection's type map to see if there is an entry for
 * that UDT. If so, the getObject method will map the UDT to the class indicated. If there is no entry, the UDT will be
 * mapped using the standard mapping.
 *
 * A user may create a new type map, which is a java.util.Map object, make an entry in it, and pass it to the java.sql
 * methods that can perform custom mapping. In this case, the method will use the given type map instead of the one
 * associated with the connection.
 * @tparam F
 *   The effect type
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
   * Creates a <code>CallableStatement</code> object for calling
   * database stored procedures.
   * The <code>CallableStatement</code> object provides
   * methods for setting up its IN and OUT parameters, and
   * methods for executing the call to a stored procedure.
   *
   * <P><B>Note:</B> This method is optimized for handling stored
   * procedure call statements. Some drivers may send the call
   * statement to the database when the method <code>prepareCall</code>
   * is done; others
   * may wait until the <code>CallableStatement</code> object
   * is executed. This has no
   * direct effect on users; however, it does affect which method
   * throws certain SQLExceptions.
   * <P>
   * Result sets created using the returned <code>CallableStatement</code>
   * object will by default be type <code>TYPE_FORWARD_ONLY</code>
   * and have a concurrency level of <code>CONCUR_READ_ONLY</code>.
   * The holdability of the created result sets can be determined by
   * calling {@link # getHoldability}.
   *
   * @param sql an SQL statement that may contain one or more '?'
   *            parameter placeholders. Typically this statement is specified using JDBC
   *            call escape syntax.
   * @return a new default <code>CallableStatement</code> object containing the
   *         pre-compiled SQL statement
   */
  def prepareCall(sql: String): F[CallableStatement[F]]

  /**
   * Converts the given SQL statement into the system's native SQL grammar.
   * A driver may convert the JDBC SQL grammar into its system's
   * native SQL grammar prior to sending it. This method returns the
   * native form of the statement that the driver would have sent.
   *
   * @param sql an SQL statement that may contain one or more '?'
   *            parameter placeholders
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
   * </ul>
   * <P>
   * <B>NOTE:</B>  If this method is called during a transaction and the
   * auto-commit mode is changed, the transaction is committed.  If
   * <code>setAutoCommit</code> is called and the auto-commit mode is
   * not changed, the call is a no-op.
   *
   * @param autoCommit <code>true</code> to enable auto-commit mode;
   *                   <code>false</code> to disable it
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
   * true if this Connection object is read-only; false otherwise
   */
  def isReadOnly: F[Boolean]

  /**
   * Sets the given catalog name in order to select
   * a subspace of this <code>Connection</code> object's database
   * in which to work.
   * <P>
   * If the driver does not support catalogs, it will
   * silently ignore this request.
   * <p>
   * Calling {@code setCatalog} has no effect on previously created or prepared
   * {@code Statement} objects. It is implementation defined whether a DBMS
   * prepare operation takes place immediately when the {@code Connection}
   * method {@code prepareStatement} or {@code prepareCall} is invoked.
   * For maximum portability, {@code setCatalog} should be called before a
   * {@code Statement} is created or prepared.
   *
   * @param catalog the name of a catalog (subspace in this
   *                <code>Connection</code> object's database) in which to work
   */
  def setCatalog(catalog: String): F[Unit]

  /**
   * Retrieves this <code>Connection</code> object's current catalog name.
   *
   * @return the current catalog name or <code>None</code> if there is none
   */
  def getCatalog(): F[String]

  /**
   * Attempts to change the transaction isolation level for this Connection object to the one given. The constants
   * defined in the interface Connection are the possible transaction isolation levels.
   *
   * Note: If this method is called during a transaction, the result is implementation-defined.
   *
   * @param level
   *   one of the following Connection constants: Connection.TRANSACTION_READ_UNCOMMITTED,
   *   Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ, or
   *   Connection.TRANSACTION_SERIALIZABLE. (Note that Connection.TRANSACTION_NONE cannot be used because it specifies
   *   that transactions are not supported.)
   */
  def setTransactionIsolation(level: Connection.TransactionIsolation): F[Unit]

  /**
   * Retrieves this Connection object's current transaction isolation level.
   *
   * @return
   *   the current transaction isolation level, which will be one of the following constants:
   *   Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_COMMITTED,
   *   Connection.TRANSACTION_REPEATABLE_READ, Connection.TRANSACTION_SERIALIZABLE, or Connection.TRANSACTION_NONE.
   */
  def getTransactionIsolation(): F[Int]

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
   * Creates a <code>PreparedStatement</code> object that will generate
   * <code>ResultSet</code> objects with the given type and concurrency.
   * This method is the same as the <code>prepareStatement</code> method
   * above, but it allows the default result set
   * type and concurrency to be overridden.
   * The holdability of the created result sets can be determined by
   * calling {@link #getHoldability}.
   *
   * @param sql a <code>String</code> object that is the SQL statement to
   *            be sent to the database; may contain one or more '?' IN
   *            parameters
   * @param resultSetType a result set type; one of
   *         <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @param resultSetConcurrency a concurrency type; one of
   *         <code>ResultSet.CONCUR_READ_ONLY</code> or
   *         <code>ResultSet.CONCUR_UPDATABLE</code>
   * @return a new PreparedStatement object containing the
   * pre-compiled SQL statement that will produce <code>ResultSet</code>
   * objects with the given type and concurrency
   */
  def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): F[PreparedStatement[F]]

  /**
   * Creates a <code>CallableStatement</code> object that will generate
   * <code>ResultSet</code> objects with the given type and concurrency.
   * This method is the same as the <code>prepareCall</code> method
   * above, but it allows the default result set
   * type and concurrency to be overridden.
   * The holdability of the created result sets can be determined by
   * calling {@link # getHoldability}.
   *
   * @param sql                  a <code>String</code> object that is the SQL statement to
   *                             be sent to the database; may contain on or more '?' parameters
   * @param resultSetType        a result set type; one of
   *                             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *                             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @param resultSetConcurrency a concurrency type; one of
   *                             <code>ResultSet.CONCUR_READ_ONLY</code> or
   *                             <code>ResultSet.CONCUR_UPDATABLE</code>
   * @return a new <code>CallableStatement</code> object containing the
   *         pre-compiled SQL statement that will produce <code>ResultSet</code>
   *         objects with the given type and concurrency
   */
  def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): F[CallableStatement[F]]

  /**
   * Creates a default <code>PreparedStatement</code> object that has
   * the capability to retrieve auto-generated keys. The given constant
   * tells the driver whether it should make auto-generated keys
   * available for retrieval.  This parameter is ignored if the SQL statement
   * is not an <code>INSERT</code> statement, or an SQL statement able to return
   * auto-generated keys (the list of such statements is vendor-specific).
   * <P>
   * <B>Note:</B> This method is optimized for handling
   * parametric SQL statements that benefit from precompilation. If
   * the driver supports precompilation,
   * the method <code>prepareStatement</code> will send
   * the statement to the database for precompilation. Some drivers
   * may not support precompilation. In this case, the statement may
   * not be sent to the database until the <code>PreparedStatement</code>
   * object is executed.  This has no direct effect on users; however, it does
   * affect which methods throw certain SQLExceptions.
   * <P>
   * Result sets created using the returned <code>PreparedStatement</code>
   * object will by default be type <code>TYPE_FORWARD_ONLY</code>
   * and have a concurrency level of <code>CONCUR_READ_ONLY</code>.
   * The holdability of the created result sets can be determined by
   * calling {@link #getHoldability}.
   *
   * @param sql an SQL statement that may contain one or more '?' IN
   *        parameter placeholders
   * @param autoGeneratedKeys a flag indicating whether auto-generated keys
   *        should be returned; one of
   *        <code>Statement.RETURN_GENERATED_KEYS</code> or
   *        <code>Statement.NO_GENERATED_KEYS</code>
   * @return a new <code>PreparedStatement</code> object, containing the
   *         pre-compiled SQL statement, that will have the capability of
   *         returning auto-generated keys
   */
  def prepareStatement(
    sql:               String,
    autoGeneratedKeys: Int
  ): F[PreparedStatement[F]]

  /**
   * Sets the schema name that will be used for subsequent queries.
   *
   * Calling setSchema has no effect on previously created or prepared Statement objects.
   * It is implementation defined whether a DBMS prepare operation takes place immediately when the Connection method [[statement]] or [[clientPreparedStatement]], [[serverPreparedStatement]] is invoked.
   * For maximum portability, setSchema should be called before a Statement is created or prepared.
   *
   * @param schema
   * the name of a schema in which to work
   */
  def setSchema(schema: String): F[Unit]

  /**
   * Retrieves this Connection object's current schema name.
   *
   * @return
   * the current schema name or null if there is none
   */
  def getSchema(): F[String]

  /**
   * Returns true if the connection has not been closed and is still valid. The driver shall submit a query on the
   * connection or use some other mechanism that positively verifies the connection is still valid when this method is
   * called.
   *
   * The query submitted by the driver to validate the connection shall be executed in the context of the current
   * transaction.
   *
   * @param timeout
   *   The time in seconds to wait for the database operation used to validate the connection to complete. If the
   *   timeout period expires before the operation completes, this method returns false. A value of 0 indicates a
   *   timeout is not applied to the database operation.
   * @return
   *   true if the connection is valid, false otherwise
   */
  def isValid(timeout: Int): F[Boolean]

object Connection:

  /**
   * Enum to specify transaction isolation level
   *
   *   - TRANSACTION_NONE: Transactions are not supported.
   *
   *   - TRANSACTION_READ_UNCOMMITTED: Data with uncommitted changes can be read from other transactions.
   *
   *   - TRANSACTION_READ_COMMITTED: Only committed changes can be read from other transactions.
   *
   *   - TRANSACTION_REPEATABLE_READ: Ensure that the same query always returns the same results, unaffected by changes
   *     from other transactions.
   *
   *   - TRANSACTION_SERIALIZABLE: Transactions are treated as if they are serialized to each other to guarantee data
   *     integrity.
   */
  enum TransactionIsolation(val code: Int):
    case TRANSACTION_NONE             extends TransactionIsolation(java.sql.Connection.TRANSACTION_NONE)
    case TRANSACTION_READ_UNCOMMITTED extends TransactionIsolation(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED)
    case TRANSACTION_READ_COMMITTED   extends TransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED)
    case TRANSACTION_REPEATABLE_READ  extends TransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ)
    case TRANSACTION_SERIALIZABLE     extends TransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE)
