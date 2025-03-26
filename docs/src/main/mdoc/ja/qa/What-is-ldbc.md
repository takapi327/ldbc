{%
  laika.title = "Q: ldbcとは何ですか？"
  laika.metadata.language = ja
%}

# Q: ldbcとは何ですか？

## A: ldbcは、型安全なデータベースアクセス、クエリ構築、スキーマ定義を実現するOSSライブラリです。  
ldbcは、Scalaの力を活かしてコンパイル時にエラーを防止しながら、直感的なコードでデータベース操作が可能になります。  

たとえば、まずはスキーマの構築方法についてです。以下はスキーマ（テーブル）定義の例です。

```scala 3
// Schema定義の例
case class User(id: Long, name: String, email: String)

class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def name: Column[String] = varchar(255)
  def email: Column[String] = varchar(255)
  
  override def * : Column[User] = (id *: name *: email).to[User]
```

ldbcでは、このようなスキーマ定義を行った後、TableQueryを使ってテーブルを抽象化することも可能です。

```scala
// TableQueryを使ったUserTableの抽象化
val userTable: TableQuery[UserTable] = TableQuery[UserTable]
// これにより、QueryBuilderのAPIでschemaを利用できます。
```

次に、上記のスキーマ定義を活用したクエリビルダーの使い方の例です。スキーマを基にしたTableQueryを使用して、データの挿入や取得を行う方法を示します。

### Schemaを使ったクエリビルダーの例

```scala 3
// Schema定義済みのUserTableとTableQueryを利用
val userTable: TableQuery[UserTable] = TableQuery[UserTable]

// Schemaを使ったデータ挿入
val schemaInsert: DBIO[Int] =
  (userTable += User(1, "Charlie", "charlie@example.com")).update

// Schemaを使ったデータ取得（Userにマッピング）
val schemaSelect = userTable.selectAll.query.to[List]

// 実行例
for
  _     <- schemaInsert.commit(conn)
  users <- schemaSelect.readOnly(conn)
yield users.foreach(println)
```

また、プレーンクエリを直接使用してデータ操作を行うことも可能です。たとえば、以下の例ではプレーン SQL を使ってデータの挿入と取得を行っています。

```scala 3
// プレーンクエリによるデータ挿入
val plainInsert: DBIO[Int] =
  sql"INSERT INTO user (id, name, email) VALUES (2, 'Dave', 'dave@example.com')".update

// プレーンクエリによるデータ取得（Userにマッピング）
val plainSelect: DBIO[List[User]] =
  sql"SELECT id, name, email FROM user".query[User].to[List]

// 実行例
for
  _     <- plainInsert.commit(conn)
  users <- plainSelect.readOnly(conn)
yield users.foreach(println)
```

このように、ldbcはシンプルかつパワフルなAPIにより、スキーマ構築からそのスキーマを利用したクエリの構築まで、直感的にデータ操作を行える魅力的なライブラリです。

## 参考資料
- [クエリビルダーの使い方](/ja/tutorial/Query-Builder.md)  
- [スキーマ定義の詳細](/ja/tutorial/Schema.md)
