{%
  laika.title = 認証プラグイン
  laika.metadata.language = ja
%}

# 認証プラグイン

ldbcでは、デフォルトで提供されているMySQL認証プラグインと別でPure Scala3で実装されたAWS Aurora IAM認証などの認証プラグインが利用できます。

## サポートされている認証方式

デフォルトでは3つの認証方式がサポートされています。

- ネイティブプラガブル認証 (MysqlNativePasswordPlugin)
- SHA-256 プラガブル認証 (Sha256PasswordPlugin)
- SHA-2 プラガブル認証のキャッシュ (CachingSha2PasswordPlugin)

上記とは別でldbcには現在追加で2つの認証方式をサポートしています。

### 1. MySQL Clear Password認証

プレーンテキストパスワード認証を使用する場合：

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
  .setSSL(SSL.Trusted)  // セキュリティのためSSL必須
  .setDefaultAuthenticationPlugin(MysqlClearPasswordPlugin)
```

**注意**: セキュリティ上の理由から、Clear Password認証使用時はSSL/TLS接続が必須です。

### 2. AWS Aurora IAM認証 {#aws-aurora-iam-authentication}

AWS Aurora でIAM認証を使用する場合：

```scala
import ldbc.amazon.plugin.AwsIamAuthenticationPlugin
import ldbc.connector.*

val hostname = "aurora-cluster.cluster-xxx.ap-northeast-1.rds.amazonaws.com"
val username = "iam-user"

val config = MySQLConfig.default
  .setHost(hostname)
  .setUser(username)
  .setDatabase("production")
  .setSSL(SSL.Trusted)  // IAM認証にはSSL必須

val awsPlugin = AwsIamAuthenticationPlugin.default[IO]("ap-northeast-1", hostname, username)

MySQLDataSource.pooling[IO](config, plugins = List(awsPlugin)).use { datasource =>
  val connector = Connector.fromDataSource(datasource)
  // クエリ実行
}
```

## 認証プラグインの設定

複数の認証プラグインを組み合わせる場合：

```scala
val plugins = List(
  MysqlClearPasswordPlugin,
  awsIamPlugin
)

val datasource = MySQLDataSource.pooling[IO](config, plugins = plugins)
```

### 必要な依存関係

各認証プラグインを使用する場合は、対応する依存関係を追加してください：

**MySQL Clear Password認証:**
```scala
libraryDependencies += "@ORGANIZATION@" %% "ldbc-authentication-plugin" % "@VERSION@"
```

**AWS Aurora IAM認証:**
```scala
libraryDependencies += "@ORGANIZATION@" %% "ldbc-aws-authentication-plugin" % "@VERSION@"
```

## 認証プラグインのカスタマイズ

ldbcでは独自の認証プラグインを組み込むことが可能となっています。
現在サポートされていない例えばAzure Database for MySQLなどで使用するためのプラグインを作成して使用することが可能となっています。

認証プラグインの作成には`AuthenticationPlugin`を使用することで実現可能です。

```scala 3
class CustomAuthenticationPlugin extends AuthenticationPlugin[IO]:
  override def name: PluginName = ???
  override def requiresConfidentiality: Boolean = ???
  override def hashPassword(password: String, scramble: Array[Byte]): IO[ByteVector] = ???
```

認証プラグインで設定しなければいけないものは以下3つのプロパティです。

| プロパティ                     | 詳細              |
|---------------------------|-----------------|
| `name`                    | `プラグインの名前`      |
| `requiresConfidentiality` | `SSLを必須かするかどうか` |
| `requiresConfidentiality` | `パスワードの処理`      |

`name`は認証プラグインで使用するプラグイン名ですが、これはMySQLがサポートしているプラグイン名である必要があります。

現在設定可能なプラグインの種類は以下です。

| プラグイン名                       | 用途                                        |
|------------------------------|-------------------------------------------|
| `mysql_clear_password`       | `クリアテキストパスワード送信（クライアント側）`                 |
| `mysql_native_password`      | `MySQL従来のパスワード認証（4.1以降）`                  |
| `sha256_password`            | `SHA-256パスワード認証`                          |
| `caching_sha2_password`      | `SHA-256ベースのキャッシュ付き認証（MySQL 8.0デフォルト）`    |
| `mysql_old_password`         | `旧式パスワード認証（MySQL 4.1以前、5.7.5で廃止）`         |
| `authentication_windows`     | `Windows認証`                               |
| `authentication_pam`         | `PAM（Pluggable Authentication Modules）認証` |
| `authentication_ldap_simple` | `LDAP簡易認証`                                |
| `authentication_ldap_sasl`   | `LDAP SASL認証`                             |
| `authentication_kerberos`    | `Kerberos認証`                              |
| `authentication_fido`        | `FIDO/WebAuthn認証（パスワードレス）`                |
| `authentication_webauthn`    | `WebAuthn認証（FIDO2対応）`                     |
| `mysql_no_login`             | `ログイン不可アカウント用（プロキシ・ストアドプログラム用）`           |
| `test_plugin_server`         | `テスト用認証プラグイン`                             |
| `auth_socket`                | `UNIXソケット経由のOS認証`                         |

### AWS SDK for Java

認証プラグインのカスタマイズを利用することでAWS SDK for Javaを使用してAWS Aurora IAM認証を実現することも可能です。

```sbt
lazy val project = project.in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "rds" % "2.40.16",
      "software.amazon.awssdk" % "auth" % "2.40.16"
    ) 
  )
```

```scala 3
import cats.effect.IO
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.RdsUtilities
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest
import scodec.bits.ByteVector

import java.nio.charset.StandardCharsets

class AwsSdkIamAuthenticationPlugin(
  host: String,
  port: Int,
  user: String,
  region: Region
) extends AuthenticationPlugin[IO]:

  override def name: PluginName = MYSQL_CLEAR_PASSWORD

  override def requiresConfidentiality: Boolean = true

  override def hashPassword(password: String, scramble: Array[Byte]): IO[ByteVector] =
    IO.blocking {
      val credentialsProvider = DefaultCredentialsProvider.builder().build()

      val rdsUtilities = RdsUtilities.builder()
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build()

      val request = GenerateAuthenticationTokenRequest.builder()
        .hostname(host)
        .port(port)
        .username(user)
        .build()

      val authToken = rdsUtilities.generateAuthenticationToken(request)

      ByteVector(authToken.getBytes(StandardCharsets.UTF_8)) :+ 0.toByte
    }
```
