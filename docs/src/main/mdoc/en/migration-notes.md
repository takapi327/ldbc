{%
  laika.title = Migration Notes
  laika.metadata.language = en
%}

# Migration Notes (0.8.x to 0.9.x)

0.9.x is a major change that re-architects ldbc to be effect-agnostic (tagless-final). The driver up to 0.8.x (`ldbc-connector`) was implemented on top of Cats Effect 3 (`IO`). In 0.9.x, ldbc introduces its own effect type-class hierarchy (`Async ⊂ Temporal ⊂ Concurrent`) and builds the driver, network layer, and connection pool on top of it. This lets **Cats Effect (`IO`) / ZIO (`Task`) / `scala.concurrent.Future`** each run natively. In addition, a lightweight effect type `Fx` that depends on no external effect library is also newly introduced in 0.9.x (`ldbc-fx`), used as the `Future` backend and for standalone use.

> **Important**: The existing `ldbc-connector` (the Cats Effect-based MySQL connector) **is still available in 0.9.x**. 0.9.x does not force migration to the effect-agnostic version; it adds the **effect-agnostic new driver `ldbc-mysql`** alongside `ldbc-connector`, together with per-effect bridges (`ldbc-cats-effect` / `ldbc-zio` / `ldbc-future`). There is no need to move to `ldbc-mysql` in a hurry.
>
> However, making ldbc effect-agnostic **did introduce breaking changes in the shared modules (`ldbc-sql` / `ldbc-core` / `ldbc-dsl`)**. Even if you keep using `ldbc-connector`, code that acquires connections (`getConnection`) or requires `Sync[DBIO]` has to be updated (streaming keeps working as long as you have `import ldbc.connector.*`). See "Breaking Changes" below for details.
>
> Also, **`ldbc-connector` is scheduled to be removed in a future version**. The effect-agnostic new driver `ldbc-mysql` (and the effect-specific bridges) is its successor, so new projects are encouraged to use `ldbc-mysql`. Existing projects should also consider migrating to `ldbc-mysql` when ready.

## Packages

**Existing packages (carried over from 0.8.x)**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-testkit`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit_3)                   |
| `ldbc-testkit-munit`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit-munit_3)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

**Packages newly added in 0.9.x**

| Module / Platform            | JVM | Scala Native | Scala.js | Overview                                                                                     |
|------------------------------|:---:|:------------:|:--------:|----------------------------------------------------------------------------------------------|
| `ldbc-effect`                |  ✅  |      ✅       |    ✅     | Effect type-class hierarchy (`Async` / `Temporal` / `Concurrent`) and concurrency primitives (Ref / Deferred / Semaphore / Resource) |
| `ldbc-fx`                    |  ✅  |      ✅       |    ✅     | The lightweight effect type `Fx` introduced in 0.9.x. A `Concurrent[Fx]` instance for the `Future` backend and standalone use |
| `ldbc-net`                   |  ✅  |      ✅       |    ✅     | Effect-agnostic non-blocking transport (IoEngine + `Socket[F]`)                              |
| `ldbc-pool`                  |  ✅  |      ✅       |    ✅     | Effect-agnostic connection pool (`F: Concurrent`)                                            |
| `ldbc-mysql`                 |  ✅  |      ✅       |    ✅     | **Effect-agnostic MySQL driver** (Cats Effect-free; the successor to `ldbc-connector`)       |
| `ldbc-telemetry`             |  ✅  |      ✅       |    ✅     | DB-agnostic OpenTelemetry tracing / metrics SPI (Tracer / Span / Meter)                      |
| `ldbc-otel4s`                |  ✅  |      ✅       |    ✅     | otel4s backend implementation of the `ldbc-telemetry` SPI (for Cats Effect)                  |
| `ldbc-zio-telemetry`         |  ✅  |      ❌       |    ❌     | zio-telemetry backend implementation of the `ldbc-telemetry` SPI (JVM only)                  |
| `ldbc-cats-effect`           |  ✅  |      ✅       |    ✅     | Cats Effect (`IO`) bridge. fs2 streaming and `Connector[IO]`                                 |
| `ldbc-future`                |  ✅  |      ✅       |    ✅     | `scala.concurrent.Future` bridge (uses `Fx` as the backend internally)                       |
| `ldbc-zio`                   |  ✅  |      ✅       |    ✅     | ZIO (`Task`) bridge. ZStream streaming and `Connector[Task]`                                 |

## 🎯 Key Changes

### 1. Effect-agnostic (tagless-final)

This is the central change in 0.9.x. The driver up to 0.8.x (`ldbc-connector`) was implemented assuming Cats Effect 3 (`IO`). In 0.9.x, ldbc introduces its own effect type-class hierarchy and builds the driver, network layer, and pool on top of it.

```
Async ⊂ Temporal ⊂ Concurrent   (ldbc-effect)
```

- **Effects that have `Concurrent` run natively**: a `Concurrent[F]` instance is provided for each of `IO` / `Task` / `Fx`, and they run without any inter-effect conversion or bridge layer.
- **`Future` cannot satisfy `Concurrent`**, so it uses `Fx` as the backend internally and converts the result to `Future` exactly once (`ldbc-future`).
- Cats Effect independence is achieved. The driver core (`ldbc-mysql` / `ldbc-net` / `ldbc-pool`) references only the `ldbc.effect` type classes and does not depend on cats-effect.

From a user's perspective, the main difference is that **the module you depend on and how you obtain a `Connector` change depending on which effect you use**.

### 2. New connector layout (per-effect bridges)

The effect-agnostic driver `ldbc-mysql` is not tied to a specific effect on its own. The `Connector[F]` that actually runs queries is obtained from the bridge module corresponding to the effect you use.

| Effect                    | Modules                                 | Where to obtain `Connector`          |
|---------------------------|-----------------------------------------|--------------------------------------|
| Cats Effect (`IO`)        | `ldbc-mysql` + `ldbc-cats-effect`       | `ldbc.catseffect.Connector`          |
| ZIO (`Task`)              | `ldbc-mysql` + `ldbc-zio`               | `ldbc.zio.Connector`                 |
| `scala.concurrent.Future` | `ldbc-mysql` + `ldbc-future`            | `ldbc.future.Connector`              |

Every bridge provides the same `fromConnection` / `fromDataSource` as `ldbc-connector` (`fromConfig` is provided on each `MySQLDataSource`). The return type is always the common base type `ldbc.Connector[F]`.

**Cats Effect (`IO`) example:**

```scala
import cats.effect.IO

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.Connector

