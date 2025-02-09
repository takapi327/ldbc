/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import scala.collection.immutable.{ ListMap, SortedMap }

import cats.*
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.{ Span, Tracer }
import org.typelevel.otel4s.Attribute

import ldbc.sql.{ ResultSet, Statement }

import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.Protocol
import ldbc.connector.ResultSetImpl

/**
 * PreparedStatement for query construction at the client side.
 *
 * @param protocol
 *   Protocol is a protocol to communicate with MySQL server.
 * @param sql
 *   the SQL statement
 * @param params
 *   the parameters
 * @param ev
 *   the effect type class
 * @tparam F
 *   the effect type
 */
case class ClientPreparedStatement[F[_]: Exchange: Tracer](
  protocol:             Protocol[F],
  serverVariables:      Map[String, String],
  sql:                  String,
  params:               Ref[F, SortedMap[Int, Parameter]],
  batchedArgs:          Ref[F, Vector[String]],
  connectionClosed:     Ref[F, Boolean],
  statementClosed:      Ref[F, Boolean],
  resultSetClosed:      Ref[F, Boolean],
  currentResultSet:     Ref[F, Option[ResultSet]],
  updateCount:          Ref[F, Long],
  moreResults:          Ref[F, Boolean],
  autoGeneratedKeys:    Ref[F, Int],
  lastInsertId:         Ref[F, Long],
  resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
)(using F: MonadThrow[F])
  extends SharedPreparedStatement[F]:

  private val attributes = protocol.initialPacket.attributes ++ List(
    Attribute("type", "Client PreparedStatement"),
    Attribute("sql", sql)
  )

  override def executeQuery(): F[ResultSet] =
    checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, ResultSet]("statement") { (span: Span[F]) =>
      params.get.flatMap { params =>
        span.addAttributes(
          (attributes ++ List(
            Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
            Attribute("execute", "query")
          ))*
        ) *>
          protocol.resetSequenceId *>
          protocol.send(
            ComQueryPacket(buildQuery(sql, params), protocol.initialPacket.capabilityFlags, ListMap.empty)
          ) *>
          protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
            case _: OKPacket =>
              F.pure(
                ResultSetImpl
                  .empty(
                    serverVariables,
                    protocol.initialPacket.serverVersion
                  )
              )
            case error: ERRPacket => F.raiseError(error.toException(Some(sql), None, params))
            case result: ColumnsNumberPacket =>
              for
                columnDefinitions <-
                  protocol.repeatProcess(
                    result.size,
                    ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
                  )
                resultSetRow <-
                  protocol.readUntilEOF[ResultSetRowPacket](
                    ResultSetRowPacket.decoder(protocol.initialPacket.capabilityFlags, columnDefinitions.length)
                  )
                resultSet = ResultSetImpl(
                              columnDefinitions,
                              resultSetRow,
                              serverVariables,
                              protocol.initialPacket.serverVersion,
                              resultSetType,
                              resultSetConcurrency,
                              Some(sql)
                            )
                _ <- currentResultSet.set(Some(resultSet))
              yield resultSet
          }
      } <* params.set(SortedMap.empty)
    }

  override def executeLargeUpdate(): F[Long] =
    checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, Long]("statement") { (span: Span[F]) =>
      params.get.flatMap { params =>
        span.addAttributes(
          (attributes ++ List(
            Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
            Attribute("execute", "update")
          ))*
        ) *>
          protocol.resetSequenceId *>
          protocol.send(
            ComQueryPacket(buildQuery(sql, params), protocol.initialPacket.capabilityFlags, ListMap.empty)
          ) *>
          protocol.receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
            case result: OKPacket => lastInsertId.set(result.lastInsertId) *> F.pure(result.affectedRows)
            case error: ERRPacket => F.raiseError(error.toException(Some(sql), None, params))
            case _: EOFPacket     => F.raiseError(new SQLException("Unexpected EOF packet"))
          }
      } <* params.set(SortedMap.empty)
    }

  override def execute(): F[Boolean] =
    if sql.toUpperCase.startsWith("SELECT") then
      executeQuery().map {
        case resultSet: ResultSetImpl => resultSet.hasRows()
        case _                        => false
      }
    else executeUpdate().map(_ => false)

  override def addBatch(): F[Unit] =
    checkClosed() *> checkNullOrEmptyQuery(sql) *> params.get.flatMap { params =>
      batchedArgs.update(_ :+ buildBatchQuery(sql, params))
    } *> params.set(SortedMap.empty)

  override def clearBatch(): F[Unit] = batchedArgs.set(Vector.empty)

  override def executeLargeBatch(): F[Array[Long]] =
    checkClosed() *> checkNullOrEmptyQuery(sql) *> (
      sql.trim.toLowerCase match
        case q if q.startsWith("insert") =>
          exchange[F, Array[Long]]("statement") { (span: Span[F]) =>
            protocol.resetSequenceId *>
              batchedArgs.get.flatMap { args =>
                span.addAttributes(
                  (attributes ++ List(
                    Attribute("execute", "batch"),
                    Attribute("size", args.length.toLong),
                    Attribute("sql", args.toArray.toSeq)
                  ))*
                ) *> (
                  if args.isEmpty then F.pure(Array.empty)
                  else
                    protocol.resetSequenceId *>
                      protocol.send(
                        ComQueryPacket(
                          sql.split("VALUES").head + " VALUES" + args.mkString(","),
                          protocol.initialPacket.capabilityFlags,
                          ListMap.empty
                        )
                      ) *>
                      protocol
                        .receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags))
                        .flatMap {
                          case _: OKPacket      => F.pure(Array.fill(args.length)(Statement.SUCCESS_NO_INFO.toLong))
                          case error: ERRPacket => F.raiseError(error.toException(Some(sql), None))
                          case _: EOFPacket     => F.raiseError(new SQLException("Unexpected EOF packet"))
                        }
                )
              }
          } <* params.set(SortedMap.empty) <* batchedArgs.set(Vector.empty)
        case q if q.startsWith("update") || q.startsWith("delete") =>
          protocol.resetSequenceId *>
            protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON) *>
            exchange[F, Array[Long]]("statement") { (span: Span[F]) =>
              protocol.resetSequenceId *>
                batchedArgs.get.flatMap { args =>
                  span.addAttributes(
                    (attributes ++ List(
                      Attribute("execute", "batch"),
                      Attribute("size", args.length.toLong),
                      Attribute("sql", args.toArray.toSeq)
                    ))*
                  ) *> (
                    if args.isEmpty then F.pure(Array.empty)
                    else
                      protocol.resetSequenceId *>
                        protocol.send(
                          ComQueryPacket(
                            args.mkString(";"),
                            protocol.initialPacket.capabilityFlags,
                            ListMap.empty
                          )
                        ) *>
                        args
                          .foldLeft(F.pure(Vector.empty[Long])) { ($acc, _) =>
                            for
                              acc <- $acc
                              result <-
                                protocol
                                  .receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags))
                                  .flatMap {
                                    case result: OKPacket =>
                                      lastInsertId.set(result.lastInsertId) *> F.pure(acc :+ result.affectedRows)
                                    case error: ERRPacket =>
                                      F.raiseError(error.toException("Failed to execute batch", acc))
                                    case _: EOFPacket => F.raiseError(new SQLException("Unexpected EOF packet"))
                                  }
                            yield result
                          }
                          .map(_.toArray)
                  )
                }
            } <*
            protocol.resetSequenceId <*
            protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF) <*
            params.set(SortedMap.empty) <*
            batchedArgs.set(Vector.empty)
        case _ =>
          F.raiseError(
            new IllegalArgumentException("The batch query must be an INSERT, UPDATE, or DELETE statement.")
          )
    )

  override def getGeneratedKeys(): F[ResultSet] =
    autoGeneratedKeys.get.flatMap {
      case Statement.RETURN_GENERATED_KEYS =>
        for
          lastInsertId <- lastInsertId.get
          resultSet = ResultSetImpl(
                        Vector(new ColumnDefinitionPacket:
                          override def table:      String                     = ""
                          override def name:       String                     = "GENERATED_KEYS"
                          override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_LONGLONG
                          override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty),
                        Vector(ResultSetRowPacket(Array(Some(lastInsertId.toString)))),
                        serverVariables,
                        protocol.initialPacket.serverVersion
                      )
          _ <- currentResultSet.set(Some(resultSet))
        yield resultSet
      case Statement.NO_GENERATED_KEYS =>
        F.raiseError(
          new SQLException(
            "Generated keys not requested. You need to specify Statement.RETURN_GENERATED_KEYS to Statement.executeUpdate(), Statement.executeLargeUpdate() or Connection.prepareStatement()."
          )
        )
    }

  override def close(): F[Unit] = statementClosed.set(true) *> resultSetClosed.set(true)
