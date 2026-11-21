{%
  laika.title = コネクション
  laika.metadata.language = ja
%}

# コネクション

前のページで[セットアップ](/ja/tutorial/Setup.md)は完了しました。このページでは、データベースに安全に接続するための方法を詳しく学びます。

ldbcでは、データベース接続を管理するための「コネクション」という概念が中心的な役割を果たします。コネクションはデータベースへの接続を開始し、クエリを実行し、接続を安全に閉じるためのリソースを提供します。この接続管理は、cats-effectのResource型を用いて安全に行われます。

## コネクタの種類

ldbcでは2種類の接続方法をサポートしています。

**jdbcコネクタ** - 標準的なJDBCドライバを使用した接続方法

- 既存のJDBCドライバをそのまま活用できる
- JDBCに慣れている開発者に親しみやすい
- 他のJDBCベースのツールとの連携が容易

**ldbcコネクタ** - ldbcが独自に最適化した専用コネクタ

- MySQLプロトコルに最適化されたパフォーマンス
- 機能面での拡張性が高い
- より多くの設定オプションが利用可能

それぞれの接続方法について詳しく見ていきましょう。

## JDBCコネクタの使用

JDBCコネクタは標準的なJDBCドライバを利用して接続を確立します。既存のJDBC知識を活かしたい場合や、他のJDBCベースのツールとの互換性を重視する場合におすすめです。

### 依存関係の追加

まず、必要な依存関係を追加します。jdbcコネクタを使用する場合は、MySQLのコネクタも一緒に追加する必要があります。

```scala
//> dep "@ORGANIZATION@::jdbc-connector:@VERSION@"
//> dep "com.mysql":"mysql-connector-j":"@MYSQL_VERSION@"
```

### DataSourceを用いたコネクション

最も一般的な方法は`DataSource`を使用する方法です。これにより接続プールなどの高度な機能も利用できます。

```scala
// 必要なインポート
import cats.effect.IO
import jdbc.connector.*

// MySQLのデータソースを設定
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")

// データソースを作成
val datasource = MySQLDataSource
  .fromDataSource[IO](ds, ExecutionContexts.synchronous)

// コネクションを使用する
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

### DriverManagerを用いたコネクション

`DriverManager`を使用して接続することも可能です。シンプルなアプリケーションや、スクリプト実行には便利です。

```scala
// 必要なインポート
import cats.effect.IO
import jdbc.connector.*

// DriverManagerからデータソースを作成
val datasource = MySQLDataSource
  .fromDriverManager[IO](
    "com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://127.0.0.1:13306/world",
    "ldbc",
    "password"
  )

// コネクションを使用する
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

### 既存のConnectionを使用

すでに確立されている`java.sql.Connection`オブジェクトがある場合は、それをラップして使用することもできます：

```scala
// 必要なインポート
import cats.effect.IO
import jdbc.connector.*

// 既存のjava.sql.Connection
val jdbcConnection: java.sql.Connection = ???

// ldbcのデータソースに変換
val datasource = MySQLDataSource.fromConnection[IO](jdbcConnection)

// コネクションを使用する
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

## ldbcコネクタの使用

ldbcコネクタはldbcが独自に開発した最適化されたコネクタで、より多くの設定オプションと柔軟性を提供します。

### 依存関係の追加

まず、必要な依存関係を追加します。

```scala
//> dep "@ORGANIZATION@::ldbc-mysql:@VERSION@"
//> dep "@ORGANIZATION@::ldbc-cats-effect:@VERSION@"
```

### 基本的な設定でのコネクション

最もシンプルな設定から始めましょう：

```scala
import cats.effect.IO
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*

// 基本的な設定でデータソースを作成
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")

// コネクションを使用する
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

### SSL設定を使用したコネクション

セキュアな接続を確立するためにSSL設定を追加できます：

※ Trustedは全ての証明書を受け入れることに注意してください。これは開発環境向けの設定です。

