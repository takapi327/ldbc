{%
  laika.title = マイグレーションノート
  laika.metadata.language = ja
%}

# マイグレーションノート (0.4.xから0.5.xへの移行)

## パッケージ

**新しく追加されたパッケージ**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

**廃止されたパッケージ**

| Module / Platform    | JVM | Scala Native | Scala.js |  
|----------------------|:---:|:------------:|:--------:|
| `ldbc-hikari`        |  ✅  |      ❌       |    ❌     | 

**全てのパッケージ**

| Module / Platform                | JVM | Scala Native | Scala.js | Scaladoc                                                                                                                                                              |
|----------------------------------|:---:|:------------:|:--------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ldbc-sql`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-sql_3)                       |
| `ldbc-core`                      |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)                      |
| `ldbc-connector`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-connector_3)                 |
| `jdbc-connector`                 |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/jdbc-connector_3)                 |
| `ldbc-dsl`                       |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3)                       |
| `ldbc-statement`                 |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-statement_3)                 |
| `ldbc-query-builder`             |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-query-builder_3)             |
| `ldbc-schema`                    |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-schema_3)                    |
| `ldbc-codegen`                   |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-codegen_3)                   |
| `ldbc-plugin`                    |  ✅  |      ❌       |    ❌     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-plugin_2.12_1.0)             |
| `ldbc-zio-interop`               |  ✅  |      ❌       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-zio-interop_3)               |
| `ldbc-authentication-plugin`     |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-authentication-plugin_3)     |
| `ldbc-aws-authentication-plugin` |  ✅  |      ✅       |    ✅     | [![Scaladoc](https://img.shields.io/badge/javadoc-0.5.0-brightgreen.svg?label=Scaladoc)](https://javadoc.io/doc/io.github.takapi327/ldbc-aws-authentication-plugin_3) |

## 🎯 主要な変更点

### 1. ZIOエコシステムサポートの追加

0.5.0から、ldbcでZIOをより簡単に使用できるようになりました。

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-zio-interop" % "0.5.0"
```

**使用例:**
```scala
import zio.*
import ldbc.zio.interop.*
import ldbc.connector.*
import ldbc.dsl.*

object Main extends ZIOAppDefault:
  
  private val datasource =
    MySQLDataSource
      .build[Task](
        host = "127.0.0.1",
        port = 3306,
        user = "ldbc"
      )
      .setPassword("password")
      .setDatabase("world")

  private val program =
    for
      connection <- datasource.getConnection
      connector = Connector.fromConnection(connection)
      result     <- sql"SELECT 1".query[Int].to[List].readOnly(connector)
    yield result

  override def run = program
```

### 2. 認証プラグインの強化

新しい認証プラグインモジュールが追加されました。これらのプラグインはPure Scala3で実装され、全てのプラットフォーム（JVM、JS、Native）で利用可能です。

#### MySQL Clear Password認証

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-authentication-plugin" % "0.5.0"
```

```scala
import ldbc.connector.*
import ldbc.authentication.plugin.*

val datasource = MySQLDataSource
  .build[IO](
    host = "localhost",
    port = 3306,
    user = "cleartext-user"
  )
  .setPassword("plaintext-password")
  .setDatabase("mydb")
  .setDefaultAuthenticationPlugin(MysqlClearPasswordPlugin)
```

#### AWS Aurora IAM認証

```scala
libraryDependencies += "io.github.takapi327" %% "ldbc-aws-authentication-plugin" % "0.5.0"
```

```scala
import ldbc.amazon.plugin.AwsIamAuthenticationPlugin
import ldbc.connector.*

val hostname = "aurora-instance.cluster-xxx.region.rds.amazonaws.com"
val username = "iam-user"
val config = MySQLConfig.default
  .setHost(hostname)
  .setUser(username)
  .setDatabase("mydb")
  .setSSL(SSL.Trusted)  // IAM認証にはSSLが必要

val plugin = AwsIamAuthenticationPlugin.default[IO]("ap-northeast-1", hostname, username)

MySQLDataSource.pooling[IO](config, plugins = List(plugin)).use { datasource =>
  val connector = Connector.fromDataSource(datasource)
  // クエリ実行
}
```

### 3. ldbc-hikariの廃止

`ldbc-hikari`は0.5.0で正式に廃止されました。組み込みのコネクションプールを使用してください。

**廃止されたAPI:**
```scala
// 使用不可
import ldbc.hikari.*
```

**推奨される方法:**
```scala
import ldbc.connector.*

