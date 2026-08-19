/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.sql.{ SQLException, SQLNonTransientException }
import scala.collection.immutable.SortedMap

import ldbc.fx.{ Fx, Ref }
import ldbc.fx.syntax.*

import ldbc.mysql.telemetry.Tracer

import ldbc.sql.{ CallableStatement, Connection, DatabaseMetaData, PreparedStatement, ResultSet, Savepoint, Statement }

import ldbc.mysql.data.*
import ldbc.mysql.exception.*
import ldbc.mysql.net.*
import ldbc.mysql.net.packet.request.*
import ldbc.mysql.net.packet.response.*
import ldbc.mysql.net.protocol.*
import ldbc.mysql.telemetry.{ DatabaseMetrics, TelemetryConfig }
import ldbc.mysql.util.StringHelper

private[ldbc] case class ConnectionImpl(
  protocol:           Protocol,
  serverVariables:    Map[String, String],
  database:           Option[String],
  readOnly:           Ref[Boolean],
  isAutoCommit:       Ref[Boolean],
  connectionClosed:   Ref[Boolean],
  useCursorFetch:     Boolean,
  useServerPrepStmts: Boolean,
  databaseTerm:       DatabaseMetaData.DatabaseTerm = DatabaseMetaData.DatabaseTerm.CATALOG,
  telemetryConfig:    TelemetryConfig               = TelemetryConfig.default,
  databaseMetrics:    DatabaseMetrics
)(using Tracer, Exchange)
  extends LdbcConnection:

  override def createStatement(): Fx[Statement[Fx]] =
    createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def prepareStatement(sql: String): Fx[PreparedStatement[Fx]] =
    buildPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def prepareCall(sql: String): Fx[CallableStatement[Fx]] =
    prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def nativeSQL(sql: String): Fx[String] = Fx.pure(sql)

  override def setAutoCommit(autoCommit: Boolean): Fx[Unit] =
    isAutoCommit.update(_ => autoCommit) *>
      createStatement()
        .flatMap(_.executeQuery("SET autocommit=" + (if autoCommit then "1" else "0")))
        .void

  override def getAutoCommit(): Fx[Boolean] = isAutoCommit.get

  override def commit(): Fx[Unit] = isAutoCommit.get.flatMap { autoCommit =>
    if !autoCommit then createStatement().flatMap(_.executeQuery("COMMIT")).void
    else
      Fx.raiseError(
        new SQLNonTransientException(
          "Can't call commit when autocommit=true",
          hint = Some("Use setAutoCommit(false) to disable autocommit.")
        )
      )
  }

  override def rollback(): Fx[Unit] = isAutoCommit.get.flatMap { autoCommit =>
    if !autoCommit then createStatement().flatMap(_.executeQuery("ROLLBACK")).void
    else
      Fx.raiseError(
        new SQLNonTransientException(
          "Can't call rollback when autocommit=true",
          hint = Some("Use setAutoCommit(false) to disable autocommit.")
        )
      )
  }

  override def close(): Fx[Unit] = getAutoCommit().flatMap { autoCommit =>
    (if !autoCommit then createStatement().flatMap(_.executeQuery("ROLLBACK")).void
     else Fx.unit) *> protocol.resetSequenceId *> protocol.comQuit() *> connectionClosed.set(true)
  }

  override def isClosed(): Fx[Boolean] = connectionClosed.get

  override def getMetaData(): Fx[DatabaseMetaData[Fx]] =
    isClosed().ifM(
      Fx.raiseError(new SQLException("No operations allowed after connection closed.")),
      (for
        statementClosed <- Ref.of[Boolean](false)
        resultSetClosed <- Ref.of[Boolean](false)
        fetchSize       <- Ref.of(0)
      yield DatabaseMetaDataImpl(
        protocol,
        serverVariables,
        connectionClosed,
        statementClosed,
        resultSetClosed,
        fetchSize,
        useCursorFetch,
        useServerPrepStmts,
        database,
        databaseTerm,
        telemetryConfig,
        databaseMetrics
      ))
    )

  override def setReadOnly(isReadOnly: Boolean): Fx[Unit] =
    readOnly.update(_ => isReadOnly) *>
      createStatement()
        .flatMap(_.executeQuery("SET SESSION TRANSACTION READ " + (if isReadOnly then "ONLY" else "WRITE")))
        .void

  override def isReadOnly: Fx[Boolean] = readOnly.get

  override def setCatalog(catalog: String): Fx[Unit] =
    databaseTerm match
      case DatabaseMetaData.DatabaseTerm.CATALOG => setSchema(catalog)
      case DatabaseMetaData.DatabaseTerm.SCHEMA  => Fx.unit

  override def getCatalog(): Fx[String] =
    databaseTerm match
      case DatabaseMetaData.DatabaseTerm.CATALOG =>
        for
          statement <- createStatement()
          result    <- statement.executeQuery("SELECT DATABASE()")
          value     <- result.getString(1)
        yield Option(value).getOrElse("")
      case DatabaseMetaData.DatabaseTerm.SCHEMA => Fx.pure(null)

  override def setTransactionIsolation(level: Int): Fx[Unit] =
    level match
      case Connection.TRANSACTION_READ_UNCOMMITTED =>
        createStatement().flatMap(_.executeQuery("SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED")).void
      case Connection.TRANSACTION_READ_COMMITTED =>
        createStatement().flatMap(_.executeQuery("SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED")).void
      case Connection.TRANSACTION_REPEATABLE_READ =>
        createStatement().flatMap(_.executeQuery("SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ")).void
      case Connection.TRANSACTION_SERIALIZABLE =>
        createStatement().flatMap(_.executeQuery("SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE")).void
      case unknown =>
        Fx.raiseError(
          MySQLErrors.featureNotSupported(
            s"Unknown transaction isolation level $unknown",
            Some("Expected READ-UNCOMMITTED, READ-COMMITTED, REPEATABLE-READ, or SERIALIZABLE")
          )
        )

  override def getTransactionIsolation(): Fx[Int] =
    for
      statement <- createStatement()
      result    <- statement.executeQuery("SELECT @@session.transaction_isolation")
      value     <- result.getString(1)
    yield Option(value) match
      case Some("READ-UNCOMMITTED") => Connection.TRANSACTION_READ_UNCOMMITTED
      case Some("READ-COMMITTED")   => Connection.TRANSACTION_READ_COMMITTED
      case Some("REPEATABLE-READ")  => Connection.TRANSACTION_REPEATABLE_READ
      case Some("SERIALIZABLE")     => Connection.TRANSACTION_SERIALIZABLE
      case Some(unknown)            =>
        throw MySQLErrors.featureNotSupported(
          s"Unknown transaction isolation level $unknown",
          Some("Expected READ-UNCOMMITTED, READ-COMMITTED, REPEATABLE-READ, or SERIALIZABLE")
        )
      case None =>
        throw MySQLErrors.featureNotSupported(
          "Unknown transaction isolation level",
          Some("Expected READ-UNCOMMITTED, READ-COMMITTED, REPEATABLE-READ, or SERIALIZABLE")
        )

  override def createStatement(resultSetType: Int, resultSetConcurrency: Int): Fx[Statement[Fx]] =
    for
      batchedArgs       <- Ref.of(Vector.empty[String])
      statementClosed   <- Ref.of[Boolean](false)
      resultSetClosed   <- Ref.of[Boolean](false)
      currentResultSet  <- Ref.of[Option[ResultSet[Fx]]](None)
      updateCount       <- Ref.of(-1L)
      moreResults       <- Ref.of(false)
      autoGeneratedKeys <-
        Ref.of(Statement.NO_GENERATED_KEYS)
      lastInsertId <- Ref.of(0L)
      fetchSize    <- Ref.of(0)
    yield StatementImpl(
      protocol,
      serverVariables,
      batchedArgs,
      connectionClosed,
      statementClosed,
      resultSetClosed,
      currentResultSet,
      updateCount,
      moreResults,
      autoGeneratedKeys,
      lastInsertId,
      fetchSize,
      useCursorFetch,
      useServerPrepStmts,
      resultSetType,
      resultSetConcurrency,
      telemetryConfig,
      databaseMetrics
    )

  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): Fx[PreparedStatement[Fx]] =
    buildPreparedStatement(sql, resultSetType, resultSetConcurrency)

  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): Fx[CallableStatement[Fx]] =
    for
      metaData  <- getMetaData()
      procName  <- extractProcedureName(sql)
      resultSet <- Fx.pure(databaseTerm == DatabaseMetaData.DatabaseTerm.SCHEMA)
                     .ifM(
                       metaData.getProcedureColumns(None, database, Some(procName), Some("%")),
                       metaData.getProcedureColumns(database, None, Some(procName), Some("%"))
                     )
      paramInfo <-
        CallableStatementImpl.ParamInfo(
          sql,
          database,
          resultSet.asInstanceOf[ResultSetImpl],
          isFunctionCall = false
        )
      params                  <- Ref.of(SortedMap.empty[Int, Parameter])
      batchedArgs             <- Ref.of(Vector.empty[String])
      statementClosed         <- Ref.of[Boolean](false)
      resultSetClosed         <- Ref.of[Boolean](false)
      currentResultSet        <- Ref.of[Option[ResultSet[Fx]]](None)
      outputParameterResult   <- Ref.of[Option[ResultSetImpl]](None)
      resultSets              <- Ref.of(List.empty[ResultSetImpl])
      parameterIndexToRsIndex <-
        Ref.of(
          List
            .fill(paramInfo.numParameters)(CallableStatementImpl.NOT_OUTPUT_PARAMETER_INDICATOR)
            .zipWithIndex
            .map((param, index) => index -> param)
            .toMap
        )
      updateCount       <- Ref.of(-1L)
      moreResults       <- Ref.of(false)
      autoGeneratedKeys <- Ref.of(Statement.NO_GENERATED_KEYS)
      lastInsertId      <- Ref.of(0L)
      fetchSize         <- Ref.of(0)
    yield CallableStatementImpl(
      protocol,
      serverVariables,
      sql,
      paramInfo,
      params,
      batchedArgs,
      connectionClosed,
      statementClosed,
      resultSetClosed,
      currentResultSet,
      outputParameterResult,
      resultSets,
      parameterIndexToRsIndex,
      updateCount,
      moreResults,
      autoGeneratedKeys,
      lastInsertId,
      fetchSize,
      useCursorFetch,
      useServerPrepStmts,
      resultSetType,
      resultSetConcurrency,
      telemetryConfig,
      databaseMetrics
    )

  override def prepareStatement(
    sql:               String,
    autoGeneratedKeys: Int
  ): Fx[PreparedStatement[Fx]] =
    buildPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, autoGeneratedKeys)

  override def clientPreparedStatement(sql: String): Fx[ClientPreparedStatement] =
    buildClientPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def clientPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int
  ): Fx[ClientPreparedStatement] =
    buildClientPreparedStatement(sql, resultSetType, resultSetConcurrency)

  override def clientPreparedStatement(
    sql:               String,
    autoGeneratedKeys: Int
  ): Fx[ClientPreparedStatement] =
    buildClientPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, autoGeneratedKeys)

  override def serverPreparedStatement(sql: String): Fx[ServerPreparedStatement] =
    buildServerPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def serverPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int
  ): Fx[ServerPreparedStatement] =
    buildServerPreparedStatement(sql, resultSetType, resultSetConcurrency, Statement.NO_GENERATED_KEYS)

  override def serverPreparedStatement(
    sql:               String,
    autoGeneratedKeys: Int
  ): Fx[ServerPreparedStatement] =
    buildServerPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, autoGeneratedKeys)

  override def setSavepoint(): Fx[Savepoint] = StringHelper.getUniqueSavepointId.flatMap(setSavepoint)

  override def setSavepoint(name: String): Fx[Savepoint] =
    for
      statement <- createStatement()
      _         <- statement.executeQuery(s"SAVEPOINT `$name`")
    yield MysqlSavepoint(name)

  override def rollback(savepoint: Savepoint): Fx[Unit] =
    createStatement().flatMap(_.executeQuery(s"ROLLBACK TO SAVEPOINT `${ savepoint.getSavepointName() }`")).void

  override def releaseSavepoint(savepoint: Savepoint): Fx[Unit] =
    createStatement().flatMap(_.executeQuery(s"RELEASE SAVEPOINT `${ savepoint.getSavepointName() }`")).void

  override def setSchema(schema: String): Fx[Unit] = protocol.resetSequenceId *> protocol.comInitDB(schema)

  override def getSchema(): Fx[String] =
    databaseTerm match
      case DatabaseMetaData.DatabaseTerm.SCHEMA =>
        for
          statement <- createStatement()
          result    <- statement.executeQuery("SELECT DATABASE()")
          value     <- result.getString(1)
        yield Option(value).getOrElse("")
      case DatabaseMetaData.DatabaseTerm.CATALOG => Fx.pure(null)

  override def getStatistics: Fx[StatisticsPacket] = protocol.resetSequenceId *> protocol.comStatistics()

  override def isValid(timeout: Int): Fx[Boolean] = protocol.resetSequenceId *> protocol.comPing()

  override def resetServerState: Fx[Unit] =
    protocol.resetSequenceId *> protocol.resetConnection *> createStatement().flatMap { statement =>
      statement.executeQuery("SET NAMES utf8mb4") *>
        statement.executeQuery("SET character_set_results = NULL") *>
        statement.executeQuery("SET autocommit=1") *>
        isAutoCommit.update(_ => true)
    }

  override def changeUser(user: String, password: String): Fx[Unit] =
    protocol.resetSequenceId *> protocol.changeUser(user, password)

  private def extractProcedureName(sql: String): Fx[String] =
    val (keyword, offset) =
      if sql.toUpperCase.contains("CALL ") then ("CALL ", 5)
      else if sql.toUpperCase.contains("SELECT ") then ("SELECT ", 7)
      else ("", -1)

    if offset != -1 then
      val endCallIndex     = StringHelper.indexOfIgnoreCase(0, sql, keyword)
      val trimmedStatement = sql.substring(endCallIndex + offset).trim()
      val name             = trimmedStatement.takeWhile(c => !Character.isWhitespace(c) && c != '(' && c != '?')
      Fx.pure(name)
    else Fx.raiseError(new SQLException("Invalid SQL statement"))

  private def buildClientPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int,
    autoGeneratedKeys:    Int = Statement.NO_GENERATED_KEYS
  ): Fx[ClientPreparedStatement] =
    for
      params            <- Ref.of(SortedMap.empty[Int, Parameter])
      batchedArgs       <- Ref.of(Vector.empty[String])
      statementClosed   <- Ref.of[Boolean](false)
      resultSetClosed   <- Ref.of[Boolean](false)
      currentResultSet  <- Ref.of[Option[ResultSet[Fx]]](None)
      updateCount       <- Ref.of(-1L)
      moreResults       <- Ref.of(false)
      autoGeneratedKeys <- Ref.of(autoGeneratedKeys)
      lastInsertId      <- Ref.of(0L)
      fetchSize         <- Ref.of(0)
    yield ClientPreparedStatement(
      protocol,
      serverVariables,
      sql,
      params,
      batchedArgs,
      connectionClosed,
      statementClosed,
      resultSetClosed,
      currentResultSet,
      updateCount,
      moreResults,
      autoGeneratedKeys,
      lastInsertId,
      fetchSize,
      useCursorFetch,
      useServerPrepStmts,
      resultSetType,
      resultSetConcurrency,
      telemetryConfig,
      databaseMetrics
    )

  private def buildServerPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int,
    autoGeneratedKeys:    Int = Statement.NO_GENERATED_KEYS
  ): Fx[ServerPreparedStatement] =
    for
      result <- protocol.resetSequenceId *> protocol.send(ComStmtPreparePacket(sql)) *>
                  protocol.receive(ComStmtPrepareOkPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
                    case error: ERRPacket           => Fx.raiseError(error.toException(Some(sql), None))
                    case ok: ComStmtPrepareOkPacket => Fx.pure(ok)
                  }
      _ <- protocol.repeatProcess(
             result.numParams,
             ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
           )
      _ <- protocol.repeatProcess(
             result.numColumns,
             ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
           )
      params            <- Ref.of(SortedMap.empty[Int, Parameter])
      batchedArgs       <- Ref.of(Vector.empty[String])
      statementClosed   <- Ref.of[Boolean](false)
      resultSetClosed   <- Ref.of[Boolean](false)
      currentResultSet  <- Ref.of[Option[ResultSet[Fx]]](None)
      updateCount       <- Ref.of(-1L)
      moreResults       <- Ref.of(false)
      autoGeneratedKeys <- Ref.of(autoGeneratedKeys)
      lastInsertId      <- Ref.of(0L)
      fetchSize         <- Ref.of(0)
    yield ServerPreparedStatement(
      protocol,
      serverVariables,
      result.statementId,
      sql,
      params,
      batchedArgs,
      connectionClosed,
      statementClosed,
      resultSetClosed,
      currentResultSet,
      updateCount,
      moreResults,
      autoGeneratedKeys,
      lastInsertId,
      fetchSize,
      useCursorFetch,
      useServerPrepStmts,
      resultSetType,
      resultSetConcurrency,
      telemetryConfig,
      databaseMetrics
    )

  private def buildPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int,
    autoGeneratedKeys:    Int = Statement.NO_GENERATED_KEYS
  ): Fx[PreparedStatement[Fx]] =
    if useServerPrepStmts then
      buildServerPreparedStatement(sql, resultSetType, resultSetConcurrency, autoGeneratedKeys)
        .asInstanceOf[Fx[PreparedStatement[Fx]]]
    else
      buildClientPreparedStatement(sql, resultSetType, resultSetConcurrency, autoGeneratedKeys)
        .asInstanceOf[Fx[PreparedStatement[Fx]]]
