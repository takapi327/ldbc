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

LDBCでのクエリ構築方法をまだ読んでいない場合は、[型安全なクエリ構築](/ldbc/ja/03-Type-safe-Query-Builder.html)の章を先に読むことをオススメしましす。

以下のコード例では、以下のimportを想定しています。

```scala 3
import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.IO
// This is just for testing. Consider using cats.effect.IOApp instead of calling
// unsafe methods directly.
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

`insert/update/delete`文を構築すると`update`メソッドを使用できるようになります。`update`メソッドはデータベースへの書き込み処理件数を返却します。

```scala 3
val insert = userQuery.insert((1L, "name", None)).update // Int
val update = userQuery.update("name", "update name").update // Int
val delete = userQuery.delete.update // Int
```

`insert`文の場合データ挿入時にAutoIncrementで生成された値を返却させたい場合があります。その場合は`update`メソッドではなく`returning`メソッドを使用して返却したいカラムを指定します。

```scala 3
val insert = userQuery.insert((1L, "name", None)).returning("id") // Long
```

`returning`メソッドで指定する値はモデルが持つプロパティ名である必要があります。また、指定したプロパティがテーブル定義上でAutoIncrementの属性が設定されていなければエラーとなってしまいます。

MySQLではデータ挿入時に返却できる値はAutoIncrementのカラムのみであるため、LDBCでも同じような仕様となっています。

## データベース操作の実行

データベース接続を行う前にコミットのタイミングや読み書き専用などの設定を行う必要があります。

### 読み取り専用

`readOnly`メソッドを使用することで実行するクエリの処理を読み込み専用にすることができます。`readOnly`メソッドは`insert/update/delete`文でも使用することができますが、書き込み処理を行うので実行時にエラーとなります。

```scala 3
val read = userQuery.selectAll.query.readOnly
```

### 自動コミット

`autoCommit`メソッドを使用することで実行するクエリの処理をクエリ実行時ごとにコミットするように設定することができます。

```scala 3
val read = userQuery.insert((1L, "name", None)).update.autoCommit
```

### トランザクション

`transaction`メソッドを使用することで複数のデータベース接続処理を1つのトランザクションにまとめることができます。

`toList/headOption/unsafe/returning/update`メソッドの戻り値は`Kleisli[F, Connection[F], T]`型となっています。そのためmapやflatMapを使用して処理を1つにまとめることができます。

1つにまとめた`Kleisli[F, Connection[F], T]`に対して`transaction`メソッドを使用することで、中で行われる全てのデータベース接続処理は1つのトランザクションにまとめて実行されます。

```scala 3
(for
  result1 <- userQuery.insert((1L, "name", None)).returning("id")
  result2 <- userQuery.update("name", "update name").update
  ...
yield ...).transaction
```

### 実行

戻り値の型、接続方法の設定を行いましたがLDBCで構築された今までの処理は`Kleisli[F, XXX, T]`型となっているため定義を行なっただけではデータベース接続処理(副作用)は発生しません。
データベース処理を実行するためには`Kleisli`の`run`を実行する必要があります。

`readOnly/autoCommit/transaction`メソッドを使用すると戻り値の型は`Kleisli[F, DataSource, T]`となるためJDBCのDataSourceを`run`に渡すことで戻り値の型を`F`に持ち上げることができます。

```scala 3
val effect = userQuery.selectAll.query[User].headOption.readOnly(dataSource) // F[User]
```

Cats Effect IOを使用している場合は、`IOApp`内で実行を行うか`unsafeRunSync`などを使用することでデータベース接続処理を実行することができます。

```scala 3
val user: Option[User] = userQuery.selectAll.query[User].headOption.readOnly(dataSource).unsafeRunSync()
```

`Kleisli`に関してはCatsの[ドキュメント](https://typelevel.org/cats/datatypes/kleisli.html)を参照してください。

## Database Action

データベース処理を実行する方法としてデータベースへの接続情報を持った`Database`を使用して行う方法も存在します。

`Database`を構築する方法はDriverManagerを使用した方法と、DataSourceから生成する方法の2種類があります。以下はMySQLのドライバーを使用してデータベースへの接続情報を持った`Database`を構築する例です。

```scala 3
val db = Database.fromMySQLDriver[IO]("database name", "host", "port number", "user name", "password")
```

`Database`を使用してデータベース処理を実行するメリットは以下になります。

- DataSourceの構築を簡略できる (DriverManagerを使用した場合)
- クエリごとにDataSourceを受け渡す必要がなくなる

`Database`を使用する方法は、DataSourceを受け渡す方法を簡略化しただけにすぎないため、どちらを使用しても実行結果に差が出ることはありません。
`flatMap`などで処理を結合しメソッドチェーンで実行するか、結合した処理を`Database`を使用して実行するかの違いでしかありません。そのため実行方法はユーザーの好きの方法を選択できます。

**Read Only**

```scala 3
val user: Option[User] = db.readOnly(userQuery.selectAll.query[User].headOption).unsafeRunSync()
```

**Auto Commit**

```scala 3
val result = db.autoCommit(userQuery.insert((1L, "name", None)).update).unsafeRunSync()
```

**Transaction**

```scala 3
db.transaction(for
  result1 <- userQuery.insert((1L, "name", None)).returning("id")
  result2 <- userQuery.update("name", "update name").update
  ...
yield ...).unsafeRunSync()
```

### Database model

LDBCでは`Database`モデルはデータベースの接続情報を持つ以外の用途でも使用されます。他の用途としてSchemaSPYのドキュメント生成に使用されることです。SchemaSPYのドキュメント生成に関しては[こちら](/ldbc/ja/06-Generating-SchemaSPY-Documentation.html)を参照してください。

すでに`Database`モデルを別の用途で生成している場合は、そのモデルを使用してデータベースの接続情報を持った`Database`を構築することができます。

```scala 3
import ldbc.dsl.io.*

