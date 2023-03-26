/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import java.sql.{ Blob, Clob, NClob, SQLWarning, SQLXML, Struct }
import java.util.Properties
import java.util.concurrent.Executor

/** A connection (session) with a specific database. SQL statements are executed and results are returned within the
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
  * method ResultSet.getObject, the getObject method will check the connection's type map to see if there is an entry
  * for that UDT. If so, the getObject method will map the UDT to the class indicated. If there is no entry, the UDT
  * will be mapped using the standard mapping.
  *
  * A user may create a new type map, which is a java.util.Map object, make an entry in it, and pass it to the java.sql
  * methods that can perform custom mapping. In this case, the method will use the given type map instead of the one
  * associated with the connection.
  * @tparam F
  *   The effect type
  */
trait Connection[F[_]]:

  /** Creates a Statement object for sending SQL statements to the database. SQL statements without parameters are
    * normally executed using Statement objects. If the same SQL statement is executed many times, it may be more
    * efficient to use a PreparedStatement object.
    *
    * Result sets created using the returned Statement object will by default be type TYPE_FORWARD_ONLY and have a
    * concurrency level of CONCUR_READ_ONLY. The holdability of the created result sets can be determined by calling
    * getHoldability.
    *
    * @return
    *   a new default Statement object
    */
  def createStatement(): F[Statement[F]]

  /** Creates a PreparedStatement object for sending parameterized SQL statements to the database.
    *
    * A SQL statement with or without IN parameters can be pre-compiled and stored in a PreparedStatement object. This
    * object can then be used to efficiently execute this statement multiple times.
    *
    * Note: This method is optimized for handling parametric SQL statements that benefit from precompilation. If the
    * driver supports precompilation, the method prepareStatement will send the statement to the database for
    * precompilation. Some drivers may not support precompilation. In this case, the statement may not be sent to the
    * database until the PreparedStatement object is executed. This has no direct effect on users; however, it does
    * affect which methods throw certain SQLException objects.
    *
    * Result sets created using the returned PreparedStatement object will by default be type TYPE_FORWARD_ONLY and have
    * a concurrency level of CONCUR_READ_ONLY. The holdability of the created result sets can be determined by calling
    * getHoldability.
    *
    * @param sql
    *   an SQL statement that may contain one or more '?' IN parameter placeholders
    * @return
    *   q new default PreparedStatement object containing the pre-compiled SQL statement
    */
  def prepareStatement(sql: String): F[PreparedStatement[F]]

  /** Converts the given SQL statement into the system's native SQL grammar. A driver may convert the JDBC SQL grammar
    * into its system's native SQL grammar prior to sending it. This method returns the native form of the statement
    * that the driver would have sent.
    *
    * @param sql
    *   an SQL statement that may contain one or more '?' parameter placeholders
    * @return
    *   the native form of this statement
    */
  def nativeSQL(sql: String): F[String]

  /** Sets this connection's auto-commit mode to the given state. If a connection is in auto-commit mode, then all its
    * SQL statements will be executed and committed as individual transactions. Otherwise, its SQL statements are
    * grouped into transactions that are terminated by a call to either the method commit or the method rollback. By
    * default, new connections are in auto-commit mode.
    *
    * The commit occurs when the statement completes. The time when the statement completes depends on the type of SQL
    * Statement:
    *
    *   - For DML statements, such as Insert, Update or Delete, and DDL statements, the statement is complete as soon as
    *     it has finished executing.
    *
    *   - For Select statements, the statement is complete when the associated result set is closed.
    *
    *   - For CallableStatement objects or for statements that return multiple results, the statement is complete when
    *     all of the associated result sets have been closed, and all update counts and output parameters have been
    *     retrieved.
    *
    * NOTE: If this method is called during a transaction and the auto-commit mode is changed, the transaction is
    * committed. If setAutoCommit is called and the auto-commit mode is not changed, the call is a no-op.
    *
    * @param autoCommit
    *   true to enable auto-commit mode; false to disable it
    */
  def setAutoCommit(autoCommit: Boolean): F[Unit]

  /** Retrieves the current auto-commit mode for this Connection object.
    *
    * @return
    *   the current state of this Connection object's auto-commit mode
    */
  def getAutoCommit(): F[Boolean]

  /** Makes all changes made since the previous commit/rollback permanent and releases any database locks currently held
    * by this Connection object. This method should be used only when auto-commit mode has been disabled.
    */
  def commit(): F[Unit]

  /** Undoes all changes made in the current transaction and releases any database locks currently held by this
    * Connection object. This method should be used only when auto-commit mode has been disabled.
    */
  def rollback(): F[Unit]

  /** Releases this Connection object's database and JDBC resources immediately instead of waiting for them to be
    * automatically released.
    *
    * Calling the method close on a Connection object that is already closed is a no-op.
    *
    * it is strongly recommended that an application explicitly commits or rolls back an active transaction prior to
    * calling the close method. If the close method is called and there is an active transaction, the results are
    * implementation-defined.
    */
  def close(): F[Unit]

  /** Retrieves whether this Connection object has been closed. A connection is closed if the method close has been
    * called on it or if certain fatal errors have occurred. This method is guaranteed to return true only when it is
    * called after the method Connection.close has been called.
    *
    * This method generally cannot be called to determine whether a connection to a database is valid or invalid. A
    * typical client can determine that a connection is invalid by catching any exceptions that might be thrown when an
    * operation is attempted.
    *
    * @return
    *   true if this Connection object is closed; false if it is still open
    */
  def isClosed(): F[Boolean]

  /** Puts this connection in read-only mode as a hint to the driver to enable database optimizations.
    *
    * Note: This method cannot be called during a transaction.
    *
    * @param readOnly
    *   true enables read-only mode; false disables it
    */
  def setReadOnly(readOnly: Boolean): F[Unit]

  /** Retrieves whether this Connection object is in read-only mode.
    *
    * @return
    *   true if this Connection object is read-only; false otherwise
    */
  def isReadOnly(): F[Boolean]

  /** Sets the given catalog name in order to select a subspace of this Connection object's database in which to work.
    *
    * If the driver does not support catalogs, it will silently ignore this request.
    *
    * Calling setCatalog has no effect on previously created or prepared Statement objects. It is implementation defined
    * whether a DBMS prepare operation takes place immediately when the Connection method prepareStatement or
    * prepareCall is invoked. For maximum portability, setCatalog should be called before a Statement is created or
    * prepared.
    *
    * @param catalog
    *   the name of a catalog (subspace in this Connection object's database) in which to work
    */
  def setCatalog(catalog: String): F[Unit]

  /** Retrieves this Connection object's current catalog name.
    *
    * @return
    *   the current catalog name or null if there is none
    */
  def getCatalog(): F[String]

  /** Attempts to change the transaction isolation level for this Connection object to the one given. The constants
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

  /** Retrieves this Connection object's current transaction isolation level.
    *
    * @return
    *   the current transaction isolation level, which will be one of the following constants:
    *   Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_COMMITTED,
    *   Connection.TRANSACTION_REPEATABLE_READ, Connection.TRANSACTION_SERIALIZABLE, or Connection.TRANSACTION_NONE.
    */
  def getTransactionIsolation(): F[Int]

  /** Retrieves the first warning reported by calls on this Connection object. If there is more than one warning,
    * subsequent warnings will be chained to the first one and can be retrieved by calling the method
    * SQLWarning.getNextWarning on the warning that was retrieved previously.
    *
    * This method may not be called on a closed connection; doing so will cause an SQLException to be thrown.
    *
    * Note: Subsequent warnings will be chained to this SQLWarning.
    *
    * @return
    *   the first SQLWarning object or null if there are none
    */
  def getWarnings(): F[java.sql.SQLWarning]

  /** Clears all warnings reported for this Connection object. After a call to this method, the method getWarnings
    * returns null until a new warning is reported for this Connection object.
    */
  def clearWarnings(): F[Unit]

  /** Creates a Statement object that will generate ResultSet objects with the given type and concurrency. This method
    * is the same as the createStatement method above, but it allows the default result set type and concurrency to be
    * overridden. The holdability of the created result sets can be determined by calling getHoldability.
    *
    * @param resultSetType
    *   a result set type; one of ResultSet.Type.TYPE_FORWARD_ONLY, ResultSet.Type.TYPE_SCROLL_INSENSITIVE, or
    *   ResultSet.Type.TYPE_SCROLL_SENSITIVE
    * @param resultSetConcurrency
    *   a concurrency type; one of ResultSet.Concur.CONCUR_READ_ONLY or ResultSet.Concur.CONCUR_UPDATABLE
    * @return
    *   a new Statement object that will generate ResultSet objects with the given type and concurrency
    */
  def createStatement(resultSetType: ResultSet.Type, resultSetConcurrency: ResultSet.Concur): F[Statement[F]]

  /** Creates a PreparedStatement object that will generate ResultSet objects with the given type and concurrency. This
    * method is the same as the prepareStatement method above, but it allows the default result set type and concurrency
    * to be overridden. The holdability of the created result sets can be determined by calling getHoldability.
    *
    * @param sql
    *   a String object that is the SQL statement to be sent to the database; may contain one or more '?' IN parameters
    * @param resultSetType
    *   a result set type; one of ResultSet.Type.TYPE_FORWARD_ONLY, ResultSet.Type.TYPE_SCROLL_INSENSITIVE, or
    *   ResultSet.Type.TYPE_SCROLL_SENSITIVE
    * @param resultSetConcurrency
    *   a concurrency type; one of ResultSet.Concur.CONCUR_READ_ONLY or ResultSet.Concur.CONCUR_UPDATABLE
    * @return
    *   a new PreparedStatement object containing the pre-compiled SQL statement that will produce ResultSet objects
    *   with the given type and concurrency
    */
  def prepareStatement(
    sql:                  String,
    resultSetType:        ResultSet.Type,
    resultSetConcurrency: ResultSet.Concur
  ): F[PreparedStatement[F]]

  /** Retrieves the Map object associated with this Connection object. Unless the application has added an entry, the
    * type map returned will be empty.
    *
    * @return
    *   the java.util.Map object associated with this Connection object
    */
  def getTypeMap(): F[Map[String, Class[?]]]

  /** Installs the given TypeMap object as the type map for this Connection object. The type map will be used for the
    * custom mapping of SQL structured types and distinct types.
    *
    * @param map
    *   the java.util.Map object to install as the replacement for this Connection object's default type map
    */
  def setTypeMap(map: Map[String, Class[?]]): F[Unit]

  /** Changes the default holdability of ResultSet objects created using this Connection object to the given
    * holdability. The default holdability of ResultSet objects can be determined by invoking
    * DatabaseMetaData.getResultSetHoldability.
    *
    * @param holdability
    *   a ResultSet holdability constant; one of ResultSet.Holdability.HOLD_CURSORS_OVER_COMMIT or
    *   ResultSet.Holdability.CLOSE_CURSORS_AT_COMMIT
    */
  def setHoldability(holdability: Int): F[Unit]

  /** Retrieves the current holdability of ResultSet objects created using this Connection object.
    *
    * @return
    *   the holdability, one of ResultSet.Holdability.HOLD_CURSORS_OVER_COMMIT or
    *   ResultSet.Holdability.CLOSE_CURSORS_AT_COMMIT
    */
  def getHoldability(): F[Int]

  /** Creates a Statement object that will generate ResultSet objects with the given type, concurrency, and holdability.
    * This method is the same as the createStatement method above, but it allows the default result set type,
    * concurrency, and holdability to be overridden.
    *
    * @param resultSetType
    *   one of the following ResultSet constants: ResultSet.Type.TYPE_FORWARD_ONLY,
    *   ResultSet.Type.TYPE_SCROLL_INSENSITIVE, or ResultSet.Type.TYPE_SCROLL_SENSITIVE
    * @param resultSetConcurrency
    *   one of the following ResultSet constants: ResultSet.Concur.CONCUR_READ_ONLY or ResultSet.Concur.CONCUR_UPDATABLE
    * @param resultSetHoldability
    *   one of the following ResultSet constants: ResultSet.Holdability.HOLD_CURSORS_OVER_COMMIT or
    *   ResultSet.Holdability.CLOSE_CURSORS_AT_COMMIT
    * @return
    *   a new Statement object that will generate ResultSet objects with the given type, concurrency, and holdability
    */
  def createStatement(
    resultSetType:        ResultSet.Type,
    resultSetConcurrency: ResultSet.Concur,
    resultSetHoldability: ResultSet.Holdability
  ): F[Statement[F]]

  /** Creates a PreparedStatement object that will generate ResultSet objects with the given type, concurrency, and
    * holdability.
    *
    * This method is the same as the prepareStatement method above, but it allows the default result set type,
    * concurrency, and holdability to be overridden.
    *
    * @param sql
    *   a String object that is the SQL statement to be sent to the database; may contain one or more '?' IN parameters
    * @param resultSetType
    *   one of the following ResultSet constants: ResultSet.Type.TYPE_FORWARD_ONLY,
    *   ResultSet.Type.TYPE_SCROLL_INSENSITIVE, or ResultSet.Type.TYPE_SCROLL_SENSITIVE
    * @param resultSetConcurrency
    *   one of the following ResultSet constants: ResultSet.Concur.CONCUR_READ_ONLY or ResultSet.Concur.CONCUR_UPDATABLE
    * @param resultSetHoldability
    *   one of the following ResultSet constants: ResultSet.Holdability.HOLD_CURSORS_OVER_COMMIT or
    *   ResultSet.Holdability.CLOSE_CURSORS_AT_COMMIT
    * @return
    *   a new PreparedStatement object, containing the pre-compiled SQL statement, that will generate ResultSet objects
    *   with the given type, concurrency, and holdability
    */
  def prepareStatement(
    sql:                  String,
    resultSetType:        ResultSet.Type,
    resultSetConcurrency: ResultSet.Concur,
    resultSetHoldability: ResultSet.Holdability
  ): F[PreparedStatement[F]]

  /** Creates a default PreparedStatement object that has the capability to retrieve auto-generated keys. The given
    * constant tells the driver whether it should make auto-generated keys available for retrieval. This parameter is
    * ignored if the SQL statement is not an INSERT statement, or an SQL statement able to return auto-generated keys
    * (the list of such statements is vendor-specific).
    *
    * Note: This method is optimized for handling parametric SQL statements that benefit from precompilation. If the
    * driver supports precompilation, the method prepareStatement will send the statement to the database for
    * precompilation. Some drivers may not support precompilation. In this case, the statement may not be sent to the
    * database until the PreparedStatement object is executed. This has no direct effect on users; however, it does
    * affect which methods throw certain SQLExceptions.
    *
    * Result sets created using the returned PreparedStatement object will by default be type TYPE_FORWARD_ONLY and have
    * a concurrency level of CONCUR_READ_ONLY. The holdability of the created result sets can be determined by calling
    * getHoldability.
    *
    * @param sql
    *   a String object that is the SQL statement to be sent to the database; may contain one or more '?' IN parameters
    * @param autoGeneratedKeys
    *   a flag indicating whether auto-generated keys should be returned; one of
    *   Statement.Generated.RETURN_GENERATED_KEYS or Statement.Generated.NO_GENERATED_KEYS
    * @return
    *   a new PreparedStatement object, containing the pre-compiled SQL statement, that will have the capability of
    *   returning auto-generated keys
    */
  def prepareStatement(sql: String, autoGeneratedKeys: Statement.Generated): F[PreparedStatement[F]]

  /** Creates a default PreparedStatement object capable of returning the auto-generated keys designated by the given
    * array. This array contains the indexes of the columns in the target table that contain the auto-generated keys
    * that should be made available. The driver will ignore the array if the SQL statement is not an INSERT statement,
    * or an SQL statement able to return auto-generated keys (the list of such statements is vendor-specific).
    *
    * An SQL statement with or without IN parameters can be pre-compiled and stored in a PreparedStatement object. This
    * object can then be used to efficiently execute this statement multiple times.
    *
    * Note: This method is optimized for handling parametric SQL statements that benefit from precompilation. If the
    * driver supports precompilation, the method prepareStatement will send the statement to the database for
    * precompilation. Some drivers may not support precompilation. In this case, the statement may not be sent to the
    * database until the PreparedStatement object is executed. This has no direct effect on users; however, it does
    * affect which methods throw certain SQLExceptions.
    *
    * Result sets created using the returned PreparedStatement object will by default be type TYPE_FORWARD_ONLY and have
    * a concurrency level of CONCUR_READ_ONLY. The holdability of the created result sets can be determined by calling
    * getHoldability.
    *
    * @param sql
    *   an SQL statement that may contain one or more '?' IN parameter placeholders
    * @param columnIndexes
    *   an array of column indexes indicating the columns that should be returned from the inserted row or rows
    * @return
    *   a new PreparedStatement object, containing the pre-compiled statement, that is capable of returning the
    *   auto-generated keys designated by the given array of column indexes
    */
  def prepareStatement(sql: String, columnIndexes: Array[Int]): F[PreparedStatement[F]]

  /** Creates a default PreparedStatement object capable of returning the auto-generated keys designated by the given
    * array. This array contains the names of the columns in the target table that contain the auto-generated keys that
    * should be returned. The driver will ignore the array if the SQL statement is not an INSERT statement, or an SQL
    * statement able to return auto-generated keys (the list of such statements is vendor-specific).
    *
    * An SQL statement with or without IN parameters can be pre-compiled and stored in a PreparedStatement object. This
    * object can then be used to efficiently execute this statement multiple times.
    *
    * Note: This method is optimized for handling parametric SQL statements that benefit from precompilation. If the
    * driver supports precompilation, the method prepareStatement will send the statement to the database for
    * precompilation. Some drivers may not support precompilation. In this case, the statement may not be sent to the
    * database until the PreparedStatement object is executed. This has no direct effect on users; however, it does
    * affect which methods throw certain SQLExceptions.
    *
    * Result sets created using the returned PreparedStatement object will by default be type TYPE_FORWARD_ONLY and have
    * a concurrency level of CONCUR_READ_ONLY. The holdability of the created result sets can be determined by calling
    * getHoldability.
    *
    * @param sql
    *   an SQL statement that may contain one or more '?' IN parameter placeholders
    * @param columnNames
    *   an array of column names indicating the columns that should be returned from the inserted row or rows
    * @return
    *   a new PreparedStatement object, containing the pre-compiled statement, that is capable of returning the
    *   auto-generated keys designated by the given array of column names
    */
  def prepareStatement(sql: String, columnNames: Array[String]): F[PreparedStatement[F]]

  /** Constructs an object that implements the Clob interface. The object returned initially contains no data. The
    * setAsciiStream, setCharacterStream and setString methods of the Clob interface may be used to add data to the
    * Clob.
    *
    * @return
    *   An object that implements the Clob interface
    */
  def createClob(): F[java.sql.Clob]

  /** Constructs an object that implements the Blob interface. The object returned initially contains no data. The
    * setBinaryStream and setBytes methods of the Blob interface may be used to add data to the Blob.
    *
    * @return
    *   An object that implements the Blob interface
    */
  def createBlob(): F[java.sql.Blob]

  /** Constructs an object that implements the NClob interface. The object returned initially contains no data. The
    * setAsciiStream, setCharacterStream and setString methods of the NClob interface may be used to add data to the
    * NClob.
    *
    * @return
    *   An object that implements the NClob interface
    */
  def createNClob(): F[java.sql.NClob]

  /** Constructs an object that implements the SQLXML interface. The object returned initially contains no data. The
    * createXmlStreamWriter object and setString method of the SQLXML interface may be used to add data to the SQLXML
    * object.
    *
    * @return
    *   An object that implements the SQLXML interface
    */
  def createSQLXML(): F[java.sql.SQLXML]

  /** Returns true if the connection has not been closed and is still valid. The driver shall submit a query on the
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

  /** Sets the value of the client info property specified by name to the value specified by value.
    *
    * Applications may use the DatabaseMetaData.getClientInfoProperties method to determine the client info properties
    * supported by the driver and the maximum length that may be specified for each property.
    *
    * The driver stores the value specified in a suitable location in the database. For example in a special register,
    * session parameter, or system table column. For efficiency the driver may defer setting the value in the database
    * until the next time a statement is executed or prepared. Other than storing the client information in the
    * appropriate place in the database, these methods shall not alter the behavior of the connection in anyway. The
    * values supplied to these methods are used for accounting, diagnostics and debugging purposes only.
    *
    * The driver shall generate a warning if the client info name specified is not recognized by the driver.
    *
    * If the value specified to this method is greater than the maximum length for the property the driver may either
    * truncate the value and generate a warning or generate a SQLClientInfoException. If the driver generates a
    * SQLClientInfoException, the value specified was not set on the connection.
    *
    * The following are standard client info properties. Drivers are not required to support these properties however if
    * the driver supports a client info property that can be described by one of the standard properties, the standard
    * property name should be used.
    *
    * ApplicationName - The name of the application currently utilizing the connection
    *
    * ClientUser - The name of the user that the application using the connection is performing work for. This may not
    * be the same as the user name that was used in establishing the connection.
    *
    * ClientHostname - The hostname of the computer the application using the connection is running on.
    *
    * @param name
    *   The name of the client info property to set
    * @param value
    *   The value to set the client info property to. If the value is null, the current value of the specified property
    *   is cleared.
    */
  def setClientInfo(name: String, value: String): F[Unit]

  /** Sets the value of the connection's client info properties. The Properties object contains the names and values of
    * the client info properties to be set. The set of client info properties contained in the properties list replaces
    * the current set of client info properties on the connection. If a property that is currently set on the connection
    * is not present in the properties list, that property is cleared. Specifying an empty properties list will clear
    * all of the properties on the connection. See setClientInfo (String, String) for more information.
    *
    * If an error occurs in setting any of the client info properties, a SQLClientInfoException is thrown. The
    * SQLClientInfoException contains information indicating which client info properties were not set. The state of the
    * client information is unknown because some databases do not allow multiple client info properties to be set
    * atomically. For those databases, one or more properties may have been set before the error occurred.
    *
    * @param properties
    *   the list of client info properties to set
    */
  def setClientInfo(properties: java.util.Properties): F[Unit]

  /** Returns the value of the client info property specified by name. This method may return null if the specified
    * client info property has not been set and does not have a default value. This method will also return null if the
    * specified client info property name is not supported by the driver.
    *
    * Applications may use the DatabaseMetaData.getClientInfoProperties method to determine the client info properties
    * supported by the driver.
    *
    * @param name
    *   The name of the client info property to retrieve
    * @return
    *   The value of the client info property specified
    */
  def getClientInfo(name: String): F[String]

  /** Returns a list containing the name and current value of each client info property supported by the driver. The
    * value of a client info property may be null if the property has not been set and does not have a default value.
    *
    * @return
    *   A Properties object that contains the name and current value of each of the client info properties supported by
    *   the driver.
    */
  def getClientInfo(): F[java.util.Properties]

  /** Factory method for creating Array objects.
    *
    * Note: When createArrayOf is used to create an array object that maps to a primitive data type, then it is
    * implementation-defined whether the Array object is an array of that primitive data type or an array of Object.
    *
    * Note: The JDBC driver is responsible for mapping the elements Object array to the default JDBC SQL type defined in
    * java.sql.Types for the given class of Object. The default mapping is specified in Appendix B of the JDBC
    * specification. If the resulting JDBC type is not the appropriate type for the given typeName then it is
    * implementation defined whether an SQLException is thrown or the driver supports the resulting conversion.
    *
    * @param typeName
    *   the SQL name of the type the elements of the array map to. The typeName is a database-specific name which may be
    *   the name of a built-in type, a user-defined type or a standard SQL type supported by this database. This is the
    *   value returned by Array.getBaseTypeName
    * @param elements
    *   the elements that populate the returned object
    * @return
    *   an Array object whose elements map to the specified SQL type
    */
  def createArrayOf(typeName: String, elements: Array[Object]): F[java.sql.Array]

  /** Factory method for creating Struct objects.
    *
    * @param typeName
    *   the SQL type name of the SQL structured type that this Struct object maps to. The typeName is the name of a
    *   user-defined type that has been defined for this database. It is the value returned by Struct.getSQLTypeName.
    * @param attributes
    *   the attributes that populate the returned object
    * @return
    *   a Struct object that maps to the given SQL type and is populated with the given attributes
    */
  def createStruct(typeName: String, attributes: Array[Object]): F[java.sql.Struct]

  /** Sets the given schema name to access.
    *
    * If the driver does not support schemas, it will silently ignore this request.
    *
    * Calling setSchema has no effect on previously created or prepared Statement objects. It is implementation defined
    * whether a DBMS prepare operation takes place immediately when the Connection method prepareStatement or
    * prepareCall is invoked. For maximum portability, setSchema should be called before a Statement is created or
    * prepared.
    *
    * @param schema
    *   the name of a schema in which to work
    */
  def setSchema(schema: String): F[Unit]

  /** Retrieves this Connection object's current schema name.
    *
    * @return
    *   the current schema name or null if there is none
    */
  def getSchema(): F[String]

  /** Terminates an open connection. Calling abort results in:
    *
    * <ul> <li>The connection marked as closed <li>Closes any physical connection to the database <li>Releases resources
    * used by the connection <li>Insures that any thread that is currently accessing the connection will either progress
    * to completion or throw an <code>SQLException</code>. </ul>
    *
    * Calling abort marks the connection closed and releases any resources. Calling abort on a closed connection is a
    * no-op.
    *
    * it is possible that the aborting and releasing of the resources that are held by the connection can take an
    * extended period of time. When the abort method returns, the connection will have been marked as closed and the
    * Executor that was passed as a parameter to abort may still be executing tasks to release resources.
    *
    * This method checks to see that there is an SQLPermission object before allowing the method to proceed. If a
    * SecurityManager exists and its checkPermission method denies calling abort, this method throws a
    * java.lang.SecurityException.
    *
    * @param executor
    *   The Executor implementation which will be used by abort.
    */
  def abort(executor: java.util.concurrent.Executor): F[Unit]

  /** Sets the maximum period a Connection or objects created from the Connection will wait for the database to reply to
    * any one request. If any request remains unanswered, the waiting method will return with a SQLException, and the
    * Connection or objects created from the Connection will be marked as closed. Any subsequent use of the objects,
    * with the exception of the close, isClosed or Connection.isValid methods, will result in a SQLException.
    *
    * Note: This method is intended to address a rare but serious condition where network partitions can cause threads
    * issuing JDBC calls to hang uninterruptedly in socket reads, until the OS TCP-TIMEOUT (typically 10 minutes). This
    * method is related to the abort() method which provides an administrator thread a means to free any such threads in
    * cases where the JDBC connection is accessible to the administrator thread. The setNetworkTimeout method will cover
    * cases where there is no administrator thread, or it has no access to the connection. This method is severe in it's
    * effects, and should be given a high enough value so it is never triggered before any more normal timeouts, such as
    * transaction timeouts.
    *
    * JDBC driver implementations may also choose to support the setNetworkTimeout method to impose a limit on database
    * response time, in environments where no network is present.
    *
    * Drivers may internally implement some or all of their API calls with multiple internal driver-database
    * transmissions, and it is left to the driver implementation to determine whether the limit will be applied always
    * to the response to the API call, or to any single request made during the API call.
    *
    * This method can be invoked more than once, such as to set a limit for an area of JDBC code, and to reset to the
    * default on exit from this area. Invocation of this method has no impact on already outstanding requests.
    *
    * The Statement.setQueryTimeout() timeout value is independent of the timeout value specified in setNetworkTimeout.
    * If the query timeout expires before the network timeout then the statement execution will be canceled. If the
    * network is still active the result will be that both the statement and connection are still usable. However if the
    * network timeout expires before the query timeout or if the statement timeout fails due to network problems, the
    * connection will be marked as closed, any resources held by the connection will be released and both the connection
    * and statement will be unusable.
    *
    * When the driver determines that the setNetworkTimeout timeout value has expired, the JDBC driver marks the
    * connection closed and releases any resources held by the connection.
    *
    * This method checks to see that there is an SQLPermission object before allowing the method to proceed. If a
    * SecurityManager exists and its checkPermission method denies calling setNetworkTimeout, this method throws a
    * java.lang.SecurityException.
    *
    * @param executor
    *   The Executor implementation which will be used by setNetworkTimeout.
    * @param milliseconds
    *   The time in milliseconds to wait for the database operation to complete. If the JDBC driver does not support
    *   milliseconds, the JDBC driver will round the value up to the nearest second. If the timeout period expires
    *   before the operation completes, a SQLException will be thrown. A value of 0 indicates that there is not timeout
    *   for database operations.
    */
  def setNetworkTimeout(executor: java.util.concurrent.Executor, milliseconds: Int): F[Unit]

  /** Retrieves the number of milliseconds the driver will wait for a database request to complete. If the limit is
    * exceeded, a SQLException is thrown.
    *
    * @return
    *   the current timeout limit in milliseconds; zero means there is no limit
    */
  def getNetworkTimeout(): F[Int]

object Connection:

  /** Enum to specify transaction isolation level
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
