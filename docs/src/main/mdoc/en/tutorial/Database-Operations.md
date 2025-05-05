{%
  laika.title = Database Operations
  laika.metadata.language = en
%}

# Database Operations

In [Updating Data](/en/tutorial/Updating-Data.md), you learned how to insert, update, and delete data. This page explains more advanced database operations such as transaction management and commit timing.

Transaction management is important for safely performing database operations. ldbc provides abstractions for transaction management that adhere to the principles of functional programming.

This section explains database operations.

Before connecting to a database, you need to configure settings such as commit timing and read/write modes.

## Read-Only

To start a read-only transaction, use the `readOnly` method.

Using the `readOnly` method allows you to set query processing to read-only mode. You can use the `readOnly` method with `insert/update/delete` statements as well, but they will result in errors at execution time since they perform write operations.

```scala
val read = sql"SELECT 1".query[Int].to[Option].readOnly(connection)
```

## Writing

To write to the database, use the `commit` method.

Using the `commit` method configures your query processing to commit after each query execution.

```scala
val write = sql"INSERT INTO `table`(`c1`, `c2`) VALUES ('column 1', 'column 2')".update.commit(connection)
```

## Transactions

To start a transaction, use the `transaction` method.

Using the `transaction` method allows you to group multiple database connection operations into a single transaction.

With ldbc, you'll organize database connection processes in the form of `DBIO[A]`. DBIO is a monad, so you can use for-comprehension to combine smaller programs into a larger program.

```scala 3
val program: DBIO[(List[Int], Option[Int], Int)] =
  for
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
    result3 <- sql"SELECT 3".query[Int].unsafe
  yield (result1, result2, result3)
```

Once you have combined operations into a single `DBIO` program, you can process them in a single transaction using the `transaction` method.

```scala
val transaction = program.transaction(connection)
```

## Next Steps

You've now learned all the basic uses of ldbc. You've acquired the knowledge necessary for everyday database operations, including database connections, query execution, reading and writing data, and transaction management.

From here, we'll move on to more advanced topics. Let's start with [Error Handling](/en/tutorial/Error-Handling.md) and learn how to properly handle exceptions that may occur in database operations.
