{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.3.xから0.4.xへの移行)

## パッケージ

**削除されたパッケージ**

| Module / Platform    | JVM | Scala Native | Scala.js |  
|----------------------|:---:|:------------:|:--------:|
| `ldbc-schemaSpy`     |  ✅  |      ❌       |    ❌     | 

**別の機能としてリニューアルされたパッケージ**

| Module / Platform    | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                  |
|----------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-core`          |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)          |

**全てのパッケージ**

| Module / Platform    | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                  |
|----------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`           |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)           |
| `ldbc-core`          |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)          |
| `ldbc-connector`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)     |
| `jdbc-connector`     |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)     |
| `ldbc-dsl`           |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)           |
| `ldbc-statement`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)     |
| `ldbc-query-builder` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3) |
| `ldbc-schema`        |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)        |
| `ldbc-codegen`       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)       |
| `ldbc-hikari`        |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-hikari_3)        |
| `ldbc-plugin`        |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.3.3-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0) |

## 🎯 主要な変更点

### 1. 組み込みコネクションプーリング機能の追加

0.4.0 から、ldbc-connector に高性能なコネクションプーリング機能が組み込まれました。これにより、HikariCP などの外部ライブラリを使用せずに、効率的なコネクション管理が可能になりました。

**メリット:**
- Cats Effect のファイバーベース並行性モデルに最適化
- CircuitBreaker による障害時の保護
- 動的プールサイジング
- 詳細なメトリクス追跡

### 2. API の変更

#### ConnectionProvider から MySQLDataSource への移行と Connector の使用

ConnectionProvider は非推奨となり、新しい MySQLDataSource と Connector API を使用します。

**旧 API (0.3.x):**
```scala
import ldbc.connector.*

// ConnectionProvider を使用
val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")

// 直接使用
provider.use { connection =>
  // SQL実行
}
```

**新 API (0.4.x):**
```scala
import ldbc.connector.*
import ldbc.core.*
import ldbc.dsl.*

// MySQLDataSource を使用
val dataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")

// Connector を作成して DBIO を実行
val connector = Connector.fromDataSource(dataSource)

// SQLクエリの実行
val result = sql"SELECT * FROM users"
  .query[User]
  .to[List]
  .readOnly(connector)

// または Connection から Connector を作成
dataSource.getConnection.use { connection =>
  val connector = Connector.fromConnection(connection)
  // DBIO を実行
  sql"INSERT INTO users (name) VALUES ($name)"
    .update
    .commit(connector)
}

// コネクションプーリング
val pooledDataSource = MySQLDataSource.pooling[IO](
  MySQLConfig.default
    .setHost("localhost")
    .setPort(3306)
    .setUser("root")
    .setPassword("password")
    .setDatabase("test")
    .setMinConnections(5)
    .setMaxConnections(20)
)

pooledDataSource.use { pool =>
  val connector = Connector.fromDataSource(pool)
  // DBIO を実行
  sql"SELECT * FROM users WHERE id = $id"
    .query[User]
    .option
    .readOnly(connector)
}
```

### 3. 設定方法の変更

#### ldbc-connector の設定

**旧方式 (0.3.x):**
```scala
val provider = ConnectionProvider
  .default[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")
  .setSSL(SSL.Trusted)
  .addSocketOption(SocketOption.receiveBufferSize(4096))
  .setReadTimeout(30.seconds)
```

**新方式 (0.4.x):**
```scala
val dataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("test")
  .setSSL(SSL.Trusted)
  .addSocketOption(SocketOption.receiveBufferSize(4096))
  .setReadTimeout(30.seconds)
  .setDebug(true)
  .setAllowPublicKeyRetrieval(true)
```

#### jdbc-connector の設定

**旧方式 (0.3.x):**
```scala
import jdbc.connector.*

val dataSource = new com.mysql.cj.jdbc.MysqlDataSource()
// 手動設定

val provider = ConnectionProvider
  .fromDataSource[IO](dataSource, ec)
```

