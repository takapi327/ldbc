# クエリビルダー

この章では、型安全にクエリを構築するための方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-query-builder" % "$version$"
```
@@@

@@@ vars
```yaml
version: '3'
services:
  mysql:
    image: mysql:"$mysqlVersion$"
    container_name: ldbc
    environment:
      MYSQL_USER: 'ldbc'
      MYSQL_PASSWORD: 'password'
      MYSQL_ROOT_PASSWORD: 'root'
    ports:
      - 13306:3306
    volumes:
      - ./database:/docker-entrypoint-initdb.d
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost" ]
      timeout: 20s
      retries: 10
```
@@@

次に、データベースの初期化を行います。以下のコードを使用して、データベースに接続し必要なテーブルを作成します。

@@snip [00-Setup.scala](/docs/src/main/scala/00-Setup.scala) { #setup }

**Scala CLIで実行**

このプログラムは、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

@@@ vars
```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala --dependency io.github.takapi327::ldbc-dsl:${version} --dependency io.github.takapi327::ldbc-connector:${version}
```
@@@

ldbcでは、クラスを使用してクエリを構築します。

```scala
import ldbc.query.builder.*

case class Task(id: Int, name: String, done: Boolean) derives Table
```

`Task`クラスは`Table`トレイトを継承しています。`Table`トレイトは`Table`クラスを継承しているため、`Table`クラスのメソッドを使用してクエリを構築することができます。

```scala
val query = Table[Task]
  .select(task => (task.id, task.name, task.done))
  .where(_.done === true)
  .orderBy(_.id.asc)
  .limit(1)
```

## SELECT

型安全にSELECT文を構築する方法はTableが提供する`select`メソッドを使用することです。ldbcではプレーンなクエリに似せて実装されているため直感的にクエリ構築が行えます。またどのようなクエリが構築されているかも一目でわかるような作りになっています。

特定のカラムのみ取得を行うSELECT文を構築するには`select`メソッドで取得したいカラムを指定するだけです。

```scala
val select = Table[Task]
  .select(_.id)

select.statement === "SELECT id FROM task"
```

複数のカラムを指定する場合は`select`メソッドで取得したいカラムを指定して指定したカラムのタプルを返すだけです。

```scala
val select = Table[Task]
  .select(task => (task.id, task.name))

select.statement === "SELECT id, name FROM task"
```

全てのカラムを指定したい場合はTableが提供する`selectAll`メソッドを使用することで構築できます。

```scala
val select = Table[Task]
  .selectAll

select.statement === "SELECT id, name, done FROM task"
```

特定のカラムの数を取得したい場合は、指定したカラムで`count`を使用することで構築できます。　

```scala
val select = Table[Task]
  .select(_.id.count)

select.statement === "SELECT COUNT(id) FROM task"
```

### WHERE

クエリに型安全にWhere条件を設定する方法は`where`メソッドを使用することです。
    
```scala
val where = Table[Task]
  .where(_.done === true)

where.statement === "SELECT id, name, done FROM task WHERE done = ?"
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

```scala
val select = Table[Task]
  .select(task => (task.id, task.name))
  .groupBy(_._2)

select.statement === "SELECT id, name FROM task GROUP BY name"
```

グループ化すると`select`で取得できるデータの数はグループの数だけとなります。そこでグループ化を行った場合には、グループ化に指定したカラムの値や、用意された関数を使ってカラムの値をグループ単位で集計した結果などを取得することができます。

`having`を使用すると`groupBy`によってグループ化されて取得したデータに関して、取得する条件を設定することができます。
    
```scala
val select = Table[Task]
  .select(task => (task.id, task.name))
  .groupBy(_._2)
  .having(_._1 > 1)

select.statement === "SELECT id, name FROM task GROUP BY name HAVING id > ?"
```

### ORDER BY

クエリに型安全にORDER BY句を設定する方法は`orderBy`メソッドを使用することです。

`orderBy`を使用することで`select`でデータを取得する時に指定したカラム名の値を基準に昇順、降順で並び替えることができます。

```scala
val select = Table[Task]
  .select(task => (task.id, task.name))
  .orderBy(_.id)

select.statement === "SELECT id, name FROM task ORDER BY id"
```

