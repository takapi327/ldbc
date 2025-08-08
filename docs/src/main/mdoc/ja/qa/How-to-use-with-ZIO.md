{%
laika.title = "Q: ZIOで使用する方法は？"
laika.metadata.language = ja
%}

# Q: ZIOで使用する方法は？

## A: ZIOで使用する場合、`zio-interop-cats`を使用します。

```scala
libraryDependencies += "dev.zio" %% "zio-interop-cats" % "<latest-version>"
```

以下は、ZIOを使用してldbcを利用するためのサンプルコードです。

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

  private def provider =
    ConnectionProvider
      .default[Task]("127.0.0.1", 13306, "ldbc", "password", "world")
      .setSSL(SSL.Trusted)

  override def run =
    provider.use { conn =>
      sql"SELECT Name FROM city"
        .query[String]
        .to[List]
        .readOnly(conn)
        .flatMap { cities =>
          Console.printLine(cities)
        }
    }
```

### パフォーマンス

Cats EffectからZIO変換によるパフォーマンス結果は以下のようになります。

@:image(/img/connector/Select_effect.svg) {
alt = "Select Benchmark for Effect System"
}
