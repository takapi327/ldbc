{%
  laika.title = Migration Notes
  laika.metadata.language = en
%}

# Migration Notes (from 0.6.x to 0.7.x)

## Packages

**All Packages**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-testkit`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit_3)                   |
| `ldbc-testkit-munit`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit-munit_3)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 Key Changes

### 1. New Testing Modules

0.7.0 introduces two dedicated modules for writing database tests more easily.

#### ldbc-testkit

A new framework-agnostic `ldbc-testkit` module has been added. It provides automatic rollback at the end of tests.

**`RollbackHandler`**: Provides a `Connector` wrapped in a `Resource` that automatically rolls back all changes after the test completes.

```scala
import ldbc.testkit.RollbackHandler

RollbackHandler.resource[F](dataSource).use { connector =>
  // All changes within this block are rolled back after the test
  connector.use { conn =>
    conn.executeUpdate("INSERT INTO users VALUES (1, 'Alice')")
  }
}
```

**`TestConnection`**: Intercepts `commit()` and `setAutoCommit(true)` as no-ops to prevent accidental commits during tests.

#### ldbc-testkit-munit

A new `ldbc-testkit-munit` module provides MUnit integration for database testing.

**`LdbcSuite`**: A base trait extending `CatsEffectSuite` with ldbc-specific test helpers.

```scala
import ldbc.testkit.munit.LdbcSuite

class MyDbTest extends LdbcSuite {

  // Test that rolls back after completion (for DML operations)
  ephemeralTest("insert and query") { conn =>
    for
      _     <- conn.executeUpdate("INSERT INTO users VALUES (1, 'Alice')")
      count <- conn.executeQuery("SELECT COUNT(*) FROM users").map(_.head)
    yield assertCount(count, 1)
  }

  // Test that actually commits (for DDL operations)
  persistentTest("create table") { conn =>
    conn.executeUpdate("CREATE TABLE IF NOT EXISTS test_table (id INT)")
  }
}
```

**Available assertion helpers:**

| Method | Description |
|--------|-------------|
| `assertCount(result, expected)` | Assert row count |
| `assertEmpty(result)` | Assert empty result set |
| `assertRowsUnordered(result, expected)` | Assert rows regardless of order |
| `assertRowsOrdered(result, expected)` | Assert rows in exact order |

### 2. New DSL Helper Functions

The following functions have been added to `ldbc.dsl.syntax.HelperFunctionsSyntax`.

#### `ident()` — Safe SQL identifier escaping

```scala
val tableName = "my_table"
sql"SELECT * FROM ${ident(tableName)}"
// → SELECT * FROM `my_table`
```

Wraps identifiers in backticks and removes NUL characters to prevent SQL injection.

#### `when()` — Conditional SQL fragments

```scala
val limit = 10
sql"SELECT * FROM users" ++ when(limit > 0)(sql" LIMIT $limit")
```

#### `paginate()` — Pagination helper

```scala
// With offset
sql"SELECT * FROM users " ++ paginate(limit = 20, offset = 40)
// → SELECT * FROM users LIMIT ? OFFSET ?

