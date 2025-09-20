{%
  laika.title = HikariCP
  laika.metadata.language = en
%}

# Integration of HikariCP with ldbc

## Introduction

HikariCP is one of the most widely used database connection pool libraries in Java.
When combined with ldbc, it enables efficient database connection management.

## Adding Dependencies

First, add the necessary dependencies to build.sbt:

```scala
libraryDependencies ++= Seq(
  "com.mysql" % "mysql-connector-j" % "8.4.0",
  "com.zaxxer" % "HikariCP" % "6.2.1"
)
```

## Basic HikariCP Configuration

HikariCP has many configuration options. Here are the main configuration items:

```scala
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

val config = new HikariConfig()
config.setJdbcUrl("jdbc:mysql://localhost:3306/database")
config.setUsername("username")
config.setPassword("password")

// Basic connection pool settings
config.setMaximumPoolSize(10)         // Maximum pool size
config.setMinimumIdle(5)             // Minimum idle connections
config.setConnectionTimeout(30000)    // Connection timeout (milliseconds)
config.setIdleTimeout(600000)        // Idle timeout (milliseconds)

val dataSource = new HikariDataSource(config)
```

## Integration Example with ldbc

Here's a basic example of combining HikariCP with ldbc:

```scala
import cats.effect.*
import com.zaxxer.hikari.HikariDataSource
import ldbc.dsl.*
import ldbc.dsl.codec.Codec
import jdbc.connector.*
import ldbc.connector.Connector

// Data model definition
case class User(id: Int, name: String, email: String)
object User:
  given Codec[User] = Codec.derived[User]

object HikariExample extends IOApp.Simple:
  def run: IO[Unit] =
    // HikariCP configuration
    val ds = new HikariDataSource()
    ds.setJdbcUrl("jdbc:mysql://localhost:3306/mydb")
    ds.setUsername("user")
    ds.setPassword("password")
    
    // DataSource and Connector setup
    val program = for
      hikari     <- Resource.fromAutoCloseable(IO(ds))
      execution  <- ExecutionContexts.fixedThreadPool[IO](10)
      datasource = MySQLDataSource.fromDataSource[IO](hikari, execution)
    yield Connector.fromDataSource(datasource)
    
    // Query execution
    program.use { connector =>
      for
        users <- sql"SELECT * FROM users".query[User].to[List].readOnly(connector)
        _     <- IO.println(s"Found users: $users")
      yield ()
    }
```

## Advanced Configuration Examples

HikariCP offers various settings for performance tuning:

```scala
val config = new HikariConfig()

// Basic settings
config.setPoolName("MyPool")                // Set pool name
config.setAutoCommit(false)                 // Disable auto-commit

// Performance settings
config.setMaxLifetime(1800000)             // Maximum connection lifetime (30 minutes)
config.setValidationTimeout(5000)          // Connection validation timeout
config.setLeakDetectionThreshold(60000)    // Connection leak detection threshold

// MySQL-specific settings
config.addDataSourceProperty("cachePrepStmts", "true")
config.addDataSourceProperty("prepStmtCacheSize", "250")
config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
```

## Summary

Combining HikariCP with ldbc provides the following benefits:

- Efficient connection pooling
- Simplified transaction management
- Type-safe query execution
- Pure functional programming using Cats Effect

With proper configuration and error handling, you can achieve stable, high-performance database access.
