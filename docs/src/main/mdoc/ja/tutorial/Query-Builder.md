{%
  laika.title = クエリビルダー
  laika.metadata.language = ja
%}

# クエリビルダー

この章では、型安全にクエリを構築するための方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

```scala
//> using dep "@ORGANIZATION@::ldbc-query-builder:@VERSION@"
```

ldbcでは、クラスを使用してクエリを構築します。

```scala 3
import ldbc.query.builder.*

case class User(id: Int, name: String, email: String) derives Table
```

`User`クラスは`Table`トレイトを継承しています。`Table`トレイトは`Table`クラスを継承しているため、`Table`クラスのメソッドを使用してクエリを構築することができます。

```scala
val query = Table[User]
  .select(user => (user.id, user.name, user.email))
  .where(_.email === "alice@example.com")
```

## SELECT

型安全にSELECT文を構築する方法はTableが提供する`select`メソッドを使用することです。ldbcではプレーンなクエリに似せて実装されているため直感的にクエリ構築が行えます。またどのようなクエリが構築されているかも一目でわかるような作りになっています。

特定のカラムのみ取得を行うSELECT文を構築するには`select`メソッドで取得したいカラムを指定するだけです。

```scala
val select = Table[User].select(_.id)

select.statement === "SELECT id FROM user"
```

複数のカラムを指定する場合は`select`メソッドで取得したいカラムを指定して指定したカラムのタプルを返すだけです。

```scala
val select = Table[User].select(user => (user.id, user.name))

select.statement === "SELECT id, name FROM user"
```

全てのカラムを指定したい場合はTableが提供する`selectAll`メソッドを使用することで構築できます。

```scala
val select = Table[User].selectAll

select.statement === "SELECT id, name, email FROM user"
```

特定のカラムの数を取得したい場合は、指定したカラムで`count`を使用することで構築できます。　

```scala
val select = Table[User].select(_.id.count)

select.statement === "SELECT COUNT(id) FROM user"
```

### WHERE

クエリに型安全にWhere条件を設定する方法は`where`メソッドを使用することです。
    
```scala
val where = Table[User].selectAll.where(_.email === "alice@example.com")

where.statement === "SELECT id, name, email FROM user WHERE email = ?"
```

`where`メソッドで使用できる条件の一覧は以下です。

| 条件                                     | ステートメント                               |
|----------------------------------------|---------------------------------------|
| `===`                                  | `column = ?`                          |
| `>=`                                   | `column >= ?`                         |
| `>`                                    | `column > ?`                          |
| `<=`                                   | `column <= ?`                         |
| `<`                                    | `column < ?`                          |
| `<>`                                   | `column <> ?`                         |
| `!==`                                  | `column != ?`                         |
| `IS ("TRUE"/"FALSE"/"UNKNOWN"/"NULL")` | `column IS {TRUE/FALSE/UNKNOWN/NULL}` |
| `<=>`                                  | `column <=> ?`                        |
| `IN (value, value, ...)`               | `column IN (?, ?, ...)`               |
| `BETWEEN (start, end)`                 | `column BETWEEN ? AND ?`              |
| `LIKE (value)`                         | `column LIKE ?`                       |
| `LIKE_ESCAPE (like, escape)`           | `column LIKE ? ESCAPE ?`              |
| `REGEXP (value)`                       | `column REGEXP ?`                     |
| `<<` (value)                           | `column << ?`                         |
| `>>` (value)                           | `column >> ?`                         |
| `DIV (cond, result)`                   | `column DIV ? = ?`                    |
| `MOD (cond, result)`                   | `column MOD ? = ?`                    |
| `^ (value)`                            | `column ^ ?`                          |
| `~ (value)`                            | `~column = ?`                         |

### GROUP BY/Having

クエリに型安全にGROUP BY句を設定する方法は`groupBy`メソッドを使用することです。

`groupBy`を使用することで`select`でデータを取得する時に指定したカラム名の値を基準にグループ化することができます。

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .groupBy(_._2)

select.statement === "SELECT id, name FROM user GROUP BY name"
```

グループ化すると`select`で取得できるデータの数はグループの数だけとなります。そこでグループ化を行った場合には、グループ化に指定したカラムの値や、用意された関数を使ってカラムの値をグループ単位で集計した結果などを取得することができます。

`having`を使用すると`groupBy`によってグループ化されて取得したデータに関して、取得する条件を設定することができます。
    
```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .groupBy(_._2)
  .having(_._1 > 1)

select.statement === "SELECT id, name FROM user GROUP BY name HAVING id > ?"
```

### ORDER BY

クエリに型安全にORDER BY句を設定する方法は`orderBy`メソッドを使用することです。

`orderBy`を使用することで`select`でデータを取得する時に指定したカラム名の値を基準に昇順、降順で並び替えることができます。

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .orderBy(_.id)

select.statement === "SELECT id, name FROM user ORDER BY id"
```

