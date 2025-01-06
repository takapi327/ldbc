/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.DatabaseMetaData

import ldbc.connector.exception.*

import com.comcast.ip4s.UnknownHostException

class ConnectionTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  test("Passing an empty string to host causes SQLException") {
    val connection = Connection[IO](
      host = "",
      port = 13306,
      user = "root"
    )
    interceptIO[SQLClientInfoException] {
      connection.use(_ => IO.unit)
    }
  }

  test("UnknownHostException occurs when invalid host is passed") {
    val connection = Connection[IO](
      host = "host",
      port = 13306,
      user = "root"
    )
    interceptIO[UnknownHostException] {
      connection.use(_ => IO.unit)
    }
  }

  test("Passing a negative value to Port causes SQLException") {
    val connection = Connection[IO](
      host = "127.0.0.1",
      port = -1,
      user = "root"
    )
    interceptIO[SQLClientInfoException] {
      connection.use(_ => IO.unit)
    }
  }

  test("SQLException occurs when passing more than 65535 values to Port") {
    val connection = Connection[IO](
      host = "127.0.0.1",
      port = 65536,
      user = "root"
    )
    interceptIO[SQLClientInfoException] {
      connection.use(_ => IO.unit)
    }
  }

  test("A user using mysql_native_password can establish a connection with the MySQL server.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password")
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "Connections to MySQL servers using users with mysql_native_password will succeed if allowPublicKeyRetrieval is enabled for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_mysql_native_user",
      password                = Some("ldbc_mysql_native_password"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with mysql_native_password will succeed for SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Users using mysql_native_password can establish a connection with the MySQL server by specifying database.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test")
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If allowPublicKeyRetrieval is enabled for non-SSL connections, a connection to a MySQL server specifying a database using a user with mysql_native_password will succeed."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_mysql_native_user",
      password                = Some("ldbc_mysql_native_password"),
      database                = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "A connection to a MySQL server with a database specified using a user with mysql_native_password will succeed with an SSL connection."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with sha256_password will fail for non-SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password")
    )
    interceptIO[SQLInvalidAuthorizationSpecException] {
      connection.use(_ => IO.unit)
    }
  }

  test(
    "Connections to MySQL servers using users with sha256_password will succeed if allowPublicKeyRetrieval is enabled for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_sha256_user",
      password                = Some("ldbc_sha256_password"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with sha256_password will succeed for SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If allowPublicKeyRetrieval is enabled for non-SSL connections, a connection to a MySQL server specifying a database using a user with sha256_password will succeed."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_sha256_user",
      password                = Some("ldbc_sha256_password"),
      database                = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "A connection to a MySQL server with a database specified using a user with sha256_password will succeed with an SSL connection."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "Connections to MySQL servers using users with caching_sha2_password will succeed if allowPublicKeyRetrieval is enabled for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc",
      password                = Some("password"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with caching_sha2_password will succeed for SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If the login information of a user using caching_sha2_password is cached, the connection to the MySQL server will succeed even for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password")
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If allowPublicKeyRetrieval is enabled for non-SSL connections, a connection to a MySQL server specifying a database using a user with caching_sha2_password will succeed."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc",
      password                = Some("password"),
      database                = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "A connection to a MySQL server with a database specified using a user with caching_sha2_password will succeed with an SSL connection."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Catalog change will change the currently connected Catalog.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        conn.setCatalog("world") *> conn.getCatalog()
      },
      "world"
    )
  }

  test("Statistics of the MySQL server can be obtained.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getStatistics.map(_.toString)),
      "COM_STATISTICS Response Packet"
    )
  }

  test("The connection is valid.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.isValid(0)))
  }

  test("Connection state reset succeeds.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.resetServerState) *> IO(true))
  }

  test("If multi-querying is not enabled, ERRPacketException is raised when multi-querying is performed.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    interceptIO[SQLSyntaxErrorException](connection.use { conn =>
      for
        statement <- conn.createStatement()
        resultSet <- statement.executeQuery("SELECT 1; SELECT2")
      yield resultSet
    })
  }

  test("Can change from mysql_native_password user to caching_sha2_password user.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(
      connection.use(_.changeUser("ldbc", "password")) *> IO.pure(true),
      true
    )
  }

  test("Can change from mysql_native_password user to sha256_password user.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(
      connection.use(_.changeUser("ldbc_sha256_user", "ldbc_sha256_password")) *> IO.pure(true),
      true
    )
  }

  test("Can change from sha256_password user to mysql_native_password user.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(
      connection.use(_.changeUser("ldbc_mysql_native_user", "ldbc_mysql_native_password")) *> IO.pure(true),
      true
    )
  }

  test("Can change from sha256_password user to caching_sha2_password user.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(
      connection.use(_.changeUser("ldbc", "password")) *> IO.pure(true),
      true
    )
  }

  test("Can change from caching_sha2_password user to mysql_native_password user.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(
      connection.use(_.changeUser("ldbc_mysql_native_user", "ldbc_mysql_native_password")) *> IO.pure(true),
      true
    )
  }

  test("Can change from caching_sha2_password user to sha256_password user.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(
      connection.use(_.changeUser("ldbc_sha256_user", "ldbc_sha256_password")) *> IO.pure(true),
      true
    )
  }

  test("The allProceduresAreCallable method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.allProceduresAreCallable())))
  }

  test("The allTablesAreSelectable method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.allTablesAreSelectable())))
  }

  test("The URL retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getURL())),
      "jdbc:mysql://127.0.0.1:13306/connector_test"
    )
  }

  test("The User name retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().flatMap(_.getUserName())),
      "ldbc@172.18.0.1"
    )
  }

  test("The isReadOnly method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.isReadOnly())))
  }

  test("The nullsAreSortedHigh method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.nullsAreSortedHigh())))
  }

  test("The nullsAreSortedLow method of DatabaseMetaData is always true.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => meta.nullsAreSortedLow())))
  }

  test("The nullsAreSortedAtStart method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.nullsAreSortedAtStart())))
  }

  test("The nullsAreSortedAtEnd method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.nullsAreSortedAtEnd())))
  }

  test("The getDatabaseProductName method of DatabaseMetaData is always MySQL.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getDatabaseProductName())),
      "MySQL"
    )
  }

  test("The Server version retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getDatabaseProductVersion())),
      "8.4.0"
    )
  }

  test("The getDriverName method of DatabaseMetaData is always MySQL Connector/L.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getDriverName())),
      "MySQL Connector/L"
    )
  }

  test("The Driver version retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getDriverVersion())),
      "ldbc-connector-0.3.0"
    )
  }

  test("The usesLocalFiles method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.usesLocalFiles())))
  }

  test("The usesLocalFilePerTable method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.usesLocalFilePerTable())))
  }

  test("The supports Mixed Case Identifiers retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(_.supportsMixedCaseIdentifiers())))
  }

  test("The storesUpperCaseIdentifiers method of DatabaseMetaData is always false.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.storesUpperCaseIdentifiers())))
  }

  test("The stores Lower Case Identifiers retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.storesLowerCaseIdentifiers())))
  }

  test("The stores Mixed Case Identifiers retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(_.storesMixedCaseIdentifiers())))
  }

  test("The supports Mixed Case Quoted Identifiers retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(_.supportsMixedCaseQuotedIdentifiers())))
  }

  test("The storesUpperCaseQuotedIdentifiers method of DatabaseMetaData is always true.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(_.storesUpperCaseQuotedIdentifiers())))
  }

  test("The stores Lower Case Quoted Identifiers retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(meta => !meta.storesLowerCaseQuotedIdentifiers())))
  }

  test("The stores Mixed Case Quoted Identifiers retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIOBoolean(connection.use(_.getMetaData().map(_.storesMixedCaseQuotedIdentifiers())))
  }

  test("The Identifier Quote String retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getIdentifierQuoteString())),
      "`"
    )
  }

  test("The SQL Keywords retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().flatMap(_.getSQLKeywords())),
      "ACCESSIBLE,ADD,ANALYZE,ASC,BEFORE,CASCADE,CHANGE,CONTINUE,DATABASE,DATABASES,DAY_HOUR,DAY_MICROSECOND,DAY_MINUTE,DAY_SECOND,DELAYED,DESC,DISTINCTROW,DIV,DUAL,ELSEIF,EMPTY,ENCLOSED,ESCAPED,EXIT,EXPLAIN,FIRST_VALUE,FLOAT4,FLOAT8,FORCE,FULLTEXT,GENERATED,GROUPS,HIGH_PRIORITY,HOUR_MICROSECOND,HOUR_MINUTE,HOUR_SECOND,IF,IGNORE,INDEX,INFILE,INT1,INT2,INT3,INT4,INT8,IO_AFTER_GTIDS,IO_BEFORE_GTIDS,ITERATE,JSON_TABLE,KEY,KEYS,KILL,LAG,LAST_VALUE,LEAD,LEAVE,LIMIT,LINEAR,LINES,LOAD,LOCK,LONG,LONGBLOB,LONGTEXT,LOOP,LOW_PRIORITY,MAXVALUE,MEDIUMBLOB,MEDIUMINT,MEDIUMTEXT,MIDDLEINT,MINUTE_MICROSECOND,MINUTE_SECOND,NO_WRITE_TO_BINLOG,NTH_VALUE,NTILE,OPTIMIZE,OPTIMIZER_COSTS,OPTION,OPTIONALLY,OUTFILE,PURGE,READ,READ_WRITE,REGEXP,RENAME,REPEAT,REPLACE,REQUIRE,RESIGNAL,RESTRICT,RLIKE,SCHEMA,SCHEMAS,SECOND_MICROSECOND,SEPARATOR,SHOW,SIGNAL,SPATIAL,SQL_BIG_RESULT,SQL_CALC_FOUND_ROWS,SQL_SMALL_RESULT,SSL,STARTING,STORED,STRAIGHT_JOIN,TERMINATED,TINYBLOB,TINYINT,TINYTEXT,UNDO,UNLOCK,UNSIGNED,USAGE,USE,UTC_DATE,UTC_TIME,UTC_TIMESTAMP,VARBINARY,VARCHARACTER,VIRTUAL,WHILE,WRITE,XOR,YEAR_MONTH,ZEROFILL"
    )
  }

  test("The Numeric Functions retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getNumericFunctions())),
      "ABS,ACOS,ASIN,ATAN,ATAN2,BIT_COUNT,CEILING,COS,COT,DEGREES,EXP,FLOOR,LOG,LOG10,MAX,MIN,MOD,PI,POW,POWER,RADIANS,RAND,ROUND,SIN,SQRT,TAN,TRUNCATE"
    )
  }

  test("The String Functions retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getStringFunctions())),
      "ASCII,BIN,BIT_LENGTH,CHAR,CHARACTER_LENGTH,CHAR_LENGTH,CONCAT,CONCAT_WS,CONV,ELT,EXPORT_SET,FIELD,FIND_IN_SET,HEX,INSERT,"
        + "INSTR,LCASE,LEFT,LENGTH,LOAD_FILE,LOCATE,LOCATE,LOWER,LPAD,LTRIM,MAKE_SET,MATCH,MID,OCT,OCTET_LENGTH,ORD,POSITION,"
        + "QUOTE,REPEAT,REPLACE,REVERSE,RIGHT,RPAD,RTRIM,SOUNDEX,SPACE,STRCMP,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING,"
        + "SUBSTRING_INDEX,TRIM,UCASE,UPPER"
    )
  }

  test("The System Functions retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getSystemFunctions())),
      "DATABASE,USER,SYSTEM_USER,SESSION_USER,PASSWORD,ENCRYPT,LAST_INSERT_ID,VERSION"
    )
  }

  test("The Time Date Functions retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getTimeDateFunctions())),
      "DAYOFWEEK,WEEKDAY,DAYOFMONTH,DAYOFYEAR,MONTH,DAYNAME,MONTHNAME,QUARTER,WEEK,YEAR,HOUR,MINUTE,SECOND,PERIOD_ADD,"
        + "PERIOD_DIFF,TO_DAYS,FROM_DAYS,DATE_FORMAT,TIME_FORMAT,CURDATE,CURRENT_DATE,CURTIME,CURRENT_TIME,NOW,SYSDATE,"
        + "CURRENT_TIMESTAMP,UNIX_TIMESTAMP,FROM_UNIXTIME,SEC_TO_TIME,TIME_TO_SEC"
    )
  }

  test("The Search String Escape retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getSearchStringEscape())),
      "\\"
    )
  }

  test("The Extra Name Characters retrieved from DatabaseMetaData matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getMetaData().map(_.getExtraNameCharacters())),
      "$"
    )
  }

  test("The result of retrieving procedure information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getProcedures(Some("connector_test"), None, Some("demoSp"))
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val procedureCat   = resultSet.getString("PROCEDURE_CAT")
            val procedureSchem = resultSet.getString("PROCEDURE_SCHEM")
            val procedureName  = resultSet.getString("PROCEDURE_NAME")
            val remarks        = resultSet.getString("REMARKS")
            val procedureType  = resultSet.getString("PROCEDURE_TYPE")
            builder += s"Procedure Catalog: $procedureCat, Procedure Schema: $procedureSchem, Procedure Name: $procedureName, Remarks: $remarks, Procedure Type: $procedureType"
          builder.result()
      },
      Vector(
        "Procedure Catalog: connector_test, Procedure Schema: null, Procedure Name: demoSp, Remarks: , Procedure Type: 1"
      )
    )
  }

  test("The result of retrieving procedure columns information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getProcedureColumns(Some("connector_test"), None, Some("demoSp"), None)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val procedureCat   = resultSet.getString("PROCEDURE_CAT")
            val procedureSchem = resultSet.getString("PROCEDURE_SCHEM")
            val procedureName  = resultSet.getString("PROCEDURE_NAME")
            val columnName     = resultSet.getString("COLUMN_NAME")
            val columnType     = resultSet.getString("COLUMN_TYPE")
            builder += s"Procedure Catalog: $procedureCat, Procedure Schema: $procedureSchem, Procedure Name: $procedureName, Column Name: $columnName, Column Type: $columnType"
          builder.result()
      },
      Vector(
        "Procedure Catalog: connector_test, Procedure Schema: null, Procedure Name: demoSp, Column Name: inputParam, Column Type: 1",
        "Procedure Catalog: connector_test, Procedure Schema: null, Procedure Name: demoSp, Column Name: inOutParam, Column Type: 2"
      )
    )
  }

  test("The result of retrieving tables information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getTables(Some("connector_test"), None, Some("all_types"), Array.empty[String])
        yield
          val builder = Vector.newBuilder[String]
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
            builder += s"Table Catalog: $tableCat, Table Schema: $tableSchem, Table Name: $tableName, Table Type: $tableType, Remarks: $remarks, Type Catalog: $typeCat, Type Schema: $typeSchem, Type Name: $typeName, Self Referencing Column Name: $selfReferencingColName, Reference Generation: $refGeneration"
          builder.result()
      },
      Vector(
        "Table Catalog: connector_test, Table Schema: null, Table Name: all_types, Table Type: TABLE, Remarks: , Type Catalog: null, Type Schema: null, Type Name: null, Self Referencing Column Name: null, Reference Generation: null"
      )
    )
  }

  test("The result of retrieving schemas information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getSchemas()
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCatalog = resultSet.getString("TABLE_CATALOG")
            val tableSchem   = resultSet.getString("TABLE_SCHEM")
            builder += s"Table Catalog: $tableCatalog, Table Schema: $tableSchem"
          builder.result()
      },
      Vector(
        "Table Catalog: def, Table Schema: benchmark",
        "Table Catalog: def, Table Schema: connector_test",
        "Table Catalog: def, Table Schema: information_schema",
        "Table Catalog: def, Table Schema: mysql",
        "Table Catalog: def, Table Schema: performance_schema",
        "Table Catalog: def, Table Schema: sys",
        "Table Catalog: def, Table Schema: world",
        "Table Catalog: def, Table Schema: world2",
        "Table Catalog: def, Table Schema: world3"
      )
    )
  }

  test("The result of retrieving catalogs information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getCatalogs()
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do builder += s"Table Catalog: ${ resultSet.getString("TABLE_CAT") }"
          builder.result()
      },
      Vector(
        "Table Catalog: benchmark",
        "Table Catalog: connector_test",
        "Table Catalog: information_schema",
        "Table Catalog: mysql",
        "Table Catalog: performance_schema",
        "Table Catalog: sys",
        "Table Catalog: world",
        "Table Catalog: world2",
        "Table Catalog: world3"
      )
    )
  }

  test("The result of retrieving tableTypes information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData <- conn.getMetaData()
          resultSet = metaData.getTableTypes()
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do builder += s"Table Type: ${ resultSet.getString("TABLE_TYPE") }"
          builder.result()
      },
      Vector(
        "Table Type: LOCAL TEMPORARY",
        "Table Type: SYSTEM TABLE",
        "Table Type: SYSTEM VIEW",
        "Table Type: TABLE",
        "Table Type: VIEW"
      )
    )
  }

  test("The result of retrieving columns information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getColumns(None, None, Some("privileges_table"), None)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat          = resultSet.getString("TABLE_CAT")
            val tableSchem        = resultSet.getString("TABLE_SCHEM")
            val tableName         = resultSet.getString("TABLE_NAME")
            val columnName        = resultSet.getString("COLUMN_NAME")
            val dataType          = resultSet.getInt("DATA_TYPE")
            val typeName          = resultSet.getString("TYPE_NAME")
            val columnSize        = resultSet.getInt("COLUMN_SIZE")
            val bufferLength      = resultSet.getInt("BUFFER_LENGTH")
            val decimalDigits     = resultSet.getInt("DECIMAL_DIGITS")
            val numPrecRadix      = resultSet.getInt("NUM_PREC_RADIX")
            val nullable          = resultSet.getInt("NULLABLE")
            val remarks           = resultSet.getString("REMARKS")
            val columnDef         = resultSet.getString("COLUMN_DEF")
            val sqlDataType       = resultSet.getInt("SQL_DATA_TYPE")
            val sqlDatetimeSub    = resultSet.getInt("SQL_DATETIME_SUB")
            val charOctetLength   = resultSet.getInt("CHAR_OCTET_LENGTH")
            val ordinalPosition   = resultSet.getInt("ORDINAL_POSITION")
            val isNullable        = resultSet.getString("IS_NULLABLE")
            val scopeCatalog      = resultSet.getString("SCOPE_CATALOG")
            val scopeSchema       = resultSet.getString("SCOPE_SCHEMA")
            val scopeTable        = resultSet.getString("SCOPE_TABLE")
            val sourceDataType    = resultSet.getShort("SOURCE_DATA_TYPE")
            val isAutoincrement   = resultSet.getString("IS_AUTOINCREMENT")
            val isGeneratedcolumn = resultSet.getString("IS_GENERATEDCOLUMN")
            builder += s"Table Cat: $tableCat, Table Schem: $tableSchem, Table Name: $tableName, Column Name: $columnName, Data Type: $dataType, Type Name: $typeName, Column Size: $columnSize, Buffer Length: $bufferLength, Decimal Digits: $decimalDigits, Num Prec Radix: $numPrecRadix, Nullable: $nullable, Remarks: $remarks, Column Def: $columnDef, SQL Data Type: $sqlDataType, SQL Datetime Sub: $sqlDatetimeSub, Char Octet Length: $charOctetLength, Ordinal Position: $ordinalPosition, Is Nullable: $isNullable, Scope Catalog: $scopeCatalog, Scope Schema: $scopeSchema, Scope Table: $scopeTable, Source Data Type: $sourceDataType, Is Autoincrement: $isAutoincrement, Is Generatedcolumn: $isGeneratedcolumn"
          builder.result()
      },
      Vector(
        "Table Cat: def, Table Schem: connector_test, Table Name: privileges_table, Column Name: c1, Data Type: 4, Type Name: INT, Column Size: 10, Buffer Length: 65535, Decimal Digits: 0, Num Prec Radix: 10, Nullable: 0, Remarks: , Column Def: null, SQL Data Type: 0, SQL Datetime Sub: 0, Char Octet Length: 0, Ordinal Position: 1, Is Nullable: NO, Scope Catalog: null, Scope Schema: null, Scope Table: null, Source Data Type: 0, Is Autoincrement: NO, Is Generatedcolumn: NO",
        "Table Cat: def, Table Schem: connector_test, Table Name: privileges_table, Column Name: c2, Data Type: 4, Type Name: INT, Column Size: 10, Buffer Length: 65535, Decimal Digits: 0, Num Prec Radix: 10, Nullable: 0, Remarks: , Column Def: null, SQL Data Type: 0, SQL Datetime Sub: 0, Char Octet Length: 0, Ordinal Position: 2, Is Nullable: NO, Scope Catalog: null, Scope Schema: null, Scope Table: null, Source Data Type: 0, Is Autoincrement: NO, Is Generatedcolumn: NO",
        "Table Cat: def, Table Schem: connector_test, Table Name: privileges_table, Column Name: updated_at, Data Type: 93, Type Name: TIMESTAMP, Column Size: 19, Buffer Length: 65535, Decimal Digits: 0, Num Prec Radix: 10, Nullable: 0, Remarks: , Column Def: CURRENT_TIMESTAMP, SQL Data Type: 0, SQL Datetime Sub: 0, Char Octet Length: 0, Ordinal Position: 3, Is Nullable: NO, Scope Catalog: null, Scope Schema: null, Scope Table: null, Source Data Type: 0, Is Autoincrement: NO, Is Generatedcolumn: YES"
      )
    )
  }

  test("The result of retrieving column privileges information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getColumnPrivileges(None, Some("connector_test"), Some("privileges_table"), None)
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
            builder += s"Table Cat: $tableCat, Table Schem: $tableSchem, Table Name: $tableName, Column Name: $columnName, Grantor: $grantor, Grantee: $grantee, Privilege: $privilege, Is Grantable: $isGrantable"
          builder.result()
      },
      Vector(
        "Table Cat: def, Table Schem: connector_test, Table Name: privileges_table, Column Name: c1, Grantor: null, Grantee: 'ldbc'@'%', Privilege: INSERT, Is Grantable: NO",
        "Table Cat: def, Table Schem: connector_test, Table Name: privileges_table, Column Name: c1, Grantor: null, Grantee: 'ldbc'@'%', Privilege: SELECT, Is Grantable: NO",
        "Table Cat: def, Table Schem: connector_test, Table Name: privileges_table, Column Name: c2, Grantor: null, Grantee: 'ldbc'@'%', Privilege: INSERT, Is Grantable: NO",
        "Table Cat: def, Table Schem: connector_test, Table Name: privileges_table, Column Name: c2, Grantor: null, Grantee: 'ldbc'@'%', Privilege: SELECT, Is Grantable: NO"
      )
    )
  }

  test("The result of retrieving table privileges information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getTablePrivileges(None, None, Some("privileges_table"))
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
            builder += s"Table Cat: $tableCat, Table Schem: $tableSchem, Table Name: $tableName, Grantor: $grantor, Grantee: $grantee, Privilege: $privilege, Is Grantable: $isGrantable"
          builder.result()
      },
      Vector(
        "Table Cat: connector_test, Table Schem: null, Table Name: privileges_table, Grantor: root@localhost, Grantee: ldbc@%, Privilege: SELECT, Is Grantable: null",
        "Table Cat: connector_test, Table Schem: null, Table Name: privileges_table, Grantor: root@localhost, Grantee: ldbc@%, Privilege: SELECT, Is Grantable: null",
        "Table Cat: connector_test, Table Schem: null, Table Name: privileges_table, Grantor: root@localhost, Grantee: ldbc@%, Privilege: SELECT, Is Grantable: null",
        "Table Cat: connector_test, Table Schem: null, Table Name: privileges_table, Grantor: root@localhost, Grantee: ldbc@%, Privilege: INSERT, Is Grantable: null",
        "Table Cat: connector_test, Table Schem: null, Table Name: privileges_table, Grantor: root@localhost, Grantee: ldbc@%, Privilege: INSERT, Is Grantable: null",
        "Table Cat: connector_test, Table Schem: null, Table Name: privileges_table, Grantor: root@localhost, Grantee: ldbc@%, Privilege: INSERT, Is Grantable: null"
      )
    )
  }

  test("The result of retrieving best row identifier information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getBestRowIdentifier(None, Some("connector_test"), "privileges_table", None, None)
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
            builder += s"Scope: $scope, Column Name: $columnName, Data Type: $dataType, Type Name: $typeName, Column Size: $columnSize, Buffer Length: $bufferLength, Decimal Digits: $decimalDigits, Pseudo Column: $pseudoColumn"
          builder.result()
      },
      Vector(
        "Scope: 2, Column Name: c1, Data Type: 4, Type Name: int, Column Size: 10, Buffer Length: 65535, Decimal Digits: 0, Pseudo Column: 1"
      )
    )
  }

  test("The result of retrieving version columns information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getVersionColumns(None, Some("connector_test"), "privileges_table")
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
            builder += s"Scope: $scope, Column Name: $columnName, Data Type: $dataType, Type Name: $typeName, Column Size: $columnSize, Buffer Length: $bufferLength, Decimal Digits: $decimalDigits, Pseudo Column: $pseudoColumn"
          builder.result()
      },
      Vector(
        "Scope: 0, Column Name: updated_at, Data Type: 93, Type Name: TIMESTAMP, Column Size: 19, Buffer Length: 65535, Decimal Digits: 0, Pseudo Column: 1"
      )
    )
  }

  test("The result of retrieving primary key information matches the specified value.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getPrimaryKeys(None, Some("connector_test"), "privileges_table")
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat   = resultSet.getString("TABLE_CAT")
            val tableSchem = resultSet.getString("TABLE_SCHEM")
            val tableName  = resultSet.getString("TABLE_NAME")
            val columnName = resultSet.getString("COLUMN_NAME")
            val keySeq     = resultSet.getShort("KEY_SEQ")
            val pkName     = resultSet.getString("PK_NAME")
            builder += s"Table Cat: $tableCat, Table Schem: $tableSchem, Table Name: $tableName, Column Name: $columnName, Key Seq: $keySeq, PK Name: $pkName"
          builder.result()
      },
      Vector(
        "Table Cat: connector_test, Table Schem: null, Table Name: privileges_table, Column Name: c1, Key Seq: 1, PK Name: PRIMARY"
      )
    )
  }

  test("The result of retrieving imported key information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getImportedKeys(None, Some("world"), "city")
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val pktableCat    = resultSet.getString("PKTABLE_CAT")
            val pktableSchem  = resultSet.getString("PKTABLE_SCHEM")
            val pktableName   = resultSet.getString("PKTABLE_NAME")
            val pkcolumnName  = resultSet.getString("PKCOLUMN_NAME")
            val fktableCat    = resultSet.getString("FKTABLE_CAT")
            val fktableSchem  = resultSet.getString("FKTABLE_SCHEM")
            val fktableName   = resultSet.getString("FKTABLE_NAME")
            val fkcolumnName  = resultSet.getString("FKCOLUMN_NAME")
            val keySeq        = resultSet.getShort("KEY_SEQ")
            val updateRule    = resultSet.getShort("UPDATE_RULE")
            val deleteRule    = resultSet.getShort("DELETE_RULE")
            val fkName        = resultSet.getString("FK_NAME")
            val pkName        = resultSet.getString("PK_NAME")
            val deferrability = resultSet.getShort("DEFERRABILITY")
            builder += s"PK Table Cat: $pktableCat, PK Table Schem: $pktableSchem, PK Table Name: $pktableName, PK Column Name: $pkcolumnName, FK Table Cat: $fktableCat, FK Table Schem: $fktableSchem, FK Table Name: $fktableName, FK Column Name: $fkcolumnName, Key Seq: $keySeq, Update Rule: $updateRule, Delete Rule: $deleteRule, FK Name: $fkName, PK Name: $pkName, Deferrability: $deferrability"
          builder.result()
      },
      Vector(
        "PK Table Cat: def, PK Table Schem: world, PK Table Name: country, PK Column Name: Code, FK Table Cat: def, FK Table Schem: world, FK Table Name: city, FK Column Name: CountryCode, Key Seq: 1, Update Rule: 1, Delete Rule: 1, FK Name: city_ibfk_1, PK Name: PRIMARY, Deferrability: 7"
      )
    )
  }

  test("The result of retrieving exported key information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getExportedKeys(None, Some("world"), "city")
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val pktableCat    = resultSet.getString("PKTABLE_CAT")
            val pktableSchem  = resultSet.getString("PKTABLE_SCHEM")
            val pktableName   = resultSet.getString("PKTABLE_NAME")
            val pkcolumnName  = resultSet.getString("PKCOLUMN_NAME")
            val fktableCat    = resultSet.getString("FKTABLE_CAT")
            val fktableSchem  = resultSet.getString("FKTABLE_SCHEM")
            val fktableName   = resultSet.getString("FKTABLE_NAME")
            val fkcolumnName  = resultSet.getString("FKCOLUMN_NAME")
            val keySeq        = resultSet.getShort("KEY_SEQ")
            val updateRule    = resultSet.getShort("UPDATE_RULE")
            val deleteRule    = resultSet.getShort("DELETE_RULE")
            val fkName        = resultSet.getString("FK_NAME")
            val pkName        = resultSet.getString("PK_NAME")
            val deferrability = resultSet.getShort("DEFERRABILITY")
            builder += s"PK Table Cat: $pktableCat, PK Table Schem: $pktableSchem, PK Table Name: $pktableName, PK Column Name: $pkcolumnName, FK Table Cat: $fktableCat, FK Table Schem: $fktableSchem, FK Table Name: $fktableName, FK Column Name: $fkcolumnName, Key Seq: $keySeq, Update Rule: $updateRule, Delete Rule: $deleteRule, FK Name: $fkName, PK Name: $pkName, Deferrability: $deferrability"
          builder.result()
      },
      Vector(
        "PK Table Cat: def, PK Table Schem: world, PK Table Name: city, PK Column Name: ID, FK Table Cat: def, FK Table Schem: world, FK Table Name: government_office, FK Column Name: CityID, Key Seq: 1, Update Rule: 1, Delete Rule: 1, FK Name: government_office_ibfk_1, PK Name: PRIMARY, Deferrability: 7"
      )
    )
  }

  test("The result of retrieving cross reference information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData <- conn.getMetaData()
          resultSet <-
            metaData.getCrossReference(None, Some("world"), "city", None, Some("world"), Some("government_office"))
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val pktableCat    = resultSet.getString("PKTABLE_CAT")
            val pktableSchem  = resultSet.getString("PKTABLE_SCHEM")
            val pktableName   = resultSet.getString("PKTABLE_NAME")
            val pkcolumnName  = resultSet.getString("PKCOLUMN_NAME")
            val fktableCat    = resultSet.getString("FKTABLE_CAT")
            val fktableSchem  = resultSet.getString("FKTABLE_SCHEM")
            val fktableName   = resultSet.getString("FKTABLE_NAME")
            val fkcolumnName  = resultSet.getString("FKCOLUMN_NAME")
            val keySeq        = resultSet.getShort("KEY_SEQ")
            val updateRule    = resultSet.getShort("UPDATE_RULE")
            val deleteRule    = resultSet.getShort("DELETE_RULE")
            val fkName        = resultSet.getString("FK_NAME")
            val pkName        = resultSet.getString("PK_NAME")
            val deferrability = resultSet.getShort("DEFERRABILITY")
            builder += s"PK Table Cat: $pktableCat, PK Table Schem: $pktableSchem, PK Table Name: $pktableName, PK Column Name: $pkcolumnName, FK Table Cat: $fktableCat, FK Table Schem: $fktableSchem, FK Table Name: $fktableName, FK Column Name: $fkcolumnName, Key Seq: $keySeq, Update Rule: $updateRule, Delete Rule: $deleteRule, FK Name: $fkName, PK Name: $pkName, Deferrability: $deferrability"
          builder.result()
      },
      Vector(
        "PK Table Cat: def, PK Table Schem: world, PK Table Name: city, PK Column Name: ID, FK Table Cat: def, FK Table Schem: world, FK Table Name: government_office, FK Column Name: CityID, Key Seq: 1, Update Rule: 1, Delete Rule: 1, FK Name: government_office_ibfk_1, PK Name: PRIMARY, Deferrability: 7"
      )
    )
  }

  test("The result of retrieving type information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData <- conn.getMetaData()
          resultSet = metaData.getTypeInfo()
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val typeName          = resultSet.getString("TYPE_NAME")
            val dataType          = resultSet.getInt("DATA_TYPE")
            val precision         = resultSet.getInt("PRECISION")
            val literalPrefix     = resultSet.getString("LITERAL_PREFIX")
            val literalSuffix     = resultSet.getString("LITERAL_SUFFIX")
            val createParams      = resultSet.getString("CREATE_PARAMS")
            val nullable          = resultSet.getShort("NULLABLE")
            val caseSensitive     = resultSet.getBoolean("CASE_SENSITIVE")
            val searchable        = resultSet.getShort("SEARCHABLE")
            val unsignedAttribute = resultSet.getBoolean("UNSIGNED_ATTRIBUTE")
            val fixedPrecScale    = resultSet.getBoolean("FIXED_PREC_SCALE")
            val autoIncrement     = resultSet.getBoolean("AUTO_INCREMENT")
            val localTypeName     = resultSet.getString("LOCAL_TYPE_NAME")
            val minimumScale      = resultSet.getShort("MINIMUM_SCALE")
            val maximumScale      = resultSet.getShort("MAXIMUM_SCALE")
            val sqlDataType       = resultSet.getShort("SQL_DATA_TYPE")
            val sqlDatetimeSub    = resultSet.getShort("SQL_DATETIME_SUB")
            val numPrecRadix      = resultSet.getShort("NUM_PREC_RADIX")
            builder += s"Type Name: $typeName, Data Type: $dataType, Precision: $precision, Literal Prefix: $literalPrefix, Literal Suffix: $literalSuffix, Create Params: $createParams, Nullable: $nullable, Case Sensitive: $caseSensitive, Searchable: $searchable, Unsigned Attribute: $unsignedAttribute, Fixed Prec Scale: $fixedPrecScale, Auto Increment: $autoIncrement, Local Type Name: $localTypeName, Minimum Scale: $minimumScale, Maximum Scale: $maximumScale, SQL Data Type: $sqlDataType, SQL Datetime Sub: $sqlDatetimeSub, Num Prec Radix: $numPrecRadix"
          builder.result()
      },
      Vector(
        "Type Name: BIT, Data Type: -7, Precision: 1, Literal Prefix: , Literal Suffix: , Create Params: [(M)], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: BIT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: TINYINT, Data Type: -6, Precision: 3, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: TINYINT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: TINYINT UNSIGNED, Data Type: -6, Precision: 3, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: TINYINT UNSIGNED, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: BIGINT, Data Type: -5, Precision: 19, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: BIGINT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: BIGINT UNSIGNED, Data Type: -5, Precision: 20, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: BIGINT UNSIGNED, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: LONG VARBINARY, Data Type: -4, Precision: 16777215, Literal Prefix: ', Literal Suffix: ', Create Params: , Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: MEDIUMBLOB, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: MEDIUMBLOB, Data Type: -4, Precision: 16777215, Literal Prefix: ', Literal Suffix: ', Create Params: , Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: MEDIUMBLOB, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: LONGBLOB, Data Type: -4, Precision: 2147483647, Literal Prefix: ', Literal Suffix: ', Create Params: , Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: LONGBLOB, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: BLOB, Data Type: -4, Precision: 65535, Literal Prefix: ', Literal Suffix: ', Create Params: [(M)], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: BLOB, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: VECTOR, Data Type: -4, Precision: 65532, Literal Prefix: ', Literal Suffix: ', Create Params: [(M)], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: VECTOR, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: VARBINARY, Data Type: -3, Precision: 65535, Literal Prefix: ', Literal Suffix: ', Create Params: (M), Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: VARBINARY, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: TINYBLOB, Data Type: -3, Precision: 255, Literal Prefix: ', Literal Suffix: ', Create Params: , Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: TINYBLOB, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: BINARY, Data Type: -2, Precision: 255, Literal Prefix: ', Literal Suffix: ', Create Params: (M), Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: BINARY, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: LONG VARCHAR, Data Type: -1, Precision: 16777215, Literal Prefix: ', Literal Suffix: ', Create Params:  [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: MEDIUMTEXT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: MEDIUMTEXT, Data Type: -1, Precision: 16777215, Literal Prefix: ', Literal Suffix: ', Create Params:  [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: MEDIUMTEXT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: LONGTEXT, Data Type: -1, Precision: 2147483647, Literal Prefix: ', Literal Suffix: ', Create Params:  [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: LONGTEXT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: TEXT, Data Type: -1, Precision: 65535, Literal Prefix: ', Literal Suffix: ', Create Params: [(M)] [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: TEXT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: CHAR, Data Type: 1, Precision: 255, Literal Prefix: ', Literal Suffix: ', Create Params: [(M)] [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: CHAR, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: ENUM, Data Type: 1, Precision: 65535, Literal Prefix: ', Literal Suffix: ', Create Params: ('value1','value2',...) [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: ENUM, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: SET, Data Type: 1, Precision: 64, Literal Prefix: ', Literal Suffix: ', Create Params: ('value1','value2',...) [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: SET, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: DECIMAL, Data Type: 3, Precision: 65, Literal Prefix: , Literal Suffix: , Create Params: [(M[,D])] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DECIMAL, Minimum Scale: -308, Maximum Scale: 308, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: NUMERIC, Data Type: 3, Precision: 65, Literal Prefix: , Literal Suffix: , Create Params: [(M[,D])] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DECIMAL, Minimum Scale: -308, Maximum Scale: 308, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: INTEGER, Data Type: 4, Precision: 10, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: INT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: INT, Data Type: 4, Precision: 10, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: INT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: MEDIUMINT, Data Type: 4, Precision: 7, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: MEDIUMINT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: INTEGER UNSIGNED, Data Type: 4, Precision: 10, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: INT UNSIGNED, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: INT UNSIGNED, Data Type: 4, Precision: 10, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: INT UNSIGNED, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: MEDIUMINT UNSIGNED, Data Type: 4, Precision: 8, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: MEDIUMINT UNSIGNED, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: SMALLINT, Data Type: 5, Precision: 5, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: SMALLINT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: SMALLINT UNSIGNED, Data Type: 5, Precision: 5, Literal Prefix: , Literal Suffix: , Create Params: [(M)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: SMALLINT UNSIGNED, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: FLOAT, Data Type: 7, Precision: 12, Literal Prefix: , Literal Suffix: , Create Params: [(M,D)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: FLOAT, Minimum Scale: -38, Maximum Scale: 38, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: DOUBLE, Data Type: 8, Precision: 22, Literal Prefix: , Literal Suffix: , Create Params: [(M,D)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DOUBLE, Minimum Scale: -308, Maximum Scale: 308, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: DOUBLE PRECISION, Data Type: 8, Precision: 22, Literal Prefix: , Literal Suffix: , Create Params: [(M,D)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DOUBLE, Minimum Scale: -308, Maximum Scale: 308, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: REAL, Data Type: 8, Precision: 22, Literal Prefix: , Literal Suffix: , Create Params: [(M,D)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DOUBLE, Minimum Scale: -308, Maximum Scale: 308, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: DOUBLE UNSIGNED, Data Type: 8, Precision: 22, Literal Prefix: , Literal Suffix: , Create Params: [(M,D)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DOUBLE UNSIGNED, Minimum Scale: -308, Maximum Scale: 308, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: DOUBLE PRECISION UNSIGNED, Data Type: 8, Precision: 22, Literal Prefix: , Literal Suffix: , Create Params: [(M,D)] [UNSIGNED] [ZEROFILL], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: true, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DOUBLE UNSIGNED, Minimum Scale: -308, Maximum Scale: 308, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: VARCHAR, Data Type: 12, Precision: 65535, Literal Prefix: ', Literal Suffix: ', Create Params: (M) [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: VARCHAR, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: TINYTEXT, Data Type: 12, Precision: 255, Literal Prefix: ', Literal Suffix: ', Create Params:  [CHARACTER SET charset_name] [COLLATE collation_name], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: TINYTEXT, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: BOOL, Data Type: 16, Precision: 3, Literal Prefix: , Literal Suffix: , Create Params: , Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: true, Local Type Name: BOOLEAN, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: DATE, Data Type: 91, Precision: 10, Literal Prefix: ', Literal Suffix: ', Create Params: , Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DATE, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: YEAR, Data Type: 91, Precision: 4, Literal Prefix: , Literal Suffix: , Create Params: [(4)], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: YEAR, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: TIME, Data Type: 92, Precision: 16, Literal Prefix: ', Literal Suffix: ', Create Params: [(fsp)], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: TIME, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: DATETIME, Data Type: 93, Precision: 26, Literal Prefix: ', Literal Suffix: ', Create Params: [(fsp)], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: DATETIME, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10",
        "Type Name: TIMESTAMP, Data Type: 93, Precision: 26, Literal Prefix: ', Literal Suffix: ', Create Params: [(fsp)], Nullable: 1, Case Sensitive: true, Searchable: 3, Unsigned Attribute: false, Fixed Prec Scale: false, Auto Increment: false, Local Type Name: TIMESTAMP, Minimum Scale: 0, Maximum Scale: 0, SQL Data Type: 0, SQL Datetime Sub: 0, Num Prec Radix: 10"
      )
    )
  }

  test("The result of retrieving index information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      database     = Some("connector_test"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData <- conn.getMetaData()
          resultSet <-
            metaData.getIndexInfo(None, Some("world"), Some("city"), true, true)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val tableCat        = resultSet.getString("TABLE_CAT")
            val tableSchem      = resultSet.getString("TABLE_SCHEM")
            val tableName       = resultSet.getString("TABLE_NAME")
            val nonUnique       = resultSet.getBoolean("NON_UNIQUE")
            val indexQualifier  = resultSet.getString("INDEX_QUALIFIER")
            val INDEXNAME       = resultSet.getString("INDEX_NAME")
            val `type`          = resultSet.getShort("TYPE")
            val ordinalPosition = resultSet.getShort("ORDINAL_POSITION")
            val columnName      = resultSet.getString("COLUMN_NAME")
            val ascOrDesc       = resultSet.getString("ASC_OR_DESC")
            val pages           = resultSet.getLong("PAGES")
            val filterCondition = resultSet.getString("FILTER_CONDITION")
            builder += s"Table Cat: $tableCat, Table Schem: $tableSchem, Table Name: $tableName, Non Unique: $nonUnique, Index Qualifier: $indexQualifier, Index Name: $INDEXNAME, Type: ${ `type` }, Ordinal Position: $ordinalPosition, Column Name: $columnName, Asc Or Desc: $ascOrDesc, Pages: $pages, Filter Condition: $filterCondition"
          builder.result()
      },
      Vector(
        "Table Cat: def, Table Schem: world, Table Name: city, Non Unique: false, Index Qualifier: null, Index Name: PRIMARY, Type: 3, Ordinal Position: 1, Column Name: ID, Asc Or Desc: A, Pages: 0, Filter Condition: null"
      )
    )
  }

  test("The result of retrieving function information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getFunctions(None, Some("sys"), None)
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val functionCat   = resultSet.getString("FUNCTION_CAT")
            val functionSchem = resultSet.getString("FUNCTION_SCHEM")
            val functionName  = resultSet.getString("FUNCTION_NAME")
            val functionType  = resultSet.getShort("FUNCTION_TYPE")
            val specificName  = resultSet.getString("SPECIFIC_NAME")
            builder += s"Function Cat: $functionCat, Function Schem: $functionSchem, Function Name: $functionName, Function Type: $functionType, Specific Name: $specificName"
          builder.result()
      },
      Vector(
        "Function Cat: def, Function Schem: sys, Function Name: extract_schema_from_file_name, Function Type: 1, Specific Name: extract_schema_from_file_name",
        "Function Cat: def, Function Schem: sys, Function Name: extract_table_from_file_name, Function Type: 1, Specific Name: extract_table_from_file_name",
        "Function Cat: def, Function Schem: sys, Function Name: format_bytes, Function Type: 1, Specific Name: format_bytes",
        "Function Cat: def, Function Schem: sys, Function Name: format_path, Function Type: 1, Specific Name: format_path",
        "Function Cat: def, Function Schem: sys, Function Name: format_statement, Function Type: 1, Specific Name: format_statement",
        "Function Cat: def, Function Schem: sys, Function Name: format_time, Function Type: 1, Specific Name: format_time",
        "Function Cat: def, Function Schem: sys, Function Name: list_add, Function Type: 1, Specific Name: list_add",
        "Function Cat: def, Function Schem: sys, Function Name: list_drop, Function Type: 1, Specific Name: list_drop",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_account_enabled, Function Type: 1, Specific Name: ps_is_account_enabled",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_consumer_enabled, Function Type: 1, Specific Name: ps_is_consumer_enabled",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_instrument_default_enabled, Function Type: 1, Specific Name: ps_is_instrument_default_enabled",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_instrument_default_timed, Function Type: 1, Specific Name: ps_is_instrument_default_timed",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_thread_instrumented, Function Type: 1, Specific Name: ps_is_thread_instrumented",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_account, Function Type: 1, Specific Name: ps_thread_account",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_id, Function Type: 1, Specific Name: ps_thread_id",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_stack, Function Type: 1, Specific Name: ps_thread_stack",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_trx_info, Function Type: 1, Specific Name: ps_thread_trx_info",
        "Function Cat: def, Function Schem: sys, Function Name: quote_identifier, Function Type: 1, Specific Name: quote_identifier",
        "Function Cat: def, Function Schem: sys, Function Name: sys_get_config, Function Type: 1, Specific Name: sys_get_config",
        "Function Cat: def, Function Schem: sys, Function Name: version_major, Function Type: 1, Specific Name: version_major",
        "Function Cat: def, Function Schem: sys, Function Name: version_minor, Function Type: 1, Specific Name: version_minor",
        "Function Cat: def, Function Schem: sys, Function Name: version_patch, Function Type: 1, Specific Name: version_patch"
      )
    )
  }

  test("The result of retrieving function column information matches the specified value.") {
    val connection = Connection[IO](
      host         = "127.0.0.1",
      port         = 13306,
      user         = "ldbc",
      password     = Some("password"),
      ssl          = SSL.Trusted,
      databaseTerm = Some(DatabaseMetaData.DatabaseTerm.SCHEMA)
    )

    assertIO(
      connection.use { conn =>
        for
          metaData  <- conn.getMetaData()
          resultSet <- metaData.getFunctionColumns(None, Some("sys"), None, Some("in_host"))
        yield
          val builder = Vector.newBuilder[String]
          while resultSet.next() do
            val functionCat     = resultSet.getString("FUNCTION_CAT")
            val functionSchem   = resultSet.getString("FUNCTION_SCHEM")
            val functionName    = resultSet.getString("FUNCTION_NAME")
            val columnName      = resultSet.getString("COLUMN_NAME")
            val columnType      = resultSet.getShort("COLUMN_TYPE")
            val dataType        = resultSet.getInt("DATA_TYPE")
            val typeName        = resultSet.getString("TYPE_NAME")
            val precision       = resultSet.getInt("PRECISION")
            val length          = resultSet.getInt("LENGTH")
            val scale           = resultSet.getShort("SCALE")
            val radix           = resultSet.getShort("RADIX")
            val nullable        = resultSet.getShort("NULLABLE")
            val remarks         = resultSet.getString("REMARKS")
            val charOctetLength = resultSet.getInt("CHAR_OCTET_LENGTH")
            val ordinalPosition = resultSet.getInt("ORDINAL_POSITION")
            val isNullable      = resultSet.getString("IS_NULLABLE")
            val specificName    = resultSet.getString("SPECIFIC_NAME")
            builder += s"Function Cat: $functionCat, Function Schem: $functionSchem, Function Name: $functionName, Column Name: $columnName, Column Type: $columnType, Data Type: $dataType, Type Name: $typeName, Precision: $precision, Length: $length, Scale: $scale, Radix: $radix, Nullable: $nullable, Remarks: $remarks, Char Octet Length: $charOctetLength, Ordinal Position: $ordinalPosition, Is Nullable: $isNullable, Specific Name: $specificName"

          builder.result()
      },
      Vector(
        "Function Cat: def, Function Schem: sys, Function Name: extract_schema_from_file_name, Column Name: , Column Type: 4, Data Type: 12, Type Name: VARCHAR, Precision: 0, Length: 64, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 256, Ordinal Position: 0, Is Nullable: YES, Specific Name: extract_schema_from_file_name",
        "Function Cat: def, Function Schem: sys, Function Name: extract_table_from_file_name, Column Name: , Column Type: 4, Data Type: 12, Type Name: VARCHAR, Precision: 0, Length: 64, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 256, Ordinal Position: 0, Is Nullable: YES, Specific Name: extract_table_from_file_name",
        "Function Cat: def, Function Schem: sys, Function Name: format_bytes, Column Name: , Column Type: 4, Data Type: -1, Type Name: TEXT, Precision: 0, Length: 65535, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 65535, Ordinal Position: 0, Is Nullable: YES, Specific Name: format_bytes",
        "Function Cat: def, Function Schem: sys, Function Name: format_path, Column Name: , Column Type: 4, Data Type: 12, Type Name: VARCHAR, Precision: 0, Length: 512, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 2048, Ordinal Position: 0, Is Nullable: YES, Specific Name: format_path",
        "Function Cat: def, Function Schem: sys, Function Name: format_statement, Column Name: , Column Type: 4, Data Type: -1, Type Name: LONGTEXT, Precision: 0, Length: 2147483647, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 2147483647, Ordinal Position: 0, Is Nullable: YES, Specific Name: format_statement",
        "Function Cat: def, Function Schem: sys, Function Name: format_time, Column Name: , Column Type: 4, Data Type: -1, Type Name: TEXT, Precision: 0, Length: 65535, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 65535, Ordinal Position: 0, Is Nullable: YES, Specific Name: format_time",
        "Function Cat: def, Function Schem: sys, Function Name: list_add, Column Name: , Column Type: 4, Data Type: -1, Type Name: TEXT, Precision: 0, Length: 65535, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 65535, Ordinal Position: 0, Is Nullable: YES, Specific Name: list_add",
        "Function Cat: def, Function Schem: sys, Function Name: list_drop, Column Name: , Column Type: 4, Data Type: -1, Type Name: TEXT, Precision: 0, Length: 65535, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 65535, Ordinal Position: 0, Is Nullable: YES, Specific Name: list_drop",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_account_enabled, Column Name: , Column Type: 4, Data Type: 1, Type Name: ENUM, Precision: 0, Length: 3, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 12, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_is_account_enabled",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_account_enabled, Column Name: in_host, Column Type: 1, Data Type: 12, Type Name: VARCHAR, Precision: 0, Length: 255, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 1020, Ordinal Position: 1, Is Nullable: YES, Specific Name: ps_is_account_enabled",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_consumer_enabled, Column Name: , Column Type: 4, Data Type: 1, Type Name: ENUM, Precision: 0, Length: 3, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 12, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_is_consumer_enabled",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_instrument_default_enabled, Column Name: , Column Type: 4, Data Type: 1, Type Name: ENUM, Precision: 0, Length: 3, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 12, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_is_instrument_default_enabled",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_instrument_default_timed, Column Name: , Column Type: 4, Data Type: 1, Type Name: ENUM, Precision: 0, Length: 3, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 12, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_is_instrument_default_timed",
        "Function Cat: def, Function Schem: sys, Function Name: ps_is_thread_instrumented, Column Name: , Column Type: 4, Data Type: 1, Type Name: ENUM, Precision: 0, Length: 7, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 28, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_is_thread_instrumented",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_account, Column Name: , Column Type: 4, Data Type: -1, Type Name: TEXT, Precision: 0, Length: 65535, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 65535, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_thread_account",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_id, Column Name: , Column Type: 4, Data Type: -5, Type Name: BIGINT UNSIGNED, Precision: 20, Length: 20, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 0, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_thread_id",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_stack, Column Name: , Column Type: 4, Data Type: -1, Type Name: LONGTEXT, Precision: 0, Length: 2147483647, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 2147483647, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_thread_stack",
        "Function Cat: def, Function Schem: sys, Function Name: ps_thread_trx_info, Column Name: , Column Type: 4, Data Type: -1, Type Name: LONGTEXT, Precision: 0, Length: 2147483647, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 2147483647, Ordinal Position: 0, Is Nullable: YES, Specific Name: ps_thread_trx_info",
        "Function Cat: def, Function Schem: sys, Function Name: quote_identifier, Column Name: , Column Type: 4, Data Type: -1, Type Name: TEXT, Precision: 0, Length: 65535, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 65535, Ordinal Position: 0, Is Nullable: YES, Specific Name: quote_identifier",
        "Function Cat: def, Function Schem: sys, Function Name: sys_get_config, Column Name: , Column Type: 4, Data Type: 12, Type Name: VARCHAR, Precision: 0, Length: 128, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 512, Ordinal Position: 0, Is Nullable: YES, Specific Name: sys_get_config",
        "Function Cat: def, Function Schem: sys, Function Name: version_major, Column Name: , Column Type: 4, Data Type: -6, Type Name: TINYINT UNSIGNED, Precision: 3, Length: 3, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 0, Ordinal Position: 0, Is Nullable: YES, Specific Name: version_major",
        "Function Cat: def, Function Schem: sys, Function Name: version_minor, Column Name: , Column Type: 4, Data Type: -6, Type Name: TINYINT UNSIGNED, Precision: 3, Length: 3, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 0, Ordinal Position: 0, Is Nullable: YES, Specific Name: version_minor",
        "Function Cat: def, Function Schem: sys, Function Name: version_patch, Column Name: , Column Type: 4, Data Type: -6, Type Name: TINYINT UNSIGNED, Precision: 3, Length: 3, Scale: 0, Radix: 10, Nullable: 1, Remarks: null, Char Octet Length: 0, Ordinal Position: 0, Is Nullable: YES, Specific Name: version_patch"
      )
    )
  }
