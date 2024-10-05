{%
  laika.title = Connection
  laika.metadata.language = en
%}

# Connection

This chapter describes how to build a connection to connect to a database.

To connect to a database, a connection must be established. A connection is a resource that manages the connection to the database. A connection provides the resources to initiate a connection to the database, execute a query, and close the connection.

ldbc connects to the database using either jdbc or ldbc's own connector. Which one to use depends on the dependencies you set up.

## Use jdbc connector

First, add the dependencies.

If you use the JDJD connector, you must also add the MySQL connector.

```scala
//> dep "@ORGANIZATION@::jdbc-connector:@VERSION@"
//> dep "com.mysql":"mysql-connector-j":"@MYSQL_VERSION@"
```

Next, create a data source using `MysqlDataSource`.

```scala
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")
```

Create a jdbc connector data source using the data source you created.

```scala
val datasource = jdbc.connector.MysqlDataSource[IO](ds)
```

Finally, a connection is created using a jdbc connector.

```scala
val connection: Resource[IO, Connection[IO]] =
  Resource.make(datasource.getConnection)(_.close())
```

Here we use the Cats Effect `Resource` to close the connection after it has been used.

## Use ldbc connector

First, add dependencies.

```scala
//> dep "@ORGANIZATION@::ldbc-connector:@VERSION@"
```

Next, Tracer is provided. ldbc connectors use Tracer to collect telemetry data. These are used to record application traces.

Here, Tracer is provided using `Tracer.noop`.

```scala 3
given Tracer[IO] = Tracer.noop[IO]
```

Finally, create a `Connection`.

```scala
val connection: Resource[IO, Connection[IO]] =
  ldbc.connector.Connection[IO](
    host     = "127.0.0.1",
    port     = 3306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("ldbc")
  )
```

The parameters for setting up a connection are as follows

| Property                  | Detail                                                                        | Required |
|---------------------------|-------------------------------------------------------------------------------|----------|
| `host`                    | `Database Host Information`                                                   | ✅        |
| `port`                    | `Database Port Information`                                                   | ✅        |
| `user`                    | `Database User Information`                                                   | ✅        |
| `password`                | `Database password information (default: None)`                               | ❌        |
| `database`                | `Database name information (default: None)`                                   | ❌        |
| `debug`                   | `Whether to display debugging information or not (default: false)`            | ✅        |
| `ssl`                     | `SSL configuration (default: SSL.None)`                                       | ✅        |
| `socketOptions`           | `Specify socket options for TCP/ UDP sockets (default: defaultSocketOptions)` | ✅        |
| `readTimeout`             | `Specify timeout period (default: Duration.Inf)`                              | ✅        |
| `allowPublicKeyRetrieval` | `Whether to retrieve the public key or not (default: false)`                  | ✅        |
