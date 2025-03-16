{%
  laika.title = コネクション
  laika.metadata.language = ja
%}

# コネクション

前のページで[セットアップ](/ja/tutorial/Setup.md)は完了しました。このページでは、データベースに安全に接続するための方法を詳しく学びます。

ldbcでは、データベース接続を管理するための「コネクション」という概念が中心的な役割を果たします。コネクションはデータベースへの接続を開始し、クエリを実行し、接続を安全に閉じるためのリソースを提供します。この接続管理は、cats-effectのResource型を用いて安全に行われます。

## コネクタの種類

ldbcでは2種類の接続方法をサポートしています。

**jdbcコネクタ** - 標準的なJDBCドライバを使用した接続方法

- 既存のJDBCドライバをそのまま活用できる
- JDBCに慣れている開発者に親しみやすい
- 他のJDBCベースのツールとの連携が容易

**ldbcコネクタ** - ldbcが独自に最適化した専用コネクタ

- MySQLプロトコルに最適化されたパフォーマンス
- 機能面での拡張性が高い
- より多くの設定オプションが利用可能

それぞれの接続方法について詳しく見ていきましょう。

## JDBCコネクタの使用

JDBCコネクタは標準的なJDBCドライバを利用して接続を確立します。既存のJDBC知識を活かしたい場合や、他のJDBCベースのツールとの互換性を重視する場合におすすめです。

### 依存関係の追加

まず、必要な依存関係を追加します。jdbcコネクタを使用する場合は、MySQLのコネクタも一緒に追加する必要があります。

```scala
//> dep "@ORGANIZATION@::jdbc-connector:@VERSION@"
//> dep "com.mysql":"mysql-connector-j":"@MYSQL_VERSION@"
```

### DataSourceを用いたコネクション

最も一般的な方法は`DataSource`を使用する方法です。これにより接続プールなどの高度な機能も利用できます。

```scala
// 必要なインポート
import cats.effect.IO
import jdbc.connector.*

// MySQLのデータソースを設定
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")

// コネクションプロバイダーを作成
val provider = MySQLProvider
  .fromDataSource[IO](ds, ExecutionContexts.synchronous)

// コネクションを使用する
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### DriverManagerを用いたコネクション

`DriverManager`を使用して接続することも可能です。シンプルなアプリケーションや、スクリプト実行には便利です。

```scala
// 必要なインポート
import cats.effect.IO
import jdbc.connector.*

// DriverManagerからプロバイダーを作成
val provider = MySQLProvider
  .fromDriverManager[IO]
  .apply(
    "com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://127.0.0.1:13306/world",
    "ldbc",
    "password",
    None // ログハンドラーは省略可能
  )

// コネクションを使用する
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### 既存のConnectionを使用

すでに確立されている`java.sql.Connection`オブジェクトがある場合は、それをラップして使用することもできます：

```scala
// 既存のjava.sql.Connection
val jdbcConnection: java.sql.Connection = ???

// ldbcのコネクションに変換
val provider = MySQLProvider.fromConnection[IO](jdbcConnection)

// コネクションを使用する
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

## ldbcコネクタの使用

ldbcコネクタはldbcが独自に開発した最適化されたコネクタで、より多くの設定オプションと柔軟性を提供します。

### 依存関係の追加

まず、必要な依存関係を追加します。

```scala
//> dep "@ORGANIZATION@::ldbc-connector:@VERSION@"
```

### 基本的な設定でのコネクション

最もシンプルな設定から始めましょう：

```scala
import cats.effect.IO
import ldbc.connector.*

// 基本的な設定でプロバイダーを作成
val provider = MySQLProvider
  .default[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")

// コネクションを使用する
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### SSL設定を使用したコネクション

セキュアな接続を確立するためにSSL設定を追加できます：

```scala
import cats.effect.IO
import ldbc.connector.*

val provider = MySQLProvider
  .default[IO]("localhost", 3306, "ldbc", "password", "world")
  .setSSL(SSL.Trusted) // SSL接続を有効化

// コネクションを使用する
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### 高度な設定を使用したコネクション

さらに多くの設定オプションを活用することができます：

```scala
import scala.concurrent.duration.*
import cats.effect.IO
import fs2.io.net.SocketOption
import ldbc.connector.*

val provider = MySQLProvider
  .default[IO]("localhost", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setDebug(true)
  .setSSL(SSL.None)
  .addSocketOption(SocketOption.receiveBufferSize(4096))
  .setReadTimeout(30.seconds)
  .setAllowPublicKeyRetrieval(true)
  .setLogHandler(customLogHandler) // 独自のログハンドラを設定

// コネクションを使用する
val program = provider.use { connection =>
  connection.execute("SELECT 1")
}
```

### Before/After処理の追加

コネクション確立後、または切断前に特定の処理を実行したい場合は、`withBefore`と`withAfter`メソッドを使用できます：

```scala
import cats.effect.IO
import ldbc.connector.*

val provider = MySQLProvider
  .default[IO]("localhost", 3306, "ldbc", "password", "world")
  .withBefore { connection =>
    // コネクション確立後に実行される処理
    connection.execute("SET time_zone = '+09:00'")
  }
  .withAfter { (result, connection) =>
    // コネクション切断前に実行される処理
    connection.execute("RESET time_zone")
  }

// コネクションを使用する
val program = provider.use { connection =>
  connection.execute("SELECT NOW()")
}
```

## 設定可能なパラメータ一覧

ldbcコネクタでは以下のパラメータが設定可能です：

| プロパティ                     | 詳細                                                            | 必須 |
|---------------------------|---------------------------------------------------------------|----|
| `host`                    | `データベースホスト情報`                                                 | ✅  |
| `port`                    | `データベースポート情報`                                                 | ✅  |
| `user`                    | `データベースユーザー情報`                                                | ✅  |
| `password`                | `データベースパスワード情報 (default: None)`                               | ❌  |
| `database`                | `データベース名情報 (default: None)`                                   | ❌  |
| `debug`                   | `デバッグ情報を表示するかどうか  (default: false)`                           | ❌  |
| `ssl`                     | `SSLの設定 (default: SSL.None)`                                  | ❌  |
| `socketOptions`           | `TCP/UDP ソケットのソケットオプションを指定する (default: defaultSocketOptions)` | ❌  |
| `readTimeout`             | `タイムアウト時間を指定する (default: Duration.Inf)`                       | ❌  |
| `allowPublicKeyRetrieval` | `公開鍵を取得するかどうか (default: false)`                               | ❌  |
| `logHandler`              | `ログ出力設定`                                                      | ❌  |
| `before`                  | `コネクション確立後に実行する処理`                                            | ❌  |
| `after`                   | `コネクションを切断する前に実行する処理`                                         | ❌  |
| `tracer`                  | `メトリクス出力用のトレーサー設定 (default: Tracer.noop)`                     | ❌  |

## リソース管理とコネクションの利用

ldbcではcats-effectのResourceを使用してコネクションのライフサイクルを管理します。以下の2つの方法でコネクションを利用できます：

### useメソッド

シンプルに使う場合は`use`メソッドが便利です：

```scala
val result = provider.use { connection =>
  // コネクションを利用した処理
  connection.execute("SELECT * FROM users")
}
```

### createConnectionメソッド

より細かいリソース管理が必要な場合は`createConnection`メソッドを使用します：

```scala 3
val program = for
  result <- provider.createConnection().use { connection =>
    // コネクションを利用した処理
    connection.execute("SELECT * FROM users")
  }
  // 他の処理...
yield result
```

これらの方法を用いることで、コネクションのオープン/クローズを安全に管理しながらデータベース操作を行うことができます。
