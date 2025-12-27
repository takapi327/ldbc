{%
  laika.title = Migration Notes
  laika.metadata.language = en
%}

# Migration Notes (from 0.4.x to 0.5.x)

## Packages

**Newly Added Packages**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

**Deprecated Packages**

| Module / Platform    | JVM | Scala Native | Scala.js |  
|----------------------|:---:|:------------:|:--------:|
| `ldbc-hikari`        |  ✅  |      ❌       |    ❌     | 

**All Packages**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 Major Changes

### 1. ZIO Ecosystem Support

Starting with 0.5.0, ldbc makes it easier to use ZIO.

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-zio-interop" % "0.5.0"
```

**Usage Example:**
```scala
import zio.*
import ldbc.zio.interop.*
import ldbc.connector.*
import ldbc.dsl.*

object Main extends ZIOAppDefault:
  
  private val datasource =
    MySQLDataSource
      .build[Task](
        host = "127.0.0.1",
        port = 3306,
        user = "ldbc"
      )
      .setPassword("password")
      .setDatabase("world")

  private val program =
    for
      connection <- datasource.getConnection
      connector = Connector.fromConnection(connection)
      result     <- sql"SELECT 1".query[Int].to[List].readOnly(connector)
    yield result

  override def run = program
```

### 2. Enhanced Authentication Plugins

New authentication plugin modules have been added. These plugins are implemented in pure Scala3 and are available on all platforms (JVM, JS, Native).

#### MySQL Clear Password Authentication

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-authentication-plugin" % "0.5.0"
```

```scala
import ldbc.connector.*
import ldbc.authentication.plugin.*

val datasource = MySQLDataSource
  .build[IO](
    host = "localhost",
    port = 3306,
    user = "cleartext-user"
  )
  .setPassword("plaintext-password")
  .setDatabase("mydb")
  .setDefaultAuthenticationPlugin(MysqlClearPasswordPlugin)
```

#### AWS Aurora IAM Authentication

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-aws-authentication-plugin" % "0.5.0"
```

```scala
import ldbc.amazon.plugin.AwsIamAuthenticationPlugin
import ldbc.connector.*

val hostname = "aurora-instance.cluster-xxx.region.rds.amazonaws.com"
val username = "iam-user"
val config = MySQLConfig.default
  .setHost(hostname)
  .setUser(username)
  .setDatabase("mydb")
  .setSSL(SSL.Trusted)  // SSL is required for IAM authentication

val plugin = AwsIamAuthenticationPlugin.default[IO]("ap-northeast-1", hostname, username)

MySQLDataSource.pooling[IO](config, plugins = List(plugin)).use { datasource =>
  val connector = Connector.fromDataSource(datasource)
  // Query execution
}
```

### 3. Deprecation of ldbc-hikari

`ldbc-hikari` is officially deprecated in 0.5.0. Please use the built-in connection pool.

**Deprecated API:**
```scala
// No longer available
import ldbc.hikari.*
```

**Recommended Approach:**
```scala
import ldbc.connector.*

val poolConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("user")
  .setPassword("password")
  .setDatabase("mydb")
  .setMinConnections(5)
  .setMaxConnections(20)

MySQLDataSource.pooling[IO](poolConfig).use { pool =>
  val connector = Connector.fromDataSource(pool)
  // Query execution
}
```

### 4. Security Enhancements

#### Enhanced SQL Parameter Escaping

String parameter escaping has been improved, strengthening protection against SQL injection attacks.

#### SSRF Attack Protection

Endpoint validation has been added during data source configuration.

```scala
// Unsafe endpoints are automatically detected and rejected
val datasource = MySQLDataSource
  .build[IO]("suspicious-host", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")
```

### 5. Performance Optimizations

#### Maximum Packet Size Configuration

Compatibility with MySQL server's `max_allowed_packet` setting has been improved.

```scala
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")
  .setMaxPacketSize(16777216)  // 16MB (match MySQL server configuration)
```

#### Connection Pool Concurrency Improvements

Connection pool state management has been improved with atomic checks, enhancing stability in concurrent environments.

### 6. API Improvements

#### File-based Query Execution

A new `updateRaws` method has been added, allowing SQL queries to be read from files and executed:

```scala
import ldbc.dsl.*
import fs2.io.file.{ Files, Path }
import fs2.text

private def readFile(filename: String): IO[String] =
  Files[IO]
    .readAll(Path(filename))
    .through(text.utf8.decode)
    .compile
    .string

for
  sql <- readFile("hoge.sql")
  _ <- DBIO.updateRaws(sql).commit(connector)
yield ()
```

## Migration Guide

### Migration from ldbc-hikari

**Before Migration (0.4.x):**
```scala
libraryDependencies ++= Seq(
  "io.github.takapi327" %% "ldbc-dsl" % "0.4.0",
  "io.github.takapi327" %% "ldbc-hikari" % "0.4.0"
)

import ldbc.hikari.*

val hikariConfig = Configuration.default
  .setJdbcUrl("jdbc:mysql://localhost:3306/mydb")
  .setUsername("user")
  .setPassword("password")
  .setMaximumPoolSize(20)

HikariDataSource.fromHikariConfig[IO](hikariConfig).use { pool =>
  // Usage
}
```

**After Migration (0.5.x):**
```scala
libraryDependencies ++= Seq(
  "io.github.takapi327" %% "ldbc-connector" % "0.5.0",
  "io.github.takapi327" %% "ldbc-dsl" % "0.5.0"
)

import ldbc.connector.*

val config = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("user")
  .setPassword("password")
  .setDatabase("mydb")
  .setMaxConnections(20)

MySQLDataSource.pooling[IO](config).use { pool =>
  val connector = Connector.fromDataSource(pool)
  // Usage
}
```

### Adding ZIO Integration

**Cats Effect Based (Traditional):**
```scala
import cats.effect.*
import ldbc.connector.*

val program: IO[List[User]] = 
  datasource.getConnection.use { connection =>
    val connector = Connector.fromConnection(connection)
    sql"SELECT * FROM users".query[User].to[List].readOnly(connector)
  }
```

**ZIO Based (New):**
```scala
import zio.*
import ldbc.zio.interop.*

val program: Task[List[User]] = 
  datasource.getConnection.use { connection =>
    val connector = Connector.fromConnection(connection)
    sql"SELECT * FROM users".query[User].to[List].readOnly(connector)
  }
```

## Summary

Migration to 0.5.x provides the following benefits:

1. **Complete ZIO Ecosystem Support**: Natural integration in ZIO-based applications
2. **Enhanced Security**: SSRF attack protection, improved SQL escaping
3. **Simplified AWS Integration**: Easy configuration for Aurora IAM authentication
4. **Performance Improvements**: Maximum packet size support, improved pool concurrency
5. **Enhanced Developer Experience**: New APIs, binary compatibility checks

Migration work mainly involves updating library dependencies and small API changes, allowing for gradual migration.
