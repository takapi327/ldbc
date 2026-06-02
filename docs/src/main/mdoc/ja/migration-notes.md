{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.6.xから0.7.xへの移行)

## パッケージ

**全てのパッケージ**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-testkit`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit_3)                   |
| `ldbc-testkit-munit`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit-munit_3)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.7.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 主要な変更点

### 1. テスト用モジュールの追加

0.7.0 ではデータベーステストを簡単に書くための専用モジュールが2つ追加されました。

#### ldbc-testkit

フレームワークに依存しない `ldbc-testkit` モジュールが追加されました。テスト終了時に自動でロールバックする仕組みを提供します。

**`RollbackHandler`**: テスト終了後に全変更を自動ロールバックする `Connector` を `Resource` として提供します。

```scala
import ldbc.testkit.RollbackHandler

RollbackHandler.resource[F](dataSource).use { connector =>
  // このブロック内の全変更はテスト終了後にロールバックされる
  connector.use { conn =>
    conn.executeUpdate("INSERT INTO users VALUES (1, 'Alice')")
  }
}
```

**`TestConnection`**: `commit()` と `setAutoCommit(true)` をno-opにインターセプトし、テスト中の意図しないコミットを防止します。

#### ldbc-testkit-munit

MUnit との統合テストサポートを提供する `ldbc-testkit-munit` モジュールが追加されました。

**`LdbcSuite`**: `CatsEffectSuite` を継承した ldbc テスト用基底トレイトです。

```scala
import ldbc.testkit.munit.LdbcSuite

class MyDbTest extends LdbcSuite {

  // テスト終了後にロールバックされるテスト（DML操作向け）
  ephemeralTest("insert and query") { conn =>
    for
      _     <- conn.executeUpdate("INSERT INTO users VALUES (1, 'Alice')")
      count <- conn.executeQuery("SELECT COUNT(*) FROM users").map(_.head)
    yield assertCount(count, 1)
  }

  // 実際にコミットするテスト（DDL操作向け）
  persistentTest("create table") { conn =>
    conn.executeUpdate("CREATE TABLE IF NOT EXISTS test_table (id INT)")
  }
}
```

**アサーションヘルパー一覧:**

| メソッド | 説明 |
|---------|------|
| `assertCount(result, expected)` | 行数を検証する |
| `assertEmpty(result)` | 結果が空であることを検証する |
| `assertRowsUnordered(result, expected)` | 順序を問わず行を検証する |
| `assertRowsOrdered(result, expected)` | 順序通りに行を検証する |

### 2. DSL ヘルパー関数の追加

`ldbc.dsl.syntax.HelperFunctionsSyntax` に以下の関数が追加されました。

#### `ident()` — SQL識別子の安全なエスケープ

```scala
val tableName = "my_table"
sql"SELECT * FROM ${ident(tableName)}"
// → SELECT * FROM `my_table`
```

識別子をバッククォートで囲み、NUL文字を除去することで SQL インジェクションを防止します。

#### `when()` — 条件付きSQL断片

```scala
val limit = 10
sql"SELECT * FROM users" ++ when(limit > 0)(sql" LIMIT $limit")
```

#### `paginate()` — ページネーションヘルパー

```scala
// オフセットあり
sql"SELECT * FROM users " ++ paginate(limit = 20, offset = 40)
// → SELECT * FROM users LIMIT ? OFFSET ?

