{%
  laika.title = "Q: Scalaコネクタでコネクションプールを使用する方法は？"
  laika.metadata.language = ja
%}

# Q: Scalaコネクタでコネクションプールを使用する方法は？

## A: ldbc-connectorがコネクションプーリング機能を内蔵するようになりました！

バージョン0.4.0から、ldbc-connectorは包括的なコネクションプーリング機能を搭載しています。このプーリングシステムは、Cats Effectのファイバーベースの並行性モデル専用に設計されており、高いパフォーマンスと優れたリソース効率を提供します。

## クイックスタート

ldbc-connectorでプール化された接続を作成・使用する方法は以下の通りです：

```scala 3
import cats.effect.*
import ldbc.connector.*
import scala.concurrent.duration.*

object ConnectionPoolExample extends IOApp.Simple:

  val run = 
    // コネクションプールの設定
    val poolConfig = MySQLConfig.default
      .setHost("localhost")
      .setPort(3306)
      .setUser("root")
      .setPassword("password")
      .setDatabase("testdb")
      // プール固有の設定
      .setMinConnections(5)         // 最低5つの接続を維持
      .setMaxConnections(20)        // 最大20接続まで
      .setConnectionTimeout(30.seconds)  // 接続取得を最大30秒待機
      
    // プールデータソースの作成
    MySQLDataSource.pooling[IO](poolConfig).use { pool =>
      // プールから接続を使用
      pool.getConnection.use { connection =>
        for
          stmt   <- connection.createStatement()
          rs     <- stmt.executeQuery("SELECT 'プール接続からこんにちは！'")
          _      <- rs.next()
          result <- rs.getString(1)
          _      <- IO.println(result)
        yield ()
      }
    }
```

## プール設定の詳細

ldbc-connectorは、コネクションプールを細かく調整するための豊富な設定オプションを提供します：

```scala 3
val advancedConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("myapp")
  .setPassword("secret")
  .setDatabase("production")
  
  // プールサイズ管理
  .setMinConnections(10)          // 最小アイドル接続数
  .setMaxConnections(50)          // 最大総接続数
  
  // タイムアウト設定
  .setConnectionTimeout(30.seconds)   // 接続取得の最大待機時間
  .setIdleTimeout(10.minutes)        // アイドル接続の削除時間
  .setMaxLifetime(30.minutes)        // 接続の最大生存時間
  .setValidationTimeout(5.seconds)   // 接続検証のタイムアウト
  
  // ヘルスチェック
  .setKeepaliveTime(2.minutes)       // アイドル接続の検証間隔
  .setConnectionTestQuery("SELECT 1") // カスタム検証クエリ（オプション）
  
  // 高度な機能
  .setLeakDetectionThreshold(2.minutes)  // 接続リークの警告
  .setAdaptiveSizing(true)              // 動的プールサイジング
  .setAdaptiveInterval(1.minute)        // プールサイズチェック間隔
```

## リソース安全性の確保

プールデータソースは`Resource`として管理され、適切なクリーンアップが保証されます：

```scala 3
import cats.effect.*
import cats.implicits.*
import ldbc.connector.*

def processUsers[F[_]: Async: Network: Console](
  pool: PooledDataSource[F]
): F[List[String]] =
  pool.getConnection.use { conn =>
    conn.prepareStatement("SELECT name FROM users").use { stmt =>
      stmt.executeQuery().flatMap { rs =>
        // 結果を安全に反復処理
        LazyList.unfold(())(_ => 
          rs.next().map(hasNext => 
            if hasNext then Some((rs.getString("name"), ())) else None
          ).toOption.flatten
        ).compile.toList
      }
    }
  }

// 使用方法
val result = MySQLDataSource.pooling[IO](config).use { pool =>
  processUsers[IO](pool)
}
```

## プールの健全性監視

組み込みメトリクスでプールのパフォーマンスを追跡：

```scala 3
import ldbc.connector.pool.*

val monitoredPool = for
  tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
  pool    <- MySQLDataSource.pooling[IO](
    config,
    metricsTracker = Some(tracker)
  )
yield (pool, tracker)

monitoredPool.use { (pool, tracker) =>
  for
    // プールを使用
    _ <- pool.getConnection.use(conn => /* クエリ実行 */ IO.unit)
    
    // メトリクスを確認
    metrics <- tracker.getMetrics
    _ <- IO.println(s"""
      |プールメトリクス:
      |  作成された総接続数: ${metrics.totalCreated}
      |  アクティブ接続数: ${metrics.activeConnections}
      |  アイドル接続数: ${metrics.idleConnections}
      |  待機中リクエスト: ${metrics.waitingRequests}
      |  総取得回数: ${metrics.totalAcquisitions}
      |  平均待機時間: ${metrics.averageAcquisitionTime}ms
    """.stripMargin)
  yield ()
}
```

## 接続ライフサイクルフック

接続の取得・解放時にカスタム処理を追加：

```scala 3
case class RequestContext(requestId: String, userId: String)

val poolWithHooks = MySQLDataSource.poolingWithBeforeAfter[IO, RequestContext](
  config = config,
  before = Some { conn =>
    // セッション変数の設定や接続の準備
    val context = RequestContext("req-123", "user-456")
    conn.createStatement()
      .flatMap(_.executeUpdate(s"SET @request_id = '${context.requestId}'"))
      .as(context)
  },
  after = Some { (context, conn) =>
    // 接続使用後のログ記録やクリーンアップ
    IO.println(s"リクエスト ${context.requestId} の接続を解放しました")
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

// 同等のldbc-connector設定
val ldbcConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setDatabase("testdb")
  .setUser("root")
  .setPassword("password")
  .setMaxConnections(20)
  .setMinConnections(5)
  .setConnectionTimeout(30.seconds)
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
