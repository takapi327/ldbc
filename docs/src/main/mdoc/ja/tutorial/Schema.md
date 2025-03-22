{%
  laika.title = スキーマ
  laika.metadata.language = ja
%}

# スキーマ

[クエリビルダー](/ja/tutorial/Query-Builder.md)で型安全にクエリを構築する方法を学びました。このページでは、Scalaコードでデータベーススキーマを定義し、テーブルとモデルのマッピングを行う方法を説明します。

スキーマ定義は、アプリケーションとデータベースの間の境界を明確にする重要な要素です。ldbcは、Scalaコードでスキーマを定義し、強力な型システムを活用してデータベースの構造を表現するための機能を提供しています。

この章では、Scala コードでデータベーススキーマを扱う方法、特に既存のデータベースなしでアプリケーションを書き始めるときに便利な、手動でスキーマを記述する方法について説明します。すでにデータベースにスキーマがある場合は、Code Generatorを使ってこの作業を省略することもできます。

## 準備

プロジェクトに以下の依存関係を設定する必要があります。

```scala
//> using dep "@ORGANIZATION@::ldbc-schema:@VERSION@"
```

以下のコード例では、以下のimportを想定しています。

```scala 3
import ldbc.schema.*
```

## テーブル定義の基本

ldbcでは、`Table`クラスを継承してテーブル定義を作成します。これにより、Scalaのモデル（ケースクラスなど）とデータベースのテーブルを関連付けることができます。

### 基本的なテーブル定義

```scala 3
// モデル定義
case class User(
  id:   Long,
  name:  String,
  age:   Option[Int] // NULLを許容するカラムの場合はOptionを使用
)

// テーブル定義
class UserTable extends Table[User]("user"): // "user"はテーブル名
  // カラム定義
  def id: Column[Long] = column[Long]("id")
  def name: Column[String] = column[String]("name")
  def age: Column[Option[Int]] = column[Option[Int]]("age")

  // モデルとのマッピング
  override def * : Column[User] = (id *: name *: age).to[User]
```

上記の例では：
- `Table[User]`はこのテーブルがUserモデルと関連付けられることを示します
- `"user"`はデータベース上のテーブル名です
- 各カラムは`column`メソッドで定義します
- `*`メソッドはテーブルの全カラムとモデルのマッピング方法を定義します

### データ型を指定したテーブル定義

カラムにはMySQLのデータ型や属性を指定できます：

```scala 3
class UserTable extends Table[User]("user"):
  // データ型や属性を指定したカラム定義
  def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
  def name: Column[String] = column[String]("name", VARCHAR(255))
  def age: Column[Option[Int]] = column[Option[Int]]("age", INT)

  override def * : Column[User] = (id *: name *: age).to[User]
```

## 専用カラム定義メソッドを使用する方法

ldbcは、各データ型に特化したカラム定義メソッドも提供しています。変数名がそのままカラム名として使用されるため、よりシンプルにコードを書くことができます。

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def name: Column[String] = varchar(255)
  def age: Column[Option[Int]] = int().defaultNull

  override def * : Column[User] = (id *: name *: age).to[User]
```

専用のカラム定義メソッドを使うと、そのデータ型に適した属性を設定できるようになり、よりタイプセーフなコードを書くことができます。

### カラム名を明示的に指定

カラム名を明示的に指定したい場合は以下のようにします：

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint("user_id").autoIncrement.primaryKey
  def name: Column[String] = varchar("user_name", 255)
  def age: Column[Option[Int]] = int("user_age").defaultNull

  override def * : Column[User] = (id *: name *: age).to[User]
```

### カラム命名規則の設定

`Naming`を使用して、カラムの命名規則を変更できます：

```scala 3
class UserTable extends Table[User]("user"):
  // パスカルケースのカラム名に変換（例：userId → UserId）
  given Naming = Naming.PASCAL
  
  def userId: Column[Long] = bigint().autoIncrement.primaryKey
  def userName: Column[String] = varchar(255)
  def userAge: Column[Option[Int]] = int().defaultNull

  override def * : Column[User] = (userId *: userName *: userAge).to[User]
```

利用可能な命名規則：
- `Naming.SNAKE`（デフォルト）: スネークケース（例：user_id）
- `Naming.CAMEL`: キャメルケース（例：userId）
- `Naming.PASCAL`: パスカルケース（例：UserId）

## 数値型カラムの定義

数値型カラムでは、以下のような操作が可能です：

### 整数型

```scala 3
def id: Column[Long] = bigint().autoIncrement.primaryKey
def count: Column[Int] = int().unsigned.default(0) // 符号なしに設定、デフォルト値0
def smallValue: Column[Short] = smallint().unsigned
```

### 小数点型

```scala 3
def price: Column[BigDecimal] = decimal(10, 2) // 合計10桁、小数点以下2桁
def rating: Column[Double] = double(5) // 倍精度浮動小数点数
def score: Column[Float] = float(4) // 単精度浮動小数点数
```

## 文字列型カラムの定義

