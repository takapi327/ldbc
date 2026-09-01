{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.7.xから0.8.xへの移行)

## パッケージ

**全てのパッケージ**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-testkit`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit_3)                   |
| `ldbc-testkit-munit`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-testkit-munit_3)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.8.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 主要な変更点

### 1. `NO_BACKSLASH_ESCAPES` 環境での SQL インジェクションを修正

**これは 0.8.0 で最も重要な変更です。ldbc コネクターを使っている場合はアップグレードを推奨します。**

0.7.x 以前のクライアントサイド prepared statement（`useServerPrepStmts = false`、既定）は、文字列パラメータをバックスラッシュエスケープ（`'` → `\'`）でのみエスケープし、サーバーの `sql_mode` を参照していませんでした。

MySQL の `NO_BACKSLASH_ESCAPES` sql_mode が有効なセッションでは、バックスラッシュは通常の文字として扱われます。そのため `\'` はクォートを無効化できず、**文字列リテラルから脱出できてしまいます**。

```scala
// 0.7.x 以前: sql_mode = 'NO_BACKSLASH_ESCAPES' のセッションで
ps.setString(1, "zzz' OR 1=1 -- ")
// 生成される SQL: WHERE t.name = 'zzz\' OR 1=1 -- '
//               → (name = 'zzz\') OR 1=1  ... 常に真になる
```

0.8.0 では以下の 3 点で修正しています。

**1. エスケープ処理を `QueryRenderer` に一元化**

文字列パラメータを SQL リテラルへ変換する経路が `QueryRenderer` ただ一つになりました。`Parameter` 自身は文字列の SQL テキスト表現を公開しなくなったため、sql_mode を考慮しない経路が構造上存在しません。

**2. sql_mode に応じたエスケープ**

| sql_mode | エスケープ方法 |
|----------|--------------|
| 既定 | `'` → `\'`、`"` → `\"`、`\` → `\\`、制御文字 → `\0` `\b` `\n` `\r` `\Z` |
| `NO_BACKSLASH_ESCAPES` | `'` → `''`（シングルクォートの二重化） |

`NO_BACKSLASH_ESCAPES` 時にクォートを二重化するのは、直前のバックスラッシュに吸収されることがない唯一の方法だからです。

**3. sql_mode をセッション中に追従**

`Protocol.noBackslashEscapes` が追加されました。初回ハンドシェイクの status flags で初期化され、以降は受信する **すべての OK / EOF パケットの status flags で更新**されます。そのため接続後に `SET SESSION sql_mode = ...` を実行した場合でも、その時点以降のクエリ生成に正しく反映されます。

ユーザーコードの変更は不要です。

### 2. JDBC 4.3 の enquote API に対応

MySQL Connector/J 9.7.0（WL #17215）に追随し、`ldbc.sql.Statement` に 4 つのメソッドを追加しました。SQL 文を動的に組み立てる際に、値や識別子を安全にクォートするために使えます。

| メソッド | 用途 |
|---------|------|
| `enquoteLiteral(value)` | 文字列をシングルクォートで囲んだリテラルにする |
| `enquoteIdentifier(identifier, alwaysQuote)` | 識別子をクォートする |
| `enquoteNCharLiteral(value)` | `N` プレフィックス付きの各国文字リテラルにする |
| `isSimpleIdentifier(identifier)` | クォートなしで使える単純な識別子か判定する |

```scala
for
  stmt <- conn.createStatement()
  a    <- stmt.enquoteLiteral("G'Day")            // 'G''Day'
  b    <- stmt.enquoteIdentifier("my table", false) // `my table`
  c    <- stmt.enquoteIdentifier("user", true)      // `user`
  d    <- stmt.enquoteNCharLiteral("Hello")         // N'Hello'
  e    <- stmt.isSimpleIdentifier("user_name")      // true
  f    <- stmt.isSimpleIdentifier("select")         // false（予約語）
yield ()
```

`isSimpleIdentifier` は MySQL の規則に従い、`[0-9a-zA-Z$_]` と `U+0080` 以上の拡張文字のみで構成され、数字のみではなく、64 文字以内で、予約語でないものを単純な識別子と判定します。

`ANSI_QUOTES` sql_mode が有効な場合、識別子のクォート文字は `` ` `` ではなく `"` になります。

