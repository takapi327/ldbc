{%
  laika.title = Query Builder
  laika.metadata.language = en
%}

# Query Builder

This chapter describes how to build a type-safe query.

The following dependencies must be set up in your project

```scala
//> using dep "@ORGANIZATION@::ldbc-query-builder:@VERSION@"
```

In ldbc, classes are used to construct queries.

```scala 3
import ldbc.query.builder.*

case class User(id: Int, name: String, email: String) derives Table
```

The `User` class inherits from the `Table` trace. Because the `Table` trace inherits from the `Table` class, methods of the `Table` class can be used to construct queries.

```scala
val query = Table[User]
  .select(user => (user.id, user.name, user.email))
  .where(_.email === "alice@example.com")
```

## SELECT

A type-safe way to construct a SELECT statement is to use the `select` method provided by Table. ldbc is implemented to resemble a plain query, making query construction intuitive. It is also easy to see at a glance what kind of query is being constructed.

To construct a SELECT statement that retrieves only specific columns, simply specify the columns you want to retrieve in the `select` method.

```scala
val select = Table[User].select(_.id)

select.statement === "SELECT id FROM user"
```

To specify multiple columns, simply specify the columns you wish to retrieve using the `select` method and return a tuple of the specified columns.

```scala
val select = Table[User].select(user => (user.id, user.name))

select.statement === "SELECT id, name FROM user"
```

全てのカラムを指定したい場合はTableが提供する`selectAll`メソッドを使用することで構築できます。

```scala
val select = Table[User].selectAll

select.statement === "SELECT id, name, email FROM user"
```

If you want to get the number of a specific column, you can construct it by using `count` on the specified column.　

```scala
val select = Table[User].select(_.id.count)

select.statement === "SELECT COUNT(id) FROM user"
```

### WHERE

A type-safe way to set a Where condition in a query is to use the `where` method.
    
```scala
val where = Table[User].selectAll.where(_.email === "alice@example.com")

where.statement === "SELECT id, name, email FROM user WHERE email = ?"
```

The following is a list of conditions that can be used in the `where` method.

| Conditions                             | Statement                             |
|----------------------------------------|---------------------------------------|
| `===`                                  | `column = ?`                          |
| `>=`                                   | `column >= ?`                         |
| `>`                                    | `column > ?`                          |
| `<=`                                   | `column <= ?`                         |
| `<`                                    | `column < ?`                          |
| `<>`                                   | `column <> ?`                         |
| `!==`                                  | `column != ?`                         |
| `IS ("TRUE"/"FALSE"/"UNKNOWN"/"NULL")` | `column IS {TRUE/FALSE/UNKNOWN/NULL}` |
| `<=>`                                  | `column <=> ?`                        |
| `IN (value, value, ...)`               | `column IN (?, ?, ...)`               |
| `BETWEEN (start, end)`                 | `column BETWEEN ? AND ?`              |
| `LIKE (value)`                         | `column LIKE ?`                       |
| `LIKE_ESCAPE (like, escape)`           | `column LIKE ? ESCAPE ?`              |
| `REGEXP (value)`                       | `column REGEXP ?`                     |
| `<<` (value)                           | `column << ?`                         |
| `>>` (value)                           | `column >> ?`                         |
| `DIV (cond, result)`                   | `column DIV ? = ?`                    |
| `MOD (cond, result)`                   | `column MOD ? = ?`                    |
| `^ (value)`                            | `column ^ ?`                          |
| `~ (value)`                            | `~column = ?`                         |

### GROUP BY/Having

A type-safe way to set the GROUP BY clause in a query is to use the `groupBy` method.

Using `groupBy` allows you to group data by the value of a column name you specify when you retrieve the data with `select`.

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .groupBy(_._2)

select.statement === "SELECT id, name FROM user GROUP BY name"
```

When grouping, the number of data that can be retrieved with `select` is the number of groups. So, when grouping is done, you can retrieve the values of the columns specified for grouping, or the results of aggregating the column values by group using the provided functions.

The `having` function allows you to set the conditions under which the data retrieved from the grouping by `groupBy` will be retrieved.

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .groupBy(_._2)
  .having(_._1 > 1)

select.statement === "SELECT id, name FROM user GROUP BY name HAVING id > ?"
```

