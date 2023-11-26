# Database Connection

This chapter describes how to use queries built with LDBC to process connections to databases.

The following dependencies must be set up for the project

@@@ vars
```scala
libraryDependencies ++= Seq(
  "$org$" %% "ldbc-dsl" % "$version$",
  "mysql" % "mysql-connector-java" % "$mysqlVersion$"
)
```
@@@

If you have not yet read about how to build queries with LDBC, we recommend that you read the chapter [Building Type-Safe Queries](/ldbc/en/03-Type-safe-Query-Builder.html) first.

The following code example assumes the following import

```scala 3
import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.IO
// This is just for testing. Consider using cats.effect.IOApp instead of calling
// unsafe methods directly.
import cats.effect.unsafe.implicits.global

import ldbc.sql.*
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.TableQuery
```

Table definitions use the following

```scala 3
case class User(
  id: Long,
  name: String,
  age: Option[Int],
)

val table = Table[User]("user")(
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
)

val userQuery = TableQuery[IO, User](table)
```

## Using DataSource

LDBC uses JDBC's DataSource for database connections, and since LDBC does not provide an implementation for building this DataSource, it is necessary to use a library such as mysql or HikariCP. In this example, we will use mysqlDataSource to build the DataSource.

```scala 3
private val dataSource = new MysqlDataSource()
dataSource.setServerName("127.0.0.1")
dataSource.setPortNumber(3306)
dataSource.setDatabaseName("database name")
dataSource.setUser("user name")
dataSource.setPassword("password")
```

## Log

LDBC can export execution and error logs of Database connections in any format using any logging library.

A logger using Cats Effect's Console is provided as standard, which can be used during development.

```scala 3
given LogHandler[IO] = LogHandler.consoleLogger
```

### Customize

Use `ldbc.dsl.logging.LogHandler` to customize logs using any logging library.

The following is the standard implementation of logging: LDBC generates the following three types of events on database connections

- Success: Successful processing
- ProcessingFailure: Error in processing after data acquisition or before database connection
- ExecFailure: Error processing connection to database

Pattern matching is used to sort out what logs to write for each event.

```scala 3
def consoleLogger[F[_]: Console: Sync]: LogHandler[F] =
  case LogEvent.Success(sql, args) =>
    Console[F].println(
      s"""Successful Statement Execution:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    )
  case LogEvent.ProcessingFailure(sql, args, failure) =>
    Console[F].errorln(
      s"""Failed ResultSet Processing:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    ) >> Console[F].printStackTrace(failure)
  case LogEvent.ExecFailure(sql, args, failure) =>
    Console[F].errorln(
      s"""Failed Statement Execution:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    ) >> Console[F].printStackTrace(failure)
```

## Query

Constructing a `select` statement allows you to use the `query` method. The `query` method is used to determine the format of the data to be retrieved. If no type is specified, the column type specified in the `select` method is returned as a Tuple.

```scala 3
val query1 = userQuery.selectAll.query // (Long, String, Option[Int])
val query2 = userQuery.select(user => (user.name, user.age)).query // (String, Option[Int])
```

Specifying a model in the `query` method will convert the acquired data to the specified model.

```scala 3
val query = userQuery.selectAll.query[User] // User
```

The model type specified in the `query` method must match the Tuple type specified in the `select` method or be type-convertible from the Tuple type to the specified model.

```scala 3
val query1 = userQuery.select(user => (user.name, user.age)).query[User] // Compile error

case class Test(name: String, age: Option[Int])
val query2 = userQuery.select(user => (user.name, user.age)).query[Test] // Test
```

### toList

After determining the type of data to retrieve with the `query` method, it is then up to the user to decide whether the data to be retrieved is to be retrieved as an array or as Optional data.

The `toList` method is used to retrieve a list of data as a result of executing a query. If you use the `toList` method to process the database and get zero data, an empty array will be returned.

```scala 3
val query1 = userQuery.selectAll.query.toList // List[(Long, String, Option[Int])]
val query2 = userQuery.selectAll.query[User].toList // List[User]
```

### headOption

