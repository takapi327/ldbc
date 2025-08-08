{%
  laika.title = データベース操作
  laika.metadata.language = ja
%}

# データベース操作

[データ更新](/ja/tutorial/Updating-Data.md)でデータの挿入、更新、削除の方法を学びました。このページでは、トランザクション管理やコミットのタイミングなど、より高度なデータベース操作について説明します。

データベース操作を安全に行うには、トランザクション管理が重要です。ldbcは、関数型プログラミングの原則に従った形で、トランザクション管理のための抽象化を提供します。

このセクションでは、データベース操作について説明します。

データベース接続を行う前にコミットのタイミングや読み書き専用などの設定を行う必要があります。

## 読み取り専用

読み取り専用のトランザクションを開始するには、`readOnly`メソッドを使用します。

`readOnly`メソッドを使用することで実行するクエリの処理を読み込み専用にすることができます。`readOnly`メソッドは`insert/update/delete`文でも使用することができますが、書き込み処理を行うので実行時にエラーとなります。

```scala
val read = sql"SELECT 1".query[Int].to[Option].readOnly(connection)
```

## 書き込み

書き込みを行うには、`commit`メソッドを使用します。

`commit`メソッドを使用することで実行するクエリの処理をクエリ実行時ごとにコミットするように設定することができます。

```scala
val write = sql"INSERT INTO `table`(`c1`, `c2`) VALUES ('column 1', 'column 2')".update.commit(connection)
```

## トランザクション

トランザクションを開始するには、`transaction`メソッドを使用します。

`transaction`メソッドを使用することで複数のデータベース接続処理を1つのトランザクションにまとめることができます。

ldbcは`DBIO[A]`という形式でデータベースへの接続処理を組むことになる。 DBIOはモナドなので、for内包を使って2つの小さなプログラムを1つの大きなプログラムにすることができます。

```scala 3
val program: DBIO[(List[Int], Option[Int], Int)] =
  for
    result1 <- sql"SELECT 1".query[Int].to[List]
    result2 <- sql"SELECT 2".query[Int].to[Option]
    result3 <- sql"SELECT 3".query[Int].unsafe
  yield (result1, result2, result3)
```

1つのプログラムとなった`DBIO`を`transaction`メソッドで1つのトランザクションでまとめて処理を行うことができます。

```scala
val transaction = program.transaction(connection)
```

## 次のステップ

ここまでで、ldbcの基本的な使い方をすべて学んできました。データベース接続、クエリ実行、データの読み書き、トランザクション管理など、日常的なデータベース操作に必要な知識を身につけました。

ここからは、より高度なトピックに進みます。まずは[エラーハンドリング](/ja/tutorial/Error-Handling.md)から始め、データベース操作で発生する可能性のある例外を適切に処理する方法を学びましょう。