文字列型カラムでは、以下のような操作が可能です：

```scala 3
def name: Column[String] = varchar(255) // 可変長文字列（最大255文字）
def code: Column[String] = char(5) // 固定長文字列（5文字）
def description: Column[String] = text() // テキスト型
def content: Column[String] = longtext() // 長いテキスト型

// 文字セット（キャラクターセット）の指定
def japaneseText: Column[String] = text().charset(Character.utf8mb4)

// 照合順序（コレーション）の指定
def sortableText: Column[String] = varchar(255)
  .charset(Character.utf8mb4)
  .collate(Collate.utf8mb4_unicode_ci)
```

## バイナリ型カラムの定義

バイナリデータを扱うカラムの定義：

```scala 3
def data: Column[Array[Byte]] = binary(255) // 固定長バイナリ
def flexData: Column[Array[Byte]] = varbinary(1000) // 可変長バイナリ
def largeData: Column[Array[Byte]] = blob() // バイナリラージオブジェクト
```

## 日付・時間型カラムの定義

日付・時間を扱うカラムの定義：

```scala 3
def birthDate: Column[LocalDate] = date() // 日付のみ
def createdAt: Column[LocalDateTime] = datetime() // 日付と時間
def updatedAt: Column[LocalDateTime] = timestamp()
  .defaultCurrentTimestamp(onUpdate = true) // 作成・更新時に自動更新
def startTime: Column[LocalTime] = time() // 時間のみ
def fiscalYear: Column[Int] = year() // 年のみ
```

## ENUM型と特殊データ型

ENUM型の使用例：

```scala 3
// ENUMの定義
enum UserStatus extends Enum:
  case Active, Inactive, Suspended
object UserStatus extends EnumDataType[UserStatus]

// テーブル定義でENUMを使用
class UserTable extends Table[User]("user"):
  // ...
  def status: Column[UserStatus] = `enum`[UserStatus]("status")
```

その他の特殊データ型：

```scala 3
def isActive: Column[Boolean] = boolean() // BOOLEAN型
def uniqueId: Column[BigInt] = serial() // SERIAL型（自動増分のBIGINT UNSIGNED）
```

## デフォルト値の設定

カラムにデフォルト値を設定する方法：

```scala 3
def score: Column[Int] = int().default(100) // 固定値
def updatedAt: Column[LocalDateTime] = timestamp()
  .defaultCurrentTimestamp() // 現在のタイムスタンプ
def createdDate: Column[LocalDate] = date()
  .defaultCurrentDate // 現在の日付
def nullableField: Column[Option[String]] = varchar(255)
  .defaultNull // NULL値
```

## 主キー・外部キー・インデックス

### 単一カラムの主キー

```scala 3
def id: Column[Long] = bigint().autoIncrement.primaryKey
```

### 複合主キーの定義

```scala 3
class OrderItemTable extends Table[OrderItem]("order_item"):
  def orderId: Column[Int] = int()
  def itemId: Column[Int] = int()
  def quantity: Column[Int] = int().default(1)
  
  // 複合主キーの定義
  override def keys = List(
    PRIMARY_KEY(orderId *: itemId)
  )

  override def * : Column[OrderItem] = (orderId *: itemId *: quantity).to[OrderItem]
```

### インデックスの定義

```scala 3
class UserTable extends Table[User]("user"):
  // ...カラム定義...
  
  // インデックスの定義
  override def keys = List(
    INDEX_KEY("idx_user_name", name), // 名前付きインデックス
    UNIQUE_KEY("idx_user_email", email) // ユニークインデックス
  )
```

インデックスタイプの指定も可能です：

```scala 3
override def keys = List(
  INDEX_KEY(
    Some("idx_name"), 
    Some(Index.Type.BTREE), // BツリーまたはHASHのインデックスタイプを指定可能
    None, 
    name
  )
)
```

### 外部キーの定義

外部キーを定義するには、まず参照先のテーブルのTableQueryを作成します：

```scala 3
// 参照先のテーブル
val userTable = TableQuery[UserTable]

// 参照元のテーブル
class ProfileTable extends Table[Profile]("profile"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def userId: Column[Long] = bigint()
  // ...他のカラム...
  
  // 外部キー定義
  def fkUser = FOREIGN_KEY(
    "fk_profile_user", // 外部キーの名前
    userId, // 参照元のカラム
    REFERENCE(userTable)(_.id) // 参照先のテーブルとカラム
      .onDelete(Reference.ReferenceOption.CASCADE) // 削除時の動作
      .onUpdate(Reference.ReferenceOption.RESTRICT) // 更新時の動作
  )
  
  override def keys = List(
    PRIMARY_KEY(id),
    fkUser // 外部キーを追加
  )
```

参照制約オプション（`ReferenceOption`）:
- `RESTRICT`: 子レコードが存在する限り親レコードの変更を許可しない
- `CASCADE`: 親レコードの変更に合わせて子レコードも変更する
- `SET_NULL`: 親レコードが変更されたとき子レコードの該当カラムをNULLに設定
- `NO_ACTION`: 制約チェックを遅延させる（基本的にはRESTRICTと同じ）
- `SET_DEFAULT`: 親レコードが変更されたとき子レコードの該当カラムをデフォルト値に設定