クエリのOptionalな結果として最初のデータを取得したい場合は、`headOption` メソッドを使用する。headOption` メソッドを使用したデータベース処理の結果が 0 の場合は、何も返されない。

Note that if you use the `headOption` method, only the first data will be returned, even if you execute a query that retrieves multiple data.

```scala 3
val query1 = userQuery.selectAll.query.headOption // Option[(Long, String, Option[Int])]
val query2 = userQuery.selectAll.query[User].headOption // Option[User]
```

### unsafe

When using the `unsafe` method, it is the same as the `headOption` method in that it returns only the first case of the retrieved data, but the data is returned as is, not as Optional. If the number of data returned is zero, an exception will be raised and appropriate exception handling is required.

It is named `unsafe` because it is likely to raise an exception at runtime.

```scala 3
val query1 = userQuery.selectAll.query.unsafe // (Long, String, Option[Int])
val query2 = userQuery.selectAll.query[User].unsafe // User
```

## Update

Constructing an `insert/update/delete` statement allows you to use the `update` method. The `update` method returns the number of write operations to the database.

```scala 3
val insert = userQuery.insert((1L, "name", None)).update // Int
val update = userQuery.update("name", "update name").update // Int
val delete = userQuery.delete.update // Int
```

In the case of an `insert` statement, you may want the values generated by AutoIncrement to be returned when inserting data. In this case, use the `returning` method instead of the `update` method to specify the columns to be returned.

```scala 3
val insert = userQuery.insert((1L, "name", None)).returning("id") // Long
```

The value specified in the `returning` method must be the name of a property that the model has. Also, if the specified property does not have the AutoIncrement attribute set on the table definition, an error will occur.

In MySQL, the only value that can be returned when inserting data is the AutoIncrement column, so the same specification applies to LDBC.

## Perform database operations

Before making a database connection, commit timing, read/write-only, and other settings must be made.

### Read Only

The `readOnly` method can be used to make the processing of a query to be executed read-only. The `readOnly` method can also be used with `insert/update/delete` statements, but it will result in an error at runtime because of the write operation.

```scala 3
val read = userQuery.selectAll.query.readOnly
```

### Auto Commit

The `autoCommit` method can be used to set the query processing to commit at each query execution.

```scala 3
val read = userQuery.insert((1L, "name", None)).update.autoCommit
```

### Transaction

The `transaction` method can be used to combine multiple database connection operations into a single transaction.

The return value of the `toList/headOption/unsafe/returning/update` method is of type `Kleisli[F, Connection[F], T]`. Therefore, you can use map or flatMap to combine the process into one.

By using the `transaction` method on a single `Kleisli[F, Connection[F], T]`, all database connection operations performed within will be combined into a single transaction.

```scala 3
(for
  result1 <- userQuery.insert((1L, "name", None)).returning("id")
  result2 <- userQuery.update("name", "update name").update
  ...
yield ...).transaction
```

### Execution

The return type and connection method have been set up, but since the process so far built with LDBC is of type `Kleisli[F, XXX, T]`, the database connection process (side effect) will not occur just by defining it.
You need to run `run` on `Kleisli` to execute the database process.

If you use the `readOnly/autoCommit/transaction` method, the return type will be `Kleisli[F, DataSource, T]`, so you can raise the return type to `F` by passing JDBC's DataSource to `run`.

```scala 3
val effect = userQuery.selectAll.query[User].headOption.readOnly(dataSource) // F[User]
```

If you are using Cats Effect IO, you can perform the database connection process by executing it in `IOApp` or by using `unsafeRunSync` or similar.

```scala 3
val user: User = userQuery.selectAll.query[User].headOption.readOnly(dataSource).unsafeRunSync()
```

For more information on `Kleisli`, please refer to Cats' [documentation](https://typelevel.org/cats/datatypes/kleisli.html).

## Database Action

There is also a way to perform database processing using `Database` with connection information to the database.

There are two ways to construct a `Database`: using the DriverManager or generating one from a DataSource. The following is an example of constructing a `Database` with connection information to a database using a MySQL driver.

```scala 3
val db = Database.fromMySQLDriver[IO]("database name", "host", "port number", "user name", "password")
```

The advantages of using `Database` to perform database processing are as follows

- Simplifies DataSource construction (when using DriverManager)
- Eliminates the need to pass a DataSource for each query

The method using `Database` is merely a simplified method of passing a DataSource, so there is no difference in execution results between the two.
The only difference is whether the processes are combined using `flatMap` or other methods and executed in a method chain, or whether the combined processes are executed using `Database`. Therefore, the user can choose the execution method of his/her choice.

**Read Only**

```scala 3
val user: Option[User] = db.readOnly(userQuery.selectAll.query[User].headOption).unsafeRunSync()
```

**Auto Commit**

```scala 3
val result = db.autoCommit(userQuery.insert((1L, "name", None)).update).unsafeRunSync()
```

**Transaction**

```scala 3
db.transaction(for
  result1 <- userQuery.insert((1L, "name", None)).returning("id")
  result2 <- userQuery.update("name", "update name").update
  ...
yield ...).unsafeRunSync()
```

### Database model

In LDBC, the `Database` model is also used for purposes other than holding database connection information. Another use is for SchemaSPY documentation generation, see [here](/ldbc/ja/06-Generating-SchemaSPY-Documentation.html) for information on SchemaSPY document generation.

If you have already generated a `Database` model for another use, you can use that model to build a `Database` with database connection information.

```scala 3
import ldbc.dsl.io.*

val database: Database = ???

