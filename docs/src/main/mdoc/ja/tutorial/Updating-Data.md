{%
  laika.title = データ更新
  laika.metadata.language = ja
%}

# データ更新

[データ選択](/ja/tutorial/Selecting-Data.md)でデータを取得する方法を学んだところで、今度はデータベースにデータを書き込む方法を見ていきましょう。このページでは、INSERT、UPDATE、DELETEといったデータ操作言語（DML）の基本を説明します。

データベースへの書き込み操作は、データベースの状態を変更するため、読み取り操作とは少し異なる挙動をします。ldbcでは、これらの操作を安全に行うための適切な抽象化を提供しています。

## 挿入

挿入は簡単で、selectと同様に動作します。ここでは、`user`テーブルに行を挿入する`DBIO`を作成するメソッドを定義します。

```scala
def insertUser(name: String, email: String): DBIO[Int] =
  sql"INSERT INTO user (name, email) VALUES ($name, $email)"
    .update
```

行を挿入してみよう。

```scala
insertUser("dave", "dave@example.com").commit.unsafeRunSync()
```

そして読み返す。

```scala
sql"SELECT * FROM user"
  .query[(Int, String, String)] // Query[IO, (Int, String, String)]
  .to[List]                     // DBIO[IO, List[(Int, String, String)]]
  .readOnly(conn)               // IO[List[(Int, String, String)]]
  .unsafeRunSync()              // List[(Int, String, String)]
  .foreach(println)             // Unit
```

## 更新

更新も同じパターンだ。ここではユーザーのメールアドレスを更新する。

```scala
def updateUserEmail(id: Int, email: String): DBIO[Int] =
  sql"UPDATE user SET email = $email WHERE id = $id"
    .update
```

結果の取得

```scala
updateUserEmail(1, "alice+1@example.com").commit.unsafeRunSync()

sql"SELECT * FROM user WHERE id = 1"
  .query[(Int, String, String)] // Query[IO, (Int, String, String)]
  .to[Option]                   // DBIO[IO, List[(Int, String, String)]]
  .readOnly(conn)               // IO[List[(Int, String, String)]]
  .unsafeRunSync()              // List[(Int, String, String)]
  .foreach(println)             // Unit
// Some((1,alice,alice+1@example.com))
```

## 自動生成キー

インサートする際には、新しく生成されたキーを返したいものです。まず、挿入して最後に生成されたキーを`LAST_INSERT_ID`で取得し、指定された行を選択するという難しい方法をとります。

```scala 3
def insertUser(name: String, email: String): DBIO[IO, (Int, String, String)] =
  for
    _    <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".update
    id   <- sql"SELECT LAST_INSERT_ID()".query[Int].unsafe
    task <- sql"SELECT * FROM user WHERE id = $id".query[(Int, String, String)].to[Option]
  yield task
```

```scala
insertUser("eve", "eve@example.com").commit.unsafeRunSync()
```

これは苛立たしいことだが、すべてのデータベースでサポートされている（ただし、「最後に使用されたIDを取得する」機能はベンダーによって異なる）。

MySQLでは、`AUTO_INCREMENT`が設定された行のみが挿入時に返すことができます。上記の操作を2つのステートメントに減らすことができます

自動生成キーを使用して行を挿入する場合、`returning`メソッドを使用して自動生成キーを取得できます。

```scala 3
def insertUser(name: String, email: String): DBIO[IO, (Int, String, String)] =
  for
    id   <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".returning[Int]
    user <- sql"SELECT * FROM user WHERE id = $id".query[(Int, String, String)].to[Option]
  yield user
```

```scala
insertUser("frank", "frank@example.com").commit.unsafeRunSync()
```

## バッチ更新

バッチ更新を行うには、`NonEmptyList`を使用して複数の行を挿入する`insertManyUser`メソッドを定義します。

```scala 3
def insertManyUser(users: NonEmptyList[(String, String)]): DBIO[IO, Int] =
  val value = users.map { case (name, email) => sql"($name, $email)" }
  (sql"INSERT INTO user (name, email) VALUES" ++ values(value)).update
```

このプログラムを実行すると、更新された行数が得られる。

```scala
val users = NonEmptyList.of(
  ("greg", "greg@example.com"),
  ("henry", "henry@example.com")
)

insertManyUser(users).commit.unsafeRunSync()
```

## 次のステップ

これでデータベースにデータを挿入、更新、削除する方法が理解できました。これらの操作を組み合わせて、実際のアプリケーションで必要なデータ操作を行うことができます。

次は[データベース操作](/ja/tutorial/Database-Operations.md)に進み、トランザクション管理などの重要な概念を学びましょう。