**新方式 (0.4.x):**
```scala
import jdbc.connector.*

// DataSource から Connector を作成
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("localhost")
ds.setPortNumber(3306)
ds.setDatabaseName("test")
ds.setUser("root")
ds.setPassword("password")

val connector = Connector.fromDataSource[IO](ds, ExecutionContexts.synchronous)

// DriverManager から Connector を作成
val connector = Connector.fromDriverManager[IO].apply(
  driver = "com.mysql.cj.jdbc.Driver",
  url = "jdbc:mysql://localhost:3306/test",
  user = "root",
  password = "password",
  logHandler = None
)

// MySQLDataSource 経由（ldbc-connector）
val dataSource = MySQLDataSource
  .fromDataSource[IO](ds, ExecutionContexts.synchronous)
```

### 4. コネクションプーリングの使用

#### 基本的な使用方法

```scala
import ldbc.connector.*
import ldbc.core.*
import scala.concurrent.duration.*

val config = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  // プール設定
  .setMinConnections(5)          // 最小接続数
  .setMaxConnections(20)         // 最大接続数
  .setConnectionTimeout(30.seconds)  // 接続タイムアウト
  .setIdleTimeout(10.minutes)        // アイドルタイムアウト
  .setMaxLifetime(30.minutes)        // 最大生存時間

MySQLDataSource.pooling[IO](config).use { pool =>
  // Connector を作成して使用
  val connector = Connector.fromDataSource(pool)

  // SQLクエリの実行
  sql"SELECT COUNT(*) FROM users"
    .query[Long]
    .unique
    .readOnly(connector)
}
```

#### メトリクス付きプール

```scala
import ldbc.connector.pool.*

val metricsResource = for {
  tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
  pool    <- MySQLDataSource.pooling[IO](
    config,
    metricsTracker = Some(tracker)
  )
} yield (pool, tracker)

metricsResource.use { case (pool, tracker) =>
  for {
    _ <- pool.getConnection.use(_.execute("SELECT 1"))
    metrics <- tracker.getMetrics
    _ <- IO.println(s"""
      |プールメトリクス:
      |  総接続数: ${metrics.totalCreated}
      |  アクティブ: ${metrics.activeConnections}
      |  アイドル: ${metrics.idleConnections}
    """.stripMargin)
  } yield ()
}
```

#### Before/After フック

```scala
case class RequestContext(requestId: String)

val poolWithHooks = MySQLDataSource.poolingWithBeforeAfter[IO, RequestContext](
  config = config,
  before = Some { conn =>
    for {
      id <- IO.randomUUID.map(_.toString)
      _  <- conn.execute(s"SET @request_id = '$id'")
    } yield RequestContext(id)
  },
  after = Some { (ctx, conn) =>
    IO.println(s"Request ${ctx.requestId} completed")
  }
)
```

### 5. 移行時の注意点

#### Scala Native での制限事項

@:callout(warning)
**重要**: Scala Native 0.4.x はシングルスレッド実行のみをサポートしています。そのため、Scala Native でコネクションプーリングを使用することは推奨されません。代わりに、各操作で新しいコネクションを作成してください：

```scala
// Scala Native での推奨使用方法
val dataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")

// プーリングは使用しない
val connector = Connector.fromDataSource(dataSource)

// DBIO の実行
sql"SELECT * FROM products WHERE price > $minPrice"
  .query[Product]
  .to[List]
  .readOnly(connector)
```
@:@

### 6. 破壊的変更

以下の API は削除または変更されています：

1. **ConnectionProvider**: 非推奨となり、`MySQLDataSource` に置き換えられました（0.5.xで削除予定）
2. **Provider トレイト**: 非推奨となり、`DataSource` トレイトに置き換えられました
3. **ldbc.sql.Provider**: 削除されました
4. **接続の直接使用**: 新しい `Connector` API を経由して使用する必要があります

### 7. DBIO 実行パターンの変更

DBIO の実行方法が変更され、より明確で柔軟になりました。

**旧方式 (0.3.x):**
```scala
provider.use { connection =>
  (for
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
  yield (result1, result2)).readOnly(connection)
}
```

