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
   * parameter determines the Java type that must be used
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

object CallableStatement:

  private val PARAMETER_NAMESPACE_PREFIX = "@com_mysql_ldbc_outparam_"

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
      resultSet:      ResultSet[F],
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
            paramName,
            isInParameter,
            isOutParameter,
            index,
            jdbcType,
            typeName,
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
    protocol:             Protocol[F],
    serverVariables:      Map[String, String],
    sql:                  String,
    paramInfo:            ParamInfo,
    params:               Ref[F, ListMap[Int, Parameter]],
    batchedArgs:          Ref[F, Vector[String]],
    connectionClosed:     Ref[F, Boolean],
    statementClosed:      Ref[F, Boolean],
    resultSetClosed:      Ref[F, Boolean],
    currentResultSet:     Ref[F, Option[ResultSet[F]]],
    updateCount:          Ref[F, Int],
    moreResults:          Ref[F, Boolean],
    autoGeneratedKeys:    Ref[F, Statement.NO_GENERATED_KEYS | Statement.RETURN_GENERATED_KEYS],
    lastInsertId:         Ref[F, Int],
    resultSetType:        Int = ResultSet.TYPE_FORWARD_ONLY,
    resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY
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

    private val attributes = protocol.initialPacket.attributes ++ List(
      Attribute("type", "CallableStatement"),
      Attribute("sql", sql)
    )

    override def executeQuery(): F[ResultSet[F]] =
      checkClosed() *>
        checkNullOrEmptyQuery(sql) *>
        exchange[F, ResultSet[F]]("statement") { (span: Span[F]) =>
          setInOutParamsOnServer(paramInfo) *>
            setOutParams(paramInfo) *>
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
            } <* params.set(ListMap.empty)
        }

    override def executeUpdate(): F[Int]       = ???
    override def execute():       F[Boolean]   = ???
    override def addBatch():      F[Unit]      = ???
    override def executeBatch():  F[List[Int]] = ???
    override def close():         F[Unit]      = ???

    override def registerOutParameter(parameterIndex: Int, sqlType: Int): F[Unit] = ???

    private def sendQuery(sql: String): F[GenericResponsePackets] =
      checkNullOrEmptyQuery(sql) *> protocol.resetSequenceId *> protocol.send(
        ComQueryPacket(sql, protocol.initialPacket.capabilityFlags, ListMap.empty)
      ) *> protocol.receive(GenericResponsePackets.decoder(protocol.initialPacket.capabilityFlags))

    private def receiveQueryResult(): F[ResultSet[F]] =
      protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
        case _: OKPacket =>
          for
            resultSetCurrentCursor <- Ref[F].of(0)
            resultSetCurrentRow    <- Ref[F].of[Option[ResultSetRowPacket]](None)
          yield ResultSet
            .empty(
              serverVariables,
              protocol.initialPacket.serverVersion,
              resultSetClosed,
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
            resultSetCurrentCursor <- Ref[F].of(0)
            resultSetCurrentRow    <- Ref[F].of(resultSetRow.headOption)
            resultSet = ResultSet(
                          columnDefinitions,
                          resultSetRow,
                          serverVariables,
                          protocol.initialPacket.serverVersion,
                          resultSetClosed,
                          resultSetCurrentCursor,
                          resultSetCurrentRow,
                          resultSetType,
                          resultSetConcurrency
                        )
            _ <- currentResultSet.set(Some(resultSet))
          yield resultSet
      }

    private def mangleParameterName(origParameterName: String): String =
      val offset = if origParameterName.nonEmpty && origParameterName.charAt(0) == '@' then 1 else 0

      val paramNameBuf = new StringBuilder(PARAMETER_NAMESPACE_PREFIX.length + origParameterName.length)
      paramNameBuf.append(PARAMETER_NAMESPACE_PREFIX)
      paramNameBuf.append(origParameterName.substring(offset))

      paramNameBuf.toString

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
              params.get(param.index) match
                case Some(parameter) =>
                  val sql = (queryBuf.toString.toCharArray ++ parameter.sql).mkString
                  sendQuery(sql).flatMap {
                    case _: OKPacket      => ev.unit
                    case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                    case _: EOFPacket     => ev.raiseError(new SQLException("Unexpected EOF packet"))
                  }
                case None => ev.raiseError(new SQLException("Parameter not found"))
            }
          else acc
        }
      else ev.unit

    private def setOutParams(paramInfo: ParamInfo): F[Unit] =
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