昇順/降順を指定したい場合は、それぞれカラムに対して `asc`/`desc`を呼び出すだけです。

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .orderBy(_.id.asc)

select.statement === "SELECT id, name FROM user ORDER BY id ASC"
```

### LIMIT/OFFSET

クエリに型安全にLIMIT句とOFFSET句を設定する方法は`limit`/`offset`メソッドを使用することです。

`limit`を設定すると`select`を実行した時に取得するデータの行数の上限を設定することができ、`offset`を設定すると何番目からのデータを取得するのかを指定することができます。

```scala
val select = Table[User]
  .select(user => (user.id, user.name))
  .limit(1)
  .offset(1)
    
select.statement === "SELECT id, name FROM user LIMIT ? OFFSET ?"
```

## JOIN/LEFT JOIN/RIGHT JOIN

クエリに型安全にJoinを設定する方法は`join`/`leftJoin`/`rightJoin`メソッドを使用することです。

Joinでは以下定義をサンプルとして使用します。

```scala 3
case class User(id: Int, name: String, email: String) derives Table
case class Product(id: Int, name: String, price: BigDecimal) derives Table
case class Order(
  id:        Int,
  userId:    Int,
  productId: Int,
  orderDate: LocalDateTime,
  quantity:  Int
) derives Table

val userTable    = Table[User]
val productTable = Table[Product]
val orderTable   = Table[Order]
```

まずシンプルなJoinを行いたい場合は、`join`を使用します。
`join`の第一引数には結合したいテーブルを渡し、第二引数では結合元のテーブルと結合したいテーブルのカラムで比較を行う関数を渡します。これはJoinにおいてのON句に該当します。

Join後の`select`は2つのテーブルからカラムを指定することになります。
    
```scala
val join = userTable.join(orderTable)((user, order) => user.id === order.userId)
  .select((user, order) => (user.name, order.quantity))

join.statement = "SELECT user.`name`, order.`quantity` FROM user JOIN order ON user.id = order.user_id"
```

次に左外部結合であるLeft Joinを行いたい場合は、`leftJoin`を使用します。
`join`が`leftJoin`に変わっただけで実装自体はシンプルなJoinの時と同じになります。

```scala 3
val leftJoin = userTable.leftJoin(orderTable)((user, order) => user.id === order.userId)
  .select((user, order) => (user.name, order.quantity))

join.statement = "SELECT user.`name`, order.`quantity` FROM user LEFT JOIN order ON user.id = order.user_id"
```

シンプルなJoinとの違いは`leftJoin`を使用した場合、結合を行うテーブルから取得するレコードはNULLになる可能性があるということです。

そのためldbcでは`leftJoin`に渡されたテーブルから取得するカラムのレコードは全てOption型になります。

```scala 3
val leftJoin = userTable.leftJoin(orderTable)((user, order) => user.id === order.userId)
  .select((user, order) => (user.name, order.quantity)) // (String, Option[Int])
```

次に右外部結合であるRight Joinを行いたい場合は、`rightJoin`を使用します。
こちらも`join`が`rightJoin`に変わっただけで実装自体はシンプルなJoinの時と同じになります。

```scala 3
val rightJoin = orderTable.rightJoin(userTable)((order, user) => order.userId === user.id)
  .select((order, user) => (order.quantity, user.name))

join.statement = "SELECT order.`quantity`, user.`name` FROM order RIGHT JOIN user ON order.user_id = user.id"
```

シンプルなJoinとの違いは`rightJoin`を使用した場合、結合元のテーブルから取得するレコードはNULLになる可能性があるということです。

そのためldbcでは`rightJoin`を使用した結合元のテーブルから取得するカラムのレコードは全てOption型になります。

```scala 3
val rightJoin = orderTable.rightJoin(userTable)((order, user) => order.userId === user.id)
  .select((order, user) => (order.quantity, user.name)) // (Option[Int], String)
```

複数のJoinを行いたい場合は、メソッドチェーンで任意のJoinメソッドを呼ぶことで実現することができます。

```scala 3
val join = 
  (productTable join orderTable)((product, order) => product.id === order.productId)
    .rightJoin(userTable)((_, order, user) => order.userId === user.id)
    .select((product, order, user) => (product.name, order.quantity, user.name)) // (Option[String], Option[Int], String)]

join.statement =
  """
    |SELECT
    |  product.`name`, 
    |  order.`quantity`,
    |  user.`name`
    |FROM product
    |JOIN order ON product.id = order.product_id
    |RIGHT JOIN user ON order.user_id = user.id
    |""".stripMargin
