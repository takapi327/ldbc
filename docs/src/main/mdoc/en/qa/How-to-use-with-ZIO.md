{%
laika.title = "Q: How to use with ZIO?"
laika.metadata.language = ja
%}

# Q: How to use with ZIO?

## A: For use with ZIO, use `ldbc-zio-interop`.

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-zio-interop" % "latest"
```

The following is sample code for using ldbc with ZIO.

```scala 3 mdoc
import zio.*

import ldbc.zio.interop.*
import ldbc.connector.*
import ldbc.dsl.*

object Main extends ZIOAppDefault:

  private val datasource =
    MySQLDataSource
      .build[Task]("127.0.0.1", 3306, "ldbc")
      .setPassword("password")
      .setDatabase("world")
      .setSSL(SSL.Trusted)

  private val connector = Connector.fromDataSource(datasource)

  override def run =
    sql"SELECT Name FROM city"
      .query[String]
      .to[List]
      .readOnly(connector)
      .flatMap { cities =>
        Console.printLine(cities)
      }
```

### Performance

Performance results from the Cats Effect to ZIO conversion are shown below.

@:image(/img/connector/Select_effect.svg) {
alt = "Select Benchmark for Effect System"
}
