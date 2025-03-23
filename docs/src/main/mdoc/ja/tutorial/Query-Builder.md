{%
  laika.title = クエリビルダー
  laika.metadata.language = ja
%}

# クエリビルダー

[カスタムデータ型](/ja/tutorial/Custom-Data-Type.md)でldbcに独自の型を追加する方法を学びました。このページでは、SQLを直接書かずに型安全にクエリを構築する方法を説明します。

クエリビルダーは、SQLの文字列補間よりもさらに型安全な方法でデータベースクエリを構築するための機能です。これにより、コンパイル時により多くのエラーを検出でき、クエリの構造に関するミスを防ぐことができます。

この章では、型安全にクエリを構築するための方法について説明します。

## 準備

プロジェクトに以下の依存関係を設定する必要があります。

```scala
//> using dep "@ORGANIZATION@::ldbc-query-builder:@VERSION@"
```

## 基本的な使い方

ldbcでは、ケースクラスを使用してテーブルを表現し、クエリを構築します。まず、シンプルなテーブルの定義から始めましょう。

```scala 3
import ldbc.dsl.codec.Codec
import ldbc.query.builder.*

// テーブル定義
case class User(id: Int, name: String, email: String) derives Table
object User:
  gicen Codec[User] = Codec.derived[User]
```

`Table`トレイトは`derives`キーワードを使って自動的に導出します。これによって、クラスのプロパティがデータベースのカラムとして扱われます。

定義したテーブルに対してクエリを実行するには、`TableQuery`を使用します：

```scala
// テーブルに対するクエリを構築
val query = TableQuery[User]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")
```

上記のコードでは：
- `TableQuery[User]` - `User`テーブルに対するクエリを作成
- `select(...)` - 取得したいカラムを指定
- `*:` - 複数のカラムを結合するための演算子
- `where(...)` - クエリの条件を指定

## テーブル定義のカスタマイズ

### カラム名の変更

プロパティ名がデータベースのカラム名と異なる場合は、`@Column`アノテーションを使用して指定できます：

```scala 3
case class User(
  id: Int,
  @Column("full_name") name: String, // nameプロパティはfull_nameカラムにマッピング
  email: String
) derives Table
```

### テーブル名の変更

デフォルトでは、クラス名がテーブル名として使われますが、`Table.derived`を使用して明示的にテーブル名を指定することもできます：

```scala 3
case class User(id: Int, name: String, email: String)
object User:
  given Table[User] = Table.derived("users") // テーブル名を"users"に指定
```

## 基本的なクエリ操作

### SELECT

#### 基本的なSELECT

特定のカラムのみを取得したい場合：

```scala
val select = TableQuery[User].select(_.id)
// SELECT id FROM user
```

複数のカラムを取得したい場合は `*:` 演算子を使って指定します：

```scala
val select = TableQuery[User].select(user => user.id *: user.name)
// SELECT id, name FROM user
```

全てのカラムを取得したい場合：

```scala
val select = TableQuery[User].selectAll
// SELECT id, name, email FROM user
```

#### 集計関数

集計関数（例：count）を使用する方法：

```scala
val select = TableQuery[User].select(_.id.count)
// SELECT COUNT(id) FROM user
```

### WHERE条件

クエリに条件を追加するには`where`メソッドを使用します：

```scala
val where = TableQuery[User].selectAll.where(_.email === "alice@example.com")
// SELECT id, name, email FROM user WHERE email = ?
```

`where`メソッドで使用できる比較演算子の一覧：

| 演算子                                    | SQLステートメント                            | 説明                       |
|----------------------------------------|---------------------------------------|--------------------------|
| `===`                                  | `column = ?`                          | 等しい                      |
| `>=`                                   | `column >= ?`                         | 以上                       |
| `>`                                    | `column > ?`                          | より大きい                    |
| `<=`                                   | `column <= ?`                         | 以下                       |
| `<`                                    | `column < ?`                          | より小さい                    |
| `<>`                                   | `column <> ?`                         | 等しくない                    |
| `!==`                                  | `column != ?`                         | 等しくない（別の書き方）             |
| `IS ("TRUE"/"FALSE"/"UNKNOWN"/"NULL")` | `column IS {TRUE/FALSE/UNKNOWN/NULL}` | 指定した値かどうか                |
| `<=>`                                  | `column <=> ?`                        | NULL安全等価演算子（NULLとの比較が可能） |
| `IN (value, value, ...)`               | `column IN (?, ?, ...)`               | 指定した値のいずれかに一致するか         |
| `BETWEEN (start, end)`                 | `column BETWEEN ? AND ?`              | 指定した範囲内にあるか              |
| `LIKE (value)`                         | `column LIKE ?`                       | パターンマッチング                |
| `LIKE_ESCAPE (like, escape)`           | `column LIKE ? ESCAPE ?`              | エスケープ文字を指定したパターンマッチング    |
| `REGEXP (value)`                       | `column REGEXP ?`                     | 正規表現                     |
| `<<` (value)                           | `column << ?`                         | ビット左シフト                  |
| `>>` (value)                           | `column >> ?`                         | ビット右シフト                  |
| `DIV (cond, result)`                   | `column DIV ? = ?`                    | 整数除算                     |
| `MOD (cond, result)`                   | `column MOD ? = ?`                    | 剰余                       |
| `^ (value)`                            | `column ^ ?`                          | ビットXOR                   |
| `~ (value)`                            | `~column = ?`                         | ビットNOT                   |

