{%
  laika.title = はじめてのldbc
  laika.metadata.language = ja
%}

# はじめてのldbc

## ldbcとは

ldbcは、Scala 3で書かれたタイプセーフなMySQLデータベースクライアントライブラリです。SQLの記述を安全かつ簡潔に行いながら、Scalaの強力な型システムを活用してデータベース操作のミスを減らすことができます。

主な特徴:

- **型安全**: コンパイル時にSQLクエリの問題を検出
- **簡潔な構文**: SQL文字列補間を使った直感的なクエリ記述
- **Cats Effectとの統合**: 純粋関数型プログラミングをサポート
- **スキーマ自動生成**: SQLからScalaコードを生成
- **クエリビルダー**: SQLを直接書かずにタイプセーフなクエリを構築

## このチュートリアルの進め方

このチュートリアルはステップバイステップで、ldbcを使ったデータベース操作の基本から応用までをカバーします。以下の順序で進めることをお勧めします：

1. [セットアップ](/ja/tutorial/Setup.md) - 環境構築とデータベースの準備
2. [コネクション](/ja/tutorial/Connection.md) - データベースへの接続方法
3. [シンプルプログラム](/ja/tutorial/Simple-Program.md) - 最初の簡単なクエリ実行
4. [パラメータ](/ja/tutorial/Parameterized-Queries.md) - パラメータ付きクエリ
5. [データ選択](/ja/tutorial/Selecting-Data.md) - SELECT文でのデータ取得
6. [データ更新](/ja/tutorial/Updating-Data.md) - INSERT/UPDATE/DELETE操作
7. [データベース操作](/ja/tutorial/Database-Operations.md) - トランザクション管理
8. [エラーハンドリング](/ja/tutorial/Error-Handling.md) - 例外処理

より高度なトピック:

9. [ロギング](/ja/tutorial/Logging.md) - クエリのロギング
10. [カスタムデータ型](/ja/tutorial/Custom-Data-Type.md) - 独自データ型のサポート
11. [クエリビルダー](/ja/tutorial/Query-Builder.md) - タイプセーフなクエリ構築
12. [スキーマ](/ja/tutorial/Schema.md) - テーブル定義
13. [スキーマコード生成](/ja/tutorial/Schema-Code-Generation.md) - SQLからコード生成

## 15分クイックスタート

まずは手早くldbcの魅力を体験してみましょう。以下のステップで、最小限のコードでデータベースクエリを実行します。

### 1. プロジェクト設定

Scala CLIを使って簡単に始められます：

```bash
mkdir ldbc-quickstart
cd ldbc-quickstart
touch QuickStart.scala
```

`QuickStart.scala`に以下のコードを記述します：

```scala
//> using scala "@SCALA_VERSION@"
//> using dep "@ORGANIZATION@::ldbc-dsl:@VERSION@"
//> using dep "@ORGANIZATION@::ldbc-connector:@VERSION@"

import cats.effect._
import cats.syntax.all._
import ldbc.dsl.io._

object QuickStart extends IOApp.Simple {

  // トレーサー設定（ログ記録用）
  given Tracer[IO] = Tracer.noop[IO]
  
  // データベース接続設定
  val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("sandbox_db")
  )
  
  def run: IO[Unit] = {
    // 簡単なクエリプログラム
    val program = for {
      // ユーザー一覧の取得
      users <- sql"SELECT id, name, email FROM user".query[(Int, String, String)].to[List]
      _     <- DBIO.delay[IO](println("--- ユーザー一覧 ---"))
      _     <- DBIO.delay[IO](users.foreach(println))
      
      // 新しいユーザーの追加
      _     <- sql"INSERT INTO user (name, email) VALUES ('山田太郎', 'yamada@example.com')".update
      
      // 追加後のユーザー数確認
      count <- sql"SELECT COUNT(*) FROM user".query[Int].unsafe
      _     <- DBIO.delay[IO](println(s"総ユーザー数: $count"))
    } yield ()
    
    // プログラムの実行
    connection.use { conn =>
      program.transaction(conn)
    }
  }
}
```

### 2. 実行

Dockerでデータベースを起動しておいてください（[セットアップ](/ja/tutorial/Setup.md)参照）。

```bash
scala-cli QuickStart.scala
```

このシンプルな例でも、ldbcの主要な特徴が見えてきます：
- SQL文字列補間で直感的にクエリを記述
- 型安全なデータ取得
- モナドベースのコンポジションでクエリを組み合わせ
- トランザクション管理

## ldbcの仕組み

ldbcは以下のコンポーネントで構成されています：

1. **DSL**: SQL文字列を安全に構築するためのAPI
2. **コネクタ**: データベース接続とクエリ実行
3. **スキーマ**: テーブル定義とコード生成
4. **クエリビルダー**: タイプセーフなクエリ構築

これらのコンポーネントが連携して、タイプセーフで効率的なデータベースアクセスを実現します。

## なぜldbcを選ぶべきか

- **学習コストの低さ**: SQLの知識をそのまま活かせる文字列補間
- **安全性**: コンパイル時の型チェックでバグを早期発見
- **生産性**: ボイラープレートコードの削減と自動コード生成
- **パフォーマンス**: 最適化されたMySQL接続管理
- **関数型プログラミングの恩恵**: Cats Effectとの統合によるコンポーザビリティ

## 次のステップ

基本を理解したら、次は[セットアップ](/ja/tutorial/Setup.md)から始めて、順番に各チュートリアルを進めてください。各トピックでは実践的な例とともに詳細な説明が提供されています。

ldbcを使えば、データベース操作がより安全で楽しいものになるはずです！
