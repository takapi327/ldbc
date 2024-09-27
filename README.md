# ldbc (Lepus Database Connectivity)

<div align="center">
  <img alt="ldbc" src="./lepus_logo.png">
</div>

[![Continuous Integration](https://github.com/takapi327/ldbc/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/takapi327/ldbc/actions/workflows/ci.yml)
[![MIT License](https://img.shields.io/badge/license-MIT-green)](https://en.wikipedia.org/wiki/MIT_License)
[![Scala Version](https://img.shields.io/badge/scala-v3.3.x-red)](https://github.com/lampepfl/dotty)
[![Typelevel Affiliate Project](https://img.shields.io/badge/typelevel-affiliate%20project-FF6169.svg)](https://typelevel.org/projects/affiliate/)
[![javadoc](https://javadoc.io/badge2/io.github.takapi327/ldbc-dsl_3/javadoc.svg)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)
[![Maven Central Version](https://maven-badges.herokuapp.com/maven-central/io.github.takapi327/ldbc-dsl_3/badge.svg?color=blue)](https://search.maven.org/artifact/io.github.takapi327/ldbc-dsl_3/0.3.0-beta7/jar)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-dsl/latest-by-scala-version.svg?color=blue)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-dsl/latest-by-scala-version.svg?color=blue&targetType=js)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-dsl/latest-by-scala-version.svg?color=blue&targetType=native)](https://index.scala-lang.org/takapi327/ldbc)

ldbc (Lepus Database Connectivity) is Pure functional JDBC layer with Cats Effect 3 and Scala 3.

ldbc is a [Typelevel](http://typelevel.org/) project. This means we embrace pure, typeful, functional programming, and provide a safe and friendly environment for teaching, learning, and contributing as described in the Scala [Code of Conduct](http://scala-lang.org/conduct.html).

Note that **ldbc** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

> [!NOTE]
> **ldbc** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

Please drop a :star: if this project interests you. I need encouragement.

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
libraryDependencies += "io.github.takapi327" %% "ldbc-dsl" % "latest"
```

For Cross-Platform projects (JVM, JS, and/or Native):

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl" % "latest"
```

The dependency package used depends on whether the database connection is made via a connector using the Java API or a connector provided by ldbc.

**Use jdbc connector**

```scala
libraryDependencies += "io.github.takapi327" %% "jdbc-connector" % "latest"
```

**Use ldbc connector**

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-connector" % "latest"
```

For Cross-Platform projects (JVM, JS, and/or Native)

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-connector" % "latest"
```

### Usage

The difference in usage is that there are differences in the way connections are built between jdbc and ldbc.

> [!CAUTION]
> **ldbc** is currently under active development. Please note that current functionality may therefore be deprecated or changed in the future.

**jdbc connector**

```scala
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
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
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
    result3 <- sql"SELECT 3".query[Int].unsafe
  yield (result1, result2, result3)).readOnly(conn)
}
```

#### Using the query builder

ldbc provides not only plain queries but also type-safe database connections using the query builder.

The first step is to set up dependencies.

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-query-builder" % "latest"
```

For Cross-Platform projects (JVM, JS, and/or Native):

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-query-builder" % "latest"
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
  userTable.selectAll.query[User].to[List].readOnly(conn)
  // "SELECT `id`, `name`, `age` FROM user"
}
```

#### Using the schema

ldbc also allows type-safe construction of schema information for tables.

The first step is to set up dependencies.

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-schema" % "latest"
```

For Cross-Platform projects (JVM, JS, and/or Native):

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-schema" % "latest"
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
  userTable.selectAll.query[User].to[List].readOnly(conn)
  // "SELECT `id`, `name`, `age` FROM user"
}
```

## Documentation

Full documentation can be found at Currently available in English and Japanese.

- [English](https://takapi327.github.io/ldbc/en/)
- [Japanese](https://takapi327.github.io/ldbc/ja/)

## Features/Roadmap

Creating a MySQL connector project written in pure Scala3.

JVM, JS and Native platforms are all supported.

> [!IMPORTANT]
> **ldbc** is currently focused on developing connectors written in pure Scala3 to work with JVM, JS and Native.
> In the future, we also plan to rewrite existing functions based on a pure Scala3 connector.

### Enhanced functionality and improved stability of the MySQL connector written in pure Scala3

Most of the jdbc functionality used in other packages of ldbc at the moment could be implemented.

However, not all jdbc APIs could be supported. Nor can we guarantee that it is proven and stable enough to operate in a production environment.

We will continue to develop features and improve the stability of the ldbc connector to achieve the same level of stability and reliability as the jdbc connector.

#### Connection pooling implementation

- [ ] Failover Countermeasures

#### Performance Verification

- [ ] Comparison with JDBC
- [ ] Comparison with other MySQL Scala libraries
- [ ] Verification of operation in AWS and other infrastructure environments

#### Other

- [ ] Additional streaming implementation
- [ ] Integration with java.sql API
- [ ] etc...

### Redesign of query builders and schema definitions

Initially, ldbc was inspired by tapir to create a development system that could centralise Scala models, sql schemas and documentation by managing a single resource at the database level.

In addition, database connection, query construction and document generation were to be used in combination with retrofitted packages, as the aim was to be able to integrate with other database systems.

As a result, we feel that it has become difficult for users to use because of the various configurations required to build it.

What users originally wanted from a database connectivity library was something simpler, easier and more intuitive to use.

Initially, ldbc aimed to create documentation from the schema, so building the schema and query builder was not as simple as it could have been, as it required a complete description of the database data types and so on.

It was therefore decided to redesign it to make it simpler and easier to use.

## Contributing

All suggestions welcome :)!

If you’d like to contribute, see the list of [issues](https://github.com/takapi327/ldbc/issues) and pick one! Or report your own. If you have an idea you’d like to discuss, that’s always a good option.

If you have any questions about why or how it works, feel free to ask on github. This probably means that the documentation, scaladocs, and code are unclear and can be improved for the benefit of all.

### Testing locally

If you want to build and run the tests for yourself, you'll need a local MySQL database. The easiest way to do this is to run `docker-compose up` from the project root.
