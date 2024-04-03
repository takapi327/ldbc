/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.util.UUID

import scala.concurrent.duration.Duration

import com.comcast.ip4s.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.data.*
import ldbc.connector.net.*
import ldbc.connector.net.protocol.*
import ldbc.connector.net.packet.response.StatisticsPacket
import ldbc.connector.exception.MySQLException
import ldbc.connector.codec.text.text

/**
 * A connection (session) with a specific database. SQL statements are executed and results are returned within the context of a connection.
 * 
 * @tparam F
 *   the effect type
 */
trait Connection[F[_]]:

  /**
   * Puts this connection in read-only mode as a hint to the driver to enable
   * database optimizations.
   *
   * @param isReadOnly
   *   true enables read-only mode; false disables it
   */
  def setReadOnly(isReadOnly: Boolean): F[Unit]

  /**
   * Sets this connection's auto-commit mode to the given state.
   * If a connection is in auto-commit mode, then all its SQL statements will be executed and committed as individual transactions.
   * Otherwise, its SQL statements are grouped into transactions that are terminated by a call to either the method commit or the method rollback.
   * By default, new connections are in auto-commit mode.
   *
   * @param isAutoCommit
   *   true to enable auto-commit mode; false to disable it
   */
  def setAutoCommit(isAutoCommit: Boolean): F[Unit]

  /**
   * Retrieves the current auto-commit mode for this Connection object.
   *
   * @return
   *   the current state of this Connection object's auto-commit mode
   */
  def getAutoCommit: F[Boolean]

  /**
   * Makes all changes made since the previous commit/rollback permanent and releases any database locks currently held by this Connection object.
   * This method should be used only when auto-commit mode has been disabled.
   */
  def commit(): F[Unit]

  /**
   * Undoes all changes made in the current transaction and releases any database locks currently held by this Connection object.
   * This method should be used only when auto-commit mode has been disabled.
   */
  def rollback(): F[Unit]

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
   * Creates a statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def statement(sql: String): Statement[F]

  /**
   * Creates a client-side prepared statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]]

  /**
   * Creates a server prepared statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]]

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
   * Releases this Connection object's database and LDBC resources immediately instead of waiting for them to be automatically released.
   *
   * Calling the method close on a Connection object that is already closed is a no-op.
   *
   * It is strongly recommended that an application explicitly commits or rolls back an active transaction prior to calling the close method.
   * If the close method is called and there is an active transaction, the results are implementation-defined.
   */
  def close(): F[Unit]

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
   * Controls whether or not multiple SQL statements are allowed to be executed at once.
   *
   * @param optionOperation
   *   [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON]] or [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF]]
   */
  def setOption(optionOperation: EnumMySQLSetOption): F[Unit]
  
  /**
   * Enables multiple SQL statements to be executed at once.
   */
  def enableMultiQueries: F[Unit] = setOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON)

  /**
   * Disables multiple SQL statements to be executed at once.
   */
  def disableMultiQueries: F[Unit] = setOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF)

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

  case class ConnectionImpl[F[_]: Temporal: Tracer: Console](
    protocol:   MySQLProtocol[F],
    readOnly:   Ref[F, Boolean],
    autoCommit: Ref[F, Boolean]
  )(using ev: MonadError[F, Throwable])
    extends Connection[F]:
    override def setReadOnly(isReadOnly: Boolean): F[Unit] =
      readOnly.update(_ => isReadOnly) *>
        protocol
          .statement("SET SESSION TRANSACTION READ " + (if isReadOnly then "ONLY" else "WRITE"))
          .executeQuery()
          .void

    override def isReadOnly: F[Boolean] = readOnly.get

    override def setAutoCommit(isAutoCommit: Boolean): F[Unit] =
      autoCommit.update(_ => isAutoCommit) *>
        protocol
          .statement("SET autocommit=" + (if isAutoCommit then "1" else "0"))
          .executeQuery()
          .void

    override def getAutoCommit: F[Boolean] = autoCommit.get

    override def commit(): F[Unit] = autoCommit.get.flatMap { autoCommit =>
      if !autoCommit then protocol.statement("COMMIT").executeQuery().void
      else ev.raiseError(new MySQLException("Can't call commit when autocommit=true"))
    }

    override def rollback(): F[Unit] = autoCommit.get.flatMap { autoCommit =>
      if !autoCommit then protocol.statement("ROLLBACK").executeQuery().void
      else ev.raiseError(new MySQLException("Can't call rollback when autocommit=true"))
    }

    override def setTransactionIsolation(level: TransactionIsolationLevel): F[Unit] =
      protocol.statement(s"SET SESSION TRANSACTION ISOLATION LEVEL ${ level.name }").executeQuery().void

    override def getTransactionIsolation: F[Connection.TransactionIsolationLevel] =
      protocol.statement("SELECT @@session.transaction_isolation").executeQuery().map { result =>
        result.rows.headOption.flatMap(_.values.headOption).flatten match
          case Some("READ-UNCOMMITTED") => Connection.TransactionIsolationLevel.READ_UNCOMMITTED
          case Some("READ-COMMITTED")   => Connection.TransactionIsolationLevel.READ_COMMITTED
          case Some("REPEATABLE-READ")  => Connection.TransactionIsolationLevel.REPEATABLE_READ
          case Some("SERIALIZABLE")     => Connection.TransactionIsolationLevel.SERIALIZABLE
          case Some(unknown)            => throw new MySQLException(s"Unknown transaction isolation level $unknown")
          case None                     => throw new MySQLException("Unknown transaction isolation level")
      }

    override def statement(sql: String): Statement[F] = protocol.statement(sql)

    override def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]] =
      protocol.clientPreparedStatement(sql)

    override def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]] =
      protocol.serverPreparedStatement(sql)

    override def setSavepoint(): F[Savepoint] = setSavepoint(UUID.randomUUID().toString)

    override def setSavepoint(name: String): F[Savepoint] =
      protocol
        .statement(s"SAVEPOINT `$name`")
        .executeQuery()
        .map(_ =>
          new Savepoint:
            override def getSavepointName: String = name
        )

    override def rollback(savepoint: Savepoint): F[Unit] =
      protocol.statement(s"ROLLBACK TO SAVEPOINT `${ savepoint.getSavepointName }`").executeQuery().void

    override def releaseSavepoint(savepoint: Savepoint): F[Unit] =
      protocol.statement(s"RELEASE SAVEPOINT `${ savepoint.getSavepointName }`").executeQuery().void

    override def close(): F[Unit] = getAutoCommit.flatMap { autoCommit =>
      if !autoCommit then protocol.statement("ROLLBACK").executeQuery().void
      else ev.unit
    }

    override def setSchema(schema: String): F[Unit] = protocol.setSchema(schema)

    override def getSchema: F[String] = protocol.statement("SELECT DATABASE()").executeQuery().map { result =>
      result.decode(text).headOption.getOrElse("")
    }

    override def getStatistics: F[StatisticsPacket] = protocol.getStatistics

    override def isValid: F[Boolean] = protocol.isValid

    override def resetServerState: F[Unit] =
      protocol.resetConnection *>
        protocol.statement("SET NAMES utf8mb4").executeQuery() *>
        protocol.statement("SET character_set_results = NULL").executeQuery() *>
        protocol.statement("SET autocommit=1").executeQuery() *>
        autoCommit.update(_ => true)
      
    override def setOption(optionOperation: EnumMySQLSetOption): F[Unit] = protocol.setOption(optionOperation)

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
    allowPublicKeyRetrieval: Boolean = false
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
                      allowPublicKeyRetrieval
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
    allowPublicKeyRetrieval: Boolean = false
  ): Resource[F, Connection[F]] =
    val capabilityFlags = defaultCapabilityFlags ++
      (if database.isDefined then List(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB) else List.empty) ++
      (if sslOptions.isDefined then List(CapabilitiesFlags.CLIENT_SSL) else List.empty)
    for
      protocol <- MySQLProtocol[F](sockets, debug, sslOptions, readTimeout, capabilityFlags)
      _ <- Resource.eval(
             protocol.authenticate(
               user,
               password.getOrElse(""),
               database,
               sslOptions.isDefined,
               allowPublicKeyRetrieval,
               capabilityFlags
             )
           )
      readOnly   <- Resource.eval(Ref[F].of[Boolean](false))
      autoCommit <- Resource.eval(Ref[F].of[Boolean](true))
      connection <- Resource.make(Temporal[F].pure(ConnectionImpl[F](protocol, readOnly, autoCommit)))(_.close())
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
    allowPublicKeyRetrieval: Boolean = false
  )(using ev: Temporal[F]): Resource[F, Connection[F]] =

    def fail[A](msg: String): Resource[F, A] =
      Resource.eval(ev.raiseError(new MySQLException(sql = None, message = msg)))

    def sockets: Resource[F, Socket[F]] =
      (Hostname.fromString(host), Port.fromInt(port)) match
        case (Some(validHost), Some(validPort)) =>
          socketGroup.client(SocketAddress(validHost, validPort), socketOptions)
        case (None, _) => fail(s"""Hostname: "$host" is not syntactically valid.""")
        case (_, None) => fail(s"Port: $port falls out of the allowed range.")

    fromSockets(sockets, host, port, user, password, database, debug, sslOptions, readTimeout, allowPublicKeyRetrieval)
