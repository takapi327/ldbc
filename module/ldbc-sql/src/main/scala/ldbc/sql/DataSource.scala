/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import java.io.PrintWriter

/** A factory for connections to the physical data source that this DataSource object represents. An alternative to the
  * DriverManager facility, a DataSource object is the preferred means of getting a connection. An object that
  * implements the DataSource interface will typically be registered with a naming service based on the Java™ Naming and
  * Directory (JNDI) API. <P> The DataSource interface is implemented by a driver vendor. There are three types of
  * implementations: <P> <OL> <LI>Basic implementation -- produces a standard Connection object <LI>Connection pooling
  * implementation -- produces a Connection object that will automatically participate in connection pooling. This
  * implementation works with a middle-tier connection pooling manager. <LI>Distributed transaction implementation --
  * produces a Connection object that may be used for distributed transactions and almost always participates in
  * connection pooling. This implementation works with a middle-tier transaction manager and almost always with a
  * connection pooling manager. </OL> <P> A DataSource object has properties that can be modified when necessary. For
  * example, if the data source is moved to a different server, the property for the server can be changed. The benefit
  * is that because the data source's properties can be changed, any code accessing that data source does not need to be
  * changed. <P> A driver that is accessed via a DataSource object does not register itself with the DriverManager.
  * Rather, a DataSource object is retrieved through a lookup operation and then used to create a Connection object.
  * With a basic implementation, the connection obtained through a DataSource object is identical to a connection
  * obtained through the DriverManager facility.
  *
  * @tparam F
  *   The effect type
  */
trait DataSource[F[_]]:

  /** Attempts to establish a connection with the data source that this DataSource object represents.
    *
    * @return
    *   a connection to the data source
    */
  def getConnection: F[Connection[F]]

  /** Attempts to establish a connection with the data source that this DataSource object represents.
    *
    * @param username
    *   the database user on whose behalf the connection is being made
    * @param password
    *   the user's password
    * @return
    *   a connection to the data source
    */
  def getConnection(username: String, password: String): F[Connection[F]]

  /** Retrieves the log writer for this DataSource object.
    *
    * The log writer is a character output stream to which all logging and tracing messages for this data source will be
    * printed. This includes messages printed by the methods of this object, messages printed by methods of other
    * objects manufactured by this object, and so on. Messages printed to a data source specific log writer are not
    * printed to the log writer associated with the java.sql.DriverManager class. When a DataSource object is created,
    * the log writer is initially null; in other words, the default is for logging to be disabled.
    */
  def getLogWriter: F[PrintWriter]

  /** Sets the log writer for this DataSource object to the given java.io.PrintWriter object.
    *
    * The log writer is a character output stream to which all logging and tracing messages for this data source will be
    * printed. This includes messages printed by the methods of this object, messages printed by methods of other
    * objects manufactured by this object, and so on. Messages printed to a data source- specific log writer are not
    * printed to the log writer associated with the java.sql.DriverManager class. When a DataSource object is created
    * the log writer is initially null; in other words, the default is for logging to be disabled.
    *
    * @param out
    *   the new log writer; to disable logging, set to null
    */
  def setLogWriter(out: PrintWriter): F[Unit]

  /** Sets the maximum time in seconds that this data source will wait while attempting to connect to a database. A
    * value of zero specifies that the timeout is the default system timeout if there is one; otherwise, it specifies
    * that there is no timeout. When a DataSource object is created, the login timeout is initially zero.
    *
    * @param seconds
    *   the data source login time limit
    */
  def setLoginTimeout(seconds: Int): F[Unit]

  /** Gets the maximum time in seconds that this data source can wait while attempting to connect to a database. A value
    * of zero means that the timeout is the default system timeout if there is one; otherwise, it means that there is no
    * timeout. When a DataSource object is created, the login timeout is initially zero.
    */
  def getLoginTimeout: F[Int]
