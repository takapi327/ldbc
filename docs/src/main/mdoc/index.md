# ldbc (Lepus Database Connectivity)

<div align="center">
  <img alt="LDBC" src="./img/lepus_logo.png">
</div>

[![Continuous Integration](https://github.com/takapi327/ldbc/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/takapi327/ldbc/actions/workflows/ci.yml)
[![MIT License](https://img.shields.io/badge/license-MIT-green)](https://en.wikipedia.org/wiki/MIT_License)
[![Scala Version](https://img.shields.io/badge/scala-v3.3.x-red)](https://github.com/lampepfl/dotty)
[![Typelevel Affiliate Project](https://img.shields.io/badge/typelevel-affiliate%20project-FF6169.svg)](https://typelevel.org/projects/affiliate/)
[![javadoc](https://javadoc.io/badge2/io.github.takapi327/ldbc-core_3/javadoc.svg)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)
[![Maven Central Version](https://maven-badges.herokuapp.com/maven-central/io.github.takapi327/ldbc-core_3/badge.svg?color=blue)](https://search.maven.org/artifact/io.github.takapi327/ldbc-core_3/0.3.0-beta2/jar)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-core/latest-by-scala-version.svg?color=blue)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-core/latest-by-scala-version.svg?color=blue&targetType=js)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-core/latest-by-scala-version.svg?color=blue&targetType=native)](https://index.scala-lang.org/takapi327/ldbc)

ldbc (Lepus Database Connectivity) is Pure functional JDBC layer with Cats Effect 3 and Scala 3.

ldbc is Created under the influence of [tapir](https://github.com/softwaremill/tapir), a declarative, type-safe web endpoint library. Using tapir, you can build type-safe endpoints and also generate OpenAPI documentation from the endpoints you build.

ldbc allows the same type-safe construction with Scala at the database layer and document generation using the constructed one.

Note that **ldbc** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

## Modules availability

ldbc is available on the JVM, Scala.js, and ScalaNative

|  Module / Platform   | JVM | Scala Native | Scala.js |  
|:--------------------:|:---:|:------------:|:--------:|
|     `ldbc-core`      |  ✅  |      ✅       |    ✅     |
|      `ldbc-sql`      |  ✅  |      ✅       |    ✅     |
| `ldbc-query-builder` |  ✅  |      ✅       |    ✅     |
|      `ldbc-dsl`      |  ✅  |      ✅       |    ✅     | 
|   `ldbc-schemaSpy`   |  ✅  |      ❌       |    ❌     | 
|    `ldbc-codegen`    |  ✅  |      ✅       |    ✅     |
|    `ldbc-hikari`     |  ✅  |      ❌       |    ❌     | 
|    `ldbc-plugin`     |  ✅  |      ❌       |    ❌     | 
|   `ldbc-connector`   |  ✅  |      ✅       |    ✅     | 
|   `jdbc-connector`   |  ✅  |      ❌       |    ❌     | 

## Quick Start

For people that want to skip the explanations and see it action, this is the place to start!

### Dependency Configuration

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-dsl" % "${version}"
```

For Cross-Platform projects (JVM, JS, and/or Native):

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl" % "${version}"
```

The dependency package used depends on whether the database connection is made via a connector using the Java API or a connector provided by ldbc.

**Use jdbc connector**

```scala
libraryDependencies += "io.github.takapi327" %% "jdbc-connector" % "${version}"
```

**Use ldbc connector**

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-connector" % "${version}"
```

For Cross-Platform projects (JVM, JS, and/or Native)

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-connector" % "${version}"
```

### Usage

The difference in usage is that there are differences in the way connections are built between jdbc and ldbc.

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
    result1 <- sql"SELECT 1".toList[Int]
    result2 <- sql"SELECT 2".headOption[Int]
    result3 <- sql"SELECT 3".unsafe[Int]
  yield (result1, result2, result3)).run(conn)
}
```

## Documentation

Full documentation can be found at Currently available in English and Japanese.

- [English](/ldbc/en/index.html)
- [Japanese](/ldbc/ja/index.html)

## Contributing

All suggestions welcome :)!

If you’d like to contribute, see the list of [issues](https://github.com/takapi327/ldbc/issues) and pick one! Or report your own. If you have an idea you’d like to discuss, that’s always a good option.

If you have any questions about why or how it works, feel free to ask on github. This probably means that the documentation, scaladocs, and code are unclear and can be improved for the benefit of all.

### Testing locally

If you want to build and run the tests for yourself, you'll need a local MySQL database. The easiest way to do this is to run `docker-compose up` from the project root.
