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

import ldbc.sql.ResultSet

import ldbc.connector.*
import ldbc.connector.data.*
import ldbc.connector.sql.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.Protocol
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

/**
 * The interface used to execute SQL stored procedures.  The JDBC API
 * provides a stored procedure SQL escape syntax that allows stored procedures
 * to be called in a standard way for all RDBMSs. This escape syntax has one
 * form that includes a result parameter and one that does not. If used, the result
 * parameter must be registered as an OUT parameter. The other parameters
 * can be used for input, output or both. Parameters are referred to
 * sequentially, by number, with the first parameter being 1.
 * <PRE>
 *   {?= call &lt;procedure-name&gt;[(&lt;arg1&gt;,&lt;arg2&gt;, ...)]}
 *   {call &lt;procedure-name&gt;[(&lt;arg1&gt;,&lt;arg2&gt;, ...)]}
 * </PRE>
 * <P>
 * IN parameter values are set using the <code>set</code> methods inherited from
 * {@link PreparedStatement}.  The type of all OUT parameters must be
 * registered prior to executing the stored procedure; their values
 * are retrieved after execution via the <code>get</code> methods provided here.
 * <P>
 * A <code>CallableStatement</code> can return one {@link ResultSet} object or
 * multiple <code>ResultSet</code> objects.  Multiple
 * <code>ResultSet</code> objects are handled using operations
 * inherited from {@link Statement}.
 * <P>
 * For maximum portability, a call's <code>ResultSet</code> objects and
 * update counts should be processed prior to getting the values of output
 * parameters.
 *
 * @tparam F
 *   the effect type
 */
