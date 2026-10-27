{%
laika.title = "Q: How to handle multiple databases (multi-tenant environment)?"
laika.metadata.language = en
%}

# Q: How to handle multiple databases (multi-tenant environment)?

## A: When dealing with multiple databases, create separate DataSources for each database.

When handling multiple databases, create separate `DataSource` instances for each database. For example, you can create different data sources for different databases as shown below, and switch between them as needed.

```scala 3
import cats.effect.IO
import ldbc.mysql.MySQLDataSource
import ldbc.catseffect.*
import ldbc.mysql.syntax.*

val datasource1 = MySQLDataSource
  .build[IO]("host", 3306, "user")
  .setPassword("password")
  .setDatabase("database1")

val datasource2 = MySQLDataSource
  .build[IO]("host", 3306, "user")
  .setPassword("password")
  .setDatabase("database2")

// Switch between datasources as needed
val program1 = datasource1.use { conn => /* operations on database1 */ }
val program2 = datasource2.use { conn => /* operations on database2 */ }
```