**新方式 (0.4.x):**
```scala
val connector = Connector.fromDataSource(dataSource)

// 各種実行モード
sql"SELECT * FROM users".query[User].to[List].readOnly(connector)    // 読み取り専用
sql"INSERT INTO users ...".update.commit(connector)                 // コミット付き
sql"UPDATE users ...".update.transaction(connector)                 // トランザクション
sql"DELETE FROM users ...".update.rollback(connector)              // ロールバック

// 複数のクエリを組み合わせる
(for
  users <- sql"SELECT * FROM users".query[User].to[List]
  count <- sql"SELECT COUNT(*) FROM users".query[Long].unique
yield (users, count)).readOnly(connector)
```

### 8. 新機能

#### CircuitBreaker

コネクションプールには、データベース障害時の保護のための CircuitBreaker が組み込まれています：

- 連続した失敗後に自動的に接続試行を停止
- 指数バックオフによる段階的な回復
- アプリケーションとデータベースの両方を保護

#### アダプティブプールサイジング

負荷に基づいてプールサイズを動的に調整：

```scala
val config = MySQLConfig.default
  // ... 他の設定
  .setAdaptiveSizing(true)
  .setAdaptiveInterval(1.minute)
```

#### リーク検出

開発環境でのコネクションリークを検出：

```scala
val config = MySQLConfig.default
  // ... 他の設定
  .setLeakDetectionThreshold(2.minutes)
```

#### ストリーミングクエリのサポート

ldbcは`fs2.Stream`を使用した効率的なストリーミングクエリをサポートします。これにより、大量のデータを扱う際のメモリ使用量を抑えることができます。

**基本的な使用方法:**
```scala
import fs2.Stream
import ldbc.dsl.*

// デフォルトのfetchSize（1）でストリーミング
val stream: Stream[DBIO, String] = 
  sql"SELECT name FROM city"
    .query[String]
    .stream

// fetchSizeを指定したストリーミング
val streamWithFetchSize: Stream[DBIO, City] = 
  sql"SELECT * FROM city"
    .query[City]
    .stream(fetchSize = 100)
```

**実践的な使用例:**
```scala
// 大量データの効率的な処理
val processLargeCities: IO[List[String]] = 
  sql"SELECT name, population FROM city"
    .query[(String, Int)]
    .stream(1000)                    // 1000行ずつ取得
    .filter(_._2 > 1000000)          // 人口100万人以上
    .map(_._1)                       // 都市名のみ取得
    .take(50)                        // 最初の50件まで
    .compile.toList
    .readOnly(connector)

// 集計処理
val calculateTotal: IO[BigDecimal] = 
  sql"SELECT amount FROM transactions WHERE year = 2024"
    .query[BigDecimal]
    .stream(5000)                    // 5000行ずつ処理
    .filter(_ > 100)                 // 100円以上の取引
    .fold(BigDecimal(0))(_ + _)      // 合計を計算
    .compile.lastOrError
    .transaction(connector)
```

**MySQLでの最適化設定:**
```scala
// サーバーサイドカーソルを有効化してメモリ効率を向上
val datasource = MySQLDataSource
  .build[IO](host, port, user)
  .setPassword(password)
  .setDatabase(database)
  .setUseCursorFetch(true)  // 真のストリーミング処理を実現
```

**ストリーミングのメリット:**
- **メモリ効率**: 大量データでもメモリ使用量を一定に保てる
- **早期処理**: データを受信しながら同時に処理できる
- **中断可能**: 条件に応じて処理を途中で止められる
- **fs2の豊富な操作**: `filter`、`map`、`take`、`fold`などの関数型操作が利用可能

## まとめ

0.4.x への移行により、以下のメリットが得られます：

1. **パフォーマンスの向上**: 組み込みプーリングによる効率的なコネクション管理
2. **より直感的な API**: ビルダーパターンによる設定の簡素化
3. **高度な機能**: CircuitBreaker、アダプティブサイジング、メトリクス追跡
4. **外部依存の削減**: HikariCP が不要に

移行作業は主に API の更新で、機能的には後方互換性が保たれているため、段階的な移行が可能です。