### ORDER BY

A type-safe way to set the ORDER BY clause in a query is to use the `orderBy` method.

Using `orderBy` allows you to sort data in ascending or descending order by the value of a column name you specify when retrieving data with `select`.

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .orderBy(_.id)

select.statement === "SELECT id, name FROM user ORDER BY id"
```

If you want to specify ascending/descending order, simply call `asc`/`desc` for the columns, respectively.

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .orderBy(_.id.asc)

select.statement === "SELECT id, name FROM user ORDER BY id ASC"
```

### LIMIT/OFFSET

A type-safe way to set the LIMIT and OFFSET clauses in a query is to use the `limit`/`offset` methods.

Setting `limit` allows you to set an upper limit on the number of rows of data to retrieve when `select` is executed, while setting `offset` allows you to specify the number of rows of data to retrieve.

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .limit(1)
  .offset(1)
    
select.statement === "SELECT id, name FROM user LIMIT ? OFFSET ?"
```

## JOIN/LEFT JOIN/RIGHT JOIN

A type-safe way to set a Join in a query is to use the `join`/`leftJoin`/`rightJoin` methods.

The following definition is used as a sample for Join.

```scala 3
case class User(id: Int, name: String, email: String) derives Table
case class Product(id: Int, name: String, price: BigDecimal) derives Table
case class Order(
  id:        Int,
  userId:    Int,
  productId: Int,
  orderDate: LocalDateTime,
  quantity:  Int
) derives Table

val userTable    = Table[User]
val productTable = Table[Product]
val orderTable   = Table[Order]
```

First, if you want to perform a simple join, use `join`.
The first argument of `join` is the table to be joined, and the second argument is a function that compares the source table with the columns of the table to be joined. This corresponds to the ON clause in the join.

After the join, the `select` will specify columns from the two tables.

```scala
val join = userTable.join(orderTable)((user, order) => user.id === order.userId)
  .select((user, order) => (user.name, order.quantity))

join.statement = "SELECT user.`name`, order.`quantity` FROM user JOIN order ON user.id = order.user_id"
```

Next, if you want to perform a Left Join, which is a left outer join, use `leftJoin`.
The implementation itself is the same as for a simple Join, only `join` is changed to `leftJoin`.

```scala 3
val leftJoin = userTable.leftJoin(orderTable)((user, order) => user.id === order.userId)
  .select((user, order) => (user.name, order.quantity))

join.statement = "SELECT user.`name`, order.`quantity` FROM user LEFT JOIN order ON user.id = order.user_id"
```

The difference from a simple Join is that when using `leftJoin`, the records retrieved from the table to be joined may be NULL.

Therefore, in ldbc, all records in the column retrieved from the table passed to `leftJoin` will be of type Option.

```scala 3
val leftJoin = userTable.leftJoin(orderTable)((user, order) => user.id === order.userId)
  .select((user, order) => (user.name, order.quantity)) // (String, Option[Int])
```

Next, if you want to perform a right join, which is a right outer join, use `rightJoin`.
The implementation itself is the same as for a simple join, only the `join` is changed to `rightJoin`.

```scala 3
val rightJoin = orderTable.rightJoin(userTable)((order, user) => order.userId === user.id)
  .select((order, user) => (order.quantity, user.name))

join.statement = "SELECT order.`quantity`, user.`name` FROM order RIGHT JOIN user ON order.user_id = user.id"
```

The difference from a simple Join is that when using `rightJoin`, the records retrieved from the join source table may be NULL.

Therefore, in ldbc, all records in the columns retrieved from the join source table using `rightJoin` will be of type Option.

```scala 3
val rightJoin = orderTable.rightJoin(userTable)((order, user) => order.userId === user.id)
  .select((order, user) => (order.quantity, user.name)) // (Option[Int], String)
```

If multiple joins are desired, this can be accomplished by calling any Join method in the method chain.

```scala 3
val join = 
  (productTable join orderTable)((product, order) => product.id === order.productId)
    .rightJoin(userTable)((_, order, user) => order.userId === user.id)
    .select((product, order, user) => (product.name, order.quantity, user.name)) // (Option[String], Option[Int], String)]

