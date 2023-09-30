# テーブル定義

この章では、Scala コードでデータベーススキーマを扱う方法、特に既存のデータベースなしでアプリケーションを書き始めるときに便利な手動でスキーマを記述する方法について説明します。すでにデータベースにスキーマがある場合は、[code generator]() を使ってこの作業を省略することもできます。

以下のコード例では、以下のimportを想定しています。

```scala 3
import ldbc.core.*
import ldbc.core.attribute.*
```

LDBCは、Scalaモデルとデータベースのテーブル定義を1対1のマッピングで管理します。モデルが保持するプロパティとテーブルが保持するカラムのマッピングは、定義順に行われます。テーブル定義は、Create文の構造と非常によく似ています。このため、テーブル定義の構築はユーザーにとって直感的なものとなります。

LDBC は、このテーブル定義をさまざまな目的で使用します。型安全なクエリの生成、ドキュメントの生成など。

```scala 3
case class User(
  id: Long,
  name: String,
  age: Option[Int],
)

val table = Table[User]("user")(                     // CREATE TABLE `user` (
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY), //   `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  column("name", VARCHAR(255)),                      //   `name` VARCHAR(255) NOT NULL,
  column("age", INT.UNSIGNED.DEFAULT(None)),         //   `age` INT unsigned DEFAULT NULL
)                                                    // );
```

すべてのカラムはcolumnメソッドで定義される。各カラムにはカラム名、データ型、属性があります。以下のプリミティブ型が標準でサポートされており、すぐに使用できます。

- Numeric types: Byte, Short, Int, Long, Float, Double, BigDecimal, BigInt
- LOB types: java.sql.Blob, java.sql.Clob, Array[Byte]
- Date types: java.sql.Date, java.sql.Time, java.sql.Timestamp
- String
- Boolean
- java.time.*

Null可能な列はOption[T]で表現され、Tはサポートされるプリミティブ型の1つである。Option型でない列はすべてNot Nullであることに注意。

## データ型

モデルが持つプロパティのScala型とカラムが持つデータ型の対応付けは、定義されたデータ型がScala型をサポートしている必要があります。サポートされていない型を割り当てようとするとコンパイルエラーが発生します。

データ型がサポートするScalaの型は以下の表の通りです。

| Data Type  | Scala Type                                                                                    |
|------------|-----------------------------------------------------------------------------------------------|
| BIT        | Byte, Short, Int, Long                                                                        |
| TINYINT    | Byte, Short                                                                                   |
| SMALLINT   | Short, Int                                                                                    |
| MEDIUMINT  | Int                                                                                           |
| INT        | Int, Long                                                                                     |
| BIGINT     | Long, BigInt                                                                                  |
| DECIMAL    | BigDecimal                                                                                    |
| FLOAT      | Float                                                                                         |
| DOUBLE     | Double                                                                                        |
| CHAR       | String                                                                                        |
| VARCHAR    | String                                                                                        |
| BINARY     | Array[Byte]                                                                                   |
| VARBINARY  | Array[Byte]                                                                                   |
| TINYBLOB   | Array[Byte]                                                                                   |
| BLOB       | Array[Byte]                                                                                   |
| MEDIUMBLOB | Array[Byte]                                                                                   |
| LONGBLOB   | Array[Byte]                                                                                   |
| TINYTEXT   | String                                                                                        |
| TEXT       | String                                                                                        |
| MEDIUMTEXT | String                                                                                        |
| DATE       | java.time.LocalDate                                                                           |
| DATETIME   | java.time.Instant, java.time.LocalDateTime, java.time.OffsetTime                              |
| TIMESTAMP  | java.time.Instant, java.time.LocalDateTime, java.time.OffsetDateTime, java.time.ZonedDateTime |
| TIME       | java.time.LocalTime                                                                           |
| YEAR       | java.time.Instant, java.time.LocalDate, java.time.Year                                        |
| BOOLEAN    | Boolean                                                                                       |

整数型を扱う際の注意点。符号あり、符号なしに応じて、扱えるデータの範囲がScalaの型に収まらないことに注意。

| Data Type | signed range                               | unsigned range           | Scala Type     | range                                                              |
|-----------|--------------------------------------------|--------------------------|----------------|--------------------------------------------------------------------|
| TINYINT   | -128 ~ 127                                 | 0 ~ 255                  | Byte<br>Short  | -128 ~ 127<br>-32768～32767                                         |
| SMALLINT  | -32768 ~ 32767                             | 0 ~ 65535                | Short<br>Int   | -32768～32767<br>-2147483648～2147483647                             |
| MEDIUMINT | -8388608 ~ 8388607                         | 0 ~ 16777215             | Int            | -2147483648～2147483647                                             |
| INT       | -2147483648	~ 2147483647                   | 0 ~ 4294967295           | Int<br>Long    | -2147483648～2147483647<br>-9223372036854775808～9223372036854775807 |
| BIGINT    | -9223372036854775808 ~ 9223372036854775807 | 0 ~ 18446744073709551615 | Long<br>BigInt | -9223372036854775808～9223372036854775807<br>...                    |

ユーザー定義の独自型やサポートされていない型を扱う場合は、[カスタム型](http://localhost:4000/ja/02-Custom-Data-Type.html) を参照してください。

## 属性

カラムにはさまざまな属性を割り当てることができます。

- `AUTO_INCREMENT`
  DDL文を作成し、SchemaSPYを文書化する際に、列を自動インクリメント・キーとしてマークする。
  MySQLでは、データ挿入時にAutoIncでないカラムを返すことはできません。そのため、必要に応じて、LDBCは戻りカラムがAutoIncとして適切にマークされているかどうかを確認します。
- `PRIMARY_KEY`
  DDL文やSchemaSPYドキュメントを作成する際に、列を主キーとしてマークする。
- `UNIQUE_KEY`
  DDL文やSchemaSPYドキュメントを作成する際に、列を一意キーとしてマークする。
- `COMMENT`
  DDL文やSchemaSPY文書を作成する際に、列にコメントを設定する。

## キーの設定

MySQLではテーブルに対してUniqueキーやIndexキー、外部キーなどの様々なキーを設定することができます。LDBCで構築したテーブル定義でこれらのキーを設定する方法を見ていきましょう。

### PRIMARY KEY

主キー（primary key）とはMySQLにおいてデータを一意に識別するための項目のことです。カラムにプライマリーキー制約を設定すると、カラムには他のデータの値を重複することのない値しか格納することができなくなります。また NULL も格納することができません。その結果、プライマリーキー制約が設定されたカラムの値を検索することで、テーブルの中でただ一つのデータを特定することができます。

LDBCではこのプライマリーキー制約を2つの方法で設定することができます。

1. columnメソッドの属性として設定する
2. tableのkeySetメソッドで設定する

**columnメソッドの属性として設定する**

columnメソッドの属性として設定する方法は非常に簡単で、columnメソッドの第3引数以降に`PRIMARY_KEY`を渡すだけです。これによって以下の場合 `id`カラムを主キーとして設定することができます。

```scala 3
column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
```

**tableのkeySetメソッドで設定する**

LDBCのテーブル定義には `keySet`というメソッドが生えており、ここで`PRIMARY_KEY`に主キーとして設定したいカラムを渡すことで主キーとして設定することができます。

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => PRIMARY_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   PRIMARY KEY (`id`)
// )
```

