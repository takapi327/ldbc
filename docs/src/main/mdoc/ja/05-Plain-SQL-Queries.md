# プレーンなSQLクエリ

時には、抽象度の高いレベルではうまくサポートされていない操作のために、独自のSQLコードを書く必要があるかもしれません。JDBCの低レイヤーに戻る代わりに、ScalaベースのAPIでLDBCのPlain SQLクエリーを使うことができます。
この章では、そのような場合にLDBCでPlain SQLクエリーを使用してデータベースへの接続処理を行うための方法について説明します。

プロジェクトへの依存関係やDataSourceの使用とログに関しては、前章の[データベース接続](http://localhost:4000/ja/04-Database-Connection.html)の章を参照してください。

## Plain SQL

LDBCでは以下のようにsql文字列補間をリテラルSQL文字列で使用してプレーンなクエリを構築します。

クエリに注入された変数や式は、結果のクエリ文字列のバインド変数に変換されます。クエリ文字列に直接挿入されるわけではないので、SQLインジェクション攻撃の危険はありません。

```scala 3
val select = sql"SELECT id, name, age FROM user WHERE id = $id" // SELECT id, name, age FROM user WHERE id = ?
val insert = sql"INSERT INTO user (id, name, age) VALUES($id, $name, $age)" // INSERT INTO user (id, name, age) VALUES(?, ?, ?)
val update = sql"UPDATE user SET id = $id, name = $name, age = $age" // UPDATE user SET id = ?, name = ?, age = ?
val delete = sql"DELETE FROM user WHERE id = $id" // DELETE FROM user WHERE id = ?
```

Plain SQLクエリーは実行時にSQL文を構築するだけです。これは安全かつ簡単に複雑なステートメントを構築する方法を提供しますが、これは単なる埋め込み文字列にすぎません。ステートメントに構文エラーがあったり、データベースとScalaコードの型が一致しなかったりしてもコンパイル時に検出することはできません。

クエリ実行結果の戻り値の型、接続方法の設定に関しては前章の「データベース接続」にある[Query](http://localhost:4000/ja/04-Database-Connection.html#Query)項目以降を参照してください。
テーブル定義を使用して構築されたクエリと同じように構築および動作します。

プレーンなクエリと型安全なクエリは構築方法が違うだけで後続の接続方法などは同じ実装です。そのため2つを組み合わせてクエリを実行することも可能です。

```scala 3
(for
  result1 <- sql"INSERT INTO user (id, name, age) VALUES($id, $name, $age)".update
  result2 <- userQuery.update("name", "update name").update
  ...
yield ...).transaction
```
