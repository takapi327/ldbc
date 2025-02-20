{%
  laika.title = スキーマ
  laika.metadata.language = ja
%}

# スキーマ

この章では、Scala コードでデータベーススキーマを扱う方法、特に既存のデータベースなしでアプリケーションを書き始めるときに便利な、手動でスキーマを記述する方法について説明します。すでにデータベースにスキーマがある場合は、Code Generatorを使ってこの作業を省略することもできます。

プロジェクトに以下の依存関係を設定する必要があります。

```scala
//> using dep "@ORGANIZATION@::ldbc-schema:@VERSION@"
```

以下のコード例では、以下のimportを想定しています。

```scala 3
import ldbc.schema.*
```

ldbcは、Scalaモデルとデータベースのテーブル定義を1対1のマッピングで管理します。

実装者はSlickと同様にカラムを定義し、モデルへのマッピングを記述するだけです。

```scala 3
case class User(
  id:    Int,
  name:  String,
  age: Option[Int],
)

class UserTable extends Table[User]("user"):
  def id: Column[Long] = column[Long]("id")
  def name: Column[String] = column[String]("name")
  def age: Column[Option[Int]] = column[Option[Int]]("age")

  override def * : Column[User] = (id *: name *: age).to[User]
```

カラムにはデータ型を設定することもできます。

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
  def name: Column[String] = column[String]("name", VARCHAR(255))
  def age: Column[Option[Int]] = column[Option[Int]]("age", INT.UNSIGNED.DEFAULT(None))

  override def * : Column[User] = (id *: name *: age).to[User]
```

すべてのカラムはcolumnメソッドで定義されます。各カラムにはカラム名、データ型、属性があります。以下のプリミティブ型が標準でサポートされており、すぐに使用できます。

- Numeric types: `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `BigDecimal`, `BigInt`
- LOB types: `java.sql.Blob`, `java.sql.Clob`, `Array[Byte]`
- Date types: `java.sql.Date`, `java.sql.Time`, `java.sql.Timestamp`
- String
- Boolean
- java.time.*

Null可能な列は`Option[T]`で表現され、Tはサポートされるプリミティブ型の1つです。Option型でない列はすべてNot Nullであることに注意してください。

## データ型

モデルが持つプロパティのScala型とカラムが持つデータ型の対応付けは、定義されたデータ型がScala型をサポートしている必要があります。サポートされていない型を割り当てようとするとコンパイルエラーが発生します。

データ型がサポートするScalaの型は以下の表の通りです。

| Data Type    | Scala Type                                                                                      |
|--------------|-------------------------------------------------------------------------------------------------|
| `BIT`        | `Byte, Short, Int, Long`                                                                        |
| `TINYINT`    | `Byte, Short`                                                                                   |
| `SMALLINT`   | `Short, Int`                                                                                    |
| `MEDIUMINT`  | `Int`                                                                                           |
| `INT`        | `Int, Long`                                                                                     |
| `BIGINT`     | `Long, BigInt`                                                                                  |
| `DECIMAL`    | `BigDecimal`                                                                                    |
| `FLOAT`      | `Float`                                                                                         |
| `DOUBLE`     | `Double`                                                                                        |
| `CHAR`       | `String`                                                                                        |
| `VARCHAR`    | `String`                                                                                        |
| `BINARY`     | `Array[Byte]`                                                                                   |
| `VARBINARY`  | `Array[Byte]`                                                                                   |
| `TINYBLOB`   | `Array[Byte]`                                                                                   |
| `BLOB`       | `Array[Byte]`                                                                                   |
| `MEDIUMBLOB` | `Array[Byte]`                                                                                   |
| `LONGBLOB`   | `Array[Byte]`                                                                                   |
| `TINYTEXT`   | `String`                                                                                        |
| `TEXT`       | `String`                                                                                        |
| `MEDIUMTEXT` | `String`                                                                                        |
| `DATE`       | `java.time.LocalDate`                                                                           |
| `DATETIME`   | `java.time.Instant, java.time.LocalDateTime, java.time.OffsetTime`                              |
| `TIMESTAMP`  | `java.time.Instant, java.time.LocalDateTime, java.time.OffsetDateTime, java.time.ZonedDateTime` |
| `TIME`       | `java.time.LocalTime`                                                                           |
| `YEAR`       | `java.time.Instant, java.time.LocalDate, java.time.Year`                                        |
| `BOOLEA`     | `Boolean`                                                                                       |

**整数型を扱う際の注意点**

符号あり、符号なしに応じて、扱えるデータの範囲がScalaの型に収まらないことに注意する必要があります。

