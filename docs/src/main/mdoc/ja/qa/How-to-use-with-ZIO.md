{%
laika.title = "Q: ZIOで使用する方法は？"
laika.metadata.language = ja
%}

# Q: ZIOで使用する方法は？

## A: ZIOで使用する場合、`ldbc-mysql`と`ldbc-zio`を使用します。

0.9.x では、エフェクト非依存ドライバ`ldbc-mysql`と ZIO（`Task`）ブリッジ`ldbc-zio`により、`zio-interop-cats`を介さずに ZIO ネイティブで動作します。

```scala
libraryDependencies ++= Seq(
  "io.github.takapi327" %% "ldbc-mysql" % "@VERSION@",
  "io.github.takapi327" %% "ldbc-zio"   % "@VERSION@"
)
```

以下は、ZIOを使用してldbcを利用するためのサンプルコードです。

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

### パフォーマンス

Cats EffectからZIO変換によるパフォーマンス結果は以下のようになります。

@:image(/img/connector/Select_effect.svg) {
alt = "Select Benchmark for Effect System"
}
