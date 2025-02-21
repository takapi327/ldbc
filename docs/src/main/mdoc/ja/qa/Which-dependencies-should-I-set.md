{%
  laika.title = "Q: どの依存関係を設定すればよいですか？"
  laika.metadata.language = ja
%}

# Q: どの依存関係を設定すればよいですか？

## A: ldbcを利用するには、用途に応じて以下の依存関係を設定する必要があります。

- プレーンなDSL  
- クエリビルダー  
- スキーマ定義とモデルマッピング  

**コネクタ**

ldbcを使用してデータベース接続処理を行うには以下のいずれかの依存関係を設定します。

**jdbc-connector**

Javaで書かれた従来のコネクタを使用する場合は以下の依存関係を設定します。

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "jdbc-connector" % "@VERSION@",
  "com.mysql" % "mysql-connector-j" % "@MYSQL_VERSION@"
)
```

**ldbc-connector**

Scalaで書かれた新しいコネクタを使用する場合は以下の依存関係を設定します。

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@"
)
```

ldbc-connectorは、JVMだけではなくJS, Nativeのプラットフォームでも動作します。

Scala.jsやScala Nativeでldbcを使用する場合は、以下のように依存関係を設定します。

```scala 3
libraryDependencies ++= Seq(
  "com.example" %%% "ldbc-connector" % "@VERSION@"
)
```

### プレーンなDSL

プレーンなDSLを使用する場合、以下の依存関係を設定します.

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-dsl" % "@VERSION@"
)
```

プレーンなDSLは、シンプルなSQL文をそのまま記述する方法です。たとえば、直接SQLリテラルを用いてクエリを実行できます。

```scala
import ldbc.dsl.io.*

val plainResult = sql"SELECT name FROM user"
  .query[String]
  .to[List]
  .readOnly(conn)
// plainResultはList[String]として返される
```

### クエリビルダー

クエリビルダーを使用する場合、以下の依存関係を設定します.

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-query-builder" % "@VERSION@"
)
```

クエリビルダーは、型安全なAPIでクエリを構築できる方法です。次の例では、`User`モデルを定義し、`TableQuery`を使ってSELECT文を構築しています。

```scala 3
import ldbc.query.builder.*
import ldbc.query.builder.syntax.io.*

case class User(id: Int, name: String, email: String) derives Table

val userQuery = TableQuery[User]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")

// userQuery.statementは "SELECT id, name, email FROM user WHERE email = ?" として生成される
```

### スキーマ定義とモデルマッピング

スキーマ定義とモデルマッピングを使用する場合、以下の依存関係を設定します.

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-schema" % "@VERSION@"
)
```

スキーマ定義とモデルマッピングを利用すると、テーブル定義とScalaモデルとの1対1のマッピングを実現できます。以下は、`User`テーブルを定義する例です。

```scala 3
import ldbc.schema.*
import ldbc.schema.syntax.io.*

case class User(id: Long, name: String, email: String)

class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def name: Column[String] = varchar(255)
  def email: Column[String] = varchar(255)
  
  override def * : Column[User] = (id *: name *: email).to[User]

val userQuery = TableQuery[UserTable]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")

// userQuery.statementは "SELECT id, name, email FROM user WHERE email = ?" として生成される
```

## 参考資料
- [クエリビルダーの使い方](/ja/tutorial/Query-Builder.md)
- [スキーマ定義の詳細](/ja/tutorial/Schema.md)
- [プレーンなDSLの利用例](/ja/tutorial/Selecting-Data.md)
- [データベース接続](/ja/tutorial/Connection.md)
- [パラメータ化されたクエリ](/ja/tutorial/Parameterized-Queries.md)
