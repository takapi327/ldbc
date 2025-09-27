{%
  laika.title = Connection
  laika.metadata.language = en
%}

# Connection

We've completed the [Setup](/en/tutorial/Setup.md) in the previous page. In this page, we'll learn in detail how to safely connect to databases.

In ldbc, the concept of "connection" plays a central role in managing database connections. A connection provides resources to establish a connection to a database, execute queries, and safely close the connection. This connection management is performed safely using the Resource type from cats-effect.

## Types of Connectors

ldbc supports two types of connection methods.

**jdbc connector** - Connection method using standard JDBC drivers

- Can utilize existing JDBC drivers as they are
- Familiar to developers who are used to JDBC
- Easy integration with other JDBC-based tools

**ldbc connector** - Dedicated connector optimized by ldbc

- Performance optimized for MySQL protocol
- High functional extensibility
- More configuration options available

Let's take a closer look at each connection method.

## Using the JDBC Connector

The JDBC connector establishes connections using standard JDBC drivers. It's recommended when you want to leverage existing JDBC knowledge or prioritize compatibility with other JDBC-based tools.

### Adding Dependencies

First, add the necessary dependencies. When using the jdbc connector, you need to add the MySQL connector as well.

```scala
//> dep "@ORGANIZATION@::jdbc-connector:@VERSION@"
//> dep "com.mysql":"mysql-connector-j":"@MYSQL_VERSION@"
```

### Connection using DataSource

The most common method is using a `DataSource`. This allows advanced features such as connection pooling.

```scala
// Required imports
import cats.effect.IO
import jdbc.connector.*

// Set up MySQL data source
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")

// Create a datasource
val datasource = MySQLDataSource
  .fromDataSource[IO](ds, ExecutionContexts.synchronous)

// Use the connection
val program = datasource.getConnection.use { connection =>
  connection.execute("SELECT 1")
}
```

### Connection using DriverManager

You can also connect using the `DriverManager`. This is convenient for simple applications or script execution.

```scala
// Required imports
import cats.effect.IO
import jdbc.connector.*

// Create a provider from DriverManager
val datasource = MySQLDataSource
  .fromDriverManager[IO]
  .apply(
    "com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://127.0.0.1:13306/world",
    "ldbc",
    "password",
    None // Log handler is optional
  )

// Use the connection
val program = datasource.getConnection.use { connection =>
  connection.execute("SELECT 1")
}
```

### Using Existing Connection

If you already have an established `java.sql.Connection` object, you can wrap and use it:

```scala
// Existing java.sql.Connection
val jdbcConnection: java.sql.Connection = ???

// Convert to ldbc connection
val datasource = MySQLDataSource.fromConnection[IO](jdbcConnection)

// Use the connection
val program = datasource.getConnection.use { connection =>
  connection.execute("SELECT 1")
}
```

## Using the ldbc Connector

The ldbc connector is an optimized connector developed by ldbc, offering more configuration options and flexibility.

### Adding Dependencies

First, add the necessary dependency.

```scala
//> dep "@ORGANIZATION@::ldbc-connector:@VERSION@"
```

### Connection with Basic Configuration

Let's start with the simplest configuration:

```scala
import cats.effect.IO
import ldbc.connector.*

// Create a provider with basic configuration
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")

// Use the connection
val program = datasource.getConnection.use { connection =>
  connection.execute("SELECT 1")
}
```

### Connection with SSL Configuration

You can add SSL configuration to establish a secure connection:

※ Note that Trusted accepts all certificates. This is a setting for development environments.

```scala
import cats.effect.IO
import ldbc.connector.*

val datasource = MySQLDataSource
  .default[IO]("localhost", 3306, "ldbc", "password", "world")
  .setSSL(SSL.Trusted) // Enable SSL connection

// Use the connection
val program = datasource.getConnection.use { connection =>
  connection.execute("SELECT 1")
}
```

ldbc supports all TLS modes provided by fs2. Below is a list of available SSL modes:

