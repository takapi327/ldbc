{%
  laika.title = Migration Note
  laika.metadata.language = en
%}

# Migration Notes (from 0.3.x to 0.4.x)

## Packages

**Removed Packages**

| Module / Platform    | JVM | Scala Native | Scala.js |  
|----------------------|:---:|:------------:|:--------:|
| `ldbc-schemaSpy`     |  ✅  |      ❌       |    ❌     | 

**Packages renewed as different features**

| Module / Platform    | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                  |
|----------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-core`          |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)          |

**All Packages**

| Module / Platform    | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                  |
|----------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`           |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)           |
| `ldbc-core`          |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)          |
| `ldbc-connector`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)     |
| `jdbc-connector`     |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)     |
| `ldbc-dsl`           |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)           |
| `ldbc-statement`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)     |
| `ldbc-query-builder` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3) |
| `ldbc-schema`        |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)        |
| `ldbc-codegen`       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)       |
| `ldbc-hikari`        |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-hikari_3)        |
| `ldbc-plugin`        |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0) |

## 🎯 Major Changes

### 1. Built-in Connection Pooling

Starting from version 0.4.0, ldbc-connector includes a high-performance connection pooling feature. This enables efficient connection management without using external libraries like HikariCP.

**Benefits:**
- Optimized for Cats Effect's fiber-based concurrency model
- Circuit breaker protection during failures
- Dynamic pool sizing
- Detailed metrics tracking

### 2. API Changes

#### Migration from ConnectionProvider to MySQLDataSource and Connector Usage

ConnectionProvider is now deprecated and replaced with the new MySQLDataSource and Connector APIs.

**Old API (0.3.x):**
```scala
import ldbc.connector.*

// Using ConnectionProvider
val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")

// Direct usage
provider.use { connection =>
  // SQL execution
}
```

**New API (0.4.x):**
```scala
import ldbc.connector.*
import ldbc.core.*
import ldbc.dsl.*

// Using MySQLDataSource
val dataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")

// Create Connector and execute DBIO
val connector = Connector.fromDataSource(dataSource)

// Execute SQL queries
val result = sql"SELECT * FROM users"
  .query[User]
  .to[List]
  .readOnly(connector)

// Or create Connector from Connection
dataSource.getConnection.use { connection =>
  val connector = Connector.fromConnection(connection)
  // Execute DBIO
  sql"INSERT INTO users (name) VALUES ($name)"
    .update
    .commit(connector)
}

// Connection pooling
val pooledDataSource = MySQLDataSource.pooling[IO](
  MySQLConfig.default
    .setHost("localhost")
    .setPort(3306)
    .setUser("root")
    .setPassword("password")
    .setDatabase("test")
    .setMinConnections(5)
    .setMaxConnections(20)
)

pooledDataSource.use { pool =>
  val connector = Connector.fromDataSource(pool)
  // Execute DBIO
  sql"SELECT * FROM users WHERE id = $id"
    .query[User]
    .option
    .readOnly(connector)
}
```

### 3. Configuration Changes

#### ldbc-connector Configuration

**Old Method (0.3.x):**
```scala
val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")
  .setSSL(SSL.Trusted)
  .addSocketOption(SocketOption.receiveBufferSize(4096))
  .setReadTimeout(30.seconds)
```

**New Method (0.4.x):**
```scala
val dataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")
  .setSSL(SSL.Trusted)
  .addSocketOption(SocketOption.receiveBufferSize(4096))
  .setReadTimeout(30.seconds)
  .setDebug(true)
  .setAllowPublicKeyRetrieval(true)
```

#### jdbc-connector Configuration

**Old Method (0.3.x):**
```scala
import jdbc.connector.*

val dataSource = new com.mysql.cj.jdbc.MysqlDataSource()
// Manual configuration

val provider = ConnectionProvider
  .fromDataSource[IO](dataSource, ec)
```

**New Method (0.4.x):**
```scala
import jdbc.connector.*

// Create Connector from DataSource
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("localhost")
ds.setPortNumber(3306)
ds.setDatabaseName("test")
ds.setUser("root")
ds.setPassword("password")

val connector = Connector.fromDataSource[IO](ds, ExecutionContexts.synchronous)

// Create Connector from DriverManager
val connector = Connector.fromDriverManager[IO].apply(
  driver = "com.mysql.cj.jdbc.Driver",
  url = "jdbc:mysql://localhost:3306/test",
  user = "root",
  password = "password",
  logHandler = None
)

// Via MySQLDataSource (ldbc-connector)
val dataSource = MySQLDataSource
  .fromDataSource[IO](ds, ExecutionContexts.synchronous)
```

### 4. Connection Pooling Usage

#### Basic Usage

```scala
import ldbc.connector.*
import ldbc.core.*
import scala.concurrent.duration.*

val config = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  // Pool configuration
  .setMinConnections(5)              // Minimum connections
  .setMaxConnections(20)             // Maximum connections
  .setConnectionTimeout(30.seconds)  // Connection timeout
  .setIdleTimeout(10.minutes)        // Idle timeout
  .setMaxLifetime(30.minutes)        // Maximum lifetime

MySQLDataSource.pooling[IO](config).use { pool =>
  // Create and use Connector
  val connector = Connector.fromDataSource(pool)

  // Execute SQL queries
  sql"SELECT COUNT(*) FROM users"
    .query[Long]
    .unique
    .readOnly(connector)
}
```

#### Pool with Metrics

