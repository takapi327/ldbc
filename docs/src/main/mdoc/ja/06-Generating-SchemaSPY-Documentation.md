# SchemaSPYドキュメントの生成

この章では、LDBCで構築したテーブル定義を使用して、SchemaSPYドキュメントの作成を行うための方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-schemaspy" % "$version$"
```
@@@

LDBCでのテーブル定義方法をまだ読んでいない場合は、先に[テーブル定義](http://localhost:4000/ja/01-Table-Definitions.html)の章を先に読むことをオススメしましす。

以下のコード例では、以下のimportを想定しています。

```scala 3
import ldbc.core.*
import ldbc.schemaspy.SchemaSpyGenerator
```

## テーブル定義から生成

SchemaSPYはデータベースへ接続を行いMeta情報やテーブル構造を取得しその情報を元にドキュメントを生成しますが、LDBCではデータベースへの接続は行わずLDBCで構築したテーブル構造を使用してSchemaSPYのドキュメントを生成します。
データベースへの接続を行わないためシンプルにSchemaSPYを使用して生成したドキュメントと乖離する項目があります。例えば、現在テーブルに保存されているレコード数などの情報は表示することができません。

ドキュメントを生成するためにはデータベースの情報が必要です。LDBCではデータベースの情報を表現するためのtraitが存在しています。

`ldbc.core.Database`を使用してデータベース情報を構築したサンプルは以下になります。

```scala 3
case class SampleLdbcDatabase(
  schemaMeta: Option[String] = None,
  catalog: Option[String] = Some("def"),
  host: String = "127.0.0.1",
  port: Int = 3306
) extends Database:

  override val databaseType: Database.Type = Database.Type.MySQL

  override val name: String = "sample_ldbc"

  override val schema: String = "sample_ldbc"

  override val character: Option[Character] = None

  override val collate: Option[Collate] = None

  override val tables = Set(
    ... // LDBCで構築したテーブル構造を列挙
  )
```

データベース情報は現状SchemaSPYのドキュメント生成でしか使用できませんが、今後の機能改修で他の用途としても使用していく予定です。

SchemaSPYのドキュメント生成には`SchemaSpyGenerator`を使用します。生成したデータベース定義を`default`メソッドに渡し、`generate`を呼び出すと第2引数に指定したファイルの場所にSchemaSPYのファイル群が生成されます。

```scala 3
@main
def run(): Unit =
  val file = java.io.File("document")
  SchemaSpyGenerator.default(SampleLdbcDatabase(), file).generate()
```

生成されたファイルの`index.html`を開くとSchemaSPYのドキュメントを確認することができます。

## データベース接続から生成

SchemaSpyGeneratorには`connect`メソッドも存在しています。こちらは標準のSchemaSpyの生成方法と同様にデータベースに接続を行いドキュメントの生成を行います。

```scala 3
def run(): Unit =
  val file = java.io.File("document")
  SchemaSpyGenerator.connect(SampleLdbcDatabase(), "user name", "password" file).generate()
```

データベース接続を行う処理はSchemaSpy内部のJavaで書かれた実装で行われます。そのためEffectシステムでスレッドなどが管理されていないことに注意してください。
