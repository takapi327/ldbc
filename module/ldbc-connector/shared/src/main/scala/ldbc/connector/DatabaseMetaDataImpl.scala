/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.util.{ Locale, StringTokenizer }

import scala.collection.immutable.{ ListMap, SortedMap }

import cats.*
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.{ Connection, DatabaseMetaData, PreparedStatement, ResultSet, RowIdLifetime, Statement }
import ldbc.sql.Types.*

import ldbc.connector.data.*
import ldbc.connector.data.Constants.*
import ldbc.connector.exception.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.protocol.*
import ldbc.connector.net.Protocol
import ldbc.connector.util.StringHelper
import ldbc.connector.util.Version

private[ldbc] case class DatabaseMetaDataImpl[F[_]: Temporal: Exchange: Tracer](
  protocol:                      Protocol[F],
  serverVariables:               Map[String, String],
  connectionClosed:              Ref[F, Boolean],
  statementClosed:               Ref[F, Boolean],
  resultSetClosed:               Ref[F, Boolean],
  database:                      Option[String]                = None,
  databaseTerm:                  DatabaseMetaData.DatabaseTerm = DatabaseMetaData.DatabaseTerm.CATALOG,
  getProceduresReturnsFunctions: Boolean                       = true,
  tinyInt1isBit:                 Boolean                       = true,
  transformedBitIsBoolean:       Boolean                       = false,
  yearIsDateType:                Boolean                       = true,
  nullDatabaseMeansCurrent:      Boolean                       = false
)(using ev: MonadError[F, Throwable])
  extends DatabaseMetaDataImpl.StaticDatabaseMetaData[F]:

  private enum FunctionConstant:
    case FUNCTION_COLUMN_UNKNOWN, FUNCTION_COLUMN_IN, FUNCTION_COLUMN_INOUT, FUNCTION_COLUMN_OUT,
      FUNCTION_COLUMN_RETURN, FUNCTION_COLUMN_RESULT, FUNCTION_NO_NULLS, FUNCTION_NULLABLE, FUNCTION_NULLABLE_UNKNOWN

  private enum TableType(val name: String):
    case LOCAL_TEMPORARY extends TableType("LOCAL TEMPORARY")
    case SYSTEM_TABLE    extends TableType("SYSTEM TABLE")
    case SYSTEM_VIEW     extends TableType("SYSTEM VIEW")
    case TABLE           extends TableType("TABLE")
    case VIEW            extends TableType("VIEW")
    case UNKNOWN         extends TableType("UNKNOWN")

    override def toString: String = name

  override def getURL(): String = protocol.hostInfo.url

  override def getUserName(): F[String] =
    protocol.resetSequenceId *>
      protocol.send(ComQueryPacket("SELECT USER()", protocol.initialPacket.capabilityFlags, ListMap.empty)) *>
      protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
        case _: OKPacket      => ev.pure("")
        case error: ERRPacket => ev.raiseError(error.toException(Some("SELECT USER()"), None))
        case result: ColumnsNumberPacket =>
          for
            columnDefinitions <-
              protocol.repeatProcess(
                result.size,
                ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
              )
            resultSetRow <- protocol.readUntilEOF[ResultSetRowPacket](
                              ResultSetRowPacket.decoder(columnDefinitions.length)
                            )
          yield resultSetRow.headOption.flatMap(_.values.headOption).flatten.getOrElse("")
      }

  override def getDatabaseProductVersion(): String = protocol.initialPacket.serverVersion.toString

  override def supportsMixedCaseIdentifiers(): Boolean =
    serverVariables.get("lower_case_table_names") match
      case Some(lowerCaseTables) =>
        !("on".equalsIgnoreCase(lowerCaseTables) || "1".equalsIgnoreCase(lowerCaseTables) || "2".equalsIgnoreCase(
          lowerCaseTables
        ))
      case None => false

  override def storesLowerCaseIdentifiers(): Boolean =
    serverVariables.get("lower_case_table_names") match
      case Some(lowerCaseTables) => "on".equalsIgnoreCase(lowerCaseTables) || "1".equalsIgnoreCase(lowerCaseTables)
      case None                  => false

  override def getIdentifierQuoteString(): String =
    val sqlModeAsString = serverVariables.get("sql_mode")
    sqlModeAsString match
      case Some(sqlMode) =>
        if sqlMode.contains("ANSI_QUOTES") then "\""
        else "`"
      case None => "`"

  override def getSQLKeywords(): F[String] =
    protocol.resetSequenceId *>
      protocol.send(
        ComQueryPacket(
          "SELECT WORD FROM INFORMATION_SCHEMA.KEYWORDS WHERE RESERVED=1 ORDER BY WORD",
          protocol.initialPacket.capabilityFlags,
          ListMap.empty
        )
      ) *>
      protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
        case _: OKPacket => ev.pure("")
        case error: ERRPacket =>
          ev.raiseError(
            error.toException(
              Some("SELECT WORD FROM INFORMATION_SCHEMA.KEYWORDS WHERE RESERVED=1 ORDER BY WORD"),
              None
            )
          )
        case result: ColumnsNumberPacket =>
          for
            columnDefinitions <-
              protocol.repeatProcess(
                result.size,
                ColumnDefinitionPacket.decoder(protocol.initialPacket.capabilityFlags)
              )
            resultSetRow <- protocol.readUntilEOF[ResultSetRowPacket](
                              ResultSetRowPacket.decoder(columnDefinitions.length)
                            )
          yield resultSetRow
            .flatMap(_.values.flatten)
            .filterNot(DatabaseMetaDataImpl.SQL2003_KEYWORDS.contains)
            .mkString(",")
      }

  override def getSchemaTerm(): String = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => "SCHEMA"
    case DatabaseMetaData.DatabaseTerm.CATALOG => ""

  override def getProcedureTerm(): String = "PROCEDURE"

  override def getCatalogTerm(): String = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => "CATALOG"
    case DatabaseMetaData.DatabaseTerm.CATALOG => "database"

  override def supportsSchemasInDataManipulation(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => true
    case DatabaseMetaData.DatabaseTerm.CATALOG => false

  override def supportsSchemasInProcedureCalls(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => true
    case DatabaseMetaData.DatabaseTerm.CATALOG => false

  override def supportsSchemasInTableDefinitions(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => true
    case DatabaseMetaData.DatabaseTerm.CATALOG => false

  override def supportsSchemasInIndexDefinitions(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => true
    case DatabaseMetaData.DatabaseTerm.CATALOG => false

  override def supportsSchemasInPrivilegeDefinitions(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => true
    case DatabaseMetaData.DatabaseTerm.CATALOG => false

  override def supportsCatalogsInDataManipulation(): Boolean =
    databaseTerm == DatabaseMetaData.DatabaseTerm.CATALOG

  override def supportsCatalogsInProcedureCalls(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => false
    case DatabaseMetaData.DatabaseTerm.CATALOG => true

  override def supportsCatalogsInTableDefinitions(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => false
    case DatabaseMetaData.DatabaseTerm.CATALOG => true

  override def supportsCatalogsInIndexDefinitions(): Boolean = databaseTerm match
    case DatabaseMetaData.DatabaseTerm.SCHEMA  => false
    case DatabaseMetaData.DatabaseTerm.CATALOG => true

  override def supportsCatalogsInPrivilegeDefinitions(): Boolean =
    databaseTerm == DatabaseMetaData.DatabaseTerm.CATALOG

  override def getProcedures(
    catalog:              Option[String],
    schemaPattern:        Option[String],
    procedureNamePattern: Option[String]
  ): F[ResultSet] =
    val db = getDatabase(catalog, schemaPattern)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA =>
          "SELECT ROUTINE_CATALOG AS PROCEDURE_CAT, ROUTINE_SCHEMA AS PROCEDURE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => "SELECT ROUTINE_SCHEMA AS PROCEDURE_CAT, NULL AS PROCEDURE_SCHEM,"
    )
    sqlBuf.append(
      " ROUTINE_NAME AS PROCEDURE_NAME, NULL AS RESERVED_1, NULL AS RESERVED_2, NULL AS RESERVED_3, ROUTINE_COMMENT AS REMARKS, CASE WHEN ROUTINE_TYPE = 'PROCEDURE' THEN "
    )
    sqlBuf.append(DatabaseMetaData.procedureNoResult)
    sqlBuf.append(" WHEN ROUTINE_TYPE='FUNCTION' THEN ")
    sqlBuf.append(DatabaseMetaData.procedureReturnsResult)
    sqlBuf.append(" ELSE ")
    sqlBuf.append(DatabaseMetaData.procedureResultUnknown)
    sqlBuf.append(" END AS PROCEDURE_TYPE, ROUTINE_NAME AS SPECIFIC_NAME FROM INFORMATION_SCHEMA.ROUTINES")

    val conditionBuf = new StringBuilder()

    if !getProceduresReturnsFunctions then conditionBuf.append(" ROUTINE_TYPE = 'PROCEDURE'")
    end if

    if db.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      end if

      conditionBuf.append(
        databaseTerm match
          case DatabaseMetaData.DatabaseTerm.SCHEMA  => " ROUTINE_SCHEMA LIKE ?"
          case DatabaseMetaData.DatabaseTerm.CATALOG => " ROUTINE_SCHEMA = ?"
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

      setting *> preparedStatement.executeQuery()
    }

  override def getProcedureColumns(
    catalog:              Option[String],
    schemaPattern:        Option[String],
    procedureNamePattern: Option[String],
    columnNamePattern:    Option[String]
  ): F[ResultSet] =
    val db = getDatabase(catalog, schemaPattern)

    val supportsFractSeconds = protocol.initialPacket.serverVersion.compare(Version(5, 6, 4)) >= 0

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA =>
          "SELECT SPECIFIC_CATALOG AS PROCEDURE_CAT, SPECIFIC_SCHEMA AS `PROCEDURE_SCHEM`,"
        case DatabaseMetaData.DatabaseTerm.CATALOG =>
          "SELECT SPECIFIC_SCHEMA AS PROCEDURE_CAT, NULL AS `PROCEDURE_SCHEM`,"
    )

    sqlBuf.append(" SPECIFIC_NAME AS `PROCEDURE_NAME`, IFNULL(PARAMETER_NAME, '') AS `COLUMN_NAME`,")
    sqlBuf.append(" CASE WHEN PARAMETER_MODE = 'IN' THEN ")
    sqlBuf.append(DatabaseMetaData.procedureColumnIn)
    sqlBuf.append(" WHEN PARAMETER_MODE = 'OUT' THEN ")
    sqlBuf.append(DatabaseMetaData.procedureColumnOut)
    sqlBuf.append(" WHEN PARAMETER_MODE = 'INOUT' THEN ")
    sqlBuf.append(DatabaseMetaData.procedureColumnInOut)
    sqlBuf.append(" WHEN ORDINAL_POSITION = 0 THEN ")
    sqlBuf.append(DatabaseMetaData.procedureColumnReturn)
    sqlBuf.append(" ELSE ")
    sqlBuf.append(DatabaseMetaData.procedureColumnUnknown)
    sqlBuf.append(" END AS `COLUMN_TYPE`, ")
    appendJdbcTypeMappingQuery(sqlBuf, "DATA_TYPE", "DTD_IDENTIFIER")
    sqlBuf.append(" AS `DATA_TYPE`, ")

    sqlBuf.append("UPPER(CASE")

    if tinyInt1isBit then
      sqlBuf.append(" WHEN UPPER(DATA_TYPE)='TINYINT' THEN CASE")
      sqlBuf.append(
        " WHEN LOCATE('ZEROFILL', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('(1)', DTD_IDENTIFIER) != 0 THEN "
      )
      sqlBuf.append(if transformedBitIsBoolean then "'BOOLEAN'" else "'BIT'")
      sqlBuf.append(
        " WHEN LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 AND LOCATE('UNSIGNED', UPPER(DATA_TYPE)) = 0 THEN 'TINYINT UNSIGNED'"
      )
      sqlBuf.append(" ELSE DATA_TYPE END ")
    end if

    sqlBuf.append(
      " WHEN LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 AND LOCATE('UNSIGNED', UPPER(DATA_TYPE)) = 0 AND LOCATE('SET', UPPER(DATA_TYPE)) <> 1 AND LOCATE('ENUM', UPPER(DATA_TYPE)) <> 1 THEN CONCAT(DATA_TYPE, ' UNSIGNED')"
    )

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POINT' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='LINESTRING' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POLYGON' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOINT' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTILINESTRING' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOLYGON' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMETRYCOLLECTION' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMCOLLECTION' THEN 'GEOMETRY'")

    sqlBuf.append(" ELSE UPPER(DATA_TYPE) END) AS TYPE_NAME,")

    sqlBuf.append(" CASE WHEN LCASE(DATA_TYPE)='date' THEN 0")

    if supportsFractSeconds then
      sqlBuf.append(
        " WHEN LCASE(DATA_TYPE)='time' OR LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp' THEN DATETIME_PRECISION"
      )
    else
      sqlBuf.append(
        " WHEN LCASE(DATA_TYPE)='time' OR LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp' THEN 0"
      )

    if tinyInt1isBit && !transformedBitIsBoolean then
      sqlBuf.append(
        " WHEN (UPPER(DATA_TYPE)='TINYINT' AND LOCATE('ZEROFILL', UPPER(DTD_IDENTIFIER)) = 0) AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('(1)', DTD_IDENTIFIER) != 0 THEN 1"
      )
    end if

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MEDIUMINT' AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 THEN 8")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='JSON' THEN 1073741824")
    sqlBuf.append(" ELSE NUMERIC_PRECISION END AS `PRECISION`,")

    sqlBuf.append(" CASE WHEN LCASE(DATA_TYPE)='date' THEN 10")

    if supportsFractSeconds then
      sqlBuf.append(
        " WHEN LCASE(DATA_TYPE)='time' THEN 8+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)"
      )
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp'")
      sqlBuf.append(
        "  THEN 19+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)"
      )
    else
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='time' THEN 8")
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp' THEN 19")

    if tinyInt1isBit && !transformedBitIsBoolean then
      sqlBuf.append(
        " WHEN (UPPER(DATA_TYPE)='TINYINT' OR UPPER(DATA_TYPE)='TINYINT UNSIGNED') AND LOCATE('ZEROFILL', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('(1)', DTD_IDENTIFIER) != 0 THEN 1"
      )
    end if

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MEDIUMINT' AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 THEN 8")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='JSON' THEN 1073741824")
    sqlBuf.append(" WHEN CHARACTER_MAXIMUM_LENGTH IS NULL THEN NUMERIC_PRECISION")
    sqlBuf.append(" WHEN CHARACTER_MAXIMUM_LENGTH > ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" THEN ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" ELSE CHARACTER_MAXIMUM_LENGTH END AS LENGTH,")

    sqlBuf.append("NUMERIC_SCALE AS `SCALE`, ")
    sqlBuf.append("10 AS RADIX,")
    sqlBuf.append(DatabaseMetaData.procedureNullable)
    sqlBuf.append(
      " AS `NULLABLE`, NULL AS `REMARKS`, NULL AS `COLUMN_DEF`, NULL AS `SQL_DATA_TYPE`, NULL AS `SQL_DATETIME_SUB`,"
    )

    sqlBuf.append(" CASE WHEN CHARACTER_OCTET_LENGTH > ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" THEN ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" ELSE CHARACTER_OCTET_LENGTH END AS `CHAR_OCTET_LENGTH`,")

    sqlBuf.append(" ORDINAL_POSITION, 'YES' AS `IS_NULLABLE`, SPECIFIC_NAME")
    sqlBuf.append(" FROM INFORMATION_SCHEMA.PARAMETERS")

    val conditionBuf = new StringBuilder()

    if !getProceduresReturnsFunctions then conditionBuf.append(" ROUTINE_TYPE = 'PROCEDURE'")
    end if

    if db.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      end if

      conditionBuf.append(
        databaseTerm match
          case DatabaseMetaData.DatabaseTerm.SCHEMA  => " SPECIFIC_SCHEMA LIKE ?"
          case DatabaseMetaData.DatabaseTerm.CATALOG => " SPECIFIC_SCHEMA = ?"
      )
    end if

    if procedureNamePattern.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      end if

      conditionBuf.append(" SPECIFIC_NAME LIKE ?")
    end if

    if columnNamePattern.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      end if

      conditionBuf.append(" (PARAMETER_NAME LIKE ? OR PARAMETER_NAME IS NULL)")
    end if

    if conditionBuf.nonEmpty then
      sqlBuf.append(" WHERE")
      sqlBuf.append(conditionBuf)
    end if

    sqlBuf.append(" ORDER BY SPECIFIC_SCHEMA, SPECIFIC_NAME, ROUTINE_TYPE, ORDINAL_POSITION")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = (db, procedureNamePattern, columnNamePattern) match
        case (Some(dbValue), Some(procedureName), Some(columnName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(
            2,
            procedureName
          ) *> preparedStatement.setString(3, columnName)
        case (Some(dbValue), Some(procedureName), None) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, procedureName)
        case (Some(dbValue), None, Some(columnName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, columnName)
        case (Some(dbValue), None, None) => preparedStatement.setString(1, dbValue)
        case (None, Some(procedureName), Some(columnName)) =>
          preparedStatement.setString(1, procedureName) *> preparedStatement.setString(2, columnName)
        case (None, Some(procedureName), None) => preparedStatement.setString(1, procedureName)
        case (None, None, Some(columnName))    => preparedStatement.setString(1, columnName)
        case (None, None, None)                => ev.unit

      setting *> preparedStatement.executeQuery()
    }

  override def getTables(
    catalog:          Option[String],
    schemaPattern:    Option[String],
    tableNamePattern: Option[String],
    types:            Array[String]
  ): F[ResultSet] =
    val db = getDatabase(catalog, schemaPattern)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA  => "SELECT TABLE_CATALOG AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => "SELECT TABLE_SCHEMA AS TABLE_CAT, NULL AS TABLE_SCHEM,"
    )

    sqlBuf.append(
      " TABLE_NAME, CASE WHEN TABLE_TYPE='BASE TABLE' THEN CASE WHEN TABLE_SCHEMA = 'mysql' OR TABLE_SCHEMA = 'performance_schema' THEN 'SYSTEM TABLE' "
    )
    sqlBuf.append(
      "ELSE 'TABLE' END WHEN TABLE_TYPE='TEMPORARY' THEN 'LOCAL_TEMPORARY' ELSE TABLE_TYPE END AS TABLE_TYPE, "
    )
    sqlBuf.append(
      "TABLE_COMMENT AS REMARKS, NULL AS TYPE_CAT, NULL AS TYPE_SCHEM, NULL AS TYPE_NAME, NULL AS SELF_REFERENCING_COL_NAME, "
    )
    sqlBuf.append("NULL AS REF_GENERATION FROM INFORMATION_SCHEMA.TABLES")

    if db.nonEmpty || tableNamePattern.nonEmpty then sqlBuf.append(" WHERE")
    end if

    db match
      case Some(dbValue) =>
        sqlBuf.append(
          if "information_schema".equalsIgnoreCase(dbValue) || "performance_schema".equalsIgnoreCase(
              dbValue
            ) || !StringHelper.hasWildcards(dbValue)
            || databaseTerm == DatabaseMetaData.DatabaseTerm.CATALOG
          then " TABLE_SCHEMA = ?"
          else " TABLE_SCHEMA LIKE ?"
        )
      case None => ()

    tableNamePattern match
      case Some(tableName) =>
        if db.nonEmpty then sqlBuf.append(" AND")
        end if
        if StringHelper.hasWildcards(tableName) then sqlBuf.append(" TABLE_NAME LIKE ?")
        else sqlBuf.append(" TABLE_NAME = ?")
      case None => ()

    if types.nonEmpty then sqlBuf.append(" HAVING TABLE_TYPE IN (?,?,?,?,?)")
    end if

    sqlBuf.append(" ORDER BY TABLE_TYPE, TABLE_SCHEMA, TABLE_NAME")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      (
        db match
          case Some(dbName) => preparedStatement.setString(1, dbName)
          case None         => preparedStatement.setString(1, "%")
      ) *> (
        tableNamePattern match
          case Some(tableName) => preparedStatement.setString(2, tableName)
          case None            => ev.unit
      ) *> (
        if types.nonEmpty then
          List.fill(5)("").zipWithIndex.foldLeft(ev.unit) {
            case (acc, (_, index)) =>
              acc *> preparedStatement.setNull(index + 3, MysqlType.NULL.jdbcType)
          } *>
            types.zipWithIndex.foldLeft(ev.unit) {
              case (acc, (tableType, index)) =>
                preparedStatement.setString(index + 3, tableType)
            }
        else ev.unit
      ) *> preparedStatement.executeQuery()
    }

  override def getCatalogs(): F[ResultSet] =
    (if databaseTerm == DatabaseMetaData.DatabaseTerm.SCHEMA then ev.pure(List.empty[String])
     else getDatabases(None)).map { dbList =>
      ResultSetImpl(
        Vector("TABLE_CAT").map { value =>
          new ColumnDefinitionPacket:
            override def table:      String                     = ""
            override def name:       String                     = value
            override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_VARCHAR
            override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
        },
        dbList.map(name => ResultSetRowPacket(Array(Some(name)))).toVector,
        serverVariables,
        protocol.initialPacket.serverVersion
      )
    }

  override def getTableTypes(): ResultSet =
    ResultSetImpl(
      Vector(
        new ColumnDefinitionPacket:
          override def table:      String                     = ""
          override def name:       String                     = "TABLE_TYPE"
          override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_VARCHAR
          override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
      ),
      TableType.values
        .filterNot(_ == TableType.UNKNOWN)
        .map(tableType => ResultSetRowPacket(Array(Some(tableType.name))))
        .toVector,
      serverVariables,
      protocol.initialPacket.serverVersion
    )

  override def getColumns(
    catalog:           Option[String],
    schemaPattern:     Option[String],
    tableName:         Option[String],
    columnNamePattern: Option[String]
  ): F[ResultSet] =
    val db = getDatabase(catalog, schemaPattern)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA  => "SELECT TABLE_CATALOG, TABLE_SCHEMA,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => "SELECT TABLE_SCHEMA, NULL,"
    )

    sqlBuf.append(" TABLE_NAME, COLUMN_NAME,")

    appendJdbcTypeMappingQuery(sqlBuf, "DATA_TYPE", "COLUMN_TYPE")
    sqlBuf.append(" AS DATA_TYPE, ")

    sqlBuf.append("UPPER(CASE")
    if tinyInt1isBit then
      sqlBuf.append(" WHEN UPPER(DATA_TYPE)='TINYINT' THEN CASE")
      sqlBuf.append(
        " WHEN LOCATE('ZEROFILL', UPPER(COLUMN_TYPE)) = 0 AND LOCATE('UNSIGNED', UPPER(COLUMN_TYPE)) = 0 AND LOCATE('(1)', COLUMN_TYPE) != 0 THEN "
      )
      sqlBuf.append(if transformedBitIsBoolean then "'BOOLEAN'" else "'BIT'")
      sqlBuf.append(
        " WHEN LOCATE('UNSIGNED', UPPER(COLUMN_TYPE)) != 0 AND LOCATE('UNSIGNED', UPPER(DATA_TYPE)) = 0 THEN 'TINYINT UNSIGNED'"
      )
      sqlBuf.append(" ELSE DATA_TYPE END ")
    end if

    sqlBuf.append(
      " WHEN LOCATE('UNSIGNED', UPPER(COLUMN_TYPE)) != 0 AND LOCATE('UNSIGNED', UPPER(DATA_TYPE)) = 0 AND LOCATE('SET', UPPER(DATA_TYPE)) <> 1 AND LOCATE('ENUM', UPPER(DATA_TYPE)) <> 1 THEN CONCAT(DATA_TYPE, ' UNSIGNED')"
    )

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POINT' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='LINESTRING' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POLYGON' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOINT' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTILINESTRING' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOLYGON' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMETRYCOLLECTION' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMCOLLECTION' THEN 'GEOMETRY'")

    sqlBuf.append(" ELSE UPPER(DATA_TYPE) END) AS TYPE_NAME,")

    sqlBuf.append("UPPER(CASE")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='DATE' THEN 10")

    if protocol.initialPacket.serverVersion.compare(Version(5, 6, 4)) >= 0 then
      sqlBuf.append(" WHEN UPPER(DATA_TYPE)='TIME'")
      sqlBuf.append("  THEN 8+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)")
      sqlBuf.append(" WHEN UPPER(DATA_TYPE)='DATETIME' OR")
      sqlBuf.append("  UPPER(DATA_TYPE)='TIMESTAMP'")
      sqlBuf.append(
        "  THEN 19+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)"
      )
    else
      sqlBuf.append(" WHEN UPPER(DATA_TYPE)='TIME' THEN 8")
      sqlBuf.append(" WHEN UPPER(DATA_TYPE)='DATETIME' OR")
      sqlBuf.append("  UPPER(DATA_TYPE)='TIMESTAMP'")
      sqlBuf.append("  THEN 19")

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='YEAR' THEN 4")
    if tinyInt1isBit && !transformedBitIsBoolean then
      sqlBuf.append(
        " WHEN UPPER(DATA_TYPE)='TINYINT' AND LOCATE('ZEROFILL', UPPER(COLUMN_TYPE)) = 0 AND LOCATE('UNSIGNED', UPPER(COLUMN_TYPE)) = 0 AND LOCATE('(1)', COLUMN_TYPE) != 0 THEN 1"
      )

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MEDIUMINT' AND LOCATE('UNSIGNED', UPPER(COLUMN_TYPE)) != 0 THEN 8")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='JSON' THEN 1073741824")

    // spatial data types
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMETRY' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POINT' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='LINESTRING' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POLYGON' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOINT' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTILINESTRING' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOLYGON' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMETRYCOLLECTION' THEN 65535")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMCOLLECTION' THEN 65535")

    sqlBuf.append(" WHEN CHARACTER_MAXIMUM_LENGTH IS NULL THEN NUMERIC_PRECISION")
    sqlBuf.append(" WHEN CHARACTER_MAXIMUM_LENGTH > ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" THEN ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" ELSE CHARACTER_MAXIMUM_LENGTH")
    sqlBuf.append(" END) AS COLUMN_SIZE,")

    sqlBuf.append(DatabaseMetaDataImpl.maxBufferSize)
    sqlBuf.append(" AS BUFFER_LENGTH,")

    sqlBuf.append("UPPER(CASE")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='DECIMAL' THEN NUMERIC_SCALE")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='FLOAT' OR UPPER(DATA_TYPE)='DOUBLE' THEN")
    sqlBuf.append(" CASE WHEN NUMERIC_SCALE IS NULL THEN 0")
    sqlBuf.append(" ELSE NUMERIC_SCALE END")
    sqlBuf.append(" ELSE NULL END) AS DECIMAL_DIGITS,")

    sqlBuf.append("10 AS NUM_PREC_RADIX,")

    sqlBuf.append("CASE")
    sqlBuf.append(" WHEN IS_NULLABLE='NO' THEN ")
    sqlBuf.append(DatabaseMetaData.columnNoNulls)
    sqlBuf.append(" ELSE CASE WHEN IS_NULLABLE='YES' THEN ")
    sqlBuf.append(DatabaseMetaData.columnNullable)
    sqlBuf.append(" ELSE ")
    sqlBuf.append(DatabaseMetaData.columnNullableUnknown)
    sqlBuf.append(" END END AS NULLABLE,")

    sqlBuf.append("COLUMN_COMMENT AS REMARKS,")
    sqlBuf.append("COLUMN_DEFAULT AS COLUMN_DEF,")
    sqlBuf.append("0 AS SQL_DATA_TYPE,")
    sqlBuf.append("0 AS SQL_DATETIME_SUB,")

    sqlBuf.append("CASE WHEN CHARACTER_OCTET_LENGTH > ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" THEN ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" ELSE CHARACTER_OCTET_LENGTH END AS CHAR_OCTET_LENGTH,")

    sqlBuf.append(
      "ORDINAL_POSITION, IS_NULLABLE, NULL AS SCOPE_CATALOG, NULL AS SCOPE_SCHEMA, NULL AS SCOPE_TABLE, NULL AS SOURCE_DATA_TYPE,"
    )
    sqlBuf.append("IF (EXTRA LIKE '%auto_increment%','YES','NO') AS IS_AUTOINCREMENT, ")
    sqlBuf.append("IF (EXTRA LIKE '%GENERATED%','YES','NO') AS IS_GENERATEDCOLUMN ")

    sqlBuf.append("FROM INFORMATION_SCHEMA.COLUMNS")

    val conditionBuf = new StringBuilder()

    db.foreach(dbValue =>
      conditionBuf.append(
        if "information_schema".equalsIgnoreCase(dbValue) || "performance_schema".equalsIgnoreCase(
            dbValue
          ) || !StringHelper.hasWildcards(dbValue)
          || databaseTerm == DatabaseMetaData.DatabaseTerm.CATALOG
        then " TABLE_SCHEMA = ?"
        else " TABLE_SCHEMA LIKE ?"
      )
    )

    tableName.foreach(name =>
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")

      conditionBuf.append(
        if StringHelper.hasWildcards(name) then " TABLE_NAME LIKE ?"
        else " TABLE_NAME = ?"
      )
    )

    columnNamePattern.foreach(columnName =>
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")

      conditionBuf.append(
        if StringHelper.hasWildcards(columnName) then " COLUMN_NAME LIKE ?"
        else " COLUMN_NAME = ?"
      )
    )

    if conditionBuf.nonEmpty then sqlBuf.append(" WHERE")
    end if

    sqlBuf.append(conditionBuf)

    sqlBuf.append(" ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION")

    prepareMetaDataSafeStatement(sqlBuf.toString())
      .flatMap { preparedStatement =>
        val settings = (db, tableName, columnNamePattern) match
          case (Some(dbValue), Some(tableNameValue), Some(columnName)) =>
            preparedStatement.setString(1, dbValue) *> preparedStatement.setString(
              2,
              tableNameValue
            ) *> preparedStatement.setString(3, columnName)
          case (Some(dbValue), Some(tableNameValue), None) =>
            preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, tableNameValue)
          case (Some(dbValue), None, Some(columnName)) =>
            preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, columnName)
          case (Some(dbValue), None, None) => preparedStatement.setString(1, dbValue)
          case (None, Some(tableNameValue), Some(columnName)) =>
            preparedStatement.setString(1, tableNameValue) *> preparedStatement.setString(2, columnName)
          case (None, Some(tableNameValue), None) => preparedStatement.setString(1, tableNameValue)
          case (None, None, Some(columnName))     => preparedStatement.setString(1, columnName)
          case (None, None, None)                 => ev.unit

        settings *> preparedStatement.executeQuery()
      }
      .map { resultSet =>
        ResultSetImpl(
          Vector(
            "TABLE_CAT",
            "TABLE_SCHEM",
            "TABLE_NAME",
            "COLUMN_NAME",
            "DATA_TYPE",
            "TYPE_NAME",
            "COLUMN_SIZE",
            "BUFFER_LENGTH",
            "DECIMAL_DIGITS",
            "NUM_PREC_RADIX",
            "NULLABLE",
            "REMARKS",
            "COLUMN_DEF",
            "SQL_DATA_TYPE",
            "SQL_DATETIME_SUB",
            "CHAR_OCTET_LENGTH",
            "ORDINAL_POSITION",
            "IS_NULLABLE",
            "SCOPE_CATALOG",
            "SCOPE_SCHEMA",
            "SCOPE_TABLE",
            "SOURCE_DATA_TYPE",
            "IS_AUTOINCREMENT",
            "IS_GENERATEDCOLUMN"
          ).map(value =>
            new ColumnDefinitionPacket:
              override def table:      String                     = ""
              override def name:       String                     = value
              override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_VARCHAR
              override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
          ),
          resultSet.asInstanceOf[ResultSetImpl].records,
          serverVariables,
          protocol.initialPacket.serverVersion
        )
      }

  override def getColumnPrivileges(
    catalog:           Option[String],
    schema:            Option[String],
    table:             Option[String],
    columnNamePattern: Option[String]
  ): F[ResultSet] =
    val db = getDatabase(catalog, schema)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA  => "SELECT TABLE_CATALOG AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => "SELECT TABLE_SCHEMA AS TABLE_CAT, NULL AS TABLE_SCHEM,"
    )

    sqlBuf.append(
      " TABLE_NAME, COLUMN_NAME, NULL AS GRANTOR, GRANTEE, PRIVILEGE_TYPE AS PRIVILEGE, IS_GRANTABLE FROM INFORMATION_SCHEMA.COLUMN_PRIVILEGES WHERE"
    )

    if db.nonEmpty then sqlBuf.append(" TABLE_SCHEMA=? AND")
    end if

    sqlBuf.append(" TABLE_NAME =?")
    if columnNamePattern.nonEmpty then sqlBuf.append(" AND COLUMN_NAME LIKE ?")
    end if
    sqlBuf.append(" ORDER BY COLUMN_NAME, PRIVILEGE_TYPE")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = (db, table, columnNamePattern) match
        case (Some(dbValue), Some(tableName), Some(columnName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, tableName) *> preparedStatement
            .setString(3, columnName)
        case (Some(dbValue), Some(tableName), None) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, tableName)
        case (Some(dbValue), None, Some(columnName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, columnName)
        case (Some(dbValue), None, None) => preparedStatement.setString(1, dbValue)
        case (None, Some(tableName), Some(columnName)) =>
          preparedStatement.setString(1, tableName) *> preparedStatement.setString(2, columnName)
        case (None, Some(tableName), None)  => preparedStatement.setString(1, tableName)
        case (None, None, Some(columnName)) => preparedStatement.setString(1, columnName)
        case (None, None, None)             => ev.unit

      setting *> preparedStatement.executeQuery()
    }

  override def getTablePrivileges(
    catalog:          Option[String],
    schemaPattern:    Option[String],
    tableNamePattern: Option[String]
  ): F[ResultSet] =

    val db = getDatabase(catalog, schemaPattern)

    val sqlBuf = new StringBuilder(
      "SELECT host,db,table_name,grantor,user,table_priv FROM mysql.tables_priv"
    )

    val conditionBuf = new StringBuilder()

    if db.nonEmpty then
      conditionBuf.append(
        if databaseTerm == DatabaseMetaData.DatabaseTerm.SCHEMA then " db LIKE ?" else " db = ?"
      )
    end if

    if tableNamePattern.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      end if
      conditionBuf.append(" table_name LIKE ?")
    end if

    if conditionBuf.nonEmpty then
      sqlBuf.append(" WHERE")
      sqlBuf.append(conditionBuf)
    end if

    prepareMetaDataSafeStatement(sqlBuf.toString())
      .flatMap { preparedStatement =>
        val setting = (db, tableNamePattern) match
          case (Some(dbValue), Some(tableName)) =>
            preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, tableName)
          case (Some(dbValue), None)   => preparedStatement.setString(1, dbValue)
          case (None, Some(tableName)) => preparedStatement.setString(1, tableName)
          case _                       => ev.unit

        setting *> preparedStatement.executeQuery()
      }
      .flatMap { resultSet =>

        val keys = Vector.newBuilder[(Option[String], Option[String], Option[String], String, String)]
        while resultSet.next() do
          val host    = Option(resultSet.getString(1))
          val db      = Option(resultSet.getString(2))
          val table   = Option(resultSet.getString(3))
          val grantor = Option(resultSet.getString(4))
          val user    = Option(resultSet.getString(5)).getOrElse("%")

          val fullUser = new StringBuilder(user)
          host.foreach(h => fullUser.append("@").append(h))

          Option(resultSet.getString(6)) match
            case Some(value) =>
              val allPrivileges   = value.toUpperCase(Locale.ENGLISH)
              val stringTokenizer = new StringTokenizer(allPrivileges, ",")

              while stringTokenizer.hasMoreTokens do
                val privilege = stringTokenizer.nextToken().trim

                keys += ((db, table, grantor, fullUser.toString(), privilege))
              end while

            case None => // no privileges
        end while

        val records = keys
          .result()
          .traverse { (db, table, grantor, user, privilege) =>
            val columnResults = getColumns(catalog, schemaPattern, table, None)
            columnResults.map { columnResult =>
              val records = Vector.newBuilder[ResultSetRowPacket]
              while columnResult.next() do
                val rows = Array(
                  if databaseTerm == DatabaseMetaData.DatabaseTerm.SCHEMA then Some("def") else db, // TABLE_CAT
                  if databaseTerm == DatabaseMetaData.DatabaseTerm.SCHEMA then db else None,        // TABLE_SCHEM
                  table,                                                                            // TABLE_NAME
                  grantor,                                                                          // GRANTOR
                  Some(user),                                                                       // GRANTEE
                  Some(privilege),                                                                  // PRIVILEGE
                  None                                                                              // IS_GRANTABLE
                )
                records += ResultSetRowPacket(rows)
              records.result()
            }
          }
          .map(_.flatten)

        records.map { records =>
          ResultSetImpl(
            Vector(
              "TABLE_CAT",
              "TABLE_SCHEM",
              "TABLE_NAME",
              "GRANTOR",
              "GRANTEE",
              "PRIVILEGE",
              "IS_GRANTABLE"
            ).map { value =>
              new ColumnDefinitionPacket:
                override def table:      String                     = ""
                override def name:       String                     = value
                override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_VARCHAR
                override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
            },
            records,
            serverVariables,
            protocol.initialPacket.serverVersion
          )
        }
      }

  override def getBestRowIdentifier(
    catalog:  Option[String],
    schema:   Option[String],
    table:    String,
    scope:    Option[Int],
    nullable: Option[Boolean]
  ): F[ResultSet] =

    val db = getDatabase(catalog, schema)

    val sqlBuf = new StringBuilder("SHOW COLUMNS FROM ")
    sqlBuf.append(table)

    db match
      case Some(dbValue) =>
        sqlBuf.append(" FROM ")
        sqlBuf.append(dbValue)
      case None => ()

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      preparedStatement.executeQuery().map { resultSet =>
        val builder = Vector.newBuilder[Option[(Int, String, Int, String, Int, Int, Int, Int)]]
        while resultSet.next() do
          resultSet.getString("Key") match
            case key if key.startsWith("PRI") =>
              val field  = resultSet.getString("Field")
              val `type` = resultSet.getString("Type")
              (Option(field), Option(`type`)) match
                case (Some(columnName), Some(value)) =>
                  val (size, decimals, typeName, hasLength) = parseTypeColumn(value)
                  val mysqlType                             = MysqlType.getByName(typeName.toUpperCase)
                  val dataType =
                    if mysqlType == MysqlType.YEAR && !yearIsDateType then SMALLINT
                    else mysqlType.jdbcType
                  val columnSize = if hasLength then size + decimals else mysqlType.precision.toInt
                  builder += Some(
                    (
                      DatabaseMetaData.bestRowSession,
                      columnName,
                      dataType,
                      typeName,
                      columnSize,
                      DatabaseMetaDataImpl.maxBufferSize,
                      decimals,
                      DatabaseMetaData.bestRowNotPseudo
                    )
                  )
                case _ => builder += None
            case _ => builder += None

        val decoded = builder.result()

        ResultSetImpl(
          Vector(
            "SCOPE",
            "COLUMN_NAME",
            "DATA_TYPE",
            "TYPE_NAME",
            "COLUMN_SIZE",
            "BUFFER_LENGTH",
            "DECIMAL_DIGITS",
            "PSEUDO_COLUMN"
          ).map { value =>
            new ColumnDefinitionPacket:
              override def table: String = ""

              override def name: String = value

              override def columnType: ColumnDataType = ColumnDataType.MYSQL_TYPE_VARCHAR

              override def flags: Seq[ColumnDefinitionFlags] = Seq.empty
          },
          decoded.flatten.map {
            case (scope, columnName, dataType, typeName, columnSize, bufferLength, decimalDigits, pseudoColumn) =>
              ResultSetRowPacket(
                Array(
                  Some(scope.toString),
                  Some(columnName),
                  Some(dataType.toString),
                  Some(typeName),
                  Some(columnSize.toString),
                  Some(bufferLength.toString),
                  Some(decimalDigits.toString),
                  Some(pseudoColumn.toString)
                )
              )
          },
          serverVariables,
          protocol.initialPacket.serverVersion
        )
      } <* preparedStatement.close()
    }

  override def getVersionColumns(catalog: Option[String], schema: Option[String], table: String): F[ResultSet] =

    val db = getDatabase(catalog, schema)

    val sqlBuf = new StringBuilder("SELECT NULL AS SCOPE, COLUMN_NAME, ")
    appendJdbcTypeMappingQuery(sqlBuf, "DATA_TYPE", "COLUMN_TYPE")
    sqlBuf.append(" AS DATA_TYPE, UPPER(COLUMN_TYPE) AS TYPE_NAME,")
    sqlBuf.append(" CASE WHEN LCASE(DATA_TYPE)='date' THEN 10")
    if protocol.initialPacket.serverVersion.compare(Version(5, 6, 4)) >= 0 then
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='time'")
      sqlBuf.append("  THEN 8+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)")
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp'")
      sqlBuf.append(
        "  THEN 19+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)"
      )
    else
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='time' THEN 8")
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp' THEN 19")

    sqlBuf.append(" WHEN CHARACTER_MAXIMUM_LENGTH IS NULL THEN NUMERIC_PRECISION WHEN CHARACTER_MAXIMUM_LENGTH > ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" THEN ")
    sqlBuf.append(Int.MaxValue)
    sqlBuf.append(" ELSE CHARACTER_MAXIMUM_LENGTH END AS COLUMN_SIZE, ")
    sqlBuf.append(DatabaseMetaDataImpl.maxBufferSize)
    sqlBuf.append(" AS BUFFER_LENGTH,NUMERIC_SCALE AS DECIMAL_DIGITS, ")
    sqlBuf.append(DatabaseMetaData.versionColumnNotPseudo)
    sqlBuf.append(" AS PSEUDO_COLUMN FROM INFORMATION_SCHEMA.COLUMNS WHERE")

    if db.nonEmpty then sqlBuf.append(" TABLE_SCHEMA = ? AND")

    sqlBuf.append(" TABLE_NAME = ?")
    sqlBuf.append(" AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%'")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = db match
        case Some(dbValue) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, table)
        case None =>
          preparedStatement.setString(1, table)

      setting *> preparedStatement.executeQuery()
    }

  override def getPrimaryKeys(catalog: Option[String], schema: Option[String], table: String): F[ResultSet] =

    val db = getDatabase(catalog, schema)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA  => "SELECT TABLE_CATALOG AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => "SELECT TABLE_SCHEMA AS TABLE_CAT, NULL AS TABLE_SCHEM,"
    )
    sqlBuf.append(
      " TABLE_NAME, COLUMN_NAME, SEQ_IN_INDEX AS KEY_SEQ, 'PRIMARY' AS PK_NAME FROM INFORMATION_SCHEMA.STATISTICS WHERE"
    )

    if db.nonEmpty then sqlBuf.append(" TABLE_SCHEMA = ? AND")

    sqlBuf.append(" TABLE_NAME = ?")
    sqlBuf.append(" AND INDEX_NAME='PRIMARY' ORDER BY TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, SEQ_IN_INDEX")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = db match
        case Some(dbValue) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, table)
        case None =>
          preparedStatement.setString(1, table)

      setting *> preparedStatement.executeQuery()
    }

  override def getImportedKeys(catalog: Option[String], schema: Option[String], table: String): F[ResultSet] =

    val db = getDatabase(catalog, schema)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA =>
          "SELECT DISTINCT A.CONSTRAINT_CATALOG AS PKTABLE_CAT, A.REFERENCED_TABLE_SCHEMA AS PKTABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG =>
          "SELECT DISTINCT A.REFERENCED_TABLE_SCHEMA AS PKTABLE_CAT,NULL AS PKTABLE_SCHEM,"
    )

    sqlBuf.append(" A.REFERENCED_TABLE_NAME AS PKTABLE_NAME, A.REFERENCED_COLUMN_NAME AS PKCOLUMN_NAME,")
    sqlBuf.append(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA => " A.TABLE_CATALOG AS FKTABLE_CAT, A.TABLE_SCHEMA AS FKTABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => " A.TABLE_SCHEMA AS FKTABLE_CAT, NULL AS FKTABLE_SCHEM,"
    )
    sqlBuf.append(" A.TABLE_NAME AS FKTABLE_NAME, A.COLUMN_NAME AS FKCOLUMN_NAME, A.ORDINAL_POSITION AS KEY_SEQ,")
    sqlBuf.append(generateUpdateRuleClause())
    sqlBuf.append(" AS UPDATE_RULE,")
    sqlBuf.append(generateDeleteRuleClause())
    sqlBuf.append(" AS DELETE_RULE, A.CONSTRAINT_NAME AS FK_NAME, R.UNIQUE_CONSTRAINT_NAME AS PK_NAME,")
    sqlBuf.append(DatabaseMetaData.importedKeyNotDeferrable)
    sqlBuf.append(" AS DEFERRABILITY FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE A")
    sqlBuf.append(" JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS B USING (CONSTRAINT_NAME, TABLE_NAME) ")
    sqlBuf.append(generateOptionalRefContraintsJoin())
    sqlBuf.append("WHERE B.CONSTRAINT_TYPE = 'FOREIGN KEY'")

    if db.nonEmpty then sqlBuf.append(" AND A.TABLE_SCHEMA = ?")

    sqlBuf.append(" AND A.TABLE_NAME=?")
    sqlBuf.append(" AND A.REFERENCED_TABLE_SCHEMA IS NOT NULL")
    sqlBuf.append(" ORDER BY A.REFERENCED_TABLE_SCHEMA, A.REFERENCED_TABLE_NAME, A.ORDINAL_POSITION")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = db match
        case Some(dbValue) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, table)
        case None =>
          preparedStatement.setString(1, table)

      setting *> preparedStatement.executeQuery()
    }

  override def getExportedKeys(catalog: Option[String], schema: Option[String], table: String): F[ResultSet] =

    val db = getDatabase(catalog, schema)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA =>
          "SELECT DISTINCT A.CONSTRAINT_CATALOG AS PKTABLE_CAT, A.REFERENCED_TABLE_SCHEMA AS PKTABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG =>
          "SELECT DISTINCT A.REFERENCED_TABLE_SCHEMA AS PKTABLE_CAT,NULL AS PKTABLE_SCHEM,"
    )
    sqlBuf.append(" A.REFERENCED_TABLE_NAME AS PKTABLE_NAME, A.REFERENCED_COLUMN_NAME AS PKCOLUMN_NAME,")
    sqlBuf.append(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA => " A.TABLE_CATALOG AS FKTABLE_CAT, A.TABLE_SCHEMA AS FKTABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => " A.TABLE_SCHEMA AS FKTABLE_CAT, NULL AS FKTABLE_SCHEM,"
    )
    sqlBuf.append(" A.TABLE_NAME AS FKTABLE_NAME, A.COLUMN_NAME AS FKCOLUMN_NAME, A.ORDINAL_POSITION AS KEY_SEQ,")
    sqlBuf.append(generateUpdateRuleClause())
    sqlBuf.append(" AS UPDATE_RULE,")
    sqlBuf.append(generateDeleteRuleClause())
    sqlBuf.append(" AS DELETE_RULE, A.CONSTRAINT_NAME AS FK_NAME, TC.CONSTRAINT_NAME AS PK_NAME,")
    sqlBuf.append(DatabaseMetaData.importedKeyNotDeferrable)
    sqlBuf.append(" AS DEFERRABILITY FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE A")
    sqlBuf.append(" JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS B USING (TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME) ")
    sqlBuf.append(generateOptionalRefContraintsJoin())
    sqlBuf.append(
      " LEFT JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS TC ON (A.REFERENCED_TABLE_SCHEMA = TC.TABLE_SCHEMA"
    )
    sqlBuf.append("  AND A.REFERENCED_TABLE_NAME = TC.TABLE_NAME")
    sqlBuf.append("  AND TC.CONSTRAINT_TYPE IN ('UNIQUE', 'PRIMARY KEY'))")
    sqlBuf.append(" WHERE B.CONSTRAINT_TYPE = 'FOREIGN KEY'")
    if db.nonEmpty then sqlBuf.append(" AND A.REFERENCED_TABLE_SCHEMA = ?")
    sqlBuf.append(" AND A.REFERENCED_TABLE_NAME=?")
    sqlBuf.append(" ORDER BY FKTABLE_NAME, FKTABLE_NAME, KEY_SEQ")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = db match
        case Some(dbValue) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, table)
        case None =>
          preparedStatement.setString(1, table)

      setting *> preparedStatement.executeQuery()
    }

  override def getCrossReference(
    parentCatalog:  Option[String],
    parentSchema:   Option[String],
    parentTable:    String,
    foreignCatalog: Option[String],
    foreignSchema:  Option[String],
    foreignTable:   Option[String]
  ): F[ResultSet] =

    val primaryDb = getDatabase(parentCatalog, parentSchema)
    val foreignDb = getDatabase(foreignCatalog, foreignSchema)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA =>
          "SELECT DISTINCT A.CONSTRAINT_CATALOG AS PKTABLE_CAT, A.REFERENCED_TABLE_SCHEMA AS PKTABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG =>
          "SELECT DISTINCT A.REFERENCED_TABLE_SCHEMA AS PKTABLE_CAT,NULL AS PKTABLE_SCHEM,"
    )
    sqlBuf.append(" A.REFERENCED_TABLE_NAME AS PKTABLE_NAME, A.REFERENCED_COLUMN_NAME AS PKCOLUMN_NAME,")
    sqlBuf.append(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA => " A.TABLE_CATALOG AS FKTABLE_CAT, A.TABLE_SCHEMA AS FKTABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => " A.TABLE_SCHEMA AS FKTABLE_CAT, NULL AS FKTABLE_SCHEM,"
    )
    sqlBuf.append(" A.TABLE_NAME AS FKTABLE_NAME, A.COLUMN_NAME AS FKCOLUMN_NAME, A.ORDINAL_POSITION AS KEY_SEQ,")
    sqlBuf.append(generateUpdateRuleClause())
    sqlBuf.append(" AS UPDATE_RULE,")
    sqlBuf.append(generateDeleteRuleClause())
    sqlBuf.append(" AS DELETE_RULE, A.CONSTRAINT_NAME AS FK_NAME, TC.CONSTRAINT_NAME AS PK_NAME,")
    sqlBuf.append(DatabaseMetaData.importedKeyNotDeferrable)
    sqlBuf.append(" AS DEFERRABILITY FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE A")
    sqlBuf.append(" JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS B USING (TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME) ")
    sqlBuf.append(generateOptionalRefContraintsJoin())
    sqlBuf.append(
      " LEFT JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS TC ON (A.REFERENCED_TABLE_SCHEMA = TC.TABLE_SCHEMA"
    )
    sqlBuf.append("  AND A.REFERENCED_TABLE_NAME = TC.TABLE_NAME")
    sqlBuf.append("  AND TC.CONSTRAINT_TYPE IN ('UNIQUE', 'PRIMARY KEY'))")
    sqlBuf.append("WHERE B.CONSTRAINT_TYPE = 'FOREIGN KEY'")

    if primaryDb.nonEmpty then sqlBuf.append(" AND A.REFERENCED_TABLE_SCHEMA=?")
    sqlBuf.append(" AND A.REFERENCED_TABLE_NAME=?")
    if foreignDb.nonEmpty then sqlBuf.append(" AND A.TABLE_SCHEMA = ?")
    sqlBuf.append(" AND A.TABLE_NAME=?")
    sqlBuf.append(" ORDER BY FKTABLE_NAME, FKTABLE_NAME, KEY_SEQ")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = (primaryDb, foreignDb, foreignTable) match
        case (Some(primaryDbValue), Some(foreignDbValue), Some(foreignTableValue)) =>
          preparedStatement.setString(1, primaryDbValue) *> preparedStatement.setString(
            2,
            parentTable
          ) *> preparedStatement.setString(3, foreignDbValue) *> preparedStatement.setString(
            4,
            foreignTableValue
          )
        case (Some(primaryDbValue), None, Some(foreignTableValue)) =>
          preparedStatement.setString(1, primaryDbValue) *> preparedStatement.setString(
            2,
            parentTable
          ) *> preparedStatement.setString(3, foreignTableValue)
        case (None, Some(foreignDbValue), Some(foreignTableValue)) =>
          preparedStatement.setString(1, parentTable) *> preparedStatement.setString(
            2,
            foreignDbValue
          ) *> preparedStatement.setString(3, foreignTableValue)
        case (None, None, Some(foreignTableValue)) =>
          preparedStatement.setString(1, parentTable) *> preparedStatement.setString(2, foreignTableValue)
        case (Some(primaryDbValue), Some(foreignDbValue), None) =>
          preparedStatement.setString(1, primaryDbValue) *> preparedStatement.setString(
            2,
            parentTable
          ) *> preparedStatement.setString(3, foreignDbValue)
        case (Some(primaryDbValue), None, None) =>
          preparedStatement.setString(1, primaryDbValue) *> preparedStatement.setString(2, parentTable)
        case (None, Some(foreignDbValue), None) =>
          preparedStatement.setString(1, parentTable) *> preparedStatement.setString(2, foreignDbValue)
        case (None, None, None) =>
          preparedStatement.setString(1, parentTable)

      setting *> preparedStatement.executeQuery()
    }

  override def getTypeInfo(): ResultSet =
    val types = Vector(
      ResultSetRowPacket(getTypeInfo("BIT")),
      ResultSetRowPacket(getTypeInfo("TINYINT")),
      ResultSetRowPacket(getTypeInfo("TINYINT UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("BIGINT")),
      ResultSetRowPacket(getTypeInfo("BIGINT UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("LONG VARBINARY")),
      ResultSetRowPacket(getTypeInfo("MEDIUMBLOB")),
      ResultSetRowPacket(getTypeInfo("LONGBLOB")),
      ResultSetRowPacket(getTypeInfo("BLOB")),
      ResultSetRowPacket(getTypeInfo("VECTOR")),
      ResultSetRowPacket(getTypeInfo("VARBINARY")),
      ResultSetRowPacket(getTypeInfo("TINYBLOB")),
      ResultSetRowPacket(getTypeInfo("BINARY")),
      ResultSetRowPacket(getTypeInfo("LONG VARCHAR")),
      ResultSetRowPacket(getTypeInfo("MEDIUMTEXT")),
      ResultSetRowPacket(getTypeInfo("LONGTEXT")),
      ResultSetRowPacket(getTypeInfo("TEXT")),
      ResultSetRowPacket(getTypeInfo("CHAR")),
      ResultSetRowPacket(getTypeInfo("ENUM")),
      ResultSetRowPacket(getTypeInfo("SET")),
      ResultSetRowPacket(getTypeInfo("DECIMAL")),
      ResultSetRowPacket(getTypeInfo("NUMERIC")),
      ResultSetRowPacket(getTypeInfo("INTEGER")),
      ResultSetRowPacket(getTypeInfo("INT")),
      ResultSetRowPacket(getTypeInfo("MEDIUMINT")),
      ResultSetRowPacket(getTypeInfo("INTEGER UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("INT UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("MEDIUMINT UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("SMALLINT")),
      ResultSetRowPacket(getTypeInfo("SMALLINT UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("FLOAT")),
      ResultSetRowPacket(getTypeInfo("DOUBLE")),
      ResultSetRowPacket(getTypeInfo("DOUBLE PRECISION")),
      ResultSetRowPacket(getTypeInfo("REAL")),
      ResultSetRowPacket(getTypeInfo("DOUBLE UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("DOUBLE PRECISION UNSIGNED")),
      ResultSetRowPacket(getTypeInfo("VARCHAR")),
      ResultSetRowPacket(getTypeInfo("TINYTEXT")),
      ResultSetRowPacket(getTypeInfo("BOOL")),
      ResultSetRowPacket(getTypeInfo("DATE")),
      ResultSetRowPacket(getTypeInfo("YEAR")),
      ResultSetRowPacket(getTypeInfo("TIME")),
      ResultSetRowPacket(getTypeInfo("DATETIME")),
      ResultSetRowPacket(getTypeInfo("TIMESTAMP"))
    )

    ResultSetImpl(
      Vector(
        "TYPE_NAME",
        "DATA_TYPE",
        "PRECISION",
        "LITERAL_PREFIX",
        "LITERAL_SUFFIX",
        "CREATE_PARAMS",
        "NULLABLE",
        "CASE_SENSITIVE",
        "SEARCHABLE",
        "UNSIGNED_ATTRIBUTE",
        "FIXED_PREC_SCALE",
        "AUTO_INCREMENT",
        "LOCAL_TYPE_NAME",
        "MINIMUM_SCALE",
        "MAXIMUM_SCALE",
        "SQL_DATA_TYPE",
        "SQL_DATETIME_SUB",
        "NUM_PREC_RADIX"
      ).map { value =>
        new ColumnDefinitionPacket:
          override def table: String = ""

          override def name: String = value

          override def columnType: ColumnDataType = ColumnDataType.MYSQL_TYPE_VARCHAR

          override def flags: Seq[ColumnDefinitionFlags] = Seq.empty
      },
      types,
      serverVariables,
      protocol.initialPacket.serverVersion
    )

  override def getIndexInfo(
    catalog:     Option[String],
    schema:      Option[String],
    table:       Option[String],
    unique:      Boolean,
    approximate: Boolean
  ): F[ResultSet] =

    val db = getDatabase(catalog, schema)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA  => "SELECT TABLE_CATALOG AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => "SELECT TABLE_SCHEMA AS TABLE_CAT, NULL AS TABLE_SCHEM,"
    )
    sqlBuf.append(" TABLE_NAME, NON_UNIQUE, NULL AS INDEX_QUALIFIER, INDEX_NAME,")
    sqlBuf.append(DatabaseMetaData.tableIndexOther)
    sqlBuf.append(" AS TYPE, SEQ_IN_INDEX AS ORDINAL_POSITION, COLUMN_NAME,")
    sqlBuf.append(
      "COLLATION AS ASC_OR_DESC, CARDINALITY, 0 AS PAGES, NULL AS FILTER_CONDITION FROM INFORMATION_SCHEMA.STATISTICS WHERE"
    )
    if db.nonEmpty then sqlBuf.append(" TABLE_SCHEMA = ? AND")
    sqlBuf.append(" TABLE_NAME = ?")

    if unique then sqlBuf.append(" AND NON_UNIQUE=0 ")
    sqlBuf.append(" ORDER BY NON_UNIQUE, INDEX_NAME, SEQ_IN_INDEX")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = (db, table) match
        case (Some(dbValue), Some(tableName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, tableName)
        case (Some(dbValue), None)   => preparedStatement.setString(1, dbValue)
        case (None, Some(tableName)) => preparedStatement.setString(1, tableName)
        case (None, None)            => ev.unit

      setting *> preparedStatement.executeQuery()
    }

  override def getUDTs(
    catalog:         Option[String],
    schemaPattern:   Option[String],
    typeNamePattern: Option[String],
    types:           Array[Int]
  ): ResultSet =
    emptyResultSet(
      Vector(
        "TYPE_CAT",
        "TYPE_SCHEM",
        "TYPE_NAME",
        "CLASS_NAME",
        "DATA_TYPE",
        "REMARKS",
        "BASE_TYPE"
      )
    )

  override def getConnection(): Connection[F] = throw new SQLFeatureNotSupportedException(
    "Connections should be available that generate this DatabaseMetaData."
  )

  override def getSuperTypes(
    catalog:         Option[String],
    schemaPattern:   Option[String],
    typeNamePattern: Option[String]
  ): ResultSet =
    emptyResultSet(
      Vector(
        "TYPE_CAT",
        "TYPE_SCHEM",
        "TYPE_NAME",
        "SUPERTYPE_CAT",
        "SUPERTYPE_SCHEM",
        "SUPERTYPE_NAME"
      )
    )

  override def getSuperTables(
    catalog:          Option[String],
    schemaPattern:    Option[String],
    tableNamePattern: Option[String]
  ): ResultSet =
    emptyResultSet(
      Vector(
        "TYPE_CAT",
        "TYPE_SCHEM",
        "TYPE_NAME",
        "SUPERTABLE_NAME"
      )
    )

  override def getAttributes(
    catalog:              Option[String],
    schemaPattern:        Option[String],
    typeNamePattern:      Option[String],
    attributeNamePattern: Option[String]
  ): ResultSet =
    emptyResultSet(
      Vector(
        "TYPE_CAT",
        "TYPE_SCHEM",
        "TYPE_NAME",
        "ATTR_NAME",
        "DATA_TYPE",
        "ATTR_TYPE_NAME",
        "ATTR_SIZE",
        "DECIMAL_DIGITS",
        "NUM_PREC_RADIX",
        "NULLABLE",
        "REMARKS",
        "ATTR_DEF",
        "SQL_DATA_TYPE",
        "SQL_DATETIME_SUB",
        "CHAR_OCTET_LENGTH",
        "ORDINAL_POSITION",
        "IS_NULLABLE",
        "SCOPE_CATALOG",
        "SCOPE_SCHEMA",
        "SCOPE_TABLE",
        "SOURCE_DATA_TYPE"
      )
    )

  override def getDatabaseMajorVersion(): Int = protocol.initialPacket.serverVersion.major

  override def getDatabaseMinorVersion(): Int = protocol.initialPacket.serverVersion.minor

  override def getSchemas(catalog: Option[String], schemaPattern: Option[String]): F[ResultSet] =
    (if databaseTerm == DatabaseMetaData.DatabaseTerm.SCHEMA then getDatabases(schemaPattern)
     else ev.pure(List.empty[String])).map { dbList =>
      ResultSetImpl(
        Vector("TABLE_CATALOG", "TABLE_SCHEM").map { value =>
          new ColumnDefinitionPacket:
            override def table:      String                     = ""
            override def name:       String                     = value
            override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_VARCHAR
            override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
        },
        dbList.map(name => ResultSetRowPacket(Array(Some("def"), Some(name)))).toVector,
        serverVariables,
        protocol.initialPacket.serverVersion
      )
    }

  override def getClientInfoProperties(): ResultSet =
    emptyResultSet(
      Vector(
        "NAME",
        "MAX_LEN",
        "DEFAULT_VALUE",
        "DESCRIPTION"
      )
    )

  override def getFunctions(
    catalog:             Option[String],
    schemaPattern:       Option[String],
    functionNamePattern: Option[String]
  ): F[ResultSet] =

    val db = getDatabase(catalog, schemaPattern)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA =>
          "SELECT ROUTINE_CATALOG AS FUNCTION_CAT, ROUTINE_SCHEMA AS FUNCTION_SCHEM,"
        case DatabaseMetaData.DatabaseTerm.CATALOG => "SELECT ROUTINE_SCHEMA AS FUNCTION_CAT, NULL AS FUNCTION_SCHEM,"
    )
    sqlBuf.append(" ROUTINE_NAME AS FUNCTION_NAME, ROUTINE_COMMENT AS REMARKS, ")
    sqlBuf.append(DatabaseMetaData.functionNoTable)
    sqlBuf.append(" AS FUNCTION_TYPE, ROUTINE_NAME AS SPECIFIC_NAME FROM INFORMATION_SCHEMA.ROUTINES")
    sqlBuf.append(" WHERE ROUTINE_TYPE LIKE 'FUNCTION'")
    if db.nonEmpty then
      sqlBuf.append(
        databaseTerm match
          case DatabaseMetaData.DatabaseTerm.SCHEMA  => " AND ROUTINE_SCHEMA LIKE ?"
          case DatabaseMetaData.DatabaseTerm.CATALOG => " AND ROUTINE_SCHEMA = ?"
      )

    if functionNamePattern.nonEmpty then sqlBuf.append(" AND ROUTINE_NAME LIKE ?")

    sqlBuf.append(" ORDER BY FUNCTION_CAT, FUNCTION_SCHEM, FUNCTION_NAME, SPECIFIC_NAME")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = (db, functionNamePattern) match
        case (Some(dbValue), Some(functionName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, functionName)
        case (Some(dbValue), None)      => preparedStatement.setString(1, dbValue)
        case (None, Some(functionName)) => preparedStatement.setString(1, functionName)
        case (None, None)               => ev.unit

      setting *> preparedStatement.executeQuery()
    }

  override def getFunctionColumns(
    catalog:             Option[String],
    schemaPattern:       Option[String],
    functionNamePattern: Option[String],
    columnNamePattern:   Option[String]
  ): F[ResultSet] =

    val supportsFractSeconds = protocol.initialPacket.serverVersion.compare(Version(5, 6, 4)) >= 0

    val db = getDatabase(catalog, schemaPattern)

    val sqlBuf = new StringBuilder(
      databaseTerm match
        case DatabaseMetaData.DatabaseTerm.SCHEMA =>
          "SELECT SPECIFIC_CATALOG AS FUNCTION_CAT, SPECIFIC_SCHEMA AS `FUNCTION_SCHEM`,"
        case DatabaseMetaData.DatabaseTerm.CATALOG =>
          "SELECT SPECIFIC_SCHEMA AS FUNCTION_CAT, NULL AS `FUNCTION_SCHEM`,"
    )
    sqlBuf.append(
      " SPECIFIC_NAME AS `FUNCTION_NAME`, IFNULL(PARAMETER_NAME, '') AS `COLUMN_NAME`, CASE WHEN PARAMETER_MODE = 'IN' THEN "
    )
    sqlBuf.append(getFunctionConstant(FunctionConstant.FUNCTION_COLUMN_IN))
    sqlBuf.append(" WHEN PARAMETER_MODE = 'OUT' THEN ")
    sqlBuf.append(getFunctionConstant(FunctionConstant.FUNCTION_COLUMN_OUT))
    sqlBuf.append(" WHEN PARAMETER_MODE = 'INOUT' THEN ")
    sqlBuf.append(getFunctionConstant(FunctionConstant.FUNCTION_COLUMN_INOUT))
    sqlBuf.append(" WHEN ORDINAL_POSITION = 0 THEN ")
    sqlBuf.append(getFunctionConstant(FunctionConstant.FUNCTION_COLUMN_RETURN))
    sqlBuf.append(" ELSE ")
    sqlBuf.append(getFunctionConstant(FunctionConstant.FUNCTION_COLUMN_UNKNOWN))
    sqlBuf.append(" END AS `COLUMN_TYPE`, ")
    appendJdbcTypeMappingQuery(sqlBuf, "DATA_TYPE", "DTD_IDENTIFIER")
    sqlBuf.append(" AS `DATA_TYPE`, ")
    sqlBuf.append("UPPER(CASE")

    if tinyInt1isBit then
      sqlBuf.append(" WHEN UPPER(DATA_TYPE)='TINYINT' THEN CASE")
      sqlBuf.append(
        " WHEN LOCATE('ZEROFILL', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('(1)', DTD_IDENTIFIER) != 0 THEN "
      )
      sqlBuf.append(if transformedBitIsBoolean then "'BOOLEAN'" else "'BIT'")
      sqlBuf.append(
        " WHEN LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 AND LOCATE('UNSIGNED', UPPER(DATA_TYPE)) = 0 THEN 'TINYINT UNSIGNED'"
      )
      sqlBuf.append(" ELSE DATA_TYPE END ")

    sqlBuf.append(
      " WHEN LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 AND LOCATE('UNSIGNED', UPPER(DATA_TYPE)) = 0 AND LOCATE('SET', UPPER(DATA_TYPE)) <> 1 AND LOCATE('ENUM', UPPER(DATA_TYPE)) <> 1 THEN CONCAT(DATA_TYPE, ' UNSIGNED')"
    )

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POINT' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='LINESTRING' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='POLYGON' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOINT' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTILINESTRING' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOLYGON' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMETRYCOLLECTION' THEN 'GEOMETRY'")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='GEOMCOLLECTION' THEN 'GEOMETRY'")

    sqlBuf.append(" ELSE UPPER(DATA_TYPE) END) AS TYPE_NAME,")

    sqlBuf.append(" CASE WHEN LCASE(DATA_TYPE)='date' THEN 0")

    if supportsFractSeconds then
      sqlBuf.append(
        " WHEN LCASE(DATA_TYPE)='time' OR LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp' THEN DATETIME_PRECISION"
      )
    else
      sqlBuf.append(
        " WHEN LCASE(DATA_TYPE)='time' OR LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp' THEN 0"
      )

    if tinyInt1isBit && !transformedBitIsBoolean then
      sqlBuf.append(
        " WHEN UPPER(DATA_TYPE)='TINYINT' AND LOCATE('ZEROFILL', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('(1)', DTD_IDENTIFIER) != 0 THEN 1"
      )

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MEDIUMINT' AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 THEN 8")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='JSON' THEN 1073741824")
    sqlBuf.append(" ELSE NUMERIC_PRECISION END AS `PRECISION`,")

    sqlBuf.append(" CASE WHEN LCASE(DATA_TYPE)='date' THEN 10")

    if supportsFractSeconds then
      sqlBuf.append(
        " WHEN LCASE(DATA_TYPE)='time' THEN 8+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)"
      )
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp'")
      sqlBuf.append(
        "  THEN 19+(CASE WHEN DATETIME_PRECISION>0 THEN DATETIME_PRECISION+1 ELSE DATETIME_PRECISION END)"
      )
    else
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='time' THEN 8")
      sqlBuf.append(" WHEN LCASE(DATA_TYPE)='datetime' OR LCASE(DATA_TYPE)='timestamp' THEN 19")

    if tinyInt1isBit && !transformedBitIsBoolean then
      sqlBuf.append(
        " WHEN (UPPER(DATA_TYPE)='TINYINT' OR UPPER(DATA_TYPE)='TINYINT UNSIGNED') AND LOCATE('ZEROFILL', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) = 0 AND LOCATE('(1)', DTD_IDENTIFIER) != 0 THEN 1"
      )

    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='MEDIUMINT' AND LOCATE('UNSIGNED', UPPER(DTD_IDENTIFIER)) != 0 THEN 8")
    sqlBuf.append(" WHEN UPPER(DATA_TYPE)='JSON' THEN 1073741824")
    sqlBuf.append(" WHEN CHARACTER_MAXIMUM_LENGTH IS NULL THEN NUMERIC_PRECISION")
    sqlBuf.append(" WHEN CHARACTER_MAXIMUM_LENGTH > " + Int.MaxValue + " THEN " + Int.MaxValue)
    sqlBuf.append(" ELSE CHARACTER_MAXIMUM_LENGTH END AS LENGTH, ")

    sqlBuf.append("NUMERIC_SCALE AS `SCALE`, 10 AS RADIX, ")
    sqlBuf.append(getFunctionConstant(FunctionConstant.FUNCTION_NULLABLE))
    sqlBuf.append(" AS `NULLABLE`,  NULL AS `REMARKS`,")

    sqlBuf.append(" CASE WHEN CHARACTER_OCTET_LENGTH > ")
    sqlBuf.append(Integer.MAX_VALUE)
    sqlBuf.append(" THEN ")
    sqlBuf.append(Integer.MAX_VALUE)
    sqlBuf.append(" ELSE CHARACTER_OCTET_LENGTH END AS `CHAR_OCTET_LENGTH`,")

    sqlBuf.append(" ORDINAL_POSITION, 'YES' AS `IS_NULLABLE`,")
    sqlBuf.append(" SPECIFIC_NAME FROM INFORMATION_SCHEMA.PARAMETERS WHERE")

    val conditionBuf = new StringBuilder()

    if db.nonEmpty then
      conditionBuf.append(
        databaseTerm match
          case DatabaseMetaData.DatabaseTerm.SCHEMA  => " SPECIFIC_SCHEMA LIKE ?"
          case DatabaseMetaData.DatabaseTerm.CATALOG => " SPECIFIC_SCHEMA = ?"
      )

    if functionNamePattern.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      conditionBuf.append(" SPECIFIC_NAME LIKE ?")

    if columnNamePattern.nonEmpty then
      if conditionBuf.nonEmpty then conditionBuf.append(" AND")
      conditionBuf.append(" (PARAMETER_NAME LIKE ? OR PARAMETER_NAME IS NULL)")

    if conditionBuf.nonEmpty then conditionBuf.append(" AND")
    conditionBuf.append(" ROUTINE_TYPE='FUNCTION'")

    sqlBuf.append(conditionBuf)
    sqlBuf.append(" ORDER BY SPECIFIC_SCHEMA, SPECIFIC_NAME, ORDINAL_POSITION")

    prepareMetaDataSafeStatement(sqlBuf.toString()).flatMap { preparedStatement =>
      val setting = (db, functionNamePattern, columnNamePattern) match
        case (Some(dbValue), Some(functionName), Some(columnName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, functionName) *> preparedStatement
            .setString(
              3,
              columnName
            )
        case (Some(dbValue), Some(functionName), None) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, functionName)
        case (Some(dbValue), None, Some(columnName)) =>
          preparedStatement.setString(1, dbValue) *> preparedStatement.setString(2, columnName)
        case (Some(dbValue), None, None) => preparedStatement.setString(1, dbValue)
        case (None, Some(functionName), Some(columnName)) =>
          preparedStatement.setString(1, functionName) *> preparedStatement.setString(2, columnName)
        case (None, Some(functionName), None) => preparedStatement.setString(1, functionName)
        case (None, None, Some(columnName))   => preparedStatement.setString(1, columnName)
        case (None, None, None)               => ev.unit

      setting *> preparedStatement.executeQuery()
    }

  override def getPseudoColumns(
    catalog:           Option[String],
    schemaPattern:     Option[String],
    tableNamePattern:  Option[String],
    columnNamePattern: Option[String]
  ): ResultSet =
    emptyResultSet(
      Vector(
        "TABLE_CAT",
        "TABLE_SCHEM",
        "TABLE_NAME",
        "COLUMN_NAME",
        "DATA_TYPE",
        "COLUMN_SIZE",
        "DECIMAL_DIGITS",
        "NUM_PREC_RADIX",
        "COLUMN_USAGE",
        "REMARKS",
        "CHAR_OCTET_LENGTH",
        "IS_NULLABLE"
      )
    )

  protected def getDatabase(catalog: Option[String], schema: Option[String]): Option[String] =
    databaseTerm match
      case DatabaseMetaData.DatabaseTerm.SCHEMA =>
        if schema.nonEmpty && nullDatabaseMeansCurrent then database else schema
      case DatabaseMetaData.DatabaseTerm.CATALOG =>
        if catalog.nonEmpty && nullDatabaseMeansCurrent then database else catalog

  /**
   * Get a prepared statement to query information_schema tables.
   *
   * @param sql
   * query
   * @return PreparedStatement
   */
  protected def prepareMetaDataSafeStatement(sql: String): F[PreparedStatement[F]] =
    for
      params            <- Ref[F].of(SortedMap.empty[Int, Parameter])
      batchedArgs       <- Ref[F].of(Vector.empty[String])
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
      ResultSet.TYPE_SCROLL_INSENSITIVE,
      ResultSet.CONCUR_READ_ONLY
    )

  private def appendJdbcTypeMappingQuery(
    buf:                     StringBuilder,
    mysqlTypeColumnName:     String,
    fullMysqlTypeColumnName: String
  ): Unit =
    buf.append("CASE ")

    MysqlType.values.foreach { mysqlType =>
      buf.append(" WHEN UPPER(")
      buf.append(mysqlTypeColumnName)
      buf.append(")='")
      buf.append(mysqlType.getName())
      buf.append("' THEN ")

      mysqlType match
        case MysqlType.TINYINT | MysqlType.TINYINT_UNSIGNED =>
          if tinyInt1isBit then
            buf.append("CASE")
            buf.append(" WHEN LOCATE('ZEROFILL', UPPER(")
            buf.append(fullMysqlTypeColumnName)
            buf.append(")) = 0 AND LOCATE('UNSIGNED', UPPER(")
            buf.append(fullMysqlTypeColumnName)
            buf.append(")) = 0 AND LOCATE('(1)', ")
            buf.append(fullMysqlTypeColumnName)
            buf.append(") != 0 THEN ")
            buf.append(if transformedBitIsBoolean then "16" else "-7")
            buf.append(" ELSE -6 END ")
          else buf.append(mysqlType.jdbcType)
        case MysqlType.YEAR => buf.append(if yearIsDateType then mysqlType.jdbcType else SMALLINT)
        case _              => buf.append(mysqlType.jdbcType)
    }

    buf.append(" WHEN UPPER(DATA_TYPE)='POINT' THEN -2")

    buf.append(" WHEN UPPER(DATA_TYPE)='LINESTRING' THEN -2")
    buf.append(" WHEN UPPER(DATA_TYPE)='POLYGON' THEN -2")
    buf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOINT' THEN -2")
    buf.append(" WHEN UPPER(DATA_TYPE)='MULTILINESTRING' THEN -2")
    buf.append(" WHEN UPPER(DATA_TYPE)='MULTIPOLYGON' THEN -2")
    buf.append(" WHEN UPPER(DATA_TYPE)='GEOMETRYCOLLECTION' THEN -2")
    buf.append(" WHEN UPPER(DATA_TYPE)='GEOMCOLLECTION' THEN -2")

    buf.append(" ELSE 1111")
    buf.append(" END ")

  private def getDatabases(dbPattern: Option[String]): F[List[String]] =

    val sqlBuf = new StringBuilder("SHOW DATABASES")

    dbPattern.foreach { pattern =>
      sqlBuf.append(" LIKE '")
      sqlBuf.append(pattern)
      sqlBuf.append("'")
    }

    for
      prepareStatement <- prepareMetaDataSafeStatement(sqlBuf.toString)
      resultSet        <- prepareStatement.executeQuery()
    yield
      val builder = List.newBuilder[String]
      while resultSet.next() do builder += resultSet.getString(1)
      builder.result()

  private def parseTypeColumn(`type`: String): (Int, Int, String, Boolean) =
    if `type`.indexOf("enum") != -1 then
      val temp      = `type`.substring(`type`.indexOf("(") + 1, `type`.indexOf(")"))
      val maxLength = temp.split(",").map(_.length - 2).max

      (maxLength, 0, "enum", false)
    else if `type`.indexOf("(") != -1 then
      val name = `type`.substring(0, `type`.indexOf("("))
      if `type`.indexOf(",") != -1 then
        val size     = `type`.substring(`type`.indexOf("(") + 1, `type`.indexOf(",")).toInt
        val decimals = `type`.substring(`type`.indexOf(",") + 1, `type`.indexOf(")")).toInt
        (size, decimals, name, true)
      else
        val size = `type`.substring(`type`.indexOf("(") + 1, `type`.indexOf(",")).toInt
        (size, 0, name, true)
    else (10, 0, `type`, false)

  private def generateUpdateRuleClause(): String =
    "CASE WHEN R.UPDATE_RULE='CASCADE' THEN " + DatabaseMetaData.importedKeyCascade + " WHEN R.UPDATE_RULE='SET NULL' THEN "
      + DatabaseMetaData.importedKeySetNull + " WHEN R.UPDATE_RULE='SET DEFAULT' THEN " + DatabaseMetaData.importedKeySetDefault
      + " WHEN R.UPDATE_RULE='RESTRICT' THEN " + DatabaseMetaData.importedKeyRestrict + " WHEN R.UPDATE_RULE='NO ACTION' THEN "
      + DatabaseMetaData.importedKeyRestrict + " ELSE " + DatabaseMetaData.importedKeyRestrict + " END "

  private def generateDeleteRuleClause(): String =
    "CASE WHEN R.DELETE_RULE='CASCADE' THEN " + DatabaseMetaData.importedKeyCascade + " WHEN R.DELETE_RULE='SET NULL' THEN "
      + DatabaseMetaData.importedKeySetNull + " WHEN R.DELETE_RULE='SET DEFAULT' THEN " + DatabaseMetaData.importedKeySetDefault
      + " WHEN R.DELETE_RULE='RESTRICT' THEN " + DatabaseMetaData.importedKeyRestrict + " WHEN R.DELETE_RULE='NO ACTION' THEN "
      + DatabaseMetaData.importedKeyRestrict + " ELSE " + DatabaseMetaData.importedKeyRestrict + " END "

  private def generateOptionalRefContraintsJoin(): String =
    "JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS R ON (R.CONSTRAINT_NAME = B.CONSTRAINT_NAME "
      + "AND R.TABLE_NAME = B.TABLE_NAME AND R.CONSTRAINT_SCHEMA = B.TABLE_SCHEMA) "

  private def getTypeInfo(mysqlTypeName: String): Array[Option[String]] =
    val mysqlType = MysqlType.getByName(mysqlTypeName)

    Array(
      Some(mysqlTypeName), // TYPE_NAME
      if mysqlType == MysqlType.YEAR && !yearIsDateType then Some(SMALLINT.toString)
      else Some(mysqlType.jdbcType.toString), // DATA_TYPE
      if mysqlType.precision > Int.MaxValue then Some(Int.MaxValue.toString)
      else Some(mysqlType.precision.toString) // PRECISION
    ) ++ (
      // LITERAL_PREFIX, LITERAL_SUFFIX
      mysqlType match
        case MysqlType.TINYBLOB | MysqlType.BLOB | MysqlType.MEDIUMBLOB | MysqlType.LONGBLOB | MysqlType.TINYTEXT |
          MysqlType.TEXT | MysqlType.MEDIUMTEXT | MysqlType.LONGTEXT | MysqlType.JSON | MysqlType.BINARY |
          MysqlType.VARBINARY | MysqlType.CHAR | MysqlType.VARCHAR | MysqlType.ENUM | MysqlType.SET | MysqlType.DATE |
          MysqlType.TIME | MysqlType.DATETIME | MysqlType.TIMESTAMP | MysqlType.GEOMETRY | MysqlType.VECTOR |
          MysqlType.UNKNOWN =>
          Array(Some("'"), Some("'"))
        case _ => Array(Some(""), Some(""))
    ) ++ Array(
      Some(mysqlType.createParams),                   // CREATE_PARAMS
      Some(DatabaseMetaData.typeNullable.toString),   // NULLABLE
      Some("true"),                                   // CASE_SENSITIVE
      Some(DatabaseMetaData.typeSearchable.toString), // SEARCHABLE
      if (mysqlType.allowedFlags & MysqlTypeVariables.FIELD_FLAG_UNSIGNED) > 0 then Some("true")
      else Some("false"), // UNSIGNED_ATTRIBUTE
      Some("false"),      // FIXED_PREC_SCALE
      (
        mysqlType match
          case MysqlType.BIGINT | MysqlType.BIGINT_UNSIGNED | MysqlType.BOOLEAN | MysqlType.INT |
            MysqlType.INT_UNSIGNED | MysqlType.MEDIUMINT | MysqlType.MEDIUMINT_UNSIGNED | MysqlType.SMALLINT |
            MysqlType.SMALLINT_UNSIGNED | MysqlType.TINYINT | MysqlType.TINYINT_UNSIGNED =>
            Some("true")
          case MysqlType.DOUBLE | MysqlType.DOUBLE_UNSIGNED | MysqlType.FLOAT | MysqlType.FLOAT_UNSIGNED =>
            val supportsAutoIncrement = protocol.initialPacket.serverVersion.compare(Version(8, 4, 0)) >= 0
            if supportsAutoIncrement then Some("true") else Some("false")
          case _ => Some("false")
      ),                   // AUTO_INCREMENT
      Some(mysqlType.name) // LOCAL_TYPE_NAME
    ) ++ (
      // MINIMUM_SCALE, MAXIMUM_SCALE
      mysqlType match
        case MysqlType.DECIMAL | MysqlType.DECIMAL_UNSIGNED | MysqlType.DOUBLE | MysqlType.DOUBLE_UNSIGNED =>
          Array(Some("-308"), Some("308"))
        case MysqlType.FLOAT | MysqlType.FLOAT_UNSIGNED =>
          Array(Some("-38"), Some("38"))
        case _ => Array(Some("0"), Some("0"))
    ) ++ Array(
      Some("0"), // SQL_DATA_TYPE
      Some("0"), // SQL_DATETIME_SUB
      Some("10") // NUM_PREC_RADIX
    )

  private def emptyResultSet(fields: Vector[String]): ResultSet =
    ResultSetImpl(
      fields.map { value =>
        new ColumnDefinitionPacket:
          override def table:      String                     = ""
          override def name:       String                     = value
          override def columnType: ColumnDataType             = ColumnDataType.MYSQL_TYPE_VARCHAR
          override def flags:      Seq[ColumnDefinitionFlags] = Seq.empty
      },
      Vector.empty,
      serverVariables,
      protocol.initialPacket.serverVersion
    )

  private def getFunctionConstant(constant: FunctionConstant): Int =
    constant match
      case FunctionConstant.FUNCTION_COLUMN_IN        => DatabaseMetaData.functionColumnIn
      case FunctionConstant.FUNCTION_COLUMN_OUT       => DatabaseMetaData.functionColumnOut
      case FunctionConstant.FUNCTION_COLUMN_INOUT     => DatabaseMetaData.functionColumnInOut
      case FunctionConstant.FUNCTION_COLUMN_RETURN    => DatabaseMetaData.functionReturn
      case FunctionConstant.FUNCTION_COLUMN_RESULT    => DatabaseMetaData.functionColumnResult
      case FunctionConstant.FUNCTION_COLUMN_UNKNOWN   => DatabaseMetaData.functionColumnUnknown
      case FunctionConstant.FUNCTION_NO_NULLS         => DatabaseMetaData.functionNoNulls
      case FunctionConstant.FUNCTION_NULLABLE         => DatabaseMetaData.functionNullable
      case FunctionConstant.FUNCTION_NULLABLE_UNKNOWN => DatabaseMetaData.functionNullableUnknown

private[ldbc] object DatabaseMetaDataImpl:

  private val SQL2003_KEYWORDS = List(
    "ABS",
    "ALL",
    "ALLOCATE",
    "ALTER",
    "AND",
    "ANY",
    "ARE",
    "ARRAY",
    "AS",
    "ASENSITIVE",
    "ASYMMETRIC",
    "AT",
    "ATOMIC",
    "AUTHORIZATION",
    "AVG",
    "BEGIN",
    "BETWEEN",
    "BIGINT",
    "BINARY",
    "BLOB",
    "BOOLEAN",
    "BOTH",
    "BY",
    "CALL",
    "CALLED",
    "CARDINALITY",
    "CASCADED",
    "CASE",
    "CAST",
    "CEIL",
    "CEILING",
    "CHAR",
    "CHARACTER",
    "CHARACTER_LENGTH",
    "CHAR_LENGTH",
    "CHECK",
    "CLOB",
    "CLOSE",
    "COALESCE",
    "COLLATE",
    "COLLECT",
    "COLUMN",
    "COMMIT",
    "CONDITION",
    "CONNECT",
    "CONSTRAINT",
    "CONVERT",
    "CORR",
    "CORRESPONDING",
    "COUNT",
    "COVAR_POP",
    "COVAR_SAMP",
    "CREATE",
    "CROSS",
    "CUBE",
    "CUME_DIST",
    "CURRENT",
    "CURRENT_DATE",
    "CURRENT_DEFAULT_TRANSFORM_GROUP",
    "CURRENT_PATH",
    "CURRENT_ROLE",
    "CURRENT_TIME",
    "CURRENT_TIMESTAMP",
    "CURRENT_TRANSFORM_GROUP_FOR_TYPE",
    "CURRENT_USER",
    "CURSOR",
    "CYCLE",
    "DATE",
    "DAY",
    "DEALLOCATE",
    "DEC",
    "DECIMAL",
    "DECLARE",
    "DEFAULT",
    "DELETE",
    "DENSE_RANK",
    "DEREF",
    "DESCRIBE",
    "DETERMINISTIC",
    "DISCONNECT",
    "DISTINCT",
    "DOUBLE",
    "DROP",
    "DYNAMIC",
    "EACH",
    "ELEMENT",
    "ELSE",
    "END",
    "END-EXEC",
    "ESCAPE",
    "EVERY",
    "EXCEPT",
    "EXEC",
    "EXECUTE",
    "EXISTS",
    "EXP",
    "EXTERNAL",
    "EXTRACT",
    "FALSE",
    "FETCH",
    "FILTER",
    "FLOAT",
    "FLOOR",
    "FOR",
    "FOREIGN",
    "FREE",
    "FROM",
    "FULL",
    "FUNCTION",
    "FUSION",
    "GET",
    "GLOBAL",
    "GRANT",
    "GROUP",
    "GROUPING",
    "HAVING",
    "HOLD",
    "HOUR",
    "IDENTITY",
    "IN",
    "INDICATOR",
    "INNER",
    "INOUT",
    "INSENSITIVE",
    "INSERT",
    "INT",
    "INTEGER",
    "INTERSECT",
    "INTERSECTION",
    "INTERVAL",
    "INTO",
    "IS",
    "JOIN",
    "LANGUAGE",
    "LARGE",
    "LATERAL",
    "LEADING",
    "LEFT",
    "LIKE",
    "LN",
    "LOCAL",
    "LOCALTIME",
    "LOCALTIMESTAMP",
    "LOWER",
    "MATCH",
    "MAX",
    "MEMBER",
    "MERGE",
    "METHOD",
    "MIN",
    "MINUTE",
    "MOD",
    "MODIFIES",
    "MODULE",
    "MONTH",
    "MULTISET",
    "NATIONAL",
    "NATURAL",
    "NCHAR",
    "NCLOB",
    "NEW",
    "NO",
    "NONE",
    "NORMALIZE",
    "NOT",
    "NULL",
    "NULLIF",
    "NUMERIC",
    "OCTET_LENGTH",
    "OF",
    "OLD",
    "ON",
    "ONLY",
    "OPEN",
    "OR",
    "ORDER",
    "OUT",
    "OUTER",
    "OVER",
    "OVERLAPS",
    "OVERLAY",
    "PARAMETER",
    "PARTITION",
    "PERCENTILE_CONT",
    "PERCENTILE_DISC",
    "PERCENT_RANK",
    "POSITION",
    "POWER",
    "PRECISION",
    "PREPARE",
    "PRIMARY",
    "PROCEDURE",
    "RANGE",
    "RANK",
    "READS",
    "REAL",
    "RECURSIVE",
    "REF",
    "REFERENCES",
    "REFERENCING",
    "REGR_AVGX",
    "REGR_AVGY",
    "REGR_COUNT",
    "REGR_INTERCEPT",
    "REGR_R2",
    "REGR_SLOPE",
    "REGR_SXX",
    "REGR_SXY",
    "REGR_SYY",
    "RELEASE",
    "RESULT",
    "RETURN",
    "RETURNS",
    "REVOKE",
    "RIGHT",
    "ROLLBACK",
    "ROLLUP",
    "ROW",
    "ROWS",
    "ROW_NUMBER",
    "SAVEPOINT",
    "SCOPE",
    "SCROLL",
    "SEARCH",
    "SECOND",
    "SELECT",
    "SENSITIVE",
    "SESSION_USER",
    "SET",
    "SIMILAR",
    "SMALLINT",
    "SOME",
    "SPECIFIC",
    "SPECIFICTYPE",
    "SQL",
    "SQLEXCEPTION",
    "SQLSTATE",
    "SQLWARNING",
    "SQRT",
    "START",
    "STATIC",
    "STDDEV_POP",
    "STDDEV_SAMP",
    "SUBMULTISET",
    "SUBSTRING",
    "SUM",
    "SYMMETRIC",
    "SYSTEM",
    "SYSTEM_USER",
    "TABLE",
    "TABLESAMPLE",
    "THEN",
    "TIME",
    "TIMESTAMP",
    "TIMEZONE_HOUR",
    "TIMEZONE_MINUTE",
    "TO",
    "TRAILING",
    "TRANSLATE",
    "TRANSLATION",
    "TREAT",
    "TRIGGER",
    "TRIM",
    "TRUE",
    "UESCAPE",
    "UNION",
    "UNIQUE",
    "UNKNOWN",
    "UNNEST",
    "UPDATE",
    "UPPER",
    "USER",
    "USING",
    "VALUE",
    "VALUES",
    "VARCHAR",
    "VARYING",
    "VAR_POP",
    "VAR_SAMP",
    "WHEN",
    "WHENEVER",
    "WHERE",
    "WIDTH_BUCKET",
    "WINDOW",
    "WITH",
    "WITHIN",
    "WITHOUT",
    "YEAR"
  )

  private val maxBufferSize = 65535

  /**
   * Trait that defines the value determined at the time MySQL is used.
   *
   * @tparam F
   *   the effect type
   */
  private[ldbc] trait StaticDatabaseMetaData[F[_]] extends DatabaseMetaData[F]:
    override def allProceduresAreCallable(): Boolean = false
    override def allTablesAreSelectable():   Boolean = false
    override def isReadOnly():               Boolean = false
    override def nullsAreSortedHigh():       Boolean = false
    override def nullsAreSortedLow():        Boolean = !nullsAreSortedHigh()
    override def nullsAreSortedAtStart():    Boolean = false
    override def nullsAreSortedAtEnd():      Boolean = false
    override def getDatabaseProductName():   String  = "MySQL"
    override def getDriverName():            String  = DRIVER_NAME
    override def getDriverVersion():         String  = s"ldbc-connector-${ DRIVER_VERSION }"
    override def getDriverMajorVersion():    Int     = DRIVER_VERSION.major
    override def getDriverMinorVersion():    Int     = DRIVER_VERSION.minor
    override def usesLocalFiles():           Boolean = false
    override def usesLocalFilePerTable():    Boolean = false

    override def getNumericFunctions(): String =
      "ABS,ACOS,ASIN,ATAN,ATAN2,BIT_COUNT,CEILING,COS,COT,DEGREES,EXP,FLOOR,LOG,LOG10,MAX,MIN,MOD,PI,POW,POWER,RADIANS,RAND,ROUND,SIN,SQRT,TAN,TRUNCATE"

    override def getStringFunctions(): String =
      "ASCII,BIN,BIT_LENGTH,CHAR,CHARACTER_LENGTH,CHAR_LENGTH,CONCAT,CONCAT_WS,CONV,ELT,EXPORT_SET,FIELD,FIND_IN_SET,HEX,INSERT,"
        + "INSTR,LCASE,LEFT,LENGTH,LOAD_FILE,LOCATE,LOCATE,LOWER,LPAD,LTRIM,MAKE_SET,MATCH,MID,OCT,OCTET_LENGTH,ORD,POSITION,"
        + "QUOTE,REPEAT,REPLACE,REVERSE,RIGHT,RPAD,RTRIM,SOUNDEX,SPACE,STRCMP,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING,"
        + "SUBSTRING_INDEX,TRIM,UCASE,UPPER"

    override def getSystemFunctions(): String =
      "DATABASE,USER,SYSTEM_USER,SESSION_USER,PASSWORD,ENCRYPT,LAST_INSERT_ID,VERSION"

    override def getTimeDateFunctions(): String =
      "DAYOFWEEK,WEEKDAY,DAYOFMONTH,DAYOFYEAR,MONTH,DAYNAME,MONTHNAME,QUARTER,WEEK,YEAR,HOUR,MINUTE,SECOND,PERIOD_ADD,"
        + "PERIOD_DIFF,TO_DAYS,FROM_DAYS,DATE_FORMAT,TIME_FORMAT,CURDATE,CURRENT_DATE,CURTIME,CURRENT_TIME,NOW,SYSDATE,"
        + "CURRENT_TIMESTAMP,UNIX_TIMESTAMP,FROM_UNIXTIME,SEC_TO_TIME,TIME_TO_SEC"

    override def getSearchStringEscape(): String = "\\"

    override def getExtraNameCharacters(): String = "$"

    override def supportsAlterTableWithAddColumn(): Boolean = true

    override def supportsAlterTableWithDropColumn(): Boolean = true

    override def supportsColumnAliasing(): Boolean = true

    override def nullPlusNonNullIsNull(): Boolean = true

    override def supportsConvert(): Boolean = false

    override def supportsConvert(fromType: Int, toType: Int): Boolean =
      fromType match
        case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY =>
          toType match
            case DECIMAL | NUMERIC | REAL | TINYINT | SMALLINT | INTEGER | BIGINT | FLOAT | DOUBLE | CHAR | VARCHAR |
              BINARY | VARBINARY | LONGVARBINARY | OTHER | DATE | TIME | TIMESTAMP =>
              true
            case _ => false
        case DECIMAL | NUMERIC | REAL | TINYINT | SMALLINT | INTEGER | BIGINT | FLOAT | DOUBLE =>
          toType match
            case DECIMAL | NUMERIC | REAL | TINYINT | SMALLINT | INTEGER | BIGINT | FLOAT | DOUBLE | CHAR | VARCHAR |
              BINARY | VARBINARY | LONGVARBINARY =>
              true
            case _ => false
        case OTHER =>
          toType match
            case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY => true
            case _                                                                 => false
        case DATE =>
          toType match
            case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY => true
            case _                                                                 => false
        case TIME =>
          toType match
            case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY => true
            case _                                                                 => false
        case TIMESTAMP =>
          toType match
            case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY | TIME | DATE => true
            case _                                                                               => false
        case _ => false

    override def supportsTableCorrelationNames():          Boolean = true
    override def supportsDifferentTableCorrelationNames(): Boolean = true
    override def supportsExpressionsInOrderBy():           Boolean = true
    override def supportsOrderByUnrelated():               Boolean = false
    override def supportsGroupBy():                        Boolean = true
    override def supportsGroupByUnrelated():               Boolean = true
    override def supportsGroupByBeyondSelect():            Boolean = true
    override def supportsLikeEscapeClause():               Boolean = true
    override def supportsMultipleResultSets():             Boolean = true
    override def supportsMultipleTransactions():           Boolean = true
    override def supportsNonNullableColumns():             Boolean = true
    override def supportsMinimumSQLGrammar():              Boolean = true
    override def supportsCoreSQLGrammar():                 Boolean = true
    override def supportsExtendedSQLGrammar():             Boolean = false
    override def supportsANSI92EntryLevelSQL():            Boolean = true
    override def supportsANSI92IntermediateSQL():          Boolean = false
    override def supportsANSI92FullSQL():                  Boolean = false
    override def supportsIntegrityEnhancementFacility():   Boolean = false
    override def supportsOuterJoins():                     Boolean = true
    override def supportsFullOuterJoins():                 Boolean = false
    override def supportsLimitedOuterJoins():              Boolean = true
    override def storesUpperCaseIdentifiers():             Boolean = false
    override def storesMixedCaseIdentifiers():             Boolean = !storesLowerCaseIdentifiers()
    override def supportsMixedCaseQuotedIdentifiers():     Boolean = supportsMixedCaseIdentifiers()
    override def storesUpperCaseQuotedIdentifiers():       Boolean = true
    override def storesLowerCaseQuotedIdentifiers():       Boolean = storesLowerCaseIdentifiers()
    override def storesMixedCaseQuotedIdentifiers():       Boolean = !storesLowerCaseIdentifiers()
    override def isCatalogAtStart():                       Boolean = true
    override def getCatalogSeparator():                    String  = "."
    override def supportsPositionedDelete():               Boolean = false
    override def supportsPositionedUpdate():               Boolean = false
    override def supportsSelectForUpdate():                Boolean = true
    override def supportsStoredProcedures():               Boolean = true
    override def supportsSubqueriesInComparisons():        Boolean = true
    override def supportsSubqueriesInExists():             Boolean = true
    override def supportsSubqueriesInIns():                Boolean = true
    override def supportsSubqueriesInQuantifieds():        Boolean = true
    override def supportsCorrelatedSubqueries():           Boolean = true
    override def supportsUnion():                          Boolean = true
    override def supportsUnionAll():                       Boolean = true
    override def supportsOpenCursorsAcrossCommit():        Boolean = false
    override def supportsOpenCursorsAcrossRollback():      Boolean = false
    override def supportsOpenStatementsAcrossCommit():     Boolean = false
    override def supportsOpenStatementsAcrossRollback():   Boolean = false
    override def getMaxBinaryLiteralLength():              Int     = 16777208
    override def getMaxCharLiteralLength():                Int     = 16777208
    override def getMaxColumnNameLength():                 Int     = 64
    override def getMaxColumnsInGroupBy():                 Int     = 64
    override def getMaxColumnsInIndex():                   Int     = 16
    override def getMaxColumnsInOrderBy():                 Int     = 64
    override def getMaxColumnsInSelect():                  Int     = 256
    override def getMaxColumnsInTable():                   Int     = 512
    override def getMaxConnections():                      Int     = 0
    override def getMaxCursorNameLength():                 Int     = 64
    override def getMaxIndexLength():                      Int     = 256
    override def getMaxSchemaNameLength():                 Int     = 0
    override def getMaxProcedureNameLength():              Int     = 0
    override def getMaxCatalogNameLength():                Int     = 32
    override def getMaxRowSize():                          Int     = Int.MaxValue - 8
    override def doesMaxRowSizeIncludeBlobs():             Boolean = true
    override def getMaxStatementLength():                  Int     = maxBufferSize - 4
    override def getMaxStatements():                       Int     = 0
    override def getMaxTableNameLength():                  Int     = 64
    override def getMaxTablesInSelect():                   Int     = 256
    override def getMaxUserNameLength():                   Int     = 16
    override def getDefaultTransactionIsolation():         Int     = Connection.TRANSACTION_REPEATABLE_READ
    override def supportsTransactions():                   Boolean = true
    override def supportsTransactionIsolationLevel(level: Int): Boolean = level match
      case Connection.TRANSACTION_READ_COMMITTED | Connection.TRANSACTION_READ_UNCOMMITTED |
        Connection.TRANSACTION_REPEATABLE_READ | Connection.TRANSACTION_SERIALIZABLE =>
        true
      case _ => false
    override def supportsDataDefinitionAndDataManipulationTransactions():    Boolean = false
    override def supportsDataManipulationTransactionsOnly():                 Boolean = false
    override def dataDefinitionCausesTransactionCommit():                    Boolean = true
    override def dataDefinitionIgnoredInTransactions():                      Boolean = false
    override def ownUpdatesAreVisible(`type`:    Int):                       Boolean = false
    override def ownDeletesAreVisible(`type`:    Int):                       Boolean = false
    override def ownInsertsAreVisible(`type`:    Int):                       Boolean = false
    override def othersUpdatesAreVisible(`type`: Int):                       Boolean = false
    override def othersDeletesAreVisible(`type`: Int):                       Boolean = false
    override def othersInsertsAreVisible(`type`: Int):                       Boolean = false
    override def updatesAreDetected(`type`:      Int):                       Boolean = false
    override def deletesAreDetected(`type`:      Int):                       Boolean = false
    override def insertsAreDetected(`type`:      Int):                       Boolean = false
    override def supportsBatchUpdates():                                     Boolean = true
    override def supportsSavepoints():                                       Boolean = true
    override def supportsNamedParameters():                                  Boolean = false
    override def supportsMultipleOpenResults():                              Boolean = true
    override def supportsGetGeneratedKeys():                                 Boolean = true
    override def getResultSetHoldability():                Int           = ResultSet.HOLD_CURSORS_OVER_COMMIT
    override def getJDBCMajorVersion():                    Int           = Constants.DRIVER_VERSION.major
    override def getJDBCMinorVersion():                    Int           = Constants.DRIVER_VERSION.minor
    override def getSQLStateType():                        Int           = DatabaseMetaData.sqlStateSQL99
    override def supportsStatementPooling():               Boolean       = false
    override def getRowIdLifetime():                       RowIdLifetime = RowIdLifetime.ROWID_UNSUPPORTED
    override def supportsStoredFunctionsUsingCallSyntax(): Boolean       = true
    override def autoCommitFailureClosesAllResultSets():   Boolean       = false
    override def generatedKeyAlwaysReturned():             Boolean       = true
    override def supportsResultSetType(`type`: Int): Boolean =
      `type` == ResultSet.TYPE_FORWARD_ONLY || `type` == ResultSet.TYPE_SCROLL_INSENSITIVE

    override def supportsResultSetConcurrency(`type`: Int, concurrency: Int): Boolean =
      if (`type` == ResultSet.TYPE_FORWARD_ONLY || `type` == ResultSet.TYPE_SCROLL_INSENSITIVE) && (concurrency == ResultSet.CONCUR_READ_ONLY || concurrency == ResultSet.CONCUR_UPDATABLE)
      then true
      else if `type` == ResultSet.TYPE_SCROLL_SENSITIVE then false
      else throw new SQLException("Illegal arguments to supportsResultSetConcurrency()")

    override def supportsResultSetHoldability(holdability: Int): Boolean =
      holdability == ResultSet.HOLD_CURSORS_OVER_COMMIT
    override def locatorsUpdateCopy(): Boolean = true
  end StaticDatabaseMetaData
