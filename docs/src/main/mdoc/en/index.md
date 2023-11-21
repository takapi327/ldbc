@@@ index
 * [Table Definitions](./01-Table-Definitions.md)
 * [Custom Data Type](./02-Custom-Data-Type.md)
 * [Type-safe Query Builder](./03-Type-safe-Query-Builder.md)
 * [Database Connection](./04-Database-Connection.md)
 * [Plain SQL Queries](./05-Plain-SQL-Queries.md)
 * [Generating SchemaSPY Documentation](./06-Generating-SchemaSPY-Documentation.md)
 * [Schema Code Generation](./07-Schema-Code-Generation.md)
@@@

# LDBC

Note that **LDBC** is pre-1.0 software and is still under active development. Newer versions may no longer be binary compatible with earlier versions.

## Introduction

Most of our application development involves the use of databases.<br>One way to access databases in Scala is to use JDBC, and there are several libraries in Scala that wrap this JDBC.

- Functional DSL (slick, quill, zio-sql)
- SQL string interpolator (Anorm, doobie)

LDBC, also a JDBC-wrapped library, is a Scala 3 library that combines aspects of each, providing a type-safe, refactorable SQL interface that can express all SQL expressions on a MySQL database.

The concept of LDBC also allows development to centralize Scala models, sql schemas, and documents by using LDBC to manage a single resource.

This concept was influenced by [tapir](https://github.com/softwaremill/tapir), a declarative, type-safe web endpoint library.<br>By using tapir, you can build type-safe endpoints and even generate OpenAPI documents from the endpoints you build.

LDBC uses Scala at the database layer to allow for the same type-safe construction and to allow documentation generation using what has been constructed.

## Why LDBC?

Development of database-based applications requires a variety of ongoing changes.

For example, what information in the tables built in the database should be handled by the application, what queries are best suited for data retrieval, etc.

Adding even a single column to a table definition requires modifying the SQL file, adding properties to the corresponding model, reflecting them in the database, updating documentation, etc.

There are many other things to consider and correct.

It is very difficult to keep up with all the maintenance during daily development, and even maintenance omissions may occur.

I think the approach of using plain SQL to retrieve data without mapping table information to the application model and then retrieving the data with a specified type is a very good way to go.

This way, there is no need to build database-specific models, because developers are free to work with data when they want to retrieve it, using the type of data they want to retrieve.<br>I also think it is very good at handling plain queries so that you can instantly see what queries are being executed.

However, this method does not eliminate document updates, etc. just because table information is no longer managed in the application.

LDBC has been developed to solve some of these problems.

- Type safety: compile-time guarantees, development-time complements, read-time information
- Declarative: Separates the form of the table definition ("What") from the database connection ("How").
- SchemaSPY Integration: Generate documents from table descriptions
- Libraries, not frameworks: can be integrated into your stack

With LDBC, database information must be managed by the application, but type safety, query construction, and document management can be centralized.

Mapping models in LDBC to table definitions is very easy.

The mapping between the properties a model has and the data types defined for its columns is also very simple. The developer simply defines the corresponding columns in the same order as the properties the model has.

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

Also, attempting to combine the wrong types will result in a compile error.

For example, passing a column of type INT to a column related to the name property of type String held by User will result in an error.

```shell
[error] -- [E007] Type Mismatch Error:
[error] 169 |    column("name", INT),
[error]     |                   ^^^
[error]     |Found:    ldbc.core.DataType.Integer[T]
[error]     |Required: ldbc.core.DataType[String]
[error]     |
[error]     |where:    T is a type variable with constraint <: Int | Long | Option[Int | Long]
```

For more information on these add-ons, see [Table Definitions](/ldbc/en/01-Table-Definitions.html).

## Quick Start

The current version is **$version$** for **Scala $scalaVersion$**.

@@@ vars
```scala
libraryDependencies ++= Seq(

  // Start with this one.
  "$org$" %% "ldbc-core" % "$version$",

  // Then add these as needed
  "$org$" %% "ldbc-dsl"           % "$version$", // Plain Query Database Connection
  "$org$" %% "ldbc-query-builder" % "$version$", // Type-safe query construction
  "$org$" %% "ldbc-schemaspy"     % "$version$", // SchemaSPY document generation
)
```
@@@

For more information on how to use the sbt plugin, please refer to this [documentation](/ldbc/en/07-Schema-Code-Generation.html).

## TODO

- JSON data type support
- SET data type support
- Geometry data type support
- Support for CHECK constraints
- Non-MySQL database support
- Streaming Support
- ZIO module support
- Integration with other database libraries
- Test Kit
- etc...
