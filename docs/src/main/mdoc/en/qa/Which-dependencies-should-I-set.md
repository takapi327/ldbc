{%
  laika.title = "Q: Which dependencies should I set?"
  laika.metadata.language = en
%}

# Q: Which dependencies should I set?

## A: To use ldbc, you need to set the following dependencies according to your needs.

- Plain DSL
- Query Builder
- Schema Definition and Model Mapping

**Connector**

To perform database connection processing using ldbc, you need to set one of the following dependencies.

**jdbc-connector**

If you want to use the traditional connector written in Java, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "jdbc-connector" % "@VERSION@",
  "com.mysql" % "mysql-connector-j" % "@MYSQL_VERSION@"
)
```

**ldbc-mysql (effect-agnostic MySQL driver)**

To use the MySQL driver written in Scala, set the driver itself `ldbc-mysql` and the bridge for the effect you use. For Cats Effect (`IO`), add `ldbc-cats-effect`:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-mysql"       % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-cats-effect" % "@VERSION@"
)
```

For ZIO (`Task`), add `ldbc-zio`; for `scala.concurrent.Future`, add `ldbc-future` instead of `ldbc-cats-effect`.

ldbc-mysql works not only on JVM but also on JS and Native platforms.

To use ldbc with Scala.js or Scala Native, set the dependencies as follows:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %%% "ldbc-mysql"       % "@VERSION@",
  "@ORGANIZATION@" %%% "ldbc-cats-effect" % "@VERSION@"
)
```

> **Note**: The legacy `ldbc-connector` is still available, but is scheduled to be removed in a future version. New projects are encouraged to use `ldbc-mysql`.

### Plain DSL

To use the plain DSL, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-dsl" % "@VERSION@"
)
```

Plain DSL is a method to write simple SQL statements as they are. For example, you can execute queries using SQL literals directly.

```scala
import ldbc.dsl.*

val plainResult = sql"SELECT name FROM user"
  .query[String]
  .to[List]
  .readOnly(connector)
// plainResult is returned as List[String]
```

### Query Builder

To use the query builder, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-query-builder" % "@VERSION@"
)
```

Query builder is a method to construct queries with type-safe API. In the following example, we define a `User` model and build a SELECT statement using `TableQuery`.

```scala 3
import ldbc.dsl.codec.Codec
import ldbc.query.builder.*

case class User(id: Int, name: String, email: String) derives Table
object User:
  given Codec[User] = Codec.derived[User]

val userQuery = TableQuery[User]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")

// userQuery.statement is generated as "SELECT id, name, email FROM user WHERE email = ?"
```

### Schema Definition and Model Mapping

To use schema definition and model mapping, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-schema" % "@VERSION@"
)
```

Schema definition and model mapping enable one-to-one mapping between table definitions and Scala models. Here's an example of defining a `User` table:

```scala 3
import ldbc.schema.*

case class User(id: Long, name: String, email: String)

class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def name: Column[String] = varchar(255)
  def email: Column[String] = varchar(255)
  
  override def * : Column[User] = (id *: name *: email).to[User]

val userQuery = TableQuery[UserTable]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")

// userQuery.statement is generated as "SELECT id, name, email FROM user WHERE email = ?"
```

### Testing

To write integration tests for repositories that use ldbc, set the following dependencies:

**ldbc-testkit** (framework-agnostic core)

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-testkit" % "@VERSION@" % Test
)
```

**ldbc-testkit-munit** (MUnit integration)

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-testkit-munit" % "@VERSION@" % Test
)
```

Both modules work on JVM, Scala.js, and Scala Native platforms:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %%% "ldbc-testkit-munit" % "@VERSION@" % Test
)
```

`ldbc-testkit-munit` provides `LdbcSuite`, a base trait that extends MUnit's `CatsEffectSuite` and offers `ephemeralTest` (auto-rollback after each test) and `persistentTest` (actual commits, for DDL).

### Code Generation from SQL Files

To generate models and table definitions from existing SQL files, add the sbt plugin to `project/plugins.sbt`. It supports both sbt 1 and sbt 2.

```scala 3
addSbtPlugin("@ORGANIZATION@" % "ldbc-plugin" % "@VERSION@")
```

See [Schema Code Generation](/en/tutorial/Schema-Code-Generation.md) for details.

### Using ldbc with ZIO

If you use ZIO (`Task`) instead of Cats Effect, add `ldbc-mysql` and the ZIO bridge `ldbc-zio`. It runs natively on ZIO without going through `zio-interop-cats`.

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-mysql" % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-zio"   % "@VERSION@"
)
```

See [How to use with ZIO](/en/qa/How-to-use-with-ZIO.md) for details.

> The legacy `ldbc-zio-interop` (using `ldbc-connector` from ZIO via `zio-interop-cats`) is still available, but the native `ldbc-zio` is recommended.

### Authentication Plugins

`ldbc-mysql` bundles the major MySQL authentication plugins, so no extra dependency is normally required. Add the following only when implementing your own authentication plugin, or when using Aurora IAM authentication.

**ldbc-authentication-plugin** (authentication plugin foundation)

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %%% "ldbc-authentication-plugin" % "@VERSION@"
)
```

**ldbc-aws-authentication-plugin** (Aurora IAM authentication)

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %%% "ldbc-aws-authentication-plugin" % "@VERSION@"
)
```

Both work on JVM, Scala.js, and Scala Native. See [Authentication Plugin](/en/reference/AuthenticationPlugin.md) for details.

## References
- [How to use Query Builder](/en/tutorial/Query-Builder.md)
- [Schema Definition Details](/en/tutorial/Schema.md)
- [Plain DSL Usage Examples](/en/tutorial/Selecting-Data.md)
- [Database Connection](/en/tutorial/Connection.md)
- [Parameterized Queries](/en/tutorial/Parameterized-Queries.md)