条件を組み合わせる例：

```scala
val complexWhere = TableQuery[User]
  .selectAll
  .where(user => user.email === "alice@example.com" && user.id > 5)
// SELECT id, name, email FROM user WHERE email = ? AND id > ?
```

### GROUP BY と HAVING

データをグループ化するには`groupBy`メソッドを使用します：

```scala
val select = TableQuery[User]
  .select(user => user.id.count *: user.name)
  .groupBy(_.name)
// SELECT COUNT(id), name FROM user GROUP BY name
```

`having`を使用してグループ化されたデータに条件を設定できます：

```scala
val select = TableQuery[User]
  .select(user => user.id.count *: user.name)
  .groupBy(_.name)
  .having(_._1 > 1)
// SELECT COUNT(id), name FROM user GROUP BY name HAVING COUNT(id) > ?
```

### ORDER BY

結果の並び順を指定するには`orderBy`メソッドを使用します：

```scala
val select = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(_.id)
// SELECT id, name FROM user ORDER BY id
```

昇順・降順を指定する場合：

```scala
// 昇順の場合
val selectAsc = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(_.id.asc)
// SELECT id, name FROM user ORDER BY id ASC

// 降順の場合
val selectDesc = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(_.id.desc)
// SELECT id, name FROM user ORDER BY id DESC

// 複数カラムで並べ替え
val selectMultiple = TableQuery[User]
  .select(user => user.id *: user.name)
  .orderBy(user => user.name.asc *: user.id.desc)
// SELECT id, name FROM user ORDER BY name ASC, id DESC
```

### LIMIT と OFFSET

結果の行数を制限するには`limit`メソッドを、スキップする行数を指定するには`offset`メソッドを使用します：

```scala
val select = TableQuery[User]
  .select(user => user.id *: user.name)
  .limit(10)    // 最大10行を取得
  .offset(5)    // 最初の5行をスキップ
// SELECT id, name FROM user LIMIT ? OFFSET ?
```

## テーブル結合（JOIN）

複数のテーブルを結合するには、以下のような方法があります。まず、テーブル定義の例を示します：

```scala 3
// テーブル定義
case class User(id: Int, name: String, email: String) derives Table
case class Product(id: Int, name: String, price: BigDecimal) derives Table
case class Order(
  id:        Int,
  userId:    Int,
  productId: Int,
  orderDate: LocalDateTime,
  quantity:  Int
) derives Table

// TableQueryの生成
val userTable    = TableQuery[User]
val productTable = TableQuery[Product]
val orderTable   = TableQuery[Order]
```

### 内部結合（INNER JOIN）

二つのテーブルの一致する行のみを取得します：

```scala
val join = userTable
  .join(orderTable)
  .on((user, order) => user.id === order.userId)
  .select((user, order) => user.name *: order.quantity)
// SELECT user.`name`, order.`quantity` FROM user JOIN order ON user.id = order.user_id
```

### 左外部結合（LEFT JOIN）

左側のテーブルのすべての行と、右側のテーブルの一致する行を取得します。一致する行がない場合、右側のカラムはNULLになります：

```scala
val leftJoin = userTable
  .leftJoin(orderTable)
  .on((user, order) => user.id === order.userId)
  .select((user, order) => user.name *: order.quantity)
// SELECT user.`name`, order.`quantity` FROM user LEFT JOIN order ON user.id = order.user_id

// 返り値の型は (String, Option[Int]) になる
// orderテーブルからのデータがNULLになる可能性があるため
```

### 右外部結合（RIGHT JOIN）

右側のテーブルのすべての行と、左側のテーブルの一致する行を取得します。一致する行がない場合、左側のカラムはNULLになります：

