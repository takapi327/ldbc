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
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

### Connection using DriverManager

You can also connect using the `DriverManager`. This is convenient for simple applications or script execution.

```scala
// Required imports
import cats.effect.IO
import jdbc.connector.*

// Create a datasource from DriverManager
val datasource = MySQLDataSource
  .fromDriverManager[IO](
    "com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://127.0.0.1:13306/world",
    "ldbc",
    "password"
  )

// Use the connection
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
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
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

## Using the ldbc Connector

The ldbc connector is an optimized connector developed by ldbc, offering more configuration options and flexibility.

### Adding Dependencies

First, add the necessary dependency.

```scala
//> dep "@ORGANIZATION@::ldbc-mysql:@VERSION@"
//> dep "@ORGANIZATION@::ldbc-cats-effect:@VERSION@"
```

### Connection with Basic Configuration

Let's start with the simplest configuration:

```scala
import cats.effect.IO
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*

// Create a datasource with basic configuration
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")

// Use the connection
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

### Connection with SSL Configuration

You can add SSL configuration to establish a secure connection:

※ Note that Trusted accepts all certificates. This is a setting for development environments.

※ For security reasons, SSL/TLS connections are required for certain authentication plugins, such as MySQL cleartext pluggable authentication. For details, see the [authentication section of the reference](/en/reference/Connector.md#authentication).

```scala
import cats.effect.IO
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted) // Development / self-signed only — see the note below

// Use the connection
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

> **⚠️ Security note:** `SSL.Trusted` trusts **all** certificates — it encrypts the connection but does **not** verify the server, so it does not protect against man-in-the-middle attacks. Use it only for development or self-signed certificates. **In production, use `SSL.System`** (a CA-signed certificate verified against the system trust store).

ldbc supports all TLS modes provided by fs2. Below is a list of available SSL modes:

| Mode                           | Platform        | Details                                                                                                                                    |
|--------------------------------|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `SSL.None`                     | `JVM/JS/Native` | `ldbc will not request SSL. This is the default.`                                                                                          |
| `SSL.Trusted`                  | `JVM/JS/Native` | `Connect via SSL and trust ALL certificates — no verification, not protected against man-in-the-middle attacks. Development / self-signed only.` |
| `SSL.System`                   | `JVM/JS/Native` | `Connect via SSL and verify the server certificate against the system trust store. Recommended for production (CA-signed certificate).` |
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
import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.{ SocketOptions, SSL }
import ldbc.catseffect.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setDebug(true)
  .setSSL(SSL.None)
  .setSocketOptions(SocketOptions.default.copy(receiveBufferSize = Some(4096)))
  .setReadTimeout(30.seconds)
  .setAllowPublicKeyRetrieval(true)

// Pass the LogHandler when creating the Connector
val connector = Connector.fromDataSource(datasource, customLogHandler)

// Use the connection
val program = sql"SELECT 1".query[Int].to[Option].readOnly(connector)
```

### Adding Before/After Processing

If you want to execute specific processing after establishing a connection or before disconnecting, you can use the `withBefore` and `withAfter` methods:

```scala
import cats.effect.IO
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.mysql.syntax.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .withBefore { connection =>
    // Processing executed after connection establishment
    connection.createStatement().flatMap(_.executeUpdate("SET time_zone = '+09:00'"))
  }
  .withAfter { (result, connection) =>
    // Processing executed before disconnection
    connection.createStatement().flatMap(_.executeUpdate("RESET time_zone")).void
  }

// Use the connection
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT NOW()"))
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
import ldbc.mysql.syntax.*

val result = datasource.use { conn =>
  // Processing using the connection
  conn.createStatement().flatMap(_.executeQuery("SELECT * FROM users"))
}
```

### getConnection Method

For more detailed resource management, use the `getConnection` method. `getConnection` returns a pair of the connection and its release action `(Connection[F], F[Unit])`, so you can handle acquisition and release explicitly:

```scala 3
val program = datasource.getConnection.flatMap { (connection, release) =>
  connection
    .createStatement()
    .flatMap(_.executeQuery("SELECT * FROM users"))
    .guarantee(release)
}
```

Using these methods, you can perform database operations while safely managing the opening/closing of connections.

## Connection Pooling

In 0.9.x, connection pooling is provided as a separate module, `ldbc-pool`. Used together with the `ldbc-mysql` driver, it significantly improves performance by reusing existing database connections instead of creating a new one for each request. It is an essential feature for production applications.

`ldbc-pool` is implemented in an effect-agnostic way (`F: Concurrent`), so it can be used with Cats Effect (`IO`), ZIO (`Task`), or `Fx`. The examples below use Cats Effect. Add `ldbc-mysql`, the effect bridge (`ldbc-cats-effect`), and `ldbc-pool` to your dependencies.

```scala
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-mysql"       % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-cats-effect" % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-pool"        % "@VERSION@"
)
```

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

Prepare a `MySQLDataSource` holding the connection information and a `ConnectionPoolConfig` defining the pool behavior, then create the pool with `PooledDataSource.fromDataSource`.

```scala
import cats.effect.IO
import scala.concurrent.duration.*

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }

// Connection information
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  .setSSL(SSL.Trusted)

// Basic pool configuration
val poolConfig = ConnectionPoolConfig(
  minConnections    = 5,             // Keep at least 5 connections ready
  maxConnections    = 20,            // Maximum 20 connections
  connectionTimeout = 30.seconds     // Wait up to 30 seconds for a connection
)

// Create the pooled data source
PooledDataSource.fromDataSource[IO](poolConfig, datasource).use { pool =>
  // Use the pool as a Connector
  val connector = Connector.fromDataSource(pool)
  sql"SELECT 1".query[Int].to[Option].readOnly(connector)
}
```

### Pool Configuration Options

`ConnectionPoolConfig` offers extensive configuration options (field names and default values):

#### Pool Size Settings
- **minConnections**: Minimum number of idle connections to maintain (required)
- **maxConnections**: Maximum total connections allowed (required)

#### Timeout Configuration
- **connectionTimeout**: Maximum time to wait for a connection from the pool (default: 30 seconds)
- **validationTimeout**: Timeout for connection validation queries (default: 5 seconds)
- **idleTimeout**: Time before idle connections are closed (default: 10 minutes)
- **maxLifetime**: Maximum lifetime of a connection before replacement (default: 30 minutes)

#### Health and Validation
- **keepaliveTime**: Interval for validating idle connections (`Option`, default: none)
- **aliveBypassWindow**: Skip validation for recently used connections (default: 500ms)

#### Advanced Features
- **leakDetectionThreshold**: Warn about connections not returned to the pool (`Option`, default: none)
- **adaptiveSizing**: Enable dynamic pool sizing based on load (default: false)
- **adaptiveInterval**: How often to check and adjust pool size (default: 30 seconds)
- **maintenanceInterval**: How often background maintenance tasks run (default: 30 seconds)

#### Other
- **poolName**: Pool identification name for logging (default: "ldbc-pool")
- **debug**: Enable debug logging (default: false)

> A custom connection validation query (`connectionTestQuery`) and pool logging (`poolLogger`) are passed as arguments to `PooledDataSource.fromDataSource` / `fromConfig`, not to `ConnectionPoolConfig` (see below).

### Example with Advanced Configuration

```scala
import cats.effect.IO
import scala.concurrent.duration.*

import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource, PoolLogger }

// Connection information
val datasource = MySQLDataSource
  .build[IO]("production-db.example.com", 3306, "app_user")
  .setPassword("secure_password")
  .setDatabase("production_db")
  .setSSL(SSL.Trusted)

// Advanced pool configuration
val advancedConfig = ConnectionPoolConfig(
  // Pool size management
  minConnections         = 10,             // Keep 10 connections ready
  maxConnections         = 50,             // Scale up to 50 connections

  // Timeout configuration
  connectionTimeout      = 30.seconds,     // Max wait for a connection
  validationTimeout      = 5.seconds,      // Connection validation timeout
  idleTimeout            = 10.minutes,     // Remove idle connections after
  maxLifetime            = 30.minutes,     // Replace connections after

  // Health checks
  keepaliveTime          = Some(2.minutes),  // Validate idle connections every 2 minutes

  // Advanced features
  leakDetectionThreshold = Some(2.minutes),  // Warn about leaked connections
  adaptiveSizing         = true,             // Enable dynamic pool sizing
  adaptiveInterval       = 1.minute,         // Check pool size every minute
  poolName               = "production-pool" // Pool name for logging
)

// Create and use the pool (pass the custom validation query and logger as fromDataSource arguments)
PooledDataSource
  .fromDataSource[IO](
    advancedConfig,
    datasource,
    connectionTestQuery = Some("SELECT 1"),
    poolLogger          = Some(PoolLogger.console[IO]())
  )
  .use { pool =>
    // Your application code
    IO.unit
  }
```

### Connection Lifecycle Hooks with Pooling

You can add custom logic that executes when connections are acquired from or returned to the pool:

```scala
import ldbc.sql.Connection

case class RequestContext(requestId: String, userId: String)

// Define hooks (after receives the result type produced by before)
val beforeHook: Connection[IO] => IO[RequestContext] = conn =>
  for
    context <- IO(RequestContext("req-123", "user-456"))
    _       <- conn.createStatement()
                 .flatMap(_.executeUpdate(s"SET @request_id = '${ context.requestId }'"))
  yield context

val afterHook: (RequestContext, Connection[IO]) => IO[Unit] = (context, conn) =>
  IO.println(s"Connection released for request: ${ context.requestId }")

// Create pool with hooks
PooledDataSource
  .fromDataSourceWithBeforeAfter[IO, RequestContext](
    poolConfig,
    datasource,
    before = beforeHook,
    after  = afterHook
  )
  .use { pool =>
    // The connection has session variables set
    sql"SELECT @request_id".query[String].to[Option].readOnly(Connector.fromDataSource(pool))
  }
```

### Monitoring Pool Health

Track your pool's performance with built-in metrics:

```scala
import ldbc.pool.*

// Create pool with metrics tracking
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
    _       <- sql"SELECT * FROM users".query[String].to[List].readOnly(Connector.fromDataSource(pool))

    // Check metrics
    metrics <- tracker.getMetrics
    _       <- IO.println(s"""
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

### Enabling Pool State Logging

`ldbc-pool` provides detailed pool state logging influenced by HikariCP. This allows you to visualize pool behavior and diagnose performance issues. Enable logging by passing a `PoolLogger` to the `poolLogger` argument, and specify the pool name via `poolName` in `ConnectionPoolConfig`:

```scala
import cats.effect.IO
import scala.concurrent.duration.*

import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource, PoolLogger }

