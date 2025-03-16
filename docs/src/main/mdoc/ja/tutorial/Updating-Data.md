{%
  laika.title = データ更新
  laika.metadata.language = ja
%}

# データ更新

[データ選択](/ja/tutorial/Selecting-Data.md)でデータを取得する方法を学んだところで、今度はデータベースにデータを書き込む方法を見ていきましょう。このページでは、INSERT、UPDATE、DELETEといったデータ操作言語（DML）の基本を説明します。

## データ更新の基本

データベースへの書き込み操作は、データベースの状態を変更するため、読み取り操作とは少し異なる動作をします。ldbcでは、これらの操作を安全に行うための適切な抽象化を提供しています。

書き込み操作を行う基本的な流れは以下の通りです：

1. SQLクエリを`sql`補間子で作成
2. 適切なメソッド（`.update`、`.returning`など）を呼び出してクエリの種類を指定
3. `.commit()`または`.transaction()`でクエリを実行
4. 結果を処理

## データの挿入（INSERT）

### 基本的なINSERT操作

データを挿入するには、SQLの`INSERT`文を使用し、ldbcの`.update`メソッドを呼び出します。以下は`user`テーブルに行を挿入する例です：

```scala
// userテーブルに新しいユーザーを挿入するメソッド
def insertUser(name: String, email: String): DBIO[IO, Int] =
  sql"INSERT INTO user (name, email) VALUES ($name, $email)"
    .update
```

このメソッドを使って実際にデータを挿入し、結果を確認してみましょう：

```scala
// 挿入操作の実行
insertUser("dave", "dave@example.com").commit(conn).unsafeRunSync()
// 戻り値は影響を受けた行数（この場合は1）

// 挿入したデータの確認
sql"SELECT id, name, email FROM user WHERE name = 'dave'"
  .query[(Int, String, String)]
  .to[Option]
  .readOnly(conn)
  .unsafeRunSync()
  .foreach { case (id, name, email) =>
    println(s"ID: $id, Name: $name, Email: $email")
  }
```

`.update`メソッドは、影響を受けた行数（この場合は1）を返します。

### 自動生成キーの取得

多くの場合、テーブルには自動インクリメントのIDなどの自動生成キーが設定されています。挿入時にこの自動生成されたキー値を取得したい場合は、`.returning[T]`メソッドを使用します：

```scala
// 挿入と同時に生成されたIDを取得するメソッド
def insertUserAndGetId(name: String, email: String): DBIO[IO, Long] =
  sql"INSERT INTO user (name, email) VALUES ($name, $email)"
    .returning[Long]
```

このメソッドを使って新しいユーザーを挿入し、自動生成されたIDを取得します：

```scala
// 挿入して自動生成されたIDを取得
val newUserId = insertUserAndGetId("frank", "frank@example.com")
  .commit(conn)
  .unsafeRunSync()

println(s"新しいユーザーのID: $newUserId")
```

**注意点**: `.returning`メソッドは、MySQLでは`AUTO_INCREMENT`が設定されたカラムのみに対応しています。

### 挿入したデータの取得

挿入と同時に、挿入したデータの全情報を取得したい場合は、自動生成キーを使用して2つのステップを組み合わせることができます：

```scala
// ユーザーを表すケースクラス
case class User(id: Long, name: String, email: String)

// ユーザーを挿入し、挿入したユーザーの情報を返すメソッド
def insertAndRetrieveUser(name: String, email: String): DBIO[IO, Option[User]] =
  for
    id   <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".returning[Long]
    user <- sql"SELECT id, name, email FROM user WHERE id = $id".query[User].to[Option]
  yield user
```

使用例：

```scala
// ユーザーを挿入し、挿入したユーザーの情報を取得
insertAndRetrieveUser("grace", "grace@example.com")
  .commit(conn)
  .unsafeRunSync()
  .foreach { user =>
    println(s"挿入されたユーザー: ID=${user.id}, Name=${user.name}, Email=${user.email}")
  }
```

## データの更新（UPDATE）

データを更新するには、SQLの`UPDATE`文を使用し、同様に`.update`メソッドを呼び出します：

```scala
// ユーザーのメールアドレスを更新するメソッド
def updateUserEmail(id: Long, newEmail: String): DBIO[IO, Int] =
  sql"UPDATE user SET email = $newEmail WHERE id = $id"
    .update
```

使用例：

```scala
// ユーザーのメールアドレスを更新
updateUserEmail(1, "alice+updated@example.com")
  .commit(conn)
  .unsafeRunSync()

// 更新されたデータの確認
sql"SELECT id, name, email FROM user WHERE id = 1"
  .query[User]
  .to[Option]
  .readOnly(conn)
  .unsafeRunSync()
  .foreach { user => 
    println(s"更新されたユーザー: ID=${user.id}, Name=${user.name}, Email=${user.email}")
  }
```

### 複数条件による更新

複雑な条件での更新も可能です：

```scala
// 特定の名前と一致するユーザーのメールアドレスを更新
def updateEmailsByName(name: String, newEmail: String): DBIO[IO, Int] =
  sql"""
    UPDATE user 
    SET email = $newEmail 
    WHERE name LIKE ${"%" + name + "%"}
  """.update
```

この例では、指定された名前のパターンに一致するすべてのユーザーのメールアドレスを更新します。

## データの削除（DELETE）

データを削除するには、SQLの`DELETE`文を使用し、`.update`メソッドを呼び出します：

```scala
// IDによるユーザー削除
def deleteUser(id: Long): DBIO[IO, Int] =
  sql"DELETE FROM user WHERE id = $id"
    .update
```

使用例：

