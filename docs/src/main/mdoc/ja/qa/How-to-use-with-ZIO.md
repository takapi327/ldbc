{%
laika.title = "Q: ZIOで使用する方法は？"
laika.metadata.language = ja
%}

# Q: ZIOで使用する方法は？

## A: ZIOで使用する場合、`ldbc-zio-interop`を使用します。

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-zio-interop" % "latest"
```

以下は、ZIOを使用してldbcを利用するためのサンプルコードです。

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

### パフォーマンス

Cats EffectからZIO変換によるパフォーマンス結果は以下のようになります。

@:image(/img/connector/Select_effect.svg) {
alt = "Select Benchmark for Effect System"
}
