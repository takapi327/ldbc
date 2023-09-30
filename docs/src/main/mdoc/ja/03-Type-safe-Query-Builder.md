# 型安全なクエリ構築

この章では、LDBCで構築したテーブル定義を使用して、型安全にクエリを構築するための方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-query-builder" % "$version$"
```
@@@

LDBCでのテーブル定義方法をまだ読んでいない場合先に[テーブル定義](http://localhost:4000/ja/01-Table-Definitions.html)の章を先に読むことをオススメしましす。

以下のコード例では、以下のimportを想定しています。

```scala 3
import cats.effect.IO
import ldbc.core.*
import ldbc.query.builder.TableQuery
```

LDBCではTableQueryにテーブル定義を渡すことで型安全なクエリ構築を行います。

```scala 3
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

val user = TableQuery[IO, User](table)
```

## SELECT

Coming soon...

## INSERT

Coming soon...

## UPDATE

Coming soon...

## DELETE

Coming soon...
