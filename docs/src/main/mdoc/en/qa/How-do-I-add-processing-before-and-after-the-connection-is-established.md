{%
  laika.title = "Q: How do I add processing before and after the connection is established?"
  laika.metadata.language = en
%}

# Q: How do I add processing before and after the connection is established?

## A: To add processing before and after connection establishment, use `withBeforeAfter`.

You can use the `withBeforeAfter` method when creating a Connection to invoke processes both before and after a connection is established. For example, as shown below, by passing arbitrary processes to `before` and `after`, you can output logs before and after connection establishment.

The second argument of `withBeforeAfter` specifies the type of the Before process result that will be passed to After.

```scala 3
import ldbc.mysql.*

def before: Connection[IO] => IO[Unit] = _ => IO.println("Connecting to...")
def after: (Unit, Connection[IO]) => IO[Unit] = (_, _) => IO.println("Connection Closed")

val datasource =
  MySQLDataSource
    ...
    .withBeforeAfter(before, after)
```

@:callout(warning)

This feature is only available when using `ldbc-mysql` (or the legacy `ldbc-connector`).

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-mysql"       % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-cats-effect" % "@VERSION@"
)
```

@:@

## References
- [Using ldbc connector](/en/tutorial/Connection.md#using-the-ldbc-connector)