昇順/降順を指定したい場合は、それぞれカラムに対して `asc`/`desc`を呼び出すだけです。

```scala
val select = Table[Task]
  .select(task => (task.id, task.name))
  .orderBy(_.id.asc)

select.statement === "SELECT id, name FROM task ORDER BY id ASC"
```

### LIMIT/OFFSET

クエリに型安全にLIMIT句とOFFSET句を設定する方法は`limit`/`offset`メソッドを使用することです。

`limit`を設定すると`select`を実行した時に取得するデータの行数の上限を設定することができ、`offset`を設定すると何番目からのデータを取得するのかを指定することができます。

```scala
val select = Table[Task]
  .select(task => (task.id, task.name))
  .limit(1)
  .offset(1)
    
select.statement === "SELECT id, name FROM task LIMIT ? OFFSET ?"
```

## JOIN/LEFT JOIN/RIGHT JOIN

クエリに型安全にJoinを設定する方法は`join`/`leftJoin`/`rightJoin`メソッドを使用することです。

Joinでは以下定義をサンプルとして使用します。

```scala
case class Country(code: String, name: String) derives Table
case class City(id: Int, name: String, countryCode: String) derives Table
case class CountryLanguage(countryCode: String, language: String) derives Table

val countryTable = Table[Country]
val cityTable = Table[City]
val countryLanguageTable = Table[CountryLanguage]
```

まずシンプルなJoinを行いたい場合は、`join`を使用します。
`join`の第一引数には結合したいテーブルを渡し、第二引数では結合元のテーブルと結合したいテーブルのカラムで比較を行う関数を渡します。これはJoinにおいてのON句に該当します。

Join後の`select`は2つのテーブルからカラムを指定することになります。
    
```scala
val join = countryTable.join(cityTable)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country JOIN city ON country.code = city.country_code"
```

次に左外部結合であるLeft Joinを行いたい場合は、`leftJoin`を使用します。
`join`が`leftJoin`に変わっただけで実装自体はシンプルなJoinの時と同じになります。

```scala 3
val leftJoin = countryTable.leftJoin(cityTable)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country LEFT JOIN city ON country.code = city.country_code"
```

シンプルなJoinとの違いは`leftJoin`を使用した場合、結合を行うテーブルから取得するレコードはNULLになる可能性があるということです。

そのためldbcでは`leftJoin`に渡されたテーブルから取得するカラムのレコードは全てOption型になります。

```scala 3
val leftJoin = countryTable.leftJoin(cityTable)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name)) // (String, Option[String])
```

次に右外部結合であるRight Joinを行いたい場合は、`rightJoin`を使用します。
こちらも`join`が`rightJoin`に変わっただけで実装自体はシンプルなJoinの時と同じになります。

```scala 3
val rightJoin = countryTable.rightJoin(cityTable)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name))

join.statement = "SELECT country.`name`, city.`name` FROM country RIGHT JOIN city ON country.code = city.country_code"
```

シンプルなJoinとの違いは`rightJoin`を使用した場合、結合元のテーブルから取得するレコードはNULLになる可能性があるということです。

そのためldbcでは`rightJoin`を使用した結合元のテーブルから取得するカラムのレコードは全てOption型になります。

```scala 3
val rightJoin = countryTable.rightJoin(cityTable)((country, city) => country.code === city.countryCode)
  .select((country, city) => (country.name, city.name)) // (Option[String], String)
```

複数のJoinを行いたい場合は、メソッドチェーンで任意のJoinメソッドを呼ぶことで実現することができます。

```scala 3
val join = 
  (countryTable join cityTable)((country, city) => country.code === city.countryCode)
    .rightJoin(countryLanguageTable)((_, city, countryLanguage) => city.countryCode === countryLanguage.countryCode)
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

## INSERT

型安全にINSERT文を構築する方法はTableが提供する以下のメソッドを使用することです。

- insert
- insertInto
- +=
- ++=

**insert**

`insert`メソッドには挿入するデータのタプルを渡します。タプルはモデルと同じプロパティの数と型である必要があります。また、挿入されるデータの順番はモデルのプロパティおよびテーブルのカラムと同じ順番である必要があります。

```scala 3
val insert = task.insert((1L, "name", false))

