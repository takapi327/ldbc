{%
  laika.title = Simple Program
  laika.metadata.language = en
%}

# Simple Program

In this section, we first explain the basic usage of ldbc by creating and executing a simple program.

※ The program environment used here is assumed to be the one built in the setup.

## The first program

In this program, we will create a program that connects to a database and retrieves the results of a calculation.

Now, let's create a query that asks the database to calculate a constant using `sql string interpolator`.

```scala
val program: Executor[IO, Option[Int]] = sql"SELECT 2".query[Int].to[Option]
```

Queries created with `sql string interpolator` use the `query` method to determine the type to retrieve. Here, `query[Int]` is used to get the `Int` type. Also, the `to` method determines the type to retrieve. Here, `to[Option]` is used to get the `Option` type.

| Method       | Return Type    | Notes                                               |
|--------------|----------------|-----------------------------------------------------|
| `to[List]`   | `F[List[A]]`   | `View all results in a list`                        |
| `to[Option]` | `F[Option[A]]` | `Result is 0 or 1, otherwise an error is generated` |
| `unsafe`     | `F[A]`         | `Exactly one result, otherwise an error will occur` |

Finally, write a program that connects to the database and returns a value. This program connects to the database, executes the query, and retrieves the results.

```scala
connection
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

Connected to database to calculate constants. Quite impressive.

**Execute with Scala CLI**

This program can also be run using the Scala CLI. The following command will execute this program.

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/02-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## Second program

What if you want to do multiple things in one transaction? Easy! Since Executor is a monad, you can use for comprehensions to make two small programs into one big program.

```scala 3
val program: Executor[IO, (List[Int], Option[Int], Int)] =
  for
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
    result3 <- sql"SELECT 3".query[Int].unsafe
  yield (result1, result2, result3)
```

Finally, write a program that connects to the database and returns a value. This program connects to the database, executes the query, and retrieves the results.

```scala
connection
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

**Run with Scala CLI**.

This program can also be run using the Scala CLI. The following command will execute this program.

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/03-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## Third program

Let's write a program that writes to a database. Here, we connect to the database, execute a query, and insert data.

```scala
val program: Executor[IO, Int] =
  sql"INSERT INTO user (name, email) VALUES ('Carol', 'carol@example.com')".update
```

The difference from the previous step is that the `commit` method is called. This commits the transaction and inserts the data into the database.

```scala
connection
  .use { conn =>
    program.commit(conn).map(println(_))
  }
  .unsafeRunSync()
```

**Execute with Scala CLI**.

This program can also be run using the Scala CLI. The following command will execute this program.

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/04-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```