これらは `Statement` と `PreparedStatement` の両方で、ldbc コネクター・jdbc コネクターのどちらでも利用できます。

> **補足**: 既存の `ident()` ヘルパーは `sql` 補間子の中で識別子を埋め込むためのもので、引き続き利用できます。`enquoteIdentifier` は JDBC 標準 API との互換性が必要な場面や、`alwaysQuote` の制御が必要な場面で使ってください。

### 3. `ldbc-plugin` が sbt 2 に対応

`ldbc-plugin` が sbt 1 と sbt 2 の両方向けにクロスビルドされるようになりました。sbt 1 用（Scala 2.12）と sbt 2 用（Scala 3）の成果物が同時に公開されます。

記述方法はどちらのバージョンでも同じで、sbt が適切な成果物を解決します。

```scala
// project/plugins.sbt — sbt 1.x / sbt 2.x のどちらでも同じ
addSbtPlugin("io.github.takapi327" % "ldbc-plugin" % "0.8.0")
```

### 4. `insert` のカラム順不一致バグを修正

タプルを渡す `insert` が、テーブルの `*` 射影で定義したエンティティマッピングを経由するようになりました。

0.7.x では、タプルをカラムのエンコーダーへ直接キャストして渡していたため、**モデルのフィールド順とテーブルの `*` 射影のカラム順が異なる場合に、値が意図しないカラムへ挿入される**可能性がありました。

```scala
userTable.insert((1L, "Alice", Some(20)))
```

上記のようなコードは、`*` 射影の順序が `id *: name *: age` でない場合、0.7.x と 0.8.0 で生成される SQL のパラメータ順が変わります。挿入結果が正しくなる方向の修正ですが、**アップグレード後にテストで確認することを推奨します**。

### 5. 依存ライブラリの更新

| ライブラリ | 変更前 (0.7.x) | 変更後 (0.8.0) |
|-----------|--------------|---------------|
| MySQL Connector/J | 9.6.0 | 9.7.0 |
| twiddles-core | 0.10.0 | 1.1.0 |

`ldbc.connector.data.Constants.DRIVER_VERSION` も `0.8.0` に更新されています。

## 破壊的変更

### `Parameter` が `sealed trait` になり `sql` が削除されました

`ldbc.connector.data.Parameter` は型ごとの case class を持つ `sealed trait` になり、`def sql: String` が削除されました。

**変更前 (0.7.x):**
```scala
trait Parameter:
  def columnDataType: ColumnDataType
  def sql:            String
  def encode:         BitVector
```

**変更後 (0.8.0):**
```scala
sealed trait Parameter:
  def columnDataType: ColumnDataType
  def encode:         BitVector
```

これは主要な変更点 1（SQL インジェクション修正）の一部です。文字列の SQL リテラル化は sql_mode に依存するため、`Parameter` からその表現を取り除き、`QueryRenderer` を通る以外の方法をなくしています。

- **独自の `Parameter` 実装**は `sealed` になったため定義できません。`Parameter.string(...)` などのファクトリメソッドを使ってください
- **`param.sql` を参照していたコード**は `param.toString` に置き換えられます。ただし `toString` は表示・診断用の sql_mode 非依存なリテラルであり、**実行する SQL の組み立てには使えません**

### `SQLException` から `params` を削除しました

`SQLException` とそのサブクラスから `params: SortedMap[Int, Parameter]` パラメータを削除しました。`ERRPacket.toException` のシグネチャも同様です。

これに伴い、以下が出力されなくなりました。

- OpenTelemetry 属性 `error.parameter.$i.type` / `error.parameter.$i.value`
- 例外メッセージ内の "and the arguments were" セクション

バインドした値が例外メッセージやテレメトリ経由で漏れる経路を塞ぐための変更です。**これらの属性を使ってダッシュボードやアラートを構成している場合は影響を受けます。**

### `Statement` に 4 つの抽象メソッドが追加されました

主要な変更点 2 の 4 メソッドは `ldbc.sql.Statement` の抽象メソッドとして追加されています。ldbc が提供するコネクターを使っている限り影響はありませんが、**`Statement` や `PreparedStatement` を独自に実装している場合はコンパイルエラーになります。**

### twiddles-core が 1.1.0 になりました

