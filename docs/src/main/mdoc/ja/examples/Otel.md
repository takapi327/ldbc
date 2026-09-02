{%
laika.title = OpenTelemetry
laika.metadata.language = ja
%}

# OpenTelemetryによる可観測性

このページでは、ldbcとOpenTelemetryを連携させて、データベースクエリのトレーシング、メトリクス収集などの可観測性を実現する方法について説明します。

テレメトリの詳細なAPIリファレンス（スパン属性一覧、TelemetryConfig、メトリクス仕様など）については [テレメトリリファレンス](../reference/Telemetry.md) を参照してください。

## 必要な依存関係

ldbcでOpenTelemetryを使用するには、以下の依存関係をプロジェクトに追加します：

```scala
libraryDependencies ++= Seq(
  // MySQL ドライバと Cats Effect ブリッジ
  "@ORGANIZATION@" %% "ldbc-mysql"       % "@VERSION@",
  "@ORGANIZATION@" %% "ldbc-cats-effect" % "@VERSION@",

  // ldbc.telemetry SPI の otel4s バックエンド
  "@ORGANIZATION@" %% "ldbc-otel4s"      % "@VERSION@",

  // otel4sライブラリ（OpenTelemetryのScala向けラッパー）
  "org.typelevel"    %% "otel4s-oteljava"                           % "0.15.1",

  // OpenTelemetryエクスポーター（データ送信用）
  "io.opentelemetry"  % "opentelemetry-exporter-otlp"               % "1.59.0" % Runtime,

  // 自動設定機能（環境変数やシステムプロパティによる構成）
  "io.opentelemetry"  % "opentelemetry-sdk-extension-autoconfigure" % "1.59.0" % Runtime,
)
```

## 基本セットアップ

`MySQLDataSource`に`setTracer`と`setMeter`を設定することでトレーシングとメトリクス収集の両方を有効化できます。

```scala
import cats.effect.*
import io.opentelemetry.api.GlobalOpenTelemetry
import org.typelevel.otel4s.oteljava.OtelJava
import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.otel4s.Otel4sTelemetry

val serviceName = "my-ldbc-app"

val resource: Resource[IO, Connector[IO]] =
  for
    otel   <- Resource
                .eval(IO.delay(GlobalOpenTelemetry.get))
                .evalMap(OtelJava.forAsync[IO])
    tracer <- Resource.eval(Otel4sTelemetry.tracerProvider(otel.tracerProvider).tracer(serviceName).get)
    meter  <- Resource.eval(Otel4sTelemetry.meterProvider(otel.meterProvider).meter(serviceName).get)
    datasource = MySQLDataSource
                   .build[IO]("localhost", 3306, "user")
                   .setPassword("password")
                   .setDatabase("mydb")
                   .setSSL(SSL.Trusted)
                   .setTracer(tracer)
                   .setMeter(meter)
  yield Connector.fromDataSource(datasource)

val program = resource.use { connector =>
  sql"SELECT * FROM users".query[String].to[List].readOnly(connector)
}
```

トレースのみ、またはメトリクスのみを使用することも可能です：

```scala
// トレースのみ
val tracingOnly = MySQLDataSource.build[IO]("localhost", 3306, "user")
  .setTracer(tracer)

// メトリクスのみ
val metricsOnly = MySQLDataSource.build[IO]("localhost", 3306, "user")
  .setMeter(meter)
```

## コネクションプールとメトリクス

コネクションプーリング使用時は、トレーサー・メーターを設定した`MySQLDataSource`を`PooledDataSource`に渡すことで、プールのメトリクス（接続待機時間・使用時間・タイムアウト件数など）が自動収集されます。

```scala
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }

val datasource = MySQLDataSource
  .build[IO]("127.0.0.1", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")
  .setSSL(SSL.Trusted)
  .setTracer(tracer)
  .setMeter(meter)

val poolConfig = ConnectionPoolConfig(minConnections = 2, maxConnections = 10)

val pool: Resource[IO, PooledDataSource[IO]] =
  PooledDataSource.fromDataSource[IO](poolConfig, datasource)
```

収集されるプールメトリクスの一覧については [テレメトリリファレンス - コネクションプールメトリクス](../reference/Telemetry.md) を参照してください。

## TelemetryConfig によるカスタマイズ

`TelemetryConfig`を使用して、テレメトリの動作を制御できます。

```scala
import ldbc.telemetry.TelemetryConfig

// デフォルト設定（仕様準拠・すべて有効）
val config = TelemetryConfig.default

// スパン名を固定名にする（クエリテキストを解析しない）
val fixedSpanName = TelemetryConfig.withoutQueryTextExtraction

// 個別にカスタマイズ
val custom = TelemetryConfig.default
  .withoutQueryTextExtraction    // "Execute Statement" のような固定スパン名を使用
  .withoutSanitization           // クエリテキストのサニタイズを無効化（注意: センシティブなデータが露出する可能性）
  .withoutInClauseCollapsing     // IN (?, ?, ?) を IN (?) に折りたたまない
```

`TelemetryConfig`はデータソースに設定します：

```scala
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")
  .setTracer(tracer)
  .setMeter(meter)
  .setTelemetryConfig(TelemetryConfig.withoutQueryTextExtraction)
```

