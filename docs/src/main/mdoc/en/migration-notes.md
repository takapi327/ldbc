{%
  laika.title = Migration Notes
  laika.metadata.language = en
%}

# Migration Notes (from 0.5.x to 0.6.x)

## Packages

**All Packages**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 Major Changes

### 1. Significant Expansion of OpenTelemetry Telemetry

0.6.0 greatly enhances telemetry support compliant with OpenTelemetry Semantic Conventions v1.39.0.

#### Addition of TelemetryConfig

`TelemetryConfig` has been added to provide fine-grained control over telemetry behavior.

```scala
import ldbc.connector.telemetry.TelemetryConfig

// Default configuration (spec-compliant)
val config = TelemetryConfig.default

// Disable query summary generation
val noSummaryConfig = TelemetryConfig.withoutQueryTextExtraction

// Custom configuration
val customConfig = TelemetryConfig.default
  .withoutQueryTextExtraction   // Disable automatic db.query.summary generation
  .withoutSanitization          // Disable query sanitization (caution: may expose sensitive data)
  .withoutInClauseCollapsing    // Disable IN clause collapsing
```

| Option | Default | Description |
|--------|:-------:|-------------|
| `extractMetadataFromQueryText` | `true` | Generate `db.query.summary` from query text for span naming |
| `sanitizeNonParameterizedQueries` | `true` | Replace literals with `?` in non-parameterized queries (required by spec) |
| `collapseInClauses` | `true` | Collapse `IN (?, ?, ?)` to `IN (?)` to reduce cardinality |

#### Addition of DatabaseMetrics and Meter

Connection pool metrics (wait time, use time, timeout count) can now be recorded via OpenTelemetry's `Meter`.

```scala
import org.typelevel.otel4s.metrics.Meter
import ldbc.connector.*

// Inject Meter to enable pool metrics
MySQLDataSource.pooling[IO](
  config = mysqlConfig,
  meter  = Some(summon[Meter[IO]])
).use { pool =>
  // Pool wait time, use time, and timeouts are automatically recorded
  pool.getConnection.use { conn => ... }
}
```

#### New Methods on MySQLDataSource

`setMeter` and `setTelemetryConfig` have been added to `MySQLDataSource`.

```scala
import org.typelevel.otel4s.metrics.Meter
import ldbc.connector.*
import ldbc.connector.telemetry.TelemetryConfig

val datasource = MySQLDataSource
  .build[IO](host = "localhost", port = 3306, user = "ldbc")
  .setPassword("password")
  .setDatabase("mydb")
  .setMeter(summon[Meter[IO]])
  .setTelemetryConfig(TelemetryConfig.default.withoutQueryTextExtraction)
```

#### pooling Method Signature Change

`meter` and `telemetryConfig` parameters have been added to `MySQLDataSource.pooling` and `MySQLDataSource.poolingWithBeforeAfter`. Existing code calling them without these arguments remains valid due to default values, but update is required when using metrics.

**Before (0.5.x):**
```scala
MySQLDataSource.pooling[IO](
  config         = mysqlConfig,
  metricsTracker = Some(tracker),
  tracer         = Some(tracer)
).use { pool => ... }
```

**After (0.6.x):**
```scala
MySQLDataSource.pooling[IO](
  config          = mysqlConfig,
  metricsTracker  = Some(tracker),
  meter           = Some(summon[Meter[IO]]),
  tracer          = Some(tracer),
  telemetryConfig = TelemetryConfig.default
).use { pool => ... }
```

#### TelemetryAttribute Key Name Changes (Breaking Change)

`TelemetryAttribute` constant names have been updated to align with Semantic Conventions v1.39.0. Updates are required if you reference `TelemetryAttribute` directly.

| Old (0.5.x) | New (0.6.x) |
|-------------|-------------|
| `DB_SYSTEM` | `DB_SYSTEM_NAME` |
| `DB_OPERATION` | `DB_OPERATION_NAME` |
| `DB_QUERY` | `DB_QUERY_TEXT` |
| `STATUS_CODE` | `DB_RESPONSE_STATUS_CODE` |
| `VERSION` | `DB_MYSQL_VERSION` |
| `THREAD_ID` | `DB_MYSQL_THREAD_ID` |
| `AUTH_PLUGIN` | `DB_MYSQL_AUTH_PLUGIN` |

