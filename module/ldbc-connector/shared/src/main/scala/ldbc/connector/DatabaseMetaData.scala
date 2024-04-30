/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.collection.immutable.ListMap

import cats.*
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.data.*
import ldbc.connector.data.Types.*
import ldbc.connector.data.Constants.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.Protocol
import ldbc.connector.net.protocol.*

/**
 * Comprehensive information about the database as a whole.
 * <P>
 * This interface is implemented by driver vendors to let users know the capabilities
 * of a Database Management System (DBMS) in combination with
 * the driver based on JDBC&trade; technology
 * ("JDBC driver") that is used with it.  Different relational DBMSs often support
 * different features, implement features in different ways, and use different
 * data types.  In addition, a driver may implement a feature on top of what the
 * DBMS offers.  Information returned by methods in this interface applies
 * to the capabilities of a particular driver and a particular DBMS working
 * together. Note that as used in this documentation, the term "database" is
 * used generically to refer to both the driver and DBMS.
 * <P>
 * A user for this interface is commonly a tool that needs to discover how to
 * deal with the underlying DBMS.  This is especially true for applications
 * that are intended to be used with more than one DBMS. For example, a tool might use the method
 * <code>getTypeInfo</code> to find out what data types can be used in a
 * <code>CREATE TABLE</code> statement.  Or a user might call the method
 * <code>supportsCorrelatedSubqueries</code> to see if it is possible to use
 * a correlated subquery or <code>supportsBatchUpdates</code> to see if it is
 * possible to use batch updates.
 * <P>
 * Some <code>DatabaseMetaData</code> methods return lists of information
 * in the form of <code>ResultSet</code> objects.
 * Regular <code>ResultSet</code> methods, such as
 * <code>getString</code> and <code>getInt</code>, can be used
 * to retrieve the data from these <code>ResultSet</code> objects.  If
 * a given form of metadata is not available, an empty <code>ResultSet</code>
 * will be returned. Additional columns beyond the columns defined to be
 * returned by the <code>ResultSet</code> object for a given method
 * can be defined by the JDBC driver vendor and must be accessed
 * by their <B>column label</B>.
 * <P>
 * Some <code>DatabaseMetaData</code> methods take arguments that are
 * String patterns.  These arguments all have names such as fooPattern.
 * Within a pattern String, "%" means match any substring of 0 or more
 * characters, and "_" means match any one character. Only metadata
 * entries matching the search pattern are returned. If a search pattern
 * argument is set to <code>null</code>, that argument's criterion will
 * be dropped from the search.
 *
 * @tparam F
 *   the effect type
 */
