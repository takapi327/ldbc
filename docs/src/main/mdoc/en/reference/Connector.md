{%
  laika.title = Connector
  laika.metadata.language = en
%}

# Connector

This chapter describes database connections using ldbc's own MySQL connector.

To connect to MySQL databases in Scala, you need to use JDBC. JDBC is a standard Java API that can also be used in Scala.
Since JDBC is implemented in Java, when using it in Scala, it can only operate in the JVM environment.

In recent years, the Scala ecosystem has seen active development of plugins to enable operation in environments like JS and Native.
Scala continues to evolve from a language that only operates in JVM environments using Java assets to one that can operate in multi-platform environments.

However, JDBC is a standard Java API and does not support operation in Scala's multi-platform environments.

Therefore, even if you create a Scala application to run in JS, Native, etc., you cannot connect to databases like MySQL because JDBC cannot be used.

The Typelevel Project includes a Scala library for [PostgreSQL](https://www.postgresql.org/) called [Skunk](https://github.com/typelevel/skunk).
This project does not use JDBC and achieves PostgreSQL connections using pure Scala. As a result, you can connect to PostgreSQL regardless of whether you're in JVM, JS, or Native environments when using Skunk.

The ldbc connector is a project being developed to enable MySQL connections regardless of whether you're in JVM, JS, or Native environments, inspired by Skunk.

The ldbc connector is the lowest layer API.
We plan to provide higher-layer APIs using this connector in the future. We also plan to maintain compatibility with existing higher-layer APIs.

To use it, you need to set up the following dependency in your project:

**JVM**

```scala 3
libraryDependencies += "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@"
```

**JS/Native**

```scala 3
libraryDependencies += "@ORGANIZATION@" %%% "ldbc-connector" % "@VERSION@"
```

**Supported Versions**

The current version supports the following versions of MySQL:

- MySQL 5.7.x
- MySQL 8.x

The main support is for MySQL 8.x. MySQL 5.7.x is sub-supported. Therefore, caution is required when operating with MySQL 5.7.x.
In the future, support for MySQL 5.7.x is planned to be discontinued.

## Connection

To connect to MySQL using the ldbc connector, use `MySQLDataSource`.

```scala 3
import cats.effect.IO
import ldbc.connector.MySQLDataSource

val datasource = MySQLDataSource
  .build[IO](
    host = "127.0.0.1",
    port = 3306,
    user = "root",
  )
```

Below is a list of properties that can be set when constructing a `Connection`.

| Property                  | Details                                                                      | Required |
|---------------------------|------------------------------------------------------------------------------|----------|
| `host`                    | `Database host information`                                                  | ✅        |
| `port`                    | `Database port information`                                                  | ✅        |
| `user`                    | `Database user information`                                                  | ✅        |
| `password`                | `Database password information (default: None)`                              | ❌        |
| `database`                | `Database name information (default: None)`                                  | ❌        |
| `debug`                   | `Whether to display debug information (default: false)`                      | ❌        |
| `ssl`                     | `SSL settings (default: SSL.None)`                                           | ❌        |
| `socketOptions`           | `Specify socket options for TCP/UDP sockets (default: defaultSocketOptions)` | ❌        |
| `readTimeout`             | `Specify timeout duration (default: Duration.Inf)`                           | ❌        |
| `allowPublicKeyRetrieval` | `Whether to retrieve public key (default: false)`                            | ❌        |
| `logHandler`              | `Log output settings`                                                        | ❌        |
| `before`                  | `Processing to be executed after connection is established`                  | ❌        |
| `after`                   | `Processing to be executed before disconnecting`                             | ❌        |
| `tracer`                  | `Tracer settings for metrics output (default: Tracer.noop)`                  | ❌        |

Create a `Connector` from `MySQLDataSource` to perform database operations. The `Connector` is an interface that translates the `DBIO` monad into actual database operations.

```scala 3
import ldbc.connector.*

// Connectorを作成
val connector = Connector.fromDataSource(datasource)

// DBIOを実行
sql"SELECT 1".query[Int].unsafe.readOnly(connector)
```

Additionally, if you need to use the low-level Connection API, you can use `getConnection` to obtain a `Resource`:

```scala 3
datasource.getConnection.use { conn =>
  // Connection APIを直接使用
}
```

### Authentication

In MySQL, authentication occurs when the client connects to the MySQL server and sends user information during the LoginRequest phase. The server then searches for the sent user in the `mysql.user` table and determines which authentication plugin to use. After determining the authentication plugin, the server calls the plugin to start user authentication and sends the result to the client. Thus, authentication in MySQL is pluggable (various types of plugins can be attached and detached).

The authentication plugins supported by MySQL are listed on the [official page](https://dev.mysql.com/doc/refman/8.0/en/authentication-plugins.html).

ldbc currently supports the following authentication plugins:

- Native Pluggable Authentication
- SHA-256 Pluggable Authentication
- SHA-2 Pluggable Authentication Cache
- Cleartext Pluggable Authentication

*Note: Native Pluggable Authentication and SHA-256 Pluggable Authentication are deprecated plugins from MySQL 8.x. It is recommended to use the SHA-2 Pluggable Authentication Cache unless there is a specific reason.*

@:callout(warning)
**Important Security Precautions：**
MySQL cleartext pluggable authentication is an authentication method that sends passwords in plain text to the server. When using this authentication plugin, you must enable SSL/TLS connections. Using it without SSL/TLS connections is extremely dangerous from a security standpoint, as passwords are transmitted over the network in plain text.

This authentication plugin is primarily used for integration with AWS IAM authentication and other external authentication systems. For instructions on configuring SSL/TLS connections, refer to the [SSL configuration section of the tutorial](/en/tutorial/Connection.md#connection-with-ssl-configuration).
@:@

You do not need to be aware of authentication plugins in the ldbc application code. Users should create users with the desired authentication plugin on the MySQL database and use those users to attempt connections to MySQL in the ldbc application code.
ldbc internally determines the authentication plugin and connects to MySQL using the appropriate authentication plugin.

## Execution

The following table is assumed to be used in subsequent processes.

```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  age INT NULL
);
```

### Statement

`Statement` is an API for executing SQL without dynamic parameters.

*Note: Since `Statement` does not use dynamic parameters, there is a risk of SQL injection depending on how it is used. Therefore, it is recommended to use `PreparedStatement` when using dynamic parameters.*

Use the `createStatement` method of `Connection` to construct a `Statement`.

#### Read Query

To execute read-only SQL, use the `executeQuery` method.

The values returned from the MySQL server as a result of executing the query are stored in `ResultSet` and returned as the return value.

```scala 3 3
datasource.getConnection.use { conn =>
  for
    statement <- conn.createStatement()
    result <- statement.executeQuery("SELECT * FROM users")
  yield
    // Processing using ResultSet
}
```

#### Write Query

To execute SQL for writing, use the `executeUpdate` method.

The values returned from the MySQL server as a result of executing the query are the number of affected rows returned as the return value.

```scala 3
datasource.getConnection.use { conn =>
  for
    statement <- conn.createStatement()
    result <- statement.executeUpdate("INSERT INTO users (name, age) VALUES ('Alice', 20)")
  yield
}
```

#### Retrieve AUTO_INCREMENT Value

To retrieve the AUTO_INCREMENT value after executing a query using `Statement`, use the `getGeneratedKeys` method.

The values returned from the MySQL server as a result of executing the query are the values generated by AUTO_INCREMENT returned as the return value.

```scala 3
datasource.getConnection.use { conn =>
  for
    statement <- conn.createStatement()
    _ <- statement.executeUpdate("INSERT INTO users (name, age) VALUES ('Alice', 20)", Statement.RETURN_GENERATED_KEYS)
    generatedKeys <- statement.getGeneratedKeys()
  yield
}
```

### Client/Server PreparedStatement

ldbc provides `PreparedStatement` divided into `Client PreparedStatement` and `Server PreparedStatement`.

`Client PreparedStatement` is an API for constructing SQL with dynamic parameters on the application and sending it to the MySQL server.
Therefore, the query sending method to the MySQL server is the same as `Statement`.

This API corresponds to JDBC's `PreparedStatement`.

For a safer way to construct queries within the MySQL server, use `Server PreparedStatement`.

`Server PreparedStatement` is an API for preparing queries to be executed within the MySQL server in advance and setting parameters on the application for execution.

With `Server PreparedStatement`, the query sending and parameter sending are separated, allowing query reuse.

When using `Server PreparedStatement`, prepare the query in advance on the MySQL server. The MySQL server uses memory to store it, but query reuse is possible, leading to performance improvements.

However, the prepared query continues to use memory until it is released, posing a risk of memory leaks.

When using `Server PreparedStatement`, use the `close` method to properly release the query.

#### Client PreparedStatement

Use the `clientPreparedStatement` method of `Connection` to construct a `Client PreparedStatement`.

```scala 3
datasource.getConnection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("SELECT * FROM users WHERE id = ?")
    ...
  yield ...
}
```

#### Server PreparedStatement

Use the `serverPreparedStatement` method of `Connection` to construct a `Server PreparedStatement`.

```scala 3
datasource.getConnection.use { conn =>
  for 
    statement <- conn.serverPreparedStatement("SELECT * FROM users WHERE id = ?")
    ...
  yield ...
}
```

#### Read Query

To execute read-only SQL, use the `executeQuery` method.

The values returned from the MySQL server as a result of executing the query are stored in `ResultSet` and returned as the return value.

```scala 3
datasource.getConnection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("SELECT * FROM users WHERE id = ?") // or conn.serverPreparedStatement("SELECT * FROM users WHERE id = ?")
    _ <- statement.setLong(1, 1)
    result <- statement.executeQuery()
  yield
    // Processing using ResultSet
}
```

When using dynamic parameters, use the `setXXX` method to set the parameters.
The `setXXX` method can also use the `Option` type. If `None` is passed, NULL is set as the parameter.

The `setXXX` method specifies the parameter index and the parameter value.

```scala 3
statement.setLong(1, 1)
```

The following methods are currently supported.

| Method          | Type                                  | Remarks                                             |
|-----------------|---------------------------------------|-----------------------------------------------------|
| `setNull`       |                                       | Sets NULL as the parameter                          |
| `setBoolean`    | `Boolean/Option[Boolean]`             |                                                     |
| `setByte`       | `Byte/Option[Byte]`                   |                                                     |
| `setShort`      | `Short/Option[Short]`                 |                                                     |
| `setInt`        | `Int/Option[Int]`                     |                                                     |
| `setLong`       | `Long/Option[Long]`                   |                                                     |
| `setBigInt`     | `BigInt/Option[BigInt]`               |                                                     |
| `setFloat`      | `Float/Option[Float]`                 |                                                     |
| `setDouble`     | `Double/Option[Double]`               |                                                     |
| `setBigDecimal` | `BigDecimal/Option[BigDecimal]`       |                                                     |
| `setString`     | `String/Option[String]`               |                                                     |
| `setBytes`      | `Array[Byte]/Option[Array[Byte]]`     |                                                     |
| `setDate`       | `LocalDate/Option[LocalDate]`         | Directly handles `java.time` instead of `java.sql`. |
| `setTime`       | `LocalTime/Option[LocalTime]`         | Directly handles `java.time` instead of `java.sql`. |
| `setTimestamp`  | `LocalDateTime/Option[LocalDateTime]` | Directly handles `java.time` instead of `java.sql`. |
| `setYear`       | `Year/Option[Year]`                   | Directly handles `java.time` instead of `java.sql`. |

#### Write Query

To execute SQL for writing, use the `executeUpdate` method.

The values returned from the MySQL server as a result of executing the query are the number of affected rows returned as the return value.

```scala 3
datasource.getConnection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)") // or conn.serverPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
    _ <- statement.setString(1, "Alice")
    _ <- statement.setInt(2, 20)
    result <- statement.executeUpdate()
  yield result
}

```

#### Retrieve AUTO_INCREMENT Value

To retrieve the AUTO_INCREMENT value after executing a query, use the `getGeneratedKeys` method.

The values returned from the MySQL server as a result of executing the query are the values generated by AUTO_INCREMENT returned as the return value.

```scala 3
datasource.getConnection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS) // or conn.serverPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)
    _ <- statement.setString(1, "Alice")
    _ <- statement.setInt(2, 20)
    _ <- statement.executeUpdate()
    getGeneratedKeys <- statement.getGeneratedKeys()
  yield getGeneratedKeys
}
```

### ResultSet

`ResultSet` is an API for storing the values returned from the MySQL server after executing a query.

To retrieve records obtained by executing SQL from `ResultSet`, you can use the `next` method and `getXXX` methods similar to JDBC, or use the ldbc-specific `decode` method.

#### next/getXXX

The `next` method returns `true` if the next record exists, and `false` if the next record does not exist.

The `getXXX` method is an API for retrieving values from records.

The `getXXX` method can specify the column index or the column name to retrieve.

```scala 3
datasource.getConnection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("SELECT `id`, `name`, `age` FROM users WHERE id = ?")
    _ <- statement.setLong(1, 1)
    result <- statement.executeQuery()
  yield
    val builder = List.newBuilder[(Long, String, Int)]
    while resultSet.next() do
      val id = resultSet.getLong(1)
      val name = resultSet.getString("name")
      val age = resultSet.getInt(3)
      builder += (id, name, age)
    builder.result()
}
```

## Transaction

To execute transactions using `Connection`, combine the `setAutoCommit` method, `commit` method, and `rollback` method.

First, use the `setAutoCommit` method to disable automatic transaction commit.

```scala 3
conn.setAutoCommit(false)
```

After performing some processing, use the `commit` method to commit the transaction.

```scala 3
for
  statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
  _ <- statement.setString(1, "Alice")
  _ <- statement.setInt(2, 20)
  _ <- conn.setAutoCommit(false)
  result <- statement.executeUpdate()
  _ <- conn.commit()
yield
```
Alternatively, use the `rollback` method to roll back the transaction.

```scala 3
for
  statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
  _ <- statement.setString(1, "Alice")
  _ <- statement.setInt(2, 20)
  _ <- conn.setAutoCommit(false)
  result <- statement.executeUpdate()
  _ <- conn.rollback()
yield
```

When the `setAutoCommit` method is used to disable automatic transaction commit, the connection is automatically rolled back when the Resource is released.

### Transaction Isolation Level

ldbc allows you to set the transaction isolation level.

The transaction isolation level is set using the `setTransactionIsolation` method.

MySQL supports the following transaction isolation levels:

- READ UNCOMMITTED
- READ COMMITTED
- REPEATABLE READ
- SERIALIZABLE

For more information on MySQL transaction isolation levels, refer to the [official documentation](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html).

```scala 3
import ldbc.connector.Connection.TransactionIsolationLevel

conn.setTransactionIsolation(TransactionIsolationLevel.REPEATABLE_READ)
```

To retrieve the currently set transaction isolation level, use the `getTransactionIsolation` method.

```scala 3
for
  isolationLevel <- conn.getTransactionIsolation()
yield
```

### Savepoints

For more advanced transaction management, you can use the "Savepoint" feature. This allows you to mark specific points during database operations, enabling you to roll back to those points if something goes wrong. This is particularly useful for complex database operations or when you need to set safe points during long transactions.

**Features:**

- Flexible Transaction Management: Use savepoints to create "checkpoints" at any point within a transaction. If needed, you can roll back to that point, saving time and improving efficiency.
- Error Recovery: Instead of redoing everything from the beginning when an error occurs, you can roll back to the last safe savepoint, saving time and improving efficiency.
- Advanced Control: By setting multiple savepoints, you can achieve more precise transaction control. Developers can easily implement more complex logic and error handling.

By leveraging this feature, your application can achieve more robust and reliable database operations.

**Setting Savepoints**

To set a savepoint, use the `setSavepoint` method. You can specify the name of the savepoint with this method.
If you do not specify a name for the savepoint, a default name generated by UUID will be set.

You can retrieve the name of the set savepoint using the `getSavepointName` method.

*Note: In MySQL, auto-commit is enabled by default. To use savepoints, you need to disable auto-commit. Otherwise, all operations will be committed each time, and you will not be able to roll back transactions using savepoints.*

```scala 3
for
  _ <- conn.setAutoCommit(false)
  savepoint <- conn.setSavepoint("savepoint1")
yield savepoint.getSavepointName
```

**Rolling Back to Savepoints**

To roll back a part of a transaction using a savepoint, pass the savepoint to the `rollback` method.
After partially rolling back using a savepoint, if you commit the entire transaction, the transaction after that savepoint will not be committed.

```scala 3
for
  _ <- conn.setAutoCommit(false)
  savepoint <- conn.setSavepoint("savepoint1")
  _ <- conn.rollback(savepoint)
  _ <- conn.commit()
yield
```

**Releasing Savepoints**

To release a savepoint, pass the savepoint to the `releaseSavepoint` method.
After releasing a savepoint, if you commit the entire transaction, the transaction after that savepoint will be committed.

```scala 3
for
  _ <- conn.setAutoCommit(false)
  savepoint <- conn.setSavepoint("savepoint1")
  _ <- conn.releaseSavepoint(savepoint)
  _ <- conn.commit()
yield
```

## Utility Commands

MySQL has several utility commands. ([Reference](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_command_phase_utility.html))

ldbc provides APIs to use these commands.

| Command                | Purpose                                                                     | Supported |
|------------------------|-----------------------------------------------------------------------------|-----------|
| `COM_QUIT`             | `Informs the server that the client is requesting to close the connection.` | ✅         |
| `COM_INIT_DB`          | `Changes the default schema of the connection.`                             | ✅         |
| `COM_STATISTICS`       | `Retrieves internal status string in a readable format.`                    | ✅         |
| `COM_DEBUG`            | `Dumps debug information to the server's standard output.`                  | ❌         |
| `COM_PING`             | `Checks if the server is alive.`                                            | ✅         |
| `COM_CHANGE_USER`      | `Changes the user of the current connection.`                               | ✅         |
| `COM_RESET_CONNECTION` | `Resets the session state.`                                                 | ✅         |
| `COM_SET_OPTION`       | `Sets options for the current connection.`                                  | ✅         |

### COM QUIT

`COM_QUIT` is a command to inform the server that the client is requesting to close the connection.

In ldbc, you can close the connection using the `close` method of `Connection`.
When you use the `close` method, the connection is closed, and you cannot use the connection in subsequent processing.

*Note: `Connection` uses `Resource` for resource management. Therefore, you do not need to use the `close` method to release resources.*

```scala 3
datasource.getConnection.use { conn =>
  conn.close()
}
```

### COM INIT DB

`COM_INIT_DB` is a command to change the default schema of the connection.

In ldbc, you can change the default schema using the `setSchema` method of `Connection`.

```scala 3
datasource.getConnection.use { conn =>
  conn.setSchema("test")
}
```

### COM STATISTICS

`COM_STATISTICS` is a command to retrieve internal status string in a readable format.

In ldbc, you can retrieve the internal status string using the `getStatistics` method of `Connection`.

```scala 3
datasource.getConnection.use { conn =>
  conn.getStatistics
}
```

The status that can be retrieved is as follows:

- `uptime` : Time since the server started
- `threads` : Number of currently connected clients
- `questions` : Number of queries since the server started
- `slowQueries` : Number of slow queries
- `opens` : Number of table opens since the server started
- `flushTables` : Number of table flushes since the server started
- `openTables` : Number of currently open tables
- `queriesPerSecondAvg` : Average number of queries per second

### COM PING

`COM_PING` is a command to check if the server is alive.

In ldbc, you can check if the server is alive using the `isValid` method of `Connection`.
If the server is alive, it returns `true`, and if it is not alive, it returns `false`.

```scala 3
datasource.getConnection.use { conn =>
  conn.isValid
}
```

### COM CHANGE USER

`COM_CHANGE_USER` is a command to change the user of the current connection.
It also resets the following connection states:

- User variables 
- Temporary tables 
- Prepared statements 
- etc...

In ldbc, you can change the user using the `changeUser` method of `Connection`.

```scala 3
datasource.getConnection.use { conn =>
  conn.changeUser("root", "password")
}
```

### COM RESET CONNECTION

`COM_RESET_CONNECTION` is a command to reset the session state.

`COM_RESET_CONNECTION` is a lighter version of `COM_CHANGE_USER`, with almost the same functionality to clean up the session state, but with the following features:

- Does not re-authenticate (thus avoiding extra client/server exchanges).
- Does not close the connection.

In ldbc, you can reset the session state using the `resetServerState` method of `Connection`.

```scala 3
datasource.getConnection.use { conn =>
  conn.resetServerState
}
```

### COM SET OPTION

`COM_SET_OPTION` is a command to set options for the current connection.

In ldbc, you can set options using the `enableMultiQueries` method and `disableMultiQueries` method of `Connection`.

Using the `enableMultiQueries` method allows you to execute multiple queries at once.
Using the `disableMultiQueries` method prevents you from executing multiple queries at once.

*Note: This can only be used for batch processing with Insert, Update, and Delete statements. If used with Select statements, only the result of the first query will be returned.*

```scala 3
datasource.getConnection.use { conn =>
  conn.enableMultiQueries *> conn.disableMultiQueries
}
```

## Batch Commands

ldbc allows you to execute multiple queries at once using batch commands.
By using batch commands, you can execute multiple queries at once, reducing the number of network round trips.

To use batch commands, use the `addBatch` method of `Statement` or `PreparedStatement` to add queries, and use the `executeBatch` method to execute the queries.

```scala 3 3
datasource.getConnection.use { conn =>
  for
    statement <- conn.createStatement()
    _ <- statement.addBatch("INSERT INTO users (name, age) VALUES ('Alice', 20)")
    _ <- statement.addBatch("INSERT INTO users (name, age) VALUES ('Bob', 30)")
    result <- statement.executeBatch()
  yield result
}
```

In the above example, you can add the data of `Alice` and `Bob` at once.
The executed query will be as follows:

```sql
INSERT INTO users (name, age) VALUES ('Alice', 20);INSERT INTO users (name, age) VALUES ('Bob', 30);
```

The return value after executing the batch command is an array of the number of affected rows for each executed query.

In the above example, the data of `Alice` is added as 1 row, and the data of `Bob` is also added as 1 row, so the return value will be `List(1, 1)`.

After executing the batch command, the queries added with the `addBatch` method so far are cleared.

To manually clear the queries, use the `clearBatch` method to clear them.

```scala 3
datasource.getConnection.use { conn =>
  for
    statement <- conn.createStatement()
    _ <- statement.addBatch("INSERT INTO users (name, age) VALUES ('Alice', 20)")
    _ <- statement.clearBatch()
    _ <- statement.addBatch("INSERT INTO users (name, age) VALUES ('Bob', 30)")
    _ <- statement.executeBatch()
  yield
}
```

In the above example, the data of `Alice` is not added, but the data of `Bob` is added.

### Difference Between Statement and PreparedStatement

There may be differences in the queries executed by batch commands when using `Statement` and `PreparedStatement`.

When executing an INSERT statement with batch commands using `Statement`, multiple queries are executed at once.
However, when executing an INSERT statement with batch commands using `PreparedStatement`, one query is executed.

For example, when executing the following query with batch commands, multiple queries are executed at once because `Statement` is used.

```scala 3
datasource.getConnection.use { conn =>
  for
    statement <- conn.createStatement()
    _ <- statement.addBatch("INSERT INTO users (name, age) VALUES ('Alice', 20)")
    _ <- statement.addBatch("INSERT INTO users (name, age) VALUES ('Bob', 30)")
    result <- statement.executeBatch()
  yield result
}

// Executed query
// INSERT INTO users (name, age) VALUES ('Alice', 20);INSERT INTO users (name, age) VALUES ('Bob', 30);
```

However, when executing the following query with batch commands, one query is executed because `PreparedStatement` is used.

```scala 3
datasource.getConnection.use { conn =>
  for
    statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
    _ <- statement.setString(1, "Alice")
    _ <- statement.setInt(2, 20)
    _ <- statement.addBatch()
    _ <- statement.setString(1, "Bob")
    _ <- statement.setInt(2, 30)
    _ <- statement.addBatch()
    result <- statement.executeBatch()
  yield result
}

// Executed query
// INSERT INTO users (name, age) VALUES ('Alice', 20), ('Bob', 30);
```

This is because when using `PreparedStatement`, you can set multiple parameters in one query by using the `addBatch` method after setting the query parameters.

## Executing Stored Procedures

ldbc provides an API for executing stored procedures.

To execute a stored procedure, use the `prepareCall` method of `Connection` to construct a `CallableStatement`.

*Note: The stored procedure used is from the [official](https://dev.mysql.com/doc/connector-j/en/connector-j-usagenotes-statements-callable.html) documentation.*

```sql
CREATE PROCEDURE demoSp(IN inputParam VARCHAR(255), INOUT inOutParam INT)
BEGIN
    DECLARE z INT;
    SET z = inOutParam + 1;
    SET inOutParam = z;

    SELECT inputParam;

    SELECT CONCAT('zyxw', inputParam);
END
```

To execute the above stored procedure, it will be as follows.

```scala 3
datasource.getConnection.use { conn =>
  for
    callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
    _ <- callableStatement.setString(1, "abcdefg")
    _ <- callableStatement.setInt(2, 1)
    hasResult <- callableStatement.execute()
    values <- Monad[IO].whileM[List, Option[String]](callableStatement.getMoreResults()) {
      for
        resultSet <- callableStatement.getResultSet().flatMap {
          case Some(rs) => IO.pure(rs)
          case None     => IO.raiseError(new Exception("No result set"))
        }
      yield resultSet.getString(1)
    }
  yield values // List(Some("abcdefg"), Some("zyxwabcdefg"))
}
```

To retrieve the value of an output parameter (a parameter specified as OUT or INOUT when creating the stored procedure), in JDBC, you need to specify the parameter before executing the statement using various `registerOutputParameter()` methods of the CallableStatement interface. However, in ldbc, you only need to set the parameter using the `setXXX` method, and the parameter will be set when executing the query.

However, you can also specify the parameter using the `registerOutputParameter()` method in ldbc.

```scala 3
datasource.getConnection.use { conn =>
  for
    callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
    _ <- callableStatement.setString(1, "abcdefg")
    _ <- callableStatement.setInt(2, 1)
    _ <- callableStatement.registerOutParameter(2, ldbc.connector.data.Types.INTEGER)
    hasResult <- callableStatement.execute()
    value <- callableStatement.getInt(2)
  yield value // 2
}
```

*Note: When specifying an Out parameter with `registerOutParameter`, if the same index value is not used to set the parameter with the `setXXX` method, the value will be set to `Null` on the server.*