## 制約の設定

特定の命名規則で制約を定義したい場合は`CONSTRAINT`を使用できます：

```scala 3
override def keys = List(
  CONSTRAINT(
    "pk_user", // 制約名
    PRIMARY_KEY(id) // 制約タイプ
  ),
  CONSTRAINT(
    "fk_user_department",
    FOREIGN_KEY(departmentId, REFERENCE(departmentTable)(_.id))
  )
)
```

## モデルとの複雑なマッピング

### ネストしたモデルのマッピング

```scala 3
case class User(
  id: Long, 
  name: UserName, // ネストした型
  contact: Contact // ネストした型
)

case class UserName(first: String, last: String)
case class Contact(email: String, phone: Option[String])

class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def firstName: Column[String] = varchar(50)
  def lastName: Column[String] = varchar(50)
  def email: Column[String] = varchar(100)
  def phone: Column[Option[String]] = varchar(20).defaultNull
  
  // ネストした値のマッピング
  def userName: Column[UserName] = (firstName *: lastName).to[UserName]
  def contact: Column[Contact] = (email *: phone).to[Contact]
  
  override def * : Column[User] = (id *: userName *: contact).to[User]
```

この設定により、次のようなSQLが生成されます：

```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `firstName` VARCHAR(50) NOT NULL,
  `lastName` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(20) NULL,
  PRIMARY KEY (`id`)
)
```

## スキーマ生成とDDLの実行

テーブル定義からDDL（Data Definition Language）を生成し、データベースにスキーマを作成します。

### TableQueryの生成

```scala 3
val users = TableQuery[UserTable]
val profiles = TableQuery[ProfileTable]
val orders = TableQuery[OrderTable]
```

### スキーマの生成と実行

```scala 3
import ldbc.dsl.*

// スキーマの組み合わせ
val schema = users.schema ++ profiles.schema ++ orders.schema

// データベース接続を使ってスキーマを適用
provider.use { conn =>
  DBIO.sequence(
    // テーブル作成（存在しない場合のみ）
    schema.createIfNotExists,
    // データ投入など他の操作...
  ).commit(conn)
}
```

### DDL操作

```scala 3
val userSchema = users.schema

// 各種DDL操作
userSchema.create            // テーブル作成
userSchema.createIfNotExists // テーブルが存在しない場合のみ作成
userSchema.drop              // テーブル削除
userSchema.dropIfExists      // テーブルが存在する場合のみ削除
userSchema.truncate          // テーブル内のすべてのデータを削除
```

### DDLステートメントの確認

実際に実行されるSQLを確認する方法：

```scala 3
// 作成クエリの確認
userSchema.create.statements.foreach(println)

// 条件付き作成クエリの確認
userSchema.createIfNotExists.statements.foreach(println)

// 削除クエリの確認
userSchema.drop.statements.foreach(println)

// 条件付き削除クエリの確認
userSchema.dropIfExists.statements.foreach(println)

// truncateクエリの確認
userSchema.truncate.statements.foreach(println)
```

## カラム属性の設定

カラムには様々な属性を設定できます：

```scala 3
def id: Column[Long] = bigint()
  .autoIncrement    // 自動増分
  .primaryKey       // 主キー
  .comment("ユーザーID") // コメント

def email: Column[String] = varchar(255)
  .unique           // ユニーク制約
  .comment("メールアドレス")

def status: Column[String] = varchar(20)
  .charset(Character.utf8mb4)  // 文字セット
  .collate(Collate.utf8mb4_unicode_ci)  // 照合順序

def hiddenField: Column[String] = varchar(100)
  .invisible        // 不可視属性（SELECT *では取得されない）

def formatField: Column[String] = varchar(100)
  .setAttributes(COLUMN_FORMAT.DYNAMIC[String]) // カラム格納フォーマット

def storageField: Column[Array[Byte]] = blob()
  .setAttributes(STORAGE.DISK[Array[Byte]]) // ストレージタイプ
```

## まとめ

ldbcのスキーマモジュールを使用することで、Scalaの型システムを活用して安全かつ表現力豊かなデータベーススキーマを定義することができます。

主な特長：
- 強力な型安全性：コンパイル時にスキーマの問題を検出
- 豊富なデータ型サポート：MySQLの全データ型をサポート
- 柔軟なモデルマッピング：単純なケースクラスからネストした複雑なモデルまで対応
- DDL生成：テーブル定義から直接SQLを生成
- 拡張性：カスタムデータ型やマッピング機能をサポート

## 次のステップ

これでScalaコードでスキーマを定義する方法がわかりました。手動でスキーマを定義することで、アプリケーションとデータベースの構造を密接に連携させることができます。

次は[スキーマコード生成](/ja/tutorial/Schema-Code-Generation.md)に進み、既存のSQLファイルからスキーマコードを自動生成する方法を学びましょう。
