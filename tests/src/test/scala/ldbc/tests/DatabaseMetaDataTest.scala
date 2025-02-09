/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.*

import ldbc.sql.*

import ldbc.connector.SSL

class LdbcDatabaseMetaDataTest extends DatabaseMetaDataTest:
  override def prefix: "ldbc" = "ldbc"

  override def connection: Resource[IO, Connection[IO]] =
    ldbc.connector.Connection[IO](
      host     = host,
      port     = port,
      user     = user,
      password = Some(password),
      database = Some(database),
      ssl      = SSL.Trusted
    )

class JdbcDatabaseMetaDataTest extends DatabaseMetaDataTest:

  val ds = new MysqlDataSource()
  ds.setServerName(host)
  ds.setPortNumber(port)
  ds.setDatabaseName(database)
  ds.setUser(user)
  ds.setPassword(password)

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: Resource[IO, Connection[IO]] =
    Resource.make(jdbc.connector.MysqlDataSource[IO](ds).getConnection)(_.close())

trait DatabaseMetaDataTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  protected val host:     String = "127.0.0.1"
  protected val port:     Int    = 13306
  protected val user:     String = "ldbc"
  protected val password: String = "password"
  protected val database: String = "connector_test"

  def prefix:     "jdbc" | "ldbc"
  def connection: Resource[IO, Connection[IO]]

  test(s"$prefix: allTablesAreSelectable") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.allTablesAreSelectable()
      },
      false
    )
  }

  test(s"$prefix: getURL") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getURL()
      },
      s"jdbc:mysql://$host:$port/$database"
    )
  }

  test(s"$prefix: getUserName") {
    assertIO(
      connection.use { conn =>
        for
          metaData <- conn.getMetaData()
          value    <- metaData.getUserName()
        yield value
      },
      s"$user@172.18.0.1"
    )
  }

  test(s"$prefix: isReadOnly") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.isReadOnly()
      },
      false
    )
  }

  test(s"$prefix: nullsAreSortedHigh") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.nullsAreSortedHigh()
      },
      false
    )
  }

  test(s"$prefix: nullsAreSortedLow") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.nullsAreSortedLow()
      },
      true
    )
  }

  test(s"$prefix: nullsAreSortedAtStart") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.nullsAreSortedAtStart()
      },
      false
    )
  }

  test(s"$prefix: nullsAreSortedAtEnd") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.nullsAreSortedAtEnd()
      },
      false
    )
  }

  test(s"$prefix: getDatabaseProductName") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDatabaseProductName()
      },
      "MySQL"
    )
  }

  test(s"$prefix: getDatabaseProductVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDatabaseProductVersion()
      },
      "8.4.0"
    )
  }

  test(s"$prefix: getDriverName") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDriverName()
      },
      if prefix == "ldbc" then "MySQL Connector/L" else "MySQL Connector/J"
    )
  }

  test(s"$prefix: getDriverVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDriverVersion()
      },
      if prefix == "jdbc" then "mysql-connector-j-8.4.0 (Revision: 1c3f5c149e0bfe31c7fbeb24e2d260cd890972c4)"
      else "ldbc-connector-0.3.0"
    )
  }

  test(s"$prefix: getDriverMajorVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDriverMajorVersion()
      },
      if prefix == "jdbc" then 8 else 0
    )
  }

  test(s"$prefix: getDriverMinorVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDriverMinorVersion()
      },
      if prefix == "jdbc" then 4 else 3
    )
  }

  test(s"$prefix: usesLocalFiles") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.usesLocalFiles()
      },
      false
    )
  }

  test(s"$prefix: usesLocalFilePerTable") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.usesLocalFilePerTable()
      },
      false
    )
  }

  test(s"$prefix: supportsMixedCaseIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsMixedCaseIdentifiers()
      },
      true
    )
  }

  test(s"$prefix: storesUpperCaseIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.storesUpperCaseIdentifiers()
      },
      false
    )
  }

  test(s"$prefix: storesLowerCaseIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.storesLowerCaseIdentifiers()
      },
      false
    )
  }

  test(s"$prefix: storesMixedCaseIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.storesMixedCaseIdentifiers()
      },
      true
    )
  }

  test(s"$prefix: supportsMixedCaseQuotedIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsMixedCaseQuotedIdentifiers()
      },
      true
    )
  }

  test(s"$prefix: storesUpperCaseQuotedIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.storesUpperCaseQuotedIdentifiers()
      },
      true
    )
  }

  test(s"$prefix: storesLowerCaseQuotedIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.storesLowerCaseQuotedIdentifiers()
      },
      false
    )
  }

  test(s"$prefix: storesMixedCaseQuotedIdentifiers") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.storesMixedCaseQuotedIdentifiers()
      },
      true
    )
  }

  test(s"$prefix: getIdentifierQuoteString") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getIdentifierQuoteString()
      },
      "`"
    )
  }

  test(s"$prefix: getSQLKeywords") {
    assertIO(
      connection.use { conn =>
        for
          metaData <- conn.getMetaData()
          value    <- metaData.getSQLKeywords()
        yield value
      },
      "ACCESSIBLE,ADD,ANALYZE,ASC,BEFORE,CASCADE,CHANGE,CONTINUE,DATABASE,DATABASES,DAY_HOUR,DAY_MICROSECOND,DAY_MINUTE,DAY_SECOND,DELAYED,DESC,DISTINCTROW,DIV,DUAL,ELSEIF,EMPTY,ENCLOSED,ESCAPED,EXIT,EXPLAIN,FIRST_VALUE,FLOAT4,FLOAT8,FORCE,FULLTEXT,GENERATED,GROUPS,HIGH_PRIORITY,HOUR_MICROSECOND,HOUR_MINUTE,HOUR_SECOND,IF,IGNORE,INDEX,INFILE,INT1,INT2,INT3,INT4,INT8,IO_AFTER_GTIDS,IO_BEFORE_GTIDS,ITERATE,JSON_TABLE,KEY,KEYS,KILL,LAG,LAST_VALUE,LEAD,LEAVE,LIMIT,LINEAR,LINES,LOAD,LOCK,LONG,LONGBLOB,LONGTEXT,LOOP,LOW_PRIORITY,MAXVALUE,MEDIUMBLOB,MEDIUMINT,MEDIUMTEXT,MIDDLEINT,MINUTE_MICROSECOND,MINUTE_SECOND,NO_WRITE_TO_BINLOG,NTH_VALUE,NTILE,OPTIMIZE,OPTIMIZER_COSTS,OPTION,OPTIONALLY,OUTFILE,PURGE,READ,READ_WRITE,REGEXP,RENAME,REPEAT,REPLACE,REQUIRE,RESIGNAL,RESTRICT,RLIKE,SCHEMA,SCHEMAS,SECOND_MICROSECOND,SEPARATOR,SHOW,SIGNAL,SPATIAL,SQL_BIG_RESULT,SQL_CALC_FOUND_ROWS,SQL_SMALL_RESULT,SSL,STARTING,STORED,STRAIGHT_JOIN,TERMINATED,TINYBLOB,TINYINT,TINYTEXT,UNDO,UNLOCK,UNSIGNED,USAGE,USE,UTC_DATE,UTC_TIME,UTC_TIMESTAMP,VARBINARY,VARCHARACTER,VIRTUAL,WHILE,WRITE,XOR,YEAR_MONTH,ZEROFILL"
    )
  }

  test(s"$prefix: getNumericFunctions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getNumericFunctions()
      },
      "ABS,ACOS,ASIN,ATAN,ATAN2,BIT_COUNT,CEILING,COS,COT,DEGREES,EXP,FLOOR,LOG,LOG10,MAX,MIN,MOD,PI,POW,POWER,RADIANS,RAND,ROUND,SIN,SQRT,TAN,TRUNCATE"
    )
  }

  test(s"$prefix: getStringFunctions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getStringFunctions()
      },
      "ASCII,BIN,BIT_LENGTH,CHAR,CHARACTER_LENGTH,CHAR_LENGTH,CONCAT,CONCAT_WS,CONV,ELT,EXPORT_SET,FIELD,FIND_IN_SET,HEX,INSERT,INSTR,LCASE,LEFT,LENGTH,LOAD_FILE,LOCATE,LOCATE,LOWER,LPAD,LTRIM,MAKE_SET,MATCH,MID,OCT,OCTET_LENGTH,ORD,POSITION,QUOTE,REPEAT,REPLACE,REVERSE,RIGHT,RPAD,RTRIM,SOUNDEX,SPACE,STRCMP,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING_INDEX,TRIM,UCASE,UPPER"
    )
  }

  test(s"$prefix: getSystemFunctions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getSystemFunctions()
      },
      "DATABASE,USER,SYSTEM_USER,SESSION_USER,PASSWORD,ENCRYPT,LAST_INSERT_ID,VERSION"
    )
  }

  test(s"$prefix: getTimeDateFunctions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getTimeDateFunctions()
      },
      "DAYOFWEEK,WEEKDAY,DAYOFMONTH,DAYOFYEAR,MONTH,DAYNAME,MONTHNAME,QUARTER,WEEK,YEAR,HOUR,MINUTE,SECOND,PERIOD_ADD,PERIOD_DIFF,TO_DAYS,FROM_DAYS,DATE_FORMAT,TIME_FORMAT,CURDATE,CURRENT_DATE,CURTIME,CURRENT_TIME,NOW,SYSDATE,CURRENT_TIMESTAMP,UNIX_TIMESTAMP,FROM_UNIXTIME,SEC_TO_TIME,TIME_TO_SEC"
    )
  }

  test(s"$prefix: getSearchStringEscape") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getSearchStringEscape()
      },
      "\\"
    )
  }

  test(s"$prefix: getExtraNameCharacters") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getExtraNameCharacters()
      },
      "$"
    )
  }

  test(s"$prefix: supportsAlterTableWithAddColumn") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsAlterTableWithAddColumn()
      },
      true
    )
  }

  test(s"$prefix: supportsAlterTableWithDropColumn") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsAlterTableWithDropColumn()
      },
      true
    )
  }

  test(s"$prefix: supportsColumnAliasing") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsColumnAliasing()
      },
      true
    )
  }

  test(s"$prefix: nullPlusNonNullIsNull") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.nullPlusNonNullIsNull()
      },
      true
    )
  }

  test(s"$prefix: supportsConvert") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsConvert()
      },
      false
    )
  }

  test(s"$prefix: supportsTableCorrelationNames") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsTableCorrelationNames()
      },
      true
    )
  }

  test(s"$prefix: supportsDifferentTableCorrelationNames") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsDifferentTableCorrelationNames()
      },
      true
    )
  }

  test(s"$prefix: supportsExpressionsInOrderBy") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsExpressionsInOrderBy()
      },
      true
    )
  }

  test(s"$prefix: supportsOrderByUnrelated") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsOrderByUnrelated()
      },
      false
    )
  }

  test(s"$prefix: supportsGroupBy") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsGroupBy()
      },
      true
    )
  }

  test(s"$prefix: supportsGroupByUnrelated") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsGroupByUnrelated()
      },
      true
    )
  }

  test(s"$prefix: supportsGroupByBeyondSelect") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsGroupByBeyondSelect()
      },
      true
    )
  }

  test(s"$prefix: supportsLikeEscapeClause") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsLikeEscapeClause()
      },
      true
    )
  }

  test(s"$prefix: supportsMultipleResultSets") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsMultipleResultSets()
      },
      true
    )
  }

  test(s"$prefix: supportsMultipleTransactions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsMultipleTransactions()
      },
      true
    )
  }

  test(s"$prefix: supportsNonNullableColumns") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsNonNullableColumns()
      },
      true
    )
  }

  test(s"$prefix: supportsMinimumSQLGrammar") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsMinimumSQLGrammar()
      },
      true
    )
  }

  test(s"$prefix: supportsCoreSQLGrammar") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsCoreSQLGrammar()
      },
      true
    )
  }

  test(s"$prefix: supportsExtendedSQLGrammar") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsExtendedSQLGrammar()
      },
      false
    )
  }

  test(s"$prefix: supportsANSI92EntryLevelSQL") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsANSI92EntryLevelSQL()
      },
      true
    )
  }

  test(s"$prefix: supportsANSI92IntermediateSQL") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsANSI92IntermediateSQL()
      },
      false
    )
  }

  test(s"$prefix: supportsANSI92FullSQL") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsANSI92FullSQL()
      },
      false
    )
  }

  test(s"$prefix: supportsIntegrityEnhancementFacility") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsIntegrityEnhancementFacility()
      },
      false
    )
  }

  test(s"$prefix: supportsOuterJoins") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsOuterJoins()
      },
      true
    )
  }

  test(s"$prefix: supportsLimitedOuterJoins") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsLimitedOuterJoins()
      },
      true
    )
  }

  test(s"$prefix: getSchemaTerm") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getSchemaTerm()
      },
      ""
    )
  }

  test(s"$prefix: getProcedureTerm") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getProcedureTerm()
      },
      "PROCEDURE"
    )
  }

  test(s"$prefix: getCatalogTerm") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getCatalogTerm()
      },
      "database"
    )
  }

  test(s"$prefix: isCatalogAtStart") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.isCatalogAtStart()
      },
      true
    )
  }

  test(s"$prefix: getCatalogSeparator") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getCatalogSeparator()
      },
      "."
    )
  }

  test(s"$prefix: supportsSchemasInDataManipulation") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSchemasInDataManipulation()
      },
      false
    )
  }

  test(s"$prefix: supportsSchemasInProcedureCalls") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSchemasInProcedureCalls()
      },
      false
    )
  }

  test(s"$prefix: supportsSchemasInTableDefinitions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSchemasInTableDefinitions()
      },
      false
    )
  }

  test(s"$prefix: supportsSchemasInIndexDefinitions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSchemasInIndexDefinitions()
      },
      false
    )
  }

  test(s"$prefix: supportsSchemasInPrivilegeDefinitions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSchemasInPrivilegeDefinitions()
      },
      false
    )
  }

  test(s"$prefix: supportsCatalogsInDataManipulation") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsCatalogsInDataManipulation()
      },
      true
    )
  }

  test(s"$prefix: supportsCatalogsInProcedureCalls") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsCatalogsInProcedureCalls()
      },
      true
    )
  }

  test(s"$prefix: supportsCatalogsInTableDefinitions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsCatalogsInTableDefinitions()
      },
      true
    )
  }

  test(s"$prefix: supportsCatalogsInIndexDefinitions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsCatalogsInIndexDefinitions()
      },
      true
    )
  }

  test(s"$prefix: supportsCatalogsInPrivilegeDefinitions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsCatalogsInPrivilegeDefinitions()
      },
      true
    )
  }

  test(s"$prefix: supportsPositionedDelete") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsPositionedDelete()
      },
      false
    )
  }

  test(s"$prefix: supportsPositionedUpdate") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsPositionedUpdate()
      },
      false
    )
  }

  test(s"$prefix: supportsSelectForUpdate") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSelectForUpdate()
      },
      true
    )
  }

  test(s"$prefix: supportsStoredProcedures") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsStoredProcedures()
      },
      true
    )
  }

  test(s"$prefix: supportsSubqueriesInComparisons") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSubqueriesInComparisons()
      },
      true
    )
  }

  test(s"$prefix: supportsSubqueriesInExists") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSubqueriesInExists()
      },
      true
    )
  }

  test(s"$prefix: supportsSubqueriesInIns") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSubqueriesInIns()
      },
      true
    )
  }

  test(s"$prefix: supportsSubqueriesInQuantifieds") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSubqueriesInQuantifieds()
      },
      true
    )
  }

  test(s"$prefix: supportsCorrelatedSubqueries") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsCorrelatedSubqueries()
      },
      true
    )
  }

  test(s"$prefix: supportsUnion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsUnion()
      },
      true
    )
  }

  test(s"$prefix: supportsUnionAll") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsUnionAll()
      },
      true
    )
  }

  test(s"$prefix: supportsOpenCursorsAcrossCommit") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsOpenCursorsAcrossCommit()
      },
      false
    )
  }

  test(s"$prefix: supportsOpenCursorsAcrossRollback") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsOpenCursorsAcrossRollback()
      },
      false
    )
  }

  test(s"$prefix: supportsOpenStatementsAcrossCommit") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsOpenStatementsAcrossCommit()
      },
      false
    )
  }

  test(s"$prefix: supportsOpenStatementsAcrossRollback") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsOpenStatementsAcrossRollback()
      },
      false
    )
  }

  test(s"$prefix: getMaxBinaryLiteralLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxBinaryLiteralLength()
      },
      16777208
    )
  }

  test(s"$prefix: getMaxCharLiteralLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxCharLiteralLength()
      },
      16777208
    )
  }

  test(s"$prefix: getMaxColumnNameLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxColumnNameLength()
      },
      64
    )
  }

  test(s"$prefix: getMaxColumnsInGroupBy") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxColumnsInGroupBy()
      },
      64
    )
  }

  test(s"$prefix: getMaxColumnsInIndex") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxColumnsInIndex()
      },
      16
    )
  }

  test(s"$prefix: getMaxColumnsInOrderBy") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxColumnsInOrderBy()
      },
      64
    )
  }

  test(s"$prefix: getMaxColumnsInSelect") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxColumnsInSelect()
      },
      256
    )
  }

  test(s"$prefix: getMaxColumnsInTable") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxColumnsInTable()
      },
      512
    )
  }

  test(s"$prefix: getMaxConnections") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxConnections()
      },
      0
    )
  }

  test(s"$prefix: getMaxCursorNameLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxCursorNameLength()
      },
      64
    )
  }

  test(s"$prefix: getMaxIndexLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxIndexLength()
      },
      256
    )
  }

  test(s"$prefix: getMaxSchemaNameLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxSchemaNameLength()
      },
      0
    )
  }

  test(s"$prefix: getMaxProcedureNameLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxProcedureNameLength()
      },
      0
    )
  }

  test(s"$prefix: getMaxCatalogNameLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxCatalogNameLength()
      },
      32
    )
  }

  test(s"$prefix: getMaxRowSize") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxRowSize()
      },
      2147483639
    )
  }

  test(s"$prefix: doesMaxRowSizeIncludeBlobs") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.doesMaxRowSizeIncludeBlobs()
      },
      true
    )
  }

  test(s"$prefix: getMaxStatementLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxStatementLength()
      },
      65531
    )
  }

  test(s"$prefix: getMaxStatements") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxStatements()
      },
      0
    )
  }

  test(s"$prefix: getMaxTableNameLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxTableNameLength()
      },
      64
    )
  }

  test(s"$prefix: getMaxTablesInSelect") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxTablesInSelect()
      },
      256
    )
  }

  test(s"$prefix: getMaxUserNameLength") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxUserNameLength()
      },
      16
    )
  }

  test(s"$prefix: getDefaultTransactionIsolation") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDefaultTransactionIsolation()
      },
      4
    )
  }

  test(s"$prefix: supportsTransactions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsTransactions()
      },
      true
    )
  }

  test(s"$prefix: supportsTransactionIsolationLevel") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsTransactionIsolationLevel(2)
      },
      true
    )
  }

  test(s"$prefix: supportsDataDefinitionAndDataManipulationTransactions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsDataDefinitionAndDataManipulationTransactions()
      },
      false
    )
  }

  test(s"$prefix: supportsDataManipulationTransactionsOnly") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsDataManipulationTransactionsOnly()
      },
      false
    )
  }

  test(s"$prefix: dataDefinitionCausesTransactionCommit") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.dataDefinitionCausesTransactionCommit()
      },
      true
    )
  }

  test(s"$prefix: dataDefinitionIgnoredInTransactions") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.dataDefinitionIgnoredInTransactions()
      },
      false
    )
  }

  test(s"$prefix: getProcedures") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getProcedures(None, None, None)
        yield
          val builder = List.newBuilder[String]
          while resultSet.next() do
            val procedureCat   = resultSet.getString("PROCEDURE_CAT")
            val procedureSchem = resultSet.getString("PROCEDURE_SCHEM")
            val procedureName  = resultSet.getString("PROCEDURE_NAME")
            val procedureType  = resultSet.getShort("PROCEDURE_TYPE")
            val specificName   = resultSet.getString("SPECIFIC_NAME")
            builder += s"$procedureCat, $procedureSchem, $procedureName, $procedureType, $specificName"
          builder.result()
      },
      List(
        "connector_test, null, demoSp, 1, demoSp",
        "connector_test, null, func1, 2, func1",
        "connector_test, null, func2, 2, func2",
        "connector_test, null, getPrice, 2, getPrice",
        "connector_test, null, proc1, 1, proc1",
        "connector_test, null, proc2, 1, proc2",
        "connector_test, null, proc3, 1, proc3",
        "connector_test, null, proc4, 1, proc4",
        "sys, null, create_synonym_db, 1, create_synonym_db",
        "sys, null, diagnostics, 1, diagnostics",
        "sys, null, execute_prepared_stmt, 1, execute_prepared_stmt",
        "sys, null, extract_schema_from_file_name, 2, extract_schema_from_file_name",
        "sys, null, extract_table_from_file_name, 2, extract_table_from_file_name",
        "sys, null, format_bytes, 2, format_bytes",
        "sys, null, format_path, 2, format_path",
        "sys, null, format_statement, 2, format_statement",
        "sys, null, format_time, 2, format_time",
        "sys, null, list_add, 2, list_add",
        "sys, null, list_drop, 2, list_drop",
        "sys, null, ps_is_account_enabled, 2, ps_is_account_enabled",
        "sys, null, ps_is_consumer_enabled, 2, ps_is_consumer_enabled",
        "sys, null, ps_is_instrument_default_enabled, 2, ps_is_instrument_default_enabled",
        "sys, null, ps_is_instrument_default_timed, 2, ps_is_instrument_default_timed",
        "sys, null, ps_is_thread_instrumented, 2, ps_is_thread_instrumented",
        "sys, null, ps_setup_disable_background_threads, 1, ps_setup_disable_background_threads",
        "sys, null, ps_setup_disable_consumer, 1, ps_setup_disable_consumer",
        "sys, null, ps_setup_disable_instrument, 1, ps_setup_disable_instrument",
        "sys, null, ps_setup_disable_thread, 1, ps_setup_disable_thread",
        "sys, null, ps_setup_enable_background_threads, 1, ps_setup_enable_background_threads",
        "sys, null, ps_setup_enable_consumer, 1, ps_setup_enable_consumer",
        "sys, null, ps_setup_enable_instrument, 1, ps_setup_enable_instrument",
        "sys, null, ps_setup_enable_thread, 1, ps_setup_enable_thread",
        "sys, null, ps_setup_reload_saved, 1, ps_setup_reload_saved",
        "sys, null, ps_setup_reset_to_default, 1, ps_setup_reset_to_default",
        "sys, null, ps_setup_save, 1, ps_setup_save",
        "sys, null, ps_setup_show_disabled, 1, ps_setup_show_disabled",
        "sys, null, ps_setup_show_disabled_consumers, 1, ps_setup_show_disabled_consumers",
        "sys, null, ps_setup_show_disabled_instruments, 1, ps_setup_show_disabled_instruments",
        "sys, null, ps_setup_show_enabled, 1, ps_setup_show_enabled",
        "sys, null, ps_setup_show_enabled_consumers, 1, ps_setup_show_enabled_consumers",
        "sys, null, ps_setup_show_enabled_instruments, 1, ps_setup_show_enabled_instruments",
        "sys, null, ps_statement_avg_latency_histogram, 1, ps_statement_avg_latency_histogram",
        "sys, null, ps_thread_account, 2, ps_thread_account",
        "sys, null, ps_thread_id, 2, ps_thread_id",
        "sys, null, ps_thread_stack, 2, ps_thread_stack",
        "sys, null, ps_thread_trx_info, 2, ps_thread_trx_info",
        "sys, null, ps_trace_statement_digest, 1, ps_trace_statement_digest",
        "sys, null, ps_trace_thread, 1, ps_trace_thread",
        "sys, null, ps_truncate_all_tables, 1, ps_truncate_all_tables",
        "sys, null, quote_identifier, 2, quote_identifier",
        "sys, null, statement_performance_analyzer, 1, statement_performance_analyzer",
        "sys, null, sys_get_config, 2, sys_get_config",
        "sys, null, table_exists, 1, table_exists",
        "sys, null, version_major, 2, version_major",
        "sys, null, version_minor, 2, version_minor",
        "sys, null, version_patch, 2, version_patch"
      )
    )
  }

  test(s"$prefix: getProcedureColumns") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getProcedureColumns(None, None, None, None)
        yield
          val builder = List.newBuilder[String]
          while resultSet.next() do
            val procedureCat    = resultSet.getString("PROCEDURE_CAT")
            val procedureSchem  = resultSet.getString("PROCEDURE_SCHEM")
            val procedureName   = resultSet.getString("PROCEDURE_NAME")
            val columnName      = resultSet.getString("COLUMN_NAME")
            val columnType      = resultSet.getInt("COLUMN_TYPE")
            val dataType        = resultSet.getInt("DATA_TYPE")
            val typeName        = resultSet.getString("TYPE_NAME")
            val precision       = resultSet.getInt("PRECISION")
            val length          = resultSet.getInt("LENGTH")
            val scale           = resultSet.getInt("SCALE")
            val radix           = resultSet.getInt("RADIX")
            val nullable        = resultSet.getInt("NULLABLE")
            val remarks         = resultSet.getString("REMARKS")
            val columnDef       = resultSet.getString("COLUMN_DEF")
            val sqlDataType     = resultSet.getInt("SQL_DATA_TYPE")
            val sqlDatetimeSub  = resultSet.getInt("SQL_DATETIME_SUB")
            val charOctetLength = resultSet.getInt("CHAR_OCTET_LENGTH")
            val ordinalPosition = resultSet.getInt("ORDINAL_POSITION")
            val isNullable      = resultSet.getString("IS_NULLABLE")
            builder += s"$procedureCat, $procedureSchem, $procedureName, $columnName, $columnType, $dataType, $typeName, $precision, $length, $scale, $radix, $nullable, $remarks, $columnDef, $sqlDataType, $sqlDatetimeSub, $charOctetLength, $ordinalPosition, $isNullable"
          builder.result()
      },
      List(
        "connector_test, null, demoSp, inputParam, 1, 12, VARCHAR, 0, 255, 0, 10, 1, null, null, 0, 0, 1020, 1, YES",
        "connector_test, null, demoSp, inOutParam, 2, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 2, YES",
        "connector_test, null, func1, , 5, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 0, YES",
        "connector_test, null, func2, , 5, 12, VARCHAR, 0, 12, 0, 10, 1, null, null, 0, 0, 48, 0, YES",
        "connector_test, null, getPrice, , 5, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 0, YES",
        "connector_test, null, getPrice, price, 1, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "connector_test, null, proc2, param, 1, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "connector_test, null, proc3, param1, 1, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "connector_test, null, proc3, param2, 1, 12, VARCHAR, 0, 8, 0, 10, 1, null, null, 0, 0, 32, 2, YES",
        "connector_test, null, proc4, param1, 4, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "connector_test, null, proc4, param2, 4, 12, VARCHAR, 0, 8, 0, 10, 1, null, null, 0, 0, 32, 2, YES",
        "sys, null, create_synonym_db, in_db_name, 1, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 1, YES",
        "sys, null, create_synonym_db, in_synonym, 1, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 2, YES",
        "sys, null, diagnostics, in_max_runtime, 1, 4, INT UNSIGNED, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, diagnostics, in_interval, 1, 4, INT UNSIGNED, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 2, YES",
        "sys, null, diagnostics, in_auto_config, 1, 1, ENUM, 0, 7, 0, 10, 1, null, null, 0, 0, 28, 3, YES",
        "sys, null, execute_prepared_stmt, in_query, 1, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, null, 0, 0, 2147483647, 1, YES",
        "sys, null, extract_schema_from_file_name, , 5, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 0, YES",
        "sys, null, extract_schema_from_file_name, path, 1, 12, VARCHAR, 0, 512, 0, 10, 1, null, null, 0, 0, 2048, 1, YES",
        "sys, null, extract_table_from_file_name, , 5, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 0, YES",
        "sys, null, extract_table_from_file_name, path, 1, 12, VARCHAR, 0, 512, 0, 10, 1, null, null, 0, 0, 2048, 1, YES",
        "sys, null, format_bytes, , 5, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 0, YES",
        "sys, null, format_bytes, bytes, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 1, YES",
        "sys, null, format_path, , 5, 12, VARCHAR, 0, 512, 0, 10, 1, null, null, 0, 0, 2048, 0, YES",
        "sys, null, format_path, in_path, 1, 12, VARCHAR, 0, 512, 0, 10, 1, null, null, 0, 0, 2048, 1, YES",
        "sys, null, format_statement, , 5, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, null, 0, 0, 2147483647, 0, YES",
        "sys, null, format_statement, statement, 1, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, null, 0, 0, 2147483647, 1, YES",
        "sys, null, format_time, , 5, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 0, YES",
        "sys, null, format_time, picoseconds, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 1, YES",
        "sys, null, list_add, , 5, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 0, YES",
        "sys, null, list_add, in_list, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 1, YES",
        "sys, null, list_add, in_add_value, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 2, YES",
        "sys, null, list_drop, , 5, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 0, YES",
        "sys, null, list_drop, in_list, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 1, YES",
        "sys, null, list_drop, in_drop_value, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 2, YES",
        "sys, null, ps_is_account_enabled, , 5, 1, ENUM, 0, 3, 0, 10, 1, null, null, 0, 0, 12, 0, YES",
        "sys, null, ps_is_account_enabled, in_host, 1, 12, VARCHAR, 0, 255, 0, 10, 1, null, null, 0, 0, 1020, 1, YES",
        "sys, null, ps_is_account_enabled, in_user, 1, 12, VARCHAR, 0, 32, 0, 10, 1, null, null, 0, 0, 128, 2, YES",
        "sys, null, ps_is_consumer_enabled, , 5, 1, ENUM, 0, 3, 0, 10, 1, null, null, 0, 0, 12, 0, YES",
        "sys, null, ps_is_consumer_enabled, in_consumer, 1, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 1, YES",
        "sys, null, ps_is_instrument_default_enabled, , 5, 1, ENUM, 0, 3, 0, 10, 1, null, null, 0, 0, 12, 0, YES",
        "sys, null, ps_is_instrument_default_enabled, in_instrument, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 1, YES",
        "sys, null, ps_is_instrument_default_timed, , 5, 1, ENUM, 0, 3, 0, 10, 1, null, null, 0, 0, 12, 0, YES",
        "sys, null, ps_is_instrument_default_timed, in_instrument, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 1, YES",
        "sys, null, ps_is_thread_instrumented, , 5, 1, ENUM, 0, 7, 0, 10, 1, null, null, 0, 0, 28, 0, YES",
        "sys, null, ps_is_thread_instrumented, in_connection_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_setup_disable_consumer, consumer, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 1, YES",
        "sys, null, ps_setup_disable_instrument, in_pattern, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 1, YES",
        "sys, null, ps_setup_disable_thread, in_connection_id, 1, -5, BIGINT, 19, 19, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_setup_enable_consumer, consumer, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 1, YES",
        "sys, null, ps_setup_enable_instrument, in_pattern, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 1, YES",
        "sys, null, ps_setup_enable_thread, in_connection_id, 1, -5, BIGINT, 19, 19, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_setup_reset_to_default, in_verbose, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_setup_save, in_timeout, 1, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_setup_show_disabled, in_show_instruments, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_setup_show_disabled, in_show_threads, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 2, YES",
        "sys, null, ps_setup_show_enabled, in_show_instruments, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_setup_show_enabled, in_show_threads, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 2, YES",
        "sys, null, ps_thread_account, , 5, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 0, YES",
        "sys, null, ps_thread_account, in_thread_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_thread_id, , 5, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, null, 0, 0, 0, 0, YES",
        "sys, null, ps_thread_id, in_connection_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_thread_stack, , 5, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, null, 0, 0, 2147483647, 0, YES",
        "sys, null, ps_thread_stack, thd_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_thread_stack, debug, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 2, YES",
        "sys, null, ps_thread_trx_info, , 5, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, null, 0, 0, 2147483647, 0, YES",
        "sys, null, ps_thread_trx_info, in_thread_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_trace_statement_digest, in_digest, 1, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 1, YES",
        "sys, null, ps_trace_statement_digest, in_runtime, 1, 4, INT, 10, 10, 0, 10, 1, null, null, 0, 0, 0, 2, YES",
        "sys, null, ps_trace_statement_digest, in_interval, 1, 3, DECIMAL, 2, 2, 2, 10, 1, null, null, 0, 0, 0, 3, YES",
        "sys, null, ps_trace_statement_digest, in_start_fresh, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 4, YES",
        "sys, null, ps_trace_statement_digest, in_auto_enable, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 5, YES",
        "sys, null, ps_trace_thread, in_thread_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, ps_trace_thread, in_outfile, 1, 12, VARCHAR, 0, 255, 0, 10, 1, null, null, 0, 0, 1020, 2, YES",
        "sys, null, ps_trace_thread, in_max_runtime, 1, 3, DECIMAL, 20, 20, 2, 10, 1, null, null, 0, 0, 0, 3, YES",
        "sys, null, ps_trace_thread, in_interval, 1, 3, DECIMAL, 20, 20, 2, 10, 1, null, null, 0, 0, 0, 4, YES",
        "sys, null, ps_trace_thread, in_start_fresh, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 5, YES",
        "sys, null, ps_trace_thread, in_auto_setup, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 6, YES",
        "sys, null, ps_trace_thread, in_debug, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 7, YES",
        "sys, null, ps_truncate_all_tables, in_verbose, 1, -7, BIT, 1, 1, 0, 10, 1, null, null, 0, 0, 0, 1, YES",
        "sys, null, quote_identifier, , 5, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 0, YES",
        "sys, null, quote_identifier, in_identifier, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, null, 0, 0, 65535, 1, YES",
        "sys, null, statement_performance_analyzer, in_action, 1, 1, ENUM, 0, 12, 0, 10, 1, null, null, 0, 0, 48, 1, YES",
        "sys, null, statement_performance_analyzer, in_table, 1, 12, VARCHAR, 0, 129, 0, 10, 1, null, null, 0, 0, 516, 2, YES",
        "sys, null, statement_performance_analyzer, in_views, 1, 1, SET, 0, 124, 0, 10, 1, null, null, 0, 0, 496, 3, YES",
        "sys, null, sys_get_config, , 5, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 0, YES",
        "sys, null, sys_get_config, in_variable_name, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 1, YES",
        "sys, null, sys_get_config, in_default_value, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, null, 0, 0, 512, 2, YES",
        "sys, null, table_exists, in_db, 1, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 1, YES",
        "sys, null, table_exists, in_table, 1, 12, VARCHAR, 0, 64, 0, 10, 1, null, null, 0, 0, 256, 2, YES",
        "sys, null, table_exists, out_exists, 4, 1, ENUM, 0, 10, 0, 10, 1, null, null, 0, 0, 40, 3, YES",
        "sys, null, version_major, , 5, -6, TINYINT UNSIGNED, 3, 3, 0, 10, 1, null, null, 0, 0, 0, 0, YES",
        "sys, null, version_minor, , 5, -6, TINYINT UNSIGNED, 3, 3, 0, 10, 1, null, null, 0, 0, 0, 0, YES",
        "sys, null, version_patch, , 5, -6, TINYINT UNSIGNED, 3, 3, 0, 10, 1, null, null, 0, 0, 0, 0, YES"
      )
    )
  }

  test(s"$prefix: getTables") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getTables(Some("world"), None, None, Array.empty[String])
        yield
          val builder = List.newBuilder[String]
          while resultSet.next() do
            val tableCat               = resultSet.getString("TABLE_CAT")
            val tableSchem             = resultSet.getString("TABLE_SCHEM")
            val tableName              = resultSet.getString("TABLE_NAME")
            val tableType              = resultSet.getString("TABLE_TYPE")
            val remarks                = resultSet.getString("REMARKS")
            val typeCat                = resultSet.getString("TYPE_CAT")
            val typeSchem              = resultSet.getString("TYPE_SCHEM")
            val typeName               = resultSet.getString("TYPE_NAME")
            val selfReferencingColName = resultSet.getString("SELF_REFERENCING_COL_NAME")
            val refGeneration          = resultSet.getString("REF_GENERATION")
            builder += s"$tableCat, $tableSchem, $tableName, $tableType, $remarks, $typeCat, $typeSchem, $typeName, $selfReferencingColName, $refGeneration"
          builder.result()
      },
      List(
        "world, null, city, TABLE, , null, null, null, null, null",
        "world, null, country, TABLE, , null, null, null, null, null",
        "world, null, countrylanguage, TABLE, , null, null, null, null, null",
        "world, null, government_office, TABLE, , null, null, null, null, null"
      )
    )
  }

  test(s"$prefix: getSchemas") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getSchemas()
        yield
          val builder = List.newBuilder[String]
          while resultSet.next() do builder += resultSet.getString("TABLE_SCHEM")
          builder.result()
      },
      List.empty
    )
  }

  test(s"$prefix: getCatalogs") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getCatalogs()
        yield
          val builder = List.newBuilder[String]
          while resultSet.next() do builder += resultSet.getString("TABLE_CAT")
          builder.result()
      },
      List(
        "benchmark",
        "connector_test",
        "information_schema",
        "mysql",
        "performance_schema",
        "sys",
        "world",
        "world2",
        "world3"
      )
    )
  }

  test(s"$prefix: getTableTypes") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield
          val resultSet = metaData.getTableTypes()
          val builder   = List.newBuilder[String]
          while resultSet.next() do builder += resultSet.getString("TABLE_TYPE")
          builder.result()
      },
      List(
        "LOCAL TEMPORARY",
        "SYSTEM TABLE",
        "SYSTEM VIEW",
        "TABLE",
        "VIEW"
      )
    )
  }

  test(s"$prefix: getColumns") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getColumns(None, None, Some("tax"), None)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat        = resultSet.getString("TABLE_CAT")
            val tableSchem      = resultSet.getString("TABLE_SCHEM")
            val tableName       = resultSet.getString("TABLE_NAME")
            val columnName      = resultSet.getString("COLUMN_NAME")
            val dataType        = resultSet.getInt("DATA_TYPE")
            val typeName        = resultSet.getString("TYPE_NAME")
            val columnSize      = resultSet.getInt("COLUMN_SIZE")
            val bufferLength    = resultSet.getInt("BUFFER_LENGTH")
            val decimalDigits   = resultSet.getInt("DECIMAL_DIGITS")
            val numPrecRadix    = resultSet.getInt("NUM_PREC_RADIX")
            val nullable        = resultSet.getInt("NULLABLE")
            val remarks         = resultSet.getString("REMARKS")
            val columnDef       = resultSet.getString("COLUMN_DEF")
            val sqlDataType     = resultSet.getInt("SQL_DATA_TYPE")
            val sqlDatetimeSub  = resultSet.getInt("SQL_DATETIME_SUB")
            val charOctetLength = resultSet.getInt("CHAR_OCTET_LENGTH")
            val ordinalPosition = resultSet.getInt("ORDINAL_POSITION")
            val isNullable      = resultSet.getString("IS_NULLABLE")
            val scopeCatalog    = resultSet.getString("SCOPE_CATALOG")
            val scopeSchema     = resultSet.getString("SCOPE_SCHEMA")
            val scopeTable      = resultSet.getString("SCOPE_TABLE")
            val sourceDataType  = resultSet.getShort("SOURCE_DATA_TYPE")
            val isAutoincrement = resultSet.getString("IS_AUTOINCREMENT")
            builder += s"$tableCat, $tableSchem, $tableName, $columnName, $dataType, $typeName, $columnSize, $bufferLength, $decimalDigits, $numPrecRadix, $nullable, $remarks, $columnDef, $sqlDataType, $sqlDatetimeSub, $charOctetLength, $ordinalPosition, $isNullable, $scopeCatalog, $scopeSchema, $scopeTable, $sourceDataType, $isAutoincrement"
          builder.result()
      },
      Vector(
        "connector_test, null, tax, id, -5, BIGINT, 19, 65535, 0, 10, 0, , null, 0, 0, 0, 1, NO, null, null, null, 0, YES",
        "connector_test, null, tax, value, 8, DOUBLE, 22, 65535, 0, 10, 0, , null, 0, 0, 0, 2, NO, null, null, null, 0, NO",
        "connector_test, null, tax, start_date, 91, DATE, 10, 65535, 0, 10, 0, , null, 0, 0, 0, 3, NO, null, null, null, 0, NO"
      )
    )
  }

  test(s"$prefix: getColumnPrivileges") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getColumnPrivileges(Some("connector_test"), None, Some("tax"), Some("id"))
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat    = resultSet.getString("TABLE_CAT")
            val tableSchem  = resultSet.getString("TABLE_SCHEM")
            val tableName   = resultSet.getString("TABLE_NAME")
            val columnName  = resultSet.getString("COLUMN_NAME")
            val grantor     = resultSet.getString("GRANTOR")
            val grantee     = resultSet.getString("GRANTEE")
            val privilege   = resultSet.getString("PRIVILEGE")
            val isGrantable = resultSet.getString("IS_GRANTABLE")
            builder += s"$tableCat, $tableSchem, $tableName, $columnName, $grantor, $grantee, $privilege, $isGrantable"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getTablePrivileges") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getTablePrivileges(None, None, Some("tax"))
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat    = resultSet.getString("TABLE_CAT")
            val tableSchem  = resultSet.getString("TABLE_SCHEM")
            val tableName   = resultSet.getString("TABLE_NAME")
            val grantor     = resultSet.getString("GRANTOR")
            val grantee     = resultSet.getString("GRANTEE")
            val privilege   = resultSet.getString("PRIVILEGE")
            val isGrantable = resultSet.getString("IS_GRANTABLE")
            builder += s"$tableCat, $tableSchem, $tableName, $grantor, $grantee, $privilege, $isGrantable"
          builder.result()
      },
      Vector.empty
    )
  }

  test(s"$prefix: getBestRowIdentifier") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getBestRowIdentifier(None, None, "tax", Some(1), Some(true))
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val scope         = resultSet.getShort("SCOPE")
            val columnName    = resultSet.getString("COLUMN_NAME")
            val dataType      = resultSet.getInt("DATA_TYPE")
            val typeName      = resultSet.getString("TYPE_NAME")
            val columnSize    = resultSet.getInt("COLUMN_SIZE")
            val bufferLength  = resultSet.getInt("BUFFER_LENGTH")
            val decimalDigits = resultSet.getShort("DECIMAL_DIGITS")
            val pseudoColumn  = resultSet.getShort("PSEUDO_COLUMN")
            builder += s"$scope, $columnName, $dataType, $typeName, $columnSize, $bufferLength, $decimalDigits, $pseudoColumn"
          builder.result()
      },
      Vector("2, id, -5, bigint, 19, 65535, 0, 1")
    )
  }

  test(s"$prefix: getVersionColumns") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getVersionColumns(None, None, "tax")
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val scope         = resultSet.getShort("SCOPE")
            val columnName    = resultSet.getString("COLUMN_NAME")
            val dataType      = resultSet.getInt("DATA_TYPE")
            val typeName      = resultSet.getString("TYPE_NAME")
            val columnSize    = resultSet.getInt("COLUMN_SIZE")
            val bufferLength  = resultSet.getInt("BUFFER_LENGTH")
            val decimalDigits = resultSet.getShort("DECIMAL_DIGITS")
            val pseudoColumn  = resultSet.getShort("PSEUDO_COLUMN")
            builder += s"$scope, $columnName, $dataType, $typeName, $columnSize, $bufferLength, $decimalDigits, $pseudoColumn"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getPrimaryKeys") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getPrimaryKeys(None, None, "tax")
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat   = resultSet.getString("TABLE_CAT")
            val tableSchem = resultSet.getString("TABLE_SCHEM")
            val tableName  = resultSet.getString("TABLE_NAME")
            val columnName = resultSet.getString("COLUMN_NAME")
            val keySeq     = resultSet.getShort("KEY_SEQ")
            val pkName     = resultSet.getString("PK_NAME")
            builder += s"$tableCat, $tableSchem, $tableName, $columnName, $keySeq, $pkName"
          builder.result()
      },
      Vector("connector_test, null, tax, id, 1, PRIMARY")
    )
  }

  test(s"$prefix: getImportedKeys") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getImportedKeys(None, None, "tax")
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val pkTableCat    = resultSet.getString("PKTABLE_CAT")
            val pkTableSchem  = resultSet.getString("PKTABLE_SCHEM")
            val pkTableName   = resultSet.getString("PKTABLE_NAME")
            val pkColumnName  = resultSet.getString("PKCOLUMN_NAME")
            val fkTableCat    = resultSet.getString("FKTABLE_CAT")
            val fkTableSchem  = resultSet.getString("FKTABLE_SCHEM")
            val fkTableName   = resultSet.getString("FKTABLE_NAME")
            val fkColumnName  = resultSet.getString("FKCOLUMN_NAME")
            val keySeq        = resultSet.getShort("KEY_SEQ")
            val updateRule    = resultSet.getShort("UPDATE_RULE")
            val deleteRule    = resultSet.getShort("DELETE_RULE")
            val fkName        = resultSet.getString("FK_NAME")
            val pkName        = resultSet.getString("PK_NAME")
            val deferrability = resultSet.getShort("DEFERRABILITY")
            builder += s"$pkTableCat, $pkTableSchem, $pkTableName, $pkColumnName, $fkTableCat, $fkTableSchem, $fkTableName, $fkColumnName, $keySeq, $updateRule, $deleteRule, $fkName, $pkName, $deferrability"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getExportedKeys") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getExportedKeys(None, None, "tax")
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val pkTableCat    = resultSet.getString("PKTABLE_CAT")
            val pkTableSchem  = resultSet.getString("PKTABLE_SCHEM")
            val pkTableName   = resultSet.getString("PKTABLE_NAME")
            val pkColumnName  = resultSet.getString("PKCOLUMN_NAME")
            val fkTableCat    = resultSet.getString("FKTABLE_CAT")
            val fkTableSchem  = resultSet.getString("FKTABLE_SCHEM")
            val fkTableName   = resultSet.getString("FKTABLE_NAME")
            val fkColumnName  = resultSet.getString("FKCOLUMN_NAME")
            val keySeq        = resultSet.getShort("KEY_SEQ")
            val updateRule    = resultSet.getShort("UPDATE_RULE")
            val deleteRule    = resultSet.getShort("DELETE_RULE")
            val fkName        = resultSet.getString("FK_NAME")
            val pkName        = resultSet.getString("PK_NAME")
            val deferrability = resultSet.getShort("DEFERRABILITY")
            builder += s"$pkTableCat, $pkTableSchem, $pkTableName, $pkColumnName, $fkTableCat, $fkTableSchem, $fkTableName, $fkColumnName, $keySeq, $updateRule, $deleteRule, $fkName, $pkName, $deferrability"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getCrossReference") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getCrossReference(None, None, "tax", None, None, Some("film"))
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val pkTableCat    = resultSet.getString("PKTABLE_CAT")
            val pkTableSchem  = resultSet.getString("PKTABLE_SCHEM")
            val pkTableName   = resultSet.getString("PKTABLE_NAME")
            val pkColumnName  = resultSet.getString("PKCOLUMN_NAME")
            val fkTableCat    = resultSet.getString("FKTABLE_CAT")
            val fkTableSchem  = resultSet.getString("FKTABLE_SCHEM")
            val fkTableName   = resultSet.getString("FKTABLE_NAME")
            val fkColumnName  = resultSet.getString("FKCOLUMN_NAME")
            val keySeq        = resultSet.getShort("KEY_SEQ")
            val updateRule    = resultSet.getShort("UPDATE_RULE")
            val deleteRule    = resultSet.getShort("DELETE_RULE")
            val fkName        = resultSet.getString("FK_NAME")
            val pkName        = resultSet.getString("PK_NAME")
            val deferrability = resultSet.getShort("DEFERRABILITY")
            builder += s"$pkTableCat, $pkTableSchem, $pkTableName, $pkColumnName, $fkTableCat, $fkTableSchem, $fkTableName, $fkColumnName, $keySeq, $updateRule, $deleteRule, $fkName, $pkName, $deferrability"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getIndexInfo") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getIndexInfo(None, None, Some("tax"), false, false)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat        = resultSet.getString("TABLE_CAT")
            val tableSchem      = resultSet.getString("TABLE_SCHEM")
            val tableName       = resultSet.getString("TABLE_NAME")
            val nonUnique       = resultSet.getBoolean("NON_UNIQUE")
            val indexQualifier  = resultSet.getString("INDEX_QUALIFIER")
            val indexName       = resultSet.getString("INDEX_NAME")
            val typed           = resultSet.getShort("TYPE")
            val ordinalPosition = resultSet.getInt("ORDINAL_POSITION")
            val columnName      = resultSet.getString("COLUMN_NAME")
            val ascOrDesc       = resultSet.getString("ASC_OR_DESC")
            val pages           = resultSet.getInt("PAGES")
            val filterCondition = resultSet.getString("FILTER_CONDITION")
            builder += s"$tableCat, $tableSchem, $tableName, $nonUnique, $indexQualifier, $indexName, $typed, $ordinalPosition, $columnName, $ascOrDesc, $pages, $filterCondition"
          builder.result()
      },
      Vector(
        "connector_test, null, tax, false, null, PRIMARY, 3, 1, id, A, 0, null"
      )
    )
  }

  test(s"$prefix: supportsResultSetType") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY)
      },
      true
    )
  }

  test(s"$prefix: supportsResultSetConcurrency") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      },
      true
    )
  }

  test(s"$prefix: ownUpdatesAreVisible") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.ownUpdatesAreVisible(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: ownDeletesAreVisible") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.ownDeletesAreVisible(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: ownInsertsAreVisible") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.ownInsertsAreVisible(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: othersUpdatesAreVisible") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.othersUpdatesAreVisible(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: othersDeletesAreVisible") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.othersDeletesAreVisible(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: othersInsertsAreVisible") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.othersInsertsAreVisible(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: updatesAreDetected") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.updatesAreDetected(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: deletesAreDetected") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.deletesAreDetected(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: insertsAreDetected") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.insertsAreDetected(ResultSet.TYPE_FORWARD_ONLY)
      },
      false
    )
  }

  test(s"$prefix: supportsBatchUpdates") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsBatchUpdates()
      },
      true
    )
  }

  test(s"$prefix: getUDTs") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield
          val resultSet = metaData.getUDTs(None, None, None, Array.empty[Int])
          val builder   = Vector.newBuilder[String]
          while resultSet.next() do
            val typeCat   = resultSet.getString("TYPE_CAT")
            val typeSchem = resultSet.getString("TYPE_SCHEM")
            val typeName  = resultSet.getString("TYPE_NAME")
            val className = resultSet.getString("CLASS_NAME")
            val dataType  = resultSet.getInt("DATA_TYPE")
            val remarks   = resultSet.getString("REMARKS")
            val baseType  = resultSet.getShort("BASE_TYPE")
            builder += s"$typeCat, $typeSchem, $typeName, $className, $dataType, $remarks, $baseType"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: supportsSavepoints") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSavepoints()
      },
      true
    )
  }

  test(s"$prefix: supportsNamedParameters") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsNamedParameters()
      },
      false
    )
  }

  test(s"$prefix: supportsMultipleOpenResults") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsMultipleOpenResults()
      },
      true
    )
  }

  test(s"$prefix: supportsGetGeneratedKeys") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsGetGeneratedKeys()
      },
      true
    )
  }

  test(s"$prefix: getSuperTypes") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield
          val resultSet = metaData.getSuperTypes(None, None, None)
          val builder   = Vector.newBuilder[String]
          while resultSet.next() do
            val typeCat        = resultSet.getString("TYPE_CAT")
            val typeSchem      = resultSet.getString("TYPE_SCHEM")
            val typeName       = resultSet.getString("TYPE_NAME")
            val superTypeCat   = resultSet.getString("SUPERTYPE_CAT")
            val superTypeSchem = resultSet.getString("SUPERTYPE_SCHEM")
            val superTypeName  = resultSet.getString("SUPERTYPE_NAME")
            builder += s"$typeCat, $typeSchem, $typeName, $superTypeCat, $superTypeSchem, $superTypeName"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getSuperTables") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield
          val resultSet = metaData.getSuperTables(None, None, None)
          val builder   = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat       = resultSet.getString("TABLE_CAT")
            val tableSchem     = resultSet.getString("TABLE_SCHEM")
            val tableName      = resultSet.getString("TABLE_NAME")
            val superTableName = resultSet.getString("SUPERTABLE_NAME")
            builder += s"$tableCat, $tableSchem, $tableName, $superTableName"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getAttributes") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield
          val resultSet = metaData.getAttributes(None, None, None, None)
          val builder   = Vector.newBuilder[String]
          while resultSet.next() do
            val typeName          = resultSet.getString("TYPE_NAME")
            val attributeName     = resultSet.getString("ATTR_NAME")
            val attributeType     = resultSet.getString("DATA_TYPE")
            val attributeTypeName = resultSet.getString("ATTR_TYPE_NAME")
            val attributeSize     = resultSet.getInt("ATTR_SIZE")
            val decimalDigits     = resultSet.getInt("DECIMAL_DIGITS")
            val numPrecRadix      = resultSet.getInt("NUM_PREC_RADIX")
            val nullable          = resultSet.getInt("NULLABLE")
            val remarks           = resultSet.getString("REMARKS")
            val attrDef           = resultSet.getString("ATTR_DEF")
            val sqlDataType       = resultSet.getInt("SQL_DATA_TYPE")
            val sqlDateTimeSub    = resultSet.getInt("SQL_DATETIME_SUB")
            val charOctetLength   = resultSet.getInt("CHAR_OCTET_LENGTH")
            val ordinalPosition   = resultSet.getInt("ORDINAL_POSITION")
            val isNullable        = resultSet.getString("IS_NULLABLE")
            builder += s"$typeName, $attributeName, $attributeType, $attributeTypeName, $attributeSize, $decimalDigits, $numPrecRadix, $nullable, $remarks, $attrDef, $sqlDataType, $sqlDateTimeSub, $charOctetLength, $ordinalPosition, $isNullable"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: supportsResultSetHoldability") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT)
      },
      true
    )
  }

  test(s"$prefix: getResultSetHoldability") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getResultSetHoldability()
      },
      1
    )
  }

  test(s"$prefix: getDatabaseMajorVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDatabaseMajorVersion()
      },
      8
    )
  }

  test(s"$prefix: getDatabaseMinorVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getDatabaseMinorVersion()
      },
      4
    )
  }

  test(s"$prefix: getJDBCMajorVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getJDBCMajorVersion()
      },
      if prefix == "jdbc" then 4 else 0
    )
  }

  test(s"$prefix: getJDBCMinorVersion") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getJDBCMinorVersion()
      },
      if prefix == "jdbc" then 2 else 3
    )
  }

  test(s"$prefix: getSQLStateType") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getSQLStateType()
      },
      2
    )
  }

  test(s"$prefix: locatorsUpdateCopy") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.locatorsUpdateCopy()
      },
      true
    )
  }

  test(s"$prefix: supportsStatementPooling") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsStatementPooling()
      },
      false
    )
  }

  test(s"$prefix: getRowIdLifetime") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getRowIdLifetime()
      },
      RowIdLifetime.ROWID_UNSUPPORTED
    )
  }

  test(s"$prefix: getSchemas") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getSchemas(None, None)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableSchem   = resultSet.getString("TABLE_SCHEM")
            val tableCatalog = resultSet.getString("TABLE_CATALOG")
            builder += s"$tableSchem, $tableCatalog"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: supportsStoredFunctionsUsingCallSyntax") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsStoredFunctionsUsingCallSyntax()
      },
      true
    )
  }

  test(s"$prefix: autoCommitFailureClosesAllResultSets") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.autoCommitFailureClosesAllResultSets()
      },
      false
    )
  }

  test(s"$prefix: getClientInfoProperties") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield
          val resultSet = metaData.getClientInfoProperties()
          val builder   = Vector.newBuilder[String]
          while resultSet.next() do
            val name         = resultSet.getString("NAME")
            val maxLen       = resultSet.getInt("MAX_LEN")
            val defaultValue = resultSet.getString("DEFAULT_VALUE")
            val description  = resultSet.getString("DESCRIPTION")
            builder += s"$name, $maxLen, $defaultValue, $description"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: getFunctions") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getFunctions(None, None, None)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val functionCat   = resultSet.getString("FUNCTION_CAT")
            val functionSchem = resultSet.getString("FUNCTION_SCHEM")
            val functionName  = resultSet.getString("FUNCTION_NAME")
            val functionType  = resultSet.getInt("FUNCTION_TYPE")
            val specificName  = resultSet.getString("SPECIFIC_NAME")
            builder += s"$functionCat, $functionSchem, $functionName, $functionType, $specificName"
          builder.result()
      },
      Vector(
        "connector_test, null, func1, 1, func1",
        "connector_test, null, func2, 1, func2",
        "connector_test, null, getPrice, 1, getPrice",
        "sys, null, extract_schema_from_file_name, 1, extract_schema_from_file_name",
        "sys, null, extract_table_from_file_name, 1, extract_table_from_file_name",
        "sys, null, format_bytes, 1, format_bytes",
        "sys, null, format_path, 1, format_path",
        "sys, null, format_statement, 1, format_statement",
        "sys, null, format_time, 1, format_time",
        "sys, null, list_add, 1, list_add",
        "sys, null, list_drop, 1, list_drop",
        "sys, null, ps_is_account_enabled, 1, ps_is_account_enabled",
        "sys, null, ps_is_consumer_enabled, 1, ps_is_consumer_enabled",
        "sys, null, ps_is_instrument_default_enabled, 1, ps_is_instrument_default_enabled",
        "sys, null, ps_is_instrument_default_timed, 1, ps_is_instrument_default_timed",
        "sys, null, ps_is_thread_instrumented, 1, ps_is_thread_instrumented",
        "sys, null, ps_thread_account, 1, ps_thread_account",
        "sys, null, ps_thread_id, 1, ps_thread_id",
        "sys, null, ps_thread_stack, 1, ps_thread_stack",
        "sys, null, ps_thread_trx_info, 1, ps_thread_trx_info",
        "sys, null, quote_identifier, 1, quote_identifier",
        "sys, null, sys_get_config, 1, sys_get_config",
        "sys, null, version_major, 1, version_major",
        "sys, null, version_minor, 1, version_minor",
        "sys, null, version_patch, 1, version_patch"
      )
    )
  }

  test(s"$prefix: getFunctionColumns") {
    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getFunctionColumns(None, None, None, None)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val functionCat     = resultSet.getString("FUNCTION_CAT")
            val functionSchem   = resultSet.getString("FUNCTION_SCHEM")
            val functionName    = resultSet.getString("FUNCTION_NAME")
            val columnName      = resultSet.getString("COLUMN_NAME")
            val columnType      = resultSet.getInt("COLUMN_TYPE")
            val dataType        = resultSet.getInt("DATA_TYPE")
            val typeName        = resultSet.getString("TYPE_NAME")
            val precision       = resultSet.getInt("PRECISION")
            val length          = resultSet.getInt("LENGTH")
            val scale           = resultSet.getInt("SCALE")
            val radix           = resultSet.getInt("RADIX")
            val nullable        = resultSet.getInt("NULLABLE")
            val remarks         = resultSet.getString("REMARKS")
            val charOctetLength = resultSet.getInt("CHAR_OCTET_LENGTH")
            val ordinalPosition = resultSet.getInt("ORDINAL_POSITION")
            val isNullable      = resultSet.getString("IS_NULLABLE")
            builder += s"$functionCat, $functionSchem, $functionName, $columnName, $columnType, $dataType, $typeName, $precision, $length, $scale, $radix, $nullable, $remarks, $charOctetLength, $ordinalPosition, $isNullable"
          builder.result()
      },
      Vector(
        "connector_test, null, func1, , 4, 4, INT, 10, 10, 0, 10, 1, null, 0, 0, YES",
        "connector_test, null, func2, , 4, 12, VARCHAR, 0, 12, 0, 10, 1, null, 48, 0, YES",
        "connector_test, null, getPrice, , 4, 4, INT, 10, 10, 0, 10, 1, null, 0, 0, YES",
        "connector_test, null, getPrice, price, 1, 4, INT, 10, 10, 0, 10, 1, null, 0, 1, YES",
        "sys, null, extract_schema_from_file_name, , 4, 12, VARCHAR, 0, 64, 0, 10, 1, null, 256, 0, YES",
        "sys, null, extract_schema_from_file_name, path, 1, 12, VARCHAR, 0, 512, 0, 10, 1, null, 2048, 1, YES",
        "sys, null, extract_table_from_file_name, , 4, 12, VARCHAR, 0, 64, 0, 10, 1, null, 256, 0, YES",
        "sys, null, extract_table_from_file_name, path, 1, 12, VARCHAR, 0, 512, 0, 10, 1, null, 2048, 1, YES",
        "sys, null, format_bytes, , 4, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 0, YES",
        "sys, null, format_bytes, bytes, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 1, YES",
        "sys, null, format_path, , 4, 12, VARCHAR, 0, 512, 0, 10, 1, null, 2048, 0, YES",
        "sys, null, format_path, in_path, 1, 12, VARCHAR, 0, 512, 0, 10, 1, null, 2048, 1, YES",
        "sys, null, format_statement, , 4, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, 2147483647, 0, YES",
        "sys, null, format_statement, statement, 1, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, 2147483647, 1, YES",
        "sys, null, format_time, , 4, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 0, YES",
        "sys, null, format_time, picoseconds, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 1, YES",
        "sys, null, list_add, , 4, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 0, YES",
        "sys, null, list_add, in_list, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 1, YES",
        "sys, null, list_add, in_add_value, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 2, YES",
        "sys, null, list_drop, , 4, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 0, YES",
        "sys, null, list_drop, in_list, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 1, YES",
        "sys, null, list_drop, in_drop_value, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 2, YES",
        "sys, null, ps_is_account_enabled, , 4, 1, ENUM, 0, 3, 0, 10, 1, null, 12, 0, YES",
        "sys, null, ps_is_account_enabled, in_host, 1, 12, VARCHAR, 0, 255, 0, 10, 1, null, 1020, 1, YES",
        "sys, null, ps_is_account_enabled, in_user, 1, 12, VARCHAR, 0, 32, 0, 10, 1, null, 128, 2, YES",
        "sys, null, ps_is_consumer_enabled, , 4, 1, ENUM, 0, 3, 0, 10, 1, null, 12, 0, YES",
        "sys, null, ps_is_consumer_enabled, in_consumer, 1, 12, VARCHAR, 0, 64, 0, 10, 1, null, 256, 1, YES",
        "sys, null, ps_is_instrument_default_enabled, , 4, 1, ENUM, 0, 3, 0, 10, 1, null, 12, 0, YES",
        "sys, null, ps_is_instrument_default_enabled, in_instrument, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, 512, 1, YES",
        "sys, null, ps_is_instrument_default_timed, , 4, 1, ENUM, 0, 3, 0, 10, 1, null, 12, 0, YES",
        "sys, null, ps_is_instrument_default_timed, in_instrument, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, 512, 1, YES",
        "sys, null, ps_is_thread_instrumented, , 4, 1, ENUM, 0, 7, 0, 10, 1, null, 28, 0, YES",
        "sys, null, ps_is_thread_instrumented, in_connection_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, 0, 1, YES",
        "sys, null, ps_thread_account, , 4, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 0, YES",
        "sys, null, ps_thread_account, in_thread_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, 0, 1, YES",
        "sys, null, ps_thread_id, , 4, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, 0, 0, YES",
        "sys, null, ps_thread_id, in_connection_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, 0, 1, YES",
        "sys, null, ps_thread_stack, , 4, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, 2147483647, 0, YES",
        "sys, null, ps_thread_stack, thd_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, 0, 1, YES",
        "sys, null, ps_thread_stack, debug, 1, -7, BIT, 1, 1, 0, 10, 1, null, 0, 2, YES",
        "sys, null, ps_thread_trx_info, , 4, -1, LONGTEXT, 0, 2147483647, 0, 10, 1, null, 2147483647, 0, YES",
        "sys, null, ps_thread_trx_info, in_thread_id, 1, -5, BIGINT UNSIGNED, 20, 20, 0, 10, 1, null, 0, 1, YES",
        "sys, null, quote_identifier, , 4, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 0, YES",
        "sys, null, quote_identifier, in_identifier, 1, -1, TEXT, 0, 65535, 0, 10, 1, null, 65535, 1, YES",
        "sys, null, sys_get_config, , 4, 12, VARCHAR, 0, 128, 0, 10, 1, null, 512, 0, YES",
        "sys, null, sys_get_config, in_variable_name, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, 512, 1, YES",
        "sys, null, sys_get_config, in_default_value, 1, 12, VARCHAR, 0, 128, 0, 10, 1, null, 512, 2, YES",
        "sys, null, version_major, , 4, -6, TINYINT UNSIGNED, 3, 3, 0, 10, 1, null, 0, 0, YES",
        "sys, null, version_minor, , 4, -6, TINYINT UNSIGNED, 3, 3, 0, 10, 1, null, 0, 0, YES",
        "sys, null, version_patch, , 4, -6, TINYINT UNSIGNED, 3, 3, 0, 10, 1, null, 0, 0, YES"
      )
    )
  }

  test(s"$prefix: getPseudoColumns") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield
          val resultSet = metaData.getPseudoColumns(None, None, None, None)
          val builder   = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat        = resultSet.getString("TABLE_CAT")
            val tableSchem      = resultSet.getString("TABLE_SCHEM")
            val tableName       = resultSet.getString("TABLE_NAME")
            val columnName      = resultSet.getString("COLUMN_NAME")
            val dataType        = resultSet.getInt("DATA_TYPE")
            val columnSize      = resultSet.getInt("COLUMN_SIZE")
            val decimalDigits   = resultSet.getInt("DECIMAL_DIGITS")
            val numPrecRadix    = resultSet.getInt("NUM_PREC_RADIX")
            val columnUsage     = resultSet.getString("COLUMN_USAGE")
            val remarks         = resultSet.getString("REMARKS")
            val charOctetLength = resultSet.getInt("CHAR_OCTET_LENGTH")
            val isNullable      = resultSet.getInt("IS_NULLABLE")
            builder += s"$tableCat, $tableSchem, $tableName, $columnName, $dataType, $columnSize, $decimalDigits, $numPrecRadix, $columnUsage, $remarks, $charOctetLength, $isNullable"
          builder.result()
      },
      Vector()
    )
  }

  test(s"$prefix: generatedKeyAlwaysReturned") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.generatedKeyAlwaysReturned()
      },
      true
    )
  }

  test(s"$prefix: getMaxLogicalLobSize") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.getMaxLogicalLobSize()
      },
      0L
    )
  }

  test(s"$prefix: supportsRefCursors") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsRefCursors()
      },
      false
    )
  }

  test(s"$prefix: supportsSharding") {
    assertIO(
      connection.use { conn =>
        for metaData <- conn.getMetaData()
        yield metaData.supportsSharding()
      },
      false
    )
  }