※ MySQL クリアテキストプラガブル認証などの一部の認証プラグインでは、セキュリティ上の理由からSSL/TLS接続が必須です。詳細は[リファレンスの認証セクション](/ja/reference/Connector.md#認証)を参照してください。

```scala
import cats.effect.IO
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted) // 開発 / 自己署名専用 — 下記の注意を参照

// コネクションを使用する
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
}
```

> **⚠️ セキュリティ注意:** `SSL.Trusted` は**すべての証明書を信頼**します。通信は暗号化されますが、サーバーの検証を行わないため **中間者攻撃(MITM)を防げません**。開発や自己署名証明書でのみ使用してください。**本番では `SSL.System`**(CA署名証明書をシステムトラストストアで検証)を使用してください。

ldbcはfs2が提供するすべてのTLSモードをサポートしています。以下は、利用可能なSSLモードのリストです：

| モード                            | プラットフォーム        | 詳細                                                                                 |
|--------------------------------|-----------------|------------------------------------------------------------------------------------|
| `SSL.None`                     | `JVM/JS/Native` | `ldbcはSSLを要求しません。これがデフォルトです。`                                                      |
| `SSL.Trusted`                  | `JVM/JS/Native` | `SSL経由で接続し、すべての証明書を信頼する（検証なし・MITMを防げない）。開発 / 自己署名専用。`                                |
| `SSL.System`                   | `JVM/JS/Native` | `SSL経由で接続し、サーバー証明書をシステムトラストストアで検証する。本番推奨（CA署名証明書）。` |
| `SSL.fromSSLContext(…)`	       | `JVM`           | `既存のSSLContextを使ってSSLで接続する。`                                                       |
| `SSL.fromKeyStoreFile(…)`	     | `JVM`           | `指定したキーストア・ファイルを使ってSSLで接続する。`                                                      |
| `SSL.fromKeyStoreResource(…)`	 | `JVM`           | `指定されたキーストア・クラスパス・リソースを使用して SSL 接続します。`                                            |
| `SSL.fromKeyStore(…)`	         | `JVM`           | `既存のキーストアを使用してSSL経由で接続する。`                                                         |
| `SSL.fromSecureContext(...)`   | `JS`            | `既存の SecureContext を使用して SSL 経由で接続します。`                                            |
| `SSL.fromS2nConfig(...)`       | `Native`        | `既存のS2nConfigを使用して、SSL経由で接続します。`                                                   |

### 高度な設定を使用したコネクション

さらに多くの設定オプションを活用することができます：

```scala
import scala.concurrent.duration.*
import cats.effect.IO
import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.{ SocketOptions, SSL }
import ldbc.catseffect.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setDebug(true)
  .setSSL(SSL.None)
  .setSocketOptions(SocketOptions.default.copy(receiveBufferSize = Some(4096)))
  .setReadTimeout(30.seconds)
  .setAllowPublicKeyRetrieval(true)

// LogHandler は Connector 生成時に渡す
val connector = Connector.fromDataSource(datasource, customLogHandler)

// コネクションを使用する
val program = sql"SELECT 1".query[Int].to[Option].readOnly(connector)
```

### Before/After処理の追加

コネクション確立後、または切断前に特定の処理を実行したい場合は、`withBefore`と`withAfter`メソッドを使用できます：

```scala
import cats.effect.IO
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.mysql.syntax.*

val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .withBefore { connection =>
    // コネクション確立後に実行される処理
    connection.createStatement().flatMap(_.executeUpdate("SET time_zone = '+09:00'"))
  }
  .withAfter { (result, connection) =>
    // コネクション切断前に実行される処理
    connection.createStatement().flatMap(_.executeUpdate("RESET time_zone")).void
  }

// コネクションを使用する
val program = datasource.use { conn =>
  conn.createStatement().flatMap(_.executeQuery("SELECT NOW()"))
}
```

## 設定可能なパラメータ一覧

ldbcコネクタでは以下のパラメータが設定可能です：

| プロパティ                     | 詳細                                                            | 必須 |
|---------------------------|---------------------------------------------------------------|----|
| `host`                    | `データベースホスト情報`                                                 | ✅  |
| `port`                    | `データベースポート情報`                                                 | ✅  |
| `user`                    | `データベースユーザー情報`                                                | ✅  |
| `password`                | `データベースパスワード情報 (default: None)`                               | ❌  |
| `database`                | `データベース名情報 (default: None)`                                   | ❌  |
| `debug`                   | `デバッグ情報を表示するかどうか  (default: false)`                           | ❌  |
| `ssl`                     | `SSLの設定 (default: SSL.None)`                                  | ❌  |
| `socketOptions`           | `TCP/UDP ソケットのソケットオプションを指定する (default: defaultSocketOptions)` | ❌  |
| `readTimeout`             | `タイムアウト時間を指定する (default: Duration.Inf)`                       | ❌  |
| `allowPublicKeyRetrieval` | `公開鍵を取得するかどうか (default: false)`                               | ❌  |
| `logHandler`              | `ログ出力設定`                                                      | ❌  |
| `before`                  | `コネクション確立後に実行する処理`                                            | ❌  |
| `after`                   | `コネクションを切断する前に実行する処理`                                         | ❌  |
| `tracer`                  | `メトリクス出力用のトレーサー設定 (default: Tracer.noop)`                     | ❌  |

## リソース管理とコネクションの利用

ldbcではcats-effectのResourceを使用してコネクションのライフサイクルを管理します。以下の2つの方法でコネクションを利用できます：

### useメソッド

シンプルに使う場合は`use`メソッドが便利です：

```scala
import ldbc.mysql.syntax.*

val result = datasource.use { conn =>
  // コネクションを利用した処理
  conn.createStatement().flatMap(_.executeQuery("SELECT * FROM users"))
}
```

### getConnectionメソッド

より細かいリソース管理が必要な場合は`getConnection`メソッドを使用します。`getConnection`はコネクションと解放処理のペア`(Connection[F], F[Unit])`を返すため、取得と解放を明示的に扱えます：

```scala 3
val program = datasource.getConnection.flatMap { (connection, release) =>
  connection
    .createStatement()
    .flatMap(_.executeQuery("SELECT * FROM users"))
    .guarantee(release)
}
```

これらの方法を用いることで、コネクションのオープン/クローズを安全に管理しながらデータベース操作を行うことができます。

## コネクションプーリング

0.9.x では、コネクションプーリングは独立したモジュール `ldbc-pool` として提供されます。`ldbc-mysql` ドライバと組み合わせて使用し、リクエストごとに新しいデータベース接続を作成する代わりに、既存のデータベース接続を再利用することで、パフォーマンスを大幅に向上させます。本番アプリケーションには不可欠な機能です。

`ldbc-pool` はエフェクト非依存（`F: Concurrent`）に実装されており、Cats Effect（`IO`）・ZIO（`Task`）・`Fx` のいずれでも利用できます。以下の例では Cats Effect を使用します。依存関係には `ldbc-mysql`・エフェクトブリッジ（`ldbc-cats-effect`）・`ldbc-pool` を追加します。

```scala
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-mysql"       % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-cats-effect" % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-pool"        % "@VERSION@"
)
```

### なぜコネクションプーリングを使うのか？

新しいデータベース接続の作成は、以下のような高コストな操作です：
- TCPハンドシェイクのためのネットワーク往復
- MySQL認証プロトコルの交換
- SSL/TLSネゴシエーション（有効な場合）
- サーバーリソースの割り当て

コネクションプーリングはこのオーバーヘッドを排除し、再利用可能な接続のプールを維持することで、以下の効果をもたらします：
- **パフォーマンスの向上**: 接続が再利用され、接続確立のオーバーヘッドが排除される
- **リソース管理の改善**: データベースへの同時接続数を制限
- **信頼性の向上**: 組み込みのヘルスチェックにより、健全な接続のみが使用される
- **自動リカバリー**: 障害が発生した接続は自動的に置き換えられる

### コネクションプールの作成

接続情報を持つ `MySQLDataSource` と、プールの挙動を定義する `ConnectionPoolConfig` を用意し、`PooledDataSource.fromDataSource` でプールを作成します。

```scala
import cats.effect.IO
import scala.concurrent.duration.*

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }

// 接続情報
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  .setSSL(SSL.Trusted)

// 基本的なプール設定
val poolConfig = ConnectionPoolConfig(
  minConnections    = 5,             // 最低5つの接続を準備
  maxConnections    = 20,            // 最大20接続まで
  connectionTimeout = 30.seconds     // 接続取得を最大30秒待機
)

// プールされたデータソースを作成
PooledDataSource.fromDataSource[IO](poolConfig, datasource).use { pool =>
  // プールを Connector として使用
  val connector = Connector.fromDataSource(pool)
  sql"SELECT 1".query[Int].to[Option].readOnly(connector)
}
```

### プール設定オプション

`ConnectionPoolConfig` は豊富な設定オプションを提供します（フィールド名と既定値）：

#### プールサイズ設定
- **minConnections**: 維持する最小アイドル接続数（必須）
- **maxConnections**: 許可される最大総接続数（必須）

#### タイムアウト設定
- **connectionTimeout**: プールから接続を待つ最大時間（デフォルト: 30秒）
- **validationTimeout**: 接続検証クエリのタイムアウト（デフォルト: 5秒）
- **idleTimeout**: アイドル接続が閉じられるまでの時間（デフォルト: 10分）
- **maxLifetime**: 接続の最大生存時間（デフォルト: 30分）

#### ヘルスチェックと検証
- **keepaliveTime**: アイドル接続の検証間隔（`Option`、デフォルト: なし）
- **aliveBypassWindow**: 最近使用された接続の検証をスキップ（デフォルト: 500ms）

#### 高度な機能
- **leakDetectionThreshold**: プールに返されない接続について警告（`Option`、デフォルト: なし）
- **adaptiveSizing**: 負荷に基づく動的プールサイジングを有効化（デフォルト: false）
- **adaptiveInterval**: プールサイズをチェック・調整する頻度（デフォルト: 30秒）
- **maintenanceInterval**: バックグラウンド保守タスクの実行間隔（デフォルト: 30秒）

#### その他
- **poolName**: ログ出力時のプール識別名（デフォルト: "ldbc-pool"）
- **debug**: デバッグログを有効化（デフォルト: false）

> 接続検証用のカスタムクエリ（`connectionTestQuery`）とプールログ（`poolLogger`）は、`ConnectionPoolConfig` ではなく `PooledDataSource.fromDataSource` / `fromConfig` の引数として渡します（後述）。

### 高度な設定例

```scala
import cats.effect.IO
import scala.concurrent.duration.*

import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource, PoolLogger }

// 接続情報
val datasource = MySQLDataSource
  .build[IO]("production-db.example.com", 3306, "app_user")
  .setPassword("secure_password")
  .setDatabase("production_db")
  .setSSL(SSL.Trusted)

// 高度なプール設定
val advancedConfig = ConnectionPoolConfig(
  // プールサイズ管理
  minConnections         = 10,             // 10接続を準備
  maxConnections         = 50,             // 最大50接続までスケール

  // タイムアウト設定
  connectionTimeout      = 30.seconds,     // 接続取得の最大待機時間
  validationTimeout      = 5.seconds,      // 接続検証のタイムアウト
  idleTimeout            = 10.minutes,     // アイドル接続の削除時間
  maxLifetime            = 30.minutes,     // 接続の置き換え時間

  // ヘルスチェック
  keepaliveTime          = Some(2.minutes),  // 2分ごとにアイドル接続を検証

  // 高度な機能
  leakDetectionThreshold = Some(2.minutes),  // リークされた接続について警告
  adaptiveSizing         = true,             // 動的プールサイジングを有効化
  adaptiveInterval       = 1.minute,         // 1分ごとにプールサイズをチェック
  poolName               = "production-pool" // ログ出力時のプール名
)

// プールを作成して使用（カスタム検証クエリとログは fromDataSource の引数で渡す）
PooledDataSource
  .fromDataSource[IO](
    advancedConfig,
    datasource,
    connectionTestQuery = Some("SELECT 1"),
    poolLogger          = Some(PoolLogger.console[IO]())
  )
  .use { pool =>
    // アプリケーションコード
    IO.unit
  }
```

### プーリングでの接続ライフサイクルフック

接続がプールから取得されたり返却されたりする際に実行されるカスタムロジックを追加できます：

```scala
import ldbc.sql.Connection

case class RequestContext(requestId: String, userId: String)

// フックを定義（before の結果型を after が受け取る）
val beforeHook: Connection[IO] => IO[RequestContext] = conn =>
  for
    context <- IO(RequestContext("req-123", "user-456"))
    _       <- conn.createStatement()
                 .flatMap(_.executeUpdate(s"SET @request_id = '${ context.requestId }'"))
  yield context

val afterHook: (RequestContext, Connection[IO]) => IO[Unit] = (context, conn) =>
  IO.println(s"リクエスト ${ context.requestId } の接続を解放しました")

// フック付きプールを作成
PooledDataSource
  .fromDataSourceWithBeforeAfter[IO, RequestContext](
    poolConfig,
    datasource,
    before = beforeHook,
    after  = afterHook
  )
  .use { pool =>
    // 接続にはセッション変数が設定されている
    sql"SELECT @request_id".query[String].to[Option].readOnly(Connector.fromDataSource(pool))
  }
```

### プールヘルスの監視

組み込みのメトリクスでプールのパフォーマンスを追跡：

```scala
import ldbc.pool.*

// メトリクス追跡付きプールを作成
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
    _       <- sql"SELECT * FROM users".query[String].to[List].readOnly(Connector.fromDataSource(pool))

    // メトリクスを確認
    metrics <- tracker.getMetrics
    _       <- IO.println(s"""
      |プールメトリクス:
      |  作成された総接続数: ${ metrics.totalCreations }
      |  取得回数:           ${ metrics.totalAcquisitions }
      |  返却回数:           ${ metrics.totalReleases }
      |  タイムアウト件数:   ${ metrics.timeouts }
      |  リーク件数:         ${ metrics.leaks }
      |  平均取得時間:       ${ metrics.acquisitionTime }
    """.stripMargin)
  yield ()
}
```

### プール状態ログの有効化

`ldbc-pool`はHikariCPに影響を受けた詳細なプール状態ログを提供します。これにより、プールの動作を可視化し、パフォーマンスの問題を診断できます。ログは`PoolLogger`を`poolLogger`引数に渡すことで有効化し、プール名は`ConnectionPoolConfig`の`poolName`で指定します：

```scala
import cats.effect.IO
import scala.concurrent.duration.*

import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource, PoolLogger }

// 接続情報
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "myuser")
  .setPassword("mypassword")
  .setDatabase("mydb")
  .setSSL(SSL.Trusted)

// プール設定（プール名を指定）
val loggedPoolConfig = ConnectionPoolConfig(
  minConnections = 5,
  maxConnections = 20,
  poolName       = "app-pool"  // ログ内でプールを識別する名前
)

// PoolLogger を渡してプールを作成
PooledDataSource
  .fromDataSource[IO](loggedPoolConfig, datasource, poolLogger = Some(PoolLogger.console[IO]()))
  .use { pool =>
    // プールを使用すると以下のようなログが出力される：
    // [INFO] app-pool - Stats (total=5, active=2, idle=3, waiting=0)
    sql"SELECT * FROM users".query[String].to[List].readOnly(Connector.fromDataSource(pool))
  }
```

プールログが有効な場合、以下のような情報がログに記録されます：

```
// プール状態の定期ログ
[INFO] app-pool - Stats (total=10, active=3, idle=7, waiting=0)

// 接続作成失敗
[ERROR] Failed to create connection to localhost:3306 (database: mydb): Connection refused

// 接続取得タイムアウト（詳細な診断情報付き）
[ERROR] Connection acquisition timeout after 30 seconds (host: localhost:3306, db: mydb, pool: 20/20, active: 20, idle: 0, waiting: 5)

// 接続バリデーション失敗
[WARN] Connection conn-123 failed validation, removing from pool

// 接続リーク検出
[WARN] Possible connection leak detected: Connection conn-456 has been in use for longer than 2 minutes
```

### プールアーキテクチャの機能

ldbcコネクションプールには、いくつかの高度な機能が含まれています：

#### サーキットブレーカー保護
データベース障害時の接続ストームを防ぎます：
- 連続した障害後に自動的にオープン
- リトライ試行に指数バックオフを使用
- アプリケーションとデータベースの両方を保護

#### ロックフリー設計
高パフォーマンスのためのConcurrentBagデータ構造を使用：
- 高並行性下での最小限の競合
- 接続再利用のためのスレッドローカル最適化
- 優れたスケーラビリティ特性

#### アダプティブプールサイジング
負荷に基づいてプールサイズを動的に調整：
- 高需要時にプールを拡大
- 低使用期間中に縮小
- リソースの無駄を防ぐ

#### 詳細なプールロギング
HikariCPに影響を受けた包括的なログシステム：
- **プール状態ログ**: 接続数、アクティブ/アイドル接続、待機キューサイズを定期的に出力
- **接続ライフサイクルログ**: 接続の作成、検証、削除時に詳細情報を記録
- **エラー診断**: 接続取得タイムアウト時に詳細なプール状態を出力
- **リーク検出ログ**: 設定された閾値を超えて使用中の接続を警告

### ベストプラクティス

1. **保守的な設定から始める**: デフォルト値から始めて、監視に基づいて調整
2. **プールメトリクスを監視**: メトリクスを使用して実際の使用パターンを理解
3. **適切なタイムアウトを設定**: ユーザー体験とリソース保護のバランスを取る
4. **開発環境でリーク検出を有効化**: 接続リークを早期に発見
5. **接続テストクエリは控えめに使用**: オーバーヘッドが発生するため、可能な限りMySQLのisValidを使用
6. **本番環境でプールログを有効化**: トラブルシューティングとパフォーマンス分析のため
7. **ワークロードを考慮**: 
   - 高スループットアプリケーション: より大きなプールサイズ
   - バースト的なワークロード: アダプティブサイジングを有効化
   - 長時間実行クエリ: 接続タイムアウトを増やす
   - デバッグとトラブルシューティング: プールログを有効化して問題を迅速に特定

### 非プール接続からの移行

プール接続への移行は簡単です：

```scala
// 移行前: 直接接続
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")

// 移行後: 同じ datasource を PooledDataSource でラップする
val pool = PooledDataSource.fromDataSource[IO](
  ConnectionPoolConfig(minConnections = 5, maxConnections = 20),
  datasource
)
```

接続情報（`MySQLDataSource`）はそのまま再利用でき、プールの挙動は`ConnectionPoolConfig`で指定します。`PooledDataSource`は`DataSource`なので、`Connector.fromDataSource`に渡す使い方も直接接続時と同じです。プールは裏側ですべての複雑さを処理します。

コネクションプーリングアーキテクチャと実装の詳細については、[コネクションプーリングアーキテクチャ](/ja/reference/Pooling.md)のリファレンスドキュメントを参照してください。
