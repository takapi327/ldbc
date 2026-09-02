{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.8.xから0.9.xへの移行)

0.9.x は ldbc をエフェクト非依存（tagless-final）へと再構築したメジャーな変更です。0.8.x までのドライバ（`ldbc-connector`）は Cats Effect 3（`IO`）を前提に実装されていましたが、0.9.x では ldbc 独自のエフェクト型クラス階層（`Async ⊂ Temporal ⊂ Concurrent`）を新設し、その上にドライバ・ネットワーク層・コネクションプールを構築しました。これにより **Cats Effect（`IO`）/ ZIO（`Task`）/ `scala.concurrent.Future`** をそれぞれネイティブに動かせるようになります。あわせて、外部のエフェクトライブラリに依存しない軽量なエフェクト型 `Fx` も 0.9.x で新設し（`ldbc-fx`）、`Future` バックエンドおよび単体利用に用いています。

> **重要**: 既存の `ldbc-connector`（Cats Effect ベースの MySQL コネクター）は **0.9.x でもそのまま利用できます**。0.9.x は互換を壊してエフェクト非依存版へ強制移行するものではなく、`ldbc-connector` の隣に **エフェクト非依存の新ドライバ `ldbc-mysql`** と、各エフェクト向けのブリッジ（`ldbc-cats-effect` / `ldbc-zio` / `ldbc-future`）を追加するものです。急いで移行する必要はありません。
>
> ただし、**`ldbc-connector` は将来のバージョンで廃止される予定です**。エフェクト非依存の新ドライバ `ldbc-mysql`（およびエフェクトに応じたブリッジ）が後継となるため、新規プロジェクトでは `ldbc-mysql` の利用を推奨します。既存プロジェクトも、準備が整い次第 `ldbc-mysql` への移行を検討してください。

## パッケージ

