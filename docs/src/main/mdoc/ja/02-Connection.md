# コネクション

この章では、データベースに接続するためのコネクション構築方法について説明します。

データベースに接続するためには、コネクションを構築する必要がある。コネクションは、データベースへの接続を管理するためのリソースである。コネクションは、データベースへの接続を開始し、クエリを実行し、接続を閉じるためのリソースを提供する。

ldbcはjdbcとldbc独自のコネクタのどちらかを使ってデータベースに接続する。どちらを使うかは設定する依存関係によって決まる。

## Use jdbc connector

まず、`build.sbt`に依存関係を追加します。

jdbcコネクタを使用する場合、MySQLのコネクタも追加する必要があります。

@@@ vars
```scala
libraryDependencies ++= Seq(
  "$org$" %% "jdbc-connector" % "$version$",
  "com.mysql" % "mysql-connector-j" % "$mysqlVersion$"
)
```
@@@

次に、`MysqlDataSource`を使用してデータソースを作成します。

```scala
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")
```

作成したデータソースを使用してjdbcコネクタのデータソースを作成します。

```scala
val datasource = jdbc.connector.MysqlDataSource[IO](ds)
```

最後に、jdbcコネクタを使用してコネクションを作成します。

```scala
val connection: Resource[IO, Connection[IO]] =
  Resource.make(datasource.getConnection)(_.close())
```

ここではCats Effectの`Resource`を使用してコネクション使用後にクローズするようにしています。

## Use ldbc connector

まず、`build.sbt`に依存関係を追加します。

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-connector" % "$version$"
```
@@@

次に、Tracerを提供します。ldbcコネクタはTracerを使用してテレメトリデータの収集を行います。 これらは、アプリケーショントレースを記録するために使用されます。

ここでは、`Tracer.noop`を使用してTracerを提供します。

```scala
given Tracer[IO] = Tracer.noop[IO]
```

最後に、`Connection`を作成します。

```scala
val connection: Resource[IO, Connection[IO]] =
  ldbc.connector.Connection[IO](
    host     = "127.0.0.1",
    port     = 3306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("ldbc")
  )
```

コネクションを設定するためのパラメータは以下の通りです。

| プロパティ                    | 詳細                                                           | 必須 |
|--------------------------|--------------------------------------------------------------|----|
| host                     | データベースホスト情報                                                  | ✅  |
| port                     | データベースポート情報                                                  | ✅  |
| user                     | データベースユーザー情報                                                 | ✅  |
| password                 | データベースパスワード情報 (default: None)                                | ❌  |
| database                 | データベース名情報 (default: None)                                    | ❌  |
| debug                    | デバッグ情報を表示するかどうか  (default: false)                            | ✅  |
| ssl                      | SSLの設定 (default: SSL.None)                                   | ✅  |
| socketOptions            | TCP/ UDP ソケットのソケットオプションを指定する (default: defaultSocketOptions) | ✅  |
| readTimeout              | タイムアウト時間を指定する (default: Duration.Inf)                        | ✅  |
| allowPublicKeyRetrieval  | 公開鍵を取得するかどうか (default: false)                                | ✅  |