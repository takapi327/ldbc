{%
  laika.title = シンプルプログラム
  laika.metadata.language = ja
%}

# シンプルなプログラム

ここではまず、シンプルなプログラムを作成し実行することでldbcの基本的な使い方を説明します。

※ ここで使用するプログラムの環境はセットアップで構築したものを前提としています。

## 1つめのプログラム

このプログラムでは、データベースに接続し計算結果を取得するプログラムを作成します。

それでは、sql string interpolatorを使って、データベースに定数の計算を依頼する問い合わせを作成してみましょう。

```scala
val program: Executor[IO, Option[Int]] = sql"SELECT 2".query[Int].to[Option]
```

sql string interpolatorを使って作成したクエリは`query`メソッドで取得する型の決定を行います。ここでは`Int`型を取得するため、`query[Int]`としています。また、`to`メソッドで取得する型を決定します。ここでは`Option`型を取得するため、`to[Option]`としています。

| Method       | Return Type    | Notes                         |
|--------------|----------------|-------------------------------|
| `to[List]`   | `F[List[A]]`   | `すべての結果をリストで表示`               |
| `to[Option]` | `F[Option[A]]` | `結果は0か1、そうでなければエラーが発生する`      |
| `unsafe`     | `F[A]`         | `正確には1つの結果で、そうでない場合はエラーが発生する` |

最後に、データベースに接続して値を返すプログラムを書きます。このプログラムは、データベースに接続し、クエリを実行し、結果を取得します。

```scala
connection
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

定数を計算するためにデータベースに接続した。かなり印象的だ。

**Scala CLIで実行**

このプログラムも、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/02-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## 2つめのプログラム

一つの取引で複数のことをしたい場合はどうすればいいのか？簡単だ！Executorはモナドなので、for内包を使って2つの小さなプログラムを1つの大きなプログラムにすることができる。

```scala
val program: Executor[IO, (List[Int], Option[Int], Int)] =
  for
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
    result3 <- sql"SELECT 3".query[Int].unsafe
  yield (result1, result2, result3)
```

最後に、データベースに接続して値を返すプログラムを書く。このプログラムは、データベースに接続し、クエリを実行し、結果を取得する。

```scala
connection
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

**Scala CLIで実行**

このプログラムも、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/03-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## 3つめのプログラム

データベースに対して書き込みを行うプログラムを書いてみよう。ここでは、データベースに接続し、クエリを実行し、データを挿入する。

```scala
val program: Executor[IO, Int] =
  sql"INSERT INTO user (name, email) VALUES ('Carol', 'carol@example.com')".update
```

先ほどと異なる点は、`commit`メソッドを呼び出すことである。これにより、トランザクションがコミットされ、データベースにデータが挿入される。

```scala
connection
  .use { conn =>
    program.commit(conn).map(println(_))
  }
  .unsafeRunSync()
```

**Scala CLIで実行**

このプログラムも、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/04-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```
