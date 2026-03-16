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

### 5. サンプル Grafana ダッシュボード

コネクションプールの可観測性を素早く確認できる Grafana ダッシュボードを提供しています。Prometheus データソースが必要です。

**ダッシュボードに含まれるパネル:**
- DB Operation Duration（p50 / p95 / p99）
- Connection Pool Status（Active / Idle / Pending / Wait Time p99 / Pool Usage %）
- Connection Pool Breakdown（時系列グラフ）
- Latency Distribution（Wait / Create / Use の平均）
- Connection Timeouts（15分ウィンドウ）
- Connection Wait Time Heatmap

<img src="../img/dashboard.png" alt="Grafana ダッシュボード" width="600" />

以下の JSON を Grafana にインポートすることで、上記のダッシュボードを作成できます。

<details>
<summary>ダッシュボード JSON を表示（クリックで展開）</summary>

<pre><code class="language-json">{
  "__inputs": [
    {
      "name": "DS_PROMETHEUS",
      "label": "Prometheus",
      "description": "",
      "type": "datasource",
      "pluginId": "prometheus",
      "pluginName": "Prometheus"
    }
  ],
  "__elements": {},
  "__requires": [
    {
      "type": "grafana",
      "id": "grafana",
      "name": "Grafana",
      "version": "10.0.0"
    },
    {
      "type": "datasource",
      "id": "prometheus",
      "name": "Prometheus",
      "version": "1.0.0"
    },
    {
      "type": "panel",
      "id": "stat",
      "name": "Stat",
      "version": ""
    },
    {
      "type": "panel",
      "id": "gauge",
      "name": "Gauge",
      "version": ""
    },
    {
      "type": "panel",
      "id": "timeseries",
      "name": "Time series",
      "version": ""
    },
    {
      "type": "panel",
      "id": "heatmap",
      "name": "Heatmap",
      "version": ""
    },
    {
      "type": "panel",
      "id": "alertlist",
      "name": "Alert list",
      "version": ""
    }
  ],
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": {
          "type": "grafana",
          "uid": "-- Grafana --"
        },
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "name": "Annotations &amp; Alerts",
        "type": "dashboard"
      }
    ]
  },
  "description": "MySQL Connection Pool observability dashboard using OpenTelemetry Semantic Conventions (db.client.* metrics). Requires OTel Java Agent or manual OTel instrumentation with Prometheus exporter.",
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 1,
  "id": null,
  "links": [
    {
      "title": "OTel DB Metrics Spec",
      "url": "https://opentelemetry.io/docs/specs/semconv/db/database-metrics/",
      "type": "link",
      "icon": "external link",
      "targetBlank": true
    }
  ],
  "liveNow": false,
  "panels": [
    {
      "id": 102,
      "type": "row",
      "title": "DB Operation Duration (OTel Stable)",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 0 },
      "collapsed": false,
      "panels": []
    },
    {
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "type": "gauge",
      "id": 50,
      "title": "Operation Duration — p50",
      "gridPos": { "h": 7, "w": 8, "x": 0, "y": 1 },
      "targets": [
        {
          "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
          "expr": "histogram_quantile(0.50, sum by (le) (increase(db_client_operation_duration_seconds_bucket{db_system_name=\"mysql\", job=~\"${job}\"}[$__range])))",
          "refId": "A", "editorMode": "code", "range": false, "instant": true
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "s", "decimals": 2, "min": 0, "max": 0.1,
          "color": { "mode": "thresholds" },
          "thresholds": { "mode": "absolute", "steps": [
            { "color": "green", "value": null },
            { "color": "yellow", "value": 0.01 },
            { "color": "orange", "value": 0.05 },
            { "color": "red", "value": 0.1 }
          ]}
        }, "overrides": []
      },
      "options": { "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "showThresholdLabels": false, "showThresholdMarkers": true, "orientation": "auto" }
    },
    {
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "type": "gauge",
      "id": 51,
      "title": "Operation Duration — p95",
      "gridPos": { "h": 7, "w": 8, "x": 8, "y": 1 },
      "targets": [
        {
          "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
          "expr": "histogram_quantile(0.95, sum by (le) (increase(db_client_operation_duration_seconds_bucket{db_system_name=\"mysql\", job=~\"${job}\"}[$__range])))",
          "refId": "A", "editorMode": "code", "range": false, "instant": true
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "s", "decimals": 2, "min": 0, "max": 0.1,
          "color": { "mode": "thresholds" },
          "thresholds": { "mode": "absolute", "steps": [
            { "color": "green", "value": null },
            { "color": "yellow", "value": 0.01 },
            { "color": "orange", "value": 0.05 },
            { "color": "red", "value": 0.1 }
          ]}
        }, "overrides": []
      },
      "options": { "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "showThresholdLabels": false, "showThresholdMarkers": true, "orientation": "auto" }
    },
    {
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "type": "gauge",
      "id": 52,
      "title": "Operation Duration — p99",
      "gridPos": { "h": 7, "w": 8, "x": 16, "y": 1 },
      "targets": [
        {
          "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
          "expr": "histogram_quantile(0.99, sum by (le) (increase(db_client_operation_duration_seconds_bucket{db_system_name=\"mysql\", job=~\"${job}\"}[$__range])))",
          "refId": "A", "editorMode": "code", "range": false, "instant": true
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "s", "decimals": 2, "min": 0, "max": 0.1,
          "color": { "mode": "thresholds" },
          "thresholds": { "mode": "absolute", "steps": [
            { "color": "green", "value": null },
            { "color": "yellow", "value": 0.01 },
            { "color": "orange", "value": 0.05 },
            { "color": "red", "value": 0.1 }
          ]}
        }, "overrides": []
      },
      "options": { "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "showThresholdLabels": false, "showThresholdMarkers": true, "orientation": "auto" }
    },
    {
      "id": 100,
      "type": "row",
      "title": "Connection Pool Status",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 8 },
      "collapsed": false,
      "panels": []
    },
    {
      "id": 1, "title": "Active (used)", "type": "stat",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 5, "w": 5, "x": 0, "y": 9 },
      "targets": [{ "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_count{db_client_connection_state=\"used\", db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "{{db_client_connection_pool_name}}", "refId": "A", "editorMode": "code", "range": true }],
      "description": "db.client.connection.count{db.client.connection.state=used}",
      "options": { "reduceOptions": { "calcs": ["lastNotNull"] }, "graphMode": "area", "colorMode": "value" },
      "fieldConfig": { "defaults": { "color": { "mode": "fixed", "fixedColor": "light-blue" }, "unit": "short" }, "overrides": [] }
    },
    {
      "id": 2, "title": "Idle", "type": "stat",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 5, "w": 5, "x": 5, "y": 9 },
      "targets": [{ "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_count{db_client_connection_state=\"idle\", db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "{{db_client_connection_pool_name}}", "refId": "A", "editorMode": "code", "range": true }],
      "description": "db.client.connection.count{db.client.connection.state=idle}",
      "options": { "reduceOptions": { "calcs": ["lastNotNull"] }, "graphMode": "area", "colorMode": "value" },
      "fieldConfig": { "defaults": { "color": { "mode": "fixed", "fixedColor": "green" }, "unit": "short" }, "overrides": [] }
    },
    {
      "id": 3, "title": "Pending Requests", "type": "stat",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 5, "w": 5, "x": 10, "y": 9 },
      "targets": [{ "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_pending_requests{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "{{db_client_connection_pool_name}}", "refId": "A", "editorMode": "code", "range": true }],
      "description": "db.client.connection.pending_requests\n0以外が持続する場合はプール枯渇の前兆",
      "options": { "reduceOptions": { "calcs": ["lastNotNull"] }, "graphMode": "area", "colorMode": "value" },
      "fieldConfig": { "defaults": { "color": { "mode": "thresholds" }, "thresholds": { "mode": "absolute", "steps": [{ "color": "green", "value": null }, { "color": "yellow", "value": 1 }, { "color": "red", "value": 3 }] }, "unit": "short" }, "overrides": [] }
    },
    {
      "id": 4, "title": "Wait Time p99", "type": "stat",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 5, "w": 5, "x": 15, "y": 9 },
      "targets": [{ "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "histogram_quantile(0.99, sum by (le) (rate(db_client_connection_wait_time_seconds_bucket{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[$__rate_interval])))", "legendFormat": "p99", "refId": "A", "editorMode": "code", "range": true, "interval": "1m" }],
      "description": "db.client.connection.wait_time (p99)",
      "options": { "reduceOptions": { "calcs": ["lastNotNull"] }, "graphMode": "area", "colorMode": "value" },
      "fieldConfig": { "defaults": { "color": { "mode": "thresholds" }, "thresholds": { "mode": "absolute", "steps": [{ "color": "purple", "value": null }, { "color": "yellow", "value": 0.03 }, { "color": "red", "value": 0.1 }] }, "unit": "s", "decimals": 3 }, "overrides": [] }
    },
    {
      "id": 5, "title": "Pool Usage", "type": "gauge",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 5, "w": 4, "x": 20, "y": 9 },
      "targets": [{ "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_count{db_client_connection_state=\"used\", db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"} / ignoring(db_client_connection_state) db_client_connection_max{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "{{db_client_connection_pool_name}}", "refId": "A", "editorMode": "code", "range": false, "instant": true }],
      "description": "active / max",
      "options": { "reduceOptions": { "calcs": ["lastNotNull"] }, "showThresholdLabels": false, "showThresholdMarkers": true },
      "fieldConfig": { "defaults": { "color": { "mode": "thresholds" }, "thresholds": { "mode": "absolute", "steps": [{ "color": "green", "value": null }, { "color": "yellow", "value": 0.7 }, { "color": "red", "value": 0.9 }] }, "unit": "percentunit", "min": 0, "max": 1 }, "overrides": [] }
    },
    {
      "id": 6, "title": "Connection Pool Breakdown", "type": "timeseries",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 9, "w": 12, "x": 0, "y": 14 },
      "targets": [
        { "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_count{db_client_connection_state=\"used\", db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "Used (active)", "refId": "A", "editorMode": "code", "range": true },
        { "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_count{db_client_connection_state=\"idle\", db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "Idle", "refId": "B", "editorMode": "code", "range": true },
        { "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_pending_requests{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "Pending", "refId": "C", "editorMode": "code", "range": true },
        { "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "db_client_connection_max{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}", "legendFormat": "Max (limit)", "refId": "D", "editorMode": "code", "range": true }
      ],
      "description": "db.client.connection.count by state + max",
      "options": { "tooltip": { "mode": "multi", "sort": "desc" }, "legend": { "displayMode": "list", "placement": "bottom", "calcs": ["lastNotNull"] } },
      "fieldConfig": {
        "defaults": { "custom": { "drawStyle": "line", "lineInterpolation": "smooth", "fillOpacity": 30, "stacking": { "mode": "normal", "group": "A" }, "lineWidth": 1, "showPoints": "never", "gradientMode": "opacity", "axisLabel": "connections", "spanNulls": true }, "unit": "short", "color": { "mode": "palette-classic" } },
        "overrides": [
          { "matcher": { "id": "byName", "options": "Used (active)" }, "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "light-blue" } }] },
          { "matcher": { "id": "byName", "options": "Idle" }, "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "green" } }] },
          { "matcher": { "id": "byName", "options": "Pending" }, "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "orange" } }] },
          { "matcher": { "id": "byName", "options": "Max (limit)" }, "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "red" } }, { "id": "custom.fillOpacity", "value": 0 }, { "id": "custom.stacking", "value": { "mode": "none" } }, { "id": "custom.lineStyle", "value": { "fill": "dash", "dash": [10, 10] } }, { "id": "custom.lineWidth", "value": 2 }] }
        ]
      }
    },
    {
      "id": 7, "title": "Latency Distribution", "type": "bargauge",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 9, "w": 12, "x": 12, "y": 14 },
      "targets": [
        { "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "increase(db_client_connection_wait_time_seconds_sum{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[$__range]) / increase(db_client_connection_wait_time_seconds_count{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[$__range])", "legendFormat": "Wait — {{db_client_connection_pool_name}}", "refId": "A", "editorMode": "code", "range": false, "instant": true },
        { "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "increase(db_client_connection_create_time_seconds_sum{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[$__range]) / increase(db_client_connection_create_time_seconds_count{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[$__range])", "legendFormat": "Create — {{db_client_connection_pool_name}}", "refId": "B", "editorMode": "code", "range": false, "instant": true },
        { "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "increase(db_client_connection_use_time_seconds_sum{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[$__range]) / increase(db_client_connection_use_time_seconds_count{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[$__range])", "legendFormat": "Use — {{db_client_connection_pool_name}}", "refId": "C", "editorMode": "code", "range": false, "instant": true }
      ],
      "description": "選択期間内の平均レイテンシ\ndb.client.connection.{wait_time, create_time, use_time}",
      "options": { "reduceOptions": { "calcs": ["lastNotNull"], "fields": "", "values": false }, "orientation": "horizontal", "displayMode": "gradient", "showUnfilled": true, "minVizWidth": 8, "minVizHeight": 16, "valueMode": "color" },
      "fieldConfig": { "defaults": { "color": { "mode": "palette-classic" }, "unit": "s", "decimals": 4, "thresholds": { "mode": "absolute", "steps": [{ "color": "green", "value": null }, { "color": "yellow", "value": 0.05 }, { "color": "red", "value": 0.5 }] } }, "overrides": [] }
    },
    {
      "id": 8, "title": "Connection Timeouts", "type": "timeseries",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 8, "w": 12, "x": 0, "y": 23 },
      "targets": [{ "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "increase(db_client_connection_timeouts_total{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[15m])", "legendFormat": "Timeouts — {{db_client_connection_pool_name}}", "refId": "A", "editorMode": "code", "range": true, "instant": false, "interval": "15m" }],
      "description": "db.client.connection.timeouts\n15分間のタイムアウト発生数",
      "options": { "tooltip": { "mode": "multi", "sort": "desc" }, "legend": { "displayMode": "list", "placement": "bottom", "calcs": ["sum", "lastNotNull"] } },
      "fieldConfig": { "defaults": { "custom": { "drawStyle": "bars", "lineWidth": 1, "fillOpacity": 60, "showPoints": "never", "stacking": { "mode": "none" }, "gradientMode": "scheme" }, "unit": "short", "color": { "mode": "fixed", "fixedColor": "red" }, "noValue": "0" }, "overrides": [] }
    },
    {
      "id": 9, "title": "Connection Wait Time Heatmap", "type": "heatmap",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 8, "w": 12, "x": 12, "y": 23 },
      "targets": [{ "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "expr": "sum by (le) (increase(db_client_connection_wait_time_seconds_bucket{db_client_connection_pool_name=~\"${pool_name}\", job=~\"${job}\"}[15m]))", "legendFormat": "{{le}}", "refId": "A", "editorMode": "code", "range": true, "instant": false, "format": "heatmap", "interval": "15m" }],
      "description": "db.client.connection.wait_time bucket distribution",
      "options": { "calculate": true, "color": { "mode": "scheme", "scheme": "Oranges", "fill": "orange", "reverse": false, "exponent": 0.5, "steps": 32 }, "yAxis": { "unit": "s", "reverse": false, "max": 0.1, "decimals": 0 }, "cellGap": 1, "tooltip": { "show": true, "yHistogram": true }, "legend": { "show": true }, "calculation": { "xBuckets": { "mode": "size" }, "yBuckets": { "mode": "size" } } },
      "fieldConfig": { "defaults": { "custom": { "scaleDistribution": { "type": "log", "log": 2 } } }, "overrides": [] }
    },
    {
      "id": 10, "title": "Alert History", "type": "alertlist",
      "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" },
      "gridPos": { "h": 8, "w": 24, "x": 0, "y": 32 },
      "targets": [],
      "description": "",
      "options": { "viewMode": "list", "groupMode": "default", "groupBy": [], "maxItems": 10, "sortOrder": 3, "dashboardAlerts": true, "alertName": "", "alertInstanceLabelFilter": "", "stateFilter": { "firing": true, "pending": true, "noData": true, "normal": true, "error": true }, "folder": "", "tags": [] }
    }
  ],
  "refresh": "10s",
  "schemaVersion": 38,
  "style": "dark",
  "tags": ["mysql", "opentelemetry", "connection-pool", "otel", "db-client"],
  "templating": {
    "list": [
      { "current": { "selected": false, "text": "Prometheus", "value": "Prometheus" }, "hide": 0, "includeAll": false, "multi": false, "name": "DS_PROMETHEUS", "options": [], "query": "prometheus", "refresh": 1, "regex": "", "skipUrlSync": false, "type": "datasource" },
      { "allValue": "", "current": { "selected": true, "text": ["All"], "value": ["$__all"] }, "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "definition": "label_values(db_client_connection_count, db_client_connection_pool_name)", "hide": 0, "includeAll": true, "label": "Pool Name", "multi": true, "name": "pool_name", "options": [], "query": { "query": "label_values(db_client_connection_count, db_client_connection_pool_name)", "refId": "StandardVariableQuery" }, "refresh": 2, "regex": "", "skipUrlSync": false, "sort": 1, "type": "query" },
      { "allValue": "", "current": { "selected": true, "text": ["All"], "value": ["$__all"] }, "datasource": { "type": "prometheus", "uid": "${DS_PROMETHEUS}" }, "definition": "label_values(db_client_connection_count, job)", "hide": 0, "includeAll": true, "label": "Job", "multi": true, "name": "job", "options": [], "query": { "query": "label_values(db_client_connection_count, job)", "refId": "StandardVariableQuery" }, "refresh": 2, "regex": "", "skipUrlSync": false, "sort": 1, "type": "query" }
    ]
  },
  "time": { "from": "now-30m", "to": "now" },
  "timepicker": { "refresh_intervals": ["5s", "10s", "30s", "1m", "5m"] },
  "timezone": "",
  "title": "MySQL Connection Pool — Observability (OpenTelemetry)",
  "uid": "mysql-pool-otel",
  "version": 1,
  "weekStart": ""
}</code></pre>

</details>

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
