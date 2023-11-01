# Type-safe Query Construction

This chapter describes how to use LDBC-built table definitions to construct type-safe queries.

The following dependencies must be set up for the project

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-query-builder" % "$version$"
```
@@@

If you have not yet read how to define tables in LDBC, we recommend that you read the chapter [Table Definitions](http://localhost:4000/en/01-Table-Definitions.html) first.

The following code example assumes the following import

```scala 3
import cats.effect.IO
import ldbc.core.*
import ldbc.query.builder.TableQuery
```

LDBC performs type-safe query construction by passing table definitions to TableQuery.

```scala 3
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

val userQuery = TableQuery[IO, User](table)
```

## SELECT

A type-safe way to construct a SELECT statement is to use the `select` method provided by TableQuery, which is implemented in LDBC to mimic a plain query, making query construction intuitive. LDBC is also designed so that you can see at a glance what kind of query is being constructed.

To construct a SELECT statement that retrieves only specific columns, simply specify the columns you want to retrieve in the `select` method.

```scala 3
val select = userQuery.select(_.id)

select.statement === "SELECT `id` FROM user"
```

To specify multiple columns, simply specify the columns you wish to retrieve using the `select` method and return a tuple of the specified columns.

```scala 3
val select = userQuery.select(user => (user.id, user.name))

select.statement === "SELECT `id`, `name` FROM user"
```

If you want to specify all columns, you can construct it by using the `selectAll` method provided by TableQuery.

```scala 3
val select = userQuery.selectAll

select.statement === "SELECT `id`, `name`, `age` FROM user"
```

If you want to get the number of a specific column, you can construct it by using `count` on the specified column.

```scala 3
val select = userQuery.select(_.id.count)

select.statement === "SELECT COUNT(id) FROM user"
```

### WHERE

A type-safe way to set a Where condition in a query is to use the `where` method.

```scala 3
val select = userQuery.select(_.id).where(_.name === "Test")

select.statement === "SELECT `id` FROM user WHERE name = ?"
```

The following is a list of conditions that can be used in the `where` method.

| condition                            | statement                             |
|--------------------------------------|---------------------------------------|
| ===                                  | `column = ?`                          |
| >=                                   | `column >= ?`                         |
| >                                    | `column > ?`                          |
| <=                                   | `column <= ?`                         |
| <                                    | `column < ?`                          |
| <>                                   | `column <> ?`                         |
| !==                                  | `column != ?`                         |
| IS ("TRUE"/"FALSE"/"UNKNOWN"/"NULL") | `column IS {TRUE/FALSE/UNKNOWN/NULL}` |
| <=>                                  | `column <=> ?`                        |
| IN (value, value, ...)               | `column IN (?, ?, ...)`               |
| BETWEEN (start, end)                 | `column BETWEEN ? AND ?`              |
| LIKE (value)                         | `column LIKE ?`                       |
| LIKE_ESCAPE (like, escape)           | `column LIKE ? ESCAPE ?`              |
| REGEXP (value)                       | `column REGEXP ?`                     |
| `<<` (value)                         | `column << ?`                         |
| `>>` (value)                         | `column >> ?`                         |
| DIV (cond, result)                   | `column DIV ? = ?`                    |
| MOD (cond, result)                   | `column MOD ? = ?`                    |
| ^ (value)                            | `column ^ ?`                          |
| ~ (value)                            | `~column = ?`                         |

## INSERT

A type-safe way to construct an INSERT statement is to use the following methods provided by TableQuery.

- insert
- insertInto
- +=
- ++=

**insert**

The `insert` method is passed a tuple of data to insert. The tuples must have the same number and type of properties as the model. Also, the order of the inserted data must be in the same order as the model properties and table columns.

```scala 3
val insert = userQuery.insert((1L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?)"
```

If you want to insert multiple data, you can construct it by passing multiple tuples to the `insert` method.

```scala 3
val insert = userQuery.insert((1L, "name", None), (2L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?), (?, ?, ?)"
```

**insertInto**

The `insert` method inserts data into all columns the table has, but if you want to insert data only into specific columns, use the `insertInto` method.

This can be used, for example, to exclude data insertion into columns with AutoIncrement or Default values.

```scala 3
val insert = userQuery.insertInto(user => (user.name, user.age)).values(("name", None))

insert.statement === "INSERT INTO user (`name`, `age`) VALUES(?, ?)"
```

If you want to insert multiple data, you can construct it by passing an array of tuples to `values`.

```scala 3
val insert = userQuery.insertInto(user => (user.name, user.age)).values(List(("name", None), ("name", Some(20))))

insert.statement === "INSERT INTO user (`name`, `age`) VALUES(?, ?), (?, ?)"
```

**+=**

The `+=` method can be used to construct an INSERT statement using a model. Note that when using a model, data is inserted into all columns.

```scala 3
val insert = userQuery += User(1L, "name", None)

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?)"
```

**++=**

Use the `++=` method if you want to insert multiple data using the model.

```scala 3
val insert = userQuery ++= List(User(1L, "name", None), User(2L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?), (?, ?, ?)"
```

## UPDATE

A type-safe way to construct an UPDATE statement is to use the `update` method provided by TableQuery.

The first argument of the `update` method is the name of the model property, not the column name of the table, and the second argument is the value to be updated. The type of the value passed as the second argument must be the same as the type of the property specified in the first argument.

```scala 3
val update = userQuery.update("name", "update name")

update.statement === "UPDATE user SET name = ?"
```

If a property name that does not exist is specified as the first argument, a compile error occurs.

```scala 3
val update = userQuery.update("hoge", "update name") // Compile error
```

If you want to update multiple columns, use the `set` method.

```scala 3
val update = userQuery.update("name", "update name").set("age", Some(20))

update.statement === "UPDATE user SET name = ?, age = ?"
```

You can also prevent the `set` method from generating queries based on conditions.

```scala 3
val update = userQuery.update("name", "update name").set("age", Some(20), false)

update.statement === "UPDATE user SET name = ?"
```

You can also use a model to construct the UPDATE statement. Note that if you use a model, all columns will be updated.

```scala 3
val update = userQuery.update(User(1L, "update name", None))

update.statement === "UPDATE user SET id = ?, name = ?, age = ?"
```

### WHERE

The `where` method can also be used to set a where condition on the update statement.

```scala 3
val update = userQuery.update("name", "update name").set("age", Some(20)).where(_.id === 1)

update.statement === "UPDATE user SET name = ?, age = ? WHERE id = ?"
```

See [where item](http://localhost:4000/en/03-Type-safe-Query-Builder.html#where) in the Insert statement for conditions that can be used in the `where` method.

## DELETE

A type-safe way to construct a DELETE statement is to use the `delete` method provided by TableQuery.

```scala 3
val delete = userQuery.delete

delete.statement === "DELETE FROM user"
```

### WHERE

The `where` method can also be used to set a Where condition on a delete statement.

```scala 3
val delete = userQuery.delete.where(_.id === 1)

delete.statement === "DELETE FROM user WHERE id = ?"
```

See [where item](http://localhost:4000/en/03-Type-safe-Query-Builder.html#where) in the Insert statement for conditions that can be used in the `where` method.
