{%
laika.title = OpenTelemetry
laika.metadata.language = ja
%}

# OpenTelemetryによる可観測性

このページでは、ldbcとOpenTelemetryを連携させて、データベースクエリのトレーシング、メトリクス収集、ロギングなどの可観測性を実現する方法について説明します。

## OpenTelemetryとは？

[OpenTelemetry](https://opentelemetry.io/)は、アプリケーションの可観測性を実現するためのオープンソースのフレームワークです。分散トレーシング、メトリクス収集、ロギングなどの機能を提供し、アプリケーションのパフォーマンスや動作を監視・分析するための統合的なアプローチを可能にします。

ldbcは、OpenTelemetryと統合することで、以下のような恩恵を得ることができます：

- データベースクエリの実行時間や処理の追跡
- クエリのパフォーマンス測定
- エラーの検出と診断
- システム全体の可視化とモニタリング

## 必要な依存関係

ldbcでOpenTelemetryを使用するには、以下の依存関係をプロジェクトに追加する必要があります：

```scala
libraryDependencies ++= Seq(
  // otel4sライブラリ（OpenTelemetryのScala向けラッパー）
  "org.typelevel" %% "otel4s-oteljava" % "0.11.2",
  
  // OpenTelemetryエクスポーター（データ送信用）
  "io.opentelemetry" % "opentelemetry-exporter-otlp" % "1.48.0" % Runtime,
  
  // 自動設定機能（環境変数やシステムプロパティによる構成）
  "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.48.0" % Runtime,
)
```

## ldbcでのOpenTelemetry設定

ldbcは、ConnectionProviderに`setTracer`メソッドを提供しており、ここにOpenTelemetryのTracerを設定することができます。

基本的な設定手順は次のとおりです：

1. OpenTelemetryを初期化する
2. TracerProviderを取得する
3. ConnectionProviderに設定する

以下に、基本的な設定例を示します：

```scala
import cats.effect.*
import io.opentelemetry.api.GlobalOpenTelemetry
import org.typelevel.otel4s.oteljava.OtelJava
import ldbc.connector.*
import ldbc.dsl.*

// サービス名（トレースの識別子）
val serviceName = "my-ldbc-app"

// リソースの設定
val resource: Resource[IO, Connection[IO]] =
  for
    // GlobalOpenTelemetryからotel4s用のインスタンスを作成
    otel <- Resource
      .eval(IO.delay(GlobalOpenTelemetry.get))
      .evalMap(OtelJava.forAsync[IO])
    
    // サービス名を指定してTracerProviderを取得
    tracer <- Resource.eval(otel.tracerProvider.get(serviceName))
    
    // データベース接続を設定し、TracerProviderを設定
    connection <- ConnectionProvider
      .default[IO]("localhost", 3306, "user", "password", "database")
      .setSSL(SSL.Trusted)
      .setTracer(tracer)  // OpenTelemetryのTracerを設定
      .createConnection()
  yield connection

// リソースを使用してクエリを実行
val program = resource.use { conn =>
  sql"SELECT * FROM users".query[String].to[List].readOnly(conn)
}
```

## 環境変数による設定

OpenTelemetryはシステムプロパティや環境変数による設定が可能です。アプリケーション起動時に以下のシステムプロパティを設定することで、OpenTelemetryの動作をカスタマイズできます：

```shell
java -Dotel.java.global-autoconfigure.enabled=true \
     -Dotel.service.name=ldbc-app \
     -Dotel.traces.exporter=otlp \
     -Dotel.metrics.exporter=none \
     -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
     -jar your-application.jar
```

SBTでの実行時には、以下のように設定できます：

```scala
javaOptions ++= Seq(
  "-Dotel.java.global-autoconfigure.enabled=true",
  "-Dotel.service.name=ldbc-app",
  "-Dotel.traces.exporter=otlp",
  "-Dotel.metrics.exporter=none"
)
```

## 実践的な例: OpenTelemetryを使ったldbcアプリケーション

ここでは、Docker Composeを使って、JaegerとPrometheusを含む観測環境を構築し、ldbcアプリケーションからトレースデータを送信する完全な例を示します。

### プロジェクト構成

以下のようなプロジェクト構成を想定します：

```
otel-example/
├── src/
│   └── main/
│       └── scala/
│           └── Main.scala
├── database/
│   └── xxx.sql
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

アプリケーションのメインコードは以下のとおりです：

```scala
import cats.effect.*
import io.opentelemetry.api.GlobalOpenTelemetry
import org.typelevel.otel4s.oteljava.OtelJava
import ldbc.connector.*
import ldbc.dsl.*

object Main extends IOApp.Simple:

  private val serviceName = "ldbc-otel-example"

  private def resource: Resource[IO, Connection[IO]] =
    for
      otel <- Resource
        .eval(IO.delay(GlobalOpenTelemetry.get))
        .evalMap(OtelJava.forAsync[IO])
      tracer <- Resource.eval(otel.tracerProvider.get(serviceName))
      connection <- ConnectionProvider
        .default[IO]("127.0.0.1", 13307, "ldbc", "password", "world")
        .setSSL(SSL.Trusted)
        .setTracer(tracer)
        .createConnection()
    yield connection

  override def run: IO[Unit] =
    resource.use { conn =>
      sql"SELECT name FROM city".query[String].to[List].readOnly(conn).flatMap { cities =>
        IO.println(cities)
      }
    }
```

### docker-compose.yaml

観測環境のセットアップには、以下のDocker Compose設定を使用します：

```yaml
services:
  database:
    image: mysql:8.0.41
    container_name: ldbc-otel-example
    ports:
      - 13307:3306
    networks:
      - static-network
    volumes:
      - ./database:/docker-entrypoint-initdb.d
      - ./database/my.cnf:/etc/database/conf.d/my.cn
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
      - "4317:4317" # OTLP gRPC receiver
      - "4318:4318" # OTLP http receiver
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

1. まず、Docker Composeで観測環境を起動します：

```bash
cd otel-example
docker-compose up -d
```

2. 次に、アプリケーションを以下のJavaオプションで実行します：

```bash
sbt -Dotel.java.global-autoconfigure.enabled=true \
    -Dotel.service.name=ldbc-otel-example \
    -Dotel.metrics.exporter=none \
    run
```

3. アプリケーション実行後、JaegerのUI（[http://localhost:16686](http://localhost:16686)）にアクセスすると、トレースが表示されます：

![Jaeger UI](../../../img/jaeger_ui.png)

GrafanaのUI（[http://localhost:3000](http://localhost:3000)）にアクセスしてトレースを確認することもできます。

![Grafana UI](../../../img/grafana_ui.png)

## トレースの詳細

ldbcのOpenTelemetry統合によって、以下のような情報がトレースに記録されます：

- **クエリ実行時間**: データベースクエリの所要時間
- **SQL文**: 実行されたSQLクエリの内容
- **クエリパラメータ**: クエリに渡されたパラメータ値（安全に記録）
- **接続情報**: サーバーやデータベース名などの接続情報
- **エラー情報**: エラーが発生した場合の詳細情報

## カスタムスパンの追加

より詳細な追跡を行いたい場合は、カスタムスパンを追加できます。以下は、クエリ実行をカスタムスパンで囲む例です：

```scala
resource.use { conn =>
  tracer.span("custom-database-operation").use { span =>
    // スパンに属性を追加
    span.setAttribute("db.operation", "select-cities")
    
    // クエリ実行
    sql"SELECT name FROM city".query[String].to[List].readOnly(conn).flatMap { cities =>
      // 結果データに関する属性を追加
      span.setAttribute("result.count", cities.size)
      IO.println(cities)
    }
  }
}
```

## トレースのカスタマイズオプション

ldbcのOpenTelemetry統合では、以下のような追加のカスタマイズが可能です：

- **サンプリングレートの設定**: 全てのクエリではなく一部のみをトレースする
- **属性のフィルタリング**: センシティブな情報をトレースから除外する
- **カスタムトレースエクスポーター**: Jaeger以外のトレースシステムへの送信

これらの設定は、OpenTelemetryの設定ファイルやシステムプロパティを通じて行うことができます。

## まとめ

ldbcとOpenTelemetryを組み合わせることで、データベース操作の詳細な可視化と監視が可能になります。本ガイドで紹介した方法を使用することで、アプリケーションのパフォーマンス問題の特定、ボトルネックの発見、およびトラブルシューティングが容易になります。

実際の運用環境では、セキュリティ設定や環境に応じた調整が必要になる場合があります。OpenTelemetryの公式ドキュメントも参照しながら、最適な設定を見つけることをお勧めします。