val database: Database = ???

val db = database.fromDriverManager()
// or
val db = database.fromDriverManager("user name", "password")
```

## HikariCPコネクションプールの使用

`ldbc-hikari`は、HikariCP接続プールを構築するためのHikariConfigおよびHikariDataSourceを構築するためのビルダーを提供します。

@@@ vars
```scala
libraryDependencies ++= Seq(
  "$org$" %% "ldbc-hikari" % "$version$",
)
```
@@@

`HikariConfigBuilder`は名前の通りHikariCPの`HikariConfig`を構築するためのビルダーです。

```scala 3
val hikariConfig: com.zaxxer.hikari.HikariConfig = HikariConfigBuilder.default.build()
```

`HikariConfigBuilder`には`default`と`from`メソッドがあり`default`を使用した場合、LDBC指定のパスを元にConfigから対象の値を取得して`HikariConfig`の構築を行います。

```text
ldbc.hikari {
  jdbc_url = ...
  username = ...
  password = ...
}
```

ユーザー独自のパスを指定したい場合は`from`メソッドを使用して引数に取得したいパスを渡す必要があります。

```scala 3
val hikariConfig: com.zaxxer.hikari.HikariConfig = HikariConfigBuilder.from("custom.path").build()

// custom.path {
//   jdbc_url = ...
//   username = ...
//   password = ...
// }
```

HikariCPに設定できる内容は[公式](https://github.com/brettwooldridge/HikariCP)を参照してください。

Configに設定できるキーの一覧は以下になります。

| キー名                         | 説明                                                                     | 型        |
|-----------------------------|------------------------------------------------------------------------|----------|
| catalog                     | 接続時に設定するデフォルトのカタログ名                                                    | String   |
| connection_timeout          | クライアントがプールからの接続を待機する最大ミリ秒数                                             | Duration |
| idle_timeout                | 接続がプール内でアイドル状態であることを許可される最大時間 (ミリ秒単位)                                  | Duration |
| leak_detection_threshold    | 接続漏れの可能性を示すメッセージがログに記録されるまでに、接続がプールから外れる時間                             | Duration |
| maximum_pool_size           | アイドル接続と使用中の接続の両方を含め、プールが許容する最大サイズ                                      | Int      |
| max_lifetime                | プール内の接続の最大寿命                                                           | Duration |
| minimum_idle                | アイドル接続と使用中接続の両方を含め、HikariCPがプール内に維持しようとするアイドル接続の最小数                    | Int      |
| pool_name                   | 接続プールの名前                                                               | String   |
| allow_pool_suspension       | プール・サスペンドを許可するかどうか                                                     | Boolean  |
| auto_commit                 | プール内の接続のデフォルトの自動コミット動作                                                 | Boolean  |
| connection_init_sql         | 新しい接続が作成されたときに、その接続がプールに追加される前に実行されるSQL文字列                             | String   |
| connection_test_query       | 接続の有効性をテストするために実行する SQL クエリ                                            | String   |
| data_source_classname       | Connections の作成に使用する JDBC DataSourceの完全修飾クラス名                          | String   |
| initialization_fail_timeout | プール初期化の失敗タイムアウト                                                        | Duration |
| isolate_internal_queries    | 内部プール・クエリ (主に有効性チェック)を、`Connection.rollback()`によって独自のトランザクションで分離するかどうか | Boolean  |
| jdbc_url                    | JDBCのURL                                                               | String   |
| readonly                    | プールに追加する接続を読み取り専用接続として設定するかどうか                                         | Boolean  |
| register_mbeans             | HikariCPがJMXにHikariConfigMXBeanとHikariPoolMXBeanを自己登録するかどうか            | Boolean  |
| schema                      | 接続時に設定するデフォルトのスキーマ名                                                    | String   |
| username                    | `DataSource.getConnection(username,password)`の呼び出しに使用されるデフォルトのユーザ名     | String   |
| password                    | `DataSource.getConnection(username,password)`の呼び出しに使用するデフォルトのパスワード     | String   |
| driver_class_name           | 使用するDriverのクラス名                                                        | String   |
| transaction_isolation       | デフォルトのトランザクション分離レベル                                                    | String   |

`HikariDataSourceBuilder`を使用することで、HikariCPの`HikariDataSource`を構築することができます。

接続プールはライフタイムで管理されるオブジェクトでありきれいにシャットダウンする必要があるため、ビルダーによって構築された`HikariDataSource`は`Resource`として管理されます。

```scala 3
val dataSource: Resource[IO, HikariDataSource] = HikariDataSourceBuilder.default[IO].buildDataSource()
```

`buildDataSource`経由で構築された`HikariDataSource`は、内部でLDBC指定のパスを元にConfigから設定を取得し構築された`HikariConfig`を使用しています。
これは`HikariConfigBuilder`の`default`経由で生成された`HikariConfig`と同等のものです。

もしユーザー指定の`HikariConfig`を使用したい場合は、`buildFromConfig`を使用することで`HikariDataSource`を構築することができます。

```scala 3
val hikariConfig = ???
val dataSource = HikariDataSourceBuilder.default[IO].buildFromConfig(hikariConfig)
```

`HikariDataSourceBuilder`を使用して構築された`HikariDataSource`は通常IOAppを使用して実行します。

```scala 3
object HikariApp extends IOApp:

  val dataSourceResource: Resource[IO, HikariDataSource] = HikariDataSourceBuilder.default[IO].buildDataSource()

  def run(args: List[String]): IO[ExitCode] =
    dataSourceResource.use { dataSource =>
       ...
    }
```

### HikariDatabase

HikariCPのコネクション情報を持った`Database`を構築する方法も存在します。

`HikariDatabase`は`HikariDataSource`と同様に`Resource`として管理されます。
そのため通常はIOAppを使用して実行します。

```scala 3
object HikariApp extends IOApp:

  val hikariConfig = ???
  val databaseResource: Resource[F, Database[F]] = HikariDatabase.fromHikariConfig[IO](hikariConfig)

  def run(args: List[String]): IO[ExitCode] =
    databaseResource.use { database =>
       for
         result <- database.readOnly(...)
        yield ExitCode.Success
    }
```