```scala
val rightJoin = orderTable
  .rightJoin(userTable)
  .on((order, user) => order.userId === user.id)
  .select((order, user) => order.quantity *: user.name)
// SELECT order.`quantity`, user.`name` FROM order RIGHT JOIN user ON order.user_id = user.id

// 返り値の型は (Option[Int], String) になる
// orderテーブルからのデータがNULLになる可能性があるため
```

### 複数テーブルの結合

3つ以上のテーブルを結合することも可能です：

```scala
val multiJoin = productTable
  .join(orderTable).on((product, order) => product.id === order.productId)
  .rightJoin(userTable).on((_, order, user) => order.userId === user.id)
  .select((product, order, user) => product.name *: order.quantity *: user.name)
// SELECT 
//   product.`name`, 
//   order.`quantity`,
//   user.`name`
// FROM product
// JOIN order ON product.id = order.product_id
// RIGHT JOIN user ON order.user_id = user.id

// 返り値の型は (Option[String], Option[Int], String) になる
// rightJoinを使用しているため、product, orderテーブルからのデータがNULLになる可能性がある
```

## INSERT文

新しいレコードを挿入するには、いくつかの方法があります：

### `insert`メソッドを使用

値のタプルを直接指定します：

```scala
val insert = TableQuery[User].insert((1, "Alice", "alice@example.com"))
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)

// 複数のレコードを一度に挿入
val multiInsert = TableQuery[User].insert(
  (1, "Alice", "alice@example.com"),
  (2, "Bob", "bob@example.com")
)
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)
```

### `insertInto`メソッドを使用

特定のカラムにのみ値を挿入する場合に便利です（例えばAUTO INCREMENTを使用している場合など）：

```scala
val insert = TableQuery[User]
  .insertInto(user => user.name *: user.email)
  .values(("Alice", "alice@example.com"))
// INSERT INTO user (`name`, `email`) VALUES(?, ?)

// 複数のレコードを一度に挿入
val multiInsert = TableQuery[User]
  .insertInto(user => user.name *: user.email)
  .values(List(
    ("Alice", "alice@example.com"),
    ("Bob", "bob@example.com")
  ))
// INSERT INTO user (`name`, `email`) VALUES(?, ?), (?, ?)
```

### モデルオブジェクトを使用（`+=`と`++=`演算子）

```scala
// 1件挿入
val insert = TableQuery[User] += User(1, "Alice", "alice@example.com")
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)

// 複数件挿入
val multiInsert = TableQuery[User] ++= List(
  User(1, "Alice", "alice@example.com"),
  User(2, "Bob", "bob@example.com")
)
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)
```

### SELECT結果を使用したINSERT

SELECTの結果を別のテーブルに挿入することもできます：

```scala
val insertSelect = TableQuery[User]
  .insertInto(user => user.id *: user.name *: user.email)
  .select(
    TableQuery[User]
      .select(user => user.id *: user.name *: user.email)
      .where(_.id > 10)
  )
// INSERT INTO user (`id`, `name`, `email`) 
// SELECT id, name, email FROM user WHERE id > ?
```

### ON DUPLICATE KEY UPDATE

ユニークキーまたはプライマリーキーが重複した場合に既存の行を更新するON DUPLICATE KEY UPDATE句の使用：

```scala
val insert = TableQuery[User]
  .insert((9, "Alice", "alice@example.com"))
  .onDuplicateKeyUpdate(v => v.name *: v.email)
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?) 
// AS new_user ON DUPLICATE KEY UPDATE `name` = new_user.`name`, `email` = new_user.`email`

// 特定のカラムのみを更新する場合
val insertWithSpecificUpdate = TableQuery[User]
  .insert((9, "Alice", "alice@example.com"))
  .onDuplicateKeyUpdate(_.name, "UpdatedName")
// INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?) 
// AS new_user ON DUPLICATE KEY UPDATE `name` = ?
```

## UPDATE文

既存のレコードを更新するには：

### 単一カラムの更新

```scala
val update = TableQuery[User].update(_.name)("UpdatedName")
// UPDATE user SET name = ?
```

### 複数カラムの更新

```scala
val update = TableQuery[User]
  .update(u => u.name *: u.email)(("UpdatedName", "updated-email@example.com"))
// UPDATE user SET name = ?, email = ?
```

### 条件付き更新（特定のカラムの更新を条件によってスキップ）

```scala
val shouldUpdate = true // または false
val update = TableQuery[User]
  .update(_.name)("UpdatedName")
  .set(_.email, "updated-email@example.com", shouldUpdate)
// shouldUpdate が true の場合: UPDATE user SET name = ?, email = ?
// shouldUpdate が false の場合: UPDATE user SET name = ?
```