trait CallableStatement[F[_]] extends PreparedStatement[F]:

  private[ldbc] def setParameter(index: Int, value: String): F[Unit] =
    params.update(_ + (index -> Parameter.parameter(value)))

  /**
   * Registers the OUT parameter in ordinal position
   * <code>parameterIndex</code> to the JDBC type
   * <code>sqlType</code>.  All OUT parameters must be registered
   * before a stored procedure is executed.
   * <p>
   * The JDBC type specified by <code>sqlType</code> for an OUT
   * parameter determines the Scala type that must be used
   * in the <code>get</code> method to read the value of that parameter.
   * <p>
   * If the JDBC type expected to be returned to this output parameter
   * is specific to this particular database, <code>sqlType</code>
   * should be <code>java.sql.Types.OTHER</code>.  The method
   * {@link #getObject} retrieves the value.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @param sqlType the JDBC type code defined by <code>java.sql.Types</code>.
   *        If the parameter is of JDBC type <code>NUMERIC</code>
   *        or <code>DECIMAL</code>, the version of
   *        <code>registerOutParameter</code> that accepts a scale value
   *        should be used.
   */
  def registerOutParameter(parameterIndex: Int, sqlType: Int): F[Unit]

  /**
   * Retrieves the value of the designated JDBC <code>CHAR</code>,
   * <code>VARCHAR</code>, or <code>LONGVARCHAR</code> parameter as a
   * <code>String</code> in the Sava programming language
   * <p>
   * For the fixed-length type JDBC <code>CHAR</code>,
   * the <code>String</code> object
   * returned has exactly the same value the SQL
   * <code>CHAR</code> value had in the
   * database, including any padding added by the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result
   *         is <code>None</code>.
   */
  def getString(parameterIndex: Int): F[Option[String]]

  /**
   * Retrieves the value of the designated JDBC <code>BIT</code>
   * or <code>BOOLEAN</code> parameter as a
   * <code>Boolean</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>false</code>.
   */
  def getBoolean(parameterIndex: Int): F[Boolean]

  /**
   * Retrieves the value of the designated JDBC <code>TINYINT</code> parameter
   * as a <code>byte</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getByte(parameterIndex: Int): F[Byte]

  /**
   * Retrieves the value of the designated JDBC <code>SMALLINT</code> parameter
   * as a <code>short</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getShort(parameterIndex: Int): F[Short]

  /**
   * Retrieves the value of the designated JDBC <code>INTEGER</code> parameter
   * as an <code>int</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getInt(parameterIndex: Int): F[Int]

  /**
   * Retrieves the value of the designated JDBC <code>BIGINT</code> parameter
   * as a <code>long</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getLong(parameterIndex: Int): F[Long]

  /**
   * Retrieves the value of the designated JDBC <code>FLOAT</code> parameter
   * as a <code>float</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>0</code>.
   */
  def getFloat(parameterIndex: Int): F[Float]

  /**
   * Retrieves the value of the designated JDBC <code>DOUBLE</code> parameter as a <code>double</code>
   * in the Sava programming language
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>0</code>.
   */
  def getDouble(parameterIndex: Int): F[Double]

  /**
   * Retrieves the value of the designated JDBC <code>BINARY</code> or
   * <code>VARBINARY</code> parameter as an array of <code>byte</code>
   * values in the Sava programming language
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>None</code>.
   */
  def getBytes(parameterIndex: Int): F[Option[Array[Byte]]]

  /**
   * Retrieves the value of the designated JDBC <code>DATE</code> parameter as a
   * <code>java.time.LocalDate</code> object.
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>None</code>.
   */
  def getDate(parameterIndex: Int): F[Option[LocalDate]]

  /**
   * Retrieves the value of the designated JDBC <code>TIME</code> parameter as a
   * <code>java.time.LocalTime</code> object.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>null</code>.
   */
  def getTime(parameterIndex: Int): F[Option[LocalTime]]

  /**
   * Retrieves the value of the designated JDBC <code>TIMESTAMP</code> parameter as a
   * <code>java.time.LocalDateTime</code> object.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>None</code>.
   */
  def getTimestamp(parameterIndex: Int): F[Option[LocalDateTime]]

  /**
   * Retrieves the value of the designated JDBC <code>NUMERIC</code> parameter as a
   * <code>java.math.BigDecimal</code> object with as many digits to the
   * right of the decimal point as the value contains.
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value in full precision.  If the value is
   * SQL <code>NULL</code>, the result is <code>None</code>.
   */
  def getBigDecimal(parameterIndex: Int): F[Option[BigDecimal]]

  /**
   * Retrieves the value of a JDBC <code>CHAR</code>, <code>VARCHAR</code>,
   * or <code>LONGVARCHAR</code> parameter as a <code>String</code> in
   * the Sava programming language
   * <p>
   * For the fixed-length type JDBC <code>CHAR</code>,
   * the <code>String</code> object
   * returned has exactly the same value the SQL
   * <code>CHAR</code> value had in the
   * database, including any padding added by the database.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>None</code>.
   */
  def getString(parameterName: String): F[Option[String]]

  /**
   * Retrieves the value of a JDBC <code>BIT</code> or <code>BOOLEAN</code>
   * parameter as a
   * <code>Boolean</code> in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>false</code>.
   */
  def getBoolean(parameterName: String): F[Boolean]

  /**
   * Retrieves the value of a JDBC <code>TINYINT</code> parameter as a <code>byte</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getByte(parameterName: String): F[Byte]

  /**
   * Retrieves the value of a JDBC <code>SMALLINT</code> parameter as a <code>short</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getShort(parameterName: String): F[Short]

  /**
   * Retrieves the value of a JDBC <code>INTEGER</code> parameter as an <code>int</code>
   * in the Sava programming language
   *
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getInt(parameterName: String): F[Int]

  /**
   * Retrieves the value of a JDBC <code>BIGINT</code> parameter as a <code>long</code>
   * in the Sava programming language
   *
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getLong(parameterName: String): F[Long]

  /**
   * Retrieves the value of a JDBC <code>FLOAT</code> parameter as a <code>float</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getFloat(parameterName: String): F[Float]

  /**
   * Retrieves the value of a JDBC <code>DOUBLE</code> parameter as a <code>double</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getDouble(parameterName: String): F[Double]

  /**
   * Retrieves the value of a JDBC <code>BINARY</code> or <code>VARBINARY</code>
   * parameter as an array of <code>byte</code> values in the Scala
   * programming language.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result is
   *  <code>None</code>.
   */
  def getBytes(parameterName: String): F[Option[Array[Byte]]]

  /**
   * Retrieves the value of a JDBC <code>DATE</code> parameter as a
   * <code>java.sql.Date</code> object.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>None</code>.
   */
  def getDate(parameterName: String): F[Option[LocalDate]]

  /**
   * Retrieves the value of a JDBC <code>TIME</code> parameter as a
   * <code>java.sql.Time</code> object.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>null</code>.
   */
  def getTime(parameterName: String): F[Option[LocalTime]]

  /**
   * Retrieves the value of a JDBC <code>TIMESTAMP</code> parameter as a
   * <code>java.sql.Timestamp</code> object.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>None</code>.
   */
  def getTimestamp(parameterName: String): F[Option[LocalDateTime]]

  /**
   * Retrieves the value of a JDBC <code>NUMERIC</code> parameter as a
   * <code>java.math.BigDecimal</code> object with as many digits to the
   * right of the decimal point as the value contains.
   * @param parameterName the name of the parameter
   * @return the parameter value in full precision. If the value is
   * SQL <code>NULL</code>, the result is <code>None</code>.
   */
  def getBigDecimal(parameterName: String): F[Option[BigDecimal]]

