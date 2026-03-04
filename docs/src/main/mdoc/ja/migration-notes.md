{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.5.xから0.6.xへの移行)

## パッケージ

**全てのパッケージ**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.6.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 主要な変更点

### 1. OpenTelemetry テレメトリの大幅拡充

0.6.0 では OpenTelemetry Semantic Conventions v1.39.0 に準拠したテレメトリサポートが大幅に強化されました。

#### TelemetryConfig の追加

テレメトリの動作を細かく制御できる `TelemetryConfig` が追加されました。

```scala
import ldbc.connector.telemetry.TelemetryConfig

// デフォルト設定（仕様準拠）
val config = TelemetryConfig.default

// クエリサマリー生成を無効化
val noSummaryConfig = TelemetryConfig.withoutQueryTextExtraction

// 個別に設定を変更
val customConfig = TelemetryConfig.default
  .withoutQueryTextExtraction   // db.query.summary の自動生成を無効化
  .withoutSanitization          // クエリのサニタイズを無効化（機密データ露出に注意）
  .withoutInClauseCollapsing    // IN句の圧縮を無効化
```

| オプション                             | デフォルト  | 説明                                           |
|-----------------------------------|:------:|----------------------------------------------|
| `extractMetadataFromQueryText`    | `true` | クエリテキストから `db.query.summary` を生成してスパン名に使用する  |
| `sanitizeNonParameterizedQueries` | `true` | 非パラメータ化クエリのリテラルを `?` に置換する（仕様で必須）            |
| `collapseInClauses`               | `true` | `IN (?, ?, ?)` を `IN (?)` に圧縮してカーディナリティを抑制する |

#### DatabaseMetrics と Meter の追加

接続プールのメトリクス（待機時間・使用時間・タイムアウト件数）を OpenTelemetry の `Meter` 経由で記録できるようになりました。

```scala
import org.typelevel.otel4s.metrics.Meter
import ldbc.connector.*

// Meter を注入してプールメトリクスを有効化
MySQLDataSource.pooling[IO](
  config  = mysqlConfig,
  meter   = Some(summon[Meter[IO]])
).use { pool =>
  // プールの待機時間・使用時間・タイムアウトが自動記録される
  pool.getConnection.use { conn => ... }
}
```

#### MySQLDataSource への新メソッド

`MySQLDataSource` に `setMeter` と `setTelemetryConfig` が追加されました。

```scala
import org.typelevel.otel4s.metrics.Meter
import ldbc.connector.*
import ldbc.connector.telemetry.TelemetryConfig

val datasource = MySQLDataSource
  .build[IO](host = "localhost", port = 3306, user = "ldbc")
  .setPassword("password")
  .setDatabase("mydb")
  .setMeter(summon[Meter[IO]])
  .setTelemetryConfig(TelemetryConfig.default.withoutQueryTextExtraction)
```

#### pooling メソッドのシグネチャ変更

`MySQLDataSource.pooling` と `MySQLDataSource.poolingWithBeforeAfter` に `meter` と `telemetryConfig` パラメータが追加されました。既存のコードは引数なしで呼び出せるため後方互換性はありますが、メトリクスを使用する場合は更新が必要です。

**移行前 (0.5.x):**
```scala
MySQLDataSource.pooling[IO](
  config         = mysqlConfig,
  metricsTracker = Some(tracker),
  tracer         = Some(tracer)
).use { pool => ... }
```

**移行後 (0.6.x):**
```scala
MySQLDataSource.pooling[IO](
  config          = mysqlConfig,
  metricsTracker  = Some(tracker),
  meter           = Some(summon[Meter[IO]]),
  tracer          = Some(tracer),
  telemetryConfig = TelemetryConfig.default
).use { pool => ... }
```

#### TelemetryAttribute のキー名変更（破壊的変更）

`TelemetryAttribute` の定数名が Semantic Conventions v1.39.0 に合わせて変更されました。`TelemetryAttribute` を直接参照している場合は更新が必要です。

| 旧 (0.5.x)      | 新 (0.6.x)                 |
|----------------|---------------------------|
| `DB_SYSTEM`    | `DB_SYSTEM_NAME`          |
| `DB_OPERATION` | `DB_OPERATION_NAME`       |
| `DB_QUERY`     | `DB_QUERY_TEXT`           |
| `STATUS_CODE`  | `DB_RESPONSE_STATUS_CODE` |
| `VERSION`      | `DB_MYSQL_VERSION`        |
| `THREAD_ID`    | `DB_MYSQL_THREAD_ID`      |
| `AUTH_PLUGIN`  | `DB_MYSQL_AUTH_PLUGIN`    |