| Mode                           | Platform        | Details                                                                                                                                    |
|--------------------------------|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `SSL.None`                     | `JVM/JS/Native` | `ldbc will not request SSL. This is the default.`                                                                                          |
| `SSL.Trusted`                  | `JVM/JS/Native` | `Connect via SSL and trust all certificates.` `Use this if you're running with a self-signed certificate, for instance.`                   |
| `SSL.System`                   | `JVM/JS/Native` | `Connect via SSL and use the system default SSLContext to verify certificates.` `Use this if you're running with a CA-signed certificate.` |
| `SSL.fromSSLContext(…)`	       | `JVM`           | `Connect via SSL using an existing SSLContext.`                                                                                            |
| `SSL.fromKeyStoreFile(…)`	     | `JVM`           | `Connect via SSL using a specified keystore file.`                                                                                         |
| `SSL.fromKeyStoreResource(…)`	 | `JVM`           | `Connect via SSL using a specified keystore classpath resource.`                                                                           |
| `SSL.fromKeyStore(…)`	         | `JVM`           | `Connect via SSL using an existing Keystore.`                                                                                              |
| `SSL.fromSecureContext(...)`   | `JS`            | `Connect via SSL using an existing SecureContext.`                                                                                         |
| `SSL.fromS2nConfig(...)`       | `Native`        | `Connect via SSL using an existing S2nConfig.`                                                                                             |

### Connection with Advanced Configuration

You can leverage many more configuration options:

```scala
import scala.concurrent.duration.*
import cats.effect.IO
import fs2.io.net.SocketOption
import ldbc.connector.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setDebug(true)
  .setSSL(SSL.None)
  .addSocketOption(SocketOption.receiveBufferSize(4096))
  .setReadTimeout(30.seconds)
  .setAllowPublicKeyRetrieval(true)
  .setLogHandler(customLogHandler) // Set custom log handler

// Use the connection
val program = datasource.getConnection.use { connection =>
  connection.execute("SELECT 1")
}
```

### Adding Before/After Processing

If you want to execute specific processing after establishing a connection or before disconnecting, you can use the `withBefore` and `withAfter` methods:

```scala
import cats.effect.IO
import ldbc.connector.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .withBefore { connection =>
    // Processing executed after connection establishment
    connection.execute("SET time_zone = '+09:00'")
  }
  .withAfter { (result, connection) =>
    // Processing executed before disconnection
    connection.execute("RESET time_zone")
  }

// Use the connection
val program = datasource.getConnection.use { connection =>
  connection.execute("SELECT NOW()")
}
```

## List of Configurable Parameters

The following parameters can be configured with the ldbc connector:

| Property                  | Details                                                               | Required |
|---------------------------|-----------------------------------------------------------------------|----|
| `host`                    | `Database host information`                                            | ✅  |
| `port`                    | `Database port information`                                            | ✅  |
| `user`                    | `Database user information`                                            | ✅  |
| `password`                | `Database password information (default: None)`                         | ❌  |
| `database`                | `Database name information (default: None)`                             | ❌  |
| `debug`                   | `Whether to display debug information (default: false)`                 | ❌  |
| `ssl`                     | `SSL configuration (default: SSL.None)`                                 | ❌  |
| `socketOptions`           | `Specify socket options for TCP/UDP sockets (default: defaultSocketOptions)` | ❌  |
| `readTimeout`             | `Specify timeout duration (default: Duration.Inf)`                      | ❌  |
| `allowPublicKeyRetrieval` | `Whether to retrieve public key (default: false)`                       | ❌  |
| `logHandler`              | `Log output configuration`                                              | ❌  |
| `before`                  | `Processing to execute after connection establishment`                  | ❌  |
| `after`                   | `Processing to execute before disconnecting`                            | ❌  |
| `tracer`                  | `Tracer configuration for metrics output (default: Tracer.noop)`        | ❌  |

