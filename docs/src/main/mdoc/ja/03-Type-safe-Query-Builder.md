# 型安全なクエリ構築

この章では、LDBCで構築したテーブル定義を使用して、型安全にクエリを構築するための方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-query-builder" % "$version$"
```
@@@

LDBCでのテーブル定義方法をまだ読んでいない場合は、[テーブル定義](http://localhost:4000/ja/01-Table-Definitions.html)の章を先に読むことをオススメしましす。

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

### GROUP BY/Having

クエリに型安全にGROUP BY句を設定する方法は`groupBy`メソッドを使用することです。

`groupBy`を使用することで`select`でデータを取得する時に指定したカラム名の値を基準にグループ化することができます。

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).groupBy(_._3)

select.statement === "SELECT `id`, `name`, `age` FROM user GROUP BY age"
```

グループ化すると`select`で取得できるデータの数はグループの数だけとなります。そこでグループ化を行った場合には、グループ化に指定したカラムの値や、用意された関数を使ってカラムの値をグループ単位で集計した結果などを取得することができます。

`having`を使用すると`groupBy`によってグループ化されて取得したデータに関して、取得する条件を設定することができます。

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).groupBy(_._3).having(_._3 > 20)

select.statement === "SELECT `id`, `name`, `age` FROM user GROUP BY age HAVING age > ?"
```

### ORDER BY

クエリに型安全にORDER BY句を設定する方法は`orderBy`メソッドを使用することです。

`orderBy`を使うことで`select`でデータを取得する時に指定したカラムの値を対象にソートした結果を取得することができます。

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).orderBy(_.age)

select.statement === "SELECT `id`, `name`, `age` FROM user ORDER BY age"
```

昇順/降順を指定したい場合は、それぞれカラムに対して `asc`/`desc`を呼び出すだけです。

```scala 3
val desc = userQuery.select(user => (user.id, user.name, user.age)).orderBy(_.age.desc)

desc.statement === "SELECT `id`, `name`, `age` FROM user ORDER BY age DESC"

val asc = userQuery.select(user => (user.id, user.name, user.age)).orderBy(_.age.asc)

asc.statement === "SELECT `id`, `name`, `age` FROM user ORDER BY age ASC"
```

### LIMIT/OFFSET

クエリに型安全にLIMIT句とOFFSET句を設定する方法は`limit`/`offset`メソッドを使用することです。

`limit`を設定すると`select`を実行した時に取得するデータの行数の上限を設定することができ、`offset`を設定すると何番目からのデータを取得するのかを指定することができます。

```scala 3
val select = userQuery.select(user => (user.id, user.name, user.age)).limit(100).offset(50)

select.statement === "SELECT `id`, `name`, `age` FROM user LIMIT ? OFFSET ?"
```

## JOIN/LEFT JOIN/RIGHT JOIN

クエリに型安全にJoinを設定する方法は`join`/`leftJoin`/`rightJoin`メソッドを使用することです。

Joinでは以下定義をサンプルとして使用します。

```scala 3
case class Country(code: String, name: String)
object Country:
  val table = Table[Country]("country")(
    column("code", CHAR(3), PRIMARY_KEY),
    column("name", VARCHAR(255))
  )

case class City(id: Long, name: String, countryCode: String)
object City:
  val table = Table[City]("city")(
    column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
    column("name", VARCHAR(255)),
    column("country_code", CHAR(3))
  )

case class CountryLanguage(
  countryCode: String,
  language:    String
)
object CountryLanguage:
  val table: Table[CountryLanguage] = Table[CountryLanguage]("country_language")(
    column("country_code", CHAR(3)),
    column("language", CHAR(30))
  )

val countryQuery = TableQuery[IO, Country](Country.table)
val cityQuery = TableQuery[IO, City](City.table)
val countryLanguageQuery = TableQuery[IO, CountryLanguage](CountryLanguage.table)
```

まずシンプルなJoinを行いたい場合は、`join`を使用します。
`join`の第一引数には結合したいテーブルを渡し、第二引数では結合元のテーブルと結合したいテーブルのカラムで比較を行う関数を渡します。これはJoinにおいてのON句に該当します。

Join後の`select`は2つのテーブルからカラムを指定することになります。

```scala 3
val join = countryQuery.join(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country JOIN city ON country.code = city.country_code"
```

次に左外部結合であるLeft Joinを行いたい場合は、`leftJoin`を使用します。
`join`が`leftJoin`に変わっただけで実装自体はシンプルなJoinの時と同じになります。

```scala 3
val leftJoin = countryQuery.leftJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country LEFT JOIN city ON country.code = city.country_code"
```

シンプルなJoinとの違いは`leftJoin`を使用した場合、結合を行うテーブルから取得するレコードはNULLになる可能性があるということです。

そのためLDBCでは`leftJoin`に渡されたテーブルから取得するカラムのレコードは全てOption型になります。

```scala 3
val leftJoin = countryQuery.leftJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name)) // (String, Option[String])
```

次に右外部結合であるRight Joinを行いたい場合は、`rightJoin`を使用します。
こちらも`join`が`rightJoin`に変わっただけで実装自体はシンプルなJoinの時と同じになります。

```scala 3
val rightJoin = countryQuery.rightJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country RIGHT JOIN city ON country.code = city.country_code"
```

シンプルなJoinとの違いは`rightJoin`を使用した場合、結合元のテーブルから取得するレコードはNULLになる可能性があるということです。

そのためLDBCでは`rightJoin`を使用した結合元のテーブルから取得するカラムのレコードは全てOption型になります。

