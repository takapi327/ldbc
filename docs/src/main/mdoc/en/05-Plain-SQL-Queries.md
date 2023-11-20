# Plain SQL Queries

Sometimes you may need to write your own SQL code for operations that are not well supported at a higher level of abstraction; instead of going back to the lower layers of JDBC, you can use LDBC's Plain SQL queries in the Scala-based API.
This chapter describes how to use Plain SQL queries in LDBC to process connections to databases in such cases.

See the previous chapter on [Database Connections](/en/04-Database-Connection.html) for project dependencies and the use and logging of DataSource.

## Plain SQL

LDBC uses sql string interpolation with literal SQL strings to construct plain queries as follows

Variables and expressions injected into the query are converted to bind variables in the resulting query string. Since they are not inserted directly into the query string, there is no risk of SQL injection attacks.

```scala 3
val select = sql"SELECT id, name, age FROM user WHERE id = $id" // SELECT id, name, age FROM user WHERE id = ?
val insert = sql"INSERT INTO user (id, name, age) VALUES($id, $name, $age)" // INSERT INTO user (id, name, age) VALUES(?, ?, ?)
val update = sql"UPDATE user SET id = $id, name = $name, age = $age" // UPDATE user SET id = ?, name = ?, age = ?
val delete = sql"DELETE FROM user WHERE id = $id" // DELETE FROM user WHERE id = ?
```

Plain SQL queries simply construct SQL statements at runtime. While this provides a safe and easy way to construct complex statements, it is merely an embedded string. Any syntax errors in the statement or type mismatch between the database and the Scala code cannot be detected at compile time.

Please refer to the [Query](/en/04-Database-Connection.html#Query) item in the previous section "Database Connection" for information on setting the return type of the query result and the connection method.
It is built and works the same way as a query built using a table definition.

Plain queries and type-safe queries are constructed differently, but the implementation is the same, including the subsequent connection methods. Therefore, it is possible to combine the two and execute the query.

```scala 3
(for
  result1 <- sql"INSERT INTO user (id, name, age) VALUES($id, $name, $age)".update
  result2 <- userQuery.update("name", "update name").update
  ...
yield ...).transaction
```