val poolConfig = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("user")
  .setPassword("password")
  .setDatabase("mydb")
  .setMinConnections(5)
  .setMaxConnections(20)

MySQLDataSource.pooling[IO](poolConfig).use { pool =>
  val connector = Connector.fromDataSource(pool)
  // クエリ実行
}
```

### 4. セキュリティ強化

#### SQLパラメータのエスケープ処理強化

文字列パラメータのエスケープ処理が改善され、SQLインジェクション攻撃への対策が強化されました。

#### SSRF攻撃対策

データソース設定時のエンドポイント検証が追加されました。

```scala
// 安全でないエンドポイントは自動的に検出・拒否される
val datasource = MySQLDataSource
  .build[IO]("suspicious-host", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")
```

### 5. パフォーマンス最適化

#### 最大パケットサイズの設定

MySQLサーバーの`max_allowed_packet`設定との互換性が改善されました。

```scala
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("mydb")
  .setMaxPacketSize(16777216)  // 16MB（MySQLサーバー設定と合わせる）
```

#### コネクションプールの同時性改善

コネクションプールの状態管理がアトミックチェックにより改善され、並行環境での安定性が向上しました。

### 6. API の改善

#### ファイルからのクエリ実行

新しい`updateRaws`メソッドが追加され、ファイルからSQLクエリを読み込んで実行できるようになりました：

```scala
import ldbc.dsl.*
import fs2.io.file.{ Files, Path }
import fs2.text

private def readFile(filename: String): IO[String] =
  Files[IO]
    .readAll(Path(filename))
    .through(text.utf8.decode)
    .compile
    .string

for
  sql <- readFile("hoge.sql")
  _ <- DBIO.updateRaws(sql).commit(connector)
yield ()
```

## 移行ガイド

### ldbc-hikariからの移行

**移行前 (0.4.x):**
```scala
libraryDependencies ++= Seq(
  "io.github.takapi327" %% "ldbc-dsl" % "0.4.0",
  "io.github.takapi327" %% "ldbc-hikari" % "0.4.0"
)

import ldbc.hikari.*

val hikariConfig = Configuration.default
  .setJdbcUrl("jdbc:mysql://localhost:3306/mydb")
  .setUsername("user")
  .setPassword("password")
  .setMaximumPoolSize(20)

HikariDataSource.fromHikariConfig[IO](hikariConfig).use { pool =>
  // 使用
}
```

**移行後 (0.5.x):**
```scala
libraryDependencies ++= Seq(
  "io.github.takapi327" %% "ldbc-connector" % "0.5.0",
  "io.github.takapi327" %% "ldbc-dsl" % "0.5.0"
)

import ldbc.connector.*

val config = MySQLConfig.default
  .setHost("localhost")
  .setPort(3306)
  .setUser("user")
  .setPassword("password")
  .setDatabase("mydb")
  .setMaxConnections(20)

MySQLDataSource.pooling[IO](config).use { pool =>
  val connector = Connector.fromDataSource(pool)
  // 使用
}
```

### ZIO統合の追加

**Cats Effectベース (従来):**
```scala
import cats.effect.*
import ldbc.connector.*

val program: IO[List[User]] = 
  datasource.getConnection.use { connection =>
    val connector = Connector.fromConnection(connection)
    sql"SELECT * FROM users".query[User].to[List].readOnly(connector)
  }
```

**ZIOベース (新規):**
```scala
import zio.*
import ldbc.zio.interop.*

val program: Task[List[User]] = 
  datasource.getConnection.use { connection =>
    val connector = Connector.fromConnection(connection)
    sql"SELECT * FROM users".query[User].to[List].readOnly(connector)
  }
```

## まとめ

0.5.x への移行により、以下のメリットが得られます：

1. **ZIOエコシステムの完全サポート**: ZIOベースアプリケーションでの自然な統合
2. **強化されたセキュリティ**: SSRF攻撃対策、SQLエスケープ処理の改善
3. **AWS統合の簡素化**: Aurora IAM認証の簡単な設定
4. **パフォーマンスの向上**: 最大パケットサイズ対応、プール並行性改善
5. **開発者体験の向上**: 新しいAPI、バイナリ互換性チェック

移行作業は主にライブラリ依存関係の更新とAPIの小さな変更で完了し、段階的な移行が可能です。
