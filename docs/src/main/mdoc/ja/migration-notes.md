{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.3.xから0.4.xへの移行)

## アーキテクチャの大幅な改善

このリリースでは、ldbcプロジェクトの内部アーキテクチャが大幅に改善されました。最も重要な変更は、新しい`ldbc-core`モジュールの導入とFree Monadベースの新しいAPIです。

### 新しいモジュール構造

**新たに追加されたモジュール**

| Module / Platform | JVM | Scala Native | Scala.js | 説明                     |
|-------------------|:---:|:------------:|:--------:|------------------------|
| `ldbc-core`       |  ✅  |      ✅       |    ✅     | Free Monadベースの新しいコアAPI |

**削除されたモジュール**

- `ldbc-schemaSpy` - 非推奨のため削除されました

**全てのパッケージ（0.4.x）**

| Module / Platform    | JVM | Scala Native | Scala.js |  
|----------------------|:---:|:------------:|:--------:|
| `ldbc-sql`           |  ✅  |      ✅       |    ✅     |
| `ldbc-core`          |  ✅  |      ✅       |    ✅     |
| `ldbc-connector`     |  ✅  |      ✅       |    ✅     | 
| `jdbc-connector`     |  ✅  |      ❌       |    ❌     | 
| `ldbc-dsl`           |  ✅  |      ✅       |    ✅     |
| `ldbc-statement`     |  ✅  |      ✅       |    ✅     |
| `ldbc-query-builder` |  ✅  |      ✅       |    ✅     |
| `ldbc-schema`        |  ✅  |      ✅       |    ✅     |
| `ldbc-codegen`       |  ✅  |      ✅       |    ✅     |
| `ldbc-hikari`        |  ✅  |      ❌       |    ❌     | 
| `ldbc-plugin`        |  ✅  |      ❌       |    ❌     |

### モジュール依存関係の変更

```
0.3.x: ldbc-dsl → ldbc-sql
0.4.x: ldbc-dsl → ldbc-core → ldbc-sql
```

## 新機能

### Free Monadベースの新しいAPI（ldbc-core）

0.4.xでは、データベース操作をより関数型プログラミングに適した形で表現するため、Free Monadベースの新しいAPIが導入されました。

#### DBIOモナド

```scala 3
import ldbc.core.*
import ldbc.free.*

// Free Monadベースのデータベース操作
type DBIO[A] = Free[ConnectionOp, A]

// 使用例
val dbio: DBIO[List[User]] = for
  stmt <- ConnectionOp.createStatement
  rs   <- StatementOp.executeQuery("SELECT * FROM users")
  users <- ResultSetOp.to[List[User]]
yield users
```

#### Provider/Connectorアーキテクチャ

```scala 3
// 接続プロバイダ
trait Provider[F[_]]:
  def use[A](f: Connector[F] => F[A]): F[A]
  
// コネクタ（接続管理とDBIO実行）
trait Connector[F[_]]:
  def run[A](dbio: DBIO[A]): F[A]
```

この新しいアーキテクチャにより、以下のメリットが得られます：
- **テスタビリティの向上**: DBIOの記述と実行を分離し、モックを使った単体テストが容易に
- **型安全性の強化**: すべてのデータベース操作が型レベルで安全に表現
- **効果の合成**: 複数のデータベース操作を関数型的に合成可能

### ストリーミングサポートの改善

`fs2-core`が`ldbc-dsl`の依存関係に追加され、データベースからのストリーミング処理がより効率的になりました。

```scala 3
import fs2.Stream
import ldbc.dsl.*

// 大量のデータをストリーミング処理
val stream: Stream[IO, User] = 
  sql"SELECT * FROM users".query[User].stream
```

## 破壊的変更

### Logging機能のパッケージ移動

Logging関連のクラスが`ldbc-sql`モジュールから新しい`ldbc-core`モジュールに移動されました。

**パッケージ名の変更**

| 0.3.x                         | 0.4.x                     |
|-------------------------------|---------------------------|
| `ldbc.sql.logging.LogEvent`   | `ldbc.logging.LogEvent`   |
| `ldbc.sql.logging.LogHandler` | `ldbc.logging.LogHandler` |

**マイグレーション方法**

```diff
-import ldbc.sql.logging.{ LogEvent, LogHandler }
+import ldbc.logging.{ LogEvent, LogHandler }
```

この変更により、Logging機能がより基礎的なレイヤーに配置され、すべてのモジュールから利用しやすくなりました。

### 依存関係の構造変更

以下の依存関係が`ldbc-dsl`から`ldbc-core`に移動されました：

- `cats-free`
- `cats-effect`

`ldbc-dsl`を使用している場合、これらの依存関係は`ldbc-core`経由で提供されるため、明示的な依存関係の追加は不要です。

### 非推奨モジュールの削除

以下のモジュールが削除されました：

- `ldbc-schemaSpy` - 非推奨警告が出ていたモジュール

このモジュールを使用している場合は、代替の実装を検討してください。

## 既存APIの継続サポート

以下の機能は0.4.xでも引き続き利用可能です：

- プレーンクエリ構文（`sql"..."`）
- クエリビルダー
- カスタムデータ型のサポート（Encoder/Decoder/Codec）
- Table/TableQuery API
- 列の合成（twiddles）

これらのAPIに変更はないため、既存のコードはそのまま動作します。

## 依存ライブラリのバージョンアップ

以下のライブラリがバージョンアップされました：

| ライブラリ                                     | 0.3.x  | 0.4.x  |
|-------------------------------------------|--------|--------|
| cats-effect                               | 3.6.1  | 3.6.2  |
| fs2-core                                  | -      | 3.12.0 |
| otel4s-core-trace                         | 0.12.0 | 0.13.1 |
| opentelemetry-exporter-otlp               | 1.51.0 | 1.52.0 |
| opentelemetry-sdk-extension-autoconfigure | 1.51.0 | 1.52.0 |

## マイグレーションのまとめ

0.3.xから0.4.xへの移行における主要なポイント：

1. **新しいアーキテクチャの採用を検討**: Free Monadベースの新しいAPIは、より高度な型安全性とテスタビリティを提供します
2. **Loggingパッケージの変更**: `ldbc.sql.logging` → `ldbc.logging`へのimport文の更新
3. **依存関係の確認**: `ldbc-schemaSpy`を使用している場合は代替手段の検討が必要
4. **既存のAPIの継続利用**: 既存のDSL APIは引き続き利用可能で、段階的な移行が可能

詳細な使用例やベストプラクティスについては、[チュートリアル](../tutorial/index.md)を参照してください。
