{%
  laika.title = HikariCP
  laika.metadata.language = ja
%}

# HikariCPとldbcの連携

## はじめに

HikariCPは、Javaのデータベースコネクションプールライブラリとしてもっとも広く使用されているライブラリの一つです。
ldbcではHikariCPと組み合わせることで、効率的なデータベース接続の管理が可能になります。

## 依存関係の追加

まず、build.sbtに必要な依存関係を追加します：

```scala
libraryDependencies ++= Seq(
  "com.mysql" % "mysql-connector-j" % "9.6.0",
  "com.zaxxer" % "HikariCP" % "6.2.1"
)
```

## HikariCPの基本設定

HikariCPには多くの設定オプションがあります。以下に主要な設定項目を示します：

```scala
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

val config = new HikariConfig()
config.setJdbcUrl("jdbc:mysql://localhost:3306/database")
config.setUsername("username")
config.setPassword("password")

// コネクションプールの基本設定
config.setMaximumPoolSize(10)         // 最大プールサイズ
config.setMinimumIdle(5)             // 最小アイドル接続数
config.setConnectionTimeout(30000)    // 接続タイムアウト（ミリ秒）
config.setIdleTimeout(600000)        // アイドルタイムアウト（ミリ秒）

val dataSource = new HikariDataSource(config)
```

## ldbcとの連携例

以下は、HikariCPとldbcを組み合わせた基本的な使用例です：

```scala
import cats.effect.*
import com.zaxxer.hikari.HikariDataSource
import ldbc.dsl.*
import ldbc.dsl.codec.Codec
import jdbc.connector.*
import ldbc.connector.Connector

// データモデルの定義
case class User(id: Int, name: String, email: String)
object User:
  given Codec[User] = Codec.derived[User]

object HikariExample extends IOApp.Simple:
  def run: IO[Unit] =
    // HikariCPの設定
    val ds = new HikariDataSource()
    ds.setJdbcUrl("jdbc:mysql://localhost:3306/mydb")
    ds.setUsername("user")
    ds.setPassword("password")
    
    // DataSourceとConnectorの設定
    val program = for
      hikari     <- Resource.fromAutoCloseable(IO(ds))
      execution  <- ExecutionContexts.fixedThreadPool[IO](10)
      datasource = MySQLDataSource.fromDataSource[IO](hikari, execution)
    yield Connector.fromDataSource(datasource)
    
    // クエリの実行
    program.use { connector =>
      for
        users <- sql"SELECT * FROM users".query[User].to[List].readOnly(connector)
        _     <- IO.println(s"Found users: $users")
      yield ()
    }
```

## 高度な設定例

HikariCPには、パフォーマンスチューニングのための様々な設定があります：

```scala
val config = new HikariConfig()

// 基本設定
config.setPoolName("MyPool")                // プール名の設定
config.setAutoCommit(false)                 // 自動コミットの無効化

// パフォーマンス設定
config.setMaxLifetime(1800000)             // 接続の最大生存時間（30分）
config.setValidationTimeout(5000)          // 接続検証のタイムアウト
config.setLeakDetectionThreshold(60000)    // 接続リーク検出のしきい値

// MySQLに特化した設定
config.addDataSourceProperty("cachePrepStmts", "true")
config.addDataSourceProperty("prepStmtCacheSize", "250")
config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
```

## まとめ

HikariCPとldbcを組み合わせることで、以下のような利点が得られます：

- 効率的なコネクションプーリング
- トランザクション管理の簡素化
- 型安全なクエリ実行
- Cats Effectを活用した純粋関数型プログラミング

適切な設定とエラーハンドリングを行うことで、安定した高パフォーマンスのデータベースアクセスが実現できます。
