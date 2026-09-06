{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.8.xから0.9.xへの移行)

0.9.x は ldbc をエフェクト非依存（tagless-final）へと再構築したメジャーな変更です。0.8.x までのドライバ（`ldbc-connector`）は Cats Effect 3（`IO`）を前提に実装されていましたが、0.9.x では ldbc 独自のエフェクト型クラス階層（`Async ⊂ Temporal ⊂ Concurrent`）を新設し、その上にドライバ・ネットワーク層・コネクションプールを構築しました。これにより **Cats Effect（`IO`）/ ZIO（`Task`）/ `scala.concurrent.Future`** をそれぞれネイティブに動かせるようになります。あわせて、外部のエフェクトライブラリに依存しない軽量なエフェクト型 `Fx` も 0.9.x で新設し（`ldbc-fx`）、`Future` バックエンドおよび単体利用に用いています。

> **重要**: 既存の `ldbc-connector`（Cats Effect ベースの MySQL コネクター）は **0.9.x でも引き続き利用できます**。0.9.x はエフェクト非依存版への強制移行を求めるものではなく、`ldbc-connector` の隣に **エフェクト非依存の新ドライバ `ldbc-mysql`** と、各エフェクト向けのブリッジ（`ldbc-cats-effect` / `ldbc-zio` / `ldbc-future`）を追加するものです。急いで `ldbc-mysql` へ移る必要はありません。
>
> ただし、エフェクト非依存化にともない**共有モジュール（`ldbc-sql` / `ldbc-core` / `ldbc-dsl`）には破壊的変更があります**。`ldbc-connector` を使い続ける場合でも、接続の取得（`getConnection`）・ストリーミング（`Query#stream`）・`Sync[DBIO]` に該当する箇所は書き換えが必要です。詳細は下記の「破壊的変更」を参照してください。
>
> また、**`ldbc-connector` は将来のバージョンで廃止される予定です**。エフェクト非依存の新ドライバ `ldbc-mysql`（およびエフェクトに応じたブリッジ）が後継となるため、新規プロジェクトでは `ldbc-mysql` の利用を推奨します。既存プロジェクトも、準備が整い次第 `ldbc-mysql` への移行を検討してください。

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

これにともない、`ldbc-dsl` は fs2 への依存をやめ、`Query#stream` を削除しました。**`ldbc-connector` を使い続ける場合でも、ストリーミングを使うなら `ldbc-cats-effect` の追加が必要です**（「破壊的変更」の 2 を参照）。

### 4. テレメトリのバックエンドを分離

トレーシング / メトリクスの SPI を DB 非依存の `ldbc-telemetry` に切り出し、実バックエンドを別モジュールにしました。

- `ldbc-otel4s`: otel4s バックエンド（Cats Effect 向け。span がエフェクト `F` 上でネイティブに動くため往復オーバーヘッドがない）
- `ldbc-zio-telemetry`: zio-telemetry バックエンド（JVM のみ）

既定では no-op（何も出力しない）で、テレメトリを使う場合にのみ対応するバックエンドモジュールを追加します。`ldbc-connector` のテレメトリ（otel4s ベース）は従来通りです。

## 破壊的変更

0.9.x は新しいモジュールを追加するリリースですが、**エフェクト非依存化にともなって共有モジュール（`ldbc-sql` / `ldbc-core` / `ldbc-dsl`）に破壊的変更があります**。`ldbc-connector` / `jdbc-connector` をそのまま使い続ける場合でも、以下に該当するコードは書き換えが必要です。

一方、**クエリの組み立て側の API は 0.8.x と同じ**です。`sql"..."` 補間子、`query` / `to[List]` / `unsafe` / `option` / `nel`、`readOnly` / `commit` / `transaction` / `rollback`、クエリビルダー（`ldbc-query-builder`）、スキーマ定義（`ldbc-schema`）、コード生成（`ldbc-codegen`）に変更はありません。

| # | 変更 | 影響を受けるのは |
|---|------|-----------------|
| 1 | `DataSource` の移動と `getConnection` の戻り値変更 | `getConnection` を直接呼んでいるコード / `DataSource` を自作しているコード |
| 2 | `Query#stream` が `ldbc-cats-effect` へ移動 | fs2 ストリーミングを使っているコード |
| 3 | `Sync[DBIO]` が `MonadError[DBIO, Throwable]` に | `DBIO` に `Sync` を要求しているコード |
| 4 | Free 代数から cats-effect 由来の操作を削除 | 独自の解釈器（`Visitor`）を実装しているコード |
| 5 | 推移的依存の変更 | `ldbc-dsl` / `ldbc-core` 経由で fs2 / cats-effect を得ていたコード |

