{%
  laika.title = Query Builder
  laika.metadata.language = en
%}

# Query Builder

We've learned how to add custom types to ldbc in the [Custom Data Type](/en/tutorial/Custom-Data-Type.md) section. In this page, we'll explain how to build queries in a type-safe way without writing SQL directly.

The Query Builder is a feature that allows you to construct database queries in a more type-safe manner than SQL string interpolation. This helps detect more errors at compile time and prevents mistakes related to query structure.

In this chapter, we'll explain methods for building type-safe queries.

## Prerequisites

You need to set up the following dependency in your project:

```scala
//> using dep "@ORGANIZATION@::ldbc-query-builder:@VERSION@"
```

## Basic Usage

In ldbc, we use case classes to represent tables and build queries. Let's start with a simple table definition:

```scala 3
import ldbc.dsl.codec.Codec
import ldbc.query.builder.*

// Table definition
case class User(id: Int, name: String, email: String) derives Table
object User:
  gicen Codec[User] = Codec.derived[User]
```

The `Table` trait is automatically derived using the `derives` keyword. This allows the class properties to be treated as database columns.

To execute queries against the defined table, use `TableQuery`:

```scala
// Build query against the table
val query = TableQuery[User]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")
```

In the code above:
- `TableQuery[User]` - Creates a query for the `User` table
- `select(...)` - Specifies which columns to retrieve
- `*:` - An operator for combining multiple columns
- `where(...)` - Specifies the query condition

## Customizing Table Definitions

### Changing Column Names

If a property name differs from the database column name, you can specify it using the `@Column` annotation:

```scala 3
case class User(
  id: Int,
  @Column("full_name") name: String, // name property maps to full_name column
  email: String
) derives Table
```

### Changing Table Names

By default, the class name is used as the table name, but you can explicitly specify a table name using `Table.derived`:

```scala 3
case class User(id: Int, name: String, email: String)
object User:
  given Table[User] = Table.derived("users") // Specify "users" as the table name
```

## Basic Query Operations

### SELECT

#### Basic SELECT

To retrieve specific columns only:

```scala
val select = TableQuery[User].select(_.id)
// SELECT id FROM user
```

To retrieve multiple columns, use the `*:` operator:

```scala
val select = TableQuery[User].select(user => user.id *: user.name)
// SELECT id, name FROM user
```

To retrieve all columns:

```scala
val select = TableQuery[User].selectAll
// SELECT id, name, email FROM user
```

#### Aggregate Functions

How to use aggregate functions (e.g., count):

```scala
val select = TableQuery[User].select(_.id.count)
// SELECT COUNT(id) FROM user
```

### WHERE Conditions

To add conditions to a query, use the `where` method:

```scala
val where = TableQuery[User].selectAll.where(_.email === "alice@example.com")
// SELECT id, name, email FROM user WHERE email = ?
```

List of comparison operators available in the `where` method:

| Operator                                 | SQL Statement                           | Description                                      |
|----------------------------------------|---------------------------------------|--------------------------------------------------|
| `===`                                  | `column = ?`                          | Equal to                                        |
| `>=`                                   | `column >= ?`                         | Greater than or equal to                        |
| `>`                                    | `column > ?`                          | Greater than                                    |
| `<=`                                   | `column <= ?`                         | Less than or equal to                           |
| `<`                                    | `column < ?`                          | Less than                                       |
| `<>`                                   | `column <> ?`                         | Not equal to                                    |
| `!==`                                  | `column != ?`                         | Not equal to (alternative syntax)               |
| `IS ("TRUE"/"FALSE"/"UNKNOWN"/"NULL")` | `column IS {TRUE/FALSE/UNKNOWN/NULL}` | Check if equals specified value                 |
| `<=>`                                  | `column <=> ?`                        | NULL-safe equal operator (can compare with NULL) |
| `IN (value, value, ...)`               | `column IN (?, ?, ...)`               | Check if matches any of the specified values    |
| `BETWEEN (start, end)`                 | `column BETWEEN ? AND ?`              | Check if within specified range                 |
| `LIKE (value)`                         | `column LIKE ?`                       | Pattern matching                                |
| `LIKE_ESCAPE (like, escape)`           | `column LIKE ? ESCAPE ?`              | Pattern matching with escape character          |
| `REGEXP (value)`                       | `column REGEXP ?`                     | Regular expression                              |
| `<<` (value)                           | `column << ?`                         | Bit left shift                                  |
| `>>` (value)                           | `column >> ?`                         | Bit right shift                                 |
| `DIV (cond, result)`                   | `column DIV ? = ?`                    | Integer division                                |
| `MOD (cond, result)`                   | `column MOD ? = ?`                    | Modulo                                          |
| `^ (value)`                            | `column ^ ?`                          | Bit XOR                                         |
| `~ (value)`                            | `~column = ?`                         | Bit NOT                                         |

