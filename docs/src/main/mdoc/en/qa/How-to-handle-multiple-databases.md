{%
laika.title = "Q: How to handle multiple databases (multi-tenant environment)?"
laika.metadata.language = en
%}

# Q: How to handle multiple databases (multi-tenant environment)?

## A: When dealing with multiple databases, create separate ConnectionProviders for each database.

When handling multiple databases, create separate `ConnectionProvider` instances for each database. For example, you can create different providers for different databases as shown below, and switch between providers as needed.

```scala 3
val provider1 = ConnectionProvider
  .default[IO]("host", 3306, "user", "password", "database1")

val provider2 = ConnectionProvider
  .default[IO]("host", 3306, "user", "password", "database2")

// Switch between providers as needed
val program1 = provider1.use { conn => /* operations on database1 */ }
val program2 = provider2.use { conn => /* operations on database2 */ }
```
