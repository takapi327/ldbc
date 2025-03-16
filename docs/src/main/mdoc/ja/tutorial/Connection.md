{%
  laika.title = コネクション
  laika.metadata.language = ja
%}

# コネクション

前のページで[セットアップ](/ja/tutorial/Setup.md)は完了しました。このページでは、データベースに安全に接続するための方法を詳しく学びます。

ldbcでは、データベース接続を管理するための「コネクション」という概念が中心的な役割を果たします。コネクションはデータベースへの接続を開始し、クエリを実行し、接続を安全に閉じるためのリソースを提供します。

ldbcでは2種類の接続方法をサポートしています：
- jdbcコネクタ - 標準的なJDBCドライバを使用
- ldbcコネクタ - ldbcが独自に最適化したコネクタ

それぞれの接続方法について見ていきましょう。

## Use jdbc connector

まず、依存関係を追加します。

jdbcコネクタを使用する場合、MySQLのコネクタも追加する必要があります。

```scala
//> dep "@ORGANIZATION@::jdbc-connector:@VERSION@"
//> dep "com.mysql":"mysql-connector-j":"@MYSQL_VERSION@"
```

次に、`MysqlDataSource`を使用してデータソースを作成します。

```scala
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")
```

作成したデータソースを使用してコネクションを管理するプロバイダーを作成します。

```scala
import jdbc.connector.*
val provider =
  MySQLProvider
    .fromDataSource[IO](ds, ExecutionContexts.synchronous)
```

## Use ldbc connector

まず、依存関係を追加します。

```scala
//> dep "@ORGANIZATION@::ldbc-connector:@VERSION@"
```

そして`Provider`を作成します。

```scala 3
import ldbc.connector.*
val provider =
  MySQLProvider
    .default[IO]("127.0.0.1", 3306, "ldbc", "password", "ldbc")
    .setSSL(SSL.Trusted)
```

コネクションを設定するためのパラメータは以下の通りです。

| プロパティ                     | 詳細                                                            | 必須 |
|---------------------------|---------------------------------------------------------------|----|
| `host`                    | `データベースホスト情報`                                                 | ✅  |
| `port`                    | `データベースポート情報`                                                 | ✅  |
| `user`                    | `データベースユーザー情報`                                                | ✅  |
| `password`                | `データベースパスワード情報 (default: None)`                               | ❌  |
| `database`                | `データベース名情報 (default: None)`                                   | ❌  |
| `debug`                   | `デバッグ情報を表示するかどうか  (default: false)`                           | ✅  |
| `ssl`                     | `SSLの設定 (default: SSL.None)`                                  | ✅  |
| `socketOptions`           | `TCP/UDP ソケットのソケットオプションを指定する (default: defaultSocketOptions)` | ✅  |
| `readTimeout`             | `タイムアウト時間を指定する (default: Duration.Inf)`                       | ✅  |
| `allowPublicKeyRetrieval` | `公開鍵を取得するかどうか (default: false)`                               | ✅  |
| `logHandler`              | `ログ出力設定`                                                      | ❌  |
| `before`                  | `コネクション確立後に実行する処理`                                            | ❌  |
| `after`                   | `コネクションを切断する前に実行する処理`                                         | ❌  |
