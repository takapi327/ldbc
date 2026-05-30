{%
  laika.title = パラメータ
  laika.metadata.language = ja
%}

# パラメータ化されたクエリ

[シンプルプログラム](/ja/tutorial/Simple-Program.md)では基本的なクエリの実行方法を学びました。実際のアプリケーションでは、ユーザー入力や変数の値に基づいてクエリを実行することが多くなります。このページでは、安全にパラメータを扱う方法を学びます。

ldbcでは、SQLインジェクション攻撃を防ぐために、パラメータ化されたクエリを使用することを強く推奨しています。パラメータ化されたクエリを使うと、SQLコードとデータを分離し、より安全なデータベースアクセスが可能になります。

## パラメータの基本

ldbcでは、SQL文にパラメータを埋め込むための2つの主要な方法があります：

1. **動的パラメータ** - 通常のパラメータとして使用され、SQLインジェクション攻撃を防ぐために`PreparedStatement`によって処理される
2. **識別子のエスケープ** - テーブル名・カラム名など識別子をバッククォートで安全に埋め込む（`ident` 関数を使用）

## 動的パラメータの追加

まずは、パラメーターを持たないクエリを作成します。

```scala
sql"SELECT name, email FROM user".query[(String, String)].to[List]
```

次にクエリをメソッドに組み込んで、ユーザーが指定する`id`と一致するデータのみを選択するパラメーターを追加してみましょう。文字列の補間を行うのと同じように、`id`引数を`$id`としてSQL文に挿入します。

```scala
val id = 1

sql"SELECT name, email FROM user WHERE id = $id".query[(String, String)].to[List]
```

Connectorを使用してクエリを実行すると問題なく動作します。

```scala
import ldbc.connector.*

// Connectorを作成
val connector = Connector.fromDataSource(datasource)

sql"SELECT name, email FROM user WHERE id = $id"
  .query[(String, String)]
  .to[List]
  .readOnly(connector)
```

ここでは何が起こっているのでしょうか？文字列リテラルをSQL文字列にドロップしているだけのように見えますが、実際には`PreparedStatement`を構築しており、`id`値は最終的に`setInt`の呼び出しによって設定されます。これにより、SQLインジェクション攻撃からアプリケーションを守ることができます。

さまざまな型のパラメータを使用できます：

```scala
val id: Int = 1
val name: String = "Alice"
val active: Boolean = true
val createdAt: LocalDateTime = LocalDateTime.now()

sql"INSERT INTO user (id, name, active, created_at) VALUES ($id, $name, $active, $createdAt)"
```

ldbcでは、各型に対して適切なエンコーダーが用意されており、Scala/Javaの値をSQLの値に安全に変換します。

## 複数のパラメータ

複数のパラメータも同様に使用できます。

```scala
val id = 1
val email = "alice@example.com"

// Connectorを使用して実行
  sql"SELECT name, email FROM user WHERE id = $id AND email > $email"
    .query[(String, String)]
    .to[List]
    .readOnly(connector)
```

## クエリーの結合

大きなクエリを構築する場合は、複数のSQLフラグメントを結合することができます。

```scala
val baseQuery = sql"SELECT name, email FROM user"
val whereClause = sql"WHERE id > $id"
val orderClause = sql"ORDER BY name ASC"

val query = baseQuery ++ whereClause ++ orderClause
```

## SQLヘルパー関数

ldbcは、複雑なSQL句を簡単に構築するためのヘルパー関数を多数提供しています。

### IN句の扱い

SQLでよくある課題は、一連の値をIN句で使用したい場合です。ldbcでは`in`関数を使って簡単に実装できます。

```scala
val ids = NonEmptyList.of(1, 2, 3)

// Connectorを使用して実行
  (sql"SELECT name, email FROM user WHERE " ++ in("id", ids))
    .query[(String, String)]
    .to[List]
    .readOnly(connector)
```

これは以下のSQLに相当します：

```sql
SELECT name, email FROM user WHERE (id IN (?, ?, ?))
```

`ids`は`NonEmptyList`である必要があることに注意してください。これはIN句が空であってはならないためです。

### その他のヘルパー関数

ldbcでは他にも多くの便利な関数を提供しています：

#### VALUES句の生成

```scala
val users = NonEmptyList.of(
  (1, "Alice", "alice@example.com"),
  (2, "Bob", "bob@example.com")
)

(sql"INSERT INTO user (id, name, email) " ++ values(users))
```

#### WHERE句の条件

AND条件とOR条件を簡単に構築できます：

