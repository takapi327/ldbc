{%
  laika.title = Parameterized Queries
  laika.metadata.language = en
%}

# Parameterized Queries

In the [Simple Program](/en/tutorial/Simple-Program.md) tutorial, we learned how to execute basic queries. In real applications, you'll often execute queries based on user input or variable values. On this page, you'll learn how to safely handle parameters.

In ldbc, we strongly recommend using parameterized queries to prevent SQL injection attacks. Parameterized queries allow you to separate SQL code from data, enabling safer database access.

## Parameter Basics

In ldbc, there are two main ways to embed parameters in SQL statements:

1. **Dynamic parameters** - Used as regular parameters, processed by `PreparedStatement` to prevent SQL injection attacks
2. **Identifier escaping** - Safely embeds table names and column names in backticks using the `ident` function

## Adding Dynamic Parameters

First, let's create a query without parameters.

```scala
sql"SELECT name, email FROM user".query[(String, String)].to[List]
```

Next, let's incorporate the query into a method and add a parameter to select only data matching the user-specified `id`. We insert the `id` argument into the SQL statement as `$id`, just like string interpolation.

```scala
val id = 1

sql"SELECT name, email FROM user WHERE id = $id".query[(String, String)].to[List]
```

When we execute the query using a connection, it works without issues.

```scala
// Create Connector
val connector = Connector.fromDataSource(datasource)

sql"SELECT name, email FROM user WHERE id = $id"
  .query[(String, String)]
  .to[List]
  .readOnly(connector)
```

What's happening here? It looks like we're just dropping string literals into an SQL string, but we're actually building a `PreparedStatement`, and the `id` value is ultimately set by a call to `setInt`. This protects our application from SQL injection attacks.

You can use parameters of various types:

```scala
val id: Int = 1
val name: String = "Alice"
val active: Boolean = true
val createdAt: LocalDateTime = LocalDateTime.now()

sql"INSERT INTO user (id, name, active, created_at) VALUES ($id, $name, $active, $createdAt)"
```

In ldbc, appropriate encoders are provided for each type, safely converting Scala/Java values to SQL values.

## Multiple Parameters

Multiple parameters can be used in the same way.

```scala
val id = 1
val email = "alice@example.com"

// Create Connector
val connector = Connector.fromDataSource(datasource)

sql"SELECT name, email FROM user WHERE id = $id AND email > $email"
  .query[(String, String)]
  .to[List]
  .readOnly(connector)
```

## Combining Queries

When building large queries, you can combine multiple SQL fragments.

```scala
val baseQuery = sql"SELECT name, email FROM user"
val whereClause = sql"WHERE id > $id"
val orderClause = sql"ORDER BY name ASC"

val query = baseQuery ++ whereClause ++ orderClause
```

## SQL Helper Functions

ldbc provides many helper functions for easily constructing complex SQL clauses.

### Handling IN Clauses

A common challenge in SQL is using a series of values in an IN clause. In ldbc, this can be easily implemented using the `in` function.

```scala
val ids = NonEmptyList.of(1, 2, 3)

// Create Connector
val connector = Connector.fromDataSource(datasource)

(sql"SELECT name, email FROM user WHERE " ++ in("id", ids))
  .query[(String, String)]
  .to[List]
  .readOnly(connector)
```

This is equivalent to the following SQL:

```sql
SELECT name, email FROM user WHERE (id IN (?, ?, ?))
```

Note that `ids` must be a `NonEmptyList` because an IN clause cannot be empty.

### Other Helper Functions

ldbc provides many other convenient functions:

#### Generating VALUES Clauses

```scala
val users = NonEmptyList.of(
  (1, "Alice", "alice@example.com"),
  (2, "Bob", "bob@example.com")
)

(sql"INSERT INTO user (id, name, email) " ++ values(users))
```

#### WHERE Clause Conditions

You can easily construct AND and OR conditions:

```scala
val activeFilter = sql"active = true"
val nameFilter = sql"name LIKE ${"A%"}"
val emailFilter = sql"email IS NOT NULL"

// WHERE (active = true) AND (name LIKE 'A%') AND (email IS NOT NULL)
val query1 = sql"SELECT * FROM user " ++ whereAnd(activeFilter, nameFilter, emailFilter)

// WHERE (active = true) OR (name LIKE 'A%')
val query2 = sql"SELECT * FROM user " ++ whereOr(activeFilter, nameFilter)
```

