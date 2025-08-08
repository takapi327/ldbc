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
2. **Static parameters** - Directly embedded as part of the SQL statement (e.g., table names, column names, etc.)

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
provider.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
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

provider.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id AND email > $email"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
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

provider.use { conn =>
  (sql"SELECT name, email FROM user WHERE " ++ in("id", ids))
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
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

## Static Parameters

Sometimes you may want to parameterize structural parts of the SQL statement, such as column names or table names. In such cases, you can use "static parameters" which directly embed values into the SQL statement.

While dynamic parameters (regular `$value`) are processed by `PreparedStatement` and replaced with `?` in the query string, static parameters are directly embedded as strings.

To use static parameters, use the `sc` function:

```scala
val column = "name"
val table = "user"

// Treating as a dynamic parameter would result in "SELECT ? FROM user"
// sql"SELECT $column FROM user".query[String].to[List]

// Treating as a static parameter results in "SELECT name FROM user"
sql"SELECT ${sc(column)} FROM ${sc(table)}".query[String].to[List]
```

In this example, the generated SQL is `SELECT name FROM user`.

> **Warning**: `sc(...)` does not escape the passed string, so passing unvalidated data such as user input directly poses a risk of SQL injection attacks. Use static parameters only from safe parts of your application (constants, configurations, etc.).

Common use cases for static parameters:

```scala
// Dynamic sort order
val sortColumn = "created_at" 
val sortDirection = "DESC"

sql"SELECT * FROM user ORDER BY ${sc(sortColumn)} ${sc(sortDirection)}"

// Dynamic table selection
val schema = "public"
val table = "user"

sql"SELECT * FROM ${sc(schema)}.${sc(table)}"
```

## Next Steps

Now you understand how to use parameterized queries. With the ability to handle parameters, you can build more complex and practical database queries.

Next, proceed to [Selecting Data](/en/tutorial/Selecting-Data.md) to learn how to retrieve data in various formats.
