@@@ index
 * [Table Definitions](01-Table-Definitions.md)
 * [Custom types]()
 * [Connecting to a Database]()
 * [Selecting Data]()
 * [Inserting, and Updating]()
 * [Plain SQL Queries]()
 * [Generating SchemaSPY documentation]()
 * [Sbt Plugin](Sbt-Plugin.md)
@@@

# LDBC

<div align="center">
  <a href="https://en.wikipedia.org/wiki/MIT_License">
    <img src="https://img.shields.io/badge/license-MIT-green">
  </a>
  <a href="https://github.com/lampepfl/dotty">
    <img src="https://img.shields.io/badge/scala-v3.x-red">
  </a>
</div>

ldbc (Lepus Database Connectivity) is Pure functional JDBC layer with Cats Effect 3 and Scala 3.

ldbc is Created under the influence of [tapir](https://github.com/softwaremill/tapir), a declarative, type-safe web endpoint library. Using tapir, you can build type-safe endpoints and also generate OpenAPI documentation from the endpoints you build.

ldbc allows the same type-safe construction with Scala at the database layer and document generation using the constructed one.

Note that **ldbc** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

## Introduction

We use Database almost exclusively in our application development.

Application development using Database requires continuous modification of various

For example, what information in the tables built in the database should be handled by the application, what queries are best suited for data retrieval, etc.

Adding a single column to a table definition alone requires modifying the sql file, adding properties to the corresponding model, reflecting them in the database, updating the documentation, and so on.

There are many other things that need to be considered and corrected, etc.

I think it is very good to take the approach of using plain SQL to retrieve data without mapping the table information to the application model, and when retrieving the data, retrieve it with the specified type.

With this approach, there is no need to construct a database-specific model, and developers can freely handle data when they want to acquire it, using the type of data they want to acquire. It is also very good in that by handling plain queries, it is possible to instantly understand what kind of queries will be executed.

However, this approach seems to require a certain amount of knowledge of both Scala and databases.

What queries can be used to retrieve data, and what Scala types can the retrieved data be stored in?

An engineer with some experience or familiarity with Scala would have no difficulty.

However, for inexperienced engineers or those who have never been exposed to Scala, this can be very difficult.

Scala is a very advanced programming language and can be written in a variety of ways, including functional and object types. For the beginning learner, it can be overwhelming to learn Scala itself.

In such a situation, if you are told that you need some knowledge about databases....

On the contrary, there are libraries that can handle database connections as if they were Scala collections.

Using this library is also a very good approach.

However, I have seen cases where the ability to operate as if dealing with Scala collections has led to confusion between the database connection process and the calculation process within the application.

Also, Scala's collection-like behavior makes it a bit difficult to understand what kind of query is being executed.

ldbc will be developed to solve some of these problems.

We would be grateful if you could select ldbc as one of the many database library options.

## Why LDBC?

- type-safety: compile-time guarantees, develop-time completions, read-time information
- declarative: separate the shape of the table definition (the “what”), from the database connection (the “how”)
- SchemaSPY integration: generate documentation from table descriptions
- library, not a framework: integrates with your stack

Mapping models to table definitions is very easy.

The mapping between the properties a model has and the data types it defines for its columns is also very simple: the developer simply defines the corresponding columns in the same order as the properties the model has.

Also, attempting to combine the wrong types will result in a compile error.

For example, passing a column of type INT to a column related to the name property of type String held by User will result in an error.

```scala mdoc:silent
import ldbc.core.*

case class User(
  id: Long,
  name: String,
  age: Option[Int],
)

val table = Table[User]("user")(
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
)
```

See the [documentation](01-Table-Definitions.md) for more information on these add-ons.

## Quick Start

The current version is **$version$** for **Scala $scala-versions$** with

- [**cats-effect**](http://typelevel.org/cats-effect/) $catsEffectVersion$
- [**MySQL**](https://github.com/mysql/) $mysqlVersion$

@@@ vars
```scala
libraryDependencies ++= Seq(

  // Start with this one
  "com.github.takapi327" %% "ldbc-core" % "$version$",

  // And add any of these as needed
  "com.github.takapi327" %% "ldbc-dsl"           % "$version$",          // Plain query database connection
  "com.github.takapi327" %% "ldbc-query-builder" % "$version$",          // Type-safe query construction
  "com.github.takapi327" %% "ldbc-schemaspy"     % "$version$",          // SchemaSPY document generation
)
```
@@@

See this [documentation](Sbt-Plugin.md) for instructions on how to use the Sbt plugin.

## Contributing

All suggestions welcome :)!

If you’d like to contribute, see the list of [issues](https://github.com/takapi327/ldbc/issues) and pick one! Or report your own. If you have an idea you’d like to discuss, that’s always a good option.

If you have any questions about why or how it works, feel free to ask on github. This probably means that the documentation, scaladocs, and code are unclear and can be improved for the benefit of all.

### Testing

If you want to build and run the tests for yourself, you'll need a local MySQL database. The easiest way to do this is to run `docker-compose up` from the project root.

## Acknowledgments
