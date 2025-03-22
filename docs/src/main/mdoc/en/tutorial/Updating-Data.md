{%
  laika.title = Data Update
  laika.metadata.language = en
%}

# Data Update

This chapter describes operations to modify data in the database and how to retrieve the updated results.

## Insert

Insert is simple and works just like select. Here we define a method to create an `Executor` that will insert rows into the `user` table.

```scala
def insertUser(name: String, email: String): Executor[IO, Int] =
  sql"INSERT INTO user (name, email) VALUES ($name, $email)"
    .update
```

Let's insert a line.

```scala
insertUser("dave", "dave@example.com").commit.unsafeRunSync()
```

And then read it back.

```scala
sql"SELECT * FROM user"
  .query[(Int, String, String)] // Query[IO, (Int, String, String)]
  .to[List]                     // Executor[IO, List[(Int, String, String)]]
  .readOnly(conn)               // IO[List[(Int, String, String)]]
  .unsafeRunSync()              // List[(Int, String, String)]
  .foreach(println)             // Unit
```

## Update

The same pattern applies to updates. Here we update the user's email address.

```scala
def updateUserEmail(id: Int, email: String): Executor[IO, Int] =
  sql"UPDATE user SET email = $email WHERE id = $id"
    .update
```

Getting Results

```scala
updateUserEmail(1, "alice+1@example.com").commit.unsafeRunSync()

sql"SELECT * FROM user WHERE id = 1"
  .query[(Int, String, String)] // Query[IO, (Int, String, String)]
  .to[Option]                   // Executor[IO, List[(Int, String, String)]]
  .readOnly(conn)               // IO[List[(Int, String, String)]]
  .unsafeRunSync()              // List[(Int, String, String)]
  .foreach(println)             // Unit
// Some((1,alice,alice+1@example.com))
```

## Auto-generated keys

When inserting, we want to return the newly generated key. We do this the hard way, first by inserting and getting the last generated key with `LAST_INSERT_ID` and then selecting the specified row.

```scala 3
def insertUser(name: String, email: String): Executor[IO, (Int, String, String)] =
  for
    _    <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".update
    id   <- sql"SELECT LAST_INSERT_ID()".query[Int].unsafe
    task <- sql"SELECT * FROM user WHERE id = $id".query[(Int, String, String)].to[Option]
  yield task
```

```scala
insertUser("eve", "eve@example.com").commit.unsafeRunSync()
```

This is frustrating, but supported by all databases (although the “get last used ID” feature varies from vendor to vendor).

In MySQL, only rows with `AUTO_INCREMENT` set can be returned on insert. The above operation can be reduced to two statements

If you are inserting rows using an auto-generated key, you can use the `returning` method to retrieve the auto-generated key.

```scala 3
def insertUser(name: String, email: String): Executor[IO, (Int, String, String)] =
  for
    id   <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".returning[Int]
    user <- sql"SELECT * FROM user WHERE id = $id".query[(Int, String, String)].to[Option]
  yield user
```

```scala
insertUser("frank", "frank@example.com").commit.unsafeRunSync()
```

## Batch update

To perform batch updates, define an `insertManyUser` method that inserts multiple rows using `NonEmptyList`.

```scala
def insertManyUser(users: NonEmptyList[(String, String)]): Executor[IO, Int] =
  val value = users.map { case (name, email) => sql"($name, $email)" }
  (sql"INSERT INTO user (name, email) VALUES" ++ values(value)).update
```

Running this program gives the updated row count.

```scala
val users = NonEmptyList.of(
  ("greg", "greg@example.com"),
  ("henry", "henry@example.com")
)

insertManyUser(users).commit.unsafeRunSync()
```
