{%
  laika.title = "Q: How to use connection pool with Scala connector?"
  laika.metadata.language = en
%}

# Q: How to use connection pool with Scala connector?

## A: Use the standalone module `ldbc-pool`.

In 0.9.x, connection pooling is provided as the standalone module `ldbc-pool`, used in combination with the `ldbc-mysql` driver. The pooling system is effect-agnostic (`F: Concurrent`), leveraging a fiber-based concurrency model to offer high performance and excellent resource efficiency (the examples below use Cats Effect).

Add `ldbc-mysql`, `ldbc-cats-effect`, and `ldbc-pool` to your dependencies.

## Quick Start

Create a pool from a `MySQLDataSource` (connection information) and a `ConnectionPoolConfig` (pool settings) with `PooledDataSource.fromDataSource`:

```scala 3
import cats.effect.*
import scala.concurrent.duration.*

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }

object ConnectionPoolExample extends IOApp.Simple:

  val run =
    // Connection information
    val datasource = MySQLDataSource
      .build[IO]("localhost", 3306, "root")
      .setPassword("password")
      .setDatabase("testdb")
      .setSSL(SSL.Trusted)

    // Connection pool configuration
    val poolConfig = ConnectionPoolConfig(
      minConnections    = 5,          // Keep at least 5 connections ready
      maxConnections    = 20,         // Maximum 20 connections
      connectionTimeout = 30.seconds  // Wait up to 30 seconds for a connection
    )

    // Create the pooled data source
    PooledDataSource.fromDataSource[IO](poolConfig, datasource).use { pool =>
      // Use the pool as a Connector
      sql"SELECT 'Hello from pooled connection!'"
        .query[String]
        .to[Option]
        .readOnly(Connector.fromDataSource(pool))
        .flatMap(IO.println)
    }
```

## Configuring Pool Settings

`ConnectionPoolConfig` offers extensive configuration options for fine-tuning your connection pool:

```scala 3
val advancedConfig = ConnectionPoolConfig(
  // Pool size management
  minConnections         = 10,              // Minimum idle connections
  maxConnections         = 50,              // Maximum total connections

  // Timeout configuration
  connectionTimeout      = 30.seconds,      // Max wait for a connection
  validationTimeout      = 5.seconds,       // Connection validation timeout
  idleTimeout            = 10.minutes,      // Remove idle connections after
  maxLifetime            = 30.minutes,      // Maximum connection lifetime

  // Health checks
  keepaliveTime          = Some(2.minutes), // Validate idle connections every

  // Advanced features
  leakDetectionThreshold = Some(2.minutes), // Warn about leaked connections
  adaptiveSizing         = true,            // Dynamic pool sizing
  adaptiveInterval       = 1.minute         // Check pool size every
)

// Pass a custom validation query as an argument to fromDataSource:
//   PooledDataSource.fromDataSource[IO](advancedConfig, datasource, connectionTestQuery = Some("SELECT 1"))
```

## Using with Resource Safety

The pooled data source is managed as a `Resource`, ensuring proper cleanup:

```scala 3
import cats.effect.*

import ldbc.dsl.*
import ldbc.pool.PooledDataSource

// Receive the pool as a Connector and run queries with the DSL
def processUsers(pool: PooledDataSource[IO]): IO[List[String]] =
  sql"SELECT name FROM users"
    .query[String]
    .to[List]
    .readOnly(Connector.fromDataSource(pool))

// Usage (the Resource reliably releases the pool)
val result = PooledDataSource.fromDataSource[IO](poolConfig, datasource).use { pool =>
  processUsers(pool)
}
```

## Monitoring Pool Health

Track your pool's performance with built-in metrics:

```scala 3
import ldbc.dsl.*
import ldbc.pool.*

val monitoredPool = for
  tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
  pool    <- PooledDataSource.fromDataSource[IO](
               poolConfig,
               datasource,
               metricsTracker = Some(tracker)
             )
yield (pool, tracker)

monitoredPool.use { (pool, tracker) =>
  for
    // Use the pool
    _ <- sql"SELECT 1".query[Int].to[Option].readOnly(Connector.fromDataSource(pool))

    // Check metrics
    metrics <- tracker.getMetrics
    _ <- IO.println(s"""
      |Pool Metrics:
      |  Total connections created: ${ metrics.totalCreations }
      |  Total acquisitions:        ${ metrics.totalAcquisitions }
      |  Total releases:            ${ metrics.totalReleases }
      |  Timeouts:                  ${ metrics.timeouts }
      |  Leaks:                     ${ metrics.leaks }
      |  Average acquisition time:  ${ metrics.acquisitionTime }
    """.stripMargin)
  yield ()
}
```

## Connection Lifecycle Hooks

Add custom behavior when connections are acquired or released:

```scala 3
import cats.syntax.all.*

import ldbc.pool.PooledDataSource

case class RequestContext(requestId: String, userId: String)

val poolWithHooks = PooledDataSource.fromDataSourceWithBeforeAfter[IO, RequestContext](
  poolConfig,
  datasource,
  before = { conn =>
    // Set session variables or prepare the connection
    val context = RequestContext("req-123", "user-456")
    conn.createStatement()
      .flatMap(_.executeUpdate(s"SET @request_id = '${ context.requestId }'"))
      .as(context)
  },
  after = { (context, conn) =>
    // Log or cleanup after connection use
    IO.println(s"Connection released for request: ${ context.requestId }")
  }
)
```

## Key Features

### Built-in Circuit Breaker
Protects against database failures by failing fast when the database is down:
- Automatically opens after 5 consecutive failures
- Waits 30 seconds before attempting reconnection
- Uses exponential backoff for repeated failures

### Fiber-Optimized
Designed for Cats Effect's lightweight fibers:
- Minimal memory overhead (~150 bytes per fiber vs 1-2MB per thread)
- Non-blocking connection acquisition
- Excellent performance under high concurrency

### Comprehensive Validation
- Automatic connection health checks
- Configurable validation queries
- Idle connection keepalive
- Connection leak detection

## Migration from JDBC/HikariCP

If you're migrating from HikariCP, here's a comparison:

```scala 3
// HikariCP configuration
val hikariConfig = new HikariConfig()
hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3306/testdb")
hikariConfig.setUsername("root")
hikariConfig.setPassword("password")
hikariConfig.setMaximumPoolSize(20)
hikariConfig.setMinimumIdle(5)
hikariConfig.setConnectionTimeout(30000)

// Equivalent ldbc (ldbc-mysql + ldbc-pool) configuration
val ldbcDataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("testdb")

val ldbcPoolConfig = ConnectionPoolConfig(
  minConnections    = 5,
  maxConnections    = 20,
  connectionTimeout = 30.seconds
)

// PooledDataSource.fromDataSource[IO](ldbcPoolConfig, ldbcDataSource)
```

## Best Practices

1. **Start with defaults**: The default configuration works well for most applications
2. **Monitor your pool**: Use metrics tracking to understand your actual usage patterns
3. **Set appropriate timeouts**: Configure based on your application's SLA requirements
4. **Enable leak detection**: In development/staging to catch connection leaks early
5. **Use lifecycle hooks**: For request tracing or session configuration

## References
- [Connection Pooling](/en/tutorial/Connection.md#connection-pooling)
- [Connection Pooling Architecture](/en/reference/Pooling.md)
- [Performance Benchmarks](/en/reference/Pooling.md#benchmark-results)
