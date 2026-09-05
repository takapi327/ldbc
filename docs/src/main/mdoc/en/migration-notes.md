{%
  laika.title = Migration Notes
  laika.metadata.language = en
%}

# Migration Notes (from 0.7.x to 0.8.x)

## Packages

**All packages**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-testkit`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit_3)                   |
| `ldbc-testkit-munit`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit-munit_3)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 Key Changes

### 1. SQL injection under `NO_BACKSLASH_ESCAPES` fixed

**This is the most important change in 0.8.0. Upgrading is recommended if you use the ldbc connector.**

In 0.7.x and earlier, client-side prepared statements (`useServerPrepStmts = false`, the default) escaped string parameters with backslash escaping only (`'` -> `\'`) and never consulted the server `sql_mode`.

When a session runs with the MySQL `NO_BACKSLASH_ESCAPES` sql_mode, a backslash is an ordinary character. `\'` therefore does not neutralize the quote, and **a string parameter can break out of its literal**.

```scala
// 0.7.x and earlier, in a session with sql_mode = 'NO_BACKSLASH_ESCAPES'
ps.setString(1, "zzz' OR 1=1 -- ")
// Rendered SQL: WHERE t.name = 'zzz\' OR 1=1 -- '
//            => (name = 'zzz\') OR 1=1  ... always true
```

0.8.0 fixes this in three parts.

**1. All escaping is centralised in `QueryRenderer`**

`QueryRenderer` is now the only path that turns a string parameter into a SQL literal. `Parameter` itself no longer exposes a SQL-text representation for strings, so a path that bypasses the sql_mode-aware logic cannot exist by construction.

**2. Escaping follows the server sql_mode**

| sql_mode | Escaping |
|----------|----------|
| default | `'` -> `\'`, `"` -> `\"`, `\` -> `\\`, control characters -> `\0` `\b` `\n` `\r` `\Z` |
| `NO_BACKSLASH_ESCAPES` | `'` -> `''` (doubling the single quote) |

Doubling the quote under `NO_BACKSLASH_ESCAPES` is the only way to embed a quote such that it can never be consumed by a preceding backslash.

**3. The sql_mode is tracked for the life of the session**

`Protocol.noBackslashEscapes` has been added. It is seeded from the status flags of the initial handshake and then updated from the status flags of **every OK / EOF packet received**. A `SET SESSION sql_mode = ...` issued after connecting is therefore reflected in subsequent query construction.

No user code changes are required.

### 2. JDBC 4.3 enquote API support

