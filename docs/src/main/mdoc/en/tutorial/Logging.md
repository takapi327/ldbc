{%
  laika.title = Logging
  laika.metadata.language = en
%}

# Logging

ldbc can export execution and error logs of database connections in any format using any logging library.

The standard logger using Cats Effect's Console is provided and can be used during development.

```scala 3
given LogHandler[IO] = LogHandler.console[IO]
```

Use `ldbc.dsl.logging.LogHandler` to customize logging using any logging library.

The following is the standard implementation of logging. ldbc generates the following three types of events on database connection

- Success: Success of processing
- ProcessingFailure: Error in processing after getting data or before connecting to the database
- ExecFailure: Error in the process of connecting to the database

Each event is sorted by pattern matching to determine what kind of log to write.

```scala 3
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