`PRIMARY_KEY`メソッドにはカラム意外にも以下のパラメーターを設定することができます。

- `Index Type` ldbc.core.Index.Type.BTREE or ldbc.core.Index.Type.HASH
- `Index Option` ldbc.core.Index.IndexOption

#### 複合キー (primary key)

1つのカラムだけではなく、複数のカラムを主キーとして組み合わせ主キーとして設定することもできます。`PRIMARY_KEY`に主キーとして設定したいカラムを複数渡すだけで複合主キーとして設定することができます。

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => PRIMARY_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   PRIMARY KEY (`id`, `name`)
// )
```

複合キーは`keySet`メソッドでの`PRIMARY_KEY`でしか設定することはできません。仮に以下のようにcolumnメソッドの属性として複数設定を行うと複合キーとしてではなく、それぞれを主キーとして設定されてしまいます。

LDBCではテーブル定義に複数`PRIMARY_KEY`を設定したとしてもコンパイルエラーにすることはできません。しかし、テーブル定義をクエリの生成やドキュメントの生成などで使用する場合エラーとなります。これはPRIMARY KEYはテーブルごとに1つしか設定することができないという制約によるものです。

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255), PRIMARY_KEY),
  column("age", INT.UNSIGNED.DEFAULT(None))
)

// CREATE TABLE `user` (
//   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
// )
```