Example of combining conditions:

```scala
val complexWhere = TableQuery[User]
  .selectAll
  .where(user => user.email === "alice@example.com" && user.id > 5)
// SELECT id, name, email FROM user WHERE email = ? AND id > ?
```

### GROUP BY and HAVING

To group data, use the `groupBy` method:

```scala
val select = TableQuery[User]
  .select(user => user.id.count *: user.name)
  .groupBy(_.name)
// SELECT COUNT(id), name FROM user GROUP BY name
```

You can use `having` to set conditions on grouped data:

```scala
val select = TableQuery[User]
  .select(user => user.id.count *: user.name)
  .groupBy(_.name)
  .having(_._1 > 1)
// SELECT COUNT(id), name FROM user GROUP BY name HAVING COUNT(id) > ?
```

### ORDER BY

To specify result order, use the `orderBy` method:

```scala
val select = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(_.id)
// SELECT id, name FROM user ORDER BY id
```

To specify ascending or descending order:

```scala
// Ascending order
val selectAsc = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(_.id.asc)
// SELECT id, name FROM user ORDER BY id ASC

// Descending order
val selectDesc = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(_.id.desc)
// SELECT id, name FROM user ORDER BY id DESC

// Sorting by multiple columns
val selectMultiple = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(user => user.name.asc *: user.id.desc)
// SELECT id, name FROM user ORDER BY name ASC, id DESC
```

### LIMIT and OFFSET

Use the `limit` method to limit the number of rows and the `offset` method to specify the number of rows to skip:

```scala
val select = TableQuery[User]
  .select(user => user.id *: user.name)
  .limit(10)    // Get a maximum of 10 rows
  .offset(5)    // Skip the first 5 rows
// SELECT id, name FROM user LIMIT ? OFFSET ?
```

## Table Joins

There are several ways to join multiple tables. First, let's show example table definitions:

```scala 3
// Table definitions
case class User(id: Int, name: String, email: String) derives Table
case class Product(id: Int, name: String, price: BigDecimal) derives Table
case class Order(
  id:        Int,
  userId:    Int,
  productId: Int,
  orderDate: LocalDateTime,
  quantity:  Int
) derives Table

// Generate TableQuery
val userTable    = TableQuery[User]
val productTable = TableQuery[Product]
val orderTable   = TableQuery[Order]
```

### Inner Join

Retrieves only the matching rows from both tables:

```scala
val join = userTable
  .join(orderTable)
  .on((user, order) => user.id === order.userId)
  .select((user, order) => user.name *: order.quantity)
// SELECT user.`name`, order.`quantity` FROM user JOIN order ON user.id = order.user_id
```

### Left Join

Retrieves all rows from the left table and matching rows from the right table. If no match, the right columns will be NULL:

```scala
val leftJoin = userTable
  .leftJoin(orderTable)
  .on((user, order) => user.id === order.userId)
  .select((user, order) => user.name *: order.quantity)
// SELECT user.`name`, order.`quantity` FROM user LEFT JOIN order ON user.id = order.user_id

// The return type is (String, Option[Int])
// since data from the order table might be NULL
```

### Right Join

Retrieves all rows from the right table and matching rows from the left table. If no match, the left columns will be NULL:

```scala
val rightJoin = orderTable
  .rightJoin(userTable)
  .on((order, user) => order.userId === user.id)
  .select((order, user) => order.quantity *: user.name)
// SELECT order.`quantity`, user.`name` FROM order RIGHT JOIN user ON order.user_id = user.id

// The return type is (Option[Int], String)
// since data from the order table might be NULL
```

### Joining Multiple Tables