**既存パッケージ（0.8.x から継続）**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-testkit`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit_3)                   |
| `ldbc-testkit-munit`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit-munit_3)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.9.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

**0.9.x で新規追加されたパッケージ**

| Module / Platform            | JVM | Scala Native | Scala.js | 概要                                                                                     |
|------------------------------|:---:|:------------:|:--------:|------------------------------------------------------------------------------------------|
| `ldbc-effect`                |  ✅  |      ✅       |    ✅     | エフェクト型クラス階層（`Async` / `Temporal` / `Concurrent`）と並行プリミティブ（Ref / Deferred / Semaphore / Resource） |
| `ldbc-fx`                    |  ✅  |      ✅       |    ✅     | 0.9.x で新設した軽量エフェクト型 `Fx`。`Future` バックエンドおよび単体利用向けの `Concurrent[Fx]` インスタンス |
| `ldbc-net`                   |  ✅  |      ✅       |    ✅     | エフェクト非依存の非ブロッキング transport（IoEngine + `Socket[F]`）                                    |
| `ldbc-pool`                  |  ✅  |      ✅       |    ✅     | エフェクト非依存のコネクションプール（`F: Concurrent`）                                                 |
| `ldbc-mysql`                 |  ✅  |      ✅       |    ✅     | **エフェクト非依存の MySQL ドライバ**（Cats Effect 非依存。`ldbc-connector` の後継）                    |
| `ldbc-telemetry`             |  ✅  |      ✅       |    ✅     | DB 非依存の OpenTelemetry トレーシング / メトリクス SPI（Tracer / Span / Meter）                        |
| `ldbc-otel4s`                |  ✅  |      ✅       |    ✅     | `ldbc-telemetry` SPI の otel4s バックエンド実装（Cats Effect 向け）                                    |
| `ldbc-zio-telemetry`         |  ✅  |      ❌       |    ❌     | `ldbc-telemetry` SPI の zio-telemetry バックエンド実装（JVM のみ）                                     |
| `ldbc-cats-effect`           |  ✅  |      ✅       |    ✅     | Cats Effect（`IO`）ブリッジ。fs2 ストリーミングと `Connector[IO]`                                      |
| `ldbc-future`                |  ✅  |      ✅       |    ✅     | `scala.concurrent.Future` ブリッジ（内部で `Fx` をバックエンドに使用）                                  |
| `ldbc-zio`                   |  ✅  |      ✅       |    ✅     | ZIO（`Task`）ブリッジ。ZStream ストリーミングと `Connector[Task]`                                      |

## 🎯 主要な変更点

### 1. エフェクト非依存（tagless-final）化

0.9.x の中心となる変更です。0.8.x までのドライバ（`ldbc-connector`）は Cats Effect 3（`IO`）を前提に実装されていました。0.9.x では、ldbc 独自のエフェクト型クラス階層を新設し、その上にドライバ・ネットワーク層・プールを構築しています。

```
Async ⊂ Temporal ⊂ Concurrent   (ldbc-effect)
```

- **`Concurrent` を持つエフェクトはネイティブに動く**: `IO` / `Task` / `Fx` それぞれに `Concurrent[F]` インスタンスを提供し、エフェクト間の変換・ブリッジ層を挟まずに実行します。
- **`Future` は `Concurrent` を満たせない**ため、内部で `Fx` をバックエンドに使い、結果を 1 回だけ `Future` に変換します（`ldbc-future`）。
- Cats Effect 非依存を実現しています。ドライバ本体（`ldbc-mysql` / `ldbc-net` / `ldbc-pool`）は `ldbc.effect` の型クラスのみを参照し、cats-effect には依存しません。

ユーザーから見た主な違いは「**どのエフェクトを使うかで、依存するモジュールと `Connector` の入手経路が変わる**」ことです。

### 2. 新しいコネクター構成（per-effect ブリッジ）

エフェクト非依存ドライバ `ldbc-mysql` は、単体では特定のエフェクトに縛られません。実際にクエリを実行する `Connector[F]` は、使用するエフェクトに対応するブリッジモジュールから取得します。

| エフェクト                     | 依存モジュール                          | `Connector` の入手元                 |
|-------------------------------|-----------------------------------------|--------------------------------------|
| Cats Effect（`IO`）           | `ldbc-mysql` + `ldbc-cats-effect`       | `ldbc.catseffect.Connector`          |
| ZIO（`Task`）                 | `ldbc-mysql` + `ldbc-zio`               | `ldbc.zio.Connector`                 |
| `scala.concurrent.Future`     | `ldbc-mysql` + `ldbc-future`            | `ldbc.future.Connector`              |

いずれのブリッジも、`ldbc-connector` と同じ `fromConnection` / `fromDataSource` を提供します（`fromConfig` は各 `MySQLDataSource` 側に用意されています）。戻り値はいずれも共通の基底型 `ldbc.Connector[F]` です。

**Cats Effect（`IO`）の例:**

```scala
import cats.effect.IO

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.catseffect.Connector

val datasource = MySQLDataSource
  .build[IO]("127.0.0.1", 3306, "user")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted)

val connector = Connector.fromDataSource(datasource)

sql"SELECT name FROM city LIMIT 1".query[String].to[Option].readOnly(connector)
```

**ZIO（`Task`）の例:**

```scala
import zio.Task

import ldbc.dsl.*
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL
import ldbc.zio.Connector
import ldbc.zio.given

val datasource = MySQLDataSource
  .build[Task]("127.0.0.1", 3306, "user")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted)