### 1. `DataSource` が `ldbc.sql` へ移動し、`getConnection` の戻り値が変わりました

| | 0.8.x | 0.9.0 |
|---|---|---|
| 型 | `ldbc.DataSource` | `ldbc.sql.DataSource` |
| `getConnection` | `Resource[F, Connection[F]]` | `F[(Connection[F], F[Unit])]`（allocated 形） |

`ldbc.DataSource` は削除され、`ldbc.sql.DataSource` になりました。`import ldbc.connector.*` / `import jdbc.connector.*` を使っている場合は、それぞれのパッケージオブジェクトが `ldbc.sql.DataSource` を re-export するため型名の解決は変わりません。`import ldbc.DataSource` と直接書いていた場合は import の変更が必要です。

`getConnection` は `Resource` ではなく、**コネクションと解放処理の組**を返すようになりました。`Resource` はエフェクトごとに別の型（`cats.effect.Resource` / ZIO の `Scope` / `ldbc.effect.Resource`）であり、シグネチャに出すと `DataSource` が特定のエフェクトに固定されてしまうためです。この形にしたことで、Cats Effect / ZIO / Fx / Future のすべてが同じ `DataSource[F]` を共有できます。

**移行前:**

```scala
datasource.getConnection.use { conn =>
  conn.setCatalog("world") *> conn.getCatalog()
}
```

**移行後（`ldbc-connector`）:** `use` 拡張メソッドを import します。bracket で実装されているため、成功・失敗・キャンセルのいずれでも解放が走ります。

```scala
import ldbc.connector.syntax.*

datasource.use { conn =>
  conn.setCatalog("world") *> conn.getCatalog()
}
```

エフェクト非依存ドライバ（`ldbc-mysql`）を使う場合は `import ldbc.mysql.syntax.*`（または `import ldbc.pool.*`）で同等の `use` が入ります。

**`Resource` として扱いたい場合**、あるいは `jdbc-connector` 単体を使っていて `ldbc.connector.syntax` が使えない場合は、次のように組み立てます。

```scala
val connection: Resource[F, Connection[F]] =
  Resource.make(datasource.getConnection)(_._2).map(_._1)
```

**`DataSource` を自作している場合**は `getConnection` の実装を修正してください。既存の `Resource` ベースの実装は `.allocated` を付けるだけで移行できます。

```scala
// 移行前
override def getConnection: Resource[F, Connection[F]] =
  Resource.fromAutoCloseable(...).map(ConnectionImpl(_))

// 移行後
override def getConnection: F[(Connection[F], F[Unit])] =
  Resource.fromAutoCloseable(...).map(ConnectionImpl(_)).allocated
```

### 2. `Query#stream` が `ldbc-dsl` から `ldbc-cats-effect` へ移りました

`ldbc-dsl` は fs2 への依存をやめ、`Query` から `stream` / `stream(fetchSize)` を削除しました。fs2 ストリーミングは `ldbc-cats-effect` の拡張メソッドとして提供されます。**`ldbc-connector` を使い続ける場合も、ストリーミングを使うなら依存の追加が必要です**（`ldbc-connector` は `ldbc-cats-effect` に依存していません）。

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-cats-effect" % "0.9.0"
```

**移行前:**

```scala
import ldbc.dsl.*

sql"SELECT name FROM city".query[String].stream(100)
```

**移行後:**

```scala
import ldbc.dsl.*
import ldbc.catseffect.*

