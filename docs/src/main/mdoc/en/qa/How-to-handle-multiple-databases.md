{%
laika.title = "Q: How to handle multiple databases (multi-tenant environment)?"
laika.metadata.language = en
%}

# Q: How to handle multiple databases (multi-tenant environment)?

## A: When dealing with multiple databases, create separate ConnectionProviders for each database.

When handling multiple databases, create separate `ConnectionProvider` instances for each database. For example, you can create different providers for different databases as shown below, and switch between providers as needed.

```scala 3
val datasource1 = MySQLDataSource
  .build[IO]("host", 3306, "user")
  .setPassword("password")
  .setDatabase("database1")

val datasource2 = MySQLDataSource
  .build[IO]("host", 3306, "user")
  .setPassword("password")
  .setDatabase("database2")

// Switch between datasources as needed
val program1 = datasource1.getConnection.use { conn => /* operations on database1 */ }
val program2 = datasource2.getConnection.use { conn => /* operations on database2 */ }
```