val db = database.fromDriverManager()
// or
val db = database.fromDriverManager("user name", "password")
```

## Using a HikariCP Connection Pool

`ldbc-hikari` provides a builder to build HikariConfig and HikariDataSource for building HikariCP connection pools.

@@@ vars
```scala
libraryDependencies ++= Seq(
  "$org$" %% "ldbc-hikari" % "$version$",
)
```
@@@

`HikariConfigBuilder` is a builder to build `HikariConfig` of HikariCP as the name suggests.

```scala 3
val hikariConfig: com.zaxxer.hikari.HikariConfig = HikariConfigBuilder.default.build()
```

The `HikariConfigBuilder` has a `default` and a `from` method. When `default` is used, the `HikariConfig` is constructed by retrieving the target values from the Config based on the LDBC specified path.

```text
ldbc.hikari {
  jdbc_url = ...
  username = ...
  password = ...
}
```

If you want to specify a user-specific path, you must use the `from` method and pass the path you want to retrieve as an argument.

```scala 3
val hikariConfig: com.zaxxer.hikari.HikariConfig = HikariConfigBuilder.from("custom.path").build()

// custom.path {
//   jdbc_url = ...
//   username = ...
//   password = ...
// }
```

Please refer to [official](https://github.com/brettwooldridge/HikariCP) for details on what can be set in HikariCP.

The following is a list of keys that can be set for Config.

| Key name                    | Description                                                                                                               | Type     |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------|----------|
| catalog                     | Default catalog name to be set when connecting                                                                            | String   |
| connection_timeout          | Maximum number of milliseconds the client will wait for a connection from the pool                                        | Duration |
| idle_timeout                | Maximum time (in milliseconds) that a connection is allowed to be idle in the pool                                        | Duration |
| leak_detection_threshold    | Time a connection is out of the pool before a message indicating a possible connection leak is logged                     | Duration |
| maximum_pool_size           | Maximum size allowed by the pool, including both idle and in-use connections                                              | Int      |
| max_lifetime                | Maximum lifetime of connections in the pool                                                                               | Duration |
| minimum_idle                | Minimum number of idle connections that HikariCP will try to keep in the pool, including both idle and in-use connections | Int      |
| pool_name                   | Connection pool name                                                                                                      | String   |
| allow_pool_suspension       | Whether to allow pool suspend                                                                                             | Boolean  |
| auto_commit                 | Default autocommit behavior for connections in the pool                                                                   | Boolean  |
| connection_init_sql         | SQL string to be executed when a new connection is created, before it is added to the pool                                | String   |
| connection_test_query       | SQL query to execute to test the validity of the connection                                                               | String   |
| data_source_classname       | Fully qualified class name of the JDBC DataSource to be used to create Connections                                        | String   |
| initialization_fail_timeout | Pool initialization failure timeout                                                                                       | Duration |
| isolate_internal_queries    | Whether internal pool queries (mainly validity checks) are separated in their own transaction by `Connection.rollback()`. | Boolean  |
| jdbc_url                    | JDBC URL                                                                                                                  | String   |
| readonly                    | Whether connections to be added to the pool should be set as read-only connections                                        | Boolean  |
| register_mbeans             | Whether HikariCP self-registers HikariConfigMXBean and HikariPoolMXBean in JMX                                            | Boolean  |
| schema                      | Default schema name to set when connecting                                                                                | String   |
| username                    | Default username used for calls to `DataSource.getConnection(username,password)`                                          | String   |
| password                    | Default password used for calling `DataSource.getConnection(username,password)`                                           | String   |
| driver_class_name           | Driver class name to be used                                                                                              | String   |
| transaction_isolation       | Default transaction isolation level                                                                                       | String   |

The `HikariDataSourceBuilder` allows you to build a `HikariDataSource` for HikariCP.

The `HikariDataSource` built by the builder is managed as a `Resource` since the connection pool is a lifetime managed object and needs to be shut down cleanly.

```scala 3
val dataSource: Resource[IO, HikariDataSource] = HikariDataSourceBuilder.default[IO].buildDataSource()
```

The `HikariDataSource` built via `buildDataSource` uses `HikariConfig`, which is built internally by retrieving settings from Config based on the LDBC specified path.
This is equivalent to `HikariConfig` generated via `default` in `HikariConfigBuilder`.

If you want to use a user-specified `HikariConfig`, you can use `buildFromConfig` to build a `HikariDataSource`.

```scala 3
val hikariConfig = ???
val dataSource = HikariDataSourceBuilder.default[IO].buildFromConfig(hikariConfig)
```

A `HikariDataSource` built with `HikariDataSourceBuilder` is usually executed using IOApp.

```scala 3
object HikariApp extends IOApp:

  val dataSourceResource: Resource[IO, HikariDataSource] = HikariDataSourceBuilder.default[IO].buildDataSource()

  def run(args: List[String]): IO[ExitCode] =
    dataSourceResource.use { dataSource =>
       ...
    }
```

### HikariDatabase

There is also a way to build a `Database` with HikariCP connection information.

The `HikariDatabase` is managed as a `Resource` like the `HikariDataSource`.
Therefore, it is usually executed using IOApp.

```scala 3
object HikariApp extends IOApp:

  val hikariConfig = ???
  val databaseResource: Resource[F, Database[F]] = HikariDatabase.fromHikariConfig[IO](hikariConfig)

  def run(args: List[String]): IO[ExitCode] =
    databaseResource.use { database =>
       for
         result <- database.readOnly(...)
        yield ExitCode.Success
    }
```
