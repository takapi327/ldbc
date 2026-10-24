{%
  laika.title = "Q: Scalaコネクタでコネクションプールを使用する方法は？"
  laika.metadata.language = ja
%}

# Q: Scalaコネクタでコネクションプールを使用する方法は？

## A: 独立モジュール `ldbc-pool` を使用します。

0.9.x では、コネクションプーリングは独立モジュール `ldbc-pool` として提供されます。`ldbc-mysql` ドライバと組み合わせて使用します。このプーリングシステムはエフェクト非依存（`F: Concurrent`）で、ファイバーベースの並行性モデルを活用し、高いパフォーマンスと優れたリソース効率を提供します（以下は Cats Effect の例です）。

依存関係には `ldbc-mysql`・`ldbc-cats-effect`・`ldbc-pool` を追加します。

## クイックスタート

`MySQLDataSource`（接続情報）と `ConnectionPoolConfig`（プール設定）から `PooledDataSource.fromDataSource` でプールを作成します：

```scala 3
import cats.effect.*
import scala.concurrent.duration.*

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }

object ConnectionPoolExample extends IOApp.Simple:

  val run =
    // 接続情報
    val datasource = MySQLDataSource
      .build[IO]("localhost", 3306, "root")
      .setPassword("password")
      .setDatabase("testdb")
      .setSSL(SSL.Trusted)

    // コネクションプールの設定
    val poolConfig = ConnectionPoolConfig(
      minConnections    = 5,          // 最低5つの接続を維持
      maxConnections    = 20,         // 最大20接続まで
      connectionTimeout = 30.seconds  // 接続取得を最大30秒待機
    )

    // プールデータソースの作成
    PooledDataSource.fromDataSource[IO](poolConfig, datasource).use { pool =>
      // プールを Connector として使用
      sql"SELECT 'プール接続からこんにちは！'"
        .query[String]
        .to[Option]
        .readOnly(Connector.fromDataSource(pool))
        .flatMap(IO.println)
    }
```

## プール設定の詳細

`ConnectionPoolConfig` は、コネクションプールを細かく調整するための豊富な設定オプションを提供します：

```scala 3
val advancedConfig = ConnectionPoolConfig(
  // プールサイズ管理
  minConnections         = 10,              // 最小アイドル接続数
  maxConnections         = 50,              // 最大総接続数

  // タイムアウト設定
  connectionTimeout      = 30.seconds,      // 接続取得の最大待機時間
  validationTimeout      = 5.seconds,       // 接続検証のタイムアウト
  idleTimeout            = 10.minutes,      // アイドル接続の削除時間
  maxLifetime            = 30.minutes,      // 接続の最大生存時間

  // ヘルスチェック
  keepaliveTime          = Some(2.minutes), // アイドル接続の検証間隔

  // 高度な機能
  leakDetectionThreshold = Some(2.minutes), // 接続リークの警告
  adaptiveSizing         = true,            // 動的プールサイジング
  adaptiveInterval       = 1.minute         // プールサイズチェック間隔
)

// カスタム検証クエリは fromDataSource の引数で渡す:
//   PooledDataSource.fromDataSource[IO](advancedConfig, datasource, connectionTestQuery = Some("SELECT 1"))
```

## リソース安全性の確保

プールデータソースは`Resource`として管理され、適切なクリーンアップが保証されます：

```scala 3
import cats.effect.*

import ldbc.dsl.*
import ldbc.pool.PooledDataSource

// プールを Connector として受け取り、DSL でクエリを実行
def processUsers(pool: PooledDataSource[IO]): IO[List[String]] =
  sql"SELECT name FROM users"
    .query[String]
    .to[List]
    .readOnly(Connector.fromDataSource(pool))

// 使用方法（Resource が確実にプールを解放する）
val result = PooledDataSource.fromDataSource[IO](poolConfig, datasource).use { pool =>
  processUsers(pool)
}
```

## プールの健全性監視

組み込みメトリクスでプールのパフォーマンスを追跡：

