{%
  laika.title = Simple Programs
  laika.metadata.language = en
%}

# Simple Programs

Now that we've learned how to configure [connections](/en/tutorial/Connection.md), let's finally run some queries against the database. This page explains how to create basic database programs using ldbc.

In ldbc, we use the `DBIO` monad to represent a series of database operations. This powerful abstraction allows you to combine multiple queries and perform database operations while enjoying the benefits of pure functional programming.

The `DBIO` monad represents operations that will be executed using an actual database connection. This delays the execution of queries until an actual connection is established, allowing for efficient resource management.

Note: The programs used on this page assume the environment built in the setup.

## First Program: Retrieving a Simple Value

In our first program, we'll learn how to connect to the database and retrieve a simple calculation result.

First, we use the `sql string interpolator` to create a query to the database. This feature allows us to write SQL queries safely and concisely.

```scala 3
// Create an SQL query and specify the return type
val program: DBIO[Option[Int]] = sql"SELECT 2".query[Int].to[Option]
```

The above code does the following:

1. `sql"SELECT 2"`: Creates an SQL query using the SQL string interpolator
2. `.query[Int]`: Specifies that we expect a result of type `Int`
3. `.to[Option]`: Specifies that we want to get the result as an `Option[Int]` type (will be `None` if no result exists)

The `to` method allows you to specify how to retrieve results. The main methods and their return types are as follows:

| Method       | Return Type    | Notes                        |
|--------------|----------------|------------------------------|
| `to[List]`   | `F[List[A]]`   | Retrieves all results as a list |
| `to[Option]` | `F[Option[A]]` | Expects 0 or 1 result, errors if more |
| `unsafe`     | `F[A]`         | Expects exactly 1 result, errors otherwise |

Next, we execute this program. We create a Connector, run the query, and display the results:

```scala 3
// Create Connector and execute the program
val connector = Connector.fromDataSource(datasource)

program.readOnly(connector).map(println(_)).unsafeRunSync()
```

In the above code:

1. `val connector = Connector.fromDataSource(datasource)`: Creates a Connector from the datasource
2. `program.readOnly(connector)`: Executes the created query in read-only mode using the Connector
3. `.map(println(_))`: Displays the result to standard output
4. `.unsafeRunSync()`: Executes the IO effect

When you run this program, it will return the result `Some(2)` from the database.

**Execute with Scala CLI**

This program can be easily executed using Scala CLI:

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/02-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## Second Program: Combining Multiple Queries

In real-world applications, you often want to execute multiple queries in a single transaction. The `DBIO` monad makes this easy. You can use for-comprehension to combine multiple queries into one larger program:

```scala 3
// Create a program by combining multiple queries
val program: DBIO[(List[Int], Option[Int], Int)] =
  for
    result1 <- sql"SELECT 1".query[Int].to[List]    // Get results as a list
    result2 <- sql"SELECT 2".query[Int].to[Option]  // Get results as an Option
    result3 <- sql"SELECT 3".query[Int].unsafe      // Get the result directly
  yield (result1, result2, result3)
```

In the above for-comprehension, we execute three different queries in sequence and bind each result to a variable. Finally, we return the three results as a tuple.

The execution method is the same as before:

```scala 3
// Create Connector and execute the program
val connector = Connector.fromDataSource(datasource)

program.readOnly(connector).map(println(_)).unsafeRunSync()
```

The execution result will look like `(List(1), Some(2), 3)`. This is a tuple containing the results of the three queries.

**Execute with Scala CLI**

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/03-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## Third Program: Data Update Operations

In addition to reading, you can also create programs that write to the database. Here's an example of inserting a new record into a user table:

```scala 3
// Execute an INSERT statement and get the number of affected rows
val program: DBIO[Int] =
  sql"INSERT INTO user (name, email) VALUES ('Carol', 'carol@example.com')".update
```

Using the `update` method, you can execute update queries such as INSERT, UPDATE, DELETE, etc. The return value is the number of affected rows (in this case, 1).

When executing update queries, you need to use the `commit` method instead of `readOnly` to commit the transaction:

```scala 3
// Create Connector, execute the update program, and commit
val connector = Connector.fromDataSource(datasource)

program.commit(connector).map(println(_)).unsafeRunSync()
```

The `commit` method automatically enables `AutoCommit` and commits the transaction after the query execution. This ensures that changes are persisted to the database.

**Execute with Scala CLI**

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/04-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## Transaction Control

ldbc provides the following main transaction control methods:

- `readOnly(conn)`: Executes in a read-only transaction (data updates are not allowed)
- `commit(conn)`: Executes in auto-commit mode and commits changes upon success
- `rollback(conn)`: Executes with auto-commit off and rolls back at the end
- `transaction(conn)`: Executes with auto-commit off, commits on success, and rolls back on exception

For complex transactions, using the `transaction` method handles commits and rollbacks appropriately:

```scala 3
val complexProgram: DBIO[Int] = for
  _ <- sql"INSERT INTO accounts (owner, balance) VALUES ('Alice', 1000)".update
  _ <- sql"INSERT INTO accounts (owner, balance) VALUES ('Bob', 500)".update
  count <- sql"SELECT COUNT(*) FROM accounts".query[Int].unsafe
yield count

// Create Connector and execute the complex program
val connector = Connector.fromDataSource(datasource)

complexProgram.transaction(connector).map(println(_)).unsafeRunSync()
```

In this program, if either INSERT fails, both changes are rolled back. Only when both succeed are the changes committed.

## Next Steps

Now that you've learned how to execute basic queries, let's take a closer look at [Parameterized Queries](/en/tutorial/Parameterized-Queries.md) next.