```scala
val activeFilter = sql"active = true"
val nameFilter = sql"name LIKE ${"A%"}"
val emailFilter = sql"email IS NOT NULL"

// WHERE (active = true) AND (name LIKE 'A%') AND (email IS NOT NULL)
val query1 = sql"SELECT * FROM user " ++ whereAnd(activeFilter, nameFilter, emailFilter)

// WHERE (active = true) OR (name LIKE 'A%')
val query2 = sql"SELECT * FROM user " ++ whereOr(activeFilter, nameFilter)
```

#### SET句の生成

UPDATE文のSET句を簡単に生成できます：

```scala
val name = "New Name"
val email = "new@example.com"

val updateValues = set(
  sql"name = $name",
  sql"email = $email",
  sql"updated_at = NOW()"
)

sql"UPDATE user " ++ updateValues ++ sql" WHERE id = 1"
```

#### ORDER BY句の生成

```scala
val query = sql"SELECT * FROM user " ++ orderBy(sql"name ASC", sql"created_at DESC")
```

### オプションの条件

条件がオプションの場合（存在しない可能性がある場合）は、`Opt`サフィックスが付いた関数を使用できます：

```scala
val nameOpt: Option[String] = Some("Alice")
val emailOpt: Option[String] = None

val nameFilter = nameOpt.map(name => sql"name = $name")
val emailFilter = emailOpt.map(email => sql"email = $email")

// nameFilterはSome(...)、emailFilterはNoneなので、WHERE句には「name = ?」のみが含まれます
val query = sql"SELECT * FROM user " ++ whereAndOpt(nameFilter, emailFilter)
```

## 識別子のエスケープ

カラム名やテーブル名など、SQL文の構造的な部分をパラメータ化したい場合があります。このような場合、`ident` 関数を使用します。

動的パラメータ（通常の`$value`）は`PreparedStatement`によって処理され、クエリ文字列では`?`に置き換えられますが、`ident` は識別子をバッククォートで囲んでSQL文に直接埋め込みます。NUL文字の除去も行うため、識別子を安全に扱えます。

```scala
val column = "name"
val table = "user"

// 動的パラメータとして扱うと「SELECT ? FROM user」となってしまう
// sql"SELECT $column FROM user".query[String].to[List]

// ident を使うと「SELECT `name` FROM `user`」となる
sql"SELECT ${ident(column)} FROM ${ident(table)}".query[String].to[List]
```

`ident` の一般的な使用例：

```scala
// 動的なカラム選択
val sortColumn = "created_at"

sql"SELECT * FROM user ORDER BY ${ident(sortColumn)} DESC"

// 動的なテーブル選択
val schema = "public"
val table = "user"

sql"SELECT * FROM ${ident(schema)}.${ident(table)}"
```

> **注意**: `ident` はバッククォートでエスケープを行いますが、信頼できる値（定数・設定値など）に対して使用することを推奨します。ユーザー入力をそのまま識別子として使用する設計は避けてください。

## 条件付きSQL断片

条件に応じてSQL断片を付加したい場合は、`when` 関数を使用します。

```scala
val limit: Option[Int] = Some(10)

sql"SELECT name, email FROM user" ++ when(limit.isDefined)(sql" LIMIT ${limit.get}")
```

`when(condition)(fragment)` は `condition` が `true` のときだけ `fragment` を追加します。`false` のときは空の断片になります。

複数の条件を組み合わせることもできます：

```scala
val nameFilter: Option[String] = Some("Alice")
val activeOnly: Boolean = true

val query =
  sql"SELECT * FROM user" ++
  when(nameFilter.isDefined)(sql" WHERE name = ${nameFilter.get}") ++
  when(activeOnly)(sql" AND active = true")
```

## ページネーション

一覧取得でよく使われる `LIMIT` / `OFFSET` を簡潔に書くには、`paginate` 関数を使用します。

```scala
// limit と offset を両方指定
sql"SELECT name, email FROM user " ++ paginate(limit = 20, offset = 40)
// → SELECT name, email FROM user LIMIT ? OFFSET ?

// limit のみ指定
sql"SELECT name, email FROM user " ++ paginate(limit = 20)
// → SELECT name, email FROM user LIMIT ?
```

ページ番号からオフセットを計算する場合の例：

```scala
val pageSize = 20
val page     = 3  // 1始まり

sql"SELECT name, email FROM user ORDER BY id " ++ paginate(limit = pageSize, offset = (page - 1) * pageSize)
```

> **注意**: `limit` または `offset` に負の値を渡すと `IllegalArgumentException` がスローされます。

## 次のステップ

これでパラメータ化されたクエリの使い方を理解できました。パラメータを扱えるようになると、より複雑で実用的なデータベースクエリを構築できるようになります。

次は[データ選択](/ja/tutorial/Selecting-Data.md)に進み、データをさまざまな形式で取得する方法を学びましょう。