insert.statement === "INSERT INTO task (`id`, `name`, `done`) VALUES(?, ?, ?)"
```

複数のデータを挿入したい場合は、`insert`メソッドに複数のタプルを渡すことで構築できます。

```scala 3
val insert = task.insert((1L, "name", false), (2L, "name", true))

insert.statement === "INSERT INTO task (`id`, `name`, `age`) VALUES(?, ?, ?), (?, ?, ?)"
```

**insertInto**

`insert`メソッドはテーブルが持つ全てのカラムにデータ挿入を行いますが、特定のカラムに対してのみデータを挿入したい場合は`insertInto`メソッドを使用します。

これはAutoIncrementやDefault値を持つカラムへのデータ挿入を除外したい場合などに使用できます。

```scala 3
val insert = task.insertInto(task => (task.name, task.done)).values(("name", false))

insert.statement === "INSERT INTO task (`name`, `done`) VALUES(?, ?)"
```

複数のデータを挿入したい場合は、`values`にタプルの配列を渡すことで構築できます。

```scala 3
val insert = task.insertInto(task => (task.name, task.done)).values(List(("name", false), ("name", true)))

insert.statement === "INSERT INTO task (`name`, `done`) VALUES(?, ?), (?, ?)"
```

**+=**

`+=`メソッドを使用することでモデルを使用してinsert文を構築することができます。モデルを使用する場合は全てのカラムにデータを挿入してしまうことに注意してください。

```scala 3
val insert = task += Task(1L, "name", false)

insert.statement === "INSERT INTO task (`id`, `name`, `done`) VALUES(?, ?, ?)"
```

**++=**

モデルを使用して複数のデータを挿入したい場合は`++=`メソッドを使用します。

```scala 3
val insert = task ++= List(Task(1L, "name", false), Task(2L, "name", true))

insert.statement === "INSERT INTO task (`id`, `name`, `done`) VALUES(?, ?, ?), (?, ?, ?)"
```

### ON DUPLICATE KEY UPDATE

ON DUPLICATE KEY UPDATE 句を指定し行を挿入すると、UNIQUEインデックスまたはPRIMARY KEYで値が重複する場合、古い行のUPDATEが発生します。

ldbcでこの処理を実現する方法は、`Insert`に対して`onDuplicateKeyUpdate`を使用することです。

```scala
val insert = task.insert((1L, "name", false)).onDuplicateKeyUpdate(v => (v.name, v.done))

insert.statement === "INSERT INTO task (`id`, `name`, `done`) VALUES(?, ?, ?) AS new_task ON DUPLICATE KEY UPDATE `name` = new_task.`name`, `done` = new_task.`done`"
```

## UPDATE

型安全にUPDATE文を構築する方法はTableが提供する`update`メソッドを使用することです。

`update`メソッドの第1引数にはテーブルのカラム名ではなくモデルのプロパティ名を指定し、第2引数に更新したい値を渡します。第2引数に渡す値の型は第1引数で指定したプロパティの型と同じである必要があります。

```scala
val update = task.update("name", "update name")

update.statement === "UPDATE task SET name = ?"
```

第1引数に存在しないプロパティ名を指定した場合コンパイルエラーとなります。

```scala 3
val update = task.update("hoge", "update name") // Compile error
```

複数のカラムを更新したい場合は`set`メソッドを使用します。

```scala 3
val update = task.update("name", "update name").set("done", false)

update.statement === "UPDATE task SET name = ?, done = ?"
```

`set`メソッドには条件に応じてクエリを生成させないようにすることもできます。

```scala 3
val update = task.update("name", "update name").set("done", false, false)

update.statement === "UPDATE task SET name = ?"
```

モデルを使用してupdate文を構築することもできます。モデルを使用する場合は全てのカラムを更新してしまうことに注意してください。

```scala 3
val update = task.update(Task(1L, "update name", false))

update.statement === "UPDATE task SET id = ?, name = ?, done = ?"
```

## DELETE

型安全にDELETE文を構築する方法はTableが提供する`delete`メソッドを使用することです。

```scala
val delete = task.delete

delete.statement === "DELETE FROM task"
```