```scala 3
val rightJoin = countryQuery.rightJoin(cityQuery)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name)) // (Option[String], String)
```

複数のJoinを行いたい場合は、メソッドチェーンで任意のJoinメソッドを呼ぶことで実現することができます。

```scala 3
val join = 
  (countryQuery join cityQuery)((country, city) => country.code === city.countryCode)
    .rightJoin(countryLanguageQuery)((_, city, countryLanguage) => city.countryCode === countryLanguage.countryCode)
    .select((country, city, countryLanguage) => (country.name, city.name, countryLanguage.language)) // (Option[String], Option[String], String)]

join.statement =
  """
    |SELECT
    |  country.`name`, 
    |  city.`name`,
    |  country_language.`language`
    |FROM country
    |JOIN city ON country.code = city.country_code
    |RIGHT JOIN country_language ON city.country_code = country_language.country_code
    |""".stripMargin
```

複数のJoinを行っている状態で`rightJoin`での結合を行うと、今までの結合が何であったかにかかわらず直前まで結合していたテーブルから取得するレコードは全てNULL許容なアクセスとなることに注意してください。

## Custom Data Type

前章でユーザー独自の型もしくはサポートされていない型を使用するためにDataTypeの`mapping`メソッドを使用して独自の型とDataTypeのマッピングを行ないました。([参照](http://localhost:4000/ja/02-Custom-Data-Type.html))

LDBCはテーブル定義とデータベースへの接続処理が分離されています。
そのためデータベースからデータを取得する際にユーザー独自の型もしくはサポートされていない型に変換したい場合は、ResultSetからのデータ取得方法を独自の型もしくはサポートされていない型と紐付けてあげる必要があります。

例えばユーザー定義のEnumを文字列型とマッピングしたい場合は、以下のようになります。

```scala 3
enum Custom:
  case ...

given ResultSetReader[IO, Custom] =
  ResultSetReader.mapping[IO, str, Custom](str => Custom.valueOf(str))
```

※ この処理は将来のバージョンでDataTypeのマッピングと統合される可能性があります。

## INSERT

型安全にINSERT文を構築する方法はTableQueryが提供する以下のメソッドを使用することです。

- insert
- insertInto
- +=
- ++=

**insert**

`insert`メソッドには挿入するデータのタプルを渡します。タプルはモデルと同じプロパティの数と型である必要があります。また、挿入されるデータの順番はモデルのプロパティおよびテーブルのカラムと同じ順番である必要があります。

```scala 3
val insert = userQuery.insert((1L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?)"
```

複数のデータを挿入したい場合は、`insert`メソッドに複数のタプルを渡すことで構築できます。

```scala 3
val insert = userQuery.insert((1L, "name", None), (2L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?), (?, ?, ?)"
```

**insertInto**

`insert`メソッドはテーブルが持つ全てのカラムにデータ挿入を行いますが、特定のカラムに対してのみデータを挿入したい場合は`insertInto`メソッドを使用します。

これはAutoIncrementやDefault値を持つカラムへのデータ挿入を除外したい場合などに使用できます。

```scala 3
val insert = userQuery.insertInto(user => (user.name, user.age)).values(("name", None))

insert.statement === "INSERT INTO user (`name`, `age`) VALUES(?, ?)"
```

複数のデータを挿入したい場合は、`values`にタプルの配列を渡すことで構築できます。

```scala 3
val insert = userQuery.insertInto(user => (user.name, user.age)).values(List(("name", None), ("name", Some(20))))

insert.statement === "INSERT INTO user (`name`, `age`) VALUES(?, ?), (?, ?)"
```

**+=**

`+=`メソッドを使用することでモデルを使用してinsert文を構築することができます。モデルを使用する場合は全てのカラムにデータを挿入してしまうことに注意してください。

```scala 3
val insert = userQuery += User(1L, "name", None)

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?)"
```

**++=**

モデルを使用して複数のデータを挿入したい場合は`++=`メソッドを使用します。

```scala 3
val insert = userQuery ++= List(User(1L, "name", None), User(2L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?), (?, ?, ?)"
```

### ON DUPLICATE KEY UPDATE

ON DUPLICATE KEY UPDATE 句を指定し行を挿入すると、UNIQUEインデックスまたはPRIMARY KEYで値が重複する場合、古い行のUPDATEが発生します。

LDBCでこの処理を実現する方法は2種類あり、`insertOrUpdate{s}`を使用するか、`Insert`に対して`onDuplicateKeyUpdate`を使用することです。

```scala 3
val insert = userQuery.insertOrUpdate((1L, "name", None))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?) AS new_user ON DUPLICATE KEY UPDATE `id` = new_user.`id`, `name` = new_user.`name`, `age` = new_user.`age`"
```

`insertOrUpdate{s}`を使用した場合、全てのカラムが更新対象となることに注意してください。重複する値があり特定のカラムのみを更新したい場合は、`onDuplicateKeyUpdate`を使用して更新したいカラムのみを指定するようにしてください。

```scala 3
val insert = userQuery.insert((1L, "name", None)).onDuplicateKeyUpdate(v => (v.name, v.age))

insert.statement === "INSERT INTO user (`id`, `name`, `age`) VALUES(?, ?, ?) AS new_user ON DUPLICATE KEY UPDATE `name` = new_user.`name`, `age` = new_user.`age`"
```

## UPDATE

型安全にUPDATE文を構築する方法はTableQueryが提供する`update`メソッドを使用することです。

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

型安全にDELETE文を構築する方法はTableQueryが提供する`delete`メソッドを使用することです。

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