val datasource = MySQLDataSource
  .build[IO]("127.0.0.1", 3306, "user")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted)

val connector = Connector.fromDataSource(datasource)

sql"SELECT name FROM city LIMIT 1".query[String].to[Option].readOnly(connector)
```

**ZIO (`Task`) example:**

```scala
import zio.Task

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.zio.Connector
import ldbc.zio.given

val datasource = MySQLDataSource
  .build[Task]("127.0.0.1", 3306, "user")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted)

val connector = Connector.fromDataSource(datasource)
```

> **If you do not migrate from `ldbc-connector`**: you can keep using `import ldbc.connector.*` with `MySQLDataSource` / `Connector.fromDataSource` as before. The query-building side APIs such as the DSL (`ldbc-dsl`) and schema (`ldbc-schema`) are shared, so you can switch between the two just by swapping the `Connector[F]`.

### 3. Streaming is native per effect

Query-result streaming is provided with each effect's native stream type.

- Cats Effect (`IO`): `fs2.Stream` (`ldbc-cats-effect`)
- ZIO (`Task`): `zio.stream.ZStream` (`ldbc-zio`)

As part of this, `ldbc-dsl` dropped its fs2 dependency and `Query#stream` was removed from it. **If you keep using `ldbc-connector`, `query.stream` keeps working as long as you have `import ldbc.connector.*`** — `ldbc-connector` depends on `ldbc-cats-effect` and re-exports `stream`. Only `ldbc-mysql` users need to add `ldbc-cats-effect`. See item 2 under "Breaking Changes" for details.

### 4. Telemetry backends are separated

The tracing / metrics SPI is extracted into the DB-agnostic `ldbc-telemetry`, and the actual backends are separate modules.

- `ldbc-otel4s`: otel4s backend (for Cats Effect; spans run natively on the effect `F`, so there is no round-trip overhead)
- `ldbc-zio-telemetry`: zio-telemetry backend (JVM only)

By default it is a no-op (emits nothing); you add the corresponding backend module only when you use telemetry. Telemetry for `ldbc-connector` (otel4s-based) is unchanged.

