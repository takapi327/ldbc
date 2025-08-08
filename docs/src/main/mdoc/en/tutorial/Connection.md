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

// Create a connection provider
val provider = ConnectionProvider
  .fromDataSource[IO](ds, ExecutionContexts.synchronous)

// Use the connection
val program = provider.use { connection =>
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
val provider = ConnectionProvider
  .fromDriverManager[IO]
  .apply(
    "com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://127.0.0.1:13306/world",
    "ldbc",
    "password",
    None // Log handler is optional
  )

// Use the connection
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### Using Existing Connection

If you already have an established `java.sql.Connection` object, you can wrap and use it:

```scala
// Existing java.sql.Connection
val jdbcConnection: java.sql.Connection = ???

// Convert to ldbc connection
val provider = ConnectionProvider.fromConnection[IO](jdbcConnection)

// Use the connection
val program = provider.use { connection =>
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
val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")

// Use the connection
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### Connection with SSL Configuration

You can add SSL configuration to establish a secure connection:

※ Note that Trusted accepts all certificates. This is a setting for development environments.

```scala
import cats.effect.IO
import ldbc.connector.*

val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "ldbc", "password", "world")
  .setSSL(SSL.Trusted) // Enable SSL connection

// Use the connection
val program = provider.use { connection =>
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

val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setDebug(true)
  .setSSL(SSL.None)
  .addSocketOption(SocketOption.receiveBufferSize(4096))
  .setReadTimeout(30.seconds)
  .setAllowPublicKeyRetrieval(true)
  .setLogHandler(customLogHandler) // Set custom log handler

// Use the connection
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### Adding Before/After Processing

If you want to execute specific processing after establishing a connection or before disconnecting, you can use the `withBefore` and `withAfter` methods:

```scala
import cats.effect.IO
import ldbc.connector.*

val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "ldbc", "password", "world")
  .withBefore { connection =>
    // Processing executed after connection establishment
    connection.execute("SET time_zone = '+09:00'")
  }
  .withAfter { (result, connection) =>
    // Processing executed before disconnection
    connection.execute("RESET time_zone")
  }

// Use the connection
val program = provider.use { connection =>
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
val result = provider.use { connection =>
  // Processing using the connection
  connection.execute("SELECT * FROM users")
}
```

### createConnection Method

For more detailed resource management, use the `createConnection` method:

```scala 3
val program = for
  result <- provider.createConnection().use { connection =>
    // Processing using the connection
    connection.execute("SELECT * FROM users")
  }
  // Other processing...
yield result
```

Using these methods, you can perform database operations while safely managing the opening/closing of connections.