Following MySQL Connector/J 9.7.0 (WL #17215), four methods have been added to `ldbc.sql.Statement`. Use them to safely quote values and identifiers when assembling SQL dynamically.

| Method | Purpose |
|--------|---------|
| `enquoteLiteral(value)` | Wrap a string in single quotes as a literal |
| `enquoteIdentifier(identifier, alwaysQuote)` | Quote an identifier |
| `enquoteNCharLiteral(value)` | Produce an `N`-prefixed national character literal |
| `isSimpleIdentifier(identifier)` | Report whether an identifier can be used without quoting |

```scala
for
  stmt <- conn.createStatement()
  a    <- stmt.enquoteLiteral("G'Day")              // 'G''Day'
  b    <- stmt.enquoteIdentifier("my table", false) // `my table`
  c    <- stmt.enquoteIdentifier("user", true)      // `user`
  d    <- stmt.enquoteNCharLiteral("Hello")         // N'Hello'
  e    <- stmt.isSimpleIdentifier("user_name")      // true
  f    <- stmt.isSimpleIdentifier("select")         // false (reserved word)
yield ()
```

Following the MySQL rules, `isSimpleIdentifier` treats an identifier as simple when it consists only of `[0-9a-zA-Z$_]` or extended characters (`U+0080` and above), is not made up solely of digits, is at most 64 characters long, and is not a reserved word.

When the `ANSI_QUOTES` sql_mode is enabled, the identifier quote character is `"` rather than `` ` ``.

The methods are available on both `Statement` and `PreparedStatement`, for the ldbc connector as well as the jdbc connector.

> **Note**: the existing `ident()` helper is for embedding identifiers inside the `sql` interpolator and remains available. Use `enquoteIdentifier` when you need compatibility with the standard JDBC API, or control over `alwaysQuote`.

### 3. `ldbc-plugin` now supports sbt 2

`ldbc-plugin` is cross-built for both sbt 1 and sbt 2. Artifacts for sbt 1 (Scala 2.12) and sbt 2 (Scala 3) are published side by side.

The declaration is identical for either version; sbt resolves the right artifact.

```scala
// project/plugins.sbt — the same for sbt 1.x and sbt 2.x
addSbtPlugin("io.github.takapi327" % "ldbc-plugin" % "0.8.0")
```

### 4. Column-order bug in `insert` fixed

The tuple overload of `insert` now goes through the entity mapping defined by the table's `*` projection.

In 0.7.x the tuple was cast onto the column encoder directly, so **values could be inserted into the wrong columns whenever the field order of the model differed from the column order of the `*` projection**.

```scala
userTable.insert((1L, "Alice", Some(20)))
```

If the `*` projection is not ordered as `id *: name *: age`, code like the above produces a different parameter order in 0.8.0 than in 0.7.x. The change makes the result correct, but **verifying it with your tests after upgrading is recommended**.

### 5. Dependency updates

| Library | Before (0.7.x) | After (0.8.0) |
|---------|---------------|---------------|
| MySQL Connector/J | 9.6.0 | 9.7.0 |
| twiddles-core | 0.10.0 | 1.1.0 |

`ldbc.connector.data.Constants.DRIVER_VERSION` has been updated to `0.8.0` as well.

## Breaking Changes

### `Parameter` is now a `sealed trait` and `sql` has been removed

`ldbc.connector.data.Parameter` is now a `sealed trait` with one case class per type, and `def sql: String` has been removed.

**Before (0.7.x):**
```scala
trait Parameter:
  def columnDataType: ColumnDataType
  def sql:            String
  def encode:         BitVector
```

**After (0.8.0):**
```scala
sealed trait Parameter:
  def columnDataType: ColumnDataType
  def encode:         BitVector
```

This is part of Key Change 1 (the SQL injection fix). Rendering a string into a SQL literal depends on the sql_mode, so that representation was removed from `Parameter` to leave `QueryRenderer` as the only route.

- **Custom `Parameter` implementations** are no longer possible now that the trait is `sealed`. Use the factory methods such as `Parameter.string(...)`
- **Code that read `param.sql`** should use `param.toString`. Note that `toString` is a sql_mode-independent literal intended for display and diagnostics, and **must not be used to assemble SQL for execution**

### `params` removed from `SQLException`

The `params: SortedMap[Int, Parameter]` parameter has been removed from `SQLException` and its subclasses, as well as from the signature of `ERRPacket.toException`.

As a result, the following are no longer emitted:

- The OpenTelemetry attributes `error.parameter.$i.type` / `error.parameter.$i.value`
- The "and the arguments were" section of the exception message

This closes the paths by which bound values could leak through exception messages and telemetry. **If you build dashboards or alerts on those attributes, you are affected.**

### Four abstract methods added to `Statement`

The four methods from Key Change 2 are added as abstract members of `ldbc.sql.Statement`. There is no impact as long as you use the connectors that ldbc provides, but **implementing `Statement` or `PreparedStatement` yourself will now fail to compile.**

### twiddles-core is now 1.1.0

The twiddles-core dependency of `ldbc-dsl` and `ldbc-connector` moved from 0.10.0 to 1.1.0. Align the version if your project uses twiddles directly.

### What has not changed

For reference, these requirements are unchanged from 0.7.x.

| | 0.7.x | 0.8.0 |
|---|---|---|
| Java versions | 17, 21, 25 | unchanged |
| Scala versions | 3.3.x / 3.8.x | unchanged |

## Deprecated APIs

The following APIs deprecated in 0.7.0 remain available in 0.8.0, but are scheduled for removal in a future version.

| API | Deprecated in | Replacement |
|-----|:-------------:|-------------|
| `sc(identifier)` | 0.7.0 | `ident(identifier)` |
| `Connection.fromSocketGroup(...)` | 0.7.0 | `Connection.fromNetwork(...)` |
| `SSL.fromKeyStoreFile(java.nio.file.Path, ...)` | 0.7.0 | `SSL.fromKeyStoreFile(fs2.io.file.Path, ...)` |

See the [0.7.x migration notes](https://takapi327.github.io/ldbc/0.7/en/migration-notes.html) for how to migrate.

## Migration Guide

### Update your dependencies

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-dsl"       % "0.8.0"
libraryDependencies += "io.github.takapi327" %% "ldbc-connector" % "0.8.0"
```

Use `%%%` for cross-platform builds.

### Replace references to `Parameter#sql`

**Before (0.7.x):**
```scala
val literal: String = parameter.sql
```

**After (0.8.0):**
```scala
val display: String = parameter.toString
```

`toString` is for display and diagnostics. Do not use it to assemble SQL for execution. To build SQL dynamically, use placeholders with `setXxx`, or the enquote API.

### Fix code that constructs `SQLException` directly

**Before (0.7.x):**
```scala
SQLException(
  message = "...",
  sql     = Some(sql),
  params  = params
)
```

**After (0.8.0):**
```scala
SQLException(
  message = "...",
  sql     = Some(sql)
)
```

### Revisit telemetry that relied on `error.parameter.*`

Bound-value attributes are no longer emitted. Use the `db.query.text` attribute to identify a query. See [Telemetry](/en/reference/Telemetry.md) for how query text is sanitized.

### Assemble SQL with dynamic identifiers safely

**Inside the `sql` interpolator (as before):**
```scala
sql"SELECT * FROM ${ident(tableName)}"
```

**When you need the standard JDBC API:**
```scala
for
  stmt      <- conn.createStatement()
  isSimple  <- stmt.isSimpleIdentifier(tableName)
  quoted    <- stmt.enquoteIdentifier(tableName, alwaysQuote = true)
  resultSet <- stmt.executeQuery(s"SELECT * FROM $quoted")
yield resultSet
```

### Use `ldbc-plugin` from an sbt 2 project

The `project/plugins.sbt` declaration is the same as for sbt 1. The artifact matching your sbt version is resolved automatically.

```scala
addSbtPlugin("io.github.takapi327" % "ldbc-plugin" % "0.8.0")
```

## Summary

Migrating to 0.8.x gives you:

1. **Improved security**: the SQL injection in client-side prepared statements under the `NO_BACKSLASH_ESCAPES` sql_mode is fixed, and escaping is centralised in one place
2. **Alignment with the JDBC standard**: enquote APIs matching MySQL Connector/J 9.7.0
3. **A smaller disclosure surface**: bound values are no longer emitted in exception messages or telemetry
4. **A more correct `insert`**: values no longer shift when the field order of a model differs from its column order
5. **sbt 2 support**: `ldbc-plugin` is usable from both sbt 1 and sbt 2

The impact on user code is limited to places that used `Parameter#sql` or the `params` argument of `SQLException` directly. Most projects can migrate by updating dependency versions alone.
