{%
laika.title = ldbc
laika.versioned = false
laika.metadata {
language = en
isRootPath = true
}
%}

# ldbc (Lepus Database Connectivity)

@:image(img/lepus_logo.png) {
alt = "ldbc (Lepus Database Connectivity)"
style = "center-logo"
}

[![Continuous Integration](https://github.com/takapi327/ldbc/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/takapi327/ldbc/actions/workflows/ci.yml)
[![MIT License](https://img.shields.io/badge/license-MIT-green)](https://en.wikipedia.org/wiki/MIT_License)
[![Scala Version](https://img.shields.io/badge/scala-v3.3.x-red)](https://github.com/lampepfl/dotty)
[![Typelevel Affiliate Project](https://img.shields.io/badge/typelevel-affiliate%20project-FF6169.svg)](https://typelevel.org/projects/affiliate/)
[![javadoc](https://javadoc.io/badge2/io.github.takapi327/ldbc-dsl_3/javadoc.svg)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)
[![Maven Central Version](https://maven-badges.herokuapp.com/maven-central/io.github.takapi327/ldbc-dsl_3/badge.svg?color=blue)](https://search.maven.org/artifact/io.github.takapi327/ldbc-dsl_3/0.3.0-beta8/jar)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-dsl/latest-by-scala-version.svg?color=blue)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-dsl/latest-by-scala-version.svg?color=blue&targetType=js)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-dsl/latest-by-scala-version.svg?color=blue&targetType=native)](https://index.scala-lang.org/takapi327/ldbc)

========================================================================================

ldbc (Lepus Database Connectivity) is Pure functional JDBC layer with Cats Effect 3 and Scala 3.

ldbc is a [Typelevel](http://typelevel.org/) project. This means we embrace pure, typeful, functional programming, and provide a safe and friendly environment for teaching, learning, and contributing as described in the Scala [Code of Conduct](http://scala-lang.org/conduct.html).

Note that **ldbc** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

## Modules availability

ldbc is available on the JVM, Scala.js, and ScalaNative

| Module / Platform    | JVM | Scala Native | Scala.js |  
|----------------------|:---:|:------------:|:--------:|
| `ldbc-core`          |  ✅  |      ✅       |    ✅     |
| `ldbc-sql`           |  ✅  |      ✅       |    ✅     |
| `ldbc-connector`     |  ✅  |      ✅       |    ✅     |
| `jdbc-connector`     |  ✅  |      ❌       |    ❌     |
| `ldbc-dsl`           |  ✅  |      ✅       |    ✅     |
| `ldbc-query-builder` |  ✅  |      ✅       |    ✅     |
| `ldbc-schema`        |  ✅  |      ✅       |    ✅     |
| `ldbc-schemaSpy`     |  ✅  |      ❌       |    ❌     |
| `ldbc-codegen`       |  ✅  |      ✅       |    ✅     |
| `ldbc-hikari`        |  ✅  |      ❌       |    ❌     |
| `ldbc-plugin`        |  ✅  |      ❌       |    ❌     |

## Quick Start

For people that want to skip the explanations and see it action, this is the place to start!

### Dependency Configuration

```scala
libraryDependencies += "@ORGANIZATION@" %% "ldbc-dsl" % "@VERSION@"
```

For Cross-Platform projects (JVM, JS, and/or Native):

```scala
libraryDependencies += "@ORGANIZATION@" %%% "ldbc-dsl" % "@VERSION@"
```

The dependency package used depends on whether the database connection is made via a connector using the Java API or a connector provided by ldbc.

**Use jdbc connector**

```scala
libraryDependencies += "@ORGANIZATION@" %% "jdbc-connector" % "@VERSION@"
```

**Use ldbc connector**

```scala
libraryDependencies += "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@"
```

For Cross-Platform projects (JVM, JS, and/or Native)

```scala
libraryDependencies += "@ORGANIZATION@" %%% "ldbc-connector" % "@VERSION@"
```

### Usage

The difference in usage is that there are differences in the way connections are built between jdbc and ldbc.

> **ldbc** is currently under active development. Please note that current functionality may therefore be deprecated or changed in the future.

**jdbc connector**

```scala
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(3306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")

val datasource = jdbc.connector.MysqlDataSource[IO](ds)

val connection: Resource[IO, Connection[IO]] =
  Resource.make(datasource.getConnection)(_.close())
```

**ldbc connector**

```scala
val connection: Resource[IO, Connection[IO]] =
  ldbc.connector.Connection[IO](
    host     = "127.0.0.1",
    port     = 3306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("ldbc"),
    ssl      = SSL.Trusted
  )
```

The connection process to the database can be carried out using the connections established by each of these methods.

```scala
val result: IO[(List[Int], Option[Int], Int)] = connection.use { conn =>
  (for
    result1 <- sql"SELECT 1".toList[Int]
    result2 <- sql"SELECT 2".headOption[Int]
    result3 <- sql"SELECT 3".unsafe[Int]
  yield (result1, result2, result3)).readOnly(conn)
}
```

#### Using the query builder

ldbc provides not only plain queries but also type-safe database connections using the query builder.

The first step is to set up dependencies.

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-query-builder" % "${version}"
```

For Cross-Platform projects (JVM, JS, and/or Native):

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-query-builder" % "${version}"
```

ldbc uses classes to construct queries.

```scala
import ldbc.query.builder.Table

case class User(
  id: Long,
  name: String,
  age: Option[Int],
) derives Table
```

The next step is to create a Table using the classes you have created.

```scala
import ldbc.query.builder.Table

val userTable = Table[User]
```

Finally, you can use the query builder to create a query.

```scala
val result: IO[List[User]] = connection.use { conn =>
  userTable.selectAll.query.to[List].readOnly(conn)
  // "SELECT `id`, `name`, `age` FROM user"
}
```

#### Using the schema

ldbc also allows type-safe construction of schema information for tables.

The first step is to set up dependencies.

```scala
libraryDependencies += "@ORGANIZATION@" %% "ldbc-schema" % "@VERSION@"
```

For Cross-Platform projects (JVM, JS, and/or Native):

```scala
libraryDependencies += "@ORGANIZATION@" %%% "ldbc-schema" % "@VERSION@"
```

The next step is to create a schema for use by the query builder.

ldbc maintains a one-to-one mapping between Scala models and database table definitions. The mapping between the properties held by the model and the columns held by the table is done in definition order. Table definitions are very similar to the structure of Create statements. This makes the construction of table definitions intuitive for the user.

```scala
import ldbc.schema.*

case class User(
  id: Long,
  name: String,
  age: Option[Int],
)

val userTable = Table[User]("user")(                 // CREATE TABLE `user` (
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY), //   `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  column("name", VARCHAR(255)),                      //   `name` VARCHAR(255) NOT NULL,
  column("age", INT.UNSIGNED.DEFAULT(None)),         //   `age` INT unsigned DEFAULT NULL
)                                                    // )
```

Finally, you can use the query builder to create a query.

```scala
val result: IO[List[User]] = connection.use { conn =>
  userTable.selectAll.query.to[List].readOnly(conn)
  // "SELECT `id`, `name`, `age` FROM user"
}
```

## Documentation

Full documentation can be found at Currently available in English and Japanese.

- [English](en/index.md)
- [Japanese](ja/index.md)

## Contributing

All suggestions welcome :)!

If you’d like to contribute, see the list of [issues](https://github.com/takapi327/ldbc/issues) and pick one! Or report your own. If you have an idea you’d like to discuss, that’s always a good option.

If you have any questions about why or how it works, feel free to ask on github. This probably means that the documentation, scaladocs, and code are unclear and can be improved for the benefit of all.

### Testing locally

If you want to build and run the tests for yourself, you'll need a local MySQL database. The easiest way to do this is to run `docker-compose up` from the project root.
