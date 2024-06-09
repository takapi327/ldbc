# Type-safe Query Construction

This chapter describes how to use LDBC-built table definitions to construct type-safe queries.

The following dependencies must be set up for the project

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-query-builder" % "$version$"
```
@@@

If you have not yet read how to define tables in LDBC, we recommend that you read the chapter [Table Definitions](/ldbc/en/01-Table-Definitions.html) first.

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

val userQuery = TableQuery[User](table)
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

### GROUP BY/Having

A type-safe way to set a Group By clause in a query is to use the `groupBy` method.

Using `groupBy` allows you to group data based on the value of a column name you specify when retrieving data with `select`.

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).groupBy(_._3)

select.statement === "SELECT `id`, `name`, `age` FROM user GROUP BY age"
```

When grouping, the number of data that can be retrieved with `select` is the number of groups. So, when grouping, you can retrieve the values of the columns specified for grouping, or the results of aggregating the column values by group using the provided functions.

The `having` allows you to set the conditions for retrieval with respect to data grouped and retrieved by `groupBy`.

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).groupBy(_._3).having(_._3 > 20)

select.statement === "SELECT `id`, `name`, `age` FROM user GROUP BY age HAVING age > ?"
```

### ORDER BY

A type-safe way to set an ORDER BY clause in a query is to use the `orderBy` method.

Using `orderBy` allows you to get the results sorted by the values of the columns you specify when retrieving data with `select`.

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).orderBy(_.age)

select.statement === "SELECT `id`, `name`, `age` FROM user ORDER BY age"
```

If you want to specify ascending/descending order, simply call `asc`/`desc` for the columns, respectively.

```scala 3
val desc = userQuery.select(user => (user.id, user.name, user.age)).orderBy(_.age.desc)

desc.statement === "SELECT `id`, `name`, `age` FROM user ORDER BY age DESC"

val asc = userQuery.select(user => (user.id, user.name, user.age)).orderBy(_.age.asc)

asc.statement === "SELECT `id`, `name`, `age` FROM user ORDER BY age ASC"
```

### LIMIT/OFFSET

A type-safe way to set the LIMIT and OFFSET clauses in a query is to use the `limit`/`offset` methods.

The `limit` can be set to the maximum number of rows of data to retrieve when `select` is executed, and the `offset` can be set to the number of rows of data to retrieve.

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).limit(100).offset(50)

select.statement === "SELECT `id`, `name`, `age` FROM user LIMIT ? OFFSET ?"
```

## JOIN/LEFT JOIN/RIGHT JOIN

A type-safe way to set a Join on a query is to use the `join`/`leftJoin`/`rightJoin` methods.

The following definition is used as a sample for Join.

```scala 3
case class Country(code: String, name: String)
object Country:
  val table = Table[Country]("country")(
    column("code", CHAR(3), PRIMARY_KEY),
    column("name", VARCHAR(255))
  )

case class City(id: Long, name: String, countryCode: String)
object City:
  val table = Table[City]("city")(
    column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
    column("name", VARCHAR(255)),
    column("country_code", CHAR(3))
  )

case class CountryLanguage(
  countryCode: String,
  language:    String
)
object CountryLanguage:
  val table: Table[CountryLanguage] = Table[CountryLanguage]("country_language")(
    column("country_code", CHAR(3)),
    column("language", CHAR(30))
  )

