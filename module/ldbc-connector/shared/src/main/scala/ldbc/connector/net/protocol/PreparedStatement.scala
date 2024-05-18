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

import ldbc.sql.{ Statement, ResultSet }

import ldbc.connector.ResultSetImpl
import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.Protocol
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

/**
 * An object that represents a precompiled SQL statement.
 * 
 * A SQL statement is precompiled and stored in a PreparedStatement object. This object can then be used to efficiently
 * execute this statement multiple times.
 * 
 * Note: The setter methods (setShort, setString, and so on) for setting IN parameter values must specify types that are
 * compatible with the defined SQL type of the input parameter. For instance, if the IN parameter has SQL type INTEGER,
 * then the method setInt should be used.
 * 
 * @tparam F
 *   The effect type
 */
trait PreparedStatement[F[_]] extends Statement[F]:

  @deprecated("This method cannot be called on a PreparedStatement.", "0.3.0")
  override def executeQuery(sql: String): F[ResultSet[F]] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  @deprecated("This method cannot be called on a PreparedStatement.", "0.3.0")
  override def executeUpdate(sql: String): F[Int] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  @deprecated("This method cannot be called on a PreparedStatement.", "0.3.0")
  override def execute(sql: String): F[Boolean] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  override def addBatch(sql: String): F[Unit] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  /**
   * Retrieves the current parameter values of this PreparedStatement object.
   */
  def params: Ref[F, ListMap[Int, Parameter]]

  /**
   * Sets the designated parameter to SQL NULL.
   * 
   * @param index
   *   the first parameter is 1, the second is 2, ...
   */
  def setNull(index: Int): F[Unit] =
    params.update(_ + (index -> Parameter.none))

  /**
   * Sets the designated parameter to the given Scala boolean value. The driver converts this to an SQL BIT or BOOLEAN
   * value when it sends it to the database.
   * 
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBoolean(index: Int, value: Boolean): F[Unit] =
    params.update(_ + (index -> Parameter.boolean(value)))

  /**
   * Sets the designated parameter to the given Scala boolean value. The driver converts this to an SQL BIT or BOOLEAN
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBoolean(index: Int, value: Option[Boolean]): F[Unit] =
    value match
      case Some(value) => setBoolean(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala byte value. The driver converts this to an SQL TINYINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setByte(index: Int, value: Byte): F[Unit] =
    params.update(_ + (index -> Parameter.byte(value)))

  /**
   * Sets the designated parameter to the given Scala byte value. The driver converts this to an SQL TINYINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setByte(index: Int, value: Option[Byte]): F[Unit] =
    value match
      case Some(value) => setByte(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala short value. The driver converts this to an SQL SMALLINT value
   * when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setShort(index: Int, value: Short): F[Unit] =
    params.update(_ + (index -> Parameter.short(value)))

  /**
   * Sets the designated parameter to the given Scala short value. The driver converts this to an SQL SMALLINT value
   * when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setShort(index: Int, value: Option[Short]): F[Unit] =
    value match
      case Some(value) => setShort(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala int value. The driver converts this to an SQL INTEGER value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setInt(index: Int, value: Int): F[Unit] =
    params.update(_ + (index -> Parameter.int(value)))

  /**
   * Sets the designated parameter to the given Scala int value. The driver converts this to an SQL INTEGER value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setInt(index: Int, value: Option[Int]): F[Unit] =
    value match
      case Some(value) => setInt(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setLong(index: Int, value: Long): F[Unit] =
    params.update(_ + (index -> Parameter.long(value)))

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setLong(index: Int, value: Option[Long]): F[Unit] =
    value match
      case Some(value) => setLong(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigInt(index: Int, value: BigInt): F[Unit] =
    params.update(_ + (index -> Parameter.bigInt(value)))

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigInt(index: Int, value: Option[BigInt]): F[Unit] =
    value match
      case Some(value) => setBigInt(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala float value. The driver converts this to an SQL REAL value when it
   * ends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setFloat(index: Int, value: Float): F[Unit] =
    params.update(_ + (index -> Parameter.float(value)))

  /**
   * Sets the designated parameter to the given Scala float value. The driver converts this to an SQL REAL value when it
   * ends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setFloat(index: Int, value: Option[Float]): F[Unit] =
    value match
      case Some(value) => setFloat(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala double value. The driver converts this to an SQL DOUBLE value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDouble(index: Int, value: Double): F[Unit] =
    params.update(_ + (index -> Parameter.double(value)))

  /**
   * Sets the designated parameter to the given Scala double value. The driver converts this to an SQL DOUBLE value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDouble(index: Int, value: Option[Double]): F[Unit] =
    value match
      case Some(value) => setDouble(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala.math.BigDecimal value. The driver converts this to an SQL NUMERIC
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigDecimal(index: Int, value: BigDecimal): F[Unit] =
    params.update(_ + (index -> Parameter.bigDecimal(value)))

  /**
   * Sets the designated parameter to the given Scala.math.BigDecimal value. The driver converts this to an SQL NUMERIC
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigDecimal(index: Int, value: Option[BigDecimal]): F[Unit] =
    value match
      case Some(value) => setBigDecimal(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala String value. The driver converts this to an SQL VARCHAR or
   * LONGVARCHAR value (depending on the argument's size relative to the driver's limits on VARCHAR values) when it
   * sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setString(index: Int, value: String): F[Unit] =
    params.update(_ + (index -> Parameter.string(value)))

  /**
   * Sets the designated parameter to the given Scala String value. The driver converts this to an SQL VARCHAR or
   * LONGVARCHAR value (depending on the argument's size relative to the driver's limits on VARCHAR values) when it
   * sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setString(index: Int, value: Option[String]): F[Unit] =
    value match
      case Some(value) => setString(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala array of bytes. The driver converts this to an SQL VARBINARY or
   * LONGVARBINARY (depending on the argument's size relative to the driver's limits on VARBINARY values) when it sends
   * it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBytes(index: Int, value: Array[Byte]): F[Unit] =
    params.update(_ + (index -> Parameter.bytes(value)))

  /**
   * Sets the designated parameter to the given Scala array of bytes. The driver converts this to an SQL VARBINARY or
   * LONGVARBINARY (depending on the argument's size relative to the driver's limits on VARBINARY values) when it sends
   * it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBytes(index: Int, value: Option[Array[Byte]]): F[Unit] =
    value match
      case Some(value) => setBytes(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given java.time.Time value. The driver converts this to an SQL TIME value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTime(index: Int, value: LocalTime): F[Unit] =
    params.update(_ + (index -> Parameter.time(value)))

  /**
   * Sets the designated parameter to the given java.time.Time value. The driver converts this to an SQL TIME value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTime(index: Int, value: Option[LocalTime]): F[Unit] =
    value match
      case Some(value) => setTime(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given java.time.Date value, using the given Calendar object. The driver uses
   * the Calendar object to construct an SQL DATE value, which the driver then sends to the database. With a Calendar
   * object, the driver can calculate the date taking into account a custom timezone. If no Calendar object is
   * specified, the driver uses the default timezone, which is that of the virtual machine running the application.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDate(index: Int, value: LocalDate): F[Unit] =
    params.update(_ + (index -> Parameter.date(value)))

  /**
   * Sets the designated parameter to the given java.time.Date value, using the given Calendar object. The driver uses
   * the Calendar object to construct an SQL DATE value, which the driver then sends to the database. With a Calendar
   * object, the driver can calculate the date taking into account a custom timezone. If no Calendar object is
   * specified, the driver uses the default timezone, which is that of the virtual machine running the application.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDate(index: Int, value: Option[LocalDate]): F[Unit] =
    value match
      case Some(value) => setDate(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given java.time.Timestamp value. The driver converts this to an SQL TIMESTAMP
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTimestamp(index: Int, value: LocalDateTime): F[Unit] =
    params.update(_ + (index -> Parameter.datetime(value)))

  /**
   * Sets the designated parameter to the given java.time.Timestamp value. The driver converts this to an SQL TIMESTAMP
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTimestamp(index: Int, value: Option[LocalDateTime]): F[Unit] =
    value match
      case Some(value) => setTimestamp(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given java.time.Year value. The driver converts this to an SQL YEAR
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setYear(index: Int, value: Year): F[Unit] =
    params.update(_ + (index -> Parameter.year(value)))

  /**
   * Sets the designated parameter to the given java.time.Year value. The driver converts this to an SQL YEAR
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setYear(index: Int, value: Option[Year]): F[Unit] =
    value match
      case Some(value) => setYear(index, value)
      case None        => setNull(index)

  /**
   * Executes the specified SQL statement and returns one or more ResultSet objects.
   */
  def executeQuery(): F[ResultSet[F]]

  /**
   * Executes the given SQL statement, which may be an INSERT, UPDATE, or DELETE statement or an SQL statement that
   * returns nothing, such as an SQL DDL statement.
   */
  def executeUpdate(): F[Int]

  /**
   * Executes the SQL statement in this <code>PreparedStatement</code> object,
   * which may be any kind of SQL statement.
   * Some prepared statements return multiple results; the <code>execute</code>
   * method handles these complex statements as well as the simpler
   * form of statements handled by the methods <code>executeQuery</code>
   * and <code>executeUpdate</code>.
   * <P>
   * The <code>execute</code> method returns a <code>boolean</code> to
   * indicate the form of the first result.  You must call either the method
   * <code>getResultSet</code> or <code>getUpdateCount</code>
   * to retrieve the result; you must call <code>getMoreResults</code> to
   * move to any subsequent result(s).
   *
   * @return <code>true</code> if the first result is a <code>ResultSet</code>
   *         object; <code>false</code> if the first result is an update
   *         count or there is no result
   */
  def execute(): F[Boolean]

  /**
   * Adds a set of parameters to this PreparedStatement object's batch of commands.
   */
  def addBatch(): F[Unit]

object PreparedStatement:

  private def buildQuery(original: String, params: ListMap[Int, Parameter]): String =
    val query = original.toCharArray
    params
      .foldLeft(query) {
        case (query, (offset, param)) =>
          val index = query.indexOf('?', offset - 1)
          if index < 0 then query
          else
            val (head, tail)         = query.splitAt(index)
            val (tailHead, tailTail) = tail.splitAt(1)
            head ++ param.sql ++ tailTail
      }
      .mkString

  private def buildBatchQuery(original: String, params: ListMap[Int, Parameter]): String =
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
    extends PreparedStatement[F],
            StatementImpl.ShareStatement[F]:

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
    extends PreparedStatement[F],
            StatementImpl.ShareStatement[F]:

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
