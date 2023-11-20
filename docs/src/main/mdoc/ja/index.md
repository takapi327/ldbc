@@@ index
 * [Table Definitions](./01-Table-Definitions.md)
 * [Custom Data Type](./02-Custom-Data-Type.md)
 * [Type-safe Query Builder](./03-Type-safe-Query-Builder.md)
 * [Database Connection](./04-Database-Connection.md)
 * [Plain SQL Queries](./05-Plain-SQL-Queries.md)
 * [Generating SchemaSPY Documentation](./06-Generating-SchemaSPY-Documentation.md)
 * [Schema Code Generation](./07-Schema-Code-Generation.md)
@@@

# LDBC

**LDBC**は1.0以前のソフトウェアであり、現在も活発に開発中であることに注意してください。新しいバージョンは以前のバージョンとバイナリ互換性がなくなってしまう可能性があります。

## はじめに

私たちのアプリケーション開発では大抵の場合データベースを使用します。<br>Scalaでデータベースアクセスを行う場合JDBCを使用する方法がありますが、ScalaにはこのJDBCをラップしたライブラリがいくつか存在しています。

- 関数型DSL (Slick, quill, zio-sql)
- SQL文字列インターポレーター (Anorm, doobie)

LDBCも同じくJDBCをラップしたライブラリであり、LDBCはそれぞれの側面を組み合わせたScala 3ライブラリで、型安全でリファクタブルなSQLインターフェイスを提供し、MySQLのデータベース上ですべてのSQL式を表現できます。

また、LDBCのコンセプトは、LDBCを使用することで単一リソースを管理することでScalaのモデルやsqlのスキーマ、ドキュメントを一元化できる開発を行えることです。

このコンセプトは宣言的でタイプセーフなWebエンドポイントライブラリである[tapir](https://github.com/softwaremill/tapir)から影響を受けました。<br>tapirを使用することで、型安全なエンドポイントを構築することができ、構築したエンドポイントからOpenAPIドキュメントを生成することもできます。

LDBCはデータベース層でScalaを使用して、同じように型安全な構築を可能にし、構築されたものを使用してドキュメントの生成を行えるようにします。

## なぜLDBCなのか？

データベースを利用したアプリケーション開発では、様々な変更を継続的に行う必要があります。

例えば、データベースに構築されたテーブルのどの情報をアプリケーションで扱うべきか、データ検索にはどのようなクエリが最適か、などである。

テーブル定義にカラムを1つ追加するだけでも、SQLファイルの修正、対応するモデルへのプロパティの追加、データベースへの反映、ドキュメントの更新などが必要になります。

他にも考慮すべきこと、修正すべきことなどたくさんあります。

日々の開発の中で全てをメンテナンスし続けるのはとても大変なことであり、メンテナンス漏れだって起こるかもしれません。

テーブル情報をアプリケーション・モデルにマッピングすることなく、プレーンなSQLでデータを取得し、データを取得する際には指定された型で取得するというアプローチは非常に良い方法だと思います。

この方法であれば、データベース固有のモデルを構築する必要がなく、開発者はデータを取得したいときに、取得したい種類のデータを使って自由にデータを扱うことができるからです。<br>また、プレーンなクエリを扱うことで、どのようなクエリが実行されるかを瞬時に把握できる点も非常に優れていると思います。

しかし、この方法ではテーブル情報のアプケーションでの管理がなくなっただけでドキュメントの更新などを解消することはできません。

LDBCは、これらの問題のいくつかを解決するために開発されています。

- 型安全性：コンパイル時の保証、開発時の補完、読み取り時の情報
- 宣言型：テーブル定義の形（"What"）とデータベース接続（"How"）を分離する。
- SchemaSPYの統合：テーブル記述からドキュメントを生成する
- フレームワークではなくライブラリ: あなたのスタックに統合できる

LDBCを使用するとデータベースの情報をアプリケーションで管理しなければいけませんが、型安全性とクエリの構築、ドキュメントの管理を一元化することができます。

LDBCでのモデルをテーブル定義にマッピングするのはとても簡単です。

モデルが持つプロパティと、そのカラムのために定義されるデータ型の間のマッピングも非常にシンプルです。開発者は、モデルが持つプロパティと同じ順序で、対応するカラムを定義するだけです。

```scala mdoc:silent
import ldbc.core.*

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
```

また、間違った型を組み合わせようとするとコンパイルエラーになります。

例えば、Userが持つString型のnameプロパティに関連するカラムにINT型のカラムを渡すとエラーになります。

```shell
[error] -- [E007] Type Mismatch Error:
[error] 169 |    column("name", INT),
[error]     |                   ^^^
[error]     |Found:    ldbc.core.DataType.Integer[T]
[error]     |Required: ldbc.core.DataType[String]
[error]     |
[error]     |where:    T is a type variable with constraint <: Int | Long | Option[Int | Long]
```

これらのアドオンの詳細については、[テーブル定義](/ja/01-Table-Definitions.html) を参照してください。

## クイックスタート

現在のバージョンは **Scala $scalaVersion$** に対応した **$version$** です。

@@@ vars
```scala
libraryDependencies ++= Seq(

  // まずはこの1つから
  "$org$" %% "ldbc-core" % "$version$",

  // そして、必要に応じてこれらを加える
  "$org$" %% "ldbc-dsl"           % "$version$", // プレーンクエリー データベース接続
  "$org$" %% "ldbc-query-builder" % "$version$", // 型安全なクエリ構築
  "$org$" %% "ldbc-schemaspy"     % "$version$", // SchemaSPYドキュメント生成
)
```
@@@

sbtプラグインの使い方については、こちらの[documentation](/ja/07-Schema-Code-Generation.html)を参照してください。

## TODO

- JSONデータタイプのサポート
- SETデータタイプのサポート
- Geometryデータタイプのサポート
- CHECK制約のサポート
- MySQL以外のデータベースサポート
- ストリーミングのサポート
- ZIOモジュールのサポート
- 他データベースライブラリとの統合
- テストキット
- etc...