val countryQuery = TableQuery[Country](Country.table)
val cityQuery = TableQuery[City](City.table)
val countryLanguageQuery = TableQuery[CountryLanguage](CountryLanguage.table)
```

If you want to do a simple Join first, use `join`.
The first argument of `join` is the table to be joined, and the second argument is a function that compares the source table with the columns of the table to be joined. This corresponds to the ON clause in Join.

After the join, the `select` will specify columns from the two tables.

```scala 3
val join = countryQuery.join(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country JOIN city ON country.code = city.country_code"
```

Next, if you want to perform a Left Join, which is a left outer join, use `leftJoin`.
The implementation itself is the same as for a simple Join, only `join` is changed to `leftJoin`.

```scala 3
val leftJoin = countryQuery.leftJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country LEFT JOIN city ON country.code = city.country_code"
```

The difference from a simple Join is that when using `leftJoin`, the records retrieved from the table to be joined may be NULL.

Therefore, in LDBC, all records in the column retrieved from the table passed to `leftJoin` will be of type Option.

```scala 3
val leftJoin = countryQuery.leftJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name)) // (String, Option[String])
```

Next, if you want to perform a Right Join, which is a right outer join, use `rightJoin`.
The implementation itself is the same as that of simple Join, only `join` is changed to `rightJoin`.

```scala 3
val rightJoin = countryQuery.rightJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country RIGHT JOIN city ON country.code = city.country_code"
```

The difference from a simple Join is that when using `rightJoin`, the records retrieved from the join source table may be NULL.

Therefore, in LDBC, all records of a column retrieved from a join source table using `rightJoin` are of type Option.

```scala 3
val rightJoin = countryQuery.rightJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name)) // (Option[String], String)
```

If multiple joins are desired, this can be accomplished by calling any Join method in the method chain.

```scala 3
val join = 
  (countryQuery join cityQuery)((country, city) => country.code === city.countryCode)
    .rightJoin(countryLanguageQuery)((_, city, countryLanguage) => city.countryCode === countryLanguage.countryCode)
    .select((country, city, countryLanguage) => (country.name, city.name, countryLanguage.language)) // (Option[String], Option[String], String)]

join.statement =
  """
    |SELECT
    |  country.`name`, 
    |  city.`name`,
    |  country_language.`language`
    |FROM country
    |JOIN city ON country.code = city.country_code
    |RIGHT JOIN country_language ON city.country_code = country_language.country_code
    |""".stripMargin
```

Note that a `rightJoin` join with multiple joins will result in NULL-acceptable access to all records retrieved from the previously joined table, regardless of what the previous join was.

## Custom Data Type

In the previous section, we used the `mapping` method of DataType to map custom types to DataType in order to use user-specific or unsupported types. ([reference](/ldbc/en/02-Custom-Data-Type.html))

LDBC separates the table definition from the process of connecting to the database.
Therefore, if you want to retrieve data from the database and convert it to a user-specific or unsupported type, you must link the method of retrieving data from the ResultSet to the user-specific or unsupported type.

For example, if you want to map a user-defined Enum to a string type

```scala 3
enum Custom:
  case ...

given ResultSetReader[IO, Custom] =
  ResultSetReader.mapping[IO, str, Custom](str => Custom.valueOf(str))
```

※ This process may be integrated with DataType mapping in a future version.

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

### ON DUPLICATE KEY UPDATE

Inserting a row with an ON DUPLICATE KEY UPDATE clause will cause an UPDATE of the old row if the UNIQUE index or PRIMARY KEY has duplicate values.

There are two ways to achieve this in LDBC: using `insertOrUpdate{s}` or using `onDuplicateKeyUpdate` for `Insert`.

```scala 3
val insert = userQuery.insertOrUpdate((1L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?) AS new_user ON DUPLICATE KEY UPDATE `id` = new_user.`id`, `name` = new_user.`name`, `age` = new_user.`age`"
```

Note that if you use `insertOrUpdate{s}`, all columns will be updated. If you have duplicate values and wish to update only certain columns, use `onDuplicateKeyUpdate` to specify only the columns you wish to update.

```scala 3
val insert = userQuery.insert((1L, "name", None)).onDuplicateKeyUpdate(v => (v.name, v.age))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?) AS new_user ON DUPLICATE KEY UPDATE `name` = new_user.`name`, `age` = new_user.`age`"
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

See [where item](/ldbc/en/03-Type-safe-Query-Builder.html#where) in the Insert statement for conditions that can be used in the `where` method.

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

See [where item](/ldbc/en/03-Type-safe-Query-Builder.html#where) in the Insert statement for conditions that can be used in the `where` method.

## DDL

A type-safe way to construct DDL is to use the following methods provided by TableQuery.

- createTable
- dropTable
- truncateTable

If you are using spec2, you can run DDL before and after the test as follows.

```scala 3
import cats.effect.IO
import cats.effect.unsafe.implicits.global

import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments
import org.specs2.specification.BeforeAfterEach

object Test extends Specification, BeforeAfterEach:

  override def before: Fragments =
    step((tableQuery.createTable.update.autoCommit(dataSource) >> IO.println("Complete create table")).unsafeRunSync())

  override def after: Fragments =
    step((tableQuery.dropTable.update.autoCommit(dataSource) >> IO.println("Complete drop table")).unsafeRunSync())
```