## Resource Management and Connection Usage

ldbc manages connection lifecycles using cats-effect's Resource. You can use connections in the following two ways:

### use Method

The `use` method is convenient for simple usage:

```scala
val result = datasource.getConnection.use { connection =>
  // Processing using the connection
  connection.execute("SELECT * FROM users")
}
```

### getConnection Method

For more detailed resource management, use the `getConnection` method:

```scala 3
val program = for
  result <- provider.getConnection().use { connection =>
    // Processing using the connection
    connection.execute("SELECT * FROM users")
  }
  // Other processing...
yield result
```

Using these methods, you can perform database operations while safely managing the opening/closing of connections.

## Connection Pooling

Starting from version 0.4.0, ldbc-connector provides built-in connection pooling capabilities. Connection pooling is essential for production applications as it significantly improves performance by reusing existing database connections instead of creating new ones for each request.

@:callout(warning)

**Limitations with Scala Native 0.4.x**

The current Scala Native 0.4.x only supports single-threaded execution. Since the connection pooling functionality in ldbc-connector is designed with multi-threading in mind, when using it with Scala Native 0.4.x, connection pooling may not work correctly.

Scala Native 0.5.x is planned to support multi-threading, but until then, using connection pooling with Scala Native is not recommended. Instead, we recommend creating and using a new connection for each database operation.

@:@

### Why Use Connection Pooling?

Creating a new database connection is an expensive operation that involves:
- Network round trips for TCP handshake
- MySQL authentication protocol exchange
- SSL/TLS negotiation (if enabled)
- Server resource allocation

Connection pooling eliminates this overhead by maintaining a pool of reusable connections, resulting in:
- **Improved Performance**: Connections are reused, eliminating connection establishment overhead
- **Better Resource Management**: Limits the number of concurrent connections to the database
- **Enhanced Reliability**: Built-in health checks ensure only healthy connections are used
- **Automatic Recovery**: Failed connections are automatically replaced

### Creating a Connection Pool

To create a pooled data source with ldbc connector:

```scala
import cats.effect.IO
import ldbc.connector.*
import scala.concurrent.duration.*

// Basic pool configuration
val poolConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  // Pool-specific settings
  .setMinConnections(5)          // Keep at least 5 connections ready
  .setMaxConnections(20)         // Maximum 20 connections
  .setConnectionTimeout(30.seconds)  // Wait up to 30 seconds for a connection

// Create the pooled data source
MySQLDataSource.pooling[IO](poolConfig).use { pool =>
  // Use connections from the pool
  pool.getConnection.use { connection =>
    connection.execute("SELECT 1")
  }
}
```

### Pool Configuration Options

The ldbc connection pool offers extensive configuration options:

#### Pool Size Settings
- **minConnections**: Minimum number of idle connections to maintain (default: 5)
- **maxConnections**: Maximum total connections allowed (default: 20)

#### Timeout Configuration
- **connectionTimeout**: Maximum time to wait for a connection from the pool (default: 30 seconds)
- **idleTimeout**: Time before idle connections are closed (default: 10 minutes)
- **maxLifetime**: Maximum lifetime of a connection before replacement (default: 30 minutes)
- **validationTimeout**: Timeout for connection validation queries (default: 5 seconds)