```scala 3
import ldbc.dsl.*
import ldbc.pool.*

val monitoredPool = for
  tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
  pool    <- PooledDataSource.fromDataSource[IO](
               poolConfig,
               datasource,
               metricsTracker = Some(tracker)
             )
yield (pool, tracker)

monitoredPool.use { (pool, tracker) =>
  for
    // プールを使用
    _ <- sql"SELECT 1".query[Int].to[Option].readOnly(Connector.fromDataSource(pool))

    // メトリクスを確認
    metrics <- tracker.getMetrics
    _ <- IO.println(s"""
      |プールメトリクス:
      |  作成された総接続数: ${ metrics.totalCreations }
      |  総取得回数:         ${ metrics.totalAcquisitions }
      |  総返却回数:         ${ metrics.totalReleases }
      |  タイムアウト件数:   ${ metrics.timeouts }
      |  リーク件数:         ${ metrics.leaks }
      |  平均取得時間:       ${ metrics.acquisitionTime }
    """.stripMargin)
  yield ()
}
```

## 接続ライフサイクルフック

接続の取得・解放時にカスタム処理を追加：

```scala 3
import cats.syntax.all.*

import ldbc.pool.PooledDataSource

case class RequestContext(requestId: String, userId: String)

val poolWithHooks = PooledDataSource.fromDataSourceWithBeforeAfter[IO, RequestContext](
  poolConfig,
  datasource,
  before = { conn =>
    // セッション変数の設定や接続の準備
    val context = RequestContext("req-123", "user-456")
    conn.createStatement()
      .flatMap(_.executeUpdate(s"SET @request_id = '${ context.requestId }'"))
      .as(context)
  },
  after = { (context, conn) =>
    // 接続使用後のログ記録やクリーンアップ
    IO.println(s"リクエスト ${ context.requestId } の接続を解放しました")
  }
)
```

## 主な機能

### 組み込みCircuit Breaker
データベース障害から保護し、データベースがダウンしている場合は高速に失敗：
- 5回連続で失敗後、自動的にオープン状態に
- 再接続試行まで30秒待機
- 繰り返し失敗時は指数バックオフを使用

### ファイバー最適化
Cats Effectの軽量ファイバー向けに設計：
- 最小限のメモリオーバーヘッド（ファイバーあたり約150バイト vs スレッドあたり1-2MB）
- 非ブロッキングな接続取得
- 高並行性下での優れたパフォーマンス

### 包括的な検証
- 自動接続ヘルスチェック
- 設定可能な検証クエリ
- アイドル接続のキープアライブ
- 接続リーク検出

## JDBC/HikariCPからの移行

HikariCPから移行する場合の比較：

```scala 3
// HikariCPの設定
val hikariConfig = new HikariConfig()
hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3306/testdb")
hikariConfig.setUsername("root")
hikariConfig.setPassword("password")
hikariConfig.setMaximumPoolSize(20)
hikariConfig.setMinimumIdle(5)
hikariConfig.setConnectionTimeout(30000)

// 同等のldbc（ldbc-mysql + ldbc-pool）設定
val ldbcDataSource = MySQLDataSource
  .build[IO]("localhost", 3306, "root")
  .setPassword("password")
  .setDatabase("testdb")

val ldbcPoolConfig = ConnectionPoolConfig(
  minConnections    = 5,
  maxConnections    = 20,
  connectionTimeout = 30.seconds
)

// PooledDataSource.fromDataSource[IO](ldbcPoolConfig, ldbcDataSource)
```

## ベストプラクティス

1. **デフォルト設定から開始**: デフォルト設定はほとんどのアプリケーションで良好に動作します
2. **プールを監視**: メトリクス追跡で実際の使用パターンを理解
3. **適切なタイムアウト設定**: アプリケーションのSLA要件に基づいて設定
4. **リーク検出を有効化**: 開発/ステージング環境で接続リークを早期発見
5. **ライフサイクルフックを活用**: リクエスト追跡やセッション設定に

## 参考資料
- [コネクションプーリング](/ja/tutorial/Connection.md#コネクションプーリング)
- [コネクションプーリングアーキテクチャ](/ja/reference/Pooling.md)
- [パフォーマンスベンチマーク](/ja/reference/Pooling.md#ベンチマーク結果)