## Breaking Changes

0.9.x is a release that adds new modules, but making ldbc effect-agnostic **did introduce breaking changes in the shared modules (`ldbc-sql` / `ldbc-core` / `ldbc-dsl`)**. Even if you keep using `ldbc-connector` / `jdbc-connector` as-is, any code matching the items below has to be updated.

On the other hand, **the query-building APIs are the same as in 0.8.x**: the `sql"..."` interpolator, `query` / `to[List]` / `unsafe` / `option` / `nel`, `readOnly` / `commit` / `transaction` / `rollback`, the query builder (`ldbc-query-builder`), schema definitions (`ldbc-schema`), and code generation (`ldbc-codegen`) are unchanged.

| # | Change | Affects |
|---|--------|---------|
| 1 | `DataSource` moved, and `getConnection`'s return type changed | Code calling `getConnection` directly / custom `DataSource` implementations |
| 2 | `Query#stream` moved to `ldbc-cats-effect` | Code using fs2 streaming (no impact when using `ldbc-connector`) |
| 3 | `Sync[DBIO]` became `MonadError[DBIO, Throwable]` | Code requiring `Sync` for `DBIO` |
| 4 | cats-effect-derived operations removed from the Free algebra | Code implementing its own interpreter (`Visitor`) |
| 5 | Transitive dependency changes | Code relying on fs2 / cats-effect coming in via `ldbc-dsl` / `ldbc-core` |

### 1. `DataSource` moved to `ldbc.sql`, and `getConnection`'s return type changed

| | 0.8.x | 0.9.0 |
|---|---|---|
| Type | `ldbc.DataSource` | `ldbc.sql.DataSource` |
| `getConnection` | `Resource[F, Connection[F]]` | `F[(Connection[F], F[Unit])]` (allocated form) |

`ldbc.DataSource` was removed and is now `ldbc.sql.DataSource`. If you use `import ldbc.connector.*` / `import jdbc.connector.*`, the type name still resolves because each package object re-exports `ldbc.sql.DataSource`. If you wrote `import ldbc.DataSource` directly, the import has to change.