```scala
import ldbc.connector.pool.*

val metricsResource = for {
  tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
  pool    <- MySQLDataSource.pooling[IO](
    config,
    metricsTracker = Some(tracker)
  )
} yield (pool, tracker)

metricsResource.use { case (pool, tracker) =>
  for {
    _ <- pool.getConnection.use(_.execute("SELECT 1"))
    metrics <- tracker.getMetrics
    _ <- IO.println(s"""
      |Pool Metrics:
      |  Total connections: ${metrics.totalCreated}
      |  Active: ${metrics.activeConnections}
      |  Idle: ${metrics.idleConnections}
    """.stripMargin)
  } yield ()
}
```

#### Before/After Hooks

```scala
case class RequestContext(requestId: String)

val poolWithHooks = MySQLDataSource.poolingWithBeforeAfter[IO, RequestContext](
  config = config,
  before = Some { conn =>
    for {
      id <- IO.randomUUID.map(_.toString)
      _  <- conn.execute(s"SET @request_id = '$id'")
    } yield RequestContext(id)
  },
  after = Some { (ctx, conn) =>
    IO.println(s"Request ${ctx.requestId} completed")
  }
)
```

### 5. Migration Considerations

#### Scala Native Limitations

@:callout(warning)
**Important**: Scala Native 0.4.x only supports single-threaded execution. Therefore, using connection pooling with Scala Native is not recommended. Instead, create a new connection for each operation:

```scala
// Recommended usage for Scala Native
val dataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")

// Don't use pooling
val connector = Connector.fromDataSource(dataSource)

// Execute DBIO
sql"SELECT * FROM products WHERE price > $minPrice"
  .query[Product]
  .to[List]
  .readOnly(connector)
```
@:@

### 6. Breaking Changes

The following APIs have been removed or changed:

1. **ConnectionProvider**: Deprecated and replaced with `MySQLDataSource` (will be removed in 0.5.x)
2. **Provider trait**: Deprecated and replaced with `DataSource` trait
3. **ldbc.sql.Provider**: Removed
4. **Direct connection usage**: Must now use the new `Connector` API

### 7. DBIO Execution Pattern Changes

The DBIO execution method has been changed to be clearer and more flexible.

**Old Method (0.3.x):**
```scala
provider.use { connection =>
  (for
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
  yield (result1, result2)).readOnly(connection)
}
```

**New Method (0.4.x):**
```scala
val connector = Connector.fromDataSource(dataSource)

// Various execution modes
sql"SELECT * FROM users".query[User].to[List].readOnly(connector)    // Read-only
sql"INSERT INTO users ...".update.commit(connector)                 // With commit
sql"UPDATE users ...".update.transaction(connector)                 // Transaction
sql"DELETE FROM users ...".update.rollback(connector)              // Rollback

// Combining multiple queries
(for
  users <- sql"SELECT * FROM users".query[User].to[List]
  count <- sql"SELECT COUNT(*) FROM users".query[Long].unique
yield (users, count)).readOnly(connector)
```

### 8. New Features

#### CircuitBreaker

The connection pool includes a built-in CircuitBreaker for protection during database failures:

- Automatically stops connection attempts after consecutive failures
- Gradual recovery through exponential backoff
- Protects both the application and database

#### Adaptive Pool Sizing

Dynamically adjusts pool size based on load:

```scala
val config = MySQLConfig.default
  // ... other configuration
  .setAdaptiveSizing(true)
  .setAdaptiveInterval(1.minute)
```

#### Leak Detection

Detects connection leaks in development:

```scala
val config = MySQLConfig.default
  // ... other configuration
  .setLeakDetectionThreshold(2.minutes)
```

#### Streaming Query Support

ldbc supports efficient streaming queries using `fs2.Stream`. This allows handling large amounts of data with controlled memory usage.

**Basic Usage:**
```scala
import fs2.Stream
import ldbc.dsl.*

// Streaming with default fetchSize (1)
val stream: Stream[DBIO, String] = 
  sql"SELECT name FROM city"
    .query[String]
    .stream

// Streaming with specified fetchSize
val streamWithFetchSize: Stream[DBIO, City] = 
  sql"SELECT * FROM city"
    .query[City]
    .stream(fetchSize = 100)
```

**Practical Examples:**
```scala
// Efficiently process large datasets
val processLargeCities: IO[List[String]] = 
  sql"SELECT name, population FROM city"
    .query[(String, Int)]
    .stream(1000)                    // Fetch 1000 rows at a time
    .filter(_._2 > 1000000)          // Cities with > 1M population
    .map(_._1)                       // Extract city names
    .take(50)                        // Take first 50
    .compile.toList
    .readOnly(connector)

// Aggregation processing
val calculateTotal: IO[BigDecimal] = 
  sql"SELECT amount FROM transactions WHERE year = 2024"
    .query[BigDecimal]
    .stream(5000)                    // Process 5000 rows at a time
    .filter(_ > 100)                 // Transactions > 100
    .fold(BigDecimal(0))(_ + _)      // Calculate total
    .compile.lastOrError
    .transaction(connector)
```

**MySQL Optimization Settings:**
```scala
// Enable server-side cursors for better memory efficiency
val datasource = MySQLDataSource
  .build[IO](host, port, user)
  .setPassword(password)
  .setDatabase(database)
  .setUseCursorFetch(true)  // Enable true streaming
```

**Streaming Benefits:**
- **Memory Efficiency**: Constant memory usage even with large datasets
- **Early Processing**: Process data while receiving it
- **Cancellable**: Stop processing based on conditions
- **Rich fs2 Operations**: Use functional operations like `filter`, `map`, `take`, `fold`

## Summary

Migrating to 0.4.x provides the following benefits:

1. **Performance Improvements**: Efficient connection management through built-in pooling
2. **More Intuitive API**: Simplified configuration with builder pattern
3. **Advanced Features**: CircuitBreaker, adaptive sizing, metrics tracking
4. **Reduced External Dependencies**: No need for HikariCP

The migration work mainly involves API updates, and since functional backward compatibility is maintained, gradual migration is possible.
