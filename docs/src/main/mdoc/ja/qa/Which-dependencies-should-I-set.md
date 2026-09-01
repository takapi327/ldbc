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
  "@ORGANIZATION@" %%% "ldbc-connector" % "@VERSION@"
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
import ldbc.dsl.*

val plainResult = sql"SELECT name FROM user"
  .query[String]
  .to[List]
  .readOnly(connector)
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
import ldbc.dsl.codec.Codec
import ldbc.query.builder.*

case class User(id: Int, name: String, email: String) derives Table
object User:
  given Codec[User] = Codec.derived[User]

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

### テスト

ldbcを使用するRepositoryの結合テストを書くには、以下の依存関係を設定します。

**ldbc-testkit**（フレームワーク非依存のコア）

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-testkit" % "@VERSION@" % Test
)
```

**ldbc-testkit-munit**（MUnit統合）

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-testkit-munit" % "@VERSION@" % Test
)
```

いずれのモジュールもJVM、Scala.js、Scala Nativeで動作します。

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %%% "ldbc-testkit-munit" % "@VERSION@" % Test
)
```

`ldbc-testkit-munit`はMUnitの`CatsEffectSuite`を継承した`LdbcSuite`トレイトを提供します。`ephemeralTest`（テスト終了後に自動ロールバック）と`persistentTest`（DDLなど実際のコミットが必要な場合）を使ってRepositoryのテストを簡潔に記述できます。

### SQLファイルからのコード生成

既存のSQLファイルからモデルとテーブル定義を生成する場合は、sbtプラグインを`project/plugins.sbt`に追加します。sbt 1とsbt 2の両方に対応しています。

```scala 3
addSbtPlugin("@ORGANIZATION@" % "ldbc-plugin" % "@VERSION@")
```

詳細は[スキーマコード生成](/ja/tutorial/Schema-Code-Generation.md)を参照してください。

### ZIOとの併用

Cats EffectではなくZIOを使用する場合は、`ldbc-zio-interop`を追加します。

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-zio-interop" % "@VERSION@"
)
```

JVMとScala.jsで動作します（ZIO Interop CatsがScala Nativeに未対応のため、Scala Nativeでは利用できません）。詳細は[ZIOとの併用](/ja/qa/How-to-use-with-ZIO.md)を参照してください。

### 認証プラグイン

`ldbc-connector`には主要なMySQL認証プラグインが同梱されているため、通常は追加の依存関係は不要です。独自の認証プラグインを実装する場合や、Aurora IAM認証を利用する場合のみ以下を追加します。

**ldbc-authentication-plugin**（認証プラグインの基盤）

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %%% "ldbc-authentication-plugin" % "@VERSION@"
)
```

**ldbc-aws-authentication-plugin**（Aurora IAM認証）

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %%% "ldbc-aws-authentication-plugin" % "@VERSION@"
)
```

いずれもJVM、Scala.js、Scala Nativeで動作します。詳細は[認証プラグイン](/ja/reference/AuthenticationPlugin.md)を参照してください。

## 参考資料
- [クエリビルダーの使い方](/ja/tutorial/Query-Builder.md)
- [スキーマ定義の詳細](/ja/tutorial/Schema.md)
- [プレーンなDSLの利用例](/ja/tutorial/Selecting-Data.md)
- [データベース接続](/ja/tutorial/Connection.md)
- [パラメータ化されたクエリ](/ja/tutorial/Parameterized-Queries.md)