#### Health and Validation
- **keepaliveTime**: Interval for validating idle connections (optional)
- **connectionTestQuery**: Custom query for connection validation (optional, defaults to using MySQL's isValid)
- **aliveBypassWindow**: Skip validation for recently used connections (default: 500ms)

#### Advanced Features
- **leakDetectionThreshold**: Warn about connections not returned to pool (optional)
- **adaptiveSizing**: Enable dynamic pool sizing based on load (default: true)
- **adaptiveInterval**: How often to check and adjust pool size (default: 30 seconds)

#### Logging Configuration
- **logPoolState**: Enable periodic logging of pool state (default: false)
- **poolStateLogInterval**: Interval for pool state logging (default: 30 seconds)
- **poolName**: Pool identification name for logging (default: "ldbc-pool")

### Example with Advanced Configuration

```scala
import cats.effect.IO
import ldbc.connector.*
import scala.concurrent.duration.*

val advancedConfig = MySQLConfig.default
  .setHost("production-db.example.com")
  .setPort(3306)
  .setUser("app_user")
  .setPassword("secure_password")
  .setDatabase("production_db")
  
  // Pool Size Management
  .setMinConnections(10)          // Keep 10 connections ready
  .setMaxConnections(50)          // Scale up to 50 connections
  
  // Timeout Configuration
  .setConnectionTimeout(30.seconds)   // Max wait for connection
  .setIdleTimeout(10.minutes)        // Remove idle connections after
  .setMaxLifetime(30.minutes)        // Replace connections after
  .setValidationTimeout(5.seconds)   // Connection validation timeout
  
  // Health Checks
  .setKeepaliveTime(2.minutes)       // Validate idle connections every 2 minutes
  .setConnectionTestQuery("SELECT 1") // Custom validation query
  
  // Advanced Features
  .setLeakDetectionThreshold(2.minutes)  // Warn about leaked connections
  .setAdaptiveSizing(true)              // Enable dynamic pool sizing
  .setAdaptiveInterval(1.minute)        // Check pool size every minute
  
  // Logging Configuration
  .setLogPoolState(true)                // Enable pool state logging
  .setPoolStateLogInterval(1.minute)    // Log pool state every minute
  .setPoolName("production-pool")       // Set pool name

// Create and use the pool
MySQLDataSource.pooling[IO](advancedConfig).use { pool =>
  // Your application code
}
```

### Connection Lifecycle Hooks with Pooling

You can add custom logic that executes when connections are acquired from or returned to the pool:

```scala
case class RequestContext(requestId: String, userId: String)

// Define hooks
val beforeHook: Connection[IO] => IO[RequestContext] = conn =>
  for {
    context <- IO(RequestContext("req-123", "user-456"))
    _ <- conn.createStatement()
          .flatMap(_.executeUpdate(s"SET @request_id = '${context.requestId}'"))
  } yield context

val afterHook: (RequestContext, Connection[IO]) => IO[Unit] = (context, conn) =>
  IO.println(s"Connection released for request: ${context.requestId}")

// Create pool with hooks
MySQLDataSource.poolingWithBeforeAfter[IO, RequestContext](
  config = poolConfig,
  before = Some(beforeHook),
  after = Some(afterHook)
).use { pool =>
  pool.getConnection.use { conn =>
    // Connection has session variables set
    conn.execute("SELECT @request_id")
  }
}
```

### Monitoring Pool Health

Track your pool's performance with built-in metrics:

```scala
import ldbc.connector.pool.*

// Create pool with metrics tracking
val monitoredPool = for {
  tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
  pool    <- MySQLDataSource.pooling[IO](
    config,
    metricsTracker = Some(tracker)
  )
} yield (pool, tracker)

monitoredPool.use { (pool, tracker) =>
  for {
    // Use the pool
    _ <- pool.getConnection.use { conn =>
      conn.execute("SELECT * FROM users")
    }
    
    // Check metrics
    metrics <- tracker.getMetrics
    _ <- IO.println(s"""
      |Pool Metrics:
      |  Total connections created: ${metrics.totalCreated}
      |  Active connections: ${metrics.activeConnections}
      |  Idle connections: ${metrics.idleConnections}
      |  Waiting requests: ${metrics.waitingRequests}
      |  Average wait time: ${metrics.averageAcquisitionTime}ms
    """.stripMargin)
  } yield ()
}
```

### Enabling Pool State Logging

ldbc-connector provides detailed pool state logging influenced by HikariCP. This allows you to visualize pool behavior and diagnose performance issues:

```scala
import cats.effect.IO
import ldbc.connector.*
import scala.concurrent.duration.*

// Configuration with pool logging enabled
val loggedPoolConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  .setMinConnections(5)
  .setMaxConnections(20)
  // Logging configuration
  .setLogPoolState(true)                  // Enable pool state logging
  .setPoolStateLogInterval(30.seconds)     // Log every 30 seconds
  .setPoolName("app-pool")                 // Name to identify pool in logs

// Create the pool
MySQLDataSource.pooling[IO](loggedPoolConfig).use { pool =>
  // Use the pool - logs like the following will be output every 30 seconds:
  // [INFO] app-pool - Stats (total=5, active=2, idle=3, waiting=0)
  pool.getConnection.use { conn =>
    conn.execute("SELECT * FROM users")
  }
}
```

When pool logging is enabled, the following information is recorded in the logs:

```
// Periodic pool state logs
[INFO] app-pool - Stats (total=10, active=3, idle=7, waiting=0)

// Connection creation failure
[ERROR] Failed to create connection to localhost:3306 (database: mydb): Connection refused

// Connection acquisition timeout (with detailed diagnostics)
[ERROR] Connection acquisition timeout after 30 seconds (host: localhost:3306, db: mydb, pool: 20/20, active: 20, idle: 0, waiting: 5)

// Connection validation failure
[WARN] Connection conn-123 failed validation, removing from pool

// Connection leak detection
[WARN] Possible connection leak detected: Connection conn-456 has been in use for longer than 2 minutes
```

### Pool Architecture Features

The ldbc connection pool includes several advanced features:

#### Circuit Breaker Protection
Prevents connection storms during database failures:
- Automatically opens after consecutive failures
- Uses exponential backoff for retry attempts
- Protects both your application and database

#### Lock-Free Design
Uses a ConcurrentBag data structure for high-performance:
- Minimal contention under high concurrency
- Thread-local optimization for connection reuse
- Excellent scalability characteristics

#### Adaptive Pool Sizing
Dynamically adjusts pool size based on load:
- Grows the pool during high demand
- Shrinks during low usage periods
- Prevents resource waste

#### Detailed Pool Logging
Comprehensive logging system influenced by HikariCP:
- **Pool State Logs**: Periodically outputs connection counts, active/idle connections, and wait queue size
- **Connection Lifecycle Logs**: Records detailed information during connection creation, validation, and removal
- **Error Diagnostics**: Outputs detailed pool state on connection acquisition timeout
- **Leak Detection Logs**: Warns about connections in use beyond configured threshold

### Best Practices

1. **Start with Conservative Settings**: Begin with default values and adjust based on monitoring
2. **Monitor Pool Metrics**: Use metrics to understand your actual usage patterns
3. **Set Appropriate Timeouts**: Balance between user experience and resource protection
4. **Enable Leak Detection in Development**: Catch connection leaks early
5. **Use Connection Test Queries Sparingly**: They add overhead; rely on MySQL's isValid when possible
6. **Enable Pool Logging in Production**: For troubleshooting and performance analysis
7. **Consider Your Workload**: 
   - High-throughput applications: larger pool sizes
   - Bursty workloads: enable adaptive sizing
   - Long-running queries: increase connection timeout
   - Debugging and troubleshooting: enable pool logging to quickly identify issues

### Migration from Non-Pooled Connections

Migrating to pooled connections is straightforward:

```scala
// Before: Direct connection
val dataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")

// After: Pooled connection
val dataSource = MySQLDataSource.pooling[IO](
  MySQLConfig.default
    .setHost("localhost")
    .setPort(3306)
    .setUser("user")
    .setPassword("password")
    .setDatabase("mydb")
    .setMinConnections(5)
    .setMaxConnections(20)
)
```

The API remains the same - you still use `getConnection` to obtain connections. The pool handles all the complexity behind the scenes.

For more detailed information about the connection pooling architecture and implementation details, see the [Connection Pooling Architecture](/en/reference/Pooling.md) reference documentation.