### モデルオブジェクトを使用した更新

```scala
val update = TableQuery[User].update(User(1, "UpdatedName", "updated-email@example.com"))
// UPDATE user SET id = ?, name = ?, email = ?
```

### WHERE条件付き更新

```scala
val update = TableQuery[User]
  .update(_.name, "UpdatedName")
  .where(_.id === 1)
// UPDATE user SET name = ? WHERE id = ?

// AND条件の追加
val updateWithMultipleConditions = TableQuery[User]
  .update(_.name, "UpdatedName")
  .where(_.id === 1)
  .and(_.email === "alice@example.com")
// UPDATE user SET name = ? WHERE id = ? AND email = ?

// OR条件の追加
val updateWithOrCondition = TableQuery[User]
  .update(_.name, "UpdatedName")
  .where(_.id === 1)
  .or(_.id === 2)
// UPDATE user SET name = ? WHERE id = ? OR id = ?
```

## DELETE文

レコードを削除するには：

### 基本的なDELETE

```scala
val delete = TableQuery[User].delete
// DELETE FROM user
```

### WHERE条件付き削除

```scala
val delete = TableQuery[User]
  .delete
  .where(_.id === 1)
// DELETE FROM user WHERE id = ?

// AND/OR条件の追加
val deleteWithMultipleConditions = TableQuery[User]
  .delete
  .where(_.id === 1)
  .and(_.email === "alice@example.com")
// DELETE FROM user WHERE id = ? AND email = ?
```

### LIMIT付き削除

特定の数のレコードのみを削除するには：

```scala
val delete = TableQuery[User]
  .delete
  .where(_.id > 10)
  .limit(5)
// DELETE FROM user WHERE id > ? LIMIT ?
```

## 高度なクエリ例

### サブクエリ

サブクエリを使用した例：

```scala
val subQuery = TableQuery[Order]
  .select(order => order.userId)
  .where(_.quantity > 10)

val mainQuery = TableQuery[User]
  .select(user => user.name)
  .where(_.id IN subQuery)
// SELECT name FROM user WHERE id IN (SELECT user_id FROM order WHERE quantity > ?)
```

### 複雑な結合と条件

```scala
val complexQuery = userTable
  .join(orderTable).on((user, order) => user.id === order.userId)
  .join(productTable).on((_, order, product) => order.productId === product.id)
  .select((user, order, product) => user.name *: product.name *: order.quantity)
  .where { case ((user, order, product)) => 
    (user.name LIKE "A%") && (product.price > 100) 
  }
  .orderBy { case ((_, _, product)) => product.price.desc }
  .limit(10)
// SELECT 
//   user.`name`, 
//   product.`name`, 
//   order.`quantity` 
// FROM user 
// JOIN order ON user.id = order.user_id 
// JOIN product ON order.product_id = product.id 
// WHERE user.name LIKE ? AND product.price > ? 
// ORDER BY product.price DESC 
// LIMIT ?
```

### 条件分岐を含むクエリ

実行時の条件によってクエリを変えたい場合：

```scala
val nameOption: Option[String] = Some("Alice") // または None
val minIdOption: Option[Int] = Some(5) // または None

val query = TableQuery[User]
  .selectAll
  .whereOpt(user => nameOption.map(name => user.name === name))
  .andOpt(user => minIdOption.map(minId => user.id >= minId))
// nameOption と minIdOption が Some の場合:
// SELECT id, name, email FROM user WHERE name = ? AND id >= ?
// nameOption が None で minIdOption が Some の場合:
// SELECT id, name, email FROM user WHERE id >= ?
// 両方が None の場合:
// SELECT id, name, email FROM user
```

## クエリの実行

構築したクエリは次のように実行します：

```scala 3
import ldbc.dsl.*

provider.use { conn =>
  (for
    // SELECTクエリの実行
    users <- TableQuery[User].selectAll.where(_.id > 5).query.to[List]  // Listとして結果を取得
    // 単一の結果を取得
    user <- TableQuery[User].selectAll.where(_.id === 1).query.to[Option]
    // 更新系クエリの実行
    _ <- TableQuery[User].update(_.name)("NewName").where(_.id === 1).update
  yield ???).transaction(conn)
}
```

## 次のステップ

これでクエリビルダーを使って型安全にクエリを構築する方法がわかりました。この方法を使うと、SQLを直接書くよりも多くのエラーをコンパイル時に検出でき、より安全なコードを書くことができます。

次は[スキーマ](/ja/tutorial/Schema.md)に進み、Scalaコードでデータベーススキーマを定義する方法を学びましょう。