trait DatabaseMetaData[F[_]]:

  /**
   * Retrieves whether the current user can call all the procedures
   * returned by the method <code>getProcedures</code>.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def allProceduresAreCallable(): Boolean

  /**
   * Retrieves whether the current user can use all the tables returned
   * by the method <code>getTables</code> in a <code>SELECT</code>
   * statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def allTablesAreSelectable(): Boolean

  /**
   * Retrieves the URL for this DBMS.
   *
   * @return the URL for this DBMS
   */
  def getURL(): String

  /**
   * Retrieves the user name as known to this database.
   *
   * @return the database user name
   */
  def getUserName(): F[String]

  /**
   * Retrieves whether this database is in read-only mode.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isReadOnly(): Boolean

  /**
   * Retrieves whether <code>NULL</code> values are sorted high.
   * Sorted high means that <code>NULL</code> values
   * sort higher than any other value in a domain.  In an ascending order,
   * if this method returns <code>true</code>,  <code>NULL</code> values
   * will appear at the end. By contrast, the method
   * <code>nullsAreSortedAtEnd</code> indicates whether <code>NULL</code> values
   * are sorted at the end regardless of sort order.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def nullsAreSortedHigh(): Boolean = false

  /**
   * Retrieves whether <code>NULL</code> values are sorted low.
   * Sorted low means that <code>NULL</code> values
   * sort lower than any other value in a domain.  In an ascending order,
   * if this method returns <code>true</code>,  <code>NULL</code> values
   * will appear at the beginning. By contrast, the method
   * <code>nullsAreSortedAtStart</code> indicates whether <code>NULL</code> values
   * are sorted at the beginning regardless of sort order.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def nullsAreSortedLow(): Boolean = false

  /**
   * Retrieves whether <code>NULL</code> values are sorted at the start regardless
   * of sort order.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def nullsAreSortedAtStart(): Boolean

  /**
   * Retrieves whether <code>NULL</code> values are sorted at the end regardless of
   * sort order.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def nullsAreSortedAtEnd(): Boolean

  /**
   * Retrieves the name of this database product.
   *
   * @return database product name
   */
  def getDatabaseProductName(): String

  /**
   * Retrieves the version number of this database product.
   *
   * @return database version number
   */
  def getDatabaseProductVersion(): String

  /**
   * Retrieves the name of this JDBC driver.
   *
   * @return JDBC driver name
   */
  def getDriverName(): String

  /**
   * Retrieves the version number of this JDBC driver as a <code>String</code>.
   *
   * @return JDBC driver version
   */
  def getDriverVersion(): String

  /**
   * Retrieves this JDBC driver's major version number.
   *
   * @return JDBC driver major version
   */
  def getDriverMajorVersion(): Int

  /**
   * Retrieves this JDBC driver's minor version number.
   *
   * @return JDBC driver minor version number
   */
  def getDriverMinorVersion(): Int

  /**
   * Retrieves whether this database stores tables in a local file.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def usesLocalFiles(): Boolean

  /**
   * Retrieves whether this database uses a file for each table.
   *
   * @return <code>true</code> if this database uses a local file for each table;
   *         <code>false</code> otherwise
   */
  def usesLocalFilePerTable(): Boolean

  /**
   * Retrieves whether this database treats mixed case unquoted SQL identifiers as
   * case sensitive and as a result stores them in mixed case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsMixedCaseIdentifiers(): Boolean

  /**
   * Retrieves whether this database treats mixed case unquoted SQL identifiers as
   * case insensitive and stores them in upper case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def storesUpperCaseIdentifiers(): Boolean

  /**
   * Retrieves whether this database treats mixed case unquoted SQL identifiers as
   * case insensitive and stores them in lower case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def storesLowerCaseIdentifiers(): Boolean

  /**
   * Retrieves whether this database treats mixed case unquoted SQL identifiers as
   * case insensitive and stores them in mixed case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def storesMixedCaseIdentifiers(): Boolean

  /**
   * Retrieves whether this database treats mixed case quoted SQL identifiers as
   * case sensitive and as a result stores them in mixed case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsMixedCaseQuotedIdentifiers(): Boolean

  /**
   * Retrieves whether this database treats mixed case quoted SQL identifiers as
   * case insensitive and stores them in upper case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def storesUpperCaseQuotedIdentifiers(): Boolean

  /**
   * Retrieves whether this database treats mixed case quoted SQL identifiers as
   * case insensitive and stores them in lower case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def storesLowerCaseQuotedIdentifiers(): Boolean

  /**
   * Retrieves whether this database treats mixed case quoted SQL identifiers as
   * case insensitive and stores them in mixed case.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def storesMixedCaseQuotedIdentifiers(): Boolean

  /**
   * Retrieves the string used to quote SQL identifiers.
   * This method returns a space " " if identifier quoting is not supported.
   *
   * @return the quoting string or a space if quoting is not supported
   */
  def getIdentifierQuoteString(): String

  /**
   * Retrieves a comma-separated list of all of this database's SQL keywords
   * that are NOT also SQL:2003 keywords.
   *
   * @return the list of this database's keywords that are not also
   *         SQL:2003 keywords
   */
  def getSQLKeywords(): F[String]

  /**
   * Retrieves a comma-separated list of math functions available with
   * this database.  These are the Open /Open CLI math function names used in
   * the JDBC function escape clause.
   *
   * @return the list of math functions supported by this database
   */
  def getNumericFunctions(): String

  /**
   * Retrieves a comma-separated list of string functions available with
   * this database.  These are the  Open Group CLI string function names used
   * in the JDBC function escape clause.
   *
   * @return the list of string functions supported by this database
   */
  def getStringFunctions(): String

  /**
   * Retrieves a comma-separated list of system functions available with
   * this database. These are the  Open Group CLI system function names used
   * in the JDBC function escape clause.
   *
   * @return a list of system functions supported by this database
   */
  def getSystemFunctions(): String

  /**
   * Retrieves a comma-separated list of the time and date functions available
   * with this database.
   *
   * @return the list of time and date functions supported by this database
   */
  def getTimeDateFunctions(): String

  /**
   * Retrieves the string that can be used to escape wildcard characters.
   * This is the string that can be used to escape '_' or '%' in
   * the catalog search parameters that are a pattern (and therefore use one
   * of the wildcard characters).
   *
   * <P>The '_' character represents any single character;
   * the '%' character represents any sequence of zero or
   * more characters.
   *
   * @return the string used to escape wildcard characters
   */
  def getSearchStringEscape(): String

  /**
   * Retrieves all the "extra" characters that can be used in unquoted
   * identifier names (those beyond a-z, A-Z, 0-9 and _).
   *
   * @return the string containing the extra characters
   */
  def getExtraNameCharacters(): String

  //--------------------------------------------------------------------
  // Functions describing which features are supported.

  /**
   * Retrieves whether this database supports <code>ALTER TABLE</code>
   * with add column.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsAlterTableWithAddColumn(): Boolean

  /**
   * Retrieves whether this database supports <code>ALTER TABLE</code>
   * with drop column.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsAlterTableWithDropColumn(): Boolean

  /**
   * Retrieves whether this database supports column aliasing.
   *
   * <P>If so, the SQL AS clause can be used to provide names for
   * computed columns or to provide alias names for columns as
   * required.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsColumnAliasing(): Boolean

  /**
   * Retrieves whether this database supports concatenations between
   * <code>NULL</code> and non-<code>NULL</code> values being
   * <code>NULL</code>.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def nullPlusNonNullIsNull(): Boolean

  /**
   * Retrieves whether this database supports the JDBC scalar function
   * <code>CONVERT</code> for the conversion of one JDBC type to another.
   * The JDBC types are the generic SQL data types defined
   * in <code>java.sql.Types</code>.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsConvert(): Boolean

  /**
   * Retrieves whether this database supports the JDBC scalar function
   * <code>CONVERT</code> for conversions between the JDBC types <i>fromType</i>
   * and <i>toType</i>.  The JDBC types are the generic SQL data types defined
   * in <code>java.sql.Types</code>.
   *
   * @param fromType the type to convert from; one of the type codes from
   *        the class <code>java.sql.Types</code>
   * @param toType the type to convert to; one of the type codes from
   *        the class <code>java.sql.Types</code>
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsConvert(fromType: Int, toType: Int): Boolean

  /**
   * Retrieves whether this database supports table correlation names.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsTableCorrelationNames(): Boolean

  /**
   * Retrieves whether, when table correlation names are supported, they
   * are restricted to being different from the names of the tables.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsDifferentTableCorrelationNames(): Boolean

  /**
   * Retrieves whether this database supports expressions in
   * <code>ORDER BY</code> lists.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsExpressionsInOrderBy(): Boolean

  /**
   * Retrieves whether this database supports using a column that is
   * not in the <code>SELECT</code> statement in an
   * <code>ORDER BY</code> clause.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsOrderByUnrelated(): Boolean

  /**
   * Retrieves whether this database supports some form of
   * <code>GROUP BY</code> clause.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsGroupBy(): Boolean

  /**
   * Retrieves whether this database supports using a column that is
   * not in the <code>SELECT</code> statement in a
   * <code>GROUP BY</code> clause.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsGroupByUnrelated(): Boolean

  /**
   * Retrieves whether this database supports using columns not included in
   * the <code>SELECT</code> statement in a <code>GROUP BY</code> clause
   * provided that all of the columns in the <code>SELECT</code> statement
   * are included in the <code>GROUP BY</code> clause.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsGroupByBeyondSelect(): Boolean

  /**
   * Retrieves whether this database supports specifying a
   * <code>LIKE</code> escape clause.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsLikeEscapeClause(): Boolean

  /**
   * Retrieves whether this database supports getting multiple
   * <code>ResultSet</code> objects from a single call to the
   * method <code>execute</code>.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsMultipleResultSets(): Boolean

  /**
   * Retrieves whether this database allows having multiple
   * transactions open at once (on different connections).
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsMultipleTransactions(): Boolean

  /**
   * Retrieves whether columns in this database may be defined as non-nullable.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsNonNullableColumns(): Boolean

  /**
   * Retrieves whether this database supports the ODBC Minimum SQL grammar.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsMinimumSQLGrammar(): Boolean

  /**
   * Retrieves whether this database supports the ODBC Core SQL grammar.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsCoreSQLGrammar(): Boolean

  /**
   * Retrieves whether this database supports the ODBC Extended SQL grammar.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsExtendedSQLGrammar(): Boolean

  /**
   * Retrieves whether this database supports the ANSI92 entry level SQL
   * grammar.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsANSI92EntryLevelSQL(): Boolean

  /**
   * Retrieves whether this database supports the ANSI92 intermediate SQL grammar supported.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsANSI92IntermediateSQL(): Boolean

  /**
   * Retrieves whether this database supports the ANSI92 full SQL grammar supported.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsANSI92FullSQL(): Boolean

  /**
   * Retrieves whether this database supports the SQL Integrity
   * Enhancement Facility.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsIntegrityEnhancementFacility(): Boolean

  /**
   * Retrieves whether this database supports some form of outer join.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsOuterJoins(): Boolean

  /**
   * Retrieves whether this database supports full nested outer joins.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsFullOuterJoins(): Boolean

  /**
   * Retrieves whether this database provides limited support for outer
   * joins.  (This will be <code>true</code> if the method
   * <code>supportsFullOuterJoins</code> returns <code>true</code>).
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsLimitedOuterJoins(): Boolean

  /**
   * Retrieves the database vendor's preferred term for "schema".
   *
   * @return the vendor term for "schema"
   */
  def getSchemaTerm(): String

  /**
   * Retrieves the database vendor's preferred term for "procedure".
   *
   * @return the vendor term for "procedure"
   */
  def getProcedureTerm(): String

  /**
   * Retrieves the database vendor's preferred term for "catalog".
   *
   * @return the vendor term for "catalog"
   */
  def getCatalogTerm(): String

  /**
   * Retrieves whether a catalog appears at the start of a fully qualified
   * table name.  If not, the catalog appears at the end.
   *
   * @return <code>true</code> if the catalog name appears at the beginning
   *         of a fully qualified table name; <code>false</code> otherwise
   */
  def isCatalogAtStart(): Boolean

  /**
   * Retrieves the <code>String</code> that this database uses as the
   * separator between a catalog and table name.
   *
   * @return the separator string
   */
  def getCatalogSeparator(): String

  /**
   * Retrieves whether a schema name can be used in a data manipulation statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSchemasInDataManipulation(): Boolean

  /**
   * Retrieves whether a schema name can be used in a procedure call statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSchemasInProcedureCalls(): Boolean

  /**
   * Retrieves whether a schema name can be used in a table definition statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSchemasInTableDefinitions(): Boolean

  /**
   * Retrieves whether a schema name can be used in an index definition statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSchemasInIndexDefinitions(): Boolean

  /**
   * Retrieves whether a schema name can be used in a privilege definition statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSchemasInPrivilegeDefinitions(): Boolean

  /**
   * Retrieves whether a catalog name can be used in a data manipulation statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsCatalogsInDataManipulation(): Boolean

  /**
   * Retrieves whether a catalog name can be used in a procedure call statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsCatalogsInProcedureCalls(): Boolean

  /**
   * Retrieves whether a catalog name can be used in a table definition statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsCatalogsInTableDefinitions(): Boolean

  /**
   * Retrieves whether a catalog name can be used in an index definition statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsCatalogsInIndexDefinitions(): Boolean

  /**
   * Retrieves whether a catalog name can be used in a privilege definition statement.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsCatalogsInPrivilegeDefinitions(): Boolean

  /**
   * Retrieves whether this database supports positioned <code>DELETE</code>
   * statements.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsPositionedDelete(): Boolean

  /**
   * Retrieves whether this database supports positioned <code>UPDATE</code>
   * statements.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsPositionedUpdate(): Boolean

  /**
   * Retrieves whether this database supports <code>SELECT FOR UPDATE</code>
   * statements.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSelectForUpdate(): Boolean

  /**
   * Retrieves whether this database supports stored procedure calls
   * that use the stored procedure escape syntax.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsStoredProcedures(): Boolean

  /**
   * Retrieves whether this database supports subqueries in comparison
   * expressions.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSubqueriesInComparisons(): Boolean

  /**
   * Retrieves whether this database supports subqueries in
   * <code>EXISTS</code> expressions.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSubqueriesInExists(): Boolean

  /**
   * Retrieves whether this database supports subqueries in
   * <code>IN</code> expressions.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSubqueriesInIns(): Boolean

  /**
   * Retrieves whether this database supports subqueries in quantified
   * expressions.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsSubqueriesInQuantifieds(): Boolean

  /**
   * Retrieves whether this database supports correlated subqueries.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsCorrelatedSubqueries(): Boolean

  /**
   * Retrieves whether this database supports SQL <code>UNION</code>.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsUnion(): Boolean

  /**
   * Retrieves whether this database supports SQL <code>UNION ALL</code>.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsUnionAll(): Boolean

  /**
   * Retrieves whether this database supports keeping cursors open
   * across commits.
   *
   * @return <code>true</code> if cursors always remain open;
   *       <code>false</code> if they might not remain open
   */
  def supportsOpenCursorsAcrossCommit(): Boolean

  /**
   * Retrieves whether this database supports keeping cursors open
   * across rollbacks.
   *
   * @return <code>true</code> if cursors always remain open;
   *       <code>false</code> if they might not remain open
   */
  def supportsOpenCursorsAcrossRollback(): Boolean

  /**
   * Retrieves whether this database supports keeping statements open
   * across commits.
   *
   * @return <code>true</code> if statements always remain open;
   *       <code>false</code> if they might not remain open
   */
  def supportsOpenStatementsAcrossCommit(): Boolean

  /**
   * Retrieves whether this database supports keeping statements open
   * across rollbacks.
   *
   * @return <code>true</code> if statements always remain open;
   *       <code>false</code> if they might not remain open
   */
  def supportsOpenStatementsAcrossRollback(): Boolean

  //----------------------------------------------------------------------
  // The following group of methods exposes various limitations
  // based on the target database with the current driver.
  // Unless otherwise specified, a result of zero means there is no
  // limit, or the limit is not known.

  /**
   * Retrieves the maximum number of hex characters this database allows in an
   * inline binary literal.
   *
   * @return max the maximum length (in hex characters) for a binary literal;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxBinaryLiteralLength(): Int

  /**
   * Retrieves the maximum number of characters this database allows
   * for a character literal.
   *
   * @return the maximum number of characters allowed for a character literal;
   *      a result of zero means that there is no limit or the limit is
   *      not known
   */
  def getMaxCharLiteralLength(): Int

  /**
   * Retrieves the maximum number of characters this database allows
   * for a column name.
   *
   * @return the maximum number of characters allowed for a column name;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxColumnNameLength(): Int

  /**
   * Retrieves the maximum number of columns this database allows in a
   * <code>GROUP BY</code> clause.
   *
   * @return the maximum number of columns allowed;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxColumnsInGroupBy(): Int

  /**
   * Retrieves the maximum number of columns this database allows in an index.
   *
   * @return the maximum number of columns allowed;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxColumnsInIndex(): Int

  /**
   * Retrieves the maximum number of columns this database allows in an
   * <code>ORDER BY</code> clause.
   *
   * @return the maximum number of columns allowed;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxColumnsInOrderBy(): Int

  /**
   * Retrieves the maximum number of columns this database allows in a
   * <code>SELECT</code> list.
   *
   * @return the maximum number of columns allowed;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxColumnsInSelect(): Int

  /**
   * Retrieves the maximum number of columns this database allows in a table.
   *
   * @return the maximum number of columns allowed;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxColumnsInTable(): Int

  /**
   * Retrieves the maximum number of concurrent connections to this
   * database that are possible.
   *
   * @return the maximum number of active connections possible at one time;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxConnections(): Int

  /**
   * Retrieves the maximum number of characters that this database allows in a
   * cursor name.
   *
   * @return the maximum number of characters allowed in a cursor name;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxCursorNameLength(): Int

  /**
   * Retrieves the maximum number of bytes this database allows for an
   * index, including all of the parts of the index.
   *
   * @return the maximum number of bytes allowed; this limit includes the
   *      composite of all the constituent parts of the index;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxIndexLength(): Int

  /**
   * Retrieves the maximum number of characters that this database allows in a
   * schema name.
   *
   * @return the maximum number of characters allowed in a schema name;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxSchemaNameLength(): Int

  /**
   * Retrieves the maximum number of characters that this database allows in a
   * procedure name.
   *
   * @return the maximum number of characters allowed in a procedure name;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxProcedureNameLength(): Int

  /**
   * Retrieves the maximum number of characters that this database allows in a
   * catalog name.
   *
   * @return the maximum number of characters allowed in a catalog name;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxCatalogNameLength(): Int

  /**
   * Retrieves the maximum number of bytes this database allows in
   * a single row.
   *
   * @return the maximum number of bytes allowed for a row; a result of
   *         zero means that there is no limit or the limit is not known
   */
  def getMaxRowSize(): Int

  /**
   * Retrieves whether the return value for the method
   * <code>getMaxRowSize</code> includes the SQL data types
   * <code>LONGVARCHAR</code> and <code>LONGVARBINARY</code>.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def doesMaxRowSizeIncludeBlobs(): Boolean

  /**
   * Retrieves the maximum number of characters this database allows in
   * an SQL statement.
   *
   * @return the maximum number of characters allowed for an SQL statement;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxStatementLength(): Int

  /**
   * Retrieves the maximum number of active statements to this database
   * that can be open at the same time.
   *
   * @return the maximum number of statements that can be open at one time;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxStatements(): Int

  /**
   * Retrieves the maximum number of characters this database allows in
   * a table name.
   *
   * @return the maximum number of characters allowed for a table name;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxTableNameLength(): Int

  /**
   * Retrieves the maximum number of tables this database allows in a
   * <code>SELECT</code> statement.
   *
   * @return the maximum number of tables allowed in a <code>SELECT</code>
   *         statement; a result of zero means that there is no limit or
   *         the limit is not known
   */
  def getMaxTablesInSelect(): Int

  /**
   * Retrieves the maximum number of characters this database allows in
   * a user name.
   *
   * @return the maximum number of characters allowed for a user name;
   *      a result of zero means that there is no limit or the limit
   *      is not known
   */
  def getMaxUserNameLength(): Int

  /**
   * Retrieves this database's default transaction isolation level.  The
   * possible values are defined in <code>java.sql.Connection</code>.
   *
   * @return the default isolation level
   */
  def getDefaultTransactionIsolation(): Int

  /**
   * Retrieves whether this database supports transactions. If not, invoking the
   * method <code>commit</code> is a noop, and the isolation level is
   * <code>TRANSACTION_NONE</code>.
   *
   * @return <code>true</code> if transactions are supported;
   *         <code>false</code> otherwise
   */
  def supportsTransactions(): Boolean

  /**
   * Retrieves whether this database supports the given transaction isolation level.
   *
   * @param level one of the transaction isolation levels defined in
   *         <code>java.sql.Connection</code>
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsTransactionIsolationLevel(level: Int): Boolean

  /**
   * Retrieves whether this database supports both data definition and
   * data manipulation statements within a transaction.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsDataDefinitionAndDataManipulationTransactions(): Boolean

  /**
   * Retrieves whether this database supports only data manipulation
   * statements within a transaction.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsDataManipulationTransactionsOnly(): Boolean

  /**
   * Retrieves whether a data definition statement within a transaction forces
   * the transaction to commit.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def dataDefinitionCausesTransactionCommit(): Boolean

  /**
   * Retrieves whether this database ignores a data definition statement
   * within a transaction.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def dataDefinitionIgnoredInTransactions(): Boolean

  /**
   * Retrieves a description of the stored procedures available in the given
   * catalog.
   * <P>
   * Only procedure descriptions matching the schema and
   * procedure name criteria are returned.  They are ordered by
   * <code>PROCEDURE_CAT</code>, <code>PROCEDURE_SCHEM</code>,
   * <code>PROCEDURE_NAME</code> and <code>SPECIFIC_ NAME</code>.
   *
   * <P>Each procedure description has the following columns:
   *  <OL>
   *  <LI><B>PROCEDURE_CAT</B> String {@code =>} procedure catalog (may be <code>null</code>)
   *  <LI><B>PROCEDURE_SCHEM</B> String {@code =>} procedure schema (may be <code>null</code>)
   *  <LI><B>PROCEDURE_NAME</B> String {@code =>} procedure name
   *  <LI> reserved for future use
   *  <LI> reserved for future use
   *  <LI> reserved for future use
   *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the procedure
   *  <LI><B>PROCEDURE_TYPE</B> short {@code =>} kind of procedure:
   *      <UL>
   *      <LI> procedureResultUnknown - Cannot determine if  a return value
   *       will be returned
   *      <LI> procedureNoResult - Does not return a return value
   *      <LI> procedureReturnsResult - Returns a return value
   *      </UL>
   *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} The name which uniquely identifies this
   * procedure within its schema.
   *  </OL>
   * <p>
   * A user may not have permissions to execute any of the procedures that are
   * returned by <code>getProcedures</code>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param procedureNamePattern a procedure name pattern; must match the
   *        procedure name as it is stored in the database
   * @return <code>ResultSet</code> - each row is a procedure description
   */
  def getProcedures(catalog: String, schemaPattern: String, procedureNamePattern: String): F[ResultSet[F]] = getProcedures(Some(catalog), Some(schemaPattern), Some(procedureNamePattern))

  def getProcedures(catalog: Option[String], schemaPattern: Option[String], procedureNamePattern: Option[String]): F[ResultSet[F]]

  /**
   * Retrieves a description of the given catalog's stored procedure parameter
   * and result columns.
   *
   * <P>Only descriptions matching the schema, procedure and
   * parameter name criteria are returned.  They are ordered by
   * PROCEDURE_CAT, PROCEDURE_SCHEM, PROCEDURE_NAME and SPECIFIC_NAME. Within this, the return value,
   * if any, is first. Next are the parameter descriptions in call
   * order. The column descriptions follow in column number order.
   *
   * <P>Each row in the <code>ResultSet</code> is a parameter description or
   * column description with the following fields:
   *  <OL>
   *  <LI><B>PROCEDURE_CAT</B> String {@code =>} procedure catalog (may be <code>null</code>)
   *  <LI><B>PROCEDURE_SCHEM</B> String {@code =>} procedure schema (may be <code>null</code>)
   *  <LI><B>PROCEDURE_NAME</B> String {@code =>} procedure name
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column/parameter name
   *  <LI><B>COLUMN_TYPE</B> Short {@code =>} kind of column/parameter:
   *      <UL>
   *      <LI> procedureColumnUnknown - nobody knows
   *      <LI> procedureColumnIn - IN parameter
   *      <LI> procedureColumnInOut - INOUT parameter
   *      <LI> procedureColumnOut - OUT parameter
   *      <LI> procedureColumnReturn - procedure return value
   *      <LI> procedureColumnResult - result column in <code>ResultSet</code>
   *      </UL>
   *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
   *  <LI><B>TYPE_NAME</B> String {@code =>} SQL type name, for a UDT type the
   *  type name is fully qualified
   *  <LI><B>PRECISION</B> int {@code =>} precision
   *  <LI><B>LENGTH</B> int {@code =>} length in bytes of data
   *  <LI><B>SCALE</B> short {@code =>} scale -  null is returned for data types where
   * SCALE is not applicable.
   *  <LI><B>RADIX</B> short {@code =>} radix
   *  <LI><B>NULLABLE</B> short {@code =>} can it contain NULL.
   *      <UL>
   *      <LI> procedureNoNulls - does not allow NULL values
   *      <LI> procedureNullable - allows NULL values
   *      <LI> procedureNullableUnknown - nullability unknown
   *      </UL>
   *  <LI><B>REMARKS</B> String {@code =>} comment describing parameter/column
   *  <LI><B>COLUMN_DEF</B> String {@code =>} default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be <code>null</code>)
   *      <UL>
   *      <LI> The string NULL (not enclosed in quotes) - if NULL was specified as the default value
   *      <LI> TRUNCATE (not enclosed in quotes)        - if the specified default value cannot be represented without truncation
   *      <LI> NULL                                     - if a default value was not specified
   *      </UL>
   *  <LI><B>SQL_DATA_TYPE</B> int  {@code =>} reserved for future use
   *  <LI><B>SQL_DATETIME_SUB</B> int  {@code =>} reserved for future use
   *  <LI><B>CHAR_OCTET_LENGTH</B> int  {@code =>} the maximum length of binary and character based columns.  For any other datatype the returned value is a
   * NULL
   *  <LI><B>ORDINAL_POSITION</B> int  {@code =>} the ordinal position, starting from 1, for the input and output parameters for a procedure. A value of 0
   *is returned if this row describes the procedure's return value.  For result set columns, it is the
   *ordinal position of the column in the result set starting from 1.  If there are
   *multiple result sets, the column ordinal positions are implementation
   * defined.
   *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
   *       <UL>
   *       <LI> YES           --- if the column can include NULLs
   *       <LI> NO            --- if the column cannot include NULLs
   *       <LI> empty string  --- if the nullability for the
   * column is unknown
   *       </UL>
   *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies this procedure within its schema.
   *  </OL>
   *
   * <P><B>Note:</B> Some databases may not return the column
   * descriptions for a procedure.
   *
   * <p>The PRECISION column represents the specified column size for the given column.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. Null is returned for data types where the
   * column size is not applicable.
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param procedureNamePattern a procedure name pattern; must match the
   *        procedure name as it is stored in the database
   * @param columnNamePattern a column name pattern; must match the column name
   *        as it is stored in the database
   * @return <code>ResultSet</code> - each row describes a stored procedure parameter or
   *      column
   */
  def getProcedureColumns(catalog: String, schemaPattern: String, procedureNamePattern: String, columnNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of the tables available in the given catalog.
   * Only table descriptions matching the catalog, schema, table
   * name and type criteria are returned.  They are ordered by
   * <code>TABLE_TYPE</code>, <code>TABLE_CAT</code>,
   * <code>TABLE_SCHEM</code> and <code>TABLE_NAME</code>.
   * <P>
   * Each table description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} table name
   *  <LI><B>TABLE_TYPE</B> String {@code =>} table type.  Typical types are "TABLE",
   *                  "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
   *                  "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
   *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the table (may be {@code null})
   *  <LI><B>TYPE_CAT</B> String {@code =>} the types catalog (may be <code>null</code>)
   *  <LI><B>TYPE_SCHEM</B> String {@code =>} the types schema (may be <code>null</code>)
   *  <LI><B>TYPE_NAME</B> String {@code =>} type name (may be <code>null</code>)
   *  <LI><B>SELF_REFERENCING_COL_NAME</B> String {@code =>} name of the designated
   *                  "identifier" column of a typed table (may be <code>null</code>)
   *  <LI><B>REF_GENERATION</B> String {@code =>} specifies how values in
   *                  SELF_REFERENCING_COL_NAME are created. Values are
   *                  "SYSTEM", "USER", "DERIVED". (may be <code>null</code>)
   *  </OL>
   *
   * <P><B>Note:</B> Some databases may not return information for
   * all tables.
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param tableNamePattern a table name pattern; must match the
   *        table name as it is stored in the database
   * @param types a list of table types, which must be from the list of table types
   *         returned from {@link #getTableTypes},to include; <code>null</code> returns
   * all types
   * @return <code>ResultSet</code> - each row is a table description
   */
  def getTables(catalog: String, schemaPattern: String, tableNamePattern: String, types: Array[String]): ResultSet[F]

  /**
   * Retrieves the schema names available in this database.  The results
   * are ordered by <code>TABLE_CATALOG</code> and
   * <code>TABLE_SCHEM</code>.
   *
   * <P>The schema columns are:
   *  <OL>
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} schema name
   *  <LI><B>TABLE_CATALOG</B> String {@code =>} catalog name (may be <code>null</code>)
   *  </OL>
   *
   * @return a <code>ResultSet</code> object in which each row is a
   *         schema description
   */
  def getSchemas(): ResultSet[F]

  /**
   * Retrieves the catalog names available in this database.  The results
   * are ordered by catalog name.
   *
   * <P>The catalog column is:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} catalog name
   *  </OL>
   *
   * @return a <code>ResultSet</code> object in which each row has a
   *         single <code>String</code> column that is a catalog name
   */
  def getCatalogs(): ResultSet[F]

  /**
   * Retrieves the table types available in this database.  The results
   * are ordered by table type.
   *
   * <P>The table type is:
   *  <OL>
   *  <LI><B>TABLE_TYPE</B> String {@code =>} table type.  Typical types are "TABLE",
   *                  "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
   *                  "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
   *  </OL>
   *
   * @return a <code>ResultSet</code> object in which each row has a
   *         single <code>String</code> column that is a table type
   */
  def getTableTypes(): ResultSet[F]

  /**
   * Retrieves a description of table columns available in
   * the specified catalog.
   *
   * <P>Only column descriptions matching the catalog, schema, table
   * and column name criteria are returned.  They are ordered by
   * <code>TABLE_CAT</code>,<code>TABLE_SCHEM</code>,
   * <code>TABLE_NAME</code>, and <code>ORDINAL_POSITION</code>.
   *
   * <P>Each column description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} table name
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
   *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
   *  <LI><B>TYPE_NAME</B> String {@code =>} Data source dependent type name,
   *  for a UDT the type name is fully qualified
   *  <LI><B>COLUMN_SIZE</B> int {@code =>} column size.
   *  <LI><B>BUFFER_LENGTH</B> is not used.
   *  <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
   * DECIMAL_DIGITS is not applicable.
   *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
   *  <LI><B>NULLABLE</B> int {@code =>} is NULL allowed.
   *      <UL>
   *      <LI> columnNoNulls - might not allow <code>NULL</code> values
   *      <LI> columnNullable - definitely allows <code>NULL</code> values
   *      <LI> columnNullableUnknown - nullability unknown
   *      </UL>
   *  <LI><B>REMARKS</B> String {@code =>} comment describing column (may be <code>null</code>)
   *  <LI><B>COLUMN_DEF</B> String {@code =>} default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be <code>null</code>)
   *  <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
   *  <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
   *  <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
   *       maximum number of bytes in the column
   *  <LI><B>ORDINAL_POSITION</B> int {@code =>} index of column in table
   *      (starting at 1)
   *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
   *       <UL>
   *       <LI> YES           --- if the column can include NULLs
   *       <LI> NO            --- if the column cannot include NULLs
   *       <LI> empty string  --- if the nullability for the
   * column is unknown
   *       </UL>
   *  <LI><B>SCOPE_CATALOG</B> String {@code =>} catalog of table that is the scope
   *      of a reference attribute (<code>null</code> if DATA_TYPE isn't REF)
   *  <LI><B>SCOPE_SCHEMA</B> String {@code =>} schema of table that is the scope
   *      of a reference attribute (<code>null</code> if the DATA_TYPE isn't REF)
   *  <LI><B>SCOPE_TABLE</B> String {@code =>} table name that this the scope
   *      of a reference attribute (<code>null</code> if the DATA_TYPE isn't REF)
   *  <LI><B>SOURCE_DATA_TYPE</B> short {@code =>} source type of a distinct type or user-generated
   *      Ref type, SQL type from java.sql.Types (<code>null</code> if DATA_TYPE
   *      isn't DISTINCT or user-generated REF)
   *   <LI><B>IS_AUTOINCREMENT</B> String  {@code =>} Indicates whether this column is auto incremented
   *       <UL>
   *       <LI> YES           --- if the column is auto incremented
   *       <LI> NO            --- if the column is not auto incremented
   *       <LI> empty string  --- if it cannot be determined whether the column is auto incremented
   *       </UL>
   *   <LI><B>IS_GENERATEDCOLUMN</B> String  {@code =>} Indicates whether this is a generated column
   *       <UL>
   *       <LI> YES           --- if this a generated column
   *       <LI> NO            --- if this not a generated column
   *       <LI> empty string  --- if it cannot be determined whether this is a generated column
   *       </UL>
   *  </OL>
   *
   * <p>The COLUMN_SIZE column specifies the column size for the given column.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. Null is returned for data types where the
   * column size is not applicable.
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param tableNamePattern a table name pattern; must match the
   *        table name as it is stored in the database
   * @param columnNamePattern a column name pattern; must match the column
   *        name as it is stored in the database
   * @return <code>ResultSet</code> - each row is a column description
   */
  def getColumns(catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of the access rights for a table's columns.
   *
   * <P>Only privileges matching the column name criteria are
   * returned.  They are ordered by COLUMN_NAME and PRIVILEGE.
   *
   * <P>Each privilege description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} table name
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
   *  <LI><B>GRANTOR</B> String {@code =>} grantor of access (may be <code>null</code>)
   *  <LI><B>GRANTEE</B> String {@code =>} grantee of access
   *  <LI><B>PRIVILEGE</B> String {@code =>} name of access (SELECT,
   *      INSERT, UPDATE, REFERENCES, ...)
   *  <LI><B>IS_GRANTABLE</B> String {@code =>} "YES" if grantee is permitted
   *      to grant to others; "NO" if not; <code>null</code> if unknown
   *  </OL>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schema a schema name; must match the schema name as it is
   *        stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param table a table name; must match the table name as it is
   *        stored in the database
   * @param columnNamePattern a column name pattern; must match the column
   *        name as it is stored in the database
   * @return <code>ResultSet</code> - each row is a column privilege description
   */
  def getColumnPrivileges(catalog: String, schema: String, table: String, columnNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of the access rights for each table available
   * in a catalog. Note that a table privilege applies to one or
   * more columns in the table. It would be wrong to assume that
   * this privilege applies to all columns (this may be true for
   * some systems but is not true for all.)
   *
   * <P>Only privileges matching the schema and table name
   * criteria are returned.  They are ordered by
   * <code>TABLE_CAT</code>,
   * <code>TABLE_SCHEM</code>, <code>TABLE_NAME</code>,
   * and <code>PRIVILEGE</code>.
   *
   * <P>Each privilege description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} table name
   *  <LI><B>GRANTOR</B> String {@code =>} grantor of access (may be <code>null</code>)
   *  <LI><B>GRANTEE</B> String {@code =>} grantee of access
   *  <LI><B>PRIVILEGE</B> String {@code =>} name of access (SELECT,
   *      INSERT, UPDATE, REFERENCES, ...)
   *  <LI><B>IS_GRANTABLE</B> String {@code =>} "YES" if grantee is permitted
   *      to grant to others; "NO" if not; <code>null</code> if unknown
   *  </OL>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param tableNamePattern a table name pattern; must match the
   *        table name as it is stored in the database
   * @return <code>ResultSet</code> - each row is a table privilege description
   */
  def getTablePrivileges(catalog: String, schemaPattern: String, tableNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of a table's optimal set of columns that
   * uniquely identifies a row. They are ordered by SCOPE.
   *
   * <P>Each column description has the following columns:
   *  <OL>
   *  <LI><B>SCOPE</B> short {@code =>} actual scope of result
   *      <UL>
   *      <LI> bestRowTemporary - very temporary, while using row
   *      <LI> bestRowTransaction - valid for remainder of current transaction
   *      <LI> bestRowSession - valid for remainder of current session
   *      </UL>
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
   *  <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from java.sql.Types
   *  <LI><B>TYPE_NAME</B> String {@code =>} Data source dependent type name,
   *  for a UDT the type name is fully qualified
   *  <LI><B>COLUMN_SIZE</B> int {@code =>} precision
   *  <LI><B>BUFFER_LENGTH</B> int {@code =>} not used
   *  <LI><B>DECIMAL_DIGITS</B> short  {@code =>} scale - Null is returned for data types where
   * DECIMAL_DIGITS is not applicable.
   *  <LI><B>PSEUDO_COLUMN</B> short {@code =>} is this a pseudo column
   *      like an Oracle ROWID
   *      <UL>
   *      <LI> bestRowUnknown - may or may not be pseudo column
   *      <LI> bestRowNotPseudo - is NOT a pseudo column
   *      <LI> bestRowPseudo - is a pseudo column
   *      </UL>
   *  </OL>
   *
   * <p>The COLUMN_SIZE column represents the specified column size for the given column.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. Null is returned for data types where the
   * column size is not applicable.
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schema a schema name; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param table a table name; must match the table name as it is stored
   *        in the database
   * @param scope the scope of interest; use same values as SCOPE
   * @param nullable include columns that are nullable.
   * @return <code>ResultSet</code> - each row is a column description
   */
  def getBestRowIdentifier(catalog: String, schema: String, table: String, scope: Int, nullable: Boolean): ResultSet[F]

  /**
   * Retrieves a description of a table's columns that are automatically
   * updated when any value in a row is updated.  They are
   * unordered.
   *
   * <P>Each column description has the following columns:
   *  <OL>
   *  <LI><B>SCOPE</B> short {@code =>} is not used
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
   *  <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from <code>java.sql.Types</code>
   *  <LI><B>TYPE_NAME</B> String {@code =>} Data source-dependent type name
   *  <LI><B>COLUMN_SIZE</B> int {@code =>} precision
   *  <LI><B>BUFFER_LENGTH</B> int {@code =>} length of column value in bytes
   *  <LI><B>DECIMAL_DIGITS</B> short  {@code =>} scale - Null is returned for data types where
   * DECIMAL_DIGITS is not applicable.
   *  <LI><B>PSEUDO_COLUMN</B> short {@code =>} whether this is pseudo column
   *      like an Oracle ROWID
   *      <UL>
   *      <LI> versionColumnUnknown - may or may not be pseudo column
   *      <LI> versionColumnNotPseudo - is NOT a pseudo column
   *      <LI> versionColumnPseudo - is a pseudo column
   *      </UL>
   *  </OL>
   *
   * <p>The COLUMN_SIZE column represents the specified column size for the given column.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. Null is returned for data types where the
   * column size is not applicable.
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schema a schema name; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param table a table name; must match the table name as it is stored
   *        in the database
   * @return a <code>ResultSet</code> object in which each row is a
   *         column description
   */
  def getVersionColumns(catalog: String, schema: String, table: String): ResultSet[F]

  /**
   * Retrieves a description of the given table's primary key columns.  They
   * are ordered by COLUMN_NAME.
   *
   * <P>Each primary key column description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} table name
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
   *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within primary key( a value
   *  of 1 represents the first column of the primary key, a value of 2 would
   *  represent the second column within the primary key).
   *  <LI><B>PK_NAME</B> String {@code =>} primary key name (may be <code>null</code>)
   *  </OL>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schema a schema name; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param table a table name; must match the table name as it is stored
   *        in the database
   * @return <code>ResultSet</code> - each row is a primary key column description
   */
  def getPrimaryKeys(catalog: String, schema: String, table: String): ResultSet[F]

  /**
   * Retrieves a description of the primary key columns that are
   * referenced by the given table's foreign key columns (the primary keys
   * imported by a table).  They are ordered by PKTABLE_CAT,
   * PKTABLE_SCHEM, PKTABLE_NAME, and KEY_SEQ.
   *
   * <P>Each primary key column description has the following columns:
   *  <OL>
   *  <LI><B>PKTABLE_CAT</B> String {@code =>} primary key table catalog
   *      being imported (may be <code>null</code>)
   *  <LI><B>PKTABLE_SCHEM</B> String {@code =>} primary key table schema
   *      being imported (may be <code>null</code>)
   *  <LI><B>PKTABLE_NAME</B> String {@code =>} primary key table name
   *      being imported
   *  <LI><B>PKCOLUMN_NAME</B> String {@code =>} primary key column name
   *      being imported
   *  <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be <code>null</code>)
   *  <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be <code>null</code>)
   *  <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
   *  <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
   *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within a foreign key( a value
   *  of 1 represents the first column of the foreign key, a value of 2 would
   *  represent the second column within the foreign key).
   *  <LI><B>UPDATE_RULE</B> short {@code =>} What happens to a
   *       foreign key when the primary key is updated:
   *      <UL>
   *      <LI> importedNoAction - do not allow update of primary
   *               key if it has been imported
   *      <LI> importedKeyCascade - change imported key to agree
   *               with primary key update
   *      <LI> importedKeySetNull - change imported key to <code>NULL</code>
   *               if its primary key has been updated
   *      <LI> importedKeySetDefault - change imported key to default values
   *               if its primary key has been updated
   *      <LI> importedKeyRestrict - same as importedKeyNoAction
   *                                 (for ODBC 2.x compatibility)
   *      </UL>
   *  <LI><B>DELETE_RULE</B> short {@code =>} What happens to
   *      the foreign key when primary is deleted.
   *      <UL>
   *      <LI> importedKeyNoAction - do not allow delete of primary
   *               key if it has been imported
   *      <LI> importedKeyCascade - delete rows that import a deleted key
   *      <LI> importedKeySetNull - change imported key to NULL if
   *               its primary key has been deleted
   *      <LI> importedKeyRestrict - same as importedKeyNoAction
   *                                 (for ODBC 2.x compatibility)
   *      <LI> importedKeySetDefault - change imported key to default if
   *               its primary key has been deleted
   *      </UL>
   *  <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be <code>null</code>)
   *  <LI><B>PK_NAME</B> String {@code =>} primary key name (may be <code>null</code>)
   *  <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
   *      constraints be deferred until commit
   *      <UL>
   *      <LI> importedKeyInitiallyDeferred - see SQL92 for definition
   *      <LI> importedKeyInitiallyImmediate - see SQL92 for definition
   *      <LI> importedKeyNotDeferrable - see SQL92 for definition
   *      </UL>
   *  </OL>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schema a schema name; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param table a table name; must match the table name as it is stored
   *        in the database
   * @return <code>ResultSet</code> - each row is a primary key column description
   */
  def getImportedKeys(catalog: String, schema: String, table: String): ResultSet[F]

  /**
   * Retrieves a description of the foreign key columns that reference the
   * given table's primary key columns (the foreign keys exported by a
   * table).  They are ordered by FKTABLE_CAT, FKTABLE_SCHEM,
   * FKTABLE_NAME, and KEY_SEQ.
   *
   * <P>Each foreign key column description has the following columns:
   *  <OL>
   *  <LI><B>PKTABLE_CAT</B> String {@code =>} primary key table catalog (may be <code>null</code>)
   *  <LI><B>PKTABLE_SCHEM</B> String {@code =>} primary key table schema (may be <code>null</code>)
   *  <LI><B>PKTABLE_NAME</B> String {@code =>} primary key table name
   *  <LI><B>PKCOLUMN_NAME</B> String {@code =>} primary key column name
   *  <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be <code>null</code>)
   *      being exported (may be <code>null</code>)
   *  <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be <code>null</code>)
   *      being exported (may be <code>null</code>)
   *  <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
   *      being exported
   *  <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
   *      being exported
   *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within foreign key( a value
   *  of 1 represents the first column of the foreign key, a value of 2 would
   *  represent the second column within the foreign key).
   *  <LI><B>UPDATE_RULE</B> short {@code =>} What happens to
   *       foreign key when primary is updated:
   *      <UL>
   *      <LI> importedNoAction - do not allow update of primary
   *               key if it has been imported
   *      <LI> importedKeyCascade - change imported key to agree
   *               with primary key update
   *      <LI> importedKeySetNull - change imported key to <code>NULL</code> if
   *               its primary key has been updated
   *      <LI> importedKeySetDefault - change imported key to default values
   *               if its primary key has been updated
   *      <LI> importedKeyRestrict - same as importedKeyNoAction
   *                                 (for ODBC 2.x compatibility)
   *      </UL>
   *  <LI><B>DELETE_RULE</B> short {@code =>} What happens to
   *      the foreign key when primary is deleted.
   *      <UL>
   *      <LI> importedKeyNoAction - do not allow delete of primary
   *               key if it has been imported
   *      <LI> importedKeyCascade - delete rows that import a deleted key
   *      <LI> importedKeySetNull - change imported key to <code>NULL</code> if
   *               its primary key has been deleted
   *      <LI> importedKeyRestrict - same as importedKeyNoAction
   *                                 (for ODBC 2.x compatibility)
   *      <LI> importedKeySetDefault - change imported key to default if
   *               its primary key has been deleted
   *      </UL>
   *  <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be <code>null</code>)
   *  <LI><B>PK_NAME</B> String {@code =>} primary key name (may be <code>null</code>)
   *  <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
   *      constraints be deferred until commit
   *      <UL>
   *      <LI> importedKeyInitiallyDeferred - see SQL92 for definition
   *      <LI> importedKeyInitiallyImmediate - see SQL92 for definition
   *      <LI> importedKeyNotDeferrable - see SQL92 for definition
   *      </UL>
   *  </OL>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in this database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schema a schema name; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param table a table name; must match the table name as it is stored
   *        in this database
   * @return a <code>ResultSet</code> object in which each row is a
   *         foreign key column description
   */
  def getExportedKeys(catalog: String, schema: String, table: String): ResultSet[F]

  /**
   * Retrieves a description of the foreign key columns in the given foreign key
   * table that reference the primary key or the columns representing a unique constraint of the  parent table (could be the same or a different table).
   * The number of columns returned from the parent table must match the number of
   * columns that make up the foreign key.  They
   * are ordered by FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, and
   * KEY_SEQ.
   *
   * <P>Each foreign key column description has the following columns:
   *  <OL>
   *  <LI><B>PKTABLE_CAT</B> String {@code =>} parent key table catalog (may be <code>null</code>)
   *  <LI><B>PKTABLE_SCHEM</B> String {@code =>} parent key table schema (may be <code>null</code>)
   *  <LI><B>PKTABLE_NAME</B> String {@code =>} parent key table name
   *  <LI><B>PKCOLUMN_NAME</B> String {@code =>} parent key column name
   *  <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be <code>null</code>)
   *      being exported (may be <code>null</code>)
   *  <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be <code>null</code>)
   *      being exported (may be <code>null</code>)
   *  <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
   *      being exported
   *  <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
   *      being exported
   *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within foreign key( a value
   *  of 1 represents the first column of the foreign key, a value of 2 would
   *  represent the second column within the foreign key).
   *  <LI><B>UPDATE_RULE</B> short {@code =>} What happens to
   *       foreign key when parent key is updated:
   *      <UL>
   *      <LI> importedNoAction - do not allow update of parent
   *               key if it has been imported
   *      <LI> importedKeyCascade - change imported key to agree
   *               with parent key update
   *      <LI> importedKeySetNull - change imported key to <code>NULL</code> if
   *               its parent key has been updated
   *      <LI> importedKeySetDefault - change imported key to default values
   *               if its parent key has been updated
   *      <LI> importedKeyRestrict - same as importedKeyNoAction
   *                                 (for ODBC 2.x compatibility)
   *      </UL>
   *  <LI><B>DELETE_RULE</B> short {@code =>} What happens to
   *      the foreign key when parent key is deleted.
   *      <UL>
   *      <LI> importedKeyNoAction - do not allow delete of parent
   *               key if it has been imported
   *      <LI> importedKeyCascade - delete rows that import a deleted key
   *      <LI> importedKeySetNull - change imported key to <code>NULL</code> if
   *               its primary key has been deleted
   *      <LI> importedKeyRestrict - same as importedKeyNoAction
   *                                 (for ODBC 2.x compatibility)
   *      <LI> importedKeySetDefault - change imported key to default if
   *               its parent key has been deleted
   *      </UL>
   *  <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be <code>null</code>)
   *  <LI><B>PK_NAME</B> String {@code =>} parent key name (may be <code>null</code>)
   *  <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
   *      constraints be deferred until commit
   *      <UL>
   *      <LI> importedKeyInitiallyDeferred - see SQL92 for definition
   *      <LI> importedKeyInitiallyImmediate - see SQL92 for definition
   *      <LI> importedKeyNotDeferrable - see SQL92 for definition
   *      </UL>
   *  </OL>
   *
   * @param parentCatalog a catalog name; must match the catalog name
   * as it is stored in the database; "" retrieves those without a
   * catalog; <code>null</code> means drop catalog name from the selection criteria
   * @param parentSchema a schema name; must match the schema name as
   * it is stored in the database; "" retrieves those without a schema;
   * <code>null</code> means drop schema name from the selection criteria
   * @param parentTable the name of the table that exports the key; must match
   * the table name as it is stored in the database
   * @param foreignCatalog a catalog name; must match the catalog name as
   * it is stored in the database; "" retrieves those without a
   * catalog; <code>null</code> means drop catalog name from the selection criteria
   * @param foreignSchema a schema name; must match the schema name as it
   * is stored in the database; "" retrieves those without a schema;
   * <code>null</code> means drop schema name from the selection criteria
   * @param foreignTable the name of the table that imports the key; must match
   * the table name as it is stored in the database
   * @return <code>ResultSet</code> - each row is a foreign key column description
   */
  def getCrossReference(parentCatalog: String, parentSchema: String, parentTable: String, foreignCatalog: String, foreignSchema: String, foreignTable: String): ResultSet[F]

  /**
   * Retrieves a description of all the data types supported by
   * this database. They are ordered by DATA_TYPE and then by how
   * closely the data type maps to the corresponding JDBC SQL type.
   *
   * <P>If the database supports SQL distinct types, then getTypeInfo() will return
   * a single row with a TYPE_NAME of DISTINCT and a DATA_TYPE of Types.DISTINCT.
   * If the database supports SQL structured types, then getTypeInfo() will return
   * a single row with a TYPE_NAME of STRUCT and a DATA_TYPE of Types.STRUCT.
   *
   * <P>If SQL distinct or structured types are supported, then information on the
   * individual types may be obtained from the getUDTs() method.
   *
   *
   * <P>Each type description has the following columns:
   *  <OL>
   *  <LI><B>TYPE_NAME</B> String {@code =>} Type name
   *  <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from java.sql.Types
   *  <LI><B>PRECISION</B> int {@code =>} maximum precision
   *  <LI><B>LITERAL_PREFIX</B> String {@code =>} prefix used to quote a literal
   *      (may be <code>null</code>)
   *  <LI><B>LITERAL_SUFFIX</B> String {@code =>} suffix used to quote a literal
   *  (may be <code>null</code>)
   *  <LI><B>CREATE_PARAMS</B> String {@code =>} parameters used in creating
   *      the type (may be <code>null</code>)
   *  <LI><B>NULLABLE</B> short {@code =>} can you use NULL for this type.
   *      <UL>
   *      <LI> typeNoNulls - does not allow NULL values
   *      <LI> typeNullable - allows NULL values
   *      <LI> typeNullableUnknown - nullability unknown
   *      </UL>
   *  <LI><B>CASE_SENSITIVE</B> boolean{@code =>} is it case sensitive.
   *  <LI><B>SEARCHABLE</B> short {@code =>} can you use "WHERE" based on this type:
   *      <UL>
   *      <LI> typePredNone - No support
   *      <LI> typePredChar - Only supported with WHERE .. LIKE
   *      <LI> typePredBasic - Supported except for WHERE .. LIKE
   *      <LI> typeSearchable - Supported for all WHERE ..
   *      </UL>
   *  <LI><B>UNSIGNED_ATTRIBUTE</B> boolean {@code =>} is it unsigned.
   *  <LI><B>FIXED_PREC_SCALE</B> boolean {@code =>} can it be a money value.
   *  <LI><B>AUTO_INCREMENT</B> boolean {@code =>} can it be used for an
   *      auto-increment value.
   *  <LI><B>LOCAL_TYPE_NAME</B> String {@code =>} localized version of type name
   *      (may be <code>null</code>)
   *  <LI><B>MINIMUM_SCALE</B> short {@code =>} minimum scale supported
   *  <LI><B>MAXIMUM_SCALE</B> short {@code =>} maximum scale supported
   *  <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
   *  <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
   *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} usually 2 or 10
   *  </OL>
   *
   * <p>The PRECISION column represents the maximum column size that the server supports for the given datatype.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. Null is returned for data types where the
   * column size is not applicable.
   *
   * @return a <code>ResultSet</code> object in which each row is an SQL
   *         type description
   */
  def getTypeInfo(): ResultSet[F]

  /**
   * Retrieves a description of the given table's indices and statistics. They are
   * ordered by NON_UNIQUE, TYPE, INDEX_NAME, and ORDINAL_POSITION.
   *
   * <P>Each index column description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} table name
   *  <LI><B>NON_UNIQUE</B> boolean {@code =>} Can index values be non-unique.
   *      false when TYPE is tableIndexStatistic
   *  <LI><B>INDEX_QUALIFIER</B> String {@code =>} index catalog (may be <code>null</code>);
   *      <code>null</code> when TYPE is tableIndexStatistic
   *  <LI><B>INDEX_NAME</B> String {@code =>} index name; <code>null</code> when TYPE is
   *      tableIndexStatistic
   *  <LI><B>TYPE</B> short {@code =>} index type:
   *      <UL>
   *      <LI> tableIndexStatistic - this identifies table statistics that are
   *           returned in conjunction with a table's index descriptions
   *      <LI> tableIndexClustered - this is a clustered index
   *      <LI> tableIndexHashed - this is a hashed index
   *      <LI> tableIndexOther - this is some other style of index
   *      </UL>
   *  <LI><B>ORDINAL_POSITION</B> short {@code =>} column sequence number
   *      within index; zero when TYPE is tableIndexStatistic
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column name; <code>null</code> when TYPE is
   *      tableIndexStatistic
   *  <LI><B>ASC_OR_DESC</B> String {@code =>} column sort sequence, "A" {@code =>} ascending,
   *      "D" {@code =>} descending, may be <code>null</code> if sort sequence is not supported;
   *      <code>null</code> when TYPE is tableIndexStatistic
   *  <LI><B>CARDINALITY</B> long {@code =>} When TYPE is tableIndexStatistic, then
   *      this is the number of rows in the table; otherwise, it is the
   *      number of unique values in the index.
   *  <LI><B>PAGES</B> long {@code =>} When TYPE is  tableIndexStatistic then
   *      this is the number of pages used for the table, otherwise it
   *      is the number of pages used for the current index.
   *  <LI><B>FILTER_CONDITION</B> String {@code =>} Filter condition, if any.
   *      (may be <code>null</code>)
   *  </OL>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in this database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schema a schema name; must match the schema name
   *        as it is stored in this database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param table a table name; must match the table name as it is stored
   *        in this database
   * @param unique when true, return only indices for unique values;
   *     when false, return indices regardless of whether unique or not
   * @param approximate when true, result is allowed to reflect approximate
   *     or out of data values; when false, results are requested to be
   *     accurate
   * @return <code>ResultSet</code> - each row is an index column description
   */
  def getIndexInfo(catalog: String, schema: String, table: String, unique: Boolean, approximate: Boolean): ResultSet[F]

  /**
   * Retrieves whether this database supports the given result set type.
   *
   * @param `type` defined in <code>java.sql.ResultSet</code>
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsResultSetType(`type`: Int): Boolean

  /**
   * Retrieves whether this database supports the given concurrency type
   * in combination with the given result set type.
   *
   * @param `type` defined in <code>java.sql.ResultSet</code>
   * @param concurrency type defined in <code>java.sql.ResultSet</code>
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsResultSetConcurrency(`type`: Int, concurrency: Int): Boolean

  /**
   * Retrieves whether for the given type of <code>ResultSet</code> object,
   * the result set's own updates are visible.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   */
  def ownUpdatesAreVisible(`type`: Int): Boolean

  /**
   * Retrieves whether a result set's own deletes are visible.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if deletes are visible for the given result set type;
   *        <code>false</code> otherwise
   */
  def ownDeletesAreVisible(`type`: Int): Boolean

  /**
   * Retrieves whether a result set's own inserts are visible.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if inserts are visible for the given result set type;
   *        <code>false</code> otherwise
   */
  def ownInsertsAreVisible(`type`: Int): Boolean

  /**
   * Retrieves whether updates made by others are visible.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if updates made by others
   *        are visible for the given result set type;
   *        <code>false</code> otherwise
   */
  def othersUpdatesAreVisible(`type`: Int): Boolean

  /**
   * Retrieves whether deletes made by others are visible.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if deletes made by others
   *        are visible for the given result set type;
   *        <code>false</code> otherwise
   */
  def othersDeletesAreVisible(`type`: Int): Boolean

  /**
   * Retrieves whether inserts made by others are visible.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if inserts made by others
   *         are visible for the given result set type;
   *         <code>false</code> otherwise
   */
  def othersInsertsAreVisible(`type`: Int): Boolean

  /**
   * Retrieves whether or not a visible row update can be detected by
   * calling the method <code>ResultSet.rowUpdated</code>.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if changes are detected by the result set type;
   *         <code>false</code> otherwise
   */
  def updatesAreDetected(`type`: Int): Boolean

  /**
   * Retrieves whether or not a visible row delete can be detected by
   * calling the method <code>ResultSet.rowDeleted</code>.  If the method
   * <code>deletesAreDetected</code> returns <code>false</code>, it means that
   * deleted rows are removed from the result set.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if deletes are detected by the given result set type;
   *         <code>false</code> otherwise
   */
  def deletesAreDetected(`type`: Int): Boolean

  /**
   * Retrieves whether or not a visible row insert can be detected
   * by calling the method <code>ResultSet.rowInserted</code>.
   *
   * @param type the <code>ResultSet</code> type; one of
   *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
   *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @return <code>true</code> if changes are detected by the specified result
   *         set type; <code>false</code> otherwise
   */
  def insertsAreDetected(`type`: Int): Boolean

  /**
   * Retrieves whether this database supports batch updates.
   *
   * @return <code>true</code> if this database supports batch updates;
   *         <code>false</code> otherwise
   */
  def supportsBatchUpdates(): Boolean

  /**
   * Retrieves a description of the user-defined types (UDTs) defined
   * in a particular schema.  Schema-specific UDTs may have type
   * <code>JAVA_OBJECT</code>, <code>STRUCT</code>,
   * or <code>DISTINCT</code>.
   *
   * <P>Only types matching the catalog, schema, type name and type
   * criteria are returned.  They are ordered by <code>DATA_TYPE</code>,
   * <code>TYPE_CAT</code>, <code>TYPE_SCHEM</code>  and
   * <code>TYPE_NAME</code>.  The type name parameter may be a fully-qualified
   * name.  In this case, the catalog and schemaPattern parameters are
   * ignored.
   *
   * <P>Each type description has the following columns:
   *  <OL>
   *  <LI><B>TYPE_CAT</B> String {@code =>} the type's catalog (may be <code>null</code>)
   *  <LI><B>TYPE_SCHEM</B> String {@code =>} type's schema (may be <code>null</code>)
   *  <LI><B>TYPE_NAME</B> String {@code =>} type name
   *  <LI><B>CLASS_NAME</B> String {@code =>} Java class name
   *  <LI><B>DATA_TYPE</B> int {@code =>} type value defined in java.sql.Types.
   *     One of JAVA_OBJECT, STRUCT, or DISTINCT
   *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the type
   *  <LI><B>BASE_TYPE</B> short {@code =>} type code of the source type of a
   *     DISTINCT type or the type that implements the user-generated
   *     reference type of the SELF_REFERENCING_COLUMN of a structured
   *     type as defined in java.sql.Types (<code>null</code> if DATA_TYPE is not
   *     DISTINCT or not STRUCT with REFERENCE_GENERATION = USER_DEFINED)
   *  </OL>
   *
   * <P><B>Note:</B> If the driver does not support UDTs, an empty
   * result set is returned.
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema pattern name; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param typeNamePattern a type name pattern; must match the type name
   *        as it is stored in the database; may be a fully qualified name
   * @param types a list of user-defined types (JAVA_OBJECT,
   *        STRUCT, or DISTINCT) to include; <code>null</code> returns all types
   * @return <code>ResultSet</code> object in which each row describes a UDT
   */
  def getUDTs(catalog: String, schemaPattern: String, typeNamePattern: String, types: Array[Int]): ResultSet[F]

  /**
   * Retrieves the connection that produced this metadata object.
   *
   * @return the connection that produced this metadata object
   */
  def getConnection(): Connection[F]

  /**
   * Retrieves whether this database supports savepoints.
   *
   * @return <code>true</code> if savepoints are supported;
   *         <code>false</code> otherwise
   */
  def supportsSavepoints(): Boolean

  /**
   * Retrieves whether this database supports named parameters to callable
   * statements.
   *
   * @return <code>true</code> if named parameters are supported;
   *         <code>false</code> otherwise
   */
  def supportsNamedParameters(): Boolean

  /**
   * Retrieves whether it is possible to have multiple <code>ResultSet</code> objects
   * returned from a <code>CallableStatement</code> object
   * simultaneously.
   *
   * @return <code>true</code> if a <code>CallableStatement</code> object
   *         can return multiple <code>ResultSet</code> objects
   *         simultaneously; <code>false</code> otherwise
   */
  def supportsMultipleOpenResults(): Boolean

  /**
   * Retrieves whether auto-generated keys can be retrieved after
   * a statement has been executed
   *
   * @return <code>true</code> if auto-generated keys can be retrieved
   *         after a statement has executed; <code>false</code> otherwise
   * <p>If <code>true</code> is returned, the JDBC driver must support the
   * returning of auto-generated keys for at least SQL INSERT statements
   */
  def supportsGetGeneratedKeys(): Boolean

  /**
   * Retrieves a description of the user-defined type (UDT) hierarchies defined in a
   * particular schema in this database. Only the immediate super type/
   * sub type relationship is modeled.
   * <P>
   * Only supertype information for UDTs matching the catalog,
   * schema, and type name is returned. The type name parameter
   * may be a fully-qualified name. When the UDT name supplied is a
   * fully-qualified name, the catalog and schemaPattern parameters are
   * ignored.
   * <P>
   * If a UDT does not have a direct super type, it is not listed here.
   * A row of the <code>ResultSet</code> object returned by this method
   * describes the designated UDT and a direct supertype. A row has the following
   * columns:
   *  <OL>
   *  <LI><B>TYPE_CAT</B> String {@code =>} the UDT's catalog (may be <code>null</code>)
   *  <LI><B>TYPE_SCHEM</B> String {@code =>} UDT's schema (may be <code>null</code>)
   *  <LI><B>TYPE_NAME</B> String {@code =>} type name of the UDT
   *  <LI><B>SUPERTYPE_CAT</B> String {@code =>} the direct super type's catalog
   *                           (may be <code>null</code>)
   *  <LI><B>SUPERTYPE_SCHEM</B> String {@code =>} the direct super type's schema
   *                             (may be <code>null</code>)
   *  <LI><B>SUPERTYPE_NAME</B> String {@code =>} the direct super type's name
   *  </OL>
   *
   * <P><B>Note:</B> If the driver does not support type hierarchies, an
   * empty result set is returned.
   *
   * @param catalog a catalog name; "" retrieves those without a catalog;
   *        <code>null</code> means drop catalog name from the selection criteria
   * @param schemaPattern a schema name pattern; "" retrieves those
   *        without a schema
   * @param typeNamePattern a UDT name pattern; may be a fully-qualified
   *        name
   * @return a <code>ResultSet</code> object in which a row gives information
   *         about the designated UDT
   */
  def getSuperTypes(catalog: String, schemaPattern: String, typeNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of the table hierarchies defined in a particular
   * schema in this database.
   *
   * <P>Only supertable information for tables matching the catalog, schema
   * and table name are returned. The table name parameter may be a fully-
   * qualified name, in which case, the catalog and schemaPattern parameters
   * are ignored. If a table does not have a super table, it is not listed here.
   * Supertables have to be defined in the same catalog and schema as the
   * sub tables. Therefore, the type description does not need to include
   * this information for the supertable.
   *
   * <P>Each type description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} the type's catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} type's schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} type name
   *  <LI><B>SUPERTABLE_NAME</B> String {@code =>} the direct super type's name
   *  </OL>
   *
   * <P><B>Note:</B> If the driver does not support type hierarchies, an
   * empty result set is returned.
   *
   * @param catalog a catalog name; "" retrieves those without a catalog;
   *        <code>null</code> means drop catalog name from the selection criteria
   * @param schemaPattern a schema name pattern; "" retrieves those
   *        without a schema
   * @param tableNamePattern a table name pattern; may be a fully-qualified
   *        name
   * @return a <code>ResultSet</code> object in which each row is a type description
   */
  def getSuperTables(catalog: String, schemaPattern: String, tableNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of the given attribute of the given type
   * for a user-defined type (UDT) that is available in the given schema
   * and catalog.
   * <P>
   * Descriptions are returned only for attributes of UDTs matching the
   * catalog, schema, type, and attribute name criteria. They are ordered by
   * <code>TYPE_CAT</code>, <code>TYPE_SCHEM</code>,
   * <code>TYPE_NAME</code> and <code>ORDINAL_POSITION</code>. This description
   * does not contain inherited attributes.
   * <P>
   * The <code>ResultSet</code> object that is returned has the following
   * columns:
   * <OL>
   *  <LI><B>TYPE_CAT</B> String {@code =>} type catalog (may be <code>null</code>)
   *  <LI><B>TYPE_SCHEM</B> String {@code =>} type schema (may be <code>null</code>)
   *  <LI><B>TYPE_NAME</B> String {@code =>} type name
   *  <LI><B>ATTR_NAME</B> String {@code =>} attribute name
   *  <LI><B>DATA_TYPE</B> int {@code =>} attribute type SQL type from java.sql.Types
   *  <LI><B>ATTR_TYPE_NAME</B> String {@code =>} Data source dependent type name.
   *  For a UDT, the type name is fully qualified. For a REF, the type name is
   *  fully qualified and represents the target type of the reference type.
   *  <LI><B>ATTR_SIZE</B> int {@code =>} column size.  For char or date
   *      types this is the maximum number of characters; for numeric or
   *      decimal types this is precision.
   *  <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
   * DECIMAL_DIGITS is not applicable.
   *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
   *  <LI><B>NULLABLE</B> int {@code =>} whether NULL is allowed
   *      <UL>
   *      <LI> attributeNoNulls - might not allow NULL values
   *      <LI> attributeNullable - definitely allows NULL values
   *      <LI> attributeNullableUnknown - nullability unknown
   *      </UL>
   *  <LI><B>REMARKS</B> String {@code =>} comment describing column (may be <code>null</code>)
   *  <LI><B>ATTR_DEF</B> String {@code =>} default value (may be <code>null</code>)
   *  <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
   *  <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
   *  <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
   *       maximum number of bytes in the column
   *  <LI><B>ORDINAL_POSITION</B> int {@code =>} index of the attribute in the UDT
   *      (starting at 1)
   *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine
   * the nullability for a attribute.
   *       <UL>
   *       <LI> YES           --- if the attribute can include NULLs
   *       <LI> NO            --- if the attribute cannot include NULLs
   *       <LI> empty string  --- if the nullability for the
   * attribute is unknown
   *       </UL>
   *  <LI><B>SCOPE_CATALOG</B> String {@code =>} catalog of table that is the
   *      scope of a reference attribute (<code>null</code> if DATA_TYPE isn't REF)
   *  <LI><B>SCOPE_SCHEMA</B> String {@code =>} schema of table that is the
   *      scope of a reference attribute (<code>null</code> if DATA_TYPE isn't REF)
   *  <LI><B>SCOPE_TABLE</B> String {@code =>} table name that is the scope of a
   *      reference attribute (<code>null</code> if the DATA_TYPE isn't REF)
   * <LI><B>SOURCE_DATA_TYPE</B> short {@code =>} source type of a distinct type or user-generated
   *      Ref type,SQL type from java.sql.Types (<code>null</code> if DATA_TYPE
   *      isn't DISTINCT or user-generated REF)
   *  </OL>
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param typeNamePattern a type name pattern; must match the
   *        type name as it is stored in the database
   * @param attributeNamePattern an attribute name pattern; must match the attribute
   *        name as it is declared in the database
   * @return a <code>ResultSet</code> object in which each row is an
   *         attribute description
   */
  def getAttributes(catalog: String, schemaPattern: String, typeNamePattern: String, attributeNamePattern: String): ResultSet[F]

  /**
   * Retrieves whether this database supports the given result set holdability.
   *
   * @param holdability one of the following constants:
   *          <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
   *          <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsResultSetHoldability(holdability: Int): Boolean

  /**
   * Retrieves this database's default holdability for <code>ResultSet</code>
   * objects.
   *
   * @return the default holdability; either
   *         <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
   *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
   */
  def getResultSetHoldability(): Int

  /**
   * Retrieves the major version number of the underlying database.
   *
   * @return the underlying database's major version
   */
  def getDatabaseMajorVersion(): Int

  /**
   * Retrieves the minor version number of the underlying database.
   *
   * @return underlying database's minor version
   */
  def getDatabaseMinorVersion(): Int

  /**
   * Retrieves the major JDBC version number for this
   * driver.
   *
   * @return JDBC version major number
   */
  def getJDBCMajorVersion(): Int

  /**
   * Retrieves the minor JDBC version number for this
   * driver.
   *
   * @return JDBC version minor number
   */
  def getJDBCMinorVersion(): Int

  /**
   * Indicates whether the SQLSTATE returned by <code>SQLException.getSQLState</code>
   * is X/Open (now known as Open Group) SQL CLI or SQL:2003.
   * @return the type of SQLSTATE; one of:
   *        sqlStateXOpen or
   *        sqlStateSQL
   */
  def getSQLStateType(): Int

  /**
   * Indicates whether updates made to a LOB are made on a copy or directly
   * to the LOB.
   * @return <code>true</code> if updates are made to a copy of the LOB;
   *         <code>false</code> if updates are made directly to the LOB
   */
  def locatorsUpdateCopy(): Boolean

  /**
   * Retrieves whether this database supports statement pooling.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsStatementPooling(): Boolean

  /**
   * Indicates whether this data source supports the SQL {@code  ROWID} type,
   * and the lifetime for which a {@link  RowId} object remains valid.
   *
   * @return the status indicating the lifetime of a {@code  RowId}
   */
  def getRowIdLifetime(): RowIdLifetime

  /**
   * Retrieves the schema names available in this database.  The results
   * are ordered by <code>TABLE_CATALOG</code> and
   * <code>TABLE_SCHEM</code>.
   *
   * <P>The schema columns are:
   *  <OL>
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} schema name
   *  <LI><B>TABLE_CATALOG</B> String {@code =>} catalog name (may be <code>null</code>)
   *  </OL>
   *
   *
   * @param catalog a catalog name; must match the catalog name as it is stored
   * in the database;"" retrieves those without a catalog; null means catalog
   * name should not be used to narrow down the search.
   * @param schemaPattern a schema name; must match the schema name as it is
   * stored in the database; null means
   * schema name should not be used to narrow down the search.
   * @return a <code>ResultSet</code> object in which each row is a
   *         schema description
   */
  def getSchemas(catalog: String, schemaPattern: String): ResultSet[F]

  /**
   * Retrieves whether this database supports invoking user-defined or vendor functions
   * using the stored procedure escape syntax.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def supportsStoredFunctionsUsingCallSyntax(): Boolean

  /**
   * Retrieves whether a <code>SQLException</code> while autoCommit is <code>true</code> indicates
   * that all open ResultSets are closed, even ones that are holdable.  When a <code>SQLException</code> occurs while
   * autocommit is <code>true</code>, it is vendor specific whether the JDBC driver responds with a commit operation, a
   * rollback operation, or by doing neither a commit nor a rollback.  A potential result of this difference
   * is in whether or not holdable ResultSets are closed.
   *
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def autoCommitFailureClosesAllResultSets(): Boolean

  /**
   * Retrieves a list of the client info properties
   * that the driver supports.  The result set contains the following columns
   *
   * <ol>
   * <li><b>NAME</b> String{@code =>} The name of the client info property<br>
   * <li><b>MAX_LEN</b> int{@code =>} The maximum length of the value for the property<br>
   * <li><b>DEFAULT_VALUE</b> String{@code =>} The default value of the property<br>
   * <li><b>DESCRIPTION</b> String{@code =>} A description of the property.  This will typically
   *                                              contain information as to where this property is
   *                                              stored in the database.
   * </ol>
   * <p>
   * The <code>ResultSet</code> is sorted by the NAME column
   *
   * @return      A <code>ResultSet</code> object; each row is a supported client info
   * property
   */
  def getClientInfoProperties(): ResultSet[F]

  /**
   * Retrieves a description of the  system and user functions available
   * in the given catalog.
   * <P>
   * Only system and user function descriptions matching the schema and
   * function name criteria are returned.  They are ordered by
   * <code>FUNCTION_CAT</code>, <code>FUNCTION_SCHEM</code>,
   * <code>FUNCTION_NAME</code> and
   * <code>SPECIFIC_ NAME</code>.
   *
   * <P>Each function description has the following columns:
   *  <OL>
   *  <LI><B>FUNCTION_CAT</B> String {@code =>} function catalog (may be <code>null</code>)
   *  <LI><B>FUNCTION_SCHEM</B> String {@code =>} function schema (may be <code>null</code>)
   *  <LI><B>FUNCTION_NAME</B> String {@code =>} function name.  This is the name
   * used to invoke the function
   *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the function
   * <LI><B>FUNCTION_TYPE</B> short {@code =>} kind of function:
   *      <UL>
   *      <LI>functionResultUnknown - Cannot determine if a return value
   *       or table will be returned
   *      <LI> functionNoTable- Does not return a table
   *      <LI> functionReturnsTable - Returns a table
   *      </UL>
   *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies
   *  this function within its schema.  This is a user specified, or DBMS
   * generated, name that may be different then the <code>FUNCTION_NAME</code>
   * for example with overload functions
   *  </OL>
   * <p>
   * A user may not have permission to execute any of the functions that are
   * returned by <code>getFunctions</code>
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param functionNamePattern a function name pattern; must match the
   *        function name as it is stored in the database
   * @return <code>ResultSet</code> - each row is a function description
   */
  def getFunctions(catalog: String, schemaPattern: String, functionNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of the given catalog's system or user
   * function parameters and return type.
   *
   * <P>Only descriptions matching the schema,  function and
   * parameter name criteria are returned. They are ordered by
   * <code>FUNCTION_CAT</code>, <code>FUNCTION_SCHEM</code>,
   * <code>FUNCTION_NAME</code> and
   * <code>SPECIFIC_ NAME</code>. Within this, the return value,
   * if any, is first. Next are the parameter descriptions in call
   * order. The column descriptions follow in column number order.
   *
   * <P>Each row in the <code>ResultSet</code>
   * is a parameter description, column description or
   * return type description with the following fields:
   *  <OL>
   *  <LI><B>FUNCTION_CAT</B> String {@code =>} function catalog (may be <code>null</code>)
   *  <LI><B>FUNCTION_SCHEM</B> String {@code =>} function schema (may be <code>null</code>)
   *  <LI><B>FUNCTION_NAME</B> String {@code =>} function name.  This is the name
   * used to invoke the function
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column/parameter name
   *  <LI><B>COLUMN_TYPE</B> Short {@code =>} kind of column/parameter:
   *      <UL>
   *      <LI> functionColumnUnknown - nobody knows
   *      <LI> functionColumnIn - IN parameter
   *      <LI> functionColumnInOut - INOUT parameter
   *      <LI> functionColumnOut - OUT parameter
   *      <LI> functionColumnReturn - function return value
   *      <LI> functionColumnResult - Indicates that the parameter or column
   *  is a column in the <code>ResultSet</code>
   *      </UL>
   *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
   *  <LI><B>TYPE_NAME</B> String {@code =>} SQL type name, for a UDT type the
   *  type name is fully qualified
   *  <LI><B>PRECISION</B> int {@code =>} precision
   *  <LI><B>LENGTH</B> int {@code =>} length in bytes of data
   *  <LI><B>SCALE</B> short {@code =>} scale -  null is returned for data types where
   * SCALE is not applicable.
   *  <LI><B>RADIX</B> short {@code =>} radix
   *  <LI><B>NULLABLE</B> short {@code =>} can it contain NULL.
   *      <UL>
   *      <LI> functionNoNulls - does not allow NULL values
   *      <LI> functionNullable - allows NULL values
   *      <LI> functionNullableUnknown - nullability unknown
   *      </UL>
   *  <LI><B>REMARKS</B> String {@code =>} comment describing column/parameter
   *  <LI><B>CHAR_OCTET_LENGTH</B> int  {@code =>} the maximum length of binary
   * and character based parameters or columns.  For any other datatype the returned value
   * is a NULL
   *  <LI><B>ORDINAL_POSITION</B> int  {@code =>} the ordinal position, starting
   * from 1, for the input and output parameters. A value of 0
   * is returned if this row describes the function's return value.
   * For result set columns, it is the
   * ordinal position of the column in the result set starting from 1.
   *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine
   * the nullability for a parameter or column.
   *       <UL>
   *       <LI> YES           --- if the parameter or column can include NULLs
   *       <LI> NO            --- if the parameter or column  cannot include NULLs
   *       <LI> empty string  --- if the nullability for the
   * parameter  or column is unknown
   *       </UL>
   *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies
   * this function within its schema.  This is a user specified, or DBMS
   * generated, name that may be different then the <code>FUNCTION_NAME</code>
   * for example with overload functions
   *  </OL>
   *
   * <p>The PRECISION column represents the specified column size for the given
   * parameter or column.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. Null is returned for data types where the
   * column size is not applicable.
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param functionNamePattern a procedure name pattern; must match the
   *        function name as it is stored in the database
   * @param columnNamePattern a parameter name pattern; must match the
   * parameter or column name as it is stored in the database
   * @return <code>ResultSet</code> - each row describes a
   * user function parameter, column  or return type
   */
  def getFunctionColumns(catalog: String, schemaPattern: String, functionNamePattern: String, columnNamePattern: String): ResultSet[F]

  /**
   * Retrieves a description of the pseudo or hidden columns available
   * in a given table within the specified catalog and schema.
   * Pseudo or hidden columns may not always be stored within
   * a table and are not visible in a ResultSet unless they are
   * specified in the query's outermost SELECT list. Pseudo or hidden
   * columns may not necessarily be able to be modified. If there are
   * no pseudo or hidden columns, an empty ResultSet is returned.
   *
   * <P>Only column descriptions matching the catalog, schema, table
   * and column name criteria are returned.  They are ordered by
   * <code>TABLE_CAT</code>,<code>TABLE_SCHEM</code>, <code>TABLE_NAME</code>
   * and <code>COLUMN_NAME</code>.
   *
   * <P>Each column description has the following columns:
   *  <OL>
   *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
   *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
   *  <LI><B>TABLE_NAME</B> String {@code =>} table name
   *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
   *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
   *  <LI><B>COLUMN_SIZE</B> int {@code =>} column size.
   *  <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
   * DECIMAL_DIGITS is not applicable.
   *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
   *  <LI><B>COLUMN_USAGE</B> String {@code =>} The allowed usage for the column.  The
   *  value returned will correspond to the enum name returned by {@link PseudoColumnUsage#name PseudoColumnUsage.name()}
   *  <LI><B>REMARKS</B> String {@code =>} comment describing column (may be <code>null</code>)
   *  <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
   *       maximum number of bytes in the column
   *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
   *       <UL>
   *       <LI> YES           --- if the column can include NULLs
   *       <LI> NO            --- if the column cannot include NULLs
   *       <LI> empty string  --- if the nullability for the column is unknown
   *       </UL>
   *  </OL>
   *
   * <p>The COLUMN_SIZE column specifies the column size for the given column.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. Null is returned for data types where the
   * column size is not applicable.
   *
   * @param catalog a catalog name; must match the catalog name as it
   *        is stored in the database; "" retrieves those without a catalog;
   *        <code>null</code> means that the catalog name should not be used to narrow
   *        the search
   * @param schemaPattern a schema name pattern; must match the schema name
   *        as it is stored in the database; "" retrieves those without a schema;
   *        <code>null</code> means that the schema name should not be used to narrow
   *        the search
   * @param tableNamePattern a table name pattern; must match the
   *        table name as it is stored in the database
   * @param columnNamePattern a column name pattern; must match the column
   *        name as it is stored in the database
   * @return <code>ResultSet</code> - each row is a column description
   */
  def getPseudoColumns(catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String): ResultSet[F]

  /**
   * Retrieves whether a generated key will always be returned if the column
   * name(s) or index(es) specified for the auto generated key column(s)
   * are valid and the statement succeeds.  The key that is returned may or
   * may not be based on the column(s) for the auto generated key.
   * Consult your JDBC driver documentation for additional details.
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def generatedKeyAlwaysReturned(): Boolean

  /**
   * Retrieves the maximum number of bytes this database allows for
   * the logical size for a {@code LOB}.
   *<p>
   * The default implementation will return {@code 0}
   *
   * @return the maximum number of bytes allowed; a result of zero
   * means that there is no limit or the limit is not known
   */
  def getMaxLogicalLobSize(): Long = 0

  /**
   * Retrieves whether this database supports REF CURSOR.
   *<p>
   * The default implementation will return {@code false}
   *
   * @return {@code true} if this database supports REF CURSOR;
   *         {@code false} otherwise
   */
  def supportsRefCursors(): Boolean = false

  /**
   * Retrieves whether this database supports sharding.
   * @implSpec
   * The default implementation will return {@code false}
   *
   * @return {@code true} if this database supports sharding;
   *         {@code false} otherwise
   */
  def supportsSharding(): Boolean = false

object DatabaseMetaData:
  
  private val SQL2003_KEYWORDS = List(
    "ABS", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "ARRAY", "AS",
    "ASENSITIVE", "ASYMMETRIC", "AT", "ATOMIC", "AUTHORIZATION", "AVG", "BEGIN", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOOLEAN", "BOTH", "BY", "CALL",
    "CALLED", "CARDINALITY", "CASCADED", "CASE", "CAST", "CEIL", "CEILING", "CHAR", "CHARACTER", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK", "CLOB",
    "CLOSE", "COALESCE", "COLLATE", "COLLECT", "COLUMN", "COMMIT", "CONDITION", "CONNECT", "CONSTRAINT", "CONVERT", "CORR", "CORRESPONDING", "COUNT",
    "COVAR_POP", "COVAR_SAMP", "CREATE", "CROSS", "CUBE", "CUME_DIST", "CURRENT", "CURRENT_DATE", "CURRENT_DEFAULT_TRANSFORM_GROUP", "CURRENT_PATH",
    "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_TRANSFORM_GROUP_FOR_TYPE", "CURRENT_USER", "CURSOR", "CYCLE", "DATE", "DAY",
    "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELETE", "DENSE_RANK", "DEREF", "DESCRIBE", "DETERMINISTIC", "DISCONNECT", "DISTINCT",
    "DOUBLE", "DROP", "DYNAMIC", "EACH", "ELEMENT", "ELSE", "END", "END-EXEC", "ESCAPE", "EVERY", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXP",
    "EXTERNAL", "EXTRACT", "FALSE", "FETCH", "FILTER", "FLOAT", "FLOOR", "FOR", "FOREIGN", "FREE", "FROM", "FULL", "FUNCTION", "FUSION", "GET",
    "GLOBAL", "GRANT", "GROUP", "GROUPING", "HAVING", "HOLD", "HOUR", "IDENTITY", "IN", "INDICATOR", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT",
    "INTEGER", "INTERSECT", "INTERSECTION", "INTERVAL", "INTO", "IS", "JOIN", "LANGUAGE", "LARGE", "LATERAL", "LEADING", "LEFT", "LIKE", "LN", "LOCAL",
    "LOCALTIME", "LOCALTIMESTAMP", "LOWER", "MATCH", "MAX", "MEMBER", "MERGE", "METHOD", "MIN", "MINUTE", "MOD", "MODIFIES", "MODULE", "MONTH",
    "MULTISET", "NATIONAL", "NATURAL", "NCHAR", "NCLOB", "NEW", "NO", "NONE", "NORMALIZE", "NOT", "NULL", "NULLIF", "NUMERIC", "OCTET_LENGTH", "OF",
    "OLD", "ON", "ONLY", "OPEN", "OR", "ORDER", "OUT", "OUTER", "OVER", "OVERLAPS", "OVERLAY", "PARAMETER", "PARTITION", "PERCENTILE_CONT",
    "PERCENTILE_DISC", "PERCENT_RANK", "POSITION", "POWER", "PRECISION", "PREPARE", "PRIMARY", "PROCEDURE", "RANGE", "RANK", "READS", "REAL",
    "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT", "REGR_R2", "REGR_SLOPE", "REGR_SXX",
    "REGR_SXY", "REGR_SYY", "RELEASE", "RESULT", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROW", "ROWS", "ROW_NUMBER", "SAVEPOINT",
    "SCOPE", "SCROLL", "SEARCH", "SECOND", "SELECT", "SENSITIVE", "SESSION_USER", "SET", "SIMILAR", "SMALLINT", "SOME", "SPECIFIC", "SPECIFICTYPE",
    "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQRT", "START", "STATIC", "STDDEV_POP", "STDDEV_SAMP", "SUBMULTISET", "SUBSTRING", "SUM",
    "SYMMETRIC", "SYSTEM", "SYSTEM_USER", "TABLE", "TABLESAMPLE", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING",
    "TRANSLATE", "TRANSLATION", "TREAT", "TRIGGER", "TRIM", "TRUE", "UESCAPE", "UNION", "UNIQUE", "UNKNOWN", "UNNEST", "UPDATE", "UPPER", "USER",
    "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "VAR_POP", "VAR_SAMP", "WHEN", "WHENEVER", "WHERE", "WIDTH_BUCKET", "WINDOW", "WITH", "WITHIN",
    "WITHOUT", "YEAR"
  )

  private val maxBufferSize = 65535
  
  enum DatabaseTerm:
    case SCHEMA, CATALOG

  /**
   * Indicates that it is not known whether the procedure returns
   * a result.
   * <P>
   * A possible value for column <code>PROCEDURE_TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getProcedures</code>.
   */
  val procedureResultUnknown: Int = 0

  /**
   * Indicates that the procedure does not return a result.
   * <P>
   * A possible value for column <code>PROCEDURE_TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getProcedures</code>.
   */
  val procedureNoResult: Int = 1

  /**
   * Indicates that the procedure returns a result.
   * <P>
   * A possible value for column <code>PROCEDURE_TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getProcedures</code>.
   */
  val procedureReturnsResult: Int = 2

  /**
   * Indicates that type of the column is unknown.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureColumnUnknown: Int = 0

  /**
   * Indicates that the column stores IN parameters.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureColumnIn: Int = 1

  /**
   * Indicates that the column stores INOUT parameters.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureColumnInOut: Int = 2

  /**
   * Indicates that the column stores results.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureColumnResult: Int = 3

  /**
   * Indicates that the column stores OUT parameters.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureColumnOut: Int = 4

  /**
   * Indicates that the column stores return values.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureColumnReturn: Int = 5

  /**
   * Indicates that <code>NULL</code> values are not allowed.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureNoNulls: Int = 0

  /**
   * Indicates that <code>NULL</code> values are allowed.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureNullable: Int = 1

  /**
   * Indicates that whether <code>NULL</code> values are allowed
   * is unknown.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getProcedureColumns</code>.
   */
  val procedureNullableUnknown: Int = 2

  /**
   * Indicates that the column might not allow <code>NULL</code> values.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> returned by the method
   * <code>getColumns</code>.
   */
  val columnNoNulls: Int = 0

  /**
   * Indicates that the column definitely allows <code>NULL</code> values.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> returned by the method
   * <code>getColumns</code>.
   */
  val columnNullable: Int = 1

  /**
   * Indicates that the nullability of columns is unknown.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> returned by the method
   * <code>getColumns</code>.
   */
  val columnNullableUnknown: Int = 2

  /**
   * Indicates that the scope of the best row identifier is
   * very temporary, lasting only while the
   * row is being used.
   * <P>
   * A possible value for the column
   * <code>SCOPE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getBestRowIdentifier</code>.
   */
  val bestRowTemporary: Int = 0

  /**
   * Indicates that the scope of the best row identifier is
   * the remainder of the current transaction.
   * <P>
   * A possible value for the column
   * <code>SCOPE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getBestRowIdentifier</code>.
   */
  val bestRowTransaction: Int = 1

  /**
   * Indicates that the scope of the best row identifier is
   * the remainder of the current session.
   * <P>
   * A possible value for the column
   * <code>SCOPE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getBestRowIdentifier</code>.
   */
  val bestRowSession: Int = 2

  /**
   * Indicates that the best row identifier may or may not be a pseudo column.
   * <P>
   * A possible value for the column
   * <code>PSEUDO_COLUMN</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getBestRowIdentifier</code>.
   */
  val bestRowUnknown: Int = 0

  /**
   * Indicates that the best row identifier is NOT a pseudo column.
   * <P>
   * A possible value for the column
   * <code>PSEUDO_COLUMN</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getBestRowIdentifier</code>.
   */
  val bestRowNotPseudo: Int = 1

  /**
   * Indicates that the best row identifier is a pseudo column.
   * <P>
   * A possible value for the column
   * <code>PSEUDO_COLUMN</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getBestRowIdentifier</code>.
   */
  val bestRowPseudo: Int = 2

  /**
   * Indicates that this version column may or may not be a pseudo column.
   * <P>
   * A possible value for the column
   * <code>PSEUDO_COLUMN</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getVersionColumns</code>.
   */
  val versionColumnUnknown: Int = 0

  /**
   * Indicates that this version column is NOT a pseudo column.
   * <P>
   * A possible value for the column
   * <code>PSEUDO_COLUMN</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getVersionColumns</code>.
   */
  val versionColumnNotPseudo: Int = 1

  /**
   * Indicates that this version column is a pseudo column.
   * <P>
   * A possible value for the column
   * <code>PSEUDO_COLUMN</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getVersionColumns</code>.
   */
  val versionColumnPseudo: Int = 2

  /**
   * For the column <code>UPDATE_RULE</code>,
   * indicates that
   * when the primary key is updated, the foreign key (imported key)
   * is changed to agree with it.
   * For the column <code>DELETE_RULE</code>,
   * it indicates that
   * when the primary key is deleted, rows that imported that key
   * are deleted.
   * <P>
   * A possible value for the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code> in the
   * <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeyCascade: Int = 0

  /**
   * For the column <code>UPDATE_RULE</code>, indicates that
   * a primary key may not be updated if it has been imported by
   * another table as a foreign key.
   * For the column <code>DELETE_RULE</code>, indicates that
   * a primary key may not be deleted if it has been imported by
   * another table as a foreign key.
   * <P>
   * A possible value for the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code> in the
   * <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeyRestrict: Int = 1

  /**
   * For the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code>, indicates that
   * when the primary key is updated or deleted, the foreign key (imported key)
   * is changed to <code>NULL</code>.
   * <P>
   * A possible value for the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code> in the
   * <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeySetNull: Int = 2

  /**
   * For the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code>, indicates that
   * if the primary key has been imported, it cannot be updated or deleted.
   * <P>
   * A possible value for the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code> in the
   * <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeyNoAction: Int = 3

  /**
   * For the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code>, indicates that
   * if the primary key is updated or deleted, the foreign key (imported key)
   * is set to the default value.
   * <P>
   * A possible value for the columns <code>UPDATE_RULE</code>
   * and <code>DELETE_RULE</code> in the
   * <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeySetDefault: Int = 4

  /**
   * Indicates deferrability.  See SQL-92 for a definition.
   * <P>
   * A possible value for the column <code>DEFERRABILITY</code>
   * in the <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeyInitiallyDeferred: Int = 5

  /**
   * Indicates deferrability.  See SQL-92 for a definition.
   * <P>
   * A possible value for the column <code>DEFERRABILITY</code>
   * in the <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeyInitiallyImmediate: Int = 6

  /**
   * Indicates deferrability.  See SQL-92 for a definition.
   * <P>
   * A possible value for the column <code>DEFERRABILITY</code>
   * in the <code>ResultSet</code> objects returned by the methods
   * <code>getImportedKeys</code>,  <code>getExportedKeys</code>,
   * and <code>getCrossReference</code>.
   */
  val importedKeyNotDeferrable: Int = 7

  /**
   * Indicates that a <code>NULL</code> value is NOT allowed for this
   * data type.
   * <P>
   * A possible value for column <code>NULLABLE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getTypeInfo</code>.
   */
  val typeNoNulls: Int = 0

  /**
   * Indicates that a <code>NULL</code> value is allowed for this
   * data type.
   * <P>
   * A possible value for column <code>NULLABLE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getTypeInfo</code>.
   */
  val typeNullable: Int = 1

  /**
   * Indicates that it is not known whether a <code>NULL</code> value
   * is allowed for this data type.
   * <P>
   * A possible value for column <code>NULLABLE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getTypeInfo</code>.
   */
  val typeNullableUnknown: Int = 2

  /**
   * Indicates that <code>WHERE</code> search clauses are not supported
   * for this type.
   * <P>
   * A possible value for column <code>SEARCHABLE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getTypeInfo</code>.
   */
  val typePredNone: Int = 0

  /**
   * Indicates that the data type
   * can be only be used in <code>WHERE</code> search clauses
   * that  use <code>LIKE</code> predicates.
   * <P>
   * A possible value for column <code>SEARCHABLE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getTypeInfo</code>.
   */
  val typePredChar: Int = 1

  /**
   * Indicates that the data type can be only be used in <code>WHERE</code>
   * search clauses
   * that do not use <code>LIKE</code> predicates.
   * <P>
   * A possible value for column <code>SEARCHABLE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getTypeInfo</code>.
   */
  val typePredBasic: Int = 2

  /**
   * Indicates that all <code>WHERE</code> search clauses can be
   * based on this type.
   * <P>
   * A possible value for column <code>SEARCHABLE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getTypeInfo</code>.
   */
  val typeSearchable: Int = 3

  /**
   * Indicates that this column contains table statistics that
   * are returned in conjunction with a table's index descriptions.
   * <P>
   * A possible value for column <code>TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getIndexInfo</code>.
   */
  val tableIndexStatistic: Short = 0

  /**
   * Indicates that this table index is a clustered index.
   * <P>
   * A possible value for column <code>TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getIndexInfo</code>.
   */
  val tableIndexClustered: Short = 1

  /**
   * Indicates that this table index is a hashed index.
   * <P>
   * A possible value for column <code>TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getIndexInfo</code>.
   */
  val tableIndexHashed: Short = 2

  /**
   * Indicates that this table index is not a clustered
   * index, a hashed index, or table statistics;
   * it is something other than these.
   * <P>
   * A possible value for column <code>TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getIndexInfo</code>.
   */
  val tableIndexOther: Short = 3

  /**
   * Indicates that <code>NULL</code> values might not be allowed.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code> in the <code>ResultSet</code> object
   * returned by the method <code>getAttributes</code>.
   */
  val attributeNoNulls: Short = 0

  /**
   * Indicates that <code>NULL</code> values are definitely allowed.
   * <P>
   * A possible value for the column <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getAttributes</code>.
   */
  val attributeNullable: Short = 1

  /**
   * Indicates that whether <code>NULL</code> values are allowed is not
   * known.
   * <P>
   * A possible value for the column <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getAttributes</code>.
   */
  val attributeNullableUnknown: Short = 2

  /**
   * A possible return value for the method
   * <code>DatabaseMetaData.getSQLStateType</code> which is used to indicate
   * whether the value returned by the method
   * <code>SQLException.getSQLState</code> is an
   * X/Open (now know as Open Group) SQL CLI SQLSTATE value.
   */
  val sqlStateXOpen: Int = 1

  /**
   * A possible return value for the method
   * <code>DatabaseMetaData.getSQLStateType</code> which is used to indicate
   * whether the value returned by the method
   * <code>SQLException.getSQLState</code> is an SQLSTATE value.
   */
  val sqlStateSQL: Int = 2

  /**
   * A possible return value for the method
   * <code>DatabaseMetaData.getSQLStateType</code> which is used to indicate
   * whether the value returned by the method
   * <code>SQLException.getSQLState</code> is an SQL99 SQLSTATE value.
   * <P>
   * <b>Note:</b>This constant remains only for compatibility reasons. Developers
   * should use the constant <code>sqlStateSQL</code> instead.
   */
  val sqlStateSQL99: Int = sqlStateSQL

  /**
   * Indicates that type of the parameter or column is unknown.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionColumnUnknown: Int = 0

  /**
   * Indicates that the parameter or column is an IN parameter.
   * <P>
   *  A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionColumnIn: Int = 1

  /**
   * Indicates that the parameter or column is an INOUT parameter.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionColumnInOut: Int = 2

  /**
   * Indicates that the parameter or column is an OUT parameter.
   * <P>
   * A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionColumnOut: Int = 3

  /**
   * Indicates that the parameter or column is a return value.
   * <P>
   *  A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionReturn: Int = 4

  /**
   * Indicates that the parameter or column is a column in a result set.
   * <P>
   *  A possible value for the column
   * <code>COLUMN_TYPE</code>
   * in the <code>ResultSet</code>
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionColumnResult: Int = 5

  /**
   * Indicates that <code>NULL</code> values are not allowed.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionNoNulls: Int = 0

  /**
   * Indicates that <code>NULL</code> values are allowed.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionNullable: Int = 1

  /**
   * Indicates that whether <code>NULL</code> values are allowed
   * is unknown.
   * <P>
   * A possible value for the column
   * <code>NULLABLE</code>
   * in the <code>ResultSet</code> object
   * returned by the method <code>getFunctionColumns</code>.
   */
  val functionNullableUnknown: Int = 2

  /**
   * Indicates that it is not known whether the function returns
   * a result or a table.
   * <P>
   * A possible value for column <code>FUNCTION_TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getFunctions</code>.
   */
  val functionResultUnknown: Int = 0

  /**
   * Indicates that the function  does not return a table.
   * <P>
   * A possible value for column <code>FUNCTION_TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getFunctions</code>.
   */
  val functionNoTable: Int = 1

  /**
   * Indicates that the function  returns a table.
   * <P>
   * A possible value for column <code>FUNCTION_TYPE</code> in the
   * <code>ResultSet</code> object returned by the method
   * <code>getFunctions</code>.
   */
  val functionReturnsTable: Int = 2

  private[ldbc] open class Impl[F[_]: Temporal: Exchange: Tracer](
    protocol:   Protocol[F],
    serverVariables: Map[String, String],
    database:   Option[String] = None,
    databaseTerm: Option[DatabaseTerm] = None,
    getProceduresReturnsFunctions: Boolean = true
  )(using ev: MonadError[F, Throwable]) extends DatabaseMetaData[F]:
    override def allProceduresAreCallable(): Boolean = false

    override def allTablesAreSelectable(): Boolean = false

    override def getURL(): String = protocol.hostInfo.url

    override def getUserName(): F[String] =
      protocol.resetSequenceId *>
        protocol.send(ComQueryPacket("SELECT USER()", protocol.initialPacket.capabilityFlags, ListMap.empty)) *>
        protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
          case _: OKPacket => ev.pure("")
          case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", "SELECT USER()"))
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
            yield resultSetRow.headOption.flatMap(_.values.headOption).flatten.getOrElse("")
        }

    override def isReadOnly(): Boolean = false

    override def nullsAreSortedHigh(): Boolean = false

    override def nullsAreSortedLow(): Boolean = !nullsAreSortedHigh()

    override def nullsAreSortedAtStart(): Boolean = false

    override def nullsAreSortedAtEnd(): Boolean = false

    override def getDatabaseProductName(): String = "MySQL"

    override def getDatabaseProductVersion(): String = protocol.initialPacket.serverVersion.toString

    override def getDriverName(): String = DRIVER_NAME

    override def getDriverVersion(): String = s"ldbc-connector-${DRIVER_VERSION}"

    override def getDriverMajorVersion(): Int = DRIVER_VERSION.major

    override def getDriverMinorVersion(): Int = DRIVER_VERSION.minor

    override def usesLocalFiles(): Boolean = false

    override def usesLocalFilePerTable(): Boolean = false

    override def supportsMixedCaseIdentifiers(): Boolean =
      serverVariables.get("lower_case_table_names") match
        case Some(lowerCaseTables) => !("on".equalsIgnoreCase(lowerCaseTables) || "1".equalsIgnoreCase(lowerCaseTables) || "2".equalsIgnoreCase(lowerCaseTables))
        case None => false

    override def storesUpperCaseIdentifiers(): Boolean = false

    override def storesLowerCaseIdentifiers(): Boolean =
      serverVariables.get("lower_case_table_names") match
        case Some(lowerCaseTables) => "on".equalsIgnoreCase(lowerCaseTables) || "1".equalsIgnoreCase(lowerCaseTables)
        case None => false

    override def storesMixedCaseIdentifiers(): Boolean = !storesLowerCaseIdentifiers()

    override def supportsMixedCaseQuotedIdentifiers(): Boolean = supportsMixedCaseIdentifiers()

    override def storesUpperCaseQuotedIdentifiers(): Boolean = true

    override def storesLowerCaseQuotedIdentifiers(): Boolean = storesLowerCaseIdentifiers()

    override def storesMixedCaseQuotedIdentifiers(): Boolean = !storesLowerCaseIdentifiers()

    override def getIdentifierQuoteString(): String =
      val sqlModeAsString = serverVariables.get("sql_mode")
      sqlModeAsString match
        case Some(sqlMode) =>
          if sqlMode.contains("ANSI_QUOTES") then "\""
          else "`"
        case None => "`"

    override def getSQLKeywords(): F[String] =
      protocol.resetSequenceId *>
        protocol.send(ComQueryPacket("SELECT WORD FROM INFORMATION_SCHEMA.KEYWORDS WHERE RESERVED=1 ORDER BY WORD", protocol.initialPacket.capabilityFlags, ListMap.empty)) *>
        protocol.receive(ColumnsNumberPacket.decoder(protocol.initialPacket.capabilityFlags)).flatMap {
          case _: OKPacket => ev.pure("")
          case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", "SELECT WORD FROM INFORMATION_SCHEMA.KEYWORDS WHERE RESERVED=1 ORDER BY WORD"))
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
            yield resultSetRow.flatMap(_.values.flatten).filterNot(SQL2003_KEYWORDS.contains).mkString(",")
        }

    override def getNumericFunctions(): String = "ABS,ACOS,ASIN,ATAN,ATAN2,BIT_COUNT,CEILING,COS,COT,DEGREES,EXP,FLOOR,LOG,LOG10,MAX,MIN,MOD,PI,POW,POWER,RADIANS,RAND,ROUND,SIN,SQRT,TAN,TRUNCATE"

    override def getStringFunctions(): String = "ASCII,BIN,BIT_LENGTH,CHAR,CHARACTER_LENGTH,CHAR_LENGTH,CONCAT,CONCAT_WS,CONV,ELT,EXPORT_SET,FIELD,FIND_IN_SET,HEX,INSERT,"
      + "INSTR,LCASE,LEFT,LENGTH,LOAD_FILE,LOCATE,LOCATE,LOWER,LPAD,LTRIM,MAKE_SET,MATCH,MID,OCT,OCTET_LENGTH,ORD,POSITION,"
      + "QUOTE,REPEAT,REPLACE,REVERSE,RIGHT,RPAD,RTRIM,SOUNDEX,SPACE,STRCMP,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING,"
      + "SUBSTRING_INDEX,TRIM,UCASE,UPPER"

    override def getSystemFunctions(): String = "DATABASE,USER,SYSTEM_USER,SESSION_USER,PASSWORD,ENCRYPT,LAST_INSERT_ID,VERSION"

    override def getTimeDateFunctions(): String = "DAYOFWEEK,WEEKDAY,DAYOFMONTH,DAYOFYEAR,MONTH,DAYNAME,MONTHNAME,QUARTER,WEEK,YEAR,HOUR,MINUTE,SECOND,PERIOD_ADD,"
      + "PERIOD_DIFF,TO_DAYS,FROM_DAYS,DATE_FORMAT,TIME_FORMAT,CURDATE,CURRENT_DATE,CURTIME,CURRENT_TIME,NOW,SYSDATE,"
      + "CURRENT_TIMESTAMP,UNIX_TIMESTAMP,FROM_UNIXTIME,SEC_TO_TIME,TIME_TO_SEC"

    override def getSearchStringEscape(): String = "\\"

    override def getExtraNameCharacters(): String = "#@"

    override def supportsAlterTableWithAddColumn(): Boolean = true

    override def supportsAlterTableWithDropColumn(): Boolean = true

    override def supportsColumnAliasing(): Boolean = true

    override def nullPlusNonNullIsNull(): Boolean = true

    override def supportsConvert(): Boolean = false

    override def supportsConvert(fromType: Int, toType: Int): Boolean =
      fromType match
        case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY =>
          toType match
            case DECIMAL | NUMERIC | REAL | TINYINT | SMALLINT | INTEGER | BIGINT | FLOAT | DOUBLE | CHAR | VARCHAR | BINARY | VARBINARY | LONGVARBINARY | OTHER | DATE | TIME | TIMESTAMP => true
            case _ => false
        case DECIMAL | NUMERIC | REAL | TINYINT | SMALLINT | INTEGER | BIGINT | FLOAT | DOUBLE =>
          toType match
            case DECIMAL | NUMERIC | REAL | TINYINT | SMALLINT | INTEGER | BIGINT | FLOAT | DOUBLE | CHAR | VARCHAR | BINARY | VARBINARY | LONGVARBINARY => true
            case _ => false
        case OTHER => toType match
          case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY => true
          case _ => false
        case DATE => toType match
          case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY => true
          case _ => false
        case TIME => toType match
          case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY => true
          case _ => false
        case TIMESTAMP => toType match
          case CHAR | VARCHAR | LONGVARCHAR | BINARY | VARBINARY | LONGVARBINARY | TIME | DATE => true
          case _ => false
        case _ => false

    override def supportsTableCorrelationNames(): Boolean = true

    override def supportsDifferentTableCorrelationNames(): Boolean = true

    override def supportsExpressionsInOrderBy(): Boolean = true

    override def supportsOrderByUnrelated(): Boolean = false

    override def supportsGroupBy(): Boolean = true

    override def supportsGroupByUnrelated(): Boolean = true

    override def supportsGroupByBeyondSelect(): Boolean = true

    override def supportsLikeEscapeClause(): Boolean = true

    override def supportsMultipleResultSets(): Boolean = true

    override def supportsMultipleTransactions(): Boolean = true

    override def supportsNonNullableColumns(): Boolean = true

    override def supportsMinimumSQLGrammar(): Boolean = true

    override def supportsCoreSQLGrammar(): Boolean = true

    override def supportsExtendedSQLGrammar(): Boolean = false

    override def supportsANSI92EntryLevelSQL(): Boolean = true

    override def supportsANSI92IntermediateSQL(): Boolean = false

    override def supportsANSI92FullSQL(): Boolean = false

    override def supportsIntegrityEnhancementFacility(): Boolean = false

    override def supportsOuterJoins(): Boolean = true

    override def supportsFullOuterJoins(): Boolean = false

    override def supportsLimitedOuterJoins(): Boolean = true

    override def getSchemaTerm(): String = databaseTerm.fold("") {
      case DatabaseTerm.SCHEMA => "SCHEMA"
      case DatabaseTerm.CATALOG => ""
    }

    override def getProcedureTerm(): String = "PROCEDURE"

    override def getCatalogTerm(): String = databaseTerm.fold("") {
      case DatabaseTerm.SCHEMA => ""
      case DatabaseTerm.CATALOG => "CATALOG"
    }

    override def isCatalogAtStart(): Boolean = true

    override def getCatalogSeparator(): String = "."

    override def supportsSchemasInDataManipulation(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => true
      case DatabaseTerm.CATALOG => false
    }

    override def supportsSchemasInProcedureCalls(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => true
      case DatabaseTerm.CATALOG => false
    }

    override def supportsSchemasInTableDefinitions(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => true
      case DatabaseTerm.CATALOG => false
    }

    override def supportsSchemasInIndexDefinitions(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => true
      case DatabaseTerm.CATALOG => false
    }

    override def supportsSchemasInPrivilegeDefinitions(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => true
      case DatabaseTerm.CATALOG => false
    }

    override def supportsCatalogsInDataManipulation(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => false
      case DatabaseTerm.CATALOG => true
    }

    override def supportsCatalogsInProcedureCalls(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => false
      case DatabaseTerm.CATALOG => true
    }

    override def supportsCatalogsInTableDefinitions(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => false
      case DatabaseTerm.CATALOG => true
    }

    override def supportsCatalogsInIndexDefinitions(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => false
      case DatabaseTerm.CATALOG => true
    }

    override def supportsCatalogsInPrivilegeDefinitions(): Boolean = databaseTerm.fold(false) {
      case DatabaseTerm.SCHEMA => false
      case DatabaseTerm.CATALOG => true
    }

    override def supportsPositionedDelete(): Boolean = false

    override def supportsPositionedUpdate(): Boolean = false

    override def supportsSelectForUpdate(): Boolean = true

    override def supportsStoredProcedures(): Boolean = true

    override def supportsSubqueriesInComparisons(): Boolean = true

    override def supportsSubqueriesInExists(): Boolean = true

    override def supportsSubqueriesInIns(): Boolean = true

    override def supportsSubqueriesInQuantifieds(): Boolean = true

    override def supportsCorrelatedSubqueries(): Boolean = true

    override def supportsUnion(): Boolean = true

    override def supportsUnionAll(): Boolean = true

    override def supportsOpenCursorsAcrossCommit(): Boolean = false

    override def supportsOpenCursorsAcrossRollback(): Boolean = false

    override def supportsOpenStatementsAcrossCommit(): Boolean = false

    override def supportsOpenStatementsAcrossRollback(): Boolean = false

    override def getMaxBinaryLiteralLength(): Int = 16777208

    override def getMaxCharLiteralLength(): Int = 16777208

    override def getMaxColumnNameLength(): Int = 64

    override def getMaxColumnsInGroupBy(): Int = 64

    override def getMaxColumnsInIndex(): Int = 16

    override def getMaxColumnsInOrderBy(): Int = 64

    override def getMaxColumnsInSelect(): Int = 256

    override def getMaxColumnsInTable(): Int = 512

    override def getMaxConnections(): Int = 0

    override def getMaxCursorNameLength(): Int = 64

    override def getMaxIndexLength(): Int = 256

    override def getMaxSchemaNameLength(): Int = 0

    override def getMaxProcedureNameLength(): Int = 0

    override def getMaxCatalogNameLength(): Int = 32

    override def getMaxRowSize(): Int = Int.MaxValue - 8

    override def doesMaxRowSizeIncludeBlobs(): Boolean = true

    override def getMaxStatementLength(): Int = maxBufferSize - 4

    override def getMaxStatements(): Int = 0

    override def getMaxTableNameLength(): Int = 64

    override def getMaxTablesInSelect(): Int = 256

    override def getMaxUserNameLength(): Int = 16

    override def getDefaultTransactionIsolation(): Int = Connection.TRANSACTION_REPEATABLE_READ

    override def supportsTransactions(): Boolean = true

    override def supportsTransactionIsolationLevel(level: Int): Boolean = level match
      case Connection.TRANSACTION_READ_COMMITTED | Connection.TRANSACTION_READ_UNCOMMITTED | Connection.TRANSACTION_REPEATABLE_READ | Connection.TRANSACTION_SERIALIZABLE => true
      case _ => false

    override def supportsDataDefinitionAndDataManipulationTransactions(): Boolean = false

    override def supportsDataManipulationTransactionsOnly(): Boolean = false

    override def dataDefinitionCausesTransactionCommit(): Boolean = true

    override def dataDefinitionIgnoredInTransactions(): Boolean = false

    override def getProcedures(catalog: Option[String], schemaPattern: Option[String], procedureNamePattern: Option[String]): F[ResultSet[F]] =
      getProceduresAndOrFunctions(catalog, schemaPattern, procedureNamePattern, true, getProceduresReturnsFunctions)

    /**
     * Retrieves a description of the given catalog's stored procedure parameter
     * and result columns.
     *
     * <P>Only descriptions matching the schema, procedure and
     * parameter name criteria are returned.  They are ordered by
     * PROCEDURE_CAT, PROCEDURE_SCHEM, PROCEDURE_NAME and SPECIFIC_NAME. Within this, the return value,
     * if any, is first. Next are the parameter descriptions in call
     * order. The column descriptions follow in column number order.
     *
     * <P>Each row in the <code>ResultSet</code> is a parameter description or
     * column description with the following fields:
     * <OL>
     * <LI><B>PROCEDURE_CAT</B> String {@code =>} procedure catalog (may be <code>null</code>)
     * <LI><B>PROCEDURE_SCHEM</B> String {@code =>} procedure schema (may be <code>null</code>)
     * <LI><B>PROCEDURE_NAME</B> String {@code =>} procedure name
     * <LI><B>COLUMN_NAME</B> String {@code =>} column/parameter name
     * <LI><B>COLUMN_TYPE</B> Short {@code =>} kind of column/parameter:
     * <UL>
     * <LI> procedureColumnUnknown - nobody knows
     * <LI> procedureColumnIn - IN parameter
     * <LI> procedureColumnInOut - INOUT parameter
     * <LI> procedureColumnOut - OUT parameter
     * <LI> procedureColumnReturn - procedure return value
     * <LI> procedureColumnResult - result column in <code>ResultSet</code>
     * </UL>
     * <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     * <LI><B>TYPE_NAME</B> String {@code =>} SQL type name, for a UDT type the
     * type name is fully qualified
     * <LI><B>PRECISION</B> int {@code =>} precision
     * <LI><B>LENGTH</B> int {@code =>} length in bytes of data
     * <LI><B>SCALE</B> short {@code =>} scale -  null is returned for data types where
     * SCALE is not applicable.
     * <LI><B>RADIX</B> short {@code =>} radix
     * <LI><B>NULLABLE</B> short {@code =>} can it contain NULL.
     * <UL>
     * <LI> procedureNoNulls - does not allow NULL values
     * <LI> procedureNullable - allows NULL values
     * <LI> procedureNullableUnknown - nullability unknown
     * </UL>
     * <LI><B>REMARKS</B> String {@code =>} comment describing parameter/column
     * <LI><B>COLUMN_DEF</B> String {@code =>} default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be <code>null</code>)
     * <UL>
     * <LI> The string NULL (not enclosed in quotes) - if NULL was specified as the default value
     * <LI> TRUNCATE (not enclosed in quotes)        - if the specified default value cannot be represented without truncation
     * <LI> NULL                                     - if a default value was not specified
     * </UL>
     * <LI><B>SQL_DATA_TYPE</B> int  {@code =>} reserved for future use
     * <LI><B>SQL_DATETIME_SUB</B> int  {@code =>} reserved for future use
     * <LI><B>CHAR_OCTET_LENGTH</B> int  {@code =>} the maximum length of binary and character based columns.  For any other datatype the returned value is a
     * NULL
     * <LI><B>ORDINAL_POSITION</B> int  {@code =>} the ordinal position, starting from 1, for the input and output parameters for a procedure. A value of 0
     * is returned if this row describes the procedure's return value.  For result set columns, it is the
     * ordinal position of the column in the result set starting from 1.  If there are
     * multiple result sets, the column ordinal positions are implementation
     * defined.
     * <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
     * <UL>
     * <LI> YES           --- if the column can include NULLs
     * <LI> NO            --- if the column cannot include NULLs
     * <LI> empty string  --- if the nullability for the
     * column is unknown
     * </UL>
     * <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies this procedure within its schema.
     * </OL>
     *
     * <P><B>Note:</B> Some databases may not return the column
     * descriptions for a procedure.
     *
     * <p>The PRECISION column represents the specified column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog              a catalog name; must match the catalog name as it
     *                             is stored in the database; "" retrieves those without a catalog;
     *                             <code>null</code> means that the catalog name should not be used to narrow
     *                             the search
     * @param schemaPattern        a schema name pattern; must match the schema name
     *                             as it is stored in the database; "" retrieves those without a schema;
     *                             <code>null</code> means that the schema name should not be used to narrow
     *                             the search
     * @param procedureNamePattern a procedure name pattern; must match the
     *                             procedure name as it is stored in the database
     * @param columnNamePattern    a column name pattern; must match the column name
     *                             as it is stored in the database
     * @return <code>ResultSet</code> - each row describes a stored procedure parameter or
     *         column
     */
    def getProcedureColumns(catalog: String, schemaPattern: String, procedureNamePattern: String, columnNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the tables available in the given catalog.
     * Only table descriptions matching the catalog, schema, table
     * name and type criteria are returned.  They are ordered by
     * <code>TABLE_TYPE</code>, <code>TABLE_CAT</code>,
     * <code>TABLE_SCHEM</code> and <code>TABLE_NAME</code>.
     * <P>
     * Each table description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} table name
     * <LI><B>TABLE_TYPE</B> String {@code =>} table type.  Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * <LI><B>REMARKS</B> String {@code =>} explanatory comment on the table (may be {@code null})
     * <LI><B>TYPE_CAT</B> String {@code =>} the types catalog (may be <code>null</code>)
     * <LI><B>TYPE_SCHEM</B> String {@code =>} the types schema (may be <code>null</code>)
     * <LI><B>TYPE_NAME</B> String {@code =>} type name (may be <code>null</code>)
     * <LI><B>SELF_REFERENCING_COL_NAME</B> String {@code =>} name of the designated
     * "identifier" column of a typed table (may be <code>null</code>)
     * <LI><B>REF_GENERATION</B> String {@code =>} specifies how values in
     * SELF_REFERENCING_COL_NAME are created. Values are
     * "SYSTEM", "USER", "DERIVED". (may be <code>null</code>)
     * </OL>
     *
     * <P><B>Note:</B> Some databases may not return information for
     * all tables.
     *
     * @param catalog          a catalog name; must match the catalog name as it
     *                         is stored in the database; "" retrieves those without a catalog;
     *                         <code>null</code> means that the catalog name should not be used to narrow
     *                         the search
     * @param schemaPattern    a schema name pattern; must match the schema name
     *                         as it is stored in the database; "" retrieves those without a schema;
     *                         <code>null</code> means that the schema name should not be used to narrow
     *                         the search
     * @param tableNamePattern a table name pattern; must match the
     *                         table name as it is stored in the database
     * @param types            a list of table types, which must be from the list of table types
     *                         returned from {@link # getTableTypes},to include; <code>null</code> returns
     *                         all types
     * @return <code>ResultSet</code> - each row is a table description
     */
    def getTables(catalog: String, schemaPattern: String, tableNamePattern: String, types: Array[String]): ResultSet[F] = ???

    /**
     * Retrieves the schema names available in this database.  The results
     * are ordered by <code>TABLE_CATALOG</code> and
     * <code>TABLE_SCHEM</code>.
     *
     * <P>The schema columns are:
     * <OL>
     * <LI><B>TABLE_SCHEM</B> String {@code =>} schema name
     * <LI><B>TABLE_CATALOG</B> String {@code =>} catalog name (may be <code>null</code>)
     * </OL>
     *
     * @return a <code>ResultSet</code> object in which each row is a
     *         schema description
     */
    def getSchemas(): ResultSet[F] = ???

    /**
     * Retrieves the catalog names available in this database.  The results
     * are ordered by catalog name.
     *
     * <P>The catalog column is:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} catalog name
     * </OL>
     *
     * @return a <code>ResultSet</code> object in which each row has a
     *         single <code>String</code> column that is a catalog name
     */
    def getCatalogs(): ResultSet[F] = ???

    /**
     * Retrieves the table types available in this database.  The results
     * are ordered by table type.
     *
     * <P>The table type is:
     * <OL>
     * <LI><B>TABLE_TYPE</B> String {@code =>} table type.  Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * </OL>
     *
     * @return a <code>ResultSet</code> object in which each row has a
     *         single <code>String</code> column that is a table type
     */
    def getTableTypes(): ResultSet[F] = ???

    /**
     * Retrieves a description of table columns available in
     * the specified catalog.
     *
     * <P>Only column descriptions matching the catalog, schema, table
     * and column name criteria are returned.  They are ordered by
     * <code>TABLE_CAT</code>,<code>TABLE_SCHEM</code>,
     * <code>TABLE_NAME</code>, and <code>ORDINAL_POSITION</code>.
     *
     * <P>Each column description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} table name
     * <LI><B>COLUMN_NAME</B> String {@code =>} column name
     * <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     * <LI><B>TYPE_NAME</B> String {@code =>} Data source dependent type name,
     * for a UDT the type name is fully qualified
     * <LI><B>COLUMN_SIZE</B> int {@code =>} column size.
     * <LI><B>BUFFER_LENGTH</B> is not used.
     * <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     * <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
     * <LI><B>NULLABLE</B> int {@code =>} is NULL allowed.
     * <UL>
     * <LI> columnNoNulls - might not allow <code>NULL</code> values
     * <LI> columnNullable - definitely allows <code>NULL</code> values
     * <LI> columnNullableUnknown - nullability unknown
     * </UL>
     * <LI><B>REMARKS</B> String {@code =>} comment describing column (may be <code>null</code>)
     * <LI><B>COLUMN_DEF</B> String {@code =>} default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be <code>null</code>)
     * <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
     * <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
     * <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
     * maximum number of bytes in the column
     * <LI><B>ORDINAL_POSITION</B> int {@code =>} index of column in table
     * (starting at 1)
     * <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
     * <UL>
     * <LI> YES           --- if the column can include NULLs
     * <LI> NO            --- if the column cannot include NULLs
     * <LI> empty string  --- if the nullability for the
     * column is unknown
     * </UL>
     * <LI><B>SCOPE_CATALOG</B> String {@code =>} catalog of table that is the scope
     * of a reference attribute (<code>null</code> if DATA_TYPE isn't REF)
     * <LI><B>SCOPE_SCHEMA</B> String {@code =>} schema of table that is the scope
     * of a reference attribute (<code>null</code> if the DATA_TYPE isn't REF)
     * <LI><B>SCOPE_TABLE</B> String {@code =>} table name that this the scope
     * of a reference attribute (<code>null</code> if the DATA_TYPE isn't REF)
     * <LI><B>SOURCE_DATA_TYPE</B> short {@code =>} source type of a distinct type or user-generated
     * Ref type, SQL type from java.sql.Types (<code>null</code> if DATA_TYPE
     * isn't DISTINCT or user-generated REF)
     * <LI><B>IS_AUTOINCREMENT</B> String  {@code =>} Indicates whether this column is auto incremented
     * <UL>
     * <LI> YES           --- if the column is auto incremented
     * <LI> NO            --- if the column is not auto incremented
     * <LI> empty string  --- if it cannot be determined whether the column is auto incremented
     * </UL>
     * <LI><B>IS_GENERATEDCOLUMN</B> String  {@code =>} Indicates whether this is a generated column
     * <UL>
     * <LI> YES           --- if this a generated column
     * <LI> NO            --- if this not a generated column
     * <LI> empty string  --- if it cannot be determined whether this is a generated column
     * </UL>
     * </OL>
     *
     * <p>The COLUMN_SIZE column specifies the column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog           a catalog name; must match the catalog name as it
     *                          is stored in the database; "" retrieves those without a catalog;
     *                          <code>null</code> means that the catalog name should not be used to narrow
     *                          the search
     * @param schemaPattern     a schema name pattern; must match the schema name
     *                          as it is stored in the database; "" retrieves those without a schema;
     *                          <code>null</code> means that the schema name should not be used to narrow
     *                          the search
     * @param tableNamePattern  a table name pattern; must match the
     *                          table name as it is stored in the database
     * @param columnNamePattern a column name pattern; must match the column
     *                          name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a column description
     */
    def getColumns(catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the access rights for a table's columns.
     *
     * <P>Only privileges matching the column name criteria are
     * returned.  They are ordered by COLUMN_NAME and PRIVILEGE.
     *
     * <P>Each privilege description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} table name
     * <LI><B>COLUMN_NAME</B> String {@code =>} column name
     * <LI><B>GRANTOR</B> String {@code =>} grantor of access (may be <code>null</code>)
     * <LI><B>GRANTEE</B> String {@code =>} grantee of access
     * <LI><B>PRIVILEGE</B> String {@code =>} name of access (SELECT,
     * INSERT, UPDATE, REFERENCES, ...)
     * <LI><B>IS_GRANTABLE</B> String {@code =>} "YES" if grantee is permitted
     * to grant to others; "NO" if not; <code>null</code> if unknown
     * </OL>
     *
     * @param catalog           a catalog name; must match the catalog name as it
     *                          is stored in the database; "" retrieves those without a catalog;
     *                          <code>null</code> means that the catalog name should not be used to narrow
     *                          the search
     * @param schema            a schema name; must match the schema name as it is
     *                          stored in the database; "" retrieves those without a schema;
     *                          <code>null</code> means that the schema name should not be used to narrow
     *                          the search
     * @param table             a table name; must match the table name as it is
     *                          stored in the database
     * @param columnNamePattern a column name pattern; must match the column
     *                          name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a column privilege description
     */
    def getColumnPrivileges(catalog: String, schema: String, table: String, columnNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the access rights for each table available
     * in a catalog. Note that a table privilege applies to one or
     * more columns in the table. It would be wrong to assume that
     * this privilege applies to all columns (this may be true for
     * some systems but is not true for all.)
     *
     * <P>Only privileges matching the schema and table name
     * criteria are returned.  They are ordered by
     * <code>TABLE_CAT</code>,
     * <code>TABLE_SCHEM</code>, <code>TABLE_NAME</code>,
     * and <code>PRIVILEGE</code>.
     *
     * <P>Each privilege description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} table name
     * <LI><B>GRANTOR</B> String {@code =>} grantor of access (may be <code>null</code>)
     * <LI><B>GRANTEE</B> String {@code =>} grantee of access
     * <LI><B>PRIVILEGE</B> String {@code =>} name of access (SELECT,
     * INSERT, UPDATE, REFERENCES, ...)
     * <LI><B>IS_GRANTABLE</B> String {@code =>} "YES" if grantee is permitted
     * to grant to others; "NO" if not; <code>null</code> if unknown
     * </OL>
     *
     * @param catalog          a catalog name; must match the catalog name as it
     *                         is stored in the database; "" retrieves those without a catalog;
     *                         <code>null</code> means that the catalog name should not be used to narrow
     *                         the search
     * @param schemaPattern    a schema name pattern; must match the schema name
     *                         as it is stored in the database; "" retrieves those without a schema;
     *                         <code>null</code> means that the schema name should not be used to narrow
     *                         the search
     * @param tableNamePattern a table name pattern; must match the
     *                         table name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a table privilege description
     */
    def getTablePrivileges(catalog: String, schemaPattern: String, tableNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of a table's optimal set of columns that
     * uniquely identifies a row. They are ordered by SCOPE.
     *
     * <P>Each column description has the following columns:
     * <OL>
     * <LI><B>SCOPE</B> short {@code =>} actual scope of result
     * <UL>
     * <LI> bestRowTemporary - very temporary, while using row
     * <LI> bestRowTransaction - valid for remainder of current transaction
     * <LI> bestRowSession - valid for remainder of current session
     * </UL>
     * <LI><B>COLUMN_NAME</B> String {@code =>} column name
     * <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from java.sql.Types
     * <LI><B>TYPE_NAME</B> String {@code =>} Data source dependent type name,
     * for a UDT the type name is fully qualified
     * <LI><B>COLUMN_SIZE</B> int {@code =>} precision
     * <LI><B>BUFFER_LENGTH</B> int {@code =>} not used
     * <LI><B>DECIMAL_DIGITS</B> short  {@code =>} scale - Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     * <LI><B>PSEUDO_COLUMN</B> short {@code =>} is this a pseudo column
     * like an Oracle ROWID
     * <UL>
     * <LI> bestRowUnknown - may or may not be pseudo column
     * <LI> bestRowNotPseudo - is NOT a pseudo column
     * <LI> bestRowPseudo - is a pseudo column
     * </UL>
     * </OL>
     *
     * <p>The COLUMN_SIZE column represents the specified column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog  a catalog name; must match the catalog name as it
     *                 is stored in the database; "" retrieves those without a catalog;
     *                 <code>null</code> means that the catalog name should not be used to narrow
     *                 the search
     * @param schema   a schema name; must match the schema name
     *                 as it is stored in the database; "" retrieves those without a schema;
     *                 <code>null</code> means that the schema name should not be used to narrow
     *                 the search
     * @param table    a table name; must match the table name as it is stored
     *                 in the database
     * @param scope    the scope of interest; use same values as SCOPE
     * @param nullable include columns that are nullable.
     * @return <code>ResultSet</code> - each row is a column description
     */
    def getBestRowIdentifier(catalog: String, schema: String, table: String, scope: Int, nullable: Boolean): ResultSet[F] = ???

    /**
     * Retrieves a description of a table's columns that are automatically
     * updated when any value in a row is updated.  They are
     * unordered.
     *
     * <P>Each column description has the following columns:
     * <OL>
     * <LI><B>SCOPE</B> short {@code =>} is not used
     * <LI><B>COLUMN_NAME</B> String {@code =>} column name
     * <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from <code>java.sql.Types</code>
     * <LI><B>TYPE_NAME</B> String {@code =>} Data source-dependent type name
     * <LI><B>COLUMN_SIZE</B> int {@code =>} precision
     * <LI><B>BUFFER_LENGTH</B> int {@code =>} length of column value in bytes
     * <LI><B>DECIMAL_DIGITS</B> short  {@code =>} scale - Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     * <LI><B>PSEUDO_COLUMN</B> short {@code =>} whether this is pseudo column
     * like an Oracle ROWID
     * <UL>
     * <LI> versionColumnUnknown - may or may not be pseudo column
     * <LI> versionColumnNotPseudo - is NOT a pseudo column
     * <LI> versionColumnPseudo - is a pseudo column
     * </UL>
     * </OL>
     *
     * <p>The COLUMN_SIZE column represents the specified column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog a catalog name; must match the catalog name as it
     *                is stored in the database; "" retrieves those without a catalog;
     *                <code>null</code> means that the catalog name should not be used to narrow
     *                the search
     * @param schema  a schema name; must match the schema name
     *                as it is stored in the database; "" retrieves those without a schema;
     *                <code>null</code> means that the schema name should not be used to narrow
     *                the search
     * @param table   a table name; must match the table name as it is stored
     *                in the database
     * @return a <code>ResultSet</code> object in which each row is a
     *         column description
     */
    def getVersionColumns(catalog: String, schema: String, table: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the given table's primary key columns.  They
     * are ordered by COLUMN_NAME.
     *
     * <P>Each primary key column description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} table name
     * <LI><B>COLUMN_NAME</B> String {@code =>} column name
     * <LI><B>KEY_SEQ</B> short {@code =>} sequence number within primary key( a value
     * of 1 represents the first column of the primary key, a value of 2 would
     * represent the second column within the primary key).
     * <LI><B>PK_NAME</B> String {@code =>} primary key name (may be <code>null</code>)
     * </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *                is stored in the database; "" retrieves those without a catalog;
     *                <code>null</code> means that the catalog name should not be used to narrow
     *                the search
     * @param schema  a schema name; must match the schema name
     *                as it is stored in the database; "" retrieves those without a schema;
     *                <code>null</code> means that the schema name should not be used to narrow
     *                the search
     * @param table   a table name; must match the table name as it is stored
     *                in the database
     * @return <code>ResultSet</code> - each row is a primary key column description
     */
    def getPrimaryKeys(catalog: String, schema: String, table: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the primary key columns that are
     * referenced by the given table's foreign key columns (the primary keys
     * imported by a table).  They are ordered by PKTABLE_CAT,
     * PKTABLE_SCHEM, PKTABLE_NAME, and KEY_SEQ.
     *
     * <P>Each primary key column description has the following columns:
     * <OL>
     * <LI><B>PKTABLE_CAT</B> String {@code =>} primary key table catalog
     * being imported (may be <code>null</code>)
     * <LI><B>PKTABLE_SCHEM</B> String {@code =>} primary key table schema
     * being imported (may be <code>null</code>)
     * <LI><B>PKTABLE_NAME</B> String {@code =>} primary key table name
     * being imported
     * <LI><B>PKCOLUMN_NAME</B> String {@code =>} primary key column name
     * being imported
     * <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be <code>null</code>)
     * <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be <code>null</code>)
     * <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
     * <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
     * <LI><B>KEY_SEQ</B> short {@code =>} sequence number within a foreign key( a value
     * of 1 represents the first column of the foreign key, a value of 2 would
     * represent the second column within the foreign key).
     * <LI><B>UPDATE_RULE</B> short {@code =>} What happens to a
     * foreign key when the primary key is updated:
     * <UL>
     * <LI> importedNoAction - do not allow update of primary
     * key if it has been imported
     * <LI> importedKeyCascade - change imported key to agree
     * with primary key update
     * <LI> importedKeySetNull - change imported key to <code>NULL</code>
     * if its primary key has been updated
     * <LI> importedKeySetDefault - change imported key to default values
     * if its primary key has been updated
     * <LI> importedKeyRestrict - same as importedKeyNoAction
     * (for ODBC 2.x compatibility)
     * </UL>
     * <LI><B>DELETE_RULE</B> short {@code =>} What happens to
     * the foreign key when primary is deleted.
     * <UL>
     * <LI> importedKeyNoAction - do not allow delete of primary
     * key if it has been imported
     * <LI> importedKeyCascade - delete rows that import a deleted key
     * <LI> importedKeySetNull - change imported key to NULL if
     * its primary key has been deleted
     * <LI> importedKeyRestrict - same as importedKeyNoAction
     * (for ODBC 2.x compatibility)
     * <LI> importedKeySetDefault - change imported key to default if
     * its primary key has been deleted
     * </UL>
     * <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be <code>null</code>)
     * <LI><B>PK_NAME</B> String {@code =>} primary key name (may be <code>null</code>)
     * <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
     * constraints be deferred until commit
     * <UL>
     * <LI> importedKeyInitiallyDeferred - see SQL92 for definition
     * <LI> importedKeyInitiallyImmediate - see SQL92 for definition
     * <LI> importedKeyNotDeferrable - see SQL92 for definition
     * </UL>
     * </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *                is stored in the database; "" retrieves those without a catalog;
     *                <code>null</code> means that the catalog name should not be used to narrow
     *                the search
     * @param schema  a schema name; must match the schema name
     *                as it is stored in the database; "" retrieves those without a schema;
     *                <code>null</code> means that the schema name should not be used to narrow
     *                the search
     * @param table   a table name; must match the table name as it is stored
     *                in the database
     * @return <code>ResultSet</code> - each row is a primary key column description
     */
    def getImportedKeys(catalog: String, schema: String, table: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the foreign key columns that reference the
     * given table's primary key columns (the foreign keys exported by a
     * table).  They are ordered by FKTABLE_CAT, FKTABLE_SCHEM,
     * FKTABLE_NAME, and KEY_SEQ.
     *
     * <P>Each foreign key column description has the following columns:
     * <OL>
     * <LI><B>PKTABLE_CAT</B> String {@code =>} primary key table catalog (may be <code>null</code>)
     * <LI><B>PKTABLE_SCHEM</B> String {@code =>} primary key table schema (may be <code>null</code>)
     * <LI><B>PKTABLE_NAME</B> String {@code =>} primary key table name
     * <LI><B>PKCOLUMN_NAME</B> String {@code =>} primary key column name
     * <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be <code>null</code>)
     * being exported (may be <code>null</code>)
     * <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be <code>null</code>)
     * being exported (may be <code>null</code>)
     * <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
     * being exported
     * <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
     * being exported
     * <LI><B>KEY_SEQ</B> short {@code =>} sequence number within foreign key( a value
     * of 1 represents the first column of the foreign key, a value of 2 would
     * represent the second column within the foreign key).
     * <LI><B>UPDATE_RULE</B> short {@code =>} What happens to
     * foreign key when primary is updated:
     * <UL>
     * <LI> importedNoAction - do not allow update of primary
     * key if it has been imported
     * <LI> importedKeyCascade - change imported key to agree
     * with primary key update
     * <LI> importedKeySetNull - change imported key to <code>NULL</code> if
     * its primary key has been updated
     * <LI> importedKeySetDefault - change imported key to default values
     * if its primary key has been updated
     * <LI> importedKeyRestrict - same as importedKeyNoAction
     * (for ODBC 2.x compatibility)
     * </UL>
     * <LI><B>DELETE_RULE</B> short {@code =>} What happens to
     * the foreign key when primary is deleted.
     * <UL>
     * <LI> importedKeyNoAction - do not allow delete of primary
     * key if it has been imported
     * <LI> importedKeyCascade - delete rows that import a deleted key
     * <LI> importedKeySetNull - change imported key to <code>NULL</code> if
     * its primary key has been deleted
     * <LI> importedKeyRestrict - same as importedKeyNoAction
     * (for ODBC 2.x compatibility)
     * <LI> importedKeySetDefault - change imported key to default if
     * its primary key has been deleted
     * </UL>
     * <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be <code>null</code>)
     * <LI><B>PK_NAME</B> String {@code =>} primary key name (may be <code>null</code>)
     * <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
     * constraints be deferred until commit
     * <UL>
     * <LI> importedKeyInitiallyDeferred - see SQL92 for definition
     * <LI> importedKeyInitiallyImmediate - see SQL92 for definition
     * <LI> importedKeyNotDeferrable - see SQL92 for definition
     * </UL>
     * </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *                is stored in this database; "" retrieves those without a catalog;
     *                <code>null</code> means that the catalog name should not be used to narrow
     *                the search
     * @param schema  a schema name; must match the schema name
     *                as it is stored in the database; "" retrieves those without a schema;
     *                <code>null</code> means that the schema name should not be used to narrow
     *                the search
     * @param table   a table name; must match the table name as it is stored
     *                in this database
     * @return a <code>ResultSet</code> object in which each row is a
     *         foreign key column description
     */
    def getExportedKeys(catalog: String, schema: String, table: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the foreign key columns in the given foreign key
     * table that reference the primary key or the columns representing a unique constraint of the  parent table (could be the same or a different table).
     * The number of columns returned from the parent table must match the number of
     * columns that make up the foreign key.  They
     * are ordered by FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, and
     * KEY_SEQ.
     *
     * <P>Each foreign key column description has the following columns:
     * <OL>
     * <LI><B>PKTABLE_CAT</B> String {@code =>} parent key table catalog (may be <code>null</code>)
     * <LI><B>PKTABLE_SCHEM</B> String {@code =>} parent key table schema (may be <code>null</code>)
     * <LI><B>PKTABLE_NAME</B> String {@code =>} parent key table name
     * <LI><B>PKCOLUMN_NAME</B> String {@code =>} parent key column name
     * <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be <code>null</code>)
     * being exported (may be <code>null</code>)
     * <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be <code>null</code>)
     * being exported (may be <code>null</code>)
     * <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
     * being exported
     * <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
     * being exported
     * <LI><B>KEY_SEQ</B> short {@code =>} sequence number within foreign key( a value
     * of 1 represents the first column of the foreign key, a value of 2 would
     * represent the second column within the foreign key).
     * <LI><B>UPDATE_RULE</B> short {@code =>} What happens to
     * foreign key when parent key is updated:
     * <UL>
     * <LI> importedNoAction - do not allow update of parent
     * key if it has been imported
     * <LI> importedKeyCascade - change imported key to agree
     * with parent key update
     * <LI> importedKeySetNull - change imported key to <code>NULL</code> if
     * its parent key has been updated
     * <LI> importedKeySetDefault - change imported key to default values
     * if its parent key has been updated
     * <LI> importedKeyRestrict - same as importedKeyNoAction
     * (for ODBC 2.x compatibility)
     * </UL>
     * <LI><B>DELETE_RULE</B> short {@code =>} What happens to
     * the foreign key when parent key is deleted.
     * <UL>
     * <LI> importedKeyNoAction - do not allow delete of parent
     * key if it has been imported
     * <LI> importedKeyCascade - delete rows that import a deleted key
     * <LI> importedKeySetNull - change imported key to <code>NULL</code> if
     * its primary key has been deleted
     * <LI> importedKeyRestrict - same as importedKeyNoAction
     * (for ODBC 2.x compatibility)
     * <LI> importedKeySetDefault - change imported key to default if
     * its parent key has been deleted
     * </UL>
     * <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be <code>null</code>)
     * <LI><B>PK_NAME</B> String {@code =>} parent key name (may be <code>null</code>)
     * <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
     * constraints be deferred until commit
     * <UL>
     * <LI> importedKeyInitiallyDeferred - see SQL92 for definition
     * <LI> importedKeyInitiallyImmediate - see SQL92 for definition
     * <LI> importedKeyNotDeferrable - see SQL92 for definition
     * </UL>
     * </OL>
     *
     * @param parentCatalog  a catalog name; must match the catalog name
     *                       as it is stored in the database; "" retrieves those without a
     *                       catalog; <code>null</code> means drop catalog name from the selection criteria
     * @param parentSchema   a schema name; must match the schema name as
     *                       it is stored in the database; "" retrieves those without a schema;
     *                       <code>null</code> means drop schema name from the selection criteria
     * @param parentTable    the name of the table that exports the key; must match
     *                       the table name as it is stored in the database
     * @param foreignCatalog a catalog name; must match the catalog name as
     *                       it is stored in the database; "" retrieves those without a
     *                       catalog; <code>null</code> means drop catalog name from the selection criteria
     * @param foreignSchema  a schema name; must match the schema name as it
     *                       is stored in the database; "" retrieves those without a schema;
     *                       <code>null</code> means drop schema name from the selection criteria
     * @param foreignTable   the name of the table that imports the key; must match
     *                       the table name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a foreign key column description
     */
    def getCrossReference(parentCatalog: String, parentSchema: String, parentTable: String, foreignCatalog: String, foreignSchema: String, foreignTable: String): ResultSet[F] = ???

    /**
     * Retrieves a description of all the data types supported by
     * this database. They are ordered by DATA_TYPE and then by how
     * closely the data type maps to the corresponding JDBC SQL type.
     *
     * <P>If the database supports SQL distinct types, then getTypeInfo() will return
     * a single row with a TYPE_NAME of DISTINCT and a DATA_TYPE of Types.DISTINCT.
     * If the database supports SQL structured types, then getTypeInfo() will return
     * a single row with a TYPE_NAME of STRUCT and a DATA_TYPE of Types.STRUCT.
     *
     * <P>If SQL distinct or structured types are supported, then information on the
     * individual types may be obtained from the getUDTs() method.
     *
     *
     * <P>Each type description has the following columns:
     * <OL>
     * <LI><B>TYPE_NAME</B> String {@code =>} Type name
     * <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from java.sql.Types
     * <LI><B>PRECISION</B> int {@code =>} maximum precision
     * <LI><B>LITERAL_PREFIX</B> String {@code =>} prefix used to quote a literal
     * (may be <code>null</code>)
     * <LI><B>LITERAL_SUFFIX</B> String {@code =>} suffix used to quote a literal
     * (may be <code>null</code>)
     * <LI><B>CREATE_PARAMS</B> String {@code =>} parameters used in creating
     * the type (may be <code>null</code>)
     * <LI><B>NULLABLE</B> short {@code =>} can you use NULL for this type.
     * <UL>
     * <LI> typeNoNulls - does not allow NULL values
     * <LI> typeNullable - allows NULL values
     * <LI> typeNullableUnknown - nullability unknown
     * </UL>
     * <LI><B>CASE_SENSITIVE</B> boolean{@code =>} is it case sensitive.
     * <LI><B>SEARCHABLE</B> short {@code =>} can you use "WHERE" based on this type:
     * <UL>
     * <LI> typePredNone - No support
     * <LI> typePredChar - Only supported with WHERE .. LIKE
     * <LI> typePredBasic - Supported except for WHERE .. LIKE
     * <LI> typeSearchable - Supported for all WHERE ..
     * </UL>
     * <LI><B>UNSIGNED_ATTRIBUTE</B> boolean {@code =>} is it unsigned.
     * <LI><B>FIXED_PREC_SCALE</B> boolean {@code =>} can it be a money value.
     * <LI><B>AUTO_INCREMENT</B> boolean {@code =>} can it be used for an
     * auto-increment value.
     * <LI><B>LOCAL_TYPE_NAME</B> String {@code =>} localized version of type name
     * (may be <code>null</code>)
     * <LI><B>MINIMUM_SCALE</B> short {@code =>} minimum scale supported
     * <LI><B>MAXIMUM_SCALE</B> short {@code =>} maximum scale supported
     * <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
     * <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
     * <LI><B>NUM_PREC_RADIX</B> int {@code =>} usually 2 or 10
     * </OL>
     *
     * <p>The PRECISION column represents the maximum column size that the server supports for the given datatype.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @return a <code>ResultSet</code> object in which each row is an SQL
     *         type description
     */
    def getTypeInfo(): ResultSet[F] = ???

    /**
     * Retrieves a description of the given table's indices and statistics. They are
     * ordered by NON_UNIQUE, TYPE, INDEX_NAME, and ORDINAL_POSITION.
     *
     * <P>Each index column description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} table name
     * <LI><B>NON_UNIQUE</B> boolean {@code =>} Can index values be non-unique.
     * false when TYPE is tableIndexStatistic
     * <LI><B>INDEX_QUALIFIER</B> String {@code =>} index catalog (may be <code>null</code>);
     * <code>null</code> when TYPE is tableIndexStatistic
     * <LI><B>INDEX_NAME</B> String {@code =>} index name; <code>null</code> when TYPE is
     * tableIndexStatistic
     * <LI><B>TYPE</B> short {@code =>} index type:
     * <UL>
     * <LI> tableIndexStatistic - this identifies table statistics that are
     * returned in conjunction with a table's index descriptions
     * <LI> tableIndexClustered - this is a clustered index
     * <LI> tableIndexHashed - this is a hashed index
     * <LI> tableIndexOther - this is some other style of index
     * </UL>
     * <LI><B>ORDINAL_POSITION</B> short {@code =>} column sequence number
     * within index; zero when TYPE is tableIndexStatistic
     * <LI><B>COLUMN_NAME</B> String {@code =>} column name; <code>null</code> when TYPE is
     * tableIndexStatistic
     * <LI><B>ASC_OR_DESC</B> String {@code =>} column sort sequence, "A" {@code =>} ascending,
     * "D" {@code =>} descending, may be <code>null</code> if sort sequence is not supported;
     * <code>null</code> when TYPE is tableIndexStatistic
     * <LI><B>CARDINALITY</B> long {@code =>} When TYPE is tableIndexStatistic, then
     * this is the number of rows in the table; otherwise, it is the
     * number of unique values in the index.
     * <LI><B>PAGES</B> long {@code =>} When TYPE is  tableIndexStatistic then
     * this is the number of pages used for the table, otherwise it
     * is the number of pages used for the current index.
     * <LI><B>FILTER_CONDITION</B> String {@code =>} Filter condition, if any.
     * (may be <code>null</code>)
     * </OL>
     *
     * @param catalog     a catalog name; must match the catalog name as it
     *                    is stored in this database; "" retrieves those without a catalog;
     *                    <code>null</code> means that the catalog name should not be used to narrow
     *                    the search
     * @param schema      a schema name; must match the schema name
     *                    as it is stored in this database; "" retrieves those without a schema;
     *                    <code>null</code> means that the schema name should not be used to narrow
     *                    the search
     * @param table       a table name; must match the table name as it is stored
     *                    in this database
     * @param unique      when true, return only indices for unique values;
     *                    when false, return indices regardless of whether unique or not
     * @param approximate when true, result is allowed to reflect approximate
     *                    or out of data values; when false, results are requested to be
     *                    accurate
     * @return <code>ResultSet</code> - each row is an index column description
     */
    def getIndexInfo(catalog: String, schema: String, table: String, unique: Boolean, approximate: Boolean): ResultSet[F] = ???

    /**
     * Retrieves whether this database supports the given result set type.
     *
     * @param `type` defined in <code>java.sql.ResultSet</code>
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    def supportsResultSetType(`type`: Int): Boolean = ???

    /**
     * Retrieves whether this database supports the given concurrency type
     * in combination with the given result set type.
     *
     * @param `type`      defined in <code>java.sql.ResultSet</code>
     * @param concurrency type defined in <code>java.sql.ResultSet</code>
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    def supportsResultSetConcurrency(`type`: Int, concurrency: Int): Boolean = ???

    /**
     * Retrieves whether for the given type of <code>ResultSet</code> object,
     * the result set's own updates are visible.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     */
    def ownUpdatesAreVisible(`type`: Int): Boolean = ???

    /**
     * Retrieves whether a result set's own deletes are visible.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if deletes are visible for the given result set type;
     *         <code>false</code> otherwise
     */
    def ownDeletesAreVisible(`type`: Int): Boolean = ???

    /**
     * Retrieves whether a result set's own inserts are visible.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if inserts are visible for the given result set type;
     *         <code>false</code> otherwise
     */
    def ownInsertsAreVisible(`type`: Int): Boolean = ???

    /**
     * Retrieves whether updates made by others are visible.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if updates made by others
     *         are visible for the given result set type;
     *         <code>false</code> otherwise
     */
    def othersUpdatesAreVisible(`type`: Int): Boolean = ???

    /**
     * Retrieves whether deletes made by others are visible.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if deletes made by others
     *         are visible for the given result set type;
     *         <code>false</code> otherwise
     */
    def othersDeletesAreVisible(`type`: Int): Boolean = ???

    /**
     * Retrieves whether inserts made by others are visible.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if inserts made by others
     *         are visible for the given result set type;
     *         <code>false</code> otherwise
     */
    def othersInsertsAreVisible(`type`: Int): Boolean = ???

    /**
     * Retrieves whether or not a visible row update can be detected by
     * calling the method <code>ResultSet.rowUpdated</code>.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if changes are detected by the result set type;
     *         <code>false</code> otherwise
     */
    def updatesAreDetected(`type`: Int): Boolean = ???

    /**
     * Retrieves whether or not a visible row delete can be detected by
     * calling the method <code>ResultSet.rowDeleted</code>.  If the method
     * <code>deletesAreDetected</code> returns <code>false</code>, it means that
     * deleted rows are removed from the result set.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if deletes are detected by the given result set type;
     *         <code>false</code> otherwise
     */
    def deletesAreDetected(`type`: Int): Boolean = ???

    /**
     * Retrieves whether or not a visible row insert can be detected
     * by calling the method <code>ResultSet.rowInserted</code>.
     *
     * @param type the <code>ResultSet</code> type; one of
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @return <code>true</code> if changes are detected by the specified result
     *         set type; <code>false</code> otherwise
     */
    def insertsAreDetected(`type`: Int): Boolean = ???

    /**
     * Retrieves whether this database supports batch updates.
     *
     * @return <code>true</code> if this database supports batch updates;
     *         <code>false</code> otherwise
     */
    def supportsBatchUpdates(): Boolean = ???

    /**
     * Retrieves a description of the user-defined types (UDTs) defined
     * in a particular schema.  Schema-specific UDTs may have type
     * <code>JAVA_OBJECT</code>, <code>STRUCT</code>,
     * or <code>DISTINCT</code>.
     *
     * <P>Only types matching the catalog, schema, type name and type
     * criteria are returned.  They are ordered by <code>DATA_TYPE</code>,
     * <code>TYPE_CAT</code>, <code>TYPE_SCHEM</code>  and
     * <code>TYPE_NAME</code>.  The type name parameter may be a fully-qualified
     * name.  In this case, the catalog and schemaPattern parameters are
     * ignored.
     *
     * <P>Each type description has the following columns:
     * <OL>
     * <LI><B>TYPE_CAT</B> String {@code =>} the type's catalog (may be <code>null</code>)
     * <LI><B>TYPE_SCHEM</B> String {@code =>} type's schema (may be <code>null</code>)
     * <LI><B>TYPE_NAME</B> String {@code =>} type name
     * <LI><B>CLASS_NAME</B> String {@code =>} Java class name
     * <LI><B>DATA_TYPE</B> int {@code =>} type value defined in java.sql.Types.
     * One of JAVA_OBJECT, STRUCT, or DISTINCT
     * <LI><B>REMARKS</B> String {@code =>} explanatory comment on the type
     * <LI><B>BASE_TYPE</B> short {@code =>} type code of the source type of a
     * DISTINCT type or the type that implements the user-generated
     * reference type of the SELF_REFERENCING_COLUMN of a structured
     * type as defined in java.sql.Types (<code>null</code> if DATA_TYPE is not
     * DISTINCT or not STRUCT with REFERENCE_GENERATION = USER_DEFINED)
     * </OL>
     *
     * <P><B>Note:</B> If the driver does not support UDTs, an empty
     * result set is returned.
     *
     * @param catalog         a catalog name; must match the catalog name as it
     *                        is stored in the database; "" retrieves those without a catalog;
     *                        <code>null</code> means that the catalog name should not be used to narrow
     *                        the search
     * @param schemaPattern   a schema pattern name; must match the schema name
     *                        as it is stored in the database; "" retrieves those without a schema;
     *                        <code>null</code> means that the schema name should not be used to narrow
     *                        the search
     * @param typeNamePattern a type name pattern; must match the type name
     *                        as it is stored in the database; may be a fully qualified name
     * @param types           a list of user-defined types (JAVA_OBJECT,
     *                        STRUCT, or DISTINCT) to include; <code>null</code> returns all types
     * @return <code>ResultSet</code> object in which each row describes a UDT
     */
    def getUDTs(catalog: String, schemaPattern: String, typeNamePattern: String, types: Array[Int]): ResultSet[F] = ???

    /**
     * Retrieves the connection that produced this metadata object.
     *
     * @return the connection that produced this metadata object
     */
    def getConnection(): Connection[F] = ???

    /**
     * Retrieves whether this database supports savepoints.
     *
     * @return <code>true</code> if savepoints are supported;
     *         <code>false</code> otherwise
     */
    def supportsSavepoints(): Boolean = ???

    /**
     * Retrieves whether this database supports named parameters to callable
     * statements.
     *
     * @return <code>true</code> if named parameters are supported;
     *         <code>false</code> otherwise
     */
    def supportsNamedParameters(): Boolean = ???

    /**
     * Retrieves whether it is possible to have multiple <code>ResultSet</code> objects
     * returned from a <code>CallableStatement</code> object
     * simultaneously.
     *
     * @return <code>true</code> if a <code>CallableStatement</code> object
     *         can return multiple <code>ResultSet</code> objects
     *         simultaneously; <code>false</code> otherwise
     */
    def supportsMultipleOpenResults(): Boolean = ???

    /**
     * Retrieves whether auto-generated keys can be retrieved after
     * a statement has been executed
     *
     * @return <code>true</code> if auto-generated keys can be retrieved
     *         after a statement has executed; <code>false</code> otherwise
     *         <p>If <code>true</code> is returned, the JDBC driver must support the
     *         returning of auto-generated keys for at least SQL INSERT statements
     */
    def supportsGetGeneratedKeys(): Boolean = ???

    /**
     * Retrieves a description of the user-defined type (UDT) hierarchies defined in a
     * particular schema in this database. Only the immediate super type/
     * sub type relationship is modeled.
     * <P>
     * Only supertype information for UDTs matching the catalog,
     * schema, and type name is returned. The type name parameter
     * may be a fully-qualified name. When the UDT name supplied is a
     * fully-qualified name, the catalog and schemaPattern parameters are
     * ignored.
     * <P>
     * If a UDT does not have a direct super type, it is not listed here.
     * A row of the <code>ResultSet</code> object returned by this method
     * describes the designated UDT and a direct supertype. A row has the following
     * columns:
     * <OL>
     * <LI><B>TYPE_CAT</B> String {@code =>} the UDT's catalog (may be <code>null</code>)
     * <LI><B>TYPE_SCHEM</B> String {@code =>} UDT's schema (may be <code>null</code>)
     * <LI><B>TYPE_NAME</B> String {@code =>} type name of the UDT
     * <LI><B>SUPERTYPE_CAT</B> String {@code =>} the direct super type's catalog
     * (may be <code>null</code>)
     * <LI><B>SUPERTYPE_SCHEM</B> String {@code =>} the direct super type's schema
     * (may be <code>null</code>)
     * <LI><B>SUPERTYPE_NAME</B> String {@code =>} the direct super type's name
     * </OL>
     *
     * <P><B>Note:</B> If the driver does not support type hierarchies, an
     * empty result set is returned.
     *
     * @param catalog         a catalog name; "" retrieves those without a catalog;
     *                        <code>null</code> means drop catalog name from the selection criteria
     * @param schemaPattern   a schema name pattern; "" retrieves those
     *                        without a schema
     * @param typeNamePattern a UDT name pattern; may be a fully-qualified
     *                        name
     * @return a <code>ResultSet</code> object in which a row gives information
     *         about the designated UDT
     */
    def getSuperTypes(catalog: String, schemaPattern: String, typeNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the table hierarchies defined in a particular
     * schema in this database.
     *
     * <P>Only supertable information for tables matching the catalog, schema
     * and table name are returned. The table name parameter may be a fully-
     * qualified name, in which case, the catalog and schemaPattern parameters
     * are ignored. If a table does not have a super table, it is not listed here.
     * Supertables have to be defined in the same catalog and schema as the
     * sub tables. Therefore, the type description does not need to include
     * this information for the supertable.
     *
     * <P>Each type description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} the type's catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} type's schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} type name
     * <LI><B>SUPERTABLE_NAME</B> String {@code =>} the direct super type's name
     * </OL>
     *
     * <P><B>Note:</B> If the driver does not support type hierarchies, an
     * empty result set is returned.
     *
     * @param catalog          a catalog name; "" retrieves those without a catalog;
     *                         <code>null</code> means drop catalog name from the selection criteria
     * @param schemaPattern    a schema name pattern; "" retrieves those
     *                         without a schema
     * @param tableNamePattern a table name pattern; may be a fully-qualified
     *                         name
     * @return a <code>ResultSet</code> object in which each row is a type description
     */
    def getSuperTables(catalog: String, schemaPattern: String, tableNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the given attribute of the given type
     * for a user-defined type (UDT) that is available in the given schema
     * and catalog.
     * <P>
     * Descriptions are returned only for attributes of UDTs matching the
     * catalog, schema, type, and attribute name criteria. They are ordered by
     * <code>TYPE_CAT</code>, <code>TYPE_SCHEM</code>,
     * <code>TYPE_NAME</code> and <code>ORDINAL_POSITION</code>. This description
     * does not contain inherited attributes.
     * <P>
     * The <code>ResultSet</code> object that is returned has the following
     * columns:
     * <OL>
     * <LI><B>TYPE_CAT</B> String {@code =>} type catalog (may be <code>null</code>)
     * <LI><B>TYPE_SCHEM</B> String {@code =>} type schema (may be <code>null</code>)
     * <LI><B>TYPE_NAME</B> String {@code =>} type name
     * <LI><B>ATTR_NAME</B> String {@code =>} attribute name
     * <LI><B>DATA_TYPE</B> int {@code =>} attribute type SQL type from java.sql.Types
     * <LI><B>ATTR_TYPE_NAME</B> String {@code =>} Data source dependent type name.
     * For a UDT, the type name is fully qualified. For a REF, the type name is
     * fully qualified and represents the target type of the reference type.
     * <LI><B>ATTR_SIZE</B> int {@code =>} column size.  For char or date
     * types this is the maximum number of characters; for numeric or
     * decimal types this is precision.
     * <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     * <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
     * <LI><B>NULLABLE</B> int {@code =>} whether NULL is allowed
     * <UL>
     * <LI> attributeNoNulls - might not allow NULL values
     * <LI> attributeNullable - definitely allows NULL values
     * <LI> attributeNullableUnknown - nullability unknown
     * </UL>
     * <LI><B>REMARKS</B> String {@code =>} comment describing column (may be <code>null</code>)
     * <LI><B>ATTR_DEF</B> String {@code =>} default value (may be <code>null</code>)
     * <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
     * <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
     * <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
     * maximum number of bytes in the column
     * <LI><B>ORDINAL_POSITION</B> int {@code =>} index of the attribute in the UDT
     * (starting at 1)
     * <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine
     * the nullability for a attribute.
     * <UL>
     * <LI> YES           --- if the attribute can include NULLs
     * <LI> NO            --- if the attribute cannot include NULLs
     * <LI> empty string  --- if the nullability for the
     * attribute is unknown
     * </UL>
     * <LI><B>SCOPE_CATALOG</B> String {@code =>} catalog of table that is the
     * scope of a reference attribute (<code>null</code> if DATA_TYPE isn't REF)
     * <LI><B>SCOPE_SCHEMA</B> String {@code =>} schema of table that is the
     * scope of a reference attribute (<code>null</code> if DATA_TYPE isn't REF)
     * <LI><B>SCOPE_TABLE</B> String {@code =>} table name that is the scope of a
     * reference attribute (<code>null</code> if the DATA_TYPE isn't REF)
     * <LI><B>SOURCE_DATA_TYPE</B> short {@code =>} source type of a distinct type or user-generated
     * Ref type,SQL type from java.sql.Types (<code>null</code> if DATA_TYPE
     * isn't DISTINCT or user-generated REF)
     * </OL>
     *
     * @param catalog              a catalog name; must match the catalog name as it
     *                             is stored in the database; "" retrieves those without a catalog;
     *                             <code>null</code> means that the catalog name should not be used to narrow
     *                             the search
     * @param schemaPattern        a schema name pattern; must match the schema name
     *                             as it is stored in the database; "" retrieves those without a schema;
     *                             <code>null</code> means that the schema name should not be used to narrow
     *                             the search
     * @param typeNamePattern      a type name pattern; must match the
     *                             type name as it is stored in the database
     * @param attributeNamePattern an attribute name pattern; must match the attribute
     *                             name as it is declared in the database
     * @return a <code>ResultSet</code> object in which each row is an
     *         attribute description
     */
    def getAttributes(catalog: String, schemaPattern: String, typeNamePattern: String, attributeNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves whether this database supports the given result set holdability.
     *
     * @param holdability one of the following constants:
     *                    <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *                    <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    def supportsResultSetHoldability(holdability: Int): Boolean = ???

    /**
     * Retrieves this database's default holdability for <code>ResultSet</code>
     * objects.
     *
     * @return the default holdability; either
     *         <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     */
    def getResultSetHoldability(): Int = ???

    /**
     * Retrieves the major version number of the underlying database.
     *
     * @return the underlying database's major version
     */
    def getDatabaseMajorVersion(): Int = ???

    /**
     * Retrieves the minor version number of the underlying database.
     *
     * @return underlying database's minor version
     */
    def getDatabaseMinorVersion(): Int = ???

    /**
     * Retrieves the major JDBC version number for this
     * driver.
     *
     * @return JDBC version major number
     */
    def getJDBCMajorVersion(): Int = ???

    /**
     * Retrieves the minor JDBC version number for this
     * driver.
     *
     * @return JDBC version minor number
     */
    def getJDBCMinorVersion(): Int = ???

    /**
     * Indicates whether the SQLSTATE returned by <code>SQLException.getSQLState</code>
     * is X/Open (now known as Open Group) SQL CLI or SQL:2003.
     *
     * @return the type of SQLSTATE; one of:
     *         sqlStateXOpen or
     *         sqlStateSQL
     */
    def getSQLStateType(): Int = ???

    /**
     * Indicates whether updates made to a LOB are made on a copy or directly
     * to the LOB.
     *
     * @return <code>true</code> if updates are made to a copy of the LOB;
     *         <code>false</code> if updates are made directly to the LOB
     */
    def locatorsUpdateCopy(): Boolean = ???

    /**
     * Retrieves whether this database supports statement pooling.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    def supportsStatementPooling(): Boolean = ???

    /**
     * Indicates whether this data source supports the SQL {@code ROWID} type,
     * and the lifetime for which a {@link RowId} object remains valid.
     *
     * @return the status indicating the lifetime of a {@code RowId}
     */
    def getRowIdLifetime(): RowIdLifetime = ???

    /**
     * Retrieves the schema names available in this database.  The results
     * are ordered by <code>TABLE_CATALOG</code> and
     * <code>TABLE_SCHEM</code>.
     *
     * <P>The schema columns are:
     * <OL>
     * <LI><B>TABLE_SCHEM</B> String {@code =>} schema name
     * <LI><B>TABLE_CATALOG</B> String {@code =>} catalog name (may be <code>null</code>)
     * </OL>
     *
     * @param catalog       a catalog name; must match the catalog name as it is stored
     *                      in the database;"" retrieves those without a catalog; null means catalog
     *                      name should not be used to narrow down the search.
     * @param schemaPattern a schema name; must match the schema name as it is
     *                      stored in the database; null means
     *                      schema name should not be used to narrow down the search.
     * @return a <code>ResultSet</code> object in which each row is a
     *         schema description
     */
    def getSchemas(catalog: String, schemaPattern: String): ResultSet[F] = ???

    /**
     * Retrieves whether this database supports invoking user-defined or vendor functions
     * using the stored procedure escape syntax.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    def supportsStoredFunctionsUsingCallSyntax(): Boolean = ???

    /**
     * Retrieves whether a <code>SQLException</code> while autoCommit is <code>true</code> indicates
     * that all open ResultSets are closed, even ones that are holdable.  When a <code>SQLException</code> occurs while
     * autocommit is <code>true</code>, it is vendor specific whether the JDBC driver responds with a commit operation, a
     * rollback operation, or by doing neither a commit nor a rollback.  A potential result of this difference
     * is in whether or not holdable ResultSets are closed.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    def autoCommitFailureClosesAllResultSets(): Boolean = ???

    /**
     * Retrieves a list of the client info properties
     * that the driver supports.  The result set contains the following columns
     *
     * <ol>
     * <li><b>NAME</b> String{@code =>} The name of the client info property<br>
     * <li><b>MAX_LEN</b> int{@code =>} The maximum length of the value for the property<br>
     * <li><b>DEFAULT_VALUE</b> String{@code =>} The default value of the property<br>
     * <li><b>DESCRIPTION</b> String{@code =>} A description of the property.  This will typically
     * contain information as to where this property is
     * stored in the database.
     * </ol>
     * <p>
     * The <code>ResultSet</code> is sorted by the NAME column
     *
     * @return A <code>ResultSet</code> object; each row is a supported client info
     *         property
     */
    def getClientInfoProperties(): ResultSet[F] = ???

    /**
     * Retrieves a description of the  system and user functions available
     * in the given catalog.
     * <P>
     * Only system and user function descriptions matching the schema and
     * function name criteria are returned.  They are ordered by
     * <code>FUNCTION_CAT</code>, <code>FUNCTION_SCHEM</code>,
     * <code>FUNCTION_NAME</code> and
     * <code>SPECIFIC_ NAME</code>.
     *
     * <P>Each function description has the following columns:
     * <OL>
     * <LI><B>FUNCTION_CAT</B> String {@code =>} function catalog (may be <code>null</code>)
     * <LI><B>FUNCTION_SCHEM</B> String {@code =>} function schema (may be <code>null</code>)
     * <LI><B>FUNCTION_NAME</B> String {@code =>} function name.  This is the name
     * used to invoke the function
     * <LI><B>REMARKS</B> String {@code =>} explanatory comment on the function
     * <LI><B>FUNCTION_TYPE</B> short {@code =>} kind of function:
     * <UL>
     * <LI>functionResultUnknown - Cannot determine if a return value
     * or table will be returned
     * <LI> functionNoTable- Does not return a table
     * <LI> functionReturnsTable - Returns a table
     * </UL>
     * <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies
     * this function within its schema.  This is a user specified, or DBMS
     * generated, name that may be different then the <code>FUNCTION_NAME</code>
     * for example with overload functions
     * </OL>
     * <p>
     * A user may not have permission to execute any of the functions that are
     * returned by <code>getFunctions</code>
     *
     * @param catalog             a catalog name; must match the catalog name as it
     *                            is stored in the database; "" retrieves those without a catalog;
     *                            <code>null</code> means that the catalog name should not be used to narrow
     *                            the search
     * @param schemaPattern       a schema name pattern; must match the schema name
     *                            as it is stored in the database; "" retrieves those without a schema;
     *                            <code>null</code> means that the schema name should not be used to narrow
     *                            the search
     * @param functionNamePattern a function name pattern; must match the
     *                            function name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a function description
     */
    def getFunctions(catalog: String, schemaPattern: String, functionNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the given catalog's system or user
     * function parameters and return type.
     *
     * <P>Only descriptions matching the schema,  function and
     * parameter name criteria are returned. They are ordered by
     * <code>FUNCTION_CAT</code>, <code>FUNCTION_SCHEM</code>,
     * <code>FUNCTION_NAME</code> and
     * <code>SPECIFIC_ NAME</code>. Within this, the return value,
     * if any, is first. Next are the parameter descriptions in call
     * order. The column descriptions follow in column number order.
     *
     * <P>Each row in the <code>ResultSet</code>
     * is a parameter description, column description or
     * return type description with the following fields:
     * <OL>
     * <LI><B>FUNCTION_CAT</B> String {@code =>} function catalog (may be <code>null</code>)
     * <LI><B>FUNCTION_SCHEM</B> String {@code =>} function schema (may be <code>null</code>)
     * <LI><B>FUNCTION_NAME</B> String {@code =>} function name.  This is the name
     * used to invoke the function
     * <LI><B>COLUMN_NAME</B> String {@code =>} column/parameter name
     * <LI><B>COLUMN_TYPE</B> Short {@code =>} kind of column/parameter:
     * <UL>
     * <LI> functionColumnUnknown - nobody knows
     * <LI> functionColumnIn - IN parameter
     * <LI> functionColumnInOut - INOUT parameter
     * <LI> functionColumnOut - OUT parameter
     * <LI> functionColumnReturn - function return value
     * <LI> functionColumnResult - Indicates that the parameter or column
     * is a column in the <code>ResultSet</code>
     * </UL>
     * <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     * <LI><B>TYPE_NAME</B> String {@code =>} SQL type name, for a UDT type the
     * type name is fully qualified
     * <LI><B>PRECISION</B> int {@code =>} precision
     * <LI><B>LENGTH</B> int {@code =>} length in bytes of data
     * <LI><B>SCALE</B> short {@code =>} scale -  null is returned for data types where
     * SCALE is not applicable.
     * <LI><B>RADIX</B> short {@code =>} radix
     * <LI><B>NULLABLE</B> short {@code =>} can it contain NULL.
     * <UL>
     * <LI> functionNoNulls - does not allow NULL values
     * <LI> functionNullable - allows NULL values
     * <LI> functionNullableUnknown - nullability unknown
     * </UL>
     * <LI><B>REMARKS</B> String {@code =>} comment describing column/parameter
     * <LI><B>CHAR_OCTET_LENGTH</B> int  {@code =>} the maximum length of binary
     * and character based parameters or columns.  For any other datatype the returned value
     * is a NULL
     * <LI><B>ORDINAL_POSITION</B> int  {@code =>} the ordinal position, starting
     * from 1, for the input and output parameters. A value of 0
     * is returned if this row describes the function's return value.
     * For result set columns, it is the
     * ordinal position of the column in the result set starting from 1.
     * <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine
     * the nullability for a parameter or column.
     * <UL>
     * <LI> YES           --- if the parameter or column can include NULLs
     * <LI> NO            --- if the parameter or column  cannot include NULLs
     * <LI> empty string  --- if the nullability for the
     * parameter  or column is unknown
     * </UL>
     * <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies
     * this function within its schema.  This is a user specified, or DBMS
     * generated, name that may be different then the <code>FUNCTION_NAME</code>
     * for example with overload functions
     * </OL>
     *
     * <p>The PRECISION column represents the specified column size for the given
     * parameter or column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog             a catalog name; must match the catalog name as it
     *                            is stored in the database; "" retrieves those without a catalog;
     *                            <code>null</code> means that the catalog name should not be used to narrow
     *                            the search
     * @param schemaPattern       a schema name pattern; must match the schema name
     *                            as it is stored in the database; "" retrieves those without a schema;
     *                            <code>null</code> means that the schema name should not be used to narrow
     *                            the search
     * @param functionNamePattern a procedure name pattern; must match the
     *                            function name as it is stored in the database
     * @param columnNamePattern   a parameter name pattern; must match the
     *                            parameter or column name as it is stored in the database
     * @return <code>ResultSet</code> - each row describes a
     *         user function parameter, column  or return type
     */
    def getFunctionColumns(catalog: String, schemaPattern: String, functionNamePattern: String, columnNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves a description of the pseudo or hidden columns available
     * in a given table within the specified catalog and schema.
     * Pseudo or hidden columns may not always be stored within
     * a table and are not visible in a ResultSet unless they are
     * specified in the query's outermost SELECT list. Pseudo or hidden
     * columns may not necessarily be able to be modified. If there are
     * no pseudo or hidden columns, an empty ResultSet is returned.
     *
     * <P>Only column descriptions matching the catalog, schema, table
     * and column name criteria are returned.  They are ordered by
     * <code>TABLE_CAT</code>,<code>TABLE_SCHEM</code>, <code>TABLE_NAME</code>
     * and <code>COLUMN_NAME</code>.
     *
     * <P>Each column description has the following columns:
     * <OL>
     * <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be <code>null</code>)
     * <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be <code>null</code>)
     * <LI><B>TABLE_NAME</B> String {@code =>} table name
     * <LI><B>COLUMN_NAME</B> String {@code =>} column name
     * <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     * <LI><B>COLUMN_SIZE</B> int {@code =>} column size.
     * <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     * <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
     * <LI><B>COLUMN_USAGE</B> String {@code =>} The allowed usage for the column.  The
     * value returned will correspond to the enum name returned by {@link PseudoColumnUsage# name PseudoColumnUsage.name()}
     * <LI><B>REMARKS</B> String {@code =>} comment describing column (may be <code>null</code>)
     * <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
     * maximum number of bytes in the column
     * <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
     * <UL>
     * <LI> YES           --- if the column can include NULLs
     * <LI> NO            --- if the column cannot include NULLs
     * <LI> empty string  --- if the nullability for the column is unknown
     * </UL>
     * </OL>
     *
     * <p>The COLUMN_SIZE column specifies the column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog           a catalog name; must match the catalog name as it
     *                          is stored in the database; "" retrieves those without a catalog;
     *                          <code>null</code> means that the catalog name should not be used to narrow
     *                          the search
     * @param schemaPattern     a schema name pattern; must match the schema name
     *                          as it is stored in the database; "" retrieves those without a schema;
     *                          <code>null</code> means that the schema name should not be used to narrow
     *                          the search
     * @param tableNamePattern  a table name pattern; must match the
     *                          table name as it is stored in the database
     * @param columnNamePattern a column name pattern; must match the column
     *                          name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a column description
     */
    def getPseudoColumns(catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String): ResultSet[F] = ???

    /**
     * Retrieves whether a generated key will always be returned if the column
     * name(s) or index(es) specified for the auto generated key column(s)
     * are valid and the statement succeeds.  The key that is returned may or
     * may not be based on the column(s) for the auto generated key.
     * Consult your JDBC driver documentation for additional details.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    def generatedKeyAlwaysReturned(): Boolean = ???

    protected def getDatabase(catalog: Option[String], schema: Option[String]): Option[String] =
      (databaseTerm, catalog, schema) match
        case (Some(DatabaseTerm.SCHEMA), None, Some(value)) => Some(value)
        case (Some(DatabaseTerm.CATALOG), Some(value), None) => Some(value)
        case _ => database

    /**
     * Get a prepared statement to query information_schema tables.
     *
     * @param sql
     *            query
     * @return PreparedStatement
     */
    protected def prepareMetaDataSafeStatement(sql: String): F[PreparedStatement[F]] =
      for
        params <- Ref[F].of(ListMap.empty[Int, Parameter])
        batchedArgs <- Ref[F].of(Vector.empty[String])
      yield PreparedStatement.Client[F](
        protocol,
        sql,
        params,
        batchedArgs,
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY
      )

    protected def getProceduresAndOrFunctions(
                                               catalog: Option[String],
                                               schemaPattern: Option[String],
                                               procedureNamePattern: Option[String],
                                               returnProcedures: Boolean,
                                               returnFunctions: Boolean
                                             ): F[ResultSet[F]] =

      val db = getDatabase(catalog, schemaPattern)
      val dbMapsToSchema = databaseTerm.contains(DatabaseTerm.SCHEMA)

      val selectFromMySQLProcSQL = new StringBuilder()

      selectFromMySQLProcSQL.append("SELECT db, name, type, comment FROM mysql.proc WHERE")

      if returnProcedures && !returnFunctions then
        selectFromMySQLProcSQL.append(" type = 'PROCEDURE' AND ")
      else if !returnProcedures && returnFunctions then
        selectFromMySQLProcSQL.append(" type = 'FUNCTION' AND ")
      end if

      selectFromMySQLProcSQL.append(if dbMapsToSchema then " db LIKE ?" else " db = ?")

      if procedureNamePattern.nonEmpty then
        selectFromMySQLProcSQL.append(" AND name LIKE ?")
      end if

      selectFromMySQLProcSQL.append(" ORDER BY name, type")

      prepareMetaDataSafeStatement(selectFromMySQLProcSQL.toString()).flatMap { preparedStatement =>
        val setting = (db, procedureNamePattern) match
          case (Some(db), Some(procedureNamePattern)) => preparedStatement.setString(1, db) *> preparedStatement.setString(2, procedureNamePattern)
          case (Some(db), None) => preparedStatement.setString(1, db)
          case _ => ev.unit

        (setting *> preparedStatement.executeQuery() <* preparedStatement.close()).recoverWith {
          case ex: SQLException =>
            (returnProcedures, returnFunctions) match
              case (false, true) =>
                val sql = "SHOW FUNCTION STATUS WHERE "
                    + (if dbMapsToSchema then "Db LIKE ?" else "Db = ?")
                    + (if procedureNamePattern.nonEmpty then " AND Name LIKE ?" else "")

                prepareMetaDataSafeStatement(sql).flatMap { preparedStatement =>
                  preparedStatement.setString(1, db) *>
                    (if procedureNamePattern.nonEmpty then preparedStatement.setString(2, procedureNamePattern) else ev.unit) *>
                    preparedStatement.executeQuery() <* preparedStatement.close()
                }
              case (true, _) =>
                val sql = "SHOW PROCEDURE STATUS WHERE "
                  + (if dbMapsToSchema then "Db LIKE ?" else "Db = ?")
                  + (if procedureNamePattern.nonEmpty then " AND Name LIKE ?" else "")
                prepareMetaDataSafeStatement(sql).flatMap { preparedStatement =>
                  preparedStatement.setString(1, db) *>
                    (if procedureNamePattern.nonEmpty then preparedStatement.setString(2, procedureNamePattern) else ev.unit) *>
                    preparedStatement.executeQuery() <* preparedStatement.close()
                }
              case _ =>
                for
                  isResultSetClosed <- Ref[F].of(false)
                  resultSetCurrentCursor <- Ref[F].of(0)
                  resultSetCurrentRow <- Ref[F].of[Option[ResultSetRowPacket]](None)
                yield ResultSet
                  .empty(
                      protocol.initialPacket.serverVersion,
                      isResultSetClosed,
                      resultSetCurrentCursor,
                      resultSetCurrentRow
                    )
        }
      }

  def apply[F[_]: Temporal: Exchange: Tracer](
                   protocol:   Protocol[F],
                   serverVariables: Map[String, String],
                   database: Option[String] = None,
                   databaseTerm: Option[DatabaseTerm] = None,
                   getProceduresReturnsFunctions: Boolean = true
                 )(using ev: MonadError[F, Throwable]): DatabaseMetaData[F] =
    new Impl[F](protocol, serverVariables, database, databaseTerm, getProceduresReturnsFunctions)