| Data Type   | Signed Range                                 | Unsigned Range             | Scala Type       | Range                                                                |
|-------------|----------------------------------------------|----------------------------|------------------|----------------------------------------------------------------------|
| `TINYINT`   | `-128 ~ 127`                                 | `0 ~ 255`                  | `Byte<br>Short`  | `-128 ~ 127<br>-32768～32767`                                         |
| `SMALLINT`  | `-32768 ~ 32767`                             | `0 ~ 65535`                | `Short<br>Int`   | `-32768～32767<br>-2147483648～2147483647`                             |
| `MEDIUMINT` | `-8388608 ~ 8388607`                         | `0 ~ 16777215`             | `Int`            | `-2147483648～2147483647`                                             |
| `INT`       | `-2147483648	~ 2147483647`                   | `0 ~ 4294967295`           | `Int<br>Long`    | `-2147483648～2147483647<br>-9223372036854775808～9223372036854775807` |
| `BIGINT`    | `-9223372036854775808 ~ 9223372036854775807` | `0 ~ 18446744073709551615` | `Long<br>BigInt` | `-9223372036854775808～9223372036854775807<br>...`                    |

ユーザー定義の独自型やサポートされていない型を扱う場合は、カスタムデータ型を参照してください。

## データ型のカラム

カラムにデータ型やその他の設定を与える場合は、引数として渡す以外にもデータ型の特徴を持つカラムを使用することができます。

この定義方法では、カラム名は変数名を使用できるためカラム名を引数として渡す必要がありません。

```scala 3
class UserTable extends Table[User]:
  def id: Column[Long] = bigint().autoIncrement().primaryKey
  def name: Column[String] = varchar(255)
  def age: Column[Option[Int]] = int().unsigned.defaultNull

  override def * : Column[User] = (id *: name *: age).to[User]
```

カラム名はNamingを暗黙的に渡すことで書式を変更することができます。
デフォルトはキャメルケースですが、パスカルケースに変更するには以下のようにします。

```scala 3
class UserTable extends Table[User]("user"):
  given Naming = Naming.PASCAL

  def id: Column[Long] = bigint().autoIncrement().primaryKey
  def name: Column[String] = varchar(255)
  def age: Column[Option[Int]] = int().unsigned.defaultNull

  override def * : Column[User] = (id *: name *: age).to[User]
```

特定のカラムの書式を変更したい場合は、カラム名を引数として渡すことで定義できます。

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint("ID").autoIncrement().primaryKey
  def name: Column[String] = varchar("NAME", 255)
  def age: Column[Option[Int]] = int("AGE").unsigned.defaultNull

  override def * : Column[User] = (id *: name *: age).to[User]
```

## 制約条件

`PRIMARY_KEY`を呼び出すメソッドを追加することで、主キー制約を定義できます。これは複合主キーを定義するのに便利です。

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint("ID").autoIncrement
  def name: Column[String] = varchar("NAME", 255)
  def age: Column[Option[Int]] = int("AGE").unsigned.defaultNull

  override def keys = List(PRIMARY_KEY(id, name))

  override def * : Column[User] = (id *: name *: age).to[User]
```

その他のインデックスも、`INDEX_KEY`メソッドで同様に定義します。

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint("ID").autoIncrement.primaryKey
  def name: Column[String] = varchar("NAME", 255)
  def age: Column[Option[Int]] = int("AGE").unsigned.defaultNull

  override def keys = List(INDEX_KEY(name))

  override def * : Column[User] = (id *: name *: age).to[User]
```

ユニーク制約も同様に定義できます。

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint("ID").autoIncrement.primaryKey
  def name: Column[String] = varchar("NAME", 255)
  def age: Column[Option[Int]] = int("AGE").unsigned.defaultNull

  override def keys = List(UNIQUE_KEY(name))

  override def * : Column[User] = (id *: name *: age).to[User]
```

外部キー制約は、テーブルの`FOREIGN_KEY`メソッドで定義できます。
まず、制約の名前、参照する列、参照されるテーブルを指定します。
テーブルのDDL文を作成する際に、外部キー定義が追加されます。

```scala 3
val userTable = TableQuery[UserTable]

class UserProfileTable extends Table[UserProfile]("user_profile"):
  // ...
  def userId: Column[Long] = bigint()

  def fkUserId = FOREIGN_KEY("FK_USER_ID", userId, REFERENCE(userTable)(_.id))

  override def keys = List(PRIMARY_KEY(id), fkUserId)
  // ...
```

## データ定義言語 (DDL)

テーブルの DDL 文は、`TableQuery`の`schema`メソッドで作成できます。複数の DDL オブジェクトを`++`で連結して複合 DDL オブジェクトを作成できます。`create`、`createIfNotExists`、`dropIfExists`、`drop` および `truncate` メソッドは、DDL 文を実行するアクションを生成します。テーブルを安全に作成および削除するには、`createIfNotExists`および`dropIfExists`メソッドを使用します。

```scala 3
val schema = TableQuery[UserTable].schema ++ TableQuery[UserProfileTable].schema

connection
  .use { conn =>
    DBIO
      .sequence(
        schema.createIfNotExists,
        schema.dropIfExists,
        schema.create,
        schema.drop
      )
      .commit(conn)
  }            
```

他のほとんどのSQLベースのActionと同様に、SQLコードを取得するためにステートメントメソッドを使用することができます。現在のところ、複数のステートメントを生成できるアクションはスキーマアクションだけです。

```scala 3
schema.create.statements.foreach(println)
schema.createIfNotExists.statements.foreach(println)
schema.truncate.statements.foreach(println)
schema.drop.statements.foreach(println)
schema.dropIfExists.statements.foreach(println)
```
