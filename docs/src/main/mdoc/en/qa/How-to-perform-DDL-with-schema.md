{%
  laika.title = "Q: How to perform DDL with schema?"
  laika.metadata.language = en
%}

# Q: How to perform DDL with schema?

## A: To generate and execute DDL from a schema, use the `schema` method of TableQuery.  
The `schema` method is a convenient mechanism that automatically generates DDL statements such as CREATE, DROP, TRUNCATE, etc. from table definitions.  
The following sample code demonstrates how to generate DDL for `UserTable` and execute table creation, data deletion, and table deletion in sequence.

```scala 3
// ...existing code...
// Example: DDL operations for UserTable

// Assuming UserTable is defined
val userSchema = TableQuery[UserTable].schema

// Examples of generating DDL statements
val createDDL   = userSchema.createIfNotExists.statements
val dropDDL     = userSchema.dropIfExists.statements
val truncateDDL = userSchema.truncate.statements

// If you want to check the generated DDL statements
createDDL.foreach(println)    // CREATE TABLE statement will be output
dropDDL.foreach(println)      // DROP TABLE statement will be output
truncateDDL.foreach(println)  // TRUNCATE TABLE statement will be output

// Example of executing DDL operations
DBIO.sequence(
    userSchema.createIfNotExists,
    userSchema.truncate,
    userSchema.dropIfExists
  )
  .commit(connector)
```

The code above implements operations that create tables while checking for their existence, reset table data as needed, and then delete the tables themselves.

## References
- [Schema Definition Details](/en/tutorial/Schema.md)  
- [Database Operations](/en/tutorial/Database-Operations.md)