```scala
// ユーザーを削除
deleteUser(5)
  .commit(conn)
  .unsafeRunSync()

// 削除の確認
sql"SELECT COUNT(*) FROM user WHERE id = 5"
  .query[Int]
  .unsafe
  .readOnly(conn)
  .unsafeRunSync() match {
    case 0 => println("ユーザーIDが5のデータは削除されました")
    case n => println(s"ユーザーIDが5のデータはまだ存在しています (数: $n)")
  }
```

### 複数行の削除

条件に一致する複数の行を一度に削除することも可能です：

```scala
// 特定のドメインのメールアドレスを持つユーザーをすべて削除
def deleteUsersByEmailDomain(domain: String): DBIO[IO, Int] =
  sql"DELETE FROM user WHERE email LIKE ${"%@" + domain}"
    .update
```

## バッチ処理（複数データの一括操作）

### 複数行の一括挿入

多数の行を効率的に挿入するには、`VALUES`句に複数の値セットを指定できます：

```scala
import cats.data.NonEmptyList

// 複数のユーザーを一括挿入
def insertManyUsers(users: NonEmptyList[(String, String)]): DBIO[IO, Int] =
  val values = users.map { case (name, email) => sql"($name, $email)" }
  (sql"INSERT INTO user (name, email) VALUES " ++ Fragments.values(values)).update
```

使用例：

```scala
// 複数ユーザーの定義
val newUsers = NonEmptyList.of(
  ("greg", "greg@example.com"),
  ("henry", "henry@example.com"),
  ("irene", "irene@example.com")
)

// 一括挿入の実行
val insertedCount = insertManyUsers(newUsers).commit(conn).unsafeRunSync()
println(s"挿入された行数: $insertedCount") // "挿入された行数: 3" が出力されるはず
```

### 複数操作のトランザクション

複数の操作をアトミックに実行するには、トランザクションを使用します。これにより、すべての操作が成功するか、すべての操作が失敗（ロールバック）するかのいずれかになります：

```scala
// ユーザーを挿入し、そのユーザーに関連する情報も挿入する例
def createUserWithProfile(name: String, email: String, bio: String): DBIO[IO, Long] =
  for
    userId    <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".returning[Long]
    profileId <- sql"INSERT INTO user_profile (user_id, bio) VALUES ($userId, $bio)".returning[Long]
  yield userId
```

このメソッドを`.transaction`を使って実行することで、ユーザーの挿入とプロフィールの挿入が一つのトランザクションとして処理されます：

```scala
// トランザクション内で実行する
val userId = createUserWithProfile("julia", "julia@example.com", "プログラマー")
  .transaction(conn)
  .unsafeRunSync()

println(s"作成されたユーザーID: $userId")
```

もし`user_profile`テーブルへの挿入が失敗した場合、`user`テーブルへの挿入も自動的にロールバックされます。

## クエリ実行メソッドの選択

ldbcでは、データ更新操作用に以下のクエリ実行メソッドが用意されています：

- `.commit(conn)` - 自動コミットモードで書き込み操作を実行します（単一のシンプルな更新操作に適しています）
- `.rollback(conn)` - 書き込み操作を実行し、必ずロールバックします（テストや動作確認用）
- `.transaction(conn)` - トランザクション内で操作を実行し、成功時のみコミットします（複数の操作を一つの単位として扱いたい場合に適しています）

```scala
// 自動コミットモードでの実行（シンプルな単一操作）
updateUserEmail(1, "new@example.com").commit(conn)

// テスト用の実行（変更は保存されない）
insertUser("test", "test@example.com").rollback(conn)

// トランザクション内での複数操作（すべて成功するか、すべて失敗するか）
(for {
  userId <- insertUser("kate", "kate@example.com").returning[Long]
  _      <- sql"INSERT INTO user_roles (user_id, role_id) VALUES ($userId, 2)".update
} yield userId).transaction(conn)
```

## エラー処理

データ更新操作中にエラーが発生した場合の処理も重要です。ldbcでは`cats-effect`の`IO`モナドを使用してエラーを処理できます：

```scala
import cats.effect.unsafe.IORuntime
import cats.effect.IO

implicit val runtime: IORuntime = IORuntime.global

// エラー処理の例
def safeUpdateUser(id: Long, newEmail: String): Unit = {
  val result = updateUserEmail(id, newEmail)
    .commit(conn)
    .attempt // IOの結果をEither[Throwable, Int]に変換
    .unsafeRunSync()
    
  result match {
    case Right(count) => println(s"更新された行数: $count")
    case Left(error)  => println(s"エラーが発生しました: ${error.getMessage}")
  }
}

// 実行例（存在しないIDを指定して更新）
safeUpdateUser(9999, "nonexistent@example.com")
```

## まとめ

ldbcを使用したデータ更新操作では、以下のポイントを覚えておくと良いでしょう：

1. **挿入操作**には`.update`または自動生成キーを取得する`.returning[T]`を使用
2. **更新操作**には`.update`を使用し、WHERE句で対象行を指定
3. **削除操作**にも`.update`を使用（実際にはDELETE文だが、操作メソッドは共通）
4. **複数の操作**をアトミックに実行するには`.transaction()`を使用
5. 単純な**単一操作**には`.commit()`を使用
6. **テスト目的**では`.rollback()`を使用して変更を破棄

これらのデータ更新操作を適切に組み合わせることで、データベースを効率的かつ安全に操作できます。

## 次のステップ

これでデータベースにデータを挿入、更新、削除する方法が理解できました。ここまでで、ldbcの基本的な使い方をすべて学んできました。データベース接続、クエリ実行、データの読み書き、トランザクション管理など、日常的なデータベース操作に必要な知識を身につけました。

ここからは、より高度なトピックに進みます。まずは[エラーハンドリング](/ja/tutorial/Error-Handling.md)から始め、データベース操作で発生する可能性のある例外を適切に処理する方法を学びましょう。