// Connection information
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  .setSSL(SSL.Trusted)

// Pool configuration (specify the pool name)
val loggedPoolConfig = ConnectionPoolConfig(
  minConnections = 5,
  maxConnections = 20,
  poolName       = "app-pool"  // Name to identify the pool in logs
)

// Create the pool passing a PoolLogger
PooledDataSource
  .fromDataSource[IO](loggedPoolConfig, datasource, poolLogger = Some(PoolLogger.console[IO]()))
  .use { pool =>
    // When you use the pool, logs like the following are output:
    // [INFO] app-pool - Stats (total=5, active=2, idle=3, waiting=0)
    sql"SELECT * FROM users".query[String].to[List].readOnly(Connector.fromDataSource(pool))
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
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")

// After: wrap the same datasource with PooledDataSource
val pool = PooledDataSource.fromDataSource[IO](
  ConnectionPoolConfig(minConnections = 5, maxConnections = 20),
  datasource
)
```

The connection information (`MySQLDataSource`) can be reused as is, and the pool behavior is specified via `ConnectionPoolConfig`. Since `PooledDataSource` is a `DataSource`, you pass it to `Connector.fromDataSource` exactly as you would for a direct connection. The pool handles all the complexity behind the scenes.

For more detailed information about the connection pooling architecture and implementation details, see the [Connection Pooling Architecture](/en/reference/Pooling.md) reference documentation.
