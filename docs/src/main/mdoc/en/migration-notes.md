{%
  laika.title = Migration Notes
  laika.metadata.language = en
%}

# Migration Notes (0.8.x to 0.9.x)

0.9.x is a major change that re-architects ldbc to be effect-agnostic (tagless-final). The driver up to 0.8.x (`ldbc-connector`) was implemented on top of Cats Effect 3 (`IO`). In 0.9.x, ldbc introduces its own effect type-class hierarchy (`Async ⊂ Temporal ⊂ Concurrent`) and builds the driver, network layer, and connection pool on top of it. This lets **Cats Effect (`IO`) / ZIO (`Task`) / `scala.concurrent.Future`** each run natively. In addition, a lightweight effect type `Fx` that depends on no external effect library is also newly introduced in 0.9.x (`ldbc-fx`), used as the `Future` backend and for standalone use.

> **Important**: The existing `ldbc-connector` (the Cats Effect-based MySQL connector) **still works as-is in 0.9.x**. 0.9.x does not break compatibility or force migration to the effect-agnostic version; it adds the **effect-agnostic new driver `ldbc-mysql`** alongside `ldbc-connector`, together with per-effect bridges (`ldbc-cats-effect` / `ldbc-zio` / `ldbc-future`). There is no need to migrate in a hurry.
>
> However, **`ldbc-connector` is scheduled to be removed in a future version**. The effect-agnostic new driver `ldbc-mysql` (and the effect-specific bridges) is its successor, so new projects are encouraged to use `ldbc-mysql`. Existing projects should also consider migrating to `ldbc-mysql` when ready.

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

fs2 streaming when using `ldbc-connector` is unchanged.

### 4. Telemetry backends are separated

The tracing / metrics SPI is extracted into the DB-agnostic `ldbc-telemetry`, and the actual backends are separate modules.

- `ldbc-otel4s`: otel4s backend (for Cats Effect; spans run natively on the effect `F`, so there is no round-trip overhead)
- `ldbc-zio-telemetry`: zio-telemetry backend (JVM only)

By default it is a no-op (emits nothing); you add the corresponding backend module only when you use telemetry. Telemetry for `ldbc-connector` (otel4s-based) is unchanged.

## Breaking Changes

### Source-compatible in principle

0.9.x is a release that **adds new modules**, and there are in principle no breaking changes for code that uses the existing `ldbc-connector`. The APIs for the DSL, query builder, schema, code generation, and so on are the same as in 0.8.x.

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

The simplest migration is to just bump the version. The `ldbc-connector` API is compatible with 0.8.x.

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"       % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-connector" % "0.9.0"
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
4. **Backward compatibility**: the existing `ldbc-connector` remains usable; migration is optional

Projects that keep using `ldbc-connector` can migrate simply by updating the dependency version. Whether to move to the effect-agnostic driver can be decided based on the effect you use and your future direction.