`getConnection` now returns **the connection together with its release action** rather than a `Resource`. `Resource` is a different type per effect (`cats.effect.Resource` / ZIO's `Scope` / `ldbc.effect.Resource`), so putting it in the signature would pin `DataSource` to one particular effect. With the allocated form, Cats Effect / ZIO / Fx / Future can all share the same `DataSource[F]`.

**Before:**

```scala
datasource.getConnection.use { conn =>
  conn.setCatalog("world") *> conn.getCatalog()
}
```

**After (`ldbc-connector`):** import the `use` extension method. It is implemented with a bracket, so the release runs on success, error, and cancellation alike.

```scala
import ldbc.connector.syntax.*

datasource.use { conn =>
  conn.setCatalog("world") *> conn.getCatalog()
}
```

With the effect-agnostic driver (`ldbc-mysql`), `import ldbc.mysql.syntax.*` (or `import ldbc.pool.*`) brings in an equivalent `use`.

**If you want a `Resource`**, or if you use `jdbc-connector` alone and therefore cannot reach `ldbc.connector.syntax`, build it yourself:

```scala
val connection: Resource[F, Connection[F]] =
  Resource.make(datasource.getConnection)(_._2).map(_._1)
```

**If you implement your own `DataSource`**, update the `getConnection` implementation. An existing `Resource`-based implementation migrates by appending `.allocated`.

```scala
// Before
override def getConnection: Resource[F, Connection[F]] =
  Resource.fromAutoCloseable(...).map(ConnectionImpl(_))

// After
override def getConnection: F[(Connection[F], F[Unit])] =
  Resource.fromAutoCloseable(...).map(ConnectionImpl(_)).allocated
```

### 2. `Query#stream` moved from `ldbc-dsl` to `ldbc-cats-effect`

`ldbc-dsl` dropped its fs2 dependency and removed `stream` / `stream(fetchSize)` from `Query`. fs2 streaming is now provided as an extension method in `ldbc-cats-effect`. **What you have to do depends on which driver you use.**

#### Using `ldbc-connector` (the existing driver)

**No dependency to add.** `ldbc-connector` depends on `ldbc-cats-effect`, and the `ldbc.connector` package object re-exports `stream` and `syncDBIO`, so `query.stream` keeps working as long as you have `import ldbc.connector.*`.

```scala
import ldbc.dsl.*
import ldbc.connector.*

sql"SELECT name FROM city".query[String].stream(100)
```

Only if you referred to the package in qualified form (`ldbc.connector.MySQLDataSource`) rather than importing it with a wildcard do you need to add `import ldbc.connector.*` — in 0.8.x, `stream` came in from `import ldbc.dsl.*`.

@:callout(warning)

When you use `ldbc-connector`, **you cannot import both `ldbc.connector.*` and `ldbc.catseffect.*` in the same file**. `stream`, `syncDBIO` and `Connector` all come in from both and every one of them becomes ambiguous. Stick to one of the two, or import only the names you need.

@:@

#### Using `ldbc-mysql` (the effect-agnostic driver)

You need to add `ldbc-cats-effect` and `import ldbc.catseffect.*`.

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-cats-effect" % "0.9.0"
```

**Before:**

```scala
import ldbc.dsl.*

sql"SELECT name FROM city".query[String].stream(100)
```

**After:**

```scala
import ldbc.dsl.*
import ldbc.catseffect.*

sql"SELECT name FROM city".query[String].stream(100)
```

To use `ZStream` with ZIO, add `ldbc-zio` and call `query.stream(connector)`. (`ZStream` is always a ZIO type, so it cannot be built on top of `DBIO`; it takes the connector as an argument instead.)

### 3. `Sync[DBIO]` became `MonadError[DBIO, Throwable]`

`ldbc-dsl` dropped its cats-effect dependency and changed the instance it provides for `DBIO`. The instance brought implicitly into scope by `import ldbc.dsl.*` changes accordingly.

| | 0.8.x | 0.9.0 |
|---|---|---|
| Provided by `import ldbc.dsl.*` | `implicit val syncDBIO: Sync[DBIO]` | `implicit val monadErrorDBIO: MonadError[DBIO, Throwable]` |
| When you need `Sync[DBIO]` | `ldbc.dsl.DBIO.syncDBIO` | `ldbc.catseffect.syncDBIO` (`ldbc-cats-effect`) |

If you only use cats error-handling combinators such as `raiseError` / `handleErrorWith` / `attempt` / `onError` on `DBIO`, no change is needed. Where `Sync[DBIO]` is explicitly required (for example when combining with fs2), add `import ldbc.catseffect.*`.

Note that `ldbc.catseffect.syncDBIO` has `rootCancelScope = Uncancelable`. `DBIO` is a Free program and cannot express real cancellation, so `MonadCancel` operations behave as their uncancelable identities.

### 4. cats-effect-derived operations removed from `DBIO` / the Free algebra

The following were removed from `ConnectionOp` / `StatementOp` / `PreparedStatementOp`:

- `Monotonic` / `Realtime` (and constructors such as `ConnectionIO.monotonic` / `realtime`)
- `ForceR` / `Uncancelable` / `Poll1` / `Canceled` / `OnCancel` (and `capturePoll`)
- the `given Sync[PreparedStatementIO]` on `PreparedStatementIO`
- `Suspend(hint: Sync.Type, thunk)`. Only `ConnectionOp` keeps a `Suspend(thunk)` that takes no `Sync.Type`; it was removed from `StatementOp` / `PreparedStatementOp`

In addition, `drainRows` was added to `ResultSetOp.Visitor` (an operation that decodes a fully materialized result set in a single effect).

`KleisliInterpreter`'s constraint was relaxed from `Sync[F]` to `MonadError[F, Throwable]`. Since `Sync[F]` satisfies `MonadError[F, Throwable]`, callers of `KleisliInterpreter` need no change.

**Impact**: code that calls `ConnectionIO.monotonic` and friends directly, and code that implements `ConnectionOp.Visitor` / `StatementOp.Visitor` / `PreparedStatementOp.Visitor` / `ResultSetOp.Visitor` itself (i.e. a custom interpreter), will fail to compile. Ordinary DSL usage is unaffected.

### 5. Transitive dependency changes

| Module | 0.8.x | 0.9.0 |
|--------|-------|-------|
| `ldbc-core` | `cats-free` + `cats-effect` | `cats-free` only |
| `ldbc-dsl` | `twiddles-core` + `fs2-core` | `twiddles-core` only |

If you relied on cats-effect or fs2 reaching your classpath via `ldbc-dsl` / `ldbc-core`, add those dependencies explicitly, or add `ldbc-cats-effect`. `ldbc-connector` still depends on cats-effect / fs2, and in 0.9.x it additionally depends on `ldbc-cats-effect`, so nothing is missing from your classpath if you use `ldbc-connector`.

### `ldbc-mysql`'s `Parameter` is a new implementation for the effect-agnostic driver

`ldbc.mysql.data.Parameter` follows the same design as `ldbc-connector`'s `Parameter` (which became a `sealed trait` with `sql` removed in 0.8.0). Turning a string into a SQL literal is done only through `ldbc.mysql.data.QueryRenderer`, and `Parameter` itself only exposes a sql_mode-independent `toString` (for diagnostics). This is an internal API of the new `ldbc-mysql` module and does not affect typical user code.

### Dependencies

When using the `ldbc-mysql` effect bridges, the following are additionally required.

| Effect | Main additional dependencies |
|--------|------------------------------|
| Cats Effect | `cats-effect` / `fs2` (brought in transitively by `ldbc-cats-effect`) |
| ZIO | `zio` / `zio-streams` (brought in transitively by `ldbc-zio`) |

### Unchanged

| | 0.8.x | 0.9.0 |
|---|---|---|
| Java versions | 17, 21, 25 | unchanged |
| Scala versions | 3.3.x / 3.8.x | unchanged |

## Migration Guide

### Keep using `ldbc-connector`

`ldbc-connector`'s `MySQLDataSource` / `Connector` / pooling APIs are the same as in 0.8.x. Start by bumping the version.

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"       % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-connector" % "0.9.0"
```

Then update the places matching these three items from "Breaking Changes". For most projects this completes the migration.

1. `datasource.getConnection.use { ... }` → add `import ldbc.connector.syntax.*` and use `datasource.use { ... }`
2. Using `query.stream` → with `ldbc-connector`, nothing to do as long as you have `import ldbc.connector.*`. With `ldbc-mysql`, add `ldbc-cats-effect` and `import ldbc.catseffect.*`
3. Requiring `Sync[DBIO]` → likewise `import ldbc.catseffect.*`

```scala
// If 2 or 3 applies
libraryDependencies += "io.github.takapi327" %%% "ldbc-cats-effect" % "0.9.0"
```

### Migrate to the effect-agnostic driver (`ldbc-mysql`)

Add `ldbc-mysql` and a bridge module depending on the effect you use.

**Cats Effect (`IO`):**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"         % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-mysql"       % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-cats-effect" % "0.9.0"
```

**ZIO (`Task`):**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"   % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-mysql" % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-zio"   % "0.9.0"
```

**`scala.concurrent.Future`:**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"    % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-mysql"  % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-future" % "0.9.0"
```

On the code side, replace `import ldbc.connector.*` with the corresponding bridge (`ldbc.catseffect.Connector` / `ldbc.zio.Connector` / `ldbc.future.Connector`) and `ldbc.mysql.MySQLDataSource`. Query building (`sql"..."` / `query` / `readOnly` / `commit`, etc.) is shared and needs no changes.

### Enable telemetry

**Cats Effect (otel4s):**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-otel4s" % "0.9.0"
```

**ZIO (zio-telemetry, JVM only):**

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-zio-telemetry" % "0.9.0"
```

## Summary

0.9.x delivers the following.

1. **Effect-agnostic**: the `Async` / `Temporal` / `Concurrent` type-class hierarchy lets you handle `IO` / `Task` / `Future` / `Fx` natively
2. **Multi-effect support**: bridges for Cats Effect / ZIO / Future are added. Streaming is also provided natively via fs2 / ZStream
3. **Telemetry separation**: a DB-agnostic SPI (`ldbc-telemetry`) separated from the otel4s / zio-telemetry backends
4. **The existing driver stays usable**: `ldbc-connector` still works in 0.9.x; moving to the effect-agnostic driver is optional

That said, making ldbc effect-agnostic introduced breaking changes in the shared modules (`ldbc-sql` / `ldbc-core` / `ldbc-dsl`). Even projects that keep using `ldbc-connector` need to update the places that call `getConnection`, use `Query#stream`, or require `Sync[DBIO]`. See "Breaking Changes" below for details. The query-building APIs are unchanged, so the edits are confined to the connection-acquisition and streaming boundaries.