// オフセットなし
sql"SELECT * FROM users " ++ paginate(limit = 20)
// → SELECT * FROM users LIMIT ?
```

`limit` または `offset` が負の値の場合は `IllegalArgumentException` をスローします。

### 3. OpenTelemetry セマンティクス属性の型安全API移行

`TelemetryAttribute` の文字列定数が `otel4s-semconv` ライブラリの型安全な API に移行されました。これは内部実装の変更のため、ユーザーコードへの直接的な影響はありません。

**移行前 (0.6.x) — 内部実装:**
```scala
TelemetryAttribute.dbSystemName
TelemetryAttribute.serverAddress(host)
TelemetryAttribute.dbNamespace(db)
TelemetryAttribute.dbOperationName(op)
TelemetryAttribute.dbQueryText(sql)
```

**移行後 (0.7.x) — 内部実装:**
```scala
DbAttributes.DbSystemName(DbAttributes.DbSystemNameValue.Mysql.value)
ServerAttributes.ServerAddress(host)
DbAttributes.DbNamespace(db)
DbAttributes.DbOperationName(op)
DbAttributes.DbQueryText(sql)
```

`TelemetryAttribute` に引き続き残るのは MySQL 固有の属性（`DB_MYSQL_VERSION`、`DB_MYSQL_THREAD_ID`、`DB_MYSQL_AUTH_PLUGIN`）と `SqlOperation` オブジェクトのみです。

### 4. Scala Native 対応の強化

`sbt-scala-native` を `0.4.17` から `0.5.10` へアップグレードしました。Scala Native 0.5 系ではマルチスレッドが正式にサポートされたことにより、ldbc の Scala Native 対応が大きく前進しています。

#### Scala Native 0.5 でのマルチスレッドサポート

Scala Native 0.4 系ではシングルスレッド実行のみサポートされていましたが、0.5 系では **真のマルチスレッド実行** が可能になりました。

Cats Effect 3.7.0 はこの変更に対応し、JVM 向けの `WorkStealingThreadPool` と `IORuntime` を Scala Native 0.5 向けにポートしています。これにより、以下の機能が Scala Native でも利用可能になりました。

- **`WorkStealingThreadPool`**: JVM と同等のワークスチールスレッドプールが Native 上で動作
- **epoll / kqueue サポート**: Linux では `epoll`、macOS/BSD では `kqueue` を使ったノンブロッキング I/O ポーリング
- **`IORuntime` の完全対応**: Fiber スケジューリングが JVM とほぼ同等に動作

#### Fiber モデルと ThreadLocal の非互換性

Cats Effect の Fiber はスレッド間を自由に移動するため、Java の `ThreadLocal` をコネクションプールのキャッシュに使う HikariCP 流のアプローチは適用できません。ldbc のコネクションプール実装（`ConcurrentBag`）はこの制約を考慮し、`Ref[F, ...]` と `Queue[F, ...]` を使ったロックフリーな共有データ構造を採用しています。


### 5. コードジェネレーターの YAML パーサ移行

JS/Native 向けの YAML パーサを `circe-scala-yaml`（armanbilge）から `scala-yaml`（VirtusLab）に移行しました。コードジェネレーターを使用しているユーザーへの API 変更はありません。

## 破壊的変更

### Java 11 のサポート廃止

Java 11（corretto@11）のサポートが廃止されました。

**サポート対象の Java バージョン:** 17、21、25

### Scala バージョンの更新

クロスビルド対象が Scala 3.7.x から **Scala 3.8.x** に変更されました。

| | 変更前 (0.6.x) | 変更後 (0.7.x) |
|---|---|---|
| Scala バージョン | 3.7.4 | 3.8.3 |

## 非推奨API

以下の API が 0.7.0 で非推奨になりました。将来のバージョンで削除される予定です。

| API | 非推奨バージョン | 代替 |
|-----|:-----------:|------|
| `sc(identifier)` | 0.7.0 | `ident(identifier)` |
| `Connection.fromSocketGroup(...)` | 0.7.0 | `Connection.fromNetwork(...)` |
| `SSL.fromKeyStoreFile(java.nio.file.Path, ...)` | 0.7.0 | `SSL.fromKeyStoreFile(fs2.io.file.Path, ...)` |

## 移行ガイド

### `sc()` を `ident()` に置き換える

`sc()` 関数は非推奨になりました。`ident()` に移行してください。

**移行前 (0.6.x):**
```scala
sql"SELECT * FROM ${sc(tableName)}"
```

**移行後 (0.7.x):**
```scala
sql"SELECT * FROM ${ident(tableName)}"
// → SELECT * FROM `tableName`
```

`ident()` は識別子をバッククォートで囲むため、テーブル名やカラム名の SQL インジェクション対策として安全です。

### `Connection.fromSocketGroup` を `fromNetwork` に置き換える

**移行前 (0.6.x):**
```scala
Connection.fromSocketGroup(socketGroup, host, port, user, password, database)
```

**移行後 (0.7.x):**
```scala
Connection.fromNetwork(network, host, port, user, password, database)
```

### `SSL.fromKeyStoreFile` のパス型を変更する

**移行前 (0.6.x):**
```scala
import java.nio.file.Paths

SSL.fromKeyStoreFile(Paths.get("/path/to/keystore"), password, keyPassword)
```

**移行後 (0.7.x):**
```scala
import fs2.io.file.Path

SSL.fromKeyStoreFile(Path("/path/to/keystore"), password, keyPassword)
```

### テスト用モジュールを導入する

既存のロールバック処理を `ldbc-testkit` に移行できます。

**`build.sbt` に依存関係を追加:**
```scala
// フレームワーク非依存
libraryDependencies += "io.github.takapi327" %% "ldbc-testkit" % "0.7.0" % Test

// MUnit 統合
libraryDependencies += "io.github.takapi327" %% "ldbc-testkit-munit" % "0.7.0" % Test
```

**移行後 (0.7.x):**
```scala
import ldbc.testkit.munit.LdbcSuite

class UserRepositoryTest extends LdbcSuite {

  ephemeralTest("ユーザーを作成できる") { conn =>
    for
      _     <- conn.executeUpdate("INSERT INTO users (name) VALUES ('Alice')")
      count <- conn.executeQuery("SELECT COUNT(*) FROM users").map(_.head)
    yield assertCount(count, 1)
  }
}
```

## まとめ

0.7.x への移行により、以下のメリットが得られます：

1. **テスト専用モジュール**: `ldbc-testkit` と `ldbc-testkit-munit` により、自動ロールバックや MUnit 統合テストが簡単に書けるようになる
2. **DSL ヘルパーの充実**: `ident()`、`when()`、`paginate()` により安全で読みやすい SQL 構築が可能
3. **OpenTelemetry の型安全化**: セマンティクス属性ライブラリへの移行により、内部実装の堅牢性が向上
4. **Scala Native 対応の強化**: `sbt-scala-native 0.5.x` 系へのアップグレードにより、クロスプラットフォームサポートが改善

ユーザーコードへの影響は主に非推奨APIの移行（`sc()` → `ident()`、`fromSocketGroup` → `fromNetwork`）と Java/Scala バージョン要件の更新です。