```

複数のJoinを行っている状態で`rightJoin`での結合を行うと、今までの結合が何であったかにかかわらず直前まで結合していたテーブルから取得するレコードは全てNULL許容なアクセスとなることに注意してください。

## INSERT

型安全にINSERT文を構築する方法はTableが提供する以下のメソッドを使用することです。

- insert
- insertInto
- +=
- ++=

**insert**

`insert`メソッドには挿入するデータのタプルを渡します。タプルはモデルと同じプロパティの数と型である必要があります。また、挿入されるデータの順番はモデルのプロパティおよびテーブルのカラムと同じ順番である必要があります。

```scala 3
val insert = user.insert((1, "name", "email@example.com"))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)"
```

複数のデータを挿入したい場合は、`insert`メソッドに複数のタプルを渡すことで構築できます。

```scala 3
val insert = user.insert((1, "name 1", "email+1@example.com"), (2, "name 2", "email+2@example.com"))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)"
```

**insertInto**

`insert`メソッドはテーブルが持つ全てのカラムにデータ挿入を行いますが、特定のカラムに対してのみデータを挿入したい場合は`insertInto`メソッドを使用します。

これはAutoIncrementやDefault値を持つカラムへのデータ挿入を除外したい場合などに使用できます。

```scala 3
val insert = user.insertInto(user => (user.name, user.email)).values(("name 3", "email+3@example.com"))

insert.statement === "INSERT INTO user (`name`, `email`) VALUES(?, ?)"
```

複数のデータを挿入したい場合は、`values`にタプルの配列を渡すことで構築できます。

```scala 3
val insert = user.insertInto(user => (user.name, user.email)).values(List(("name 4", "email+4@example.com"), ("name 5", "email+5@example.com")))

insert.statement === "INSERT INTO user (`name`, `email`) VALUES(?, ?), (?, ?)"
```

**+=**

`+=`メソッドを使用することでモデルを使用してinsert文を構築することができます。モデルを使用する場合は全てのカラムにデータを挿入してしまうことに注意してください。

```scala 3
val insert = user += User(6, "name 6", "email+6@example.com")

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?)"
```

**++=**

モデルを使用して複数のデータを挿入したい場合は`++=`メソッドを使用します。

```scala 3
val insert = user ++= List(User(7, "name 7", "email+7@example.com"), User(8, "name 8", "email+8@example.com"))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?), (?, ?, ?)"
```

### ON DUPLICATE KEY UPDATE

ON DUPLICATE KEY UPDATE 句を指定し行を挿入すると、UNIQUEインデックスまたはPRIMARY KEYで値が重複する場合、古い行のUPDATEが発生します。

ldbcでこの処理を実現する方法は、`Insert`に対して`onDuplicateKeyUpdate`を使用することです。

```scala
val insert = user.insert((9, "name", "email+9@example.com")).onDuplicateKeyUpdate(v => (v.name, v.email))

insert.statement === "INSERT INTO user (`id`, `name`, `email`) VALUES(?, ?, ?) AS new_user ON DUPLICATE KEY UPDATE `name` = new_user.`name`, `email` = new_user.`email`"
```

## UPDATE

型安全にUPDATE文を構築する方法はTableが提供する`update`メソッドを使用することです。

`update`メソッドの第1引数にはテーブルのカラム名ではなくモデルのプロパティ名を指定し、第2引数に更新したい値を渡します。第2引数に渡す値の型は第1引数で指定したプロパティの型と同じである必要があります。

```scala
val update = user.update("name", "update name")

update.statement === "UPDATE user SET name = ?"
```

第1引数に存在しないプロパティ名を指定した場合コンパイルエラーとなります。

```scala 3
val update = user.update("hoge", "update name") // Compile error
```

複数のカラムを更新したい場合は`set`メソッドを使用します。

```scala 3
val update = user.update("name", "update name").set("email", "update-email@example.com")

update.statement === "UPDATE user SET name = ?, email = ?"
```

`set`メソッドには条件に応じてクエリを生成させないようにすることもできます。

```scala 3
val update = user.update("name", "update name").set("email", "update-email@example.com", false)

update.statement === "UPDATE user SET name = ?"
```

モデルを使用してupdate文を構築することもできます。モデルを使用する場合は全てのカラムを更新してしまうことに注意してください。

```scala 3
val update = user.update(User(1, "update name", "update-email@example.com"))

update.statement === "UPDATE user SET id = ?, name = ?, email = ?"
```

## DELETE

型安全にDELETE文を構築する方法はTableが提供する`delete`メソッドを使用することです。

```scala
val delete = user.delete

delete.statement === "DELETE FROM user"
```