## 実践的な例: Jaeger + Prometheus + Grafana 環境

Docker Composeを使ってJaeger・Prometheus・Grafanaを含む観測環境を構築し、ldbcアプリケーションからトレースとメトリクスを送信する完全な例です。

### プロジェクト構成

```
otel-example/
├── src/
│   └── main/
│       └── scala/
│           └── Main.scala
├── database/
│   └── init.sql
├── dependencies/
│   ├── jaeger/
│   │   └── jaeger-ui.json
│   ├── prometheus/
│   │   └── prometheus.yml
│   └── grafana/
│       └── datasources.yaml
└── docker-compose.yaml
```

### Main.scala

```scala
import cats.effect.*
import io.opentelemetry.api.GlobalOpenTelemetry
import org.typelevel.otel4s.oteljava.OtelJava
import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.*
import ldbc.otel4s.Otel4sTelemetry

object Main extends IOApp.Simple:

  private val serviceName = "ldbc-otel-example"

  private def setupObservability: Resource[IO, Connector[IO]] =
    for
      otel   <- Resource
                  .eval(IO.delay(GlobalOpenTelemetry.get))
                  .evalMap(OtelJava.forAsync[IO])
      tracer <- Resource.eval(Otel4sTelemetry.tracerProvider(otel.tracerProvider).tracer(serviceName).get)
      meter  <- Resource.eval(Otel4sTelemetry.meterProvider(otel.meterProvider).meter(serviceName).get)
      datasource = MySQLDataSource
                     .build[IO]("127.0.0.1", 13306, "ldbc")
                     .setPassword("password")
                     .setDatabase("world")
                     .setSSL(SSL.Trusted)
                     .setTracer(tracer)
                     .setMeter(meter)
    yield Connector.fromDataSource(datasource)

  override def run: IO[Unit] =
    setupObservability.use { connector =>
      sql"SELECT name FROM city".query[String].to[List].readOnly(connector).flatMap { cities =>
        IO.println(cities)
      }
    }
```

### docker-compose.yaml

```yaml
services:
  database:
    image: mysql:@MYSQL_VERSION@
    container_name: ldbc-otel-example
    ports:
      - 13306:3306
    networks:
      - static-network
    volumes:
      - ./database:/docker-entrypoint-initdb.d
    environment:
      MYSQL_USER: 'ldbc'
      MYSQL_PASSWORD: 'password'
      MYSQL_ROOT_PASSWORD: 'root'
      TZ: 'Asia/Tokyo'
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost" ]
      timeout: 20s
      retries: 10

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - "./dependencies/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml"
    ports:
      - "9090:9090"
    networks:
      - static-network

  jaeger:
    image: jaegertracing/all-in-one:latest
    volumes:
      - "./dependencies/jaeger/jaeger-ui.json:/etc/jaeger/jaeger-ui.json"
    command: --query.ui-config /etc/jaeger/jaeger-ui.json
    environment:
      - METRICS_STORAGE_TYPE=prometheus
      - PROMETHEUS_SERVER_URL=http://prometheus:9090
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "16685:16685" # GRPC
      - "16686:16686" # UI
      - "4317:4317"   # OTLP gRPC receiver
      - "4318:4318"   # OTLP HTTP receiver
    networks:
      - static-network

  grafana:
    image: grafana/grafana-oss
    restart: unless-stopped
    volumes:
      - "./dependencies/grafana/datasources.yaml:/etc/grafana/provisioning/datasources/datasources.yaml"
    ports:
      - "3000:3000"
    networks:
      - static-network
    depends_on:
      jaeger:
        condition: service_started

networks:
  static-network:
    name: static-network
```

### 実行方法

1. Docker Composeで観測環境を起動します：

```bash
cd otel-example
docker-compose up -d
```

2. アプリケーションを以下のJavaオプションで実行します：

```bash
sbt -Dotel.java.global-autoconfigure.enabled=true \
    -Dotel.service.name=ldbc-otel-example \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=otlp \
    -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
    run
```

SBTの `build.sbt` に設定する場合：

```scala
javaOptions ++= Seq(
  "-Dotel.java.global-autoconfigure.enabled=true",
  "-Dotel.service.name=ldbc-otel-example",
  "-Dotel.traces.exporter=otlp",
  "-Dotel.metrics.exporter=otlp",
  "-Dotel.exporter.otlp.endpoint=http://localhost:4317"
)
```

3. アプリケーション実行後、JaegerのUI（[http://localhost:16686](http://localhost:16686)）でトレースを確認できます：

![Jaeger UI](../../../img/jaeger_ui.png)

GrafanaのUI（[http://localhost:3000](http://localhost:3000)）でもトレースとメトリクスを確認できます：

![Grafana UI](../../../img/grafana_ui.png)

## まとめ

ldbcとOpenTelemetryを組み合わせることで、データベース操作の詳細な可視化と監視が可能になります。

- **トレース**: `setTracer` でクエリの実行時間・エラー情報・スパン属性を記録
- **メトリクス**: `setMeter` でクエリ実行時間・返却行数・プール状態を記録
- **TelemetryConfig**: クエリサニタイズやスパン名生成の挙動をカスタマイズ

スパン属性やメトリクスの詳細仕様については [テレメトリリファレンス](../reference/Telemetry.md) を参照してください。
