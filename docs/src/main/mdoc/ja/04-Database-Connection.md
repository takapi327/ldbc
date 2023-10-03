# データベース接続

この章では、LDBCで構築したクエリを使用して、データベースへの接続処理を行うための方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

@@@ vars
```scala
libraryDependencies ++= Seq(
  "$org$" %% "ldbc-dsl" % "$version$",
  "mysql" % "mysql-connector-java" % "$mysqlVersion$"
)
```
@@@

LDBCでのクエリ構築方法をまだ読んでいない場合は、先に[型安全なクエリ構築](http://localhost:4000/ja/03-Type-safe-Query-Builder.html)の章を先に読むことをオススメしましす。

以下のコード例では、以下のimportを想定しています。

```scala 3
import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.sql.*
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.TableQuery
```

テーブル定義は以下を使用します。

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

val userQuery = TableQuery[IO, User](table)
```

## DataSourceの使用

LDBCはデータベース接続にJDBCのDataSourceを使用します。LDBCにはこのDataSourceを構築する実装は提供されていないため、mysqlやHikariCPなどのライブラリを使用する必要があります。今回の例ではMysqlDataSourceを使用してDataSourceの構築を行います。

```scala 3
private val dataSource = new MysqlDataSource()
dataSource.setServerName("127.0.0.1")
dataSource.setPortNumber(3306)
dataSource.setDatabaseName("database name")
dataSource.setUser("user name")
dataSource.setPassword("password")
```

## ログ

LDBCではDatabase接続の実行ログやエラーログを任意のロギングライブラリを使用して任意の形式で書き出すことができます。

標準ではCats EffectのConsoleを使用したロガーが提供されているため開発時はこちらを使用することができます。

```scala 3
given LogHandler[IO] = LogHandler.consoleLogger
```

### カスタマイズ

任意のロギングライブラリを使用してログをカスタマイズする場合は`ldbc.dsl.logging.LogHandler`を使用します。

以下は標準実装のログ実装です。LDBCではデータベース接続で以下3種類のイベントが発生します。

- Success: 処理の成功
- ProcessingFailure: データ取得後もしくはデータベース接続前の処理のエラー
- ExecFailure: データベースへの接続処理のエラー

それぞれのイベントでどのようなログを書き込むかをパターンマッチングによって振り分けを行います。

```scala 3
def consoleLogger[F[_]: Console: Sync]: LogHandler[F] =
  case LogEvent.Success(sql, args) =>
    Console[F].println(
      s"""Successful Statement Execution:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    )
  case LogEvent.ProcessingFailure(sql, args, failure) =>
    Console[F].errorln(
      s"""Failed ResultSet Processing:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    ) >> Console[F].printStackTrace(failure)
  case LogEvent.ExecFailure(sql, args, failure) =>
    Console[F].errorln(
      s"""Failed Statement Execution:
         |  $sql
         |
         | arguments = [${ args.mkString(",") }]
         |""".stripMargin
    ) >> Console[F].printStackTrace(failure)
```

## Query

`select`文を構築すると`query`メソッドを使用できるようになります。`query`メソッドは取得後のデータ形式を決定するために使用します。特段何も型を指定しない場合は`select`メソッドで指定したカラムの型がTupleとして返却されます。

```scala 3
val query1 = userQuery.selectAll.query // (Long, String, Option[Int])
val query2 = userQuery.select(user => (user.name, user.age)).query // (String, Option[Int])
```

`query`メソッドにモデルを指定すると取得後のデータを指定したモデルに変換することができます。

```scala 3
val query = userQuery.selectAll.query[User] // User
```

`query`メソッドで指定するモデルの型は`select`メソッドで指定したTupleの型と一致するか、Tupleの型から指定したモデルへの型変換が可能なものでなければなりません。

```scala 3
val query1 = userQuery.select(user => (user.name, user.age)).query[User] // Compile error

case class Test(name: String, age: Option[Int])
val query2 = userQuery.select(user => (user.name, user.age)).query[Test] // Test
```

### toList

`query`メソッドで取得する型を決定したあとは、取得するデータを配列で取得するかOptionalなデータとして取得するかを決定します。

クエリを実行した結果データの一覧を取得したい場合は、`toList`メソッドを使用します。`toList`メソッドを使用してデータベース処理を行なった結果、データ取得件数が0件であった場合空の配列が返されます。

```scala 3
val query1 = userQuery.selectAll.query.toList // List[(Long, String, Option[Int])]
val query2 = userQuery.selectAll.query[User].toList // List[User]
```

### headOption

クエリを実行した結果最初の1件のデータをOptionalで取得したい場合は、`headOption`メソッドを使用します。`headOption`メソッドを使用してデータベース処理を行なった結果データ取得件数が0件であった場合Noneが返されます。

`headOption`メソッドを使用した場合、複数のデータを取得するクエリを実行したとしても最初のデータのみ返されることに注意してください。

```scala 3
val query1 = userQuery.selectAll.query.headOption // Option[(Long, String, Option[Int])]
val query2 = userQuery.selectAll.query[User].headOption // Option[User]
```

### unsafe

`unsafe`メソッドを使用した場合、取得したデータの最初の1件のみ返されることは`headOption`メソッドと同じですが、データはOptionalにはならずそのままのデータが返却されます。もし取得したデータの件数が0件であった場合は例外が発生するため適切な例外ハンドリングを行う必要があります。

実行時に例外を発生する可能性が高いため`unsafe`という名前になっています。

```scala 3
val query1 = userQuery.selectAll.query.unsafe // (Long, String, Option[Int])
val query2 = userQuery.selectAll.query[User].unsafe // User
```

## Update

Coming soon...

## Read Only

Coming soon...

## Auto Commit

Coming soon...

## Transaction

Coming soon...

## Executing Database Actions

Coming soon...
