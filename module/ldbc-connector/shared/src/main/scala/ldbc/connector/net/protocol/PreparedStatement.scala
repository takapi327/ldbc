/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import java.time.*

import scala.collection.immutable.ListMap

import cats.*
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{ Tracer, Span }

import ldbc.sql.{ Statement, PreparedStatement, ResultSet }

import ldbc.connector.ResultSetImpl
import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.Protocol
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

object PreparedStatementImpl:

  private[ldbc] trait Share[F[_]: Temporal] extends PreparedStatement[F], StatementImpl.ShareStatement[F]:

    def params: Ref[F, ListMap[Int, Parameter]]

    override def setNull(index: Int, sqlType: Int): F[Unit] =
      params.update(_ + (index -> Parameter.none))

    override def setBoolean(index: Int, value: Boolean): F[Unit] =
      params.update(_ + (index -> Parameter.boolean(value)))

    override def setByte(index: Int, value: Byte): F[Unit] =
      params.update(_ + (index -> Parameter.byte(value)))

    override def setShort(index: Int, value: Short): F[Unit] =
      params.update(_ + (index -> Parameter.short(value)))

    override def setInt(index: Int, value: Int): F[Unit] =
      params.update(_ + (index -> Parameter.int(value)))

    override def setLong(index: Int, value: Long): F[Unit] =
      params.update(_ + (index -> Parameter.long(value)))

    override def setFloat(index: Int, value: Float): F[Unit] =
      params.update(_ + (index -> Parameter.float(value)))

    override def setDouble(index: Int, value: Double): F[Unit] =
      params.update(_ + (index -> Parameter.double(value)))

    override def setBigDecimal(index: Int, value: BigDecimal): F[Unit] =
      params.update(_ + (index -> Parameter.bigDecimal(value)))

    override def setString(index: Int, value: String): F[Unit] =
      params.update(_ + (index -> Parameter.string(value)))

    override def setBytes(index: Int, value: Array[Byte]): F[Unit] =
      params.update(_ + (index -> Parameter.bytes(value)))

    override def setTime(index: Int, value: LocalTime): F[Unit] =
      params.update(_ + (index -> Parameter.time(value)))

    override def setDate(index: Int, value: LocalDate): F[Unit] =
      params.update(_ + (index -> Parameter.date(value)))

    override def setTimestamp(index: Int, value: LocalDateTime): F[Unit] =
      params.update(_ + (index -> Parameter.datetime(value)))

    override def setObject(parameterIndex: Int, value: Object): F[Unit] =
      value match
        case null => setNull(parameterIndex, MysqlType.NULL.jdbcType)
        case value if value.isInstanceOf[Boolean] => setBoolean(parameterIndex, value.asInstanceOf[Boolean])
        case value if value.isInstanceOf[Byte]    => setByte(parameterIndex, value.asInstanceOf[Byte])
        case value if value.isInstanceOf[Short]   => setShort(parameterIndex, value.asInstanceOf[Short])
        case value if value.isInstanceOf[Int]     => setInt(parameterIndex, value.asInstanceOf[Int])
        case value if value.isInstanceOf[Long]    => setLong(parameterIndex, value.asInstanceOf[Long])
        case value if value.isInstanceOf[Float]   => setFloat(parameterIndex, value.asInstanceOf[Float])
        case value if value.isInstanceOf[Double]  => setDouble(parameterIndex, value.asInstanceOf[Double])
        case value if value.isInstanceOf[String]  => setString(parameterIndex, value.asInstanceOf[String])
        case value if value.isInstanceOf[Array[Byte]] => setBytes(parameterIndex, value.asInstanceOf[Array[Byte]])
        case value if value.isInstanceOf[LocalTime] => setTime(parameterIndex, value.asInstanceOf[LocalTime])
        case value if value.isInstanceOf[LocalDate] => setDate(parameterIndex, value.asInstanceOf[LocalDate])
        case value if value.isInstanceOf[LocalDateTime] => setTimestamp(parameterIndex, value.asInstanceOf[LocalDateTime])
        case unknown => throw new SQLException(s"Unsupported object type ${unknown.getClass.getName} for setObject")

    protected def buildQuery(original: String, params: ListMap[Int, Parameter]): String =
      val query = original.toCharArray
      params
        .foldLeft(query) {
          case (query, (offset, param)) =>
            val index = query.indexOf('?', offset - 1)
            if index < 0 then query
            else
              val (head, tail) = query.splitAt(index)
              val (tailHead, tailTail) = tail.splitAt(1)
              head ++ param.sql ++ tailTail
        }
        .mkString

    protected def buildBatchQuery(original: String, params: ListMap[Int, Parameter]): String =
      val placeholderCount = original.split("\\?", -1).length - 1
      require(placeholderCount == params.size, "The number of parameters does not match the number of placeholders")
      original.trim.toLowerCase match
        case q if q.startsWith("insert") =>
          val bindQuery = buildQuery(original, params)
          bindQuery.split("VALUES").last
        case q if q.startsWith("update") || q.startsWith("delete") => buildQuery(original, params)
        case _ => throw new IllegalArgumentException("The batch query must be an INSERT, UPDATE, or DELETE statement.")

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
  case class Client[F[_]: Temporal: Exchange: Tracer](
    protocol:             Protocol[F],
    serverVariables:      Map[String, String],
    sql:                  String,
    params:               Ref[F, ListMap[Int, Parameter]],
    batchedArgs:          Ref[F, Vector[String]],
    connectionClosed:     Ref[F, Boolean],
    statementClosed:      Ref[F, Boolean],
    resultSetClosed:      Ref[F, Boolean],
    currentResultSet:     Ref[F, Option[ResultSet[F]]],
    updateCount:          Ref[F, Int],
    moreResults:          Ref[F, Boolean],
    autoGeneratedKeys:    Ref[F, Int],
    lastInsertId:         Ref[F, Int],
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
  )(using ev: MonadError[F, Throwable])
    extends Share[F]:

    private val attributes = protocol.initialPacket.attributes ++ List(
      Attribute("type", "Client PreparedStatement"),
      Attribute("sql", sql)
    )

    override def executeQuery(): F[ResultSet[F]] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, ResultSet[F]]("statement") { (span: Span[F]) =>
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
                for
                  lastColumnReadNullable <- Ref[F].of(true)
                  resultSetCurrentCursor <- Ref[F].of(0)
                  resultSetCurrentRow    <- Ref[F].of[Option[ResultSetRowPacket]](None)
                yield ResultSetImpl
                  .empty(
                    serverVariables,
                    protocol.initialPacket.serverVersion,
                    resultSetClosed,
                    lastColumnReadNullable,
                    resultSetCurrentCursor,
                    resultSetCurrentRow
                  )
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
        } <* params.set(ListMap.empty)
      }

    override def executeUpdate(): F[Int] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, Int]("statement") { (span: Span[F]) =>
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
              case result: OKPacket => lastInsertId.set(result.lastInsertId) *> ev.pure(result.affectedRows)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
            }
        } <* params.set(ListMap.empty)
      }

    override def execute(): F[Boolean] =
      if sql.toUpperCase.startsWith("SELECT") then
        executeQuery().flatMap {
          case resultSet: ResultSetImpl[F] => resultSet.hasRows()
          case _                           => ev.pure(false)
        }
      else executeUpdate().map(_ => false)

    override def addBatch(): F[Unit] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> params.get.flatMap { params =>
        batchedArgs.update(_ :+ buildBatchQuery(sql, params))
      } *> params.set(ListMap.empty)

    override def clearBatch(): F[Unit] = batchedArgs.set(Vector.empty)

    override def executeBatch(): F[Array[Int]] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> (
        sql.trim.toLowerCase match
          case q if q.startsWith("insert") =>
            exchange[F, Array[Int]]("statement") { (span: Span[F]) =>
              protocol.resetSequenceId *>
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
                          ComQueryPacket(
                            sql.split("VALUES").head + " VALUES" + args.mkString(","),
                            protocol.initialPacket.capabilityFlags,
                            ListMap.empty
                          )
                        ) *>
                        protocol
                          .receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags))
                          .flatMap {
                            case _: OKPacket      => ev.pure(Array.fill(args.length)(Statement.SUCCESS_NO_INFO))
                            case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                            case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
                          }
                  )
                }
            } <* params.set(ListMap.empty) <* batchedArgs.set(Vector.empty)
          case q if q.startsWith("update") || q.startsWith("delete") =>
            protocol.resetSequenceId *>
              protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON) *>
              exchange[F, Array[Int]]("statement") { (span: Span[F]) =>
                protocol.resetSequenceId *>
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
                            ComQueryPacket(
                              args.mkString(";"),
                              protocol.initialPacket.capabilityFlags,
                              ListMap.empty
                            )
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
                            .map(_.toArray)
                    )
                  }
              } <*
              protocol.resetSequenceId <*
              protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF) <*
              params.set(ListMap.empty) <*
              batchedArgs.set(Vector.empty)
          case _ =>
            ev.raiseError(
              new IllegalArgumentException("The batch query must be an INSERT, UPDATE, or DELETE statement.")
            )
      )

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

    override def close(): F[Unit] = statementClosed.set(true) *> resultSetClosed.set(true)

  /**
   * PreparedStatement for query construction at the server side.
   *
   * @param protocol
   *   Protocol is a protocol to communicate with MySQL server.
   * @param statementId
   *   the statement id
   * @param sql
   *   the SQL statement
   * @param params
   *   the parameters
   * @tparam F
   *   The effect type
   */
  case class Server[F[_]: Temporal: Exchange: Tracer](
    protocol:             Protocol[F],
    serverVariables:      Map[String, String],
    statementId:          Long,
    sql:                  String,
    params:               Ref[F, ListMap[Int, Parameter]],
    batchedArgs:          Ref[F, Vector[String]],
    connectionClosed:     Ref[F, Boolean],
    statementClosed:      Ref[F, Boolean],
    resultSetClosed:      Ref[F, Boolean],
    currentResultSet:     Ref[F, Option[ResultSet[F]]],
    updateCount:          Ref[F, Int],
    moreResults:          Ref[F, Boolean],
    autoGeneratedKeys:    Ref[F, Int],
    lastInsertId:         Ref[F, Int],
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
  )(using ev: MonadError[F, Throwable])
    extends Share[F]:

    private val attributes = List(
      Attribute("type", "Server PreparedStatement"),
      Attribute("sql", sql)
    )

    override def executeQuery(): F[ResultSet[F]] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, ResultSet[F]]("statement") { (span: Span[F]) =>
        for
          parameter <- params.get
          columnCount <-
            span.addAttributes(
              (attributes ++ List(
                Attribute("params", parameter.map((_, param) => param.toString).mkString(", ")),
                Attribute("execute", "query")
              ))*
            ) *>
              protocol.resetSequenceId *>
              protocol.send(ComStmtExecutePacket(statementId, parameter)) *>
              protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
                case _: OKPacket                 => ev.pure(ColumnsNumberPacket(0))
                case error: ERRPacket            => ev.raiseError(error.toException("Failed to execute query", sql))
                case result: ColumnsNumberPacket => ev.pure(result)
              }
          columnDefinitions <-
            protocol.repeatProcess(
              columnCount.size,
              ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
            )
          resultSetRow <-
            protocol.readUntilEOF[BinaryProtocolResultSetRowPacket](
              BinaryProtocolResultSetRowPacket.decoder(protocol.initialPacket.capabilityFlags, columnDefinitions),
              Vector.empty
            )
          _                      <- params.set(ListMap.empty)
          lastColumnReadNullable <- Ref[F].of(true)
          resultSetCurrentCursor <- Ref[F].of(0)
          resultSetCurrentRow    <- Ref[F].of[Option[ResultSetRowPacket]](resultSetRow.headOption)
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

    override def executeUpdate(): F[Int] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> exchange[F, Int]("statement") { (span: Span[F]) =>
        params.get.flatMap { params =>
          span.addAttributes(
            (attributes ++ List(
              Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
              Attribute("execute", "update")
            ))*
          ) *>
            protocol.resetSequenceId *>
            protocol.send(ComStmtExecutePacket(statementId, params)) *>
            protocol.receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
              case result: OKPacket => lastInsertId.set(result.lastInsertId) *> ev.pure(result.affectedRows)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
            }
        } <* params.set(ListMap.empty)
      }

    override def execute(): F[Boolean] =
      if sql.toUpperCase.startsWith("SELECT") then
        executeQuery().flatMap {
          case resultSet: ResultSetImpl[F] => resultSet.hasRows()
          case _                           => ev.pure(false)
        }
      else executeUpdate().map(_ => false)

    override def addBatch(): F[Unit] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> params.get.flatMap { params =>
        batchedArgs.update(_ :+ buildBatchQuery(sql, params))
      } *> params.set(ListMap.empty)

    override def clearBatch(): F[Unit] = batchedArgs.set(Vector.empty)

    override def executeBatch(): F[Array[Int]] =
      checkClosed() *> checkNullOrEmptyQuery(sql) *> (
        sql.trim.toLowerCase match
          case q if q.startsWith("insert") =>
            exchange[F, Array[Int]]("statement") { (span: Span[F]) =>
              protocol.resetSequenceId *>
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
                          ComQueryPacket(
                            sql.split("VALUES").head + " VALUES" + args.mkString(","),
                            protocol.initialPacket.capabilityFlags,
                            ListMap.empty
                          )
                        ) *>
                        protocol
                          .receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags))
                          .flatMap {
                            case _: OKPacket      => ev.pure(Array.fill(args.length)(Statement.SUCCESS_NO_INFO))
                            case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                            case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
                          }
                  )
                }
            } <* params.set(ListMap.empty) <* batchedArgs.set(Vector.empty)
          case q if q.startsWith("update") || q.startsWith("delete") =>
            protocol.resetSequenceId *>
              protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON) *>
              exchange[F, Array[Int]]("statement") { (span: Span[F]) =>
                protocol.resetSequenceId *>
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
                            ComQueryPacket(
                              args.mkString(";"),
                              protocol.initialPacket.capabilityFlags,
                              ListMap.empty
                            )
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
                            .map(_.toArray)
                    )
                  }
              } <*
              protocol.resetSequenceId <*
              protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF) <*
              params.set(ListMap.empty) <*
              batchedArgs.set(Vector.empty)
          case _ =>
            ev.raiseError(
              new IllegalArgumentException("The batch query must be an INSERT, UPDATE, or DELETE statement.")
            )
      )

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

    override def close(): F[Unit] =
      exchange[F, Unit]("statement") { (span: Span[F]) =>
        span.addAttributes(
          (attributes ++ List(Attribute("execute", "close"), Attribute("statementId", statementId)))*
        ) *> protocol.resetSequenceId *> protocol.send(ComStmtClosePacket(statementId)) *> statementClosed.set(
          true
        ) *> resultSetClosed.set(true)
      }