It's possible to join three or more tables:

```scala
val multiJoin = productTable
  .join(orderTable).on((product, order) => product.id === order.productId)
  .rightJoin(userTable).on((_, order, user) => order.userId === user.id)
  .select((product, order, user) => product.name *: order.quantity *: user.name)
// SELECT 
//   product.`name`, 
//   order.`quantity`,
//   user.`name`
// FROM product
// JOIN order ON product.id = order.product_id
// RIGHT JOIN user ON order.user_id = user.id

// The return type is (Option[String], Option[Int], String)
// because rightJoin is used, data from product and order tables might be NULL
```

## INSERT Statement

There are several methods to insert new records:

### Using the `insert` Method

Directly specify a tuple of values:

```scala
val insert = TableQuery[User].insert((1, "Alice", "alice@example.com"))
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)

// Insert multiple records at once
val multiInsert = TableQuery[User].insert(
  (1, "Alice", "alice@example.com"),
  (2, "Bob", "bob@example.com")
)
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)
```

### Using the `insertInto` Method

Useful when you want to insert values only into specific columns (e.g., when using AUTO INCREMENT):

```scala
val insert = TableQuery[User]
  .insertInto(user => user.name *: user.email)
  .values(("Alice", "alice@example.com"))
// INSERT INTO user (`name`, `email`) VALUES(?, ?)

// Insert multiple records at once
val multiInsert = TableQuery[User]
  .insertInto(user => user.name *: user.email)
  .values(List(
    ("Alice", "alice@example.com"),
    ("Bob", "bob@example.com")
  ))
// INSERT INTO user (`name`, `email`) VALUES(?, ?), (?, ?)
```

### Using Model Objects (with `+=` and `++=` Operators)

```scala
// Insert one record
val insert = TableQuery[User] += User(1, "Alice", "alice@example.com")
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)

// Insert multiple records
val multiInsert = TableQuery[User] ++= List(
  User(1, "Alice", "alice@example.com"),
  User(2, "Bob", "bob@example.com")
)
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)
```

### INSERT Using SELECT Results

You can also insert results from a SELECT query into another table:

```scala
val insertSelect = TableQuery[User]
  .insertInto(user => user.id *: user.name *: user.email)
  .select(
    TableQuery[User]
      .select(user => user.id *: user.name *: user.email)
      .where(_.id > 10)
  )
// INSERT INTO user (`id`, `name`, `email`) 
// SELECT id, name, email FROM user WHERE id > ?
```

### ON DUPLICATE KEY UPDATE

Using the ON DUPLICATE KEY UPDATE clause to update existing rows when a unique or primary key is duplicated:

```scala
val insert = TableQuery[User]
  .insert((9, "Alice", "alice@example.com"))
  .onDuplicateKeyUpdate(v => v.name *: v.email)
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?) 
// AS new_user ON DUPLICATE KEY UPDATE `name` = new_user.`name`, `email` = new_user.`email`

// When updating only specific columns
val insertWithSpecificUpdate = TableQuery[User]
  .insert((9, "Alice", "alice@example.com"))
  .onDuplicateKeyUpdate(_.name, "UpdatedName")
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?) 
// AS new_user ON DUPLICATE KEY UPDATE `name` = ?
```

## UPDATE Statement

To update existing records:

### Updating a Single Column

```scala
val update = TableQuery[User].update(_.name)("UpdatedName")
// UPDATE user SET name = ?
```

### Updating Multiple Columns

```scala
val update = TableQuery[User]
  .update(u => u.name *: u.email)(("UpdatedName", "updated-email@example.com"))
// UPDATE user SET name = ?, email = ?
```

### Conditional Updates (Skip Updating Specific Columns Based on Conditions)

```scala
val shouldUpdate = true // or false
val update = TableQuery[User]
  .update(_.name)("UpdatedName")
  .set(_.email, "updated-email@example.com", shouldUpdate)
// If shouldUpdate is true: UPDATE user SET name = ?, email = ?
// If shouldUpdate is false: UPDATE user SET name = ?
```

### Update Using Model Objects

```scala
val update = TableQuery[User].update(User(1, "UpdatedName", "updated-email@example.com"))
// UPDATE user SET id = ?, name = ?, email = ?
```