### UNIQUE KEY

一意キー（unique key）とはMySQLにおいてデータを一意に識別するための項目のことです。カラムに一意性制約を設定すると、カラムには他のデータの値を重複することのない値しか格納することができなくなります。

LDBCではこの一意性制約を2つの方法で設定することができます。

1. columnメソッドの属性として設定する
2. tableのkeySetメソッドで設定する

**columnメソッドの属性として設定する**

columnメソッドの属性として設定する方法は非常に簡単で、columnメソッドの第3引数以降に`UNIQUE_KEY`を渡すだけです。これによって以下の場合 `id`カラムを一意キーとして設定することができます。

```scala 3
column("id", BIGINT, AUTO_INCREMENT, UNIQUE_KEY)
```

**tableのkeySetメソッドで設定する**

LDBCのテーブル定義には `keySet`というメソッドが生えており、ここで`UNIQUE_KEY`に一意キーとして設定したいカラムを渡すことで一意キーとして設定することができます。

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => UNIQUE_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   UNIQUE KEY (`id`)
// )
```

`UNIQUE_KEY`メソッドにはカラム意外にも以下のパラメーターを設定することができます。

- `Index Name` String
- `Index Type` ldbc.core.Index.Type.BTREE or ldbc.core.Index.Type.HASH
- `Index Option` ldbc.core.Index.IndexOption

#### 複合キー (unique key)

1つのカラムだけではなく、複数のカラムを一意キーとして組み合わせ一意キーとして設定することもできます。`UNIQUE_KEY`に一意キーとして設定したいカラムを複数渡すだけで複合一意キーとして設定することができます。

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => UNIQUE_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   UNIQUE KEY (`id`, `name`)
// )
```

複合キーは`keySet`メソッドでの`UNIQUE_KEY`でしか設定することはできません。仮にcolumnメソッドの属性として複数設定を行うと複合キーとしてではなく、それぞれを一意キーとして設定されてしまいます。

### INDEX KEY

インデックスキー（index key）とはMySQLにおいて目的のレコードを効率よく取得するための「索引」のことです。

LDBCではこのインデックスを2つの方法で設定することができます。

1. columnメソッドの属性として設定する
2. tableのkeySetメソッドで設定する

**columnメソッドの属性として設定する**

columnメソッドの属性として設定する方法は非常に簡単で、columnメソッドの第3引数以降に`INDEX_KEY`を渡すだけです。これによって以下の場合 `id`カラムをインデックスとして設定することができます。

```scala 3
column("id", BIGINT, AUTO_INCREMENT, INDEX_KEY)
```

**tableのkeySetメソッドで設定する**

LDBCのテーブル定義には `keySet`というメソッドが生えており、ここで`INDEX_KEY`にインデックスとして設定したいカラムを渡すことでインデックスキーとして設定することができます。

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => INDEX_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   INDEX KEY (`id`)
// )
```

`INDEX_KEY`メソッドにはカラム意外にも以下のパラメーターを設定することができます。

- `Index Name` String
- `Index Type` ldbc.core.Index.Type.BTREE or ldbc.core.Index.Type.HASH
- `Index Option` ldbc.core.Index.IndexOption

#### 複合キー (index key)

1つのカラムだけではなく、複数のカラムをインデックスキーとして組み合わせインデックスキーとして設定することもできます。`INDEX_KEY`にインデックスキーとして設定したいカラムを複数渡すだけで複合インデックスとして設定することができます。

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => INDEX_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   INDEX KEY (`id`, `name`)
// )
```

