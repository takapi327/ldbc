{%
  laika.title = はじめに
  laika.metadata.language = ja
%}

# ldbcチュートリアル

ldbcは、Scala 3で書かれたタイプセーフなMySQLデータベースクライアントです。SQLの記述を安全かつ簡潔に行いながら、Scalaの強力な型システムを活用してデータベース操作のミスを減らすことができます。

## ldbcの主な特徴

- **型安全**: コンパイル時にSQLクエリの問題を検出
- **簡潔な構文**: SQL文字列補間を使った直感的なクエリ記述
- **Cats Effectとの統合**: 純粋関数型プログラミングをサポート
- **スキーマ自動生成**: SQLからScalaコードを生成
- **クエリビルダー**: SQLを直接書かずにタイプセーフなクエリを構築

## クイックスタート

まずはldbcの基本的な使い方を見てみましょう。以下は、データベースからユーザー情報を取得し、新しいユーザーを追加する簡単な例です：

```scala 3
import cats.effect.*
import cats.syntax.all.*
import ldbc.connector.*
import ldbc.dsl.*

// データベース接続の設定
val provider =
  ConnectionProvider
    .default[IO]("127.0.0.1", 3306, "ldbc", "password", "ldbc")
    .setSSL(SSL.Trusted)

// クエリの実行
val program = for
  // ユーザー一覧の取得
  users <- sql"SELECT id, name FROM user".query[(Int, String)].to[List]
  
  // 新しいユーザーの追加
  _ <- sql"INSERT INTO user (name, email) VALUES ('山田太郎', 'yamada@example.com')".update
  
  // 更新後のユーザー数確認
  count <- sql"SELECT COUNT(*) FROM user".query[Int].unsafe
yield (users, count)

// プログラムの実行
provider.use { conn =>
  program.transaction(conn)
}
```

## チュートリアルの進め方

このチュートリアルシリーズは、ldbcを段階的に学べるように構成されています。以下の順序で進めることをお勧めします：

### 1. セットアップ

まずはldbcを使うための環境を整えましょう。

- [セットアップ](/ja/tutorial/Setup.md) - 開発環境とデータベースの準備
- [コネクション](/ja/tutorial/Connection.md) - データベースへの接続方法

### 2. 基本的な操作

次に、日常的によく使う機能を学びます。

- [シンプルプログラム](/ja/tutorial/Simple-Program.md) - 最初の簡単なクエリ実行
- [パラメータ](/ja/tutorial/Parameterized-Queries.md) - パラメータ付きクエリ
- [データ選択](/ja/tutorial/Selecting-Data.md) - SELECT文でのデータ取得
- [データ更新](/ja/tutorial/Updating-Data.md) - INSERT/UPDATE/DELETE操作
- [データベース操作](/ja/tutorial/Database-Operations.md) - トランザクション管理

### 3. 応用操作

基本を理解したら、より高度な機能に進みましょう。

- [エラーハンドリング](/ja/tutorial/Error-Handling.md) - 例外処理
- [ロギング](/ja/tutorial/Logging.md) - クエリのロギング
- [カスタムデータ型](/ja/tutorial/Custom-Data-Type.md) - 独自データ型のサポート
- [クエリビルダー](/ja/tutorial/Query-Builder.md) - タイプセーフなクエリ構築
- [スキーマ](/ja/tutorial/Schema.md) - テーブル定義
- [スキーマコード生成](/ja/tutorial/Schema-Code-Generation.md) - SQLからコード生成

## なぜldbcを選ぶべきか

- **学習コストの低さ**: SQLの知識をそのまま活かせる文字列補間
- **安全性**: コンパイル時の型チェックでバグを早期発見
- **生産性**: ボイラープレートコードの削減と自動コード生成
- **パフォーマンス**: 最適化されたMySQL接続管理
- **関数型プログラミングの恩恵**: Cats Effectとの統合によるコンポーザビリティ

それでは、[セットアップ](/ja/tutorial/Setup.md)から始めましょう！

## 詳細なナビゲーション

@:navigationTree {
  entries = [ { target = "/ja/tutorial", depth = 2 } ]
}