// Without offset
sql"SELECT * FROM users " ++ paginate(limit = 20)
// → SELECT * FROM users LIMIT ?
```

Throws `IllegalArgumentException` if `limit` or `offset` is negative.

### 3. OpenTelemetry Type-Safe Semantic Conventions Migration

`TelemetryAttribute` string constants have been migrated to the type-safe API from the `otel4s-semconv` library. This is an internal change with no direct impact on user code.

**Before (0.6.x) — internal implementation:**
```scala
TelemetryAttribute.dbSystemName
TelemetryAttribute.serverAddress(host)
TelemetryAttribute.dbNamespace(db)
TelemetryAttribute.dbOperationName(op)
TelemetryAttribute.dbQueryText(sql)
```

**After (0.7.x) — internal implementation:**
```scala
DbAttributes.DbSystemName(DbAttributes.DbSystemNameValue.Mysql.value)
ServerAttributes.ServerAddress(host)
DbAttributes.DbNamespace(db)
DbAttributes.DbOperationName(op)
DbAttributes.DbQueryText(sql)
```

What remains in `TelemetryAttribute` are MySQL-specific attributes (`DB_MYSQL_VERSION`, `DB_MYSQL_THREAD_ID`, `DB_MYSQL_AUTH_PLUGIN`) and the `SqlOperation` object only.

### 4. Enhanced Scala Native Support

`sbt-scala-native` has been upgraded from `0.4.17` to `0.5.10`. Scala Native 0.5 introduces official multithreading support, which significantly advances ldbc's Scala Native capabilities.

#### Multithreading Support in Scala Native 0.5

Scala Native 0.4 only supported single-threaded execution, but 0.5 enables **true multithreaded execution**.

Cats Effect 3.7.0 responds to this change by porting the JVM's `WorkStealingThreadPool` and `IORuntime` to Scala Native 0.5. As a result, the following features are now available on Scala Native:

- **`WorkStealingThreadPool`**: A work-stealing thread pool equivalent to the JVM now runs on Native
- **epoll / kqueue support**: Non-blocking I/O polling via `epoll` on Linux and `kqueue` on macOS/BSD
- **Full `IORuntime` support**: Fiber scheduling works nearly on par with the JVM

#### Fiber Model and ThreadLocal Incompatibility

Because Cats Effect Fibers can migrate freely between threads, the HikariCP-style approach of using `ThreadLocal` for connection pool caching does not apply. ldbc's connection pool implementation (`ConcurrentBag`) accounts for this constraint by adopting lock-free shared data structures using `Ref[F, ...]` and `Queue[F, ...]`.

### 5. Code Generator YAML Parser Migration

The YAML parser for JS/Native platforms has been migrated from `circe-scala-yaml` (armanbilge) to `scala-yaml` (VirtusLab). There are no API changes for users of the code generator.

## Breaking Changes

### Java 11 Support Dropped

Java 11 (corretto@11) is no longer supported.

**Supported Java versions:** 17, 21, 25

### Scala Version Update

The minimum supported Scala version has been updated from Scala 3.7.x to **Scala 3.8.x**.

| | Before (0.6.x) | After (0.7.x) |
|---|---|---|
| Scala version | 3.7.4 | 3.8.3 |

## Deprecated APIs

The following APIs are deprecated in 0.7.0 and will be removed in a future release.

| API | Deprecated in | Replacement |
|-----|:------------:|-------------|
| `sc(identifier)` | 0.7.0 | `ident(identifier)` |
| `Connection.fromSocketGroup(...)` | 0.7.0 | `Connection.fromNetwork(...)` |
| `SSL.fromKeyStoreFile(java.nio.file.Path, ...)` | 0.7.0 | `SSL.fromKeyStoreFile(fs2.io.file.Path, ...)` |

## Migration Guide

### Replace `sc()` with `ident()`

The `sc()` function is deprecated. Please migrate to `ident()`.

**Before (0.6.x):**
```scala
sql"SELECT * FROM ${sc(tableName)}"
```

**After (0.7.x):**
```scala
sql"SELECT * FROM ${ident(tableName)}"
// → SELECT * FROM `tableName`
```

`ident()` wraps identifiers in backticks, making it safe against SQL injection for table and column names.

### Replace `Connection.fromSocketGroup` with `fromNetwork`

**Before (0.6.x):**
```scala
Connection.fromSocketGroup(socketGroup, host, port, user, password, database)
```

**After (0.7.x):**
```scala
Connection.fromNetwork(network, host, port, user, password, database)
```

### Update the path type for `SSL.fromKeyStoreFile`

**Before (0.6.x):**
```scala
import java.nio.file.Paths

SSL.fromKeyStoreFile(Paths.get("/path/to/keystore"), password, keyPassword)
```

**After (0.7.x):**
```scala
import fs2.io.file.Path

SSL.fromKeyStoreFile(Path("/path/to/keystore"), password, keyPassword)
```

### Adopt the testing modules

You can migrate existing rollback logic to `ldbc-testkit`.

**Add dependencies to `build.sbt`:**
```scala
// Framework-agnostic
libraryDependencies += "io.github.takapi327" %% "ldbc-testkit" % "0.7.0" % Test

// MUnit integration
libraryDependencies += "io.github.takapi327" %% "ldbc-testkit-munit" % "0.7.0" % Test
```

**After (0.7.x):**
```scala
import ldbc.testkit.munit.LdbcSuite

class UserRepositoryTest extends LdbcSuite {

  ephemeralTest("can create a user") { conn =>
    for
      _     <- conn.executeUpdate("INSERT INTO users (name) VALUES ('Alice')")
      count <- conn.executeQuery("SELECT COUNT(*) FROM users").map(_.head)
    yield assertCount(count, 1)
  }
}
```

## Summary

Migrating to 0.7.x brings the following benefits:

1. **Dedicated testing modules**: `ldbc-testkit` and `ldbc-testkit-munit` make it easy to write tests with automatic rollback and MUnit integration
2. **Richer DSL helpers**: `ident()`, `when()`, and `paginate()` enable safer and more readable SQL construction
3. **Type-safe OpenTelemetry**: Migration to the semantic conventions library improves the robustness of internal implementation
4. **Enhanced Scala Native support**: Upgrading to `sbt-scala-native 0.5.x` improves cross-platform support with true multithreading

The impact on user code is primarily the migration of deprecated APIs (`sc()` → `ident()`, `fromSocketGroup` → `fromNetwork`) and updating Java/Scala version requirements.