object CallableStatement:

  val NOT_OUTPUT_PARAMETER_INDICATOR: Int = Int.MinValue

  private val PARAMETER_NAMESPACE_PREFIX = "@ldbc_mysql_outparam_"

  /**
   * CallableStatementParameter represents a parameter in a stored procedure.
   *
   * @param paramName
   *   the name of the parameter
   * @param isIn
   *   whether the parameter is an input parameter
   * @param isOut
   *   whether the parameter is an output parameter
   * @param index
   *   the index of the parameter
   * @param jdbcType
   *   the JDBC type of the parameter
   * @param typeName
   *   the name of the type of the parameter
   * @param precision
   *   the precision of the parameter
   * @param scale
   *   the scale of the parameter
   * @param nullability
   *   the nullability of the parameter
   * @param inOutModifier
   *   the in/out modifier of the parameter
   */
  case class CallableStatementParameter(
    paramName:     Option[String],
    isIn:          Boolean,
    isOut:         Boolean,
    index:         Int,
    jdbcType:      Int,
    typeName:      Option[String],
    precision:     Int,
    scale:         Int,
    nullability:   Short,
    inOutModifier: Int
  )

  /**
   * ParamInfo represents the information about the parameters in a stored procedure.
   *
   * @param nativeSql
   *   the original SQL statement
   * @param dbInUse
   *   the database in use
   * @param isFunctionCall
   *   whether the SQL statement is a function call
   * @param numParameters
   *   the number of parameters in the SQL statement
   * @param parameterList
   *   a list of CallableStatementParameter representing each parameter
   * @param parameterMap
   *   a map from parameter name to CallableStatementParameter
   */
  case class ParamInfo(
    nativeSql:      String,
    dbInUse:        Option[String],
    isFunctionCall: Boolean,
    numParameters:  Int,
    parameterList:  List[CallableStatementParameter],
    parameterMap:   ListMap[String, CallableStatementParameter]
  )

  object ParamInfo:

    def apply[F[_]: Temporal](
      nativeSql:      String,
      database:       Option[String],
      resultSet:      ResultSetImpl[F],
      isFunctionCall: Boolean
    ): F[ParamInfo] =
      val parameterListF = Monad[F].whileM[List, CallableStatementParameter](resultSet.next()) {
        for
          index           <- resultSet.getRow()
          paramName       <- resultSet.getString(4)
          procedureColumn <- resultSet.getInt(5)
          jdbcType        <- resultSet.getInt(6)
          typeName        <- resultSet.getString(7)
          precision       <- resultSet.getInt(8)
          scale           <- resultSet.getInt(19)
          nullability     <- resultSet.getShort(12)
        yield
          val inOutModifier = procedureColumn match
            case DatabaseMetaData.procedureColumnIn    => ParameterMetaData.parameterModeIn
            case DatabaseMetaData.procedureColumnInOut => ParameterMetaData.parameterModeInOut
            case DatabaseMetaData.procedureColumnOut | DatabaseMetaData.procedureColumnReturn =>
              ParameterMetaData.parameterModeOut
            case _ => ParameterMetaData.parameterModeUnknown

          val (isOutParameter, isInParameter) =
            if index - 1 == 0 && isFunctionCall then (true, false)
            else if inOutModifier == DatabaseMetaData.procedureColumnInOut then (true, true)
            else if inOutModifier == DatabaseMetaData.procedureColumnIn then (false, true)
            else if inOutModifier == DatabaseMetaData.procedureColumnOut then (true, false)
            else (false, false)
          CallableStatementParameter(
            Option(paramName),
            isInParameter,
            isOutParameter,
            index,
            jdbcType,
            Option(typeName),
            precision,
            scale,
            nullability,
            inOutModifier
          )
      }

      for
        numParameters <- resultSet.rowLength()
        parameterList <- parameterListF
      yield ParamInfo(
        nativeSql      = nativeSql,
        dbInUse        = database,
        isFunctionCall = isFunctionCall,
        numParameters  = numParameters,
        parameterList  = parameterList,
        parameterMap   = ListMap(parameterList.map(p => p.paramName.getOrElse("") -> p)*)
      )

  private[ldbc] case class Impl[F[_]: Temporal: Exchange: Tracer](
    protocol:                Protocol[F],
    serverVariables:         Map[String, String],
    sql:                     String,
    paramInfo:               ParamInfo,
    params:                  Ref[F, ListMap[Int, Parameter]],
    batchedArgs:             Ref[F, Vector[String]],
    connectionClosed:        Ref[F, Boolean],
    statementClosed:         Ref[F, Boolean],
    resultSetClosed:         Ref[F, Boolean],
    currentResultSet:        Ref[F, Option[ResultSet[F]]],
    outputParameterResult:   Ref[F, Option[ResultSetImpl[F]]],
    resultSets:              Ref[F, List[ResultSetImpl[F]]],
    parameterIndexToRsIndex: Ref[F, Map[Int, Int]],
    updateCount:             Ref[F, Int],
    moreResults:             Ref[F, Boolean],
    autoGeneratedKeys:       Ref[F, Statement.NO_GENERATED_KEYS | Statement.RETURN_GENERATED_KEYS],
    lastInsertId:            Ref[F, Int],
    resultSetType:           Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency:    Int = ResultSet.CONCUR_READ_ONLY
  )(using ev: MonadError[F, Throwable])
    extends CallableStatement[F],
            Statement.ShareStatement[F]:

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

    private val attributes = protocol.initialPacket.attributes ++ List(
      Attribute("type", "CallableStatement"),
      Attribute("sql", sql)
    )

    override def executeQuery(): F[ResultSet[F]] =
      checkClosed() *>
        checkNullOrEmptyQuery(sql) *>
        exchange[F, ResultSet[F]]("statement") { (span: Span[F]) =>
          if sql.toUpperCase.startsWith("CALL") then
            executeCallStatement(span).flatMap { resultSets =>
              resultSets.headOption match
                case None =>
                  for
                    lastColumnReadNullable <- Ref[F].of(true)
                    resultSetCurrentCursor <- Ref[F].of(0)
                    resultSetCurrentRow    <- Ref[F].of[Option[ResultSetRowPacket]](None)
                    resultSet = ResultSetImpl.empty(
                                  serverVariables,
                                  protocol.initialPacket.serverVersion,
                                  resultSetClosed,
                                  lastColumnReadNullable,
                                  resultSetCurrentCursor,
                                  resultSetCurrentRow
                                )
                    _ <- currentResultSet.set(Some(resultSet))
                  yield resultSet
                case Some(resultSet) =>
                  currentResultSet.update(_ => Some(resultSet)) *> resultSet.pure[F]
            } <* retrieveOutParams()
          else
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
                receiveQueryResult()
            }
        } <* params.set(ListMap.empty)

    override def executeUpdate(): F[Int] =
      checkClosed() *>
        checkNullOrEmptyQuery(sql) *>
        exchange[F, Int]("statement") { (span: Span[F]) =>
          if sql.toUpperCase.startsWith("CALL") then
            executeCallStatement(span).flatMap { resultSets =>
              resultSets.headOption match
                case None =>
                  for
                    lastColumnReadNullable <- Ref[F].of(true)
                    resultSetCurrentCursor <- Ref[F].of(0)
                    resultSetCurrentRow    <- Ref[F].of[Option[ResultSetRowPacket]](None)
                    resultSet = ResultSetImpl.empty(
                                  serverVariables,
                                  protocol.initialPacket.serverVersion,
                                  resultSetClosed,
                                  lastColumnReadNullable,
                                  resultSetCurrentCursor,
                                  resultSetCurrentRow
                                )
                    _ <- currentResultSet.set(Some(resultSet))
                  yield resultSet
                case Some(resultSet) =>
                  currentResultSet.update(_ => Some(resultSet)) *> resultSet.pure[F]
            } *> retrieveOutParams() *> ev.pure(-1)
          else
            params.get.flatMap { params =>
              span.addAttributes(
                (attributes ++ List(
                  Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
                  Attribute("execute", "update")
                ))*
              ) *>
                sendQuery(buildQuery(sql, params)).flatMap {
                  case result: OKPacket => lastInsertId.set(result.lastInsertId) *> ev.pure(result.affectedRows)
                  case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                  case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
                }
            }
        }

    override def execute(): F[Boolean] =
      checkClosed() *>
        checkNullOrEmptyQuery(sql) *>
        exchange[F, Boolean]("statement") { (span: Span[F]) =>
          if sql.toUpperCase.startsWith("CALL") then
            executeCallStatement(span).flatMap { results =>
              moreResults.update(_ => results.nonEmpty) *>
                currentResultSet.update(_ => results.headOption) *>
                resultSets.set(results.toList) *>
                ev.pure(results.nonEmpty)
            } <* retrieveOutParams()
          else
            params.get
              .flatMap { params =>
                span.addAttributes(
                  (attributes ++ List(
                    Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
                    Attribute("execute", "update")
                  ))*
                ) *>
                  sendQuery(buildQuery(sql, params)).flatMap {
                    case result: OKPacket => lastInsertId.set(result.lastInsertId) *> ev.pure(result.affectedRows)
                    case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                    case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
                  }
              }
              .map(_ => false)
        }

    override def getMoreResults(): F[Boolean] =
      checkClosed() *> moreResults.get.flatMap { isMoreResults =>
        if isMoreResults then
          resultSets.get.flatMap {
            case Nil => moreResults.set(false) *> ev.pure(false)
            case resultSet :: tail =>
              currentResultSet.set(Some(resultSet)) *> resultSets.set(tail) *> ev.pure(true)
          }
        else ev.pure(false)
      }

    override def addBatch(): F[Unit] =
      checkClosed() *>
        checkNullOrEmptyQuery(sql) *> (
          sql.toUpperCase match
            case q if q.startsWith("CALL") =>
              setInOutParamsOnServer(paramInfo) *> setOutParams()
            case _ => ev.unit
        ) *>
        params.get.flatMap { params =>
          batchedArgs.update(_ :+ buildBatchQuery(sql, params))
        } *>
        params.set(ListMap.empty)

    override def executeBatch(): F[List[Int]] =
      checkClosed() *>
        checkNullOrEmptyQuery(sql) *>
        exchange[F, List[Int]]("statement") { (span: Span[F]) =>
          batchedArgs.get.flatMap { args =>
            span.addAttributes(
              (attributes ++ List(
                Attribute("execute", "batch"),
                Attribute("size", args.length.toLong),
                Attribute("sql", args.toArray.toSeq)
              ))*
            ) *> (
              if args.isEmpty then ev.pure(List.empty)
              else
                sql.toUpperCase match
                  case q if q.startsWith("INSERT") =>
                    sendQuery(sql.split("VALUES").head + " VALUES" + args.mkString(","))
                      .flatMap {
                        case _: OKPacket      => ev.pure(List.fill(args.length)(Statement.SUCCESS_NO_INFO))
                        case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                        case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
                      }
                  case q if q.startsWith("update") || q.startsWith("delete") || q.startsWith("CALL") =>
                    protocol.resetSequenceId *>
                      protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON) *>
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
                        .map(_.toList) <*
                      protocol.resetSequenceId <*
                      protocol.comSetOption(EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF)
                  case _ =>
                    ev.raiseError(
                      new SQLException("The batch query must be an INSERT, UPDATE, or DELETE, CALL statement.")
                    )
            )
          }
        } <* params.set(ListMap.empty) <* batchedArgs.set(Vector.empty)

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

    override def registerOutParameter(parameterIndex: Int, sqlType: Int): F[Unit] =
      if paramInfo.numParameters > 0 then
        paramInfo.parameterList.find(_.index == parameterIndex) match
          case Some(param) =>
            (if param.jdbcType == sqlType then ev.unit
             else
               ev.raiseError(
                 new SQLException(
                   "The type specified for the parameter does not match the type registered as a procedure."
                 )
               )
            ) *> (
              if param.isOut && param.isIn then
                val paramName          = param.paramName.getOrElse("nullnp" + param.index)
                val inOutParameterName = mangleParameterName(paramName)

                val queryBuf = new StringBuilder(4 + inOutParameterName.length + 1)
                queryBuf.append("SET ")
                queryBuf.append(inOutParameterName)
                queryBuf.append("=")

                params.get.flatMap { params =>
                  val sql =
                    (queryBuf.toString.toCharArray ++ params.get(param.index).fold("NULL".toCharArray)(_.sql)).mkString
                  sendQuery(sql).flatMap {
                    case _: OKPacket      => ev.unit
                    case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                    case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
                  }
                }
              else ev.raiseError(new SQLException("No output parameters returned by procedure."))
            )
          case None =>
            ev.raiseError(
              new SQLException(s"Parameter index of $parameterIndex is out of range (1, ${ paramInfo.numParameters })")
            )
      else ev.unit

    override def getString(parameterIndex: Int): F[Option[String]] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getString(index))
      yield Option(value)

    override def getBoolean(parameterIndex: Int): F[Boolean] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getBoolean(index))
      yield value

    override def getByte(parameterIndex: Int): F[Byte] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getByte(index))
      yield value

    override def getShort(parameterIndex: Int): F[Short] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getShort(index))
      yield value

    override def getInt(parameterIndex: Int): F[Int] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getInt(index))
      yield value

    override def getLong(parameterIndex: Int): F[Long] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getLong(index))
      yield value

    override def getFloat(parameterIndex: Int): F[Float] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getFloat(index))
      yield value

    override def getDouble(parameterIndex: Int): F[Double] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getDouble(index))
      yield value

    override def getBytes(parameterIndex: Int): F[Option[Array[Byte]]] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getBytes(index))
      yield Option(value)

    override def getDate(parameterIndex: Int): F[Option[LocalDate]] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getDate(index))
      yield Option(value)

    override def getTime(parameterIndex: Int): F[Option[LocalTime]] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getTime(index))
      yield Option(value)

    override def getTimestamp(parameterIndex: Int): F[Option[LocalDateTime]] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getTimestamp(index))
      yield Option(value)

    override def getBigDecimal(parameterIndex: Int): F[Option[BigDecimal]] =
      for
        resultSet <- checkBounds(parameterIndex) *> getOutputParameters()
        paramMap  <- parameterIndexToRsIndex.get
        index = paramMap.getOrElse(parameterIndex, parameterIndex)
        value <-
          (if index == NOT_OUTPUT_PARAMETER_INDICATOR then
             ev.raiseError(new SQLException(s"Parameter $parameterIndex is not registered as an output parameter"))
           else resultSet.getBigDecimal(index))
      yield Option(value)

    override def getString(parameterName: String): F[Option[String]] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getString(mangleParameterName(parameterName))
      yield Option(value)

    override def getBoolean(parameterName: String): F[Boolean] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getBoolean(mangleParameterName(parameterName))
      yield value

    override def getByte(parameterName: String): F[Byte] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getByte(mangleParameterName(parameterName))
      yield value

    override def getShort(parameterName: String): F[Short] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getShort(mangleParameterName(parameterName))
      yield value

    override def getInt(parameterName: String): F[Int] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getInt(mangleParameterName(parameterName))
      yield value

    override def getLong(parameterName: String): F[Long] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getLong(mangleParameterName(parameterName))
      yield value

    override def getFloat(parameterName: String): F[Float] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getFloat(mangleParameterName(parameterName))
      yield value

    override def getDouble(parameterName: String): F[Double] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getDouble(mangleParameterName(parameterName))
      yield value

    override def getBytes(parameterName: String): F[Option[Array[Byte]]] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getBytes(mangleParameterName(parameterName))
      yield Option(value)

    override def getDate(parameterName: String): F[Option[LocalDate]] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getDate(mangleParameterName(parameterName))
      yield Option(value)

    override def getTime(parameterName: String): F[Option[LocalTime]] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getTime(mangleParameterName(parameterName))
      yield Option(value)

    override def getTimestamp(parameterName: String): F[Option[LocalDateTime]] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getTimestamp(mangleParameterName(parameterName))
      yield Option(value)

    override def getBigDecimal(parameterName: String): F[Option[BigDecimal]] =
      for
        resultSet <- getOutputParameters()
        value     <- resultSet.getBigDecimal(mangleParameterName(parameterName))
      yield Option(value)

    private def sendQuery(sql: String): F[GenericResponsePackets] =
      checkNullOrEmptyQuery(sql) *> protocol.resetSequenceId *> protocol.send(
        ComQueryPacket(sql, protocol.initialPacket.capabilityFlags, ListMap.empty)
      ) *> protocol.receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags))

    private def receiveUntilOkPacket(resultSets: Vector[ResultSetImpl[F]]): F[Vector[ResultSetImpl[F]]] =
      protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
        case _: OKPacket      => resultSets.pure[F]
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
            resultSets <- receiveUntilOkPacket(resultSets :+ resultSet)
          yield resultSets
      }

    private def receiveQueryResult(): F[ResultSet[F]] =
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

    /**
     * Change the parameter name to an arbitrary prefixed naming.
     *
     * @param origParameterName
     *   the original parameter name
     * @return
     *   the parameter name
     */
    private def mangleParameterName(origParameterName: String): String =
      val offset = if origParameterName.nonEmpty && origParameterName.charAt(0) == '@' then 1 else 0

      val paramNameBuf = new StringBuilder(PARAMETER_NAMESPACE_PREFIX.length + origParameterName.length)
      paramNameBuf.append(PARAMETER_NAMESPACE_PREFIX)
      paramNameBuf.append(origParameterName.substring(offset))

      paramNameBuf.toString

    /**
     * Set output parameters to be used by the server.
     *
     * @param paramInfo
     *   the parameter information
     */
    private def setInOutParamsOnServer(paramInfo: ParamInfo): F[Unit] =
      if paramInfo.numParameters > 0 then
        paramInfo.parameterList.foldLeft(ev.unit) { (acc, param) =>
          if param.isOut && param.isIn then
            val paramName          = param.paramName.getOrElse("nullnp" + param.index)
            val inOutParameterName = mangleParameterName(paramName)

            val queryBuf = new StringBuilder(4 + inOutParameterName.length + 1)
            queryBuf.append("SET ")
            queryBuf.append(inOutParameterName)
            queryBuf.append("=")

            acc *> params.get.flatMap { params =>
              val sql =
                (queryBuf.toString.toCharArray ++ params.get(param.index).fold("NULL".toCharArray)(_.sql)).mkString
              sendQuery(sql).flatMap {
                case _: OKPacket      => ev.unit
                case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
              }
            }
          else acc
        }
      else ev.unit

    /**
     * Set output parameters to be handled by the client.
     */
    private def setOutParams(): F[Unit] =
      if paramInfo.numParameters > 0 then
        paramInfo.parameterList.foldLeft(ev.unit) { (acc, param) =>
          if !paramInfo.isFunctionCall && param.isOut then
            val paramName        = param.paramName.getOrElse("nullnp" + param.index)
            val outParameterName = mangleParameterName(paramName)

            acc *> params.get.flatMap { params =>
              for
                outParamIndex <- (
                                   if params.isEmpty then ev.pure(param.index)
                                   else
                                     params.keys
                                       .find(_ == param.index)
                                       .fold(
                                         ev.raiseError(
                                           new SQLException(
                                             s"Parameter ${ param.index } is not registered as an output parameter"
                                           )
                                         )
                                       )(_.pure[F])
                                 )
                _ <- setParameter(outParamIndex, outParameterName)
              yield ()
            }
          else acc
        }
      else ev.unit

    /**
     * Issues a second query to retrieve all output parameters.
     */
    private def retrieveOutParams(): F[Unit] =
      val parameters = paramInfo.parameterList.foldLeft(Vector.empty[(Int, String)]) { (acc, param) =>
        if param.isOut then
          val paramName        = param.paramName.getOrElse("nullnp" + param.index)
          val outParameterName = mangleParameterName(paramName)
          acc :+ (param.index, outParameterName)
        else acc
      }

      if paramInfo.numParameters > 0 && parameters.nonEmpty then

        val sql = parameters.zipWithIndex
          .map {
            case ((_, paramName), index) =>
              val prefix = if index != 0 then ", " else ""
              val atSign = if !paramName.startsWith("@") then "@" else ""
              s"$prefix$atSign$paramName"
          }
          .mkString("SELECT ", "", "")

        checkClosed() *>
          checkNullOrEmptyQuery(sql) *>
          protocol.resetSequenceId *>
          protocol.send(ComQueryPacket(sql, protocol.initialPacket.capabilityFlags, ListMap.empty)) *>
          receiveQueryResult().flatMap {
            case resultSet: ResultSetImpl[F] => outputParameterResult.update(_ => Some(resultSet))
          } *>
          parameters.zipWithIndex.foldLeft(ev.unit) {
            case (acc, ((paramIndex, _), index)) =>
              acc *> parameterIndexToRsIndex.update(_ + (paramIndex -> (index + 1)))
          }
      else ev.unit

    /**
     * Returns the ResultSet that holds the output parameters, or throws an
     * appropriate exception if none exist, or they weren't returned.
     *
     * @return
     *   the ResultSet that holds the output parameters
     */
    private def getOutputParameters(): F[ResultSetImpl[F]] =
      outputParameterResult.get.flatMap {
        case None =>
          if paramInfo.numParameters == 0 then ev.raiseError(new SQLException("No output parameters registered."))
          else ev.raiseError(new SQLException("No output parameters returned by procedure."))
        case Some(resultSet) => resultSet.pure[F]
      }

    /**
     * Checks if the parameter index is within the bounds of the number of parameters.
     *
     * @param paramIndex
     *   the parameter index to check
     */
    private def checkBounds(paramIndex: Int): F[Unit] =
      if paramIndex < 1 || paramIndex > paramInfo.numParameters then
        ev.raiseError(
          new SQLException(s"Parameter index of ${ paramIndex } is out of range (1, ${ paramInfo.numParameters })")
        )
      else ev.unit

    /**
     * Executes a CALL/Stored function statement.
     *
     * @param span
     *   the span
     * @return
     *   a list of ResultSet
     */
    private def executeCallStatement(span: Span[F]): F[Vector[ResultSetImpl[F]]] =
      setInOutParamsOnServer(paramInfo) *>
        setOutParams() *>
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
            receiveUntilOkPacket(Vector.empty)
        }
