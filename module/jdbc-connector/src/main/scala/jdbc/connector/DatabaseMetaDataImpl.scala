/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import cats.syntax.all.*

import cats.effect.Sync

import ldbc.sql.{ Connection, DatabaseMetaData, ResultSet, RowIdLifetime }

private[jdbc] case class DatabaseMetaDataImpl[F[_]: Sync](metaData: java.sql.DatabaseMetaData)
  extends DatabaseMetaData[F]:

  override def allProceduresAreCallable(): Boolean = metaData.allProceduresAreCallable

  override def allTablesAreSelectable(): Boolean = metaData.allTablesAreSelectable

  override def getURL(): String = metaData.getURL

  override def getUserName(): F[String] = Sync[F].blocking(metaData.getUserName)

  override def isReadOnly(): Boolean = metaData.isReadOnly

  override def nullsAreSortedHigh(): Boolean = metaData.nullsAreSortedHigh

  override def nullsAreSortedLow(): Boolean = metaData.nullsAreSortedLow

  override def nullsAreSortedAtStart(): Boolean = metaData.nullsAreSortedAtStart

  override def nullsAreSortedAtEnd(): Boolean = metaData.nullsAreSortedAtEnd

  override def getDatabaseProductName(): String = metaData.getDatabaseProductName

  override def getDatabaseProductVersion(): String = metaData.getDatabaseProductVersion

  override def getDriverName(): String = metaData.getDriverName

  override def getDriverVersion(): String = metaData.getDriverVersion

  override def getDriverMajorVersion(): Int = metaData.getDriverMajorVersion

  override def getDriverMinorVersion(): Int = metaData.getDriverMinorVersion

  override def usesLocalFiles(): Boolean = metaData.usesLocalFiles

  override def usesLocalFilePerTable(): Boolean = metaData.usesLocalFilePerTable

  override def supportsMixedCaseIdentifiers(): Boolean = metaData.supportsMixedCaseIdentifiers

  override def storesUpperCaseIdentifiers(): Boolean = metaData.storesUpperCaseIdentifiers

  override def storesLowerCaseIdentifiers(): Boolean = metaData.storesLowerCaseIdentifiers

  override def storesMixedCaseIdentifiers(): Boolean = metaData.storesMixedCaseIdentifiers

  override def supportsMixedCaseQuotedIdentifiers(): Boolean = metaData.supportsMixedCaseQuotedIdentifiers

  override def storesUpperCaseQuotedIdentifiers(): Boolean = metaData.storesUpperCaseQuotedIdentifiers

  override def storesLowerCaseQuotedIdentifiers(): Boolean = metaData.storesLowerCaseQuotedIdentifiers

  override def storesMixedCaseQuotedIdentifiers(): Boolean = metaData.storesMixedCaseQuotedIdentifiers

  override def getIdentifierQuoteString(): String = metaData.getIdentifierQuoteString

  override def getSQLKeywords(): F[String] = Sync[F].blocking(metaData.getSQLKeywords)

  override def getNumericFunctions(): String = metaData.getNumericFunctions

  override def getStringFunctions(): String = metaData.getStringFunctions

  override def getSystemFunctions(): String = metaData.getSystemFunctions

  override def getTimeDateFunctions(): String = metaData.getTimeDateFunctions

  override def getSearchStringEscape(): String = metaData.getSearchStringEscape

  override def getExtraNameCharacters(): String = metaData.getExtraNameCharacters

  override def supportsAlterTableWithAddColumn(): Boolean = metaData.supportsAlterTableWithAddColumn

  override def supportsAlterTableWithDropColumn(): Boolean = metaData.supportsAlterTableWithDropColumn

  override def supportsColumnAliasing(): Boolean = metaData.supportsColumnAliasing

  override def nullPlusNonNullIsNull(): Boolean = metaData.nullPlusNonNullIsNull

  override def supportsConvert(): Boolean = metaData.supportsConvert

  override def supportsConvert(fromType: Int, toType: Int): Boolean = metaData.supportsConvert(fromType, toType)

  override def supportsTableCorrelationNames(): Boolean = metaData.supportsTableCorrelationNames

  override def supportsDifferentTableCorrelationNames(): Boolean = metaData.supportsDifferentTableCorrelationNames

  override def supportsExpressionsInOrderBy(): Boolean = metaData.supportsExpressionsInOrderBy

  override def supportsOrderByUnrelated(): Boolean = metaData.supportsOrderByUnrelated

  override def supportsGroupBy(): Boolean = metaData.supportsGroupBy

  override def supportsGroupByUnrelated(): Boolean = metaData.supportsGroupByUnrelated

  override def supportsGroupByBeyondSelect(): Boolean = metaData.supportsGroupByBeyondSelect

  override def supportsLikeEscapeClause(): Boolean = metaData.supportsLikeEscapeClause

  override def supportsMultipleResultSets(): Boolean = metaData.supportsMultipleResultSets

  override def supportsMultipleTransactions(): Boolean = metaData.supportsMultipleTransactions

  override def supportsNonNullableColumns(): Boolean = metaData.supportsNonNullableColumns

  override def supportsMinimumSQLGrammar(): Boolean = metaData.supportsMinimumSQLGrammar

  override def supportsCoreSQLGrammar(): Boolean = metaData.supportsCoreSQLGrammar

  override def supportsExtendedSQLGrammar(): Boolean = metaData.supportsExtendedSQLGrammar

  override def supportsANSI92EntryLevelSQL(): Boolean = metaData.supportsANSI92EntryLevelSQL

  override def supportsANSI92IntermediateSQL(): Boolean = metaData.supportsANSI92IntermediateSQL

  override def supportsANSI92FullSQL(): Boolean = metaData.supportsANSI92FullSQL

  override def supportsIntegrityEnhancementFacility(): Boolean = metaData.supportsIntegrityEnhancementFacility

  override def supportsOuterJoins(): Boolean = metaData.supportsOuterJoins

  override def supportsFullOuterJoins(): Boolean = metaData.supportsFullOuterJoins

  override def supportsLimitedOuterJoins(): Boolean = metaData.supportsLimitedOuterJoins

  override def getSchemaTerm(): String = metaData.getSchemaTerm

  override def getProcedureTerm(): String = metaData.getProcedureTerm

  override def getCatalogTerm(): String = metaData.getCatalogTerm

  override def isCatalogAtStart(): Boolean = metaData.isCatalogAtStart

  override def getCatalogSeparator(): String = metaData.getCatalogSeparator

  override def supportsSchemasInDataManipulation(): Boolean = metaData.supportsSchemasInDataManipulation

  override def supportsSchemasInProcedureCalls(): Boolean = metaData.supportsSchemasInProcedureCalls

  override def supportsSchemasInTableDefinitions(): Boolean = metaData.supportsSchemasInTableDefinitions

  override def supportsSchemasInIndexDefinitions(): Boolean = metaData.supportsSchemasInIndexDefinitions

  override def supportsSchemasInPrivilegeDefinitions(): Boolean = metaData.supportsSchemasInPrivilegeDefinitions

  override def supportsCatalogsInDataManipulation(): Boolean = metaData.supportsCatalogsInDataManipulation

  override def supportsCatalogsInProcedureCalls(): Boolean = metaData.supportsCatalogsInProcedureCalls

  override def supportsCatalogsInTableDefinitions(): Boolean = metaData.supportsCatalogsInTableDefinitions

  override def supportsCatalogsInIndexDefinitions(): Boolean = metaData.supportsCatalogsInIndexDefinitions

  override def supportsCatalogsInPrivilegeDefinitions(): Boolean = metaData.supportsCatalogsInPrivilegeDefinitions

  override def supportsPositionedDelete(): Boolean = metaData.supportsPositionedDelete

  override def supportsPositionedUpdate(): Boolean = metaData.supportsPositionedUpdate

  override def supportsSelectForUpdate(): Boolean = metaData.supportsSelectForUpdate

  override def supportsStoredProcedures(): Boolean = metaData.supportsStoredProcedures

  override def supportsSubqueriesInComparisons(): Boolean = metaData.supportsSubqueriesInComparisons

  override def supportsSubqueriesInExists(): Boolean = metaData.supportsSubqueriesInExists

  override def supportsSubqueriesInIns(): Boolean = metaData.supportsSubqueriesInIns

  override def supportsSubqueriesInQuantifieds(): Boolean = metaData.supportsSubqueriesInQuantifieds

  override def supportsCorrelatedSubqueries(): Boolean = metaData.supportsCorrelatedSubqueries

  override def supportsUnion(): Boolean = metaData.supportsUnion

  override def supportsUnionAll(): Boolean = metaData.supportsUnionAll

  override def supportsOpenCursorsAcrossCommit(): Boolean = metaData.supportsOpenCursorsAcrossCommit

  override def supportsOpenCursorsAcrossRollback(): Boolean = metaData.supportsOpenCursorsAcrossRollback

  override def supportsOpenStatementsAcrossCommit(): Boolean = metaData.supportsOpenStatementsAcrossCommit

  override def supportsOpenStatementsAcrossRollback(): Boolean = metaData.supportsOpenStatementsAcrossRollback

  override def getMaxBinaryLiteralLength(): Int = metaData.getMaxBinaryLiteralLength

  override def getMaxCharLiteralLength(): Int = metaData.getMaxCharLiteralLength

  override def getMaxColumnNameLength(): Int = metaData.getMaxColumnNameLength

  override def getMaxColumnsInGroupBy(): Int = metaData.getMaxColumnsInGroupBy

  override def getMaxColumnsInIndex(): Int = metaData.getMaxColumnsInIndex

  override def getMaxColumnsInOrderBy(): Int = metaData.getMaxColumnsInOrderBy

  override def getMaxColumnsInSelect(): Int = metaData.getMaxColumnsInSelect

  override def getMaxColumnsInTable(): Int = metaData.getMaxColumnsInTable

  override def getMaxConnections(): Int = metaData.getMaxConnections

  override def getMaxCursorNameLength(): Int = metaData.getMaxCursorNameLength

  override def getMaxIndexLength(): Int = metaData.getMaxIndexLength

  override def getMaxSchemaNameLength(): Int = metaData.getMaxSchemaNameLength

  override def getMaxProcedureNameLength(): Int = metaData.getMaxProcedureNameLength

  override def getMaxCatalogNameLength(): Int = metaData.getMaxCatalogNameLength

  override def getMaxRowSize(): Int = metaData.getMaxRowSize

  override def doesMaxRowSizeIncludeBlobs(): Boolean = metaData.doesMaxRowSizeIncludeBlobs

  override def getMaxStatementLength(): Int = metaData.getMaxStatementLength

  override def getMaxStatements(): Int = metaData.getMaxStatements

  override def getMaxTableNameLength(): Int = metaData.getMaxTableNameLength

  override def getMaxTablesInSelect(): Int = metaData.getMaxTablesInSelect

  override def getMaxUserNameLength(): Int = metaData.getMaxUserNameLength

  override def getDefaultTransactionIsolation(): Int = metaData.getDefaultTransactionIsolation

  override def supportsTransactions(): Boolean = metaData.supportsTransactions

  override def supportsTransactionIsolationLevel(level: Int): Boolean =
    metaData.supportsTransactionIsolationLevel(level)

  override def supportsDataDefinitionAndDataManipulationTransactions(): Boolean =
    metaData.supportsDataDefinitionAndDataManipulationTransactions

  override def supportsDataManipulationTransactionsOnly(): Boolean =
    metaData.supportsDataManipulationTransactionsOnly

  override def dataDefinitionCausesTransactionCommit(): Boolean = metaData.dataDefinitionCausesTransactionCommit

  override def dataDefinitionIgnoredInTransactions(): Boolean = metaData.dataDefinitionIgnoredInTransactions

  override def getProcedures(
    catalog:              Option[String],
    schemaPattern:        Option[String],
    procedureNamePattern: Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getProcedures(
          catalog.orNull,
          schemaPattern.orNull,
          procedureNamePattern.orNull
        )
      )
      .map(ResultSetImpl.apply)

  override def getProcedureColumns(
    catalog:              Option[String],
    schemaPattern:        Option[String],
    procedureNamePattern: Option[String],
    columnNamePattern:    Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getProcedureColumns(
          catalog.orNull,
          schemaPattern.orNull,
          procedureNamePattern.orNull,
          columnNamePattern.orNull
        )
      )
      .map(ResultSetImpl.apply)

  override def getTables(
    catalog:          Option[String],
    schemaPattern:    Option[String],
    tableNamePattern: Option[String],
    types:            Array[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getTables(
          catalog.orNull,
          schemaPattern.orNull,
          tableNamePattern.orNull,
          types
        )
      )
      .map(ResultSetImpl.apply)

  override def getSchemas(): F[ResultSet] = Sync[F].blocking(metaData.getSchemas()).map(ResultSetImpl.apply)

  override def getCatalogs(): F[ResultSet] = Sync[F].blocking(metaData.getCatalogs).map(ResultSetImpl.apply)

  override def getTableTypes(): ResultSet = ResultSetImpl(metaData.getTableTypes)

  override def getColumns(
    catalog:           Option[String],
    schemaPattern:     Option[String],
    tableName:         Option[String],
    columnNamePattern: Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getColumns(catalog.orNull, schemaPattern.orNull, tableName.orNull, columnNamePattern.orNull)
      )
      .map(ResultSetImpl.apply)

  override def getColumnPrivileges(
    catalog:           Option[String],
    schema:            Option[String],
    table:             Option[String],
    columnNamePattern: Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getColumnPrivileges(catalog.orNull, schema.orNull, table.orNull, columnNamePattern.orNull)
      )
      .map(ResultSetImpl.apply)

  override def getTablePrivileges(
    catalog:          Option[String],
    schemaPattern:    Option[String],
    tableNamePattern: Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(metaData.getTablePrivileges(catalog.orNull, schemaPattern.orNull, tableNamePattern.orNull))
      .map(ResultSetImpl.apply)

  override def getBestRowIdentifier(
    catalog:  Option[String],
    schema:   Option[String],
    table:    String,
    scope:    Option[Int],
    nullable: Option[Boolean]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getBestRowIdentifier(
          catalog.orNull,
          schema.orNull,
          table,
          scope.getOrElse(0),
          nullable.getOrElse(false)
        )
      )
      .map(ResultSetImpl.apply)

  override def getVersionColumns(
    catalog: Option[String],
    schema:  Option[String],
    table:   String
  ): F[ResultSet] =
    Sync[F].blocking(metaData.getVersionColumns(catalog.orNull, schema.orNull, table)).map(ResultSetImpl.apply)

  override def getPrimaryKeys(catalog: Option[String], schema: Option[String], table: String): F[ResultSet] =
    Sync[F].blocking(metaData.getPrimaryKeys(catalog.orNull, schema.orNull, table)).map(ResultSetImpl.apply)

  override def getImportedKeys(catalog: Option[String], schema: Option[String], table: String): F[ResultSet] =
    Sync[F].blocking(metaData.getImportedKeys(catalog.orNull, schema.orNull, table)).map(ResultSetImpl.apply)

  override def getExportedKeys(catalog: Option[String], schema: Option[String], table: String): F[ResultSet] =
    Sync[F].blocking(metaData.getExportedKeys(catalog.orNull, schema.orNull, table)).map(ResultSetImpl.apply)

  override def getCrossReference(
    parentCatalog:  Option[String],
    parentSchema:   Option[String],
    parentTable:    String,
    foreignCatalog: Option[String],
    foreignSchema:  Option[String],
    foreignTable:   Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getCrossReference(
          parentCatalog.orNull,
          parentSchema.orNull,
          parentTable,
          foreignCatalog.orNull,
          foreignSchema.orNull,
          foreignTable.orNull
        )
      )
      .map(ResultSetImpl.apply)

  override def getTypeInfo(): ResultSet = ResultSetImpl(metaData.getTypeInfo)

  override def getIndexInfo(
    catalog:     Option[String],
    schema:      Option[String],
    table:       Option[String],
    unique:      Boolean,
    approximate: Boolean
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getIndexInfo(
          catalog.orNull,
          schema.orNull,
          table.orNull,
          unique,
          approximate
        )
      )
      .map(ResultSetImpl.apply)

  override def supportsResultSetType(`type`: Int): Boolean = metaData.supportsResultSetType(`type`)

  override def supportsResultSetConcurrency(`type`: Int, concurrency: Int): Boolean =
    metaData.supportsResultSetConcurrency(`type`, concurrency)

  override def ownUpdatesAreVisible(`type`: Int): Boolean = metaData.ownUpdatesAreVisible(`type`)

  override def ownDeletesAreVisible(`type`: Int): Boolean = metaData.ownDeletesAreVisible(`type`)

  override def ownInsertsAreVisible(`type`: Int): Boolean = metaData.ownInsertsAreVisible(`type`)

  override def othersUpdatesAreVisible(`type`: Int): Boolean = metaData.othersUpdatesAreVisible(`type`)

  override def othersDeletesAreVisible(`type`: Int): Boolean = metaData.othersDeletesAreVisible(`type`)

  override def othersInsertsAreVisible(`type`: Int): Boolean = metaData.othersInsertsAreVisible(`type`)

  override def updatesAreDetected(`type`: Int): Boolean = metaData.updatesAreDetected(`type`)

  override def deletesAreDetected(`type`: Int): Boolean = metaData.deletesAreDetected(`type`)

  override def insertsAreDetected(`type`: Int): Boolean = metaData.insertsAreDetected(`type`)

  override def supportsBatchUpdates(): Boolean = metaData.supportsBatchUpdates

  override def getUDTs(
    catalog:         Option[String],
    schemaPattern:   Option[String],
    typeNamePattern: Option[String],
    types:           Array[Int]
  ): ResultSet =
    ResultSetImpl(
      metaData.getUDTs(
        catalog.orNull,
        schemaPattern.orNull,
        typeNamePattern.orNull,
        types
      )
    )

  override def getConnection(): Connection[F] = ConnectionImpl(metaData.getConnection)

  override def supportsSavepoints(): Boolean = metaData.supportsSavepoints

  override def supportsNamedParameters(): Boolean = metaData.supportsNamedParameters

  override def supportsMultipleOpenResults(): Boolean = metaData.supportsMultipleOpenResults

  override def supportsGetGeneratedKeys(): Boolean = metaData.supportsGetGeneratedKeys

  override def getSuperTypes(
    catalog:         Option[String],
    schemaPattern:   Option[String],
    typeNamePattern: Option[String]
  ): ResultSet =
    ResultSetImpl(
      metaData.getSuperTypes(
        catalog.orNull,
        schemaPattern.orNull,
        typeNamePattern.orNull
      )
    )

  override def getSuperTables(
    catalog:          Option[String],
    schemaPattern:    Option[String],
    tableNamePattern: Option[String]
  ): ResultSet =
    ResultSetImpl(
      metaData.getSuperTables(
        catalog.orNull,
        schemaPattern.orNull,
        tableNamePattern.orNull
      )
    )

  override def getAttributes(
    catalog:              Option[String],
    schemaPattern:        Option[String],
    typeNamePattern:      Option[String],
    attributeNamePattern: Option[String]
  ): ResultSet =
    ResultSetImpl(
      metaData.getAttributes(
        catalog.orNull,
        schemaPattern.orNull,
        typeNamePattern.orNull,
        attributeNamePattern.orNull
      )
    )

  override def supportsResultSetHoldability(holdability: Int): Boolean =
    metaData.supportsResultSetHoldability(holdability)

  override def getResultSetHoldability(): Int = metaData.getResultSetHoldability

  override def getDatabaseMajorVersion(): Int = metaData.getDatabaseMajorVersion

  override def getDatabaseMinorVersion(): Int = metaData.getDatabaseMinorVersion

  override def getJDBCMajorVersion(): Int = metaData.getJDBCMajorVersion

  override def getJDBCMinorVersion(): Int = metaData.getJDBCMinorVersion

  override def getSQLStateType(): Int = metaData.getSQLStateType

  override def locatorsUpdateCopy(): Boolean = metaData.locatorsUpdateCopy

  override def supportsStatementPooling(): Boolean = metaData.supportsStatementPooling

  override def getRowIdLifetime(): RowIdLifetime = metaData.getRowIdLifetime match
    case java.sql.RowIdLifetime.ROWID_UNSUPPORTED       => RowIdLifetime.ROWID_UNSUPPORTED
    case java.sql.RowIdLifetime.ROWID_VALID_OTHER       => RowIdLifetime.ROWID_VALID_OTHER
    case java.sql.RowIdLifetime.ROWID_VALID_SESSION     => RowIdLifetime.ROWID_VALID_SESSION
    case java.sql.RowIdLifetime.ROWID_VALID_TRANSACTION => RowIdLifetime.ROWID_VALID_TRANSACTION
    case java.sql.RowIdLifetime.ROWID_VALID_FOREVER     => RowIdLifetime.ROWID_VALID_FOREVER

  override def getSchemas(catalog: Option[String], schemaPattern: Option[String]): F[ResultSet] =
    Sync[F].blocking(metaData.getSchemas(catalog.orNull, schemaPattern.orNull)).map(ResultSetImpl.apply)

  override def supportsStoredFunctionsUsingCallSyntax(): Boolean = metaData.supportsStoredFunctionsUsingCallSyntax

  override def autoCommitFailureClosesAllResultSets(): Boolean = metaData.autoCommitFailureClosesAllResultSets

  override def getClientInfoProperties(): ResultSet =
    ResultSetImpl(metaData.getClientInfoProperties)

  override def getFunctions(
    catalog:             Option[String],
    schemaPattern:       Option[String],
    functionNamePattern: Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getFunctions(
          catalog.orNull,
          schemaPattern.orNull,
          functionNamePattern.orNull
        )
      )
      .map(ResultSetImpl.apply)

  override def getFunctionColumns(
    catalog:             Option[String],
    schemaPattern:       Option[String],
    functionNamePattern: Option[String],
    columnNamePattern:   Option[String]
  ): F[ResultSet] =
    Sync[F]
      .blocking(
        metaData.getFunctionColumns(
          catalog.orNull,
          schemaPattern.orNull,
          functionNamePattern.orNull,
          columnNamePattern.orNull
        )
      )
      .map(ResultSetImpl.apply)

  override def getPseudoColumns(
    catalog:           Option[String],
    schemaPattern:     Option[String],
    tableNamePattern:  Option[String],
    columnNamePattern: Option[String]
  ): ResultSet =
    ResultSetImpl(
      metaData.getPseudoColumns(
        catalog.orNull,
        schemaPattern.orNull,
        tableNamePattern.orNull,
        columnNamePattern.orNull
      )
    )

  override def generatedKeyAlwaysReturned(): Boolean = metaData.generatedKeyAlwaysReturned
