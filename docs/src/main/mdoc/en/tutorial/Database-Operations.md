{%
  laika.title = Database Operation
  laika.metadata.language = en
%}

# Database Operation

This section describes database operations.

Before making a database connection, you need to configure settings such as commit timing and read/write-only.

## Read Only

Use the `readOnly` method to initiate a read-only transaction.

The `readOnly` method can be used to make the processing of a query to be executed read-only. The `readOnly` method can also be used with `insert/update/delete` statements, but it will result in an error at runtime because of the write operation.

```scala
val read = sql"SELECT 1".query[Int].to[Option].readOnly(connection)
```

## Writing

To write, use the `commit` method.

The `commit` method can be used to set up the processing of a query to be committed at each query execution.

```scala
val write = sql"INSERT INTO `table`(`c1`, `c2`) VALUES ('column 1', 'column 2')".update.commit(connection)
```

## Transaction

Use the `transaction` method to initiate a transaction.

The `transaction` method can be used to combine multiple database connection operations into a single transaction.

ldbc will build a process to connect to the database in the form of `Executor[F, A]`. Since Executor is a monad, two small programs can be combined into one large program using for comprehensions.

```scala 3
val program: Executor[IO, (List[Int], Option[Int], Int)] =
  for
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
    result3 <- sql"SELECT 3".query[Int].unsafe
  yield (result1, result2, result3)
```

The `transaction` method can be used to combine `Executor` programs into a single transaction.

```scala
val transaction = program.transaction(connection)
```