sql"SELECT name FROM city".query[String].stream(100)
```

> **注意**: `import ldbc.connector.*` と `import ldbc.catseffect.*` を同時に行うと、`Connector` という名前が両方から入ってくるため曖昧参照になります。コネクターの生成側を `ldbc.connector.Connector.fromDataSource(...)` のように修飾するか、必要なものだけを個別に import してください。

ZIO で `ZStream` を使う場合は `ldbc-zio` を追加し、`query.stream(connector)` を呼びます（`ZStream` は常に ZIO 上の型のため、`DBIO` の上には構築できず、コネクターを引数に取る形になっています）。

### 3. `Sync[DBIO]` が `MonadError[DBIO, Throwable]` になりました

`ldbc-dsl` は cats-effect への依存をやめ、`DBIO` に与えるインスタンスを変更しました。`import ldbc.dsl.*` で暗黙に入ってくるインスタンスも変わります。

| | 0.8.x | 0.9.0 |
|---|---|---|
| `import ldbc.dsl.*` が提供 | `implicit val syncDBIO: Sync[DBIO]` | `implicit val monadErrorDBIO: MonadError[DBIO, Throwable]` |
| `Sync[DBIO]` が必要な場合 | `ldbc.dsl.DBIO.syncDBIO` | `ldbc.catseffect.syncDBIO`（`ldbc-cats-effect`） |

`DBIO` に対して `raiseError` / `handleErrorWith` / `attempt` / `onError` といった cats のエラー処理コンビネータを使っているだけであれば、書き換えは不要です。`Sync[DBIO]` を明示的に要求している箇所（fs2 と組み合わせる場合など）では `import ldbc.catseffect.*` を追加してください。

なお `ldbc.catseffect.syncDBIO` の `rootCancelScope` は `Uncancelable` です。`DBIO` は Free プログラムであり本物のキャンセルを表現できないため、`MonadCancel` 系の操作はキャンセル不可の恒等として振る舞います。

### 4. `DBIO` / Free 代数から cats-effect 由来の操作を削除しました

`ConnectionOp` / `StatementOp` / `PreparedStatementOp` から次を削除しました。

- `Monotonic` / `Realtime`（および `ConnectionIO.monotonic` / `realtime` などのコンストラクタ）
- `ForceR` / `Uncancelable` / `Poll1` / `Canceled` / `OnCancel`（および `capturePoll`）
- `PreparedStatementIO` の `given Sync[PreparedStatementIO]`
- `Suspend(hint: Sync.Type, thunk)`。`ConnectionOp` にのみ `Sync.Type` を取らない `Suspend(thunk)` が残り、`StatementOp` / `PreparedStatementOp` からは削除されました

あわせて `ResultSetOp.Visitor` に `drainRows` が追加されました（メモリ上に確定済みの結果セットを 1 つのエフェクトの中で一括デコードするための操作です）。

`KleisliInterpreter` の型制約は `Sync[F]` から `MonadError[F, Throwable]` に緩和されています。`Sync[F]` は `MonadError[F, Throwable]` を満たすため、`KleisliInterpreter` を使う側の変更は不要です。

**影響**: `ConnectionIO.monotonic` などを直接呼んでいるコード、および `ConnectionOp.Visitor` / `StatementOp.Visitor` / `PreparedStatementOp.Visitor` / `ResultSetOp.Visitor` を自前で実装している（独自の解釈器を書いている）コードはコンパイルエラーになります。通常の DSL 利用では影響ありません。

### 5. 推移的依存の変更

| モジュール | 0.8.x | 0.9.0 |
|-----------|-------|-------|
| `ldbc-core` | `cats-free` + `cats-effect` | `cats-free` のみ |
| `ldbc-dsl` | `twiddles-core` + `fs2-core` | `twiddles-core` のみ |

`ldbc-dsl` / `ldbc-core` 経由で cats-effect や fs2 がクラスパスに入ることを当てにしていた場合は、明示的に依存を追加するか、`ldbc-cats-effect` を追加してください。`ldbc-connector` は従来どおり cats-effect / fs2 に依存しているため、`ldbc-connector` を使っている場合はクラスパス上の内容は変わりません（ただし `Query#stream` は 2 のとおり移動しています）。

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

`ldbc-connector` の `MySQLDataSource` / `Connector` / プーリングの API は 0.8.x と同じです。まずバージョンを上げます。

```scala
libraryDependencies += "io.github.takapi327" %%% "ldbc-dsl"       % "0.9.0"
libraryDependencies += "io.github.takapi327" %%% "ldbc-connector" % "0.9.0"
```

そのうえで、「破壊的変更」のうち次の 3 点に該当する箇所を書き換えます。多くのプロジェクトではこれで移行が完了します。

1. `datasource.getConnection.use { ... }` → `import ldbc.connector.syntax.*` を追加して `datasource.use { ... }`
2. `query.stream` を使っている → `ldbc-cats-effect` を依存に追加し、`import ldbc.catseffect.*`
3. `Sync[DBIO]` を要求している → 同じく `import ldbc.catseffect.*`

```scala
// 2 / 3 に該当する場合
libraryDependencies += "io.github.takapi327" %%% "ldbc-cats-effect" % "0.9.0"
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
4. **既存ドライバの継続利用**: 既存の `ldbc-connector` は 0.9.x でも利用可能。エフェクト非依存ドライバへ移すかどうかは任意

ただし、エフェクト非依存化にともなって共有モジュール（`ldbc-sql` / `ldbc-core` / `ldbc-dsl`）には破壊的変更があります。`ldbc-connector` を使い続けるプロジェクトでも、`getConnection` の呼び出し・`Query#stream`・`Sync[DBIO]` に該当する箇所は書き換えが必要です。詳しくは「破壊的変更」を参照してください。クエリの組み立て側の API は変わらないため、書き換えは接続の取得とストリーミングの境界に限られます。