### 2. MySQL 9.x サポートの強化

0.6.0 から MySQL 9.x が正式にサポートされます。接続先の MySQL バージョンに応じて内部の挙動が自動的に切り替わります。

#### MySQL 9.3.0 以降の getCatalogs 変更

MySQL 9.3.0 以降では `getCatalogs()` の内部実装が `mysql` システムテーブルから `INFORMATION_SCHEMA.SCHEMATA` に切り替わります。ユーザーコードへの影響はありませんが、MySQL 9.3+ に接続している場合は `INFORMATION_SCHEMA` への SELECT 権限が必要になります。

#### getTablePrivileges の変更

`DatabaseMetaData.getTablePrivileges()` の内部実装が `mysql.tables_priv` から `INFORMATION_SCHEMA.TABLE_PRIVILEGES` に変更されました。

### 3. スキーマ: VECTOR 型の追加

`ldbc-schema` モジュールに MySQL の `VECTOR` 型を表す `DataType` が追加されました。

```scala
import ldbc.schema.*

class EmbeddingTable extends Table[Embedding]("embeddings"):
  def id:        Column[Long]         = column[Long]("id")
  def embedding: Column[Array[Float]] = column[Array[Float]]("embedding", VECTOR(1536))

  override def * = (id *: embedding).to[Embedding]
```

### 4. エラートレーシングの改善

Protocol 層でエラー発生時に `span.setStatus(StatusCode.Error, message)` が自動設定されるようになりました。分散トレーシングツール（Jaeger、Zipkin 等）でエラースパンが正しく表示されます。ユーザーコードへの変更は不要です。

## 移行ガイド

### TelemetryAttribute の定数名を更新する

`TelemetryAttribute` の定数を直接使用している場合は、新しいキー名に更新してください。

**移行前 (0.5.x):**
```scala
import ldbc.connector.telemetry.TelemetryAttribute

val attr = Attribute(TelemetryAttribute.DB_SYSTEM, "mysql")
val op   = Attribute(TelemetryAttribute.DB_OPERATION, "SELECT")
```

**移行後 (0.6.x):**
```scala
import ldbc.connector.telemetry.TelemetryAttribute

val attr = Attribute(TelemetryAttribute.DB_SYSTEM_NAME, "mysql")
val op   = Attribute(TelemetryAttribute.DB_OPERATION_NAME, "SELECT")
```

### OTel メトリクスを有効化する

接続プールのメトリクスを収集したい場合は、`Meter` インスタンスを渡してください。

```scala
import cats.effect.IO
import org.typelevel.otel4s.metrics.Meter
import ldbc.connector.*
import ldbc.connector.telemetry.TelemetryConfig

// otel4s のセットアップ（例）
OtelJava.global[IO].use { otel =>
  otel.meterProvider.get("ldbc").flatMap { meter =>
    MySQLDataSource.pooling[IO](
      config          = mysqlConfig,
      meter           = Some(meter),
      telemetryConfig = TelemetryConfig.default
    ).use { pool =>
      pool.getConnection.use { conn =>
        // db.client.connection.wait_duration などのメトリクスが記録される
        sql"SELECT 1".query[Int].to[List].readOnly(Connector.fromDataSource(pool))
      }
    }
  }
}
```

### テレメトリ設定をカスタマイズする

クエリテキストをスパン名に含めたくない場合や、サニタイズを制御したい場合は `TelemetryConfig` を設定してください。

```scala
import ldbc.connector.telemetry.TelemetryConfig

// スパン名を固定名にする（クエリテキストを解析しない）
val config = TelemetryConfig.withoutQueryTextExtraction

// 全て無効化（開発・デバッグ用途のみ推奨）
val debugConfig = TelemetryConfig.default
  .withoutQueryTextExtraction
  .withoutSanitization
  .withoutInClauseCollapsing
```

## まとめ

0.6.x への移行により、以下のメリットが得られます：

1. **OpenTelemetry 仕様準拠の強化**: Semantic Conventions v1.39.0 に準拠した属性キー名と動作
2. **プールメトリクスの可視化**: 接続待機時間・使用時間・タイムアウト件数を OTel メトリクスとして収集可能
3. **MySQL 9.x の正式サポート**: MySQL 9.3+ の API 変更に自動追従
4. **新しい VECTOR 型**: MySQL の `VECTOR` 型をスキーマ定義で直接扱える
5. **エラートレーシングの改善**: スパンのエラー状態が分散トレーシングツールで正確に表示される

ほとんどの変更は後方互換性があります。`TelemetryAttribute` の定数名を直接使用している場合のみ更新が必要です。