val connector = Connector.fromDataSource(datasource)
```

> **`ldbc-connector` から移行しない場合**: これまで通り `import ldbc.connector.*` の `MySQLDataSource` / `Connector.fromDataSource` を使い続けられます。DSL（`ldbc-dsl`）やスキーマ（`ldbc-schema`）などクエリ組み立て側の API は共通で、`Connector[F]` を差し替えるだけで両者を切り替えられます。

### 3. ストリーミングはエフェクトごとにネイティブ

クエリ結果のストリーミングを、各エフェクトのネイティブなストリーム型で提供します。

- Cats Effect（`IO`）: `fs2.Stream`（`ldbc-cats-effect`）
- ZIO（`Task`）: `zio.stream.ZStream`（`ldbc-zio`）

`ldbc-connector` を使う場合の fs2 ストリーミングは従来通りです。

### 4. テレメトリのバックエンドを分離

トレーシング / メトリクスの SPI を DB 非依存の `ldbc-telemetry` に切り出し、実バックエンドを別モジュールにしました。

- `ldbc-otel4s`: otel4s バックエンド（Cats Effect 向け。span がエフェクト `F` 上でネイティブに動くため往復オーバーヘッドがない）
- `ldbc-zio-telemetry`: zio-telemetry バックエンド（JVM のみ）

既定では no-op（何も出力しない）で、テレメトリを使う場合にのみ対応するバックエンドモジュールを追加します。`ldbc-connector` のテレメトリ（otel4s ベース）は従来通りです。

## 破壊的変更

### 基本的にソース互換

0.9.x は**新しいモジュールを追加する**リリースであり、既存の `ldbc-connector` を使うコードには原則として破壊的変更はありません。DSL・クエリビルダー・スキーマ・コード生成などの API は 0.8.x と同じです。

### `ldbc-mysql` の `Parameter` はエフェクト非依存ドライバ向けの新実装

`ldbc.mysql.data.Parameter` は、`ldbc-connector` の `Parameter`（0.8.0 で `sealed trait` 化・`sql` 削除済み）と同じ設計です。文字列の SQL リテラル化は `ldbc.mysql.data.QueryRenderer` を通してのみ行われ、`Parameter` 自身は sql_mode 非依存の `toString`（診断用）しか公開しません。これは新モジュール `ldbc-mysql` の内部 API であり、通常のユーザーコードには影響しません。

### 依存ライブラリ

`ldbc-mysql` 系のエフェクトブリッジを使う場合、追加で以下が必要になります。

| エフェクト | 追加で必要な主な依存 |
|-----------|--------------------|
| Cats Effect | `cats-effect` / `fs2`（`ldbc-cats-effect` が推移的に導入） |
| ZIO | `zio` / `zio-streams`（`ldbc-zio` が推移的に導入） |

### 変更がないもの

| | 0.8.x | 0.9.0 |
|---|---|---|
| Java バージョン | 17、21、25 | 変更なし |
| Scala バージョン | 3.3.x / 3.8.x | 変更なし |

## 移行ガイド

### そのまま `ldbc-connector` を使い続ける

最も簡単な移行は、バージョンを上げるだけです。`ldbc-connector` の API は 0.8.x と互換です。

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"       % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-connector" % "0.9.0"
```

### エフェクト非依存ドライバ（`ldbc-mysql`）へ移行する

使用するエフェクトに応じて、`ldbc-mysql` とブリッジモジュールを追加します。

**Cats Effect（`IO`）:**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"         % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-mysql"       % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-cats-effect" % "0.9.0"
```

**ZIO（`Task`）:**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"   % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-mysql" % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-zio"   % "0.9.0"
```

**`scala.concurrent.Future`:**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"    % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-mysql"  % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-future" % "0.9.0"
```

コード側は、`import ldbc.connector.*` を対応するブリッジ（`ldbc.catseffect.Connector` / `ldbc.zio.Connector` / `ldbc.future.Connector`）と `ldbc.mysql.MySQLDataSource` に置き換えます。クエリの組み立て（`sql"..."` / `query` / `readOnly` / `commit` など）は共通のため変更不要です。

### テレメトリを有効にする

**Cats Effect（otel4s）:**

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-otel4s" % "0.9.0"
```

**ZIO（zio-telemetry、JVM のみ）:**

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-zio-telemetry" % "0.9.0"
```

## まとめ

0.9.x では以下が実現されます。

1. **エフェクト非依存化**: `Async` / `Temporal` / `Concurrent` の型クラス階層により、`IO` / `Task` / `Future` / `Fx` をネイティブに扱える
2. **マルチエフェクト対応**: Cats Effect / ZIO / Future 向けのブリッジを追加。ストリーミングも fs2 / ZStream でネイティブに提供
3. **テレメトリの分離**: DB 非依存の SPI（`ldbc-telemetry`）と、otel4s / zio-telemetry バックエンドの分離
4. **後方互換**: 既存の `ldbc-connector` はそのまま利用可能。移行は任意

`ldbc-connector` を使い続けるプロジェクトは、依存バージョンの更新のみで移行できます。エフェクト非依存ドライバへ移すかどうかは、利用するエフェクトや将来的な方針に応じて選択できます。
