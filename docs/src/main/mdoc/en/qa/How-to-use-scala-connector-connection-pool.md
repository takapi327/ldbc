{%
  laika.title = "Q: How to use connection pool with Scala connector?"
  laika.metadata.language = en
%}

# Q: How to use connection pool with Scala connector?

## A: ldbc-connector now provides built-in connection pooling support!

Starting from version 0.4.0, ldbc-connector includes comprehensive connection pooling functionality. The pooling system is designed specifically for Cats Effect's fiber-based concurrency model, offering high performance and excellent resource efficiency.

## Quick Start

Here's how to create and use a pooled connection with ldbc-connector:

```scala 3
import cats.effect.*
import ldbc.connector.*
import scala.concurrent.duration.*

object ConnectionPoolExample extends IOApp.Simple:

  val run = 
    // Configure your connection pool
    val poolConfig = MySQLConfig.default
      .setHost("localhost")
      .setPort(3306)
      .setUser("root")
      .setPassword("password")
      .setDatabase("testdb")
      // Pool-specific settings
      .setMinConnections(5)         // Keep at least 5 connections ready
      .setMaxConnections(20)        // Maximum 20 connections
      .setConnectionTimeout(30.seconds)  // Wait up to 30 seconds for a connection
      
    // Create the pooled data source
    MySQLDataSource.pooling[IO](poolConfig).use { pool =>
      // Use connections from the pool
      pool.getConnection.use { connection =>
        for
          stmt   <- connection.createStatement()
          rs     <- stmt.executeQuery("SELECT 'Hello from pooled connection!'")
          _      <- rs.next()
          result <- rs.getString(1)
          _      <- IO.println(result)
        yield ()
      }
    }
```

## Configuring Pool Settings

ldbc-connector offers extensive configuration options for fine-tuning your connection pool:

```scala 3
val advancedConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("myapp")
  .setPassword("secret")
  .setDatabase("production")
  
  // Pool Size Management
  .setMinConnections(10)          // Minimum idle connections
  .setMaxConnections(50)          // Maximum total connections
  
  // Timeout Configuration
  .setConnectionTimeout(30.seconds)   // Max wait for connection
  .setIdleTimeout(10.minutes)        // Remove idle connections after
  .setMaxLifetime(30.minutes)        // Replace connections after
  .setValidationTimeout(5.seconds)   // Connection validation timeout
  
  // Health Checks
  .setKeepaliveTime(2.minutes)       // Validate idle connections every
  .setConnectionTestQuery("SELECT 1") // Custom validation query (optional)
  
  // Advanced Features
  .setLeakDetectionThreshold(2.minutes)  // Warn about leaked connections
  .setAdaptiveSizing(true)              // Dynamic pool sizing
  .setAdaptiveInterval(1.minute)        // Check pool size every
```

## Using with Resource Safety

The pooled data source is managed as a `Resource`, ensuring proper cleanup:

```scala 3
import cats.effect.*
import cats.implicits.*
import ldbc.connector.*

def processUsers[F[_]: Async: Network: Console](
  pool: PooledDataSource[F]
): F[List[String]] =
  pool.getConnection.use { conn =>
    conn.prepareStatement("SELECT name FROM users").use { stmt =>
      stmt.executeQuery().flatMap { rs =>
        // Safely iterate through results
        LazyList.unfold(())(_ => 
          rs.next().map(hasNext => 
            if hasNext then Some((rs.getString("name"), ())) else None
          ).toOption.flatten
        ).compile.toList
      }
    }
  }

// Usage
val result = MySQLDataSource.pooling[IO](config).use { pool =>
  processUsers[IO](pool)
}
```

## Monitoring Pool Health

Track your pool's performance with built-in metrics:

```scala 3
import ldbc.connector.pool.*

val monitoredPool = for
  tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
  pool    <- MySQLDataSource.pooling[IO](
    config,
    metricsTracker = Some(tracker)
  )
yield (pool, tracker)

monitoredPool.use { (pool, tracker) =>
  for
    // Use the pool
    _ <- pool.getConnection.use(conn => /* your queries */ IO.unit)
    
    // Check metrics
    metrics <- tracker.getMetrics
    _ <- IO.println(s"""
      |Pool Metrics:
      |  Total connections created: ${metrics.totalCreated}
      |  Active connections: ${metrics.activeConnections}
      |  Idle connections: ${metrics.idleConnections}
      |  Waiting requests: ${metrics.waitingRequests}
      |  Total acquisitions: ${metrics.totalAcquisitions}
      |  Average wait time: ${metrics.averageAcquisitionTime}ms
    """.stripMargin)
  yield ()
}
```

## Connection Lifecycle Hooks

Add custom behavior when connections are acquired or released:

```scala 3
case class RequestContext(requestId: String, userId: String)

val poolWithHooks = MySQLDataSource.poolingWithBeforeAfter[IO, RequestContext](
  config = config,
  before = Some { conn =>
    // Set session variables or prepare connection
    val context = RequestContext("req-123", "user-456")
    conn.createStatement()
      .flatMap(_.executeUpdate(s"SET @request_id = '${context.requestId}'"))
      .as(context)
  },
  after = Some { (context, conn) =>
    // Log or cleanup after connection use
    IO.println(s"Connection released for request: ${context.requestId}")
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

// Equivalent ldbc-connector configuration
val ldbcConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setDatabase("testdb")
  .setUser("root")
  .setPassword("password")
  .setMaxConnections(20)
  .setMinConnections(5)
  .setConnectionTimeout(30.seconds)
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