`ldbc-dsl` と `ldbc-connector` が依存する twiddles-core が 0.10.0 から 1.1.0 に上がりました。twiddles を直接利用しているプロジェクトはバージョンを揃えてください。

### 変更がないもの

参考までに、0.7.x から変わっていない要件を挙げます。

| | 0.7.x | 0.8.0 |
|---|---|---|
| Java バージョン | 17、21、25 | 変更なし |
| Scala バージョン | 3.3.x / 3.8.x | 変更なし |

## 非推奨API

0.7.0 で非推奨になった以下の API は 0.8.0 でも引き続き利用できますが、将来のバージョンで削除される予定です。

| API | 非推奨バージョン | 代替 |
|-----|:-----------:|------|
| `sc(identifier)` | 0.7.0 | `ident(identifier)` |
| `Connection.fromSocketGroup(...)` | 0.7.0 | `Connection.fromNetwork(...)` |
| `SSL.fromKeyStoreFile(java.nio.file.Path, ...)` | 0.7.0 | `SSL.fromKeyStoreFile(fs2.io.file.Path, ...)` |

移行方法は [0.7.x のマイグレーションノート](https://takapi327.github.io/ldbc/0.7/ja/migration-notes.html) を参照してください。

## 移行ガイド

### 依存関係を更新する

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-dsl"       % "0.8.0"
libraryDependencies += "io.github.takapi327" %% "ldbc-connector" % "0.8.0"
```

クロスプラットフォーム構成では `%%%` を使ってください。

### `Parameter#sql` の参照を置き換える

**移行前 (0.7.x):**
```scala
val literal: String = parameter.sql
```

**移行後 (0.8.0):**
```scala
val display: String = parameter.toString
```

`toString` は表示・診断用です。実行する SQL を組み立てる目的では使わないでください。SQL を動的に組み立てる場合は、プレースホルダと `setXxx` を使うか、enquote API を使ってください。

### `SQLException` を直接構築しているコードを修正する

**移行前 (0.7.x):**
```scala
SQLException(
  message = "...",
  sql     = Some(sql),
  params  = params
)
```

**移行後 (0.8.0):**
```scala
SQLException(
  message = "...",
  sql     = Some(sql)
)
```

### `error.parameter.*` に依存したテレメトリ設定を見直す

バインド値の属性は出力されなくなりました。クエリの特定には `db.query.text` 属性を利用してください。クエリテキストのサニタイズ方法は [テレメトリ](/ja/reference/Telemetry.md) を参照してください。

### 動的な識別子を含む SQL を安全に組み立てる

**`sql` 補間子の中で使う場合（従来どおり）:**
```scala
sql"SELECT * FROM ${ident(tableName)}"
```

**JDBC 標準 API 互換の方法が必要な場合:**
```scala
for
  stmt      <- conn.createStatement()
  isSimple  <- stmt.isSimpleIdentifier(tableName)
  quoted    <- stmt.enquoteIdentifier(tableName, alwaysQuote = true)
  resultSet <- stmt.executeQuery(s"SELECT * FROM $quoted")
yield resultSet
```

### sbt 2 のプロジェクトで `ldbc-plugin` を使う

`project/plugins.sbt` の記述は sbt 1 と同じです。sbt のバージョンに応じた成果物が自動的に解決されます。

```scala
addSbtPlugin("io.github.takapi327" % "ldbc-plugin" % "0.8.0")
```

## まとめ

0.8.x への移行により、以下のメリットが得られます：

1. **セキュリティの向上**: `NO_BACKSLASH_ESCAPES` sql_mode 下でのクライアントサイド prepared statement の SQL インジェクションが修正され、エスケープ処理が一箇所に集約された
2. **JDBC 標準への追随**: MySQL Connector/J 9.7.0 に合わせて enquote 系 API に対応
3. **情報漏洩経路の縮小**: 例外メッセージとテレメトリからバインド値が出力されなくなった
4. **`insert` の正確性**: モデルのフィールド順とカラム順が異なるテーブルでの値ずれが修正された
5. **sbt 2 対応**: `ldbc-plugin` が sbt 1 / sbt 2 の両方で利用可能に

ユーザーコードへの影響は、`Parameter#sql` と `SQLException` の `params` を直接利用していた場合に限られます。多くのプロジェクトでは依存関係のバージョン更新のみで移行できます。
