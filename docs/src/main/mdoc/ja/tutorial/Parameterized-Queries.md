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
2. **静的パラメータ** - SQL文の一部として直接埋め込まれる（例：テーブル名、カラム名など）

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

コネクションを使用してクエリを実行すると問題なく動作します。

```scala
provider.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
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

provider.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id AND email > $email"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
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

provider.use { conn =>
  (sql"SELECT name, email FROM user WHERE " ++ in("id", ids))
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
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

## 静的なパラメーター

カラム名やテーブル名など、SQL文の構造的な部分をパラメータ化したい場合があります。このような場合、値をそのままSQL文に埋め込む「静的パラメータ」を使用します。

動的パラメータ（通常の`$value`）は`PreparedStatement`によって処理され、クエリ文字列では`?`に置き換えられますが、静的パラメータは文字列として直接埋め込まれます。

静的パラメータを使用するには、`sc`関数を使用します：

```scala
val column = "name"
val table = "user"

// 動的パラメータとして扱うと「SELECT ? FROM user」となってしまう
// sql"SELECT $column FROM user".query[String].to[List]

// 静的パラメータとして扱うと「SELECT name FROM user」となる
sql"SELECT ${sc(column)} FROM ${sc(table)}".query[String].to[List]
```

この例では、生成されるSQLは`SELECT name FROM user`です。

> **警告**: `sc(...)`は渡された文字列のエスケープを行わないため、ユーザー入力などの検証されていないデータをそのまま渡すと、SQLインジェクション攻撃のリスクがあります。静的パラメータはアプリケーションの安全な部分（定数や設定など）からのみ使用してください。

静的パラメータの一般的な使用例：

```scala
// 動的なソート順
val sortColumn = "created_at" 
val sortDirection = "DESC"

sql"SELECT * FROM user ORDER BY ${sc(sortColumn)} ${sc(sortDirection)}"

// 動的なテーブル選択
val schema = "public"
val table = "user"

sql"SELECT * FROM ${sc(schema)}.${sc(table)}"
```

## 次のステップ

これでパラメータ化されたクエリの使い方を理解できました。パラメータを扱えるようになると、より複雑で実用的なデータベースクエリを構築できるようになります。

次は[データ選択](/ja/tutorial/Selecting-Data.md)に進み、データをさまざまな形式で取得する方法を学びましょう。