### UPDATE with WHERE Conditions

```scala
val update = TableQuery[User]
  .update(_.name, "UpdatedName")
  .where(_.id === 1)
// UPDATE user SET name = ? WHERE id = ?

// Adding AND condition
val updateWithMultipleConditions = TableQuery[User]
  .update(_.name, "UpdatedName")
  .where(_.id === 1)
  .and(_.email === "alice@example.com")
// UPDATE user SET name = ? WHERE id = ? AND email = ?

// Adding OR condition
val updateWithOrCondition = TableQuery[User]
  .update(_.name, "UpdatedName")
  .where(_.id === 1)
  .or(_.id === 2)
// UPDATE user SET name = ? WHERE id = ? OR id = ?
```

## DELETE Statement

To delete records:

### Basic DELETE

```scala
val delete = TableQuery[User].delete
// DELETE FROM user
```

### DELETE with WHERE Conditions

```scala
val delete = TableQuery[User]
  .delete
  .where(_.id === 1)
// DELETE FROM user WHERE id = ?

// Adding AND/OR conditions
val deleteWithMultipleConditions = TableQuery[User]
  .delete
  .where(_.id === 1)
  .and(_.email === "alice@example.com")
// DELETE FROM user WHERE id = ? AND email = ?
```

### DELETE with LIMIT

To delete only a specific number of records:

```scala
val delete = TableQuery[User]
  .delete
  .where(_.id > 10)
  .limit(5)
// DELETE FROM user WHERE id > ? LIMIT ?
```

## Advanced Query Examples

### Subqueries

Example using a subquery:

```scala
val subQuery = TableQuery[Order]
  .select(order => order.userId)
  .where(_.quantity > 10)

val mainQuery = TableQuery[User]
  .select(user => user.name)
  .where(_.id IN subQuery)
// SELECT name FROM user WHERE id IN (SELECT user_id FROM order WHERE quantity > ?)
```

### Complex Joins and Conditions

```scala
val complexQuery = userTable
  .join(orderTable).on((user, order) => user.id === order.userId)
  .join(productTable).on((_, order, product) => order.productId === product.id)
  .select((user, order, product) => user.name *: product.name *: order.quantity)
  .where { case ((user, order, product)) => 
    (user.name LIKE "A%") && (product.price > 100) 
  }
  .orderBy { case ((_, _, product)) => product.price.desc }
  .limit(10)
// SELECT 
//   user.`name`, 
//   product.`name`, 
//   order.`quantity` 
// FROM user 
// JOIN order ON user.id = order.user_id 
// JOIN product ON order.product_id = product.id 
// WHERE user.name LIKE ? AND product.price > ? 
// ORDER BY product.price DESC 
// LIMIT ?
```

### Queries with Conditional Branching

When you want to change the query based on runtime conditions:

```scala
val nameOption: Option[String] = Some("Alice") // or None
val minIdOption: Option[Int] = Some(5) // or None

val query = TableQuery[User]
  .selectAll
  .whereOpt(user => nameOption.map(name => user.name === name))
  .andOpt(user => minIdOption.map(minId => user.id >= minId))
// If both nameOption and minIdOption are Some:
// SELECT id, name, email FROM user WHERE name = ? AND id >= ?
// If nameOption is None and minIdOption is Some:
// SELECT id, name, email FROM user WHERE id >= ?
// If both are None:
// SELECT id, name, email FROM user
```

## Executing Queries

Execute the constructed queries as follows:

```scala 3
import ldbc.dsl.*

// Create Connector
val connector = Connector.fromDataSource(datasource)

(for
  // Execute a SELECT query
  users <- TableQuery[User].selectAll.where(_.id > 5).query.to[List]  // Get results as a List
  // Get a single result
  user <- TableQuery[User].selectAll.where(_.id === 1).query.to[Option]
  // Execute an update query
  _ <- TableQuery[User].update(_.name)("NewName").where(_.id === 1).update
yield ???).transaction(connector)
```

## Next Steps

Now you know how to build type-safe queries using the Query Builder. This approach allows you to detect more errors at compile time than writing SQL directly, enabling you to write safer code.

Next, proceed to [Schema](/en/tutorial/Schema.md) to learn how to define database schemas using Scala code.
