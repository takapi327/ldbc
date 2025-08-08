{%
  laika.title = "Q: What is the difference between a Java connector and a Scala connector?"
  laika.metadata.language = en
%}

# Q: What is the difference between a Java connector and a Scala connector?

## A: While both Java connector (jdbc-connector) and Scala connector (ldbc-connector) provide database connectivity, they differ in the following aspects:

### A: Java Connector (jdbc-connector)
The Java connector uses traditional JDBC API to connect to databases.  
- Depends on JDBC drivers (e.g., `mysql-connector-j` for MySQL) and requires low-level configuration.  
- Connection establishment, query execution, and result retrieval are implemented using traditional procedural APIs.  

```scala
import com.mysql.cj.jdbc.MysqlDataSource
import cats.effect.IO
import ldbc.dsl.DBIO

// Example of datasource configuration and JDBC connector usage
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")

val provider = ConnectionProvider.fromDataSource[IO](ex, ExecutionContexts.synchronous)

// Example of using Java JDBC API-based connection
provider.use { conn =>
  // Execute SQL using PreparedStatement etc.
  DBIO.pure(()).commit(conn)
}
```

### A: Scala Connector (ldbc-connector)
The Scala connector manages database connections leveraging type safety and functional programming.  
- Uses Cats Effect's `Resource` and `IO` for safe connection acquisition and release.  
- Enables intuitive data manipulation when combined with DSL and query builders.  
- Additionally, ldbc-connector works not only on JVM but also on non-JVM platforms like Scala.js and Scala Native.  
  This makes database connectivity easily achievable in cross-platform development environments.

```scala
import cats.effect.IO
import ldbc.connector.*
import ldbc.dsl.DBIO

// Example of creating a connection using ldbc-connector (JVM, Scala.js, Scala Native compatible)
val provider =
  ConnectionProvider
    .default[IO]("127.0.0.1", 3306, "ldbc", "password", "ldbc")
    .setSSL(SSL.Trusted)

// Example of using Scala connector: automatically ensures connection closure using Resource internally
provider.use { conn =>
  // Can execute SQL using ldbc DSL and DBIO
  DBIO.pure(()).commit(conn)
}
```

### A: Key Differences
- **API Design Philosophy**:  
  While Java connector uses traditional procedural JDBC API as is, Scala connector is based on functional programming, achieving type-safe and declarative connection management.
- **Error Handling and Resource Management**:  
  Scala connector uses Cats Effect's `Resource` and `IO` for safe connection acquisition and release, enabling concise error handling.
- **Integration**:  
  Scala connector seamlessly integrates with ldbc DSL and query builders, providing a unified type-safe API from query definition to execution.

## References
- [Connection](/en/tutorial/Connection.md)  
- [What is ldbc?](/en/qa/What-is-ldbc.md)
