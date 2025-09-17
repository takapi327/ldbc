{%
laika.title = "Q: How to use with ZIO?"
laika.metadata.language = ja
%}

# Q: How to use with ZIO?

## A: For use with ZIO, use `zio-interop-cats`.

```scala
libraryDependencies += "dev.zio" %% "zio-interop-cats" % "<latest-version>"
```

The following is sample code for using ldbc with ZIO.

```scala 3 mdoc
import java.util.UUID

import cats.effect.std.UUIDGen

import fs2.hashing.Hashing
import fs2.io.net.Network

import zio.*
import zio.interop.catz.*

object Main extends ZIOAppDefault:

  given cats.effect.std.Console[Task] = cats.effect.std.Console.make[Task]
  given UUIDGen[Task] with
    override def randomUUID: Task[UUID] = ZIO.attempt(UUID.randomUUID())
  given Hashing[Task] = Hashing.forSync[Task]
  given Network[Task] = Network.forAsync[Task]

  private def datasource =
    MySQLDataSource
      .build[Task]("127.0.0.1", 13306, "ldbc")
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

### パフォーマンス

Performance results from the Cats Effect to ZIO conversion are shown below.

@:image(/img/connector/Select_effect.svg) {
alt = "Select Benchmark for Effect System"
}
