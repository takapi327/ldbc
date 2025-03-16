{%
  laika.title = シンプルプログラム
  laika.metadata.language = ja
%}

# シンプルなプログラム

[コネクション](/ja/tutorial/Connection.md)の設定方法を学んだところで、いよいよデータベースにクエリを実行してみましょう。このページでは、ldbcを使った基本的なデータベースプログラムの作成方法を説明します。

ldbcでは、`DBIO`モナドを使って一連のデータベース操作を表現します。この強力な抽象化により、複数のクエリを組み合わせたり、純粋関数型プログラミングの恩恵を受けながらデータベース操作を行うことができます。

`DBIO`モナドは、実際のデータベース接続を使って実行される処理を表現します。これにより、実際の接続が行われるまでクエリの実行が遅延され、効率的にリソースを管理できます。

※ このページで使用するプログラムの環境はセットアップで構築したものを前提としています。

## 1つめのプログラム：単純な値の取得

最初のプログラムでは、データベースに接続し、単純な計算結果を取得する方法を学びます。

まず、`sql string interpolator`を使って、データベースに対する問い合わせを作成します。この機能を使うことで、SQLクエリを安全かつ簡潔に記述することができます。

```scala 3
// SQLクエリを作成し、返り値の型を指定
val program: DBIO[Option[Int]] = sql"SELECT 2".query[Int].to[Option]
```

上記のコードでは次のことを行なっています：

1. `sql"SELECT 2"`：SQL文字列補間子を使ってSQLクエリを作成
2. `.query[Int]`：結果として`Int`型の値を期待することを指定
3. `.to[Option]`：結果を`Option[Int]`型で取得することを指定（結果が存在しない場合は`None`）

`to`メソッドでは、結果の取得方法を指定できます。主要なメソッドと戻り値の型は以下の通りです：

| Method       | Return Type    | Notes                        |
|--------------|----------------|------------------------------|
| `to[List]`   | `F[List[A]]`   | すべての結果をリストで取得                |
| `to[Option]` | `F[Option[A]]` | 結果は0か1行、それ以上はエラーが発生する        |
| `unsafe`     | `F[A]`         | 正確に1行の結果を期待、そうでない場合はエラーが発生する |

次に、このプログラムを実行します。データベース接続を取得し、クエリを実行し、結果を表示します：

```scala 3
// データベース接続を取得し、プログラムを実行
provider
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

上記のコードでは：

1. `provider.use`：データベース接続プロバイダーからコネクションを取得
2. `program.readOnly(conn)`：作成したクエリを読み取り専用モードで実行
3. `.map(println(_))`：結果を標準出力に表示
4. `.unsafeRunSync()`：IO効果を実行

このプログラムを実行すると、データベースから`Some(2)`という結果が返ってきます。

**Scala CLIで実行**

このプログラムは、Scala CLIを使って簡単に実行できます：

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/02-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## 2つめのプログラム：複数のクエリの組み合わせ

実際のアプリケーションでは、一つのトランザクションで複数のクエリを実行したいケースが多くあります。`DBIO`モナドはこれを簡単に実現します。for内包表記を使って、複数のクエリを一つの大きなプログラムに組み合わせることができます：

```scala 3
// 複数のクエリを組み合わせてプログラムを作成
val program: DBIO[(List[Int], Option[Int], Int)] =
  for
    result1 <- sql"SELECT 1".query[Int].to[List]    // 結果をリストで取得
    result2 <- sql"SELECT 2".query[Int].to[Option]  // 結果をOptionで取得
    result3 <- sql"SELECT 3".query[Int].unsafe      // 結果を直接取得
  yield (result1, result2, result3)
```

上記のfor内包表記では、3つの異なるクエリを順番に実行し、それぞれの結果を変数に束縛しています。最後に、3つの結果をタプルとして返しています。

このプログラムの実行方法も前回と同様です：

```scala 3
// データベース接続を取得し、プログラムを実行
provider
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

実行結果は`(List(1), Some(2), 3)`のようになります。これは3つのクエリの結果が1つのタプルにまとめられたものです。

**Scala CLIで実行**

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/03-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## 3つめのプログラム：データ更新操作

読み取りだけでなく、データベースに対して書き込みを行うプログラムも作成できます。以下は、ユーザーテーブルに新しいレコードを挿入する例です：

```scala 3
// INSERT文を実行し、影響を受けた行数を取得
val program: DBIO[Int] =
  sql"INSERT INTO user (name, email) VALUES ('Carol', 'carol@example.com')".update
```

`update`メソッドを使用すると、INSERT、UPDATE、DELETEなどの更新系クエリを実行できます。戻り値は影響を受けた行数（この場合は1）です。

更新系クエリを実行する場合は、`readOnly`ではなく`commit`メソッドを使用してトランザクションをコミットする必要があります：

```scala 3
// データベース接続を取得し、更新プログラムを実行してコミット
provider
  .use { conn =>
    program.commit(conn).map(println(_))
  }
  .unsafeRunSync()
```

`commit`メソッドは自動的に`AutoCommit`を有効にして、クエリ実行後にトランザクションをコミットします。これにより、変更がデータベースに永続化されます。

**Scala CLIで実行**

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/04-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## トランザクション制御

ldbcでは、以下の主要なトランザクション制御メソッドを提供しています：

- `readOnly(conn)`: 読み取り専用のトランザクションで実行します（データ更新ができません）
- `commit(conn)`: 自動コミットモードで実行し、成功時に変更をコミットします
- `rollback(conn)`: 自動コミットをオフにして実行し、終了時にロールバックします
- `transaction(conn)`: 自動コミットをオフにして実行し、成功時にコミットし、例外発生時にはロールバックします

複雑なトランザクションを実装する場合は、`transaction`メソッドを使用すると、適切にコミットとロールバックを処理できます：

```scala 3
val complexProgram: DBIO[Int] = for
  _ <- sql"INSERT INTO accounts (owner, balance) VALUES ('Alice', 1000)".update
  _ <- sql"INSERT INTO accounts (owner, balance) VALUES ('Bob', 500)".update
  count <- sql"SELECT COUNT(*) FROM accounts".query[Int].unsafe
yield count

provider
  .use { conn =>
    complexProgram.transaction(conn).map(println(_))
  }
  .unsafeRunSync()
```

このプログラムでは、どちらかのINSERTが失敗した場合、両方の変更がロールバックされます。成功した場合のみ、両方の変更がコミットされます。

## 次のステップ

基本的なクエリの実行方法を学んだところで、次は[パラメータ付きクエリ](/ja/tutorial/Parameterized-Queries.md)についてさらに詳しく見ていきましょう。