join.statement =
  """
    |SELECT
    |  product.`name`, 
    |  order.`quantity`,
    |  user.`name`
    |FROM product
    |JOIN order ON product.id = order.product_id
    |RIGHT JOIN user ON order.user_id = user.id
    |""".stripMargin
```

Note that a `rightJoin` join with multiple joins will result in NULL-acceptable access to all records retrieved from the previously joined table, regardless of what the previous join was.

## INSERT

A type-safe way to construct an INSERT statement is to use the following methods provided by Table.

- insert
- insertInto
- +=
- ++=

**insert**

The `insert` method is passed a tuple of data to insert. The tuples must have the same number and type of properties as the model. Also, the order of the inserted data must be in the same order as the model properties and table columns.

```scala 3
val insert = user.insert((1, "name", "email@example.com"))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)"
```

If you want to insert multiple data, you can construct it by passing multiple tuples to the `insert` method.

```scala 3
val insert = user.insert((1, "name 1", "email+1@example.com"), (2, "name 2", "email+2@example.com"))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)"
```

**insertInto**

The `insert` method will insert data into all columns the table has, but if you want to insert data only into specific columns, use the `insertInto` method.

This can be used to exclude data insertion into columns with AutoIncrement or Default values.

```scala 3
val insert = user.insertInto(user => (user.name, user.email)).values(("name 3", "email+3@example.com"))

insert.statement === "INSERT INTO user (`name`, `email`) VALUES(?, ?)"
```

If you want to insert multiple data, you can construct it by passing an array of tuples to `values`.

```scala 3
val insert = user.insertInto(user => (user.name, user.email)).values(List(("name 4", "email+4@example.com"), ("name 5", "email+5@example.com")))

insert.statement === "INSERT INTO user (`name`, `email`) VALUES(?, ?), (?, ?)"
```

**+=**

The `+=` method can be used to construct an INSERT statement using a model. Note that when using a model, data is inserted into all columns.

```scala 3
val insert = user += User(6, "name 6", "email+6@example.com")

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)"
```

**++=**

Use the `++=` method if you want to insert multiple data using the model.

```scala 3
val insert = user ++= List(User(7, "name 7", "email+7@example.com"), User(8, "name 8", "email+8@example.com"))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)"
```

### ON DUPLICATE KEY UPDATE

Inserting a row with an ON DUPLICATE KEY UPDATE clause will cause an UPDATE of the old row if it has a duplicate value in a UNIQUE index or PRIMARY KEY.

The ldbc way to accomplish this is to use `onDuplicateKeyUpdate` for `Insert`.

```scala
val insert = user.insert((9, "name", "email+9@example.com")).onDuplicateKeyUpdate(v => (v.name, v.email))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?) AS new_user ON DUPLICATE KEY UPDATE `name` = new_user.`name`, `email` = new_user.`email`"
```

## UPDATE

A type-safe way to construct an UPDATE statement is to use the `update` method provided by Table.

The first argument of the `update` method is the name of the model property, not the column name of the table, and the second argument is the value to be updated. The type of the value passed as the second argument must be the same as the type of the property specified in the first argument.

```scala
val update = user.update("name", "update name")

update.statement === "UPDATE user SET name = ?"
```

If a property name that does not exist is specified as the first argument, a compile error occurs.

```scala 3
val update = user.update("hoge", "update name") // Compile error
```

If you want to update multiple columns, use the `set` method.

```scala 3
val update = user.update("name", "update name").set("email", "update-email@example.com")

update.statement === "UPDATE user SET name = ?, email = ?"
```

You can also prevent the `set` method from generating queries based on conditions.

```scala 3
val update = user.update("name", "update name").set("email", "update-email@example.com", false)

update.statement === "UPDATE user SET name = ?"
```

You can also use a model to construct the UPDATE statement. Note that if you use a model, all columns will be updated.

```scala 3
val update = user.update(User(1, "update name", "update-email@example.com"))

update.statement === "UPDATE user SET id = ?, name = ?, email = ?"
```

## DELETE

A type-safe way to construct a DELETE statement is to use the `delete` method provided by Table.

```scala
val delete = user.delete

delete.statement === "DELETE FROM user"
```
