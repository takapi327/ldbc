/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.collection.immutable.SortedMap

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.{ Connection, Statement, PreparedStatement, CallableStatement, ResultSet, DatabaseMetaData, Savepoint }

import ldbc.connector.data.*
import ldbc.connector.util.StringHelper
import ldbc.connector.exception.*
import ldbc.connector.net.*
import ldbc.connector.net.protocol.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*

private[ldbc] case class ConnectionImpl[F[_]: Temporal: Tracer: Console: Exchange](
  protocol:         Protocol[F],
  serverVariables:  Map[String, String],
  database:         Option[String],
  readOnly:         Ref[F, Boolean],
  isAutoCommit:     Ref[F, Boolean],
  connectionClosed: Ref[F, Boolean],
  databaseTerm:     Option[DatabaseMetaData.DatabaseTerm] = None
)(using ev: MonadError[F, Throwable])
  extends LdbcConnection[F]:

  override def createStatement(): F[Statement[F]] =
    createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def prepareStatement(sql: String): F[PreparedStatement[F]] =
    prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def prepareCall(sql: String): F[CallableStatement[F]] =
    prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def nativeSQL(sql: String): F[String] = ev.pure(sql)

  override def setAutoCommit(autoCommit: Boolean): F[Unit] =
    isAutoCommit.update(_ => autoCommit) *>
      createStatement()
        .flatMap(_.executeQuery("SET autocommit=" + (if autoCommit then "1" else "0")))
        .void

  override def getAutoCommit(): F[Boolean] = isAutoCommit.get

  override def commit(): F[Unit] = isAutoCommit.get.flatMap { autoCommit =>
    if !autoCommit then createStatement().flatMap(_.executeQuery("COMMIT")).void
    else
      ev.raiseError(
        new SQLNonTransientException(
          "Can't call commit when autocommit=true",
          hint = Some("Use setAutoCommit(false) to disable autocommit.")
        )
      )
  }

  override def rollback(): F[Unit] = isAutoCommit.get.flatMap { autoCommit =>
    if !autoCommit then createStatement().flatMap(_.executeQuery("ROLLBACK")).void
    else
      ev.raiseError(
        new SQLNonTransientException(
          "Can't call rollback when autocommit=true",
          hint = Some("Use setAutoCommit(false) to disable autocommit.")
        )
      )
  }

  override def close(): F[Unit] = getAutoCommit().flatMap { autoCommit =>
    (if !autoCommit then createStatement().flatMap(_.executeQuery("ROLLBACK")).void
     else ev.unit) *> protocol.resetSequenceId *> protocol.comQuit() *> connectionClosed.set(true)
  }

  override def isClosed(): F[Boolean] = connectionClosed.get

  override def getMetaData(): F[DatabaseMetaData[F]] =
    isClosed().ifM(
      ev.raiseError(new SQLException("No operations allowed after connection closed.")),
      (for
        statementClosed <- Ref[F].of[Boolean](false)
        resultSetClosed <- Ref[F].of[Boolean](false)
      yield DatabaseMetaDataImpl[F](
        protocol,
        serverVariables,
        connectionClosed,
        statementClosed,
        resultSetClosed,
        database,
        databaseTerm
      ))
    )

  override def setReadOnly(isReadOnly: Boolean): F[Unit] =
    readOnly.update(_ => isReadOnly) *>
      createStatement()
        .flatMap(_.executeQuery("SET SESSION TRANSACTION READ " + (if isReadOnly then "ONLY" else "WRITE")))
        .void

  override def isReadOnly: F[Boolean] = readOnly.get

  override def setCatalog(catalog: String): F[Unit] =
    if databaseTerm.contains(DatabaseMetaData.DatabaseTerm.CATALOG) then setSchema(catalog)
    else ev.unit

  override def getCatalog(): F[String] =
    if databaseTerm.contains(DatabaseMetaData.DatabaseTerm.CATALOG) then getSchema()
    else ev.pure("")

  override def setTransactionIsolation(level: Int): F[Unit] =
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
        ev.raiseError(
          SQLFeatureNotSupportedException.submitIssues(
            s"Unknown transaction isolation level $unknown",
            Some("Expected READ-UNCOMMITTED, READ-COMMITTED, REPEATABLE-READ, or SERIALIZABLE")
          )
        )

  override def getTransactionIsolation(): F[Int] =
    for
      statement            <- createStatement()
      result               <- statement.executeQuery("SELECT @@session.transaction_isolation")
    yield Option(result.getString(1)) match
      case Some("READ-UNCOMMITTED") => Connection.TRANSACTION_READ_UNCOMMITTED
      case Some("READ-COMMITTED")   => Connection.TRANSACTION_READ_COMMITTED
      case Some("REPEATABLE-READ")  => Connection.TRANSACTION_REPEATABLE_READ
      case Some("SERIALIZABLE")     => Connection.TRANSACTION_SERIALIZABLE
      case Some(unknown) =>
        throw SQLFeatureNotSupportedException.submitIssues(
          s"Unknown transaction isolation level $unknown",
          Some("Expected READ-UNCOMMITTED, READ-COMMITTED, REPEATABLE-READ, or SERIALIZABLE")
        )
      case None =>
        throw SQLFeatureNotSupportedException.submitIssues(
          "Unknown transaction isolation level",
          Some("Expected READ-UNCOMMITTED, READ-COMMITTED, REPEATABLE-READ, or SERIALIZABLE")
        )

  override def createStatement(resultSetType: Int, resultSetConcurrency: Int): F[Statement[F]] =
    for
      batchedArgs      <- Ref[F].of(Vector.empty[String])
      statementClosed  <- Ref[F].of[Boolean](false)
      resultSetClosed  <- Ref[F].of[Boolean](false)
      currentResultSet <- Ref[F].of[Option[ResultSet]](None)
      updateCount      <- Ref[F].of(-1L)
      moreResults      <- Ref[F].of(false)
      autoGeneratedKeys <-
        Ref[F].of(Statement.NO_GENERATED_KEYS)
      lastInsertId <- Ref[F].of(0L)
    yield StatementImpl[F](
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
      resultSetType,
      resultSetConcurrency
    )

  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): F[PreparedStatement[F]] =
    for
      params           <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs      <- Ref[F].of(Vector.empty[String])
      statementClosed  <- Ref[F].of[Boolean](false)
      resultSetClosed  <- Ref[F].of[Boolean](false)
      currentResultSet <- Ref[F].of[Option[ResultSet]](None)
      updateCount      <- Ref[F].of(-1L)
      moreResults      <- Ref[F].of(false)
      autoGeneratedKeys <-
        Ref[F].of(Statement.NO_GENERATED_KEYS)
      lastInsertId <- Ref[F].of(0L)
    yield ClientPreparedStatement[F](
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
      resultSetType,
      resultSetConcurrency
    )

  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): F[CallableStatement[F]] =
    for
      metaData <- getMetaData()
      procName <- extractProcedureName(sql)
      resultSet <- ev.pure(databaseTerm.contains(DatabaseMetaData.DatabaseTerm.SCHEMA))
                     .ifM(
                       metaData.getProcedureColumns(None, database, Some(procName), Some("%")),
                       metaData.getProcedureColumns(database, None, Some(procName), Some("%"))
                     )
      paramInfo =
        CallableStatementImpl.ParamInfo(
          sql,
          database,
          resultSet.asInstanceOf[ResultSetImpl],
          isFunctionCall = false
        )
      params                <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs           <- Ref[F].of(Vector.empty[String])
      statementClosed       <- Ref[F].of[Boolean](false)
      resultSetClosed       <- Ref[F].of[Boolean](false)
      currentResultSet      <- Ref[F].of[Option[ResultSet]](None)
      outputParameterResult <- Ref[F].of[Option[ResultSetImpl]](None)
      resultSets            <- Ref[F].of(List.empty[ResultSetImpl])
      parameterIndexToRsIndex <-
        Ref[F].of(
          List
            .fill(paramInfo.numParameters)(CallableStatementImpl.NOT_OUTPUT_PARAMETER_INDICATOR)
            .zipWithIndex
            .map((param, index) => index -> param)
            .toMap
        )
      updateCount       <- Ref[F].of(-1L)
      moreResults       <- Ref[F].of(false)
      autoGeneratedKeys <- Ref[F].of(Statement.NO_GENERATED_KEYS)
      lastInsertId      <- Ref[F].of(0L)
    yield CallableStatementImpl[F](
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
      resultSetType,
      resultSetConcurrency
    )

  override def prepareStatement(
    sql:               String,
    autoGeneratedKeys: Int
  ): F[PreparedStatement[F]] =
    for
      params           <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs      <- Ref[F].of(Vector.empty[String])
      statementClosed  <- Ref[F].of[Boolean](false)
      resultSetClosed  <- Ref[F].of[Boolean](false)
      currentResultSet <- Ref[F].of[Option[ResultSet]](None)
      updateCount      <- Ref[F].of(-1L)
      moreResults      <- Ref[F].of(false)
      autoGeneratedKeys <-
        Ref[F].of(autoGeneratedKeys)
      lastInsertId <- Ref[F].of(0L)
    yield ClientPreparedStatement[F](
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
      ResultSet.TYPE_FORWARD_ONLY,
      ResultSet.CONCUR_READ_ONLY
    )

  override def clientPreparedStatement(sql: String): F[ClientPreparedStatement[F]] =
    clientPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def clientPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int
  ): F[ClientPreparedStatement[F]] =
    for
      params            <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs       <- Ref[F].of(Vector.empty[String])
      statementClosed   <- Ref[F].of[Boolean](false)
      resultSetClosed   <- Ref[F].of[Boolean](false)
      currentResultSet  <- Ref[F].of[Option[ResultSet]](None)
      updateCount       <- Ref[F].of(-1L)
      moreResults       <- Ref[F].of(false)
      autoGeneratedKeys <- Ref[F].of(Statement.NO_GENERATED_KEYS)
      lastInsertId      <- Ref[F].of(0L)
    yield ClientPreparedStatement[F](
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
      resultSetType,
      resultSetConcurrency
    )

  override def clientPreparedStatement(
    sql:               String,
    autoGeneratedKeys: Int
  ): F[ClientPreparedStatement[F]] =
    for
      params            <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs       <- Ref[F].of(Vector.empty[String])
      statementClosed   <- Ref[F].of[Boolean](false)
      resultSetClosed   <- Ref[F].of[Boolean](false)
      currentResultSet  <- Ref[F].of[Option[ResultSet]](None)
      updateCount       <- Ref[F].of(-1L)
      moreResults       <- Ref[F].of(false)
      autoGeneratedKeys <- Ref[F].of(autoGeneratedKeys)
      lastInsertId      <- Ref[F].of(0L)
    yield ClientPreparedStatement[F](
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
      ResultSet.TYPE_FORWARD_ONLY,
      ResultSet.CONCUR_READ_ONLY
    )

  override def serverPreparedStatement(sql: String): F[ServerPreparedStatement[F]] =
    serverPreparedStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  override def serverPreparedStatement(
    sql:                  String,
    resultSetType:        Int,
    resultSetConcurrency: Int
  ): F[ServerPreparedStatement[F]] =
    for
      result <- protocol.resetSequenceId *> protocol.send(ComStmtPreparePacket(sql)) *>
                  protocol.receive(ComStmtPrepareOkPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
                    case error: ERRPacket           => ev.raiseError(error.toException(Some(sql), None))
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
      params            <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs       <- Ref[F].of(Vector.empty[String])
      statementClosed   <- Ref[F].of[Boolean](false)
      resultSetClosed   <- Ref[F].of[Boolean](false)
      currentResultSet  <- Ref[F].of[Option[ResultSet]](None)
      updateCount       <- Ref[F].of(-1L)
      moreResults       <- Ref[F].of(false)
      autoGeneratedKeys <- Ref[F].of(Statement.NO_GENERATED_KEYS)
      lastInsertId      <- Ref[F].of(0L)
    yield ServerPreparedStatement[F](
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
      resultSetType,
      resultSetConcurrency
    )

  override def serverPreparedStatement(
    sql:               String,
    autoGeneratedKeys: Int
  ): F[ServerPreparedStatement[F]] =
    for
      result <- protocol.resetSequenceId *> protocol.send(ComStmtPreparePacket(sql)) *>
                  protocol.receive(ComStmtPrepareOkPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
                    case error: ERRPacket           => ev.raiseError(error.toException(Some(sql), None))
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
      params            <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs       <- Ref[F].of(Vector.empty[String])
      statementClosed   <- Ref[F].of[Boolean](false)
      resultSetClosed   <- Ref[F].of[Boolean](false)
      currentResultSet  <- Ref[F].of[Option[ResultSet]](None)
      updateCount       <- Ref[F].of(-1L)
      moreResults       <- Ref[F].of(false)
      autoGeneratedKeys <- Ref[F].of(autoGeneratedKeys)
      lastInsertId      <- Ref[F].of(0L)
    yield ServerPreparedStatement[F](
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
      ResultSet.TYPE_FORWARD_ONLY,
      ResultSet.CONCUR_READ_ONLY
    )

  override def setSavepoint(): F[Savepoint] = setSavepoint(StringHelper.getUniqueSavepointId)

  override def setSavepoint(name: String): F[Savepoint] =
    for
      statement <- createStatement()
      _         <- statement.executeQuery(s"SAVEPOINT `$name`")
    yield MysqlSavepoint(name)

  override def rollback(savepoint: Savepoint): F[Unit] =
    createStatement().flatMap(_.executeQuery(s"ROLLBACK TO SAVEPOINT `${ savepoint.getSavepointName() }`")).void

  override def releaseSavepoint(savepoint: Savepoint): F[Unit] =
    createStatement().flatMap(_.executeQuery(s"RELEASE SAVEPOINT `${ savepoint.getSavepointName() }`")).void

  override def setSchema(schema: String): F[Unit] = protocol.resetSequenceId *> protocol.comInitDB(schema)

  override def getSchema(): F[String] =
    for
      statement <- createStatement()
      result    <- statement.executeQuery("SELECT DATABASE()")
    yield Option(result.getString(1)).getOrElse("")

  override def getStatistics: F[StatisticsPacket] = protocol.resetSequenceId *> protocol.comStatistics()

  override def isValid(timeout: Int): F[Boolean] = protocol.resetSequenceId *> protocol.comPing()

  override def resetServerState: F[Unit] =
    protocol.resetSequenceId *> protocol.resetConnection *> createStatement().flatMap { statement =>
      statement.executeQuery("SET NAMES utf8mb4") *>
        statement.executeQuery("SET character_set_results = NULL") *>
        statement.executeQuery("SET autocommit=1") *>
        isAutoCommit.update(_ => true)
    }

  override def changeUser(user: String, password: String): F[Unit] =
    protocol.resetSequenceId *> protocol.changeUser(user, password)

  private def extractProcedureName(sql: String): F[String] =
    val (keyword, offset) =
      if sql.toUpperCase.contains("CALL ") then ("CALL ", 5)
      else if sql.toUpperCase.contains("SELECT ") then ("SELECT ", 7)
      else ("", -1)

    if offset != -1 then
      val endCallIndex     = StringHelper.indexOfIgnoreCase(0, sql, keyword)
      val trimmedStatement = sql.substring(endCallIndex + offset).trim()
      val name             = trimmedStatement.takeWhile(c => !Character.isWhitespace(c) && c != '(' && c != '?')
      ev.pure(name)
    else ev.raiseError(new SQLException("Invalid SQL statement"))
