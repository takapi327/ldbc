/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.MonadError
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.DatabaseMetaData.*
import ldbc.connector.net.Protocol
import ldbc.connector.net.protocol.*

/**
 * DatabaseMetaData implementation that uses INFORMATION_SCHEMA
 * @param protocol
 * @param serverVariables
 * @param databaseTerm
 * @param getProceduresReturnsFunctions
 * @tparam F
 *   The effect type
 */
private[ldbc] case class DatabaseMetaDataUsingInfoSchema[F[_]: Temporal: Exchange: Tracer](
  protocol:                      Protocol[F],
  serverVariables:               Map[String, String],
  database:                      Option[String]       = None,
  databaseTerm:                  Option[DatabaseTerm] = None,
  getProceduresReturnsFunctions: Boolean              = true
)(using ev: MonadError[F, Throwable])
  extends Impl[F](protocol, serverVariables, database, databaseTerm, getProceduresReturnsFunctions):

  override def getProcedures(
    catalog:              Option[String],
    schemaPattern:        Option[String],
    procedureNamePattern: Option[String]
  ): F[ResultSet[F]] =

    val db = getDatabase(catalog, schemaPattern)

    val sqlBuf = new StringBuilder(
      if databaseTerm.contains(DatabaseTerm.SCHEMA) then
        "SELECT ROUTINE_CATALOG AS PROCEDURE_CAT, ROUTINE_SCHEMA AS PROCEDURE_SCHEM,"
      else "SELECT ROUTINE_SCHEMA AS PROCEDURE_CAT, NULL AS PROCEDURE_SCHEM,"
    )
    sqlBuf.append(
      " ROUTINE_NAME AS PROCEDURE_NAME, NULL AS RESERVED_1, NULL AS RESERVED_2, NULL AS RESERVED_3, ROUTINE_COMMENT AS REMARKS, CASE WHEN ROUTINE_TYPE = 'PROCEDURE' THEN "
    )
    sqlBuf.append(procedureNoResult)
    sqlBuf.append(" WHEN ROUTINE_TYPE='FUNCTION' THEN ")
    sqlBuf.append(procedureReturnsResult)
    sqlBuf.append(" ELSE ")
    sqlBuf.append(procedureResultUnknown)
    sqlBuf.append(" END AS PROCEDURE_TYPE, ROUTINE_NAME AS SPECIFIC_NAME FROM INFORMATION_SCHEMA.ROUTINES")

    val conditionBuf = new StringBuilder()

    if getProceduresReturnsFunctions then conditionBuf.append(" ROUTINE_TYPE = 'PROCEDURE'")
    end if

    if db.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      end if

      conditionBuf.append(
        if databaseTerm.contains(DatabaseTerm.SCHEMA) then " ROUTINE_SCHEMA LIKE ?"
        else " ROUTINE_SCHEMA = ?"
      )
    end if

    if procedureNamePattern.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      end if

      conditionBuf.append(" ROUTINE_NAME LIKE ?")
    end if

    if conditionBuf.nonEmpty then
      sqlBuf.append(" WHERE")
      sqlBuf.append(conditionBuf)
    end if

    sqlBuf.append(" ORDER BY ROUTINE_SCHEMA, ROUTINE_NAME, ROUTINE_TYPE")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = (db, procedureNamePattern) match
        case (Some(dbValue), Some(procedureName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, procedureName)
        case (Some(dbValue), None)       => preparedStatement.setString(1, dbValue)
        case (None, Some(procedureName)) => preparedStatement.setString(1, procedureName)
        case _                           => ev.unit

      setting *> preparedStatement.executeQuery() <* preparedStatement.close()
    }
