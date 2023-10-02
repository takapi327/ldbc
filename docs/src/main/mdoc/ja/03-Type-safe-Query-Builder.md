# 型安全なクエリ構築

この章では、LDBCで構築したテーブル定義を使用して、型安全にクエリを構築するための方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-query-builder" % "$version$"
```
@@@

LDBCでのテーブル定義方法をまだ読んでいない場合先に[テーブル定義](http://localhost:4000/ja/01-Table-Definitions.html)の章を先に読むことをオススメしましす。

以下のコード例では、以下のimportを想定しています。

```scala 3
import cats.effect.IO
import ldbc.core.*
import ldbc.query.builder.TableQuery
```

LDBCではTableQueryにテーブル定義を渡すことで型安全なクエリ構築を行います。

```scala 3
case class User(
  id: Long,
  name: String,
  age: Option[Int],
)

val table = Table[User]("user")(
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
)

val userQuery = TableQuery[IO, User](table)
```

## SELECT

型安全にSELECT文を構築する方法はTableQueryが提供する`select`メソッドを使用することです。LDBCではプレーンなクエリに似せて実装されているため直感的にクエリ構築が行えます。またどのようなクエリが構築されているかも一目でわかるような作りになっています。

特定のカラムのみ取得を行うSELECT文を構築するには`select`メソッドで取得したいカラムを指定するだけです。

```scala 3
val select = userQuery.select(_.id)

select.statement === "SELECT `id` FROM user"
```

複数のカラムを指定する場合は`select`メソッドで取得したいカラムを指定して指定したカラムのタプルを返すだけです。

```scala 3
val select = userQuery.select(user => (user.id, user.name))

select.statement === "SELECT `id`, `name` FROM user"
```

全てのカラムを指定したい場合はTableQueryが提供する`selectAll`メソッドを使用することで構築できます。

```scala 3
val select = userQuery.selectAll

select.statement === "SELECT `id`, `name`, `age` FROM user"
```

特定のカラムの数を取得したい場合は、指定したカラムで`count`を使用することで構築できます。　

```scala 3
val select = userQuery.select(_.id.count)

select.statement === "SELECT COUNT(id) FROM user"
```

### WHERE

クエリに型安全にWhere条件を設定する方法は`where`メソッドを使用することです。

```scala 3
val select = userQuery.select(_.id).where(_.name === "Test")

select.statement === "SELECT `id` FROM user WHERE name = ?"
```

`where`メソッドで使用できる条件の一覧は以下です。

| 条件                                   | ステートメント                               |
|--------------------------------------|---------------------------------------|
| ===                                  | `column = ?`                          |
| >=                                   | `column >= ?`                         |
| >                                    | `column > ?`                          |
| <=                                   | `column <= ?`                         |
| <                                    | `column < ?`                          |
| <>                                   | `column <> ?`                         |
| !==                                  | `column != ?`                         |
| IS ("TRUE"/"FALSE"/"UNKNOWN"/"NULL") | `column IS {TRUE/FALSE/UNKNOWN/NULL}` |
| <=>                                  | `column <=> ?`                        |
| IN (value, value, ...)               | `column IN (?, ?, ...)`               |
| BETWEEN (start, end)                 | `column BETWEEN ? AND ?`              |
| LIKE (value)                         | `column LIKE ?`                       |
| LIKE_ESCAPE (like, escape)           | `column LIKE ? ESCAPE ?`              |
| REGEXP (value)                       | `column REGEXP ?`                     |
| `<<` (value)                         | `column << ?`                         |
| `>>` (value)                         | `column >> ?`                         |
| DIV (cond, result)                   | `column DIV ? = ?`                    |
| MOD (cond, result)                   | `column MOD ? = ?`                    |
| ^ (value)                            | `column ^ ?`                          |
| ~ (value)                            | `~column = ?`                         |

## INSERT

Coming soon...

## UPDATE

型安全にSELECT文を構築する方法はTableQueryが提供する`update`メソッドを使用することです。

`update`メソッドの第1引数にはテーブルのカラム名ではなくモデルのプロパティ名を指定し、第2引数に更新したい値を渡します。第2引数に渡す値の型は第1引数で指定したプロパティの型と同じである必要があります。

```scala 3
val update = userQuery.update("name", "update name")

update.statement === "UPDATE user SET name = ?"
```

第1引数に存在しないプロパティ名を指定した場合コンパイルエラーとなります。

```scala 3
val update = userQuery.update("hoge", "update name") // Compile error
```

複数のカラムを更新したい場合は`set`メソッドを使用します。

```scala 3
val update = userQuery.update("name", "update name").set("age", Some(20))

update.statement === "UPDATE user SET name = ?, age = ?"
```

`set`メソッドには条件に応じてクエリを生成させないようにすることもできます。

```scala 3
val update = userQuery.update("name", "update name").set("age", Some(20), false)

update.statement === "UPDATE user SET name = ?"
```

モデルを使用してupdate文を構築することもできます。モデルを使用する場合は全てのカラムを更新してしまうことに注意してください。

```scala 3
val update = userQuery.update(User(1L, "update name", None))

update.statement === "UPDATE user SET id = ?, name = ?, age = ?"
```

### WHERE

`where`メソッドを使用することでupdate文にもWhere条件を設定することができます。

```scala 3
val update = userQuery.update("name", "update name").set("age", Some(20)).where(_.id === 1)

update.statement === "UPDATE user SET name = ?, age = ? WHERE id = ?"
```

`where`メソッドで使用できる条件はInsert文の[where項目](http://localhost:4000/ja/03-Type-safe-Query-Builder.html#where)を参照してください。

## DELETE

型安全にSELECT文を構築する方法はTableQueryが提供する`delete`メソッドを使用することです。

```scala 3
val delete = userQuery.delete

delete.statement === "DELETE FROM user"
```

### WHERE

`where`メソッドを使用することでdelete文にもWhere条件を設定することができます。

```scala 3
val delete = userQuery.delete.where(_.id === 1)

delete.statement === "DELETE FROM user WHERE id = ?"
```

`where`メソッドで使用できる条件はInsert文の[where項目](http://localhost:4000/ja/03-Type-safe-Query-Builder.html#where)を参照してください。