複合キーは`keySet`メソッドでの`INDEX_KEY`でしか設定することはできません。仮にcolumnメソッドの属性として複数設定を行うと複合インデックスとしてではなく、それぞれをインデックスキーとして設定されてしまいます。

### FOREIGN KEY

外部キー（foreign key）とは、MySQLにおいてデータの整合性を保つための制約（参照整合性制約）です。  外部キーに設定されているカラムには、参照先となるテーブルのカラム内に存在している値しか設定できません。

LDBCではこの外部キー制約をtableのkeySetメソッドを使用する方法で設定することができます。

```scala 3
val post = Table[Post]("post")(
  column("id", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255))
)

val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long])
)
  .keySet(table => FOREIGN_KEY(table.postId, REFERENCE(post, post.id)))

// CREATE TABLE `user` (
//   ...,
//   CONSTRAINT `fk_post_id` FOREIGN KEY (`post_id`)  REFERENCES `post` (`id`)
// )
```

`FOREIGN_KEY`メソッドにはカラムとReference値意外にも以下のパラメーターを設定することができます。

- `Index Name` String

外部キー制約には親テーブルの削除時と更新時の挙動を設定することができます。`REFERENCE`メソッドに`onDelete`と`onUpdate`メソッドが提供されているのでこちらを使用することでそれぞれ設定することができます。

設定することのできる値は`ldbc.core.Reference.ReferenceOption`から取得することができます。

```scala 3
val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long])
)
  .keySet(table => FOREIGN_KEY(table.postId, REFERENCE(post, post.id).onDelete(Reference.ReferenceOption.RESTRICT)))

// CREATE TABLE `user` (
//   ...,
//   CONSTRAINT `fk_post_id` FOREIGN KEY (`post_id`)  REFERENCES `post` (`id`) ON DELETE RESTRICT
// )
```

設定することのできる値は以下になります。

- `RESTRICT`: 親テーブルに対する削除または更新操作を拒否します。
- `CASCADE`: 親テーブルから行を削除または更新し、子テーブル内の一致する行を自動的に削除または更新します。
- `SET_NULL`: 親テーブルから行を削除または更新し、子テーブルの外部キーカラムを NULL に設定します。
- `NO_ACTION`: 標準 SQL のキーワード。 MySQLでは、RESTRICT と同等です。
- `SET_DEFAULT`: このアクションは MySQL パーサーによって認識されますが、InnoDB と NDB はどちらも、ON DELETE SET DEFAULT または ON UPDATE SET DEFAULT 句を含むテーブル定義を拒否します。

#### 複合キー (foreign key)

1つのカラムだけではなく、複数のカラムを外部キーとして組み合わせて設定することもできます。`FOREIGN_KEY`に外部キーとして設定したいカラムを複数渡すだけで複合外部キーとして設定することができます。

```scala 3
val post = Table[Post]("post")(
  column("id", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255)),
  column("category", SMALLINT[Short])
)

val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long]),
  column("post_category", SMALLINT[Short])
)
  .keySet(table => FOREIGN_KEY(List(table.postId, table.postCategory), REFERENCE(post, post.id, post.category)))

// CREATE TABLE `user` (
//   ...,
//   CONSTRAINT `fk_post_id` FOREIGN KEY (`post_id`, `post_category`)  REFERENCES `post` (`id`, `category`)
// )
```

### 制約名

MySQLではCONSTRAINTを使用することで制約に対して任意の名前を付与することができます。この制約名はデータベース単位で一意の値である必要があります。

LDBCではCONSTRAINTメソッドが提供されているのでキー制約などの制約を設定する処理をCONSTRAINTメソッドに渡すだけで設定することができます。

```scala 3
val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long])
)
  .keySet(table => CONSTRAINT("fk_post_id", FOREIGN_KEY(table.postId, REFERENCE(post, post.id))))

// CREATE TABLE `user` (
//   ...,
//   CONSTRAINT `fk_post_id` FOREIGN KEY (`post_id`)  REFERENCES `post` (`id`)
// )
```
