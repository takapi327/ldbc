{%
  laika.title = Introduction
  laika.metadata.language = en
%}

# ldbc Tutorial

ldbc is a type-safe MySQL database client written in Scala 3. It allows you to write SQL safely and concisely while leveraging Scala's powerful type system to reduce database operation errors.

## Key Features of ldbc

- **Type Safety**: Detects SQL query issues at compile time
- **Concise Syntax**: Intuitive query writing using SQL string interpolation
- **Cats Effect Integration**: Supports pure functional programming
- **Schema Auto-generation**: Generate Scala code from SQL
- **Query Builder**: Build type-safe queries without writing SQL directly

## Quick Start

Let's first take a look at the basic usage of ldbc. Here's a simple example of retrieving user information from the database and adding a new user:

```scala 3
import cats.effect.*
import cats.syntax.all.*
import ldbc.connector.*
import ldbc.dsl.*

// Database connection configuration
val provider =
  ConnectionProvider
    .default[IO]("127.0.0.1", 3306, "ldbc", "password", "ldbc")
    .setSSL(SSL.Trusted)

// Executing queries
val program = for
  // Retrieving user list
  users <- sql"SELECT id, name FROM user".query[(Int, String)].to[List]
  
  // Adding a new user
  _ <- sql"INSERT INTO user (name, email) VALUES ('Taro Yamada', 'yamada@example.com')".update
  
  // Checking user count after update
  count <- sql"SELECT COUNT(*) FROM user".query[Int].unsafe
yield (users, count)

// Executing the program
provider.use { conn =>
  program.transaction(conn)
}
```

## How to Proceed with this Tutorial

This tutorial series is structured to help you learn ldbc step by step. We recommend proceeding in the following order:

### 1. Setup

First, let's prepare the environment for using ldbc.

- [Setup](/en/tutorial/Setup.md) - Preparing development environment and database
- [Connection](/en/tutorial/Connection.md) - How to connect to a database

### 2. Basic Operations

Next, let's learn frequently used features for everyday tasks.

- [Simple Program](/en/tutorial/Simple-Program.md) - Execute your first simple queries
- [Parameters](/en/tutorial/Parameterized-Queries.md) - Queries with parameters
- [Selecting Data](/en/tutorial/Selecting-Data.md) - Retrieving data with SELECT statements
- [Updating Data](/en/tutorial/Updating-Data.md) - INSERT/UPDATE/DELETE operations
- [Database Operations](/en/tutorial/Database-Operations.md) - Transaction management

### 3. Advanced Operations

After understanding the basics, let's move on to more advanced features.

- [Error Handling](/en/tutorial/Error-Handling.md) - Exception handling
- [Logging](/en/tutorial/Logging.md) - Query logging
- [Custom Data Types](/en/tutorial/Custom-Data-Type.md) - Support for custom data types
- [Query Builder](/en/tutorial/Query-Builder.md) - Type-safe query construction
- [Schema](/en/tutorial/Schema.md) - Table definitions
- [Schema Code Generation](/en/tutorial/Schema-Code-Generation.md) - Code generation from SQL

## Why Choose ldbc?

- **Low Learning Curve**: String interpolation that leverages your existing SQL knowledge
- **Safety**: Early bug detection through compile-time type checking
- **Productivity**: Reduction of boilerplate code and automatic code generation
- **Performance**: Optimized MySQL connection management
- **Benefits of Functional Programming**: Composability through Cats Effect integration

Now, let's start with [Setup](/en/tutorial/Setup.md)!

## Detailed Navigation

@:navigationTree {
  entries = [ { target = "/en/tutorial", depth = 2 } ]
}
