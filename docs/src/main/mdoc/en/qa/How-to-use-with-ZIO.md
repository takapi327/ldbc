{%
laika.title = "Q: How to use with ZIO?"
laika.metadata.language = ja
%}

# Q: How to use with ZIO?

## A: For use with ZIO, use `ldbc-mysql` and `ldbc-zio`.

In 0.9.x, the effect-agnostic driver `ldbc-mysql` and the ZIO (`Task`) bridge `ldbc-zio` let you run natively on ZIO without going through `zio-interop-cats`.

```scala
libraryDependencies ++= Seq(
  "io.github.takapi327" %% "ldbc-mysql" % "@VERSION@",
  "io.github.takapi327" %% "ldbc-zio"   % "@VERSION@"
)
```

The following is sample code for using ldbc with ZIO.

```scala 3
import zio.*

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.zio.Connector
import ldbc.zio.given

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