#### Generating SET Clauses

You can easily generate SET clauses for UPDATE statements:

```scala
val name = "New Name"
val email = "new@example.com"

val updateValues = set(
  sql"name = $name",
  sql"email = $email",
  sql"updated_at = NOW()"
)

sql"UPDATE user " ++ updateValues ++ sql" WHERE id = 1"
```

#### Generating ORDER BY Clauses

```scala
val query = sql"SELECT * FROM user " ++ orderBy(sql"name ASC", sql"created_at DESC")
```

### Optional Conditions

When conditions are optional (may not exist), you can use functions with the `Opt` suffix:

```scala
val nameOpt: Option[String] = Some("Alice")
val emailOpt: Option[String] = None

val nameFilter = nameOpt.map(name => sql"name = $name")
val emailFilter = emailOpt.map(email => sql"email = $email")

// Since nameFilter is Some(...) and emailFilter is None, the WHERE clause will only contain "name = ?"
val query = sql"SELECT * FROM user " ++ whereAndOpt(nameFilter, emailFilter)
```

## Identifier Escaping

Sometimes you may want to parameterize structural parts of the SQL statement, such as column names or table names. In such cases, use the `ident` function.

While dynamic parameters (regular `$value`) are processed by `PreparedStatement` and replaced with `?` in the query string, `ident` wraps identifiers in backticks and embeds them directly into the SQL statement. It also removes NUL characters, allowing identifiers to be handled safely.

```scala
val column = "name"
val table = "user"

// Treating as a dynamic parameter would result in "SELECT ? FROM user"
// sql"SELECT $column FROM user".query[String].to[List]

// Using ident results in "SELECT `name` FROM `user`"
sql"SELECT ${ident(column)} FROM ${ident(table)}".query[String].to[List]
```

Common use cases for `ident`:

```scala
// Dynamic column selection
val sortColumn = "created_at"

sql"SELECT * FROM user ORDER BY ${ident(sortColumn)} DESC"

// Dynamic table selection
val schema = "public"
val table = "user"

sql"SELECT * FROM ${ident(schema)}.${ident(table)}"
```

> **Note**: While `ident` escapes with backticks, it is recommended to use it only with trusted values (constants, configuration values, etc.). Avoid using user input directly as identifiers.

## Conditional SQL Fragments

When you want to conditionally append a SQL fragment, use the `when` function.

```scala
val limit: Option[Int] = Some(10)

sql"SELECT name, email FROM user" ++ when(limit.isDefined)(sql" LIMIT ${limit.get}")
```

`when(condition)(fragment)` appends `fragment` only when `condition` is `true`. When `false`, it produces an empty fragment.

You can combine multiple conditions:

```scala
val nameFilter: Option[String] = Some("Alice")
val activeOnly: Boolean = true

val query =
  sql"SELECT * FROM user" ++
  when(nameFilter.isDefined)(sql" WHERE name = ${nameFilter.get}") ++
  when(activeOnly)(sql" AND active = true")
```

## Pagination

For list queries that commonly require `LIMIT` / `OFFSET`, use the `paginate` function for concise pagination.

```scala
// Specify both limit and offset
sql"SELECT name, email FROM user " ++ paginate(limit = 20, offset = 40)
// → SELECT name, email FROM user LIMIT ? OFFSET ?

// Specify limit only
sql"SELECT name, email FROM user " ++ paginate(limit = 20)
// → SELECT name, email FROM user LIMIT ?
```

Example calculating offset from a page number:

```scala
val pageSize = 20
val page     = 3  // 1-based

sql"SELECT name, email FROM user ORDER BY id " ++ paginate(limit = pageSize, offset = (page - 1) * pageSize)
```

> **Note**: Passing a negative value for `limit` or `offset` throws an `IllegalArgumentException`.

## Next Steps

Now you understand how to use parameterized queries. With the ability to handle parameters, you can build more complex and practical database queries.

Next, proceed to [Selecting Data](/en/tutorial/Selecting-Data.md) to learn how to retrieve data in various formats.
