{%
laika.title = テレメトリ
laika.metadata.language = ja
%}

# テレメトリ（トレースとメトリクス）

ldbcは[OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/db/)に準拠した分散トレーシングとメトリクス収集をサポートしています。内部では[otel4s](https://typelevel.org/otel4s/)を使用しており、JVM・JS・Native のすべてのプラットフォームで動作します。

OpenTelemetryバックエンドを接続しない場合、トレースとメトリクスは自動的にno-op（何もしない）となり、パフォーマンスへの影響はありません。

## 依存関係

ldbc-connectorは`otel4s-core`（抽象API）のみに依存しています。実際にテレメトリデータを収集・送信するには、バックエンド実装を追加する必要があります。

```scala
libraryDependencies ++= Seq(
  // ldbcコネクタ（otel4s-coreを含む）
  "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@",

  // OpenTelemetry Java SDKバックエンド
  "org.typelevel"    %% "otel4s-oteljava"                           % "0.15.0",
  "io.opentelemetry"  % "opentelemetry-exporter-otlp"               % "1.58.0" % Runtime,
  "io.opentelemetry"  % "opentelemetry-sdk-extension-autoconfigure" % "1.58.0" % Runtime,
)
```

## セットアップ

`Tracer[F]`と`Meter[F]`を`MySQLDataSource`に設定します。

```scala
import cats.effect.*
import io.opentelemetry.api.GlobalOpenTelemetry
import org.typelevel.otel4s.oteljava.OtelJava
import ldbc.connector.*

object Main extends IOApp.Simple:

  override def run: IO[Unit] =
    val resource = for
      otel   <- Resource
                   .eval(IO.delay(GlobalOpenTelemetry.get))
                   .evalMap(OtelJava.forAsync[IO])
      tracer <- Resource.eval(otel.tracerProvider.get("my-app"))
      meter  <- Resource.eval(otel.meterProvider.get("my-app"))
      datasource = MySQLDataSource
                     .build[IO]("127.0.0.1", 3306, "user")
                     .setPassword("password")
                     .setDatabase("mydb")
                     .setTracer(tracer)
                     .setMeter(meter)
      connection <- datasource.getConnection
    yield connection

    resource.use { conn =>
      conn.createStatement().flatMap(_.executeQuery("SELECT 1")).void
    }
```

コネクションプーリング使用時は`meter`を`pooling`メソッドに渡します。

```scala
val pool = MySQLDataSource.pooling[IO](
  config = MySQLConfig.default
    .setHost("127.0.0.1")
    .setPort(3306)
    .setUser("user")
    .setPassword("password")
    .setDatabase("mydb"),
  meter  = Some(meter),
  tracer = Some(tracer)
)
```

## トレーシング

### 概要

ldbcは、データベース操作ごとにOpenTelemetryスパンを生成します。スパンは`exchange`機構を通じて作成され、MySQL プロトコルの直列性を保証する`Mutex`と連携しています。

### スパン名

スパン名はOpenTelemetry Semantic Conventionsの優先順位に従って生成されます：

1. `db.query.summary`が利用可能な場合（例: `SELECT users`）
2. `{db.operation.name} {target}`（例: `SELECT users`）
3. `{target}`のみ
4. フォールバック: `mysql`

`TelemetryConfig`の`extractMetadataFromQueryText`が`true`（デフォルト）の場合、SQLからオペレーション名とテーブル名を自動抽出し、動的なスパン名を生成します。`false`の場合は`Execute Statement`のような固定のスパン名を使用します。

以下はldbcが生成する主なスパンの一覧です：

| スパン名                               | 説明                               |
|------------------------------------|----------------------------------|
| `{operation} {table}`              | `SQL文から動的に生成（例: `SELECT users`）` |
| `Execute Statement`                | `固定スパン名モード時のStatement実行`         |
| `Execute Prepared Statement`       | `固定スパン名モード時のPreparedStatement実行` |
| `Execute Statement Batch`          | `Statementのバッチ実行`                |
| `Execute Prepared Statement Batch` | `PreparedStatementのバッチ実行`        |
| `Callable Statement`               | `CallableStatementの実行`           |
| `Create Connection`                | `コネクション確立`                       |
| `Close Connection`                 | `コネクション切断`                       |
| `Deallocate Prepared Statement`    | `サーバー側PreparedStatementの解放`      |
| `Commit`                           | `トランザクションコミット`                   |
| `Rollback`                         | `トランザクションロールバック`                 |

### スパン属性

各スパンには以下の属性が付与されます：

| 属性                        | 要求レベル                    | 説明               | 値の例                                |
|---------------------------|--------------------------|------------------|------------------------------------|
| `db.system.name`          | `Required`               | `DBMS名`          | `mysql`                            |
| `server.address`          | `Recommended`            | `ホスト名`           | `127.0.0.1`                        |
| `server.port`             | `Conditionally Required` | `ポート番号`          | `3306`                             |
| `db.namespace`            | `Conditionally Required` | `データベース名`        | `mydb`                             |
| `db.query.text`           | `Opt-In`                 | `実行されたSQL`       | `SELECT * FROM users WHERE id = ?` |
| `db.collection.name`      | `Conditionally Required` | `テーブル名`          | `users`                            |
| `db.operation.name`       | `Conditionally Required` | `操作名`            | `SELECT`                           |
| `db.operation.batch.size` | `Conditionally Required` | `バッチサイズ（2以上）`    | `10`                               |
| `db.mysql.thread_id`      | —                        | `MySQLスレッドID`    | `42`                               |
| `db.mysql.version`        | —                        | `MySQLサーバーバージョン` | `8.0.32`                           |
| `error.type`              | `Conditionally Required` | `エラー種別（失敗時のみ）`   | `SQLException`                     |

### エラー記録

データベース操作でエラーが発生した場合、スパンには以下が記録されます：

- スパンのステータスが`Error`に設定される
- `error.type`属性にエラーの型名が記録される
- `recordException`によりスタックトレースがスパンイベントとして記録される
- MySQLからの`ERRPacket`の場合、エラーコードやSQLStateも属性として記録される

### TelemetryConfig

`TelemetryConfig`を使用して、テレメトリの動作をカスタマイズできます。

```scala
import ldbc.connector.telemetry.TelemetryConfig

// デフォルト設定（すべて有効）
val default = TelemetryConfig.default

// SQLからのメタデータ抽出を無効にする（固定スパン名を使用）
val fixed = TelemetryConfig.withoutQueryTextExtraction

// 個別設定
val custom = TelemetryConfig.default
  .withoutQueryTextExtraction      // 固定スパン名を使用
  .withoutSanitization             // クエリテキストのサニタイズを無効化
  .withoutInClauseCollapsing       // IN句の折りたたみを無効化
```

| 設定                                | デフォルト  | 説明                                     |
|-----------------------------------|--------|----------------------------------------|
| `extractMetadataFromQueryText`    | `true` | `SQLからオペレーション名・テーブル名を抽出してスパン名を動的に生成する` |
| `sanitizeNonParameterizedQueries` | `true` | `パラメータ化されていないクエリのリテラル値を`?`に置換する`       |
| `collapseInClauses`               | `true` | `IN (?, ?, ?)`を`IN (?)`に折りたたむ          |

#### クエリのサニタイズ

`sanitizeNonParameterizedQueries`が有効な場合、`db.query.text`属性に記録されるSQLから文字列リテラル、数値、NULL等が`?`に置換されます。これにより、センシティブなデータがトレースに含まれることを防ぎます。

```
-- 元のSQL
SELECT * FROM users WHERE name = 'Alice' AND age > 25

-- サニタイズ後（db.query.text に記録される値）
SELECT * FROM users WHERE name = ? AND age > ?
```

#### IN句の折りたたみ

`collapseInClauses`が有効な場合、IN句のパラメータ数が可変でもスパンのカーディナリティを抑えます。

```
-- 元のSQL
SELECT * FROM users WHERE id IN (?, ?, ?, ?)

-- 折りたたみ後
SELECT * FROM users WHERE id IN (?)
```

## メトリクス

### 概要

ldbcは[OpenTelemetry Database Metrics Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/db/database-metrics/)に準拠したメトリクスを収集します。メトリクスは**オペレーションメトリクス**と**コネクションプールメトリクス**の2種類に分かれます。

### オペレーションメトリクス

全ての`Statement`、`PreparedStatement`、`CallableStatement`の実行で自動的に記録されます。

#### db.client.operation.duration

| 項目              | 内容                                               |
|-----------------|--------------------------------------------------|
| **Instrument型** | `Histogram`                                      |
| **単位**          | `s（秒）`                                           |
| **安定性**         | `Stable`                                         |
| **説明**          | `データベースクライアント操作の所要時間`                            |
| **バケット境界**      | `[0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1, 5, 10]` |

`executeQuery`、`executeUpdate`、`executeBatch`のすべてで記録されます。キャンセルされた操作は記録されません。

#### db.client.response.returned_rows

| 項目              | 内容                                                              |
|-----------------|-----------------------------------------------------------------|
| **Instrument型** | `Histogram`                                                     |
| **単位**          | `{row}`                                                         |
| **安定性**         | `Development`                                                   |
| **説明**          | `クエリによって返された行数`                                                 |
| **バケット境界**      | `[1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000]` |

`executeQuery`系のメソッドでのみ記録されます。`executeUpdate`や`executeBatch`では記録されません。

@:callout(warning)

**カーソルフェッチ使用時の制限事項**

`useCursorFetch=true`の場合、`StreamingResultSet`が使用され行は`fetchSize`単位で逐次取得されます。この場合、`db.client.response.returned_rows`には初回フェッチ分の行数のみが記録されます。`next()`呼び出しで後続フェッチされた行は含まれません。

これはOpenTelemetry Semantic Conventionsがストリーミング/カーソルベースのResultSetに対する明確なガイダンスを提供していないことに起因します。MySQL Connector/JおよびOpenTelemetry公式JDBCインストルメンテーションはいずれも`db.client.response.returned_rows`自体を未実装であり、業界全体で未解決の課題です。

`useCursorFetch=false`（デフォルト）の場合は全行が一度に取得されるため、正確な値が記録されます。

@:@

#### メトリクス属性

オペレーションメトリクスには以下の低カーディナリティ属性が付与されます。トレース属性とは異なり、メトリクスの集約コストを抑えるため高カーディナリティな属性（`db.query.text`、`db.mysql.thread_id`等）は除外されています。

| 属性               | 説明        | 値の例         |
|------------------|-----------|-------------|
| `db.system.name` | `DBMS名`   | `mysql`     |
| `server.address` | `ホスト名`    | `127.0.0.1` |
| `server.port`    | `ポート番号`   | `3306`      |
| `db.namespace`   | `データベース名` | `mydb`      |

### コネクションプールメトリクス

コネクションプーリング（`PooledDataSource`）使用時にのみ記録されます。すべてのメトリクスは`db.client.connection.pool.name`属性を持ちます。

#### Histogram メトリクス

| メトリクス名                             | 単位  | 説明                         |
|------------------------------------|-----|----------------------------|
| `db.client.connection.create_time` | `s` | `新しい物理コネクションの作成にかかった時間`    |
| `db.client.connection.wait_time`   | `s` | `プールからコネクションを取得するまでの待ち時間`  |
| `db.client.connection.use_time`    | `s` | `コネクションが借りられてから返却されるまでの時間` |

#### Counter メトリクス

| メトリクス名                          | 単位          | 説明                    |
|---------------------------------|-------------|-----------------------|
| `db.client.connection.timeouts` | `{timeout}` | `コネクション取得のタイムアウト発生回数` |

#### ゲージメトリクス（Observable）

以下のメトリクスはOTelの`BatchCallback`を通じてエクスポート時に報告されます。

| メトリクス名                                  | 単位             | 説明                                     |
|-----------------------------------------|----------------|----------------------------------------|
| `db.client.connection.count`            | `{connection}` | `現在のコネクション数（`state`属性: `idle`/`used`）` |
| `db.client.connection.idle.max`         | `{connection}` | `アイドルコネクションの最大許容数`                     |
| `db.client.connection.idle.min`         | `{connection}` | `アイドルコネクションの最小維持数`                     |
| `db.client.connection.max`              | `{connection}` | `コネクションの最大許容数`                         |
| `db.client.connection.pending_requests` | `{request}`    | `コネクション取得待ちのリクエスト数`                    |

## トレースとメトリクスの違い

ldbcではトレースとメトリクスで異なる属性を使用しています。これはOpenTelemetryのベストプラクティスに従い、メトリクスのカーディナリティを適切に管理するためです。

| 属性                   | トレース | メトリクス | 理由                   |
|----------------------|------|-------|----------------------|
| `db.system.name`     | ✅    | ✅     | `低カーディナリティ`          |
| `server.address`     | ✅    | ✅     | `低カーディナリティ`          |
| `server.port`        | ✅    | ✅     | `低カーディナリティ`          |
| `db.namespace`       | ✅    | ✅     | `低カーディナリティ`          |
| `db.query.text`      | ✅    | ❌     | `クエリごとに異なる高カーディナリティ` |
| `db.collection.name` | ✅    | ❌     | `テーブル名はスパン属性として十分`   |
| `db.mysql.thread_id` | ✅    | ❌     | `コネクションごとに変動する`      |
| `db.mysql.version`   | ✅    | ❌     | `バージョン情報はスパンで十分`     |
| `error.type`         | ✅    | ❌     | `エラーの詳細はスパンで追跡`      |

## Tracer/Meterの選択的使用

`Tracer`と`Meter`は独立して使用できます。

```scala
// トレースのみ（メトリクスなし）
val ds = MySQLDataSource.build[IO]("localhost", 3306, "user")
  .setTracer(tracer)

// メトリクスのみ（トレースなし）
val ds = MySQLDataSource.build[IO]("localhost", 3306, "user")
  .setMeter(meter)

// 両方
val ds = MySQLDataSource.build[IO]("localhost", 3306, "user")
  .setTracer(tracer)
  .setMeter(meter)

// どちらも設定しない（no-op、パフォーマンス影響なし）
val ds = MySQLDataSource.build[IO]("localhost", 3306, "user")
```
