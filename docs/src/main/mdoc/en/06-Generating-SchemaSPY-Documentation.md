# SchemaSPY Document Generation

This chapter describes how to use table definitions built in LDBC to create SchemaSPY documents.

The following dependencies must be set up for the project

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-schemaspy" % "$version$"
```
@@@

If you have not yet read how to define tables in LDBC, we recommend that you read the chapter [Table Definitions](http://localhost:4000/en/01-Table-Definitions.html) first.

The following code example assumes the following import

```scala 3
import ldbc.core.*
import ldbc.schemaspy.SchemaSpyGenerator
```

## Generated from table definitions

SchemaSPY connects to the database to obtain Meta information and table structures, and generates documents based on this information. LDBC, on the other hand, does not connect to the database, but generates SchemaSPY documents using the table structures constructed by LDBC.
Some items deviate from the documentation generated using SchemaSPY simply because it does not make a connection to the database. For example, information such as the number of records currently stored in a table cannot be displayed.

Database information is required to generate documents, and LDBC has a trait for representing database information.

A sample of database information built using `ldbc.core.Database` is shown below.

```scala 3
case class SampleLdbcDatabase(
  schemaMeta: Option[String] = None,
  catalog: Option[String] = Some("def"),
  host: String = "127.0.0.1",
  port: Int = 3306
) extends Database:

  override val databaseType: Database.Type = Database.Type.MySQL

  override val name: String = "sample_ldbc"

  override val schema: String = "sample_ldbc"

  override val character: Option[Character] = None

  override val collate: Option[Collate] = None

  override val tables = Set(
    ... // Enumerate table structures built with LDBC
  )
```

Database information can currently only be used for SchemaSPY document generation, but we plan to use it for other purposes as well in future feature enhancements.

Use `SchemaSpyGenerator` to generate SchemaSPY documents. Pass the generated database definition to the `default` method and call `generate` to generate SchemaSPY files at the file location specified in the second argument.

```scala 3
@main
def run(): Unit =
  val file = java.io.File("document")
  SchemaSpyGenerator.default(SampleLdbcDatabase(), file).generate()
```

Open the generated file `index.html` to see the SchemaSPY documentation.

## Generated from database connection

SchemaSpyGenerator also has a `connect` method. This method connects to the database and generates documents in the same way as the standard SchemaSpy generator.

```scala 3
@main
def run(): Unit =
  val file = java.io.File("document")
  SchemaSpyGenerator.connect(SampleLdbcDatabase(), "user name", "password" file).generate()
```

The process of making database connections is done in a Java-written implementation inside SchemaSpy. Note that threads are not managed by the Effect system.
