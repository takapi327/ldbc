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

import ldbc.sql.{ Statement, ResultSet }

import ldbc.connector.ResultSetImpl
import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.Protocol
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

private[ldbc] case class StatementImpl[F[_]: Temporal: Exchange: Tracer](
  protocol:             Protocol[F],
  serverVariables:      Map[String, String],
  batchedArgs:          Ref[F, Vector[String]],
  connectionClosed:     Ref[F, Boolean],
  statementClosed:      Ref[F, Boolean],
  resultSetClosed:      Ref[F, Boolean],
  currentResultSet:     Ref[F, Option[ResultSet[F]]],
  updateCount:          Ref[F, Long],
  moreResults:          Ref[F, Boolean],
  autoGeneratedKeys:    Ref[F, Int],
  lastInsertId:         Ref[F, Long],
  resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
)(using ev: MonadError[F, Throwable])
  extends StatementImpl.ShareStatement[F]:

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
              resultSet = ResultSetImpl
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
              resultSet = ResultSetImpl(
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
              lastInsertId.set(result.lastInsertId) *> updateCount.updateAndGet(_ => result.affectedRows).map(_.toInt)
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
          case resultSet: ResultSetImpl[F] => resultSet.hasRows()
          case _                           => ev.pure(false)
        }
      else executeUpdate(sql).map(_ => false)
    )

  override def addBatch(sql: String): F[Unit] = batchedArgs.update(_ :+ sql)

  override def clearBatch(): F[Unit] = batchedArgs.set(Vector.empty)

  override def executeBatch(): F[Array[Int]] =
    checkClosed() *> protocol.resetSequenceId *>
      protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON) *>
      exchange[F, Array[Int]]("statement") { (span: Span[F]) =>
        batchedArgs.get.flatMap { args =>
          span.addAttributes(
            (attributes ++ List(
              Attribute("execute", "batch"),
              Attribute("size", args.length.toLong),
              Attribute("sql", args.toArray.toSeq)
            ))*
          ) *> (
            if args.isEmpty then ev.pure(Array.empty)
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
                              lastInsertId.set(result.lastInsertId) *> ev.pure(acc :+ result.affectedRows.toInt)
                            case error: ERRPacket =>
                              ev.raiseError(error.toException("Failed to execute batch", acc))
                            case _: EOFPacket => ev.raiseError(new SQLException("Unexpected EOF packet"))
                          }
                    yield result
                  }
                  .map(_.toArray)
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
          resultSet = ResultSetImpl(
                        Vector(new ColumnDefinitionPacket:
                          override def table: String = ""

                          override def name: String = "GENERATED_KEYS"

                          override def columnType: ColumnDataType = ColumnDataType.MYSQL_TYPE_LONGLONG

                          override def flags: Seq[ColumnDefinitionFlags] = Seq.empty
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

object StatementImpl:

  private[ldbc] trait ShareStatement[F[_]: Temporal](using ev: MonadError[F, Throwable]) extends Statement[F]:

    def statementClosed:   Ref[F, Boolean]
    def connectionClosed:  Ref[F, Boolean]
    def currentResultSet:  Ref[F, Option[ResultSet[F]]]
    def updateCount:       Ref[F, Long]
    def moreResults:       Ref[F, Boolean]
    def autoGeneratedKeys: Ref[F, Int]
    def lastInsertId:      Ref[F, Long]

    override def getResultSet():   F[Option[ResultSet[F]]] = checkClosed() *> currentResultSet.get
    override def getUpdateCount(): F[Int]                  = checkClosed() *> updateCount.get.map(_.toInt)
    override def getMoreResults(): F[Boolean]              = checkClosed() *> moreResults.get

    override def executeUpdate(sql: String, autoGeneratedKeys: Int): F[Int] =
      this.autoGeneratedKeys.set(autoGeneratedKeys) *> executeUpdate(sql)

    override def execute(sql: String, autoGeneratedKeys: Int): F[Boolean] =
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