### 2. Enhanced MySQL 9.x Support

MySQL 9.x is officially supported starting from 0.6.0. Internal behavior automatically adapts based on the connected MySQL version.

#### getCatalogs Change for MySQL 9.3.0+

For MySQL 9.3.0 and later, the internal implementation of `getCatalogs()` switches from the `mysql` system table to `INFORMATION_SCHEMA.SCHEMATA`. There is no impact on user code, but when connecting to MySQL 9.3+, SELECT privileges on `INFORMATION_SCHEMA` are required.

#### getTablePrivileges Change

The internal implementation of `DatabaseMetaData.getTablePrivileges()` has been changed from `mysql.tables_priv` to `INFORMATION_SCHEMA.TABLE_PRIVILEGES`.

### 3. Schema: VECTOR Type Added

A `DataType` representing MySQL's `VECTOR` type has been added to the `ldbc-schema` module.

```scala
import ldbc.schema.*

class EmbeddingTable extends Table[Embedding]("embeddings"):
  def id:        Column[Long]         = column[Long]("id")
  def embedding: Column[Array[Float]] = column[Array[Float]]("embedding", VECTOR(1536))

  override def * = (id *: embedding).to[Embedding]
```

### 4. Improved Error Tracing

`span.setStatus(StatusCode.Error, message)` is now automatically set when an error occurs in the Protocol layer. Error spans are correctly displayed in distributed tracing tools (Jaeger, Zipkin, etc.). No changes to user code are required.

## Migration Guide

### Update TelemetryAttribute Constant Names

If you are using `TelemetryAttribute` constants directly, update them to the new key names.

**Before (0.5.x):**
```scala
import ldbc.connector.telemetry.TelemetryAttribute

val attr = Attribute(TelemetryAttribute.DB_SYSTEM, "mysql")
val op   = Attribute(TelemetryAttribute.DB_OPERATION, "SELECT")
```

**After (0.6.x):**
```scala
import ldbc.connector.telemetry.TelemetryAttribute

val attr = Attribute(TelemetryAttribute.DB_SYSTEM_NAME, "mysql")
val op   = Attribute(TelemetryAttribute.DB_OPERATION_NAME, "SELECT")
```

### Enable OTel Metrics

To collect connection pool metrics, pass a `Meter` instance.

```scala
import cats.effect.IO
import org.typelevel.otel4s.metrics.Meter
import ldbc.connector.*
import ldbc.connector.telemetry.TelemetryConfig

// otel4s setup (example)
OtelJava.global[IO].use { otel =>
  otel.meterProvider.get("ldbc").flatMap { meter =>
    MySQLDataSource.pooling[IO](
      config          = mysqlConfig,
      meter           = Some(meter),
      telemetryConfig = TelemetryConfig.default
    ).use { pool =>
      pool.getConnection.use { conn =>
        // Metrics such as db.client.connection.wait_duration are recorded
        sql"SELECT 1".query[Int].to[List].readOnly(Connector.fromDataSource(pool))
      }
    }
  }
}
```

### Customize Telemetry Configuration

Use `TelemetryConfig` to control whether query text is included in span names or to manage sanitization.

```scala
import ldbc.connector.telemetry.TelemetryConfig

// Use fixed span names (no query text parsing)
val config = TelemetryConfig.withoutQueryTextExtraction

// Disable all (recommended for development/debugging only)
val debugConfig = TelemetryConfig.default
  .withoutQueryTextExtraction
  .withoutSanitization
  .withoutInClauseCollapsing
```

## Summary

Migration to 0.6.x provides the following benefits:

1. **Enhanced OpenTelemetry Spec Compliance**: Attribute key names and behavior aligned with Semantic Conventions v1.39.0
2. **Visible Pool Metrics**: Collect connection wait time, use time, and timeout count as OTel metrics
3. **Official MySQL 9.x Support**: Automatically adapts to API changes in MySQL 9.3+
4. **New VECTOR Type**: Handle MySQL's `VECTOR` type directly in schema definitions
5. **Improved Error Tracing**: Span error status is accurately displayed in distributed tracing tools

Most changes are backward compatible. Updates are only required if you directly use `TelemetryAttribute` constant names.
