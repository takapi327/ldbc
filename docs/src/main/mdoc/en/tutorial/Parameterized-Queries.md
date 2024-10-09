{%
  laika.title = Parameter
  laika.metadata.language = en
%}

# Parameterized Queries

In this chapter, you will learn how to construct parameterized queries.

## Adding parameters

First, create a query with no parameters.

```scala
sql"SELECT name, email FROM user".query[(String, String)].to[List]
```

Next, let's incorporate the query into a method and add a parameter to select only the data that matches the `id` specified by the user. Insert the `id` argument as `$id` into the SQL statement, just as you would interpolate a string.

```scala
val id = 1

sql"SELECT name, email FROM user WHERE id = $id".query[(String, String)].to[List]
```

Querying with connections works fine.

```scala
connection.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
```

What is happening here? It looks like we are just dropping a string literal into an SQL string, but we are actually building a `PreparedStatement` and the `id` value is eventually set by a call to `setInt`.

## Multiple parameters

Multiple parameters work the same way. No surprises.

```scala
val id = 1
val email = "alice@example.com"

connection.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id AND email > $email"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
```

## Handling IN Clauses

A common irritation when dealing with SQL literals is the desire to inline a series of arguments into an IN clause, but SQL does not support this concept (nor does JDBC support anything).

```scala
val ids = NonEmptyList.of(1, 2, 3)

connection.use { conn =>
  (sql"SELECT name, email FROM user WHERE" ++ in("id", ids))
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
```

Note that the `ids` is `NonEmptyList` since the IN clause must not be empty.

Executing this query yields the desired result

ldbc provides several other useful functions.

- `values` - Creates a VALUES clause.
- `in` - Creates an IN clause.
- `notIn` - Creates a NOT IN clause.
- `and` - Generates an AND clause.
- `or` - Generates an OR clause.
- `whereAnd` - Generates a WHERE clause with multiple conditions enclosed in AND clauses.
- `whereOr` - Generates WHERE clauses for multiple conditions enclosed in OR clauses.
- `set` - Generates a SET clause.
- `orderBy` - Generates an ORDER BY clause.

## Static parameters

Although parameters are dynamic, sometimes you may want to use them as parameters but treat them as static values.

For example, to change the column to be retrieved based on the value received, you can write the following

```scala
val column = "name"

sql"SELECT $column FROM user".query[String].to[List]
```

Dynamic parameters are handled by `PreparedStatement`, so the query string itself is replaced by `? `.

Thus, the query will be executed as `SELECT ? FROM user`.

This makes it difficult to understand the query output in the log, so if you want to treat `$column` as a static value, set `$column` to `${sc(column)}` so that it is directly embedded in the query string.

```scala
val column = "name"

sql"SELECT ${sc(column)} FROM user".query[String].to[List]
```

This query is executed as `SELECT name FROM user`.

> `sc(...)` Note that does not escape the passed string. Passing user-supplied data is an injection risk.
