{%
  laika.title = logging
  laika.metadata.language = en
%}

# logging

You learned how to handle errors in [Error Handling](/en/tutorial/Error-Handling.md). This page describes how to set up logging and log query execution and errors.

Logging is an important part of understanding application behavior and diagnosing problems. ldbc provides a customizable logging system that can be integrated with any logging library.

ldbc can export execution and error logs of database connections in any format using any logging library.

A logger using Cats Effect's Console is provided for use during development. It is not enabled by default: unless you set a `LogHandler`, ldbc does not log query execution.

To customize logging using an arbitrary logging library, use `ldbc.logging.LogHandler`.

The following is the standard implementation of logging. ldbc generates the following three types of events on database connection.

```scala 3
import cats.effect.Sync
import cats.effect.std.Console

import ldbc.logging.{ LogEvent, LogHandler }

def console[F[_]: Console: Sync]: LogHandler[F] =
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

@:callout(warning)
`args` contains the values bound to the query parameters, which may include personal data, passwords, or tokens. The example above writes them out as-is, which is intended for development only. In production, do not log `args`, or mask sensitive values before logging them. What a `LogHandler` writes is the application's responsibility.
@:@

The created `LogHandler` can be passed as the second argument when creating a `Connector` to use any log output method.

```scala 3
import ldbc.logging.LogHandler
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*

val datasource =
  MySQLDataSource
    .build[IO]("127.0.0.1", 3306, "ldbc")
    .setPassword("password")
    .setDatabase("ldbc")
    .setSSL(SSL.Trusted)

val connector = Connector.fromDataSource(datasource, console[IO])
```

## Next Steps

Now you know how to set up logging in ldbc. Setting up proper logging will make it easier to monitor application behavior and diagnose problems.

Next, go to [Custom Data Types](/en/tutorial/Custom-Data-Type.md) to learn how to use your own data types in ldbc.
