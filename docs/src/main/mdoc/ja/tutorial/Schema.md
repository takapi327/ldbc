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
