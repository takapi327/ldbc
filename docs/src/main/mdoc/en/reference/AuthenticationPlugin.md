{%
  laika.title = Authentication Plugin
  laika.metadata.language = en
%}

# Authentication Plugin

In ldbc, you can use authentication plugins implemented in pure Scala3, such as AWS Aurora IAM authentication, in addition to the MySQL authentication plugins provided by default.

## Supported Authentication Methods

By default, three authentication methods are supported.

- Native Pluggable Authentication (MysqlNativePasswordPlugin)
- SHA-256 Pluggable Authentication (Sha256PasswordPlugin)
- Caching SHA-2 Pluggable Authentication (CachingSha2PasswordPlugin)

Apart from the above, ldbc currently supports an additional 2 authentication methods.

### 1. MySQL Clear Password Authentication

When using plain text password authentication:

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
  .setSSL(SSL.Trusted)  // SSL required for security
  .setDefaultAuthenticationPlugin(MysqlClearPasswordPlugin)
```

**Note**: For security reasons, SSL/TLS connection is required when using Clear Password authentication.

### 2. AWS Aurora IAM Authentication {#aws-aurora-iam-authentication}

When using IAM authentication with AWS Aurora:

```scala
import ldbc.amazon.plugin.AwsIamAuthenticationPlugin
import ldbc.connector.*

val hostname = "aurora-cluster.cluster-xxx.ap-northeast-1.rds.amazonaws.com"
val username = "iam-user"

val config = MySQLConfig.default
  .setHost(hostname)
  .setUser(username)
  .setDatabase("production")
  .setSSL(SSL.Trusted)  // SSL required for IAM authentication

val awsPlugin = AwsIamAuthenticationPlugin.default[IO]("ap-northeast-1", hostname, username)

MySQLDataSource.pooling[IO](config, plugins = List(awsPlugin)).use { datasource =>
  val connector = Connector.fromDataSource(datasource)
  // Query execution
}
```

## Authentication Plugin Configuration

When combining multiple authentication plugins:

```scala
val plugins = List(
  MysqlClearPasswordPlugin,
  awsIamPlugin
)

val datasource = MySQLDataSource.pooling[IO](config, plugins = plugins)
```

### Required Dependencies

When using each authentication plugin, add the corresponding dependencies:

**MySQL Clear Password Authentication:**
```scala
libraryDependencies += "@ORGANIZATION@" %% "ldbc-authentication-plugin" % "@VERSION@"
```

**AWS Aurora IAM Authentication:**
```scala
libraryDependencies += "@ORGANIZATION@" %% "ldbc-aws-authentication-plugin" % "@VERSION@"
```

## Authentication Plugin Customization

ldbc allows you to integrate custom authentication plugins.
It is possible to create and use plugins for services that are not currently supported, such as Azure Database for MySQL.

Creating authentication plugins can be achieved by using `AuthenticationPlugin`.

```scala 3
class CustomAuthenticationPlugin extends AuthenticationPlugin[IO]:
  override def name: PluginName = ???
  override def requiresConfidentiality: Boolean = ???
  override def hashPassword(password: String, scramble: Array[Byte]): IO[ByteVector] = ???
```

There are three properties that must be configured for authentication plugins:

| Property                  | Details                    |
|---------------------------|----------------------------|
| `name`                    | Plugin name                |
| `requiresConfidentiality` | Whether SSL is required    |
| `hashPassword`            | Password processing        |

`name` is the plugin name used by the authentication plugin, which must be a plugin name supported by MySQL.

The currently configurable plugin types are as follows:

| Plugin Name                  | Purpose                                                          |
|------------------------------|------------------------------------------------------------------|
| `mysql_clear_password`       | Clear text password transmission (client side)                  |
| `mysql_native_password`      | MySQL traditional password authentication (4.1 and later)       |
| `sha256_password`            | SHA-256 password authentication                                  |
| `caching_sha2_password`      | SHA-256 based authentication with caching (MySQL 8.0 default)   |
| `mysql_old_password`         | Legacy password authentication (MySQL 4.1 and earlier, deprecated in 5.7.5) |
| `authentication_windows`     | Windows authentication                                           |
| `authentication_pam`         | PAM (Pluggable Authentication Modules) authentication           |
| `authentication_ldap_simple` | LDAP simple authentication                                       |
| `authentication_ldap_sasl`   | LDAP SASL authentication                                         |
| `authentication_kerberos`    | Kerberos authentication                                          |
| `authentication_fido`        | FIDO/WebAuthn authentication (passwordless)                     |
| `authentication_webauthn`    | WebAuthn authentication (FIDO2 compatible)                      |
| `mysql_no_login`             | For non-login accounts (for proxy/stored programs)              |
| `test_plugin_server`         | Test authentication plugin                                       |
| `auth_socket`                | OS authentication via UNIX socket                               |

### AWS SDK for Java

By utilizing authentication plugin customization, you can also implement AWS Aurora IAM authentication using the AWS SDK for Java.

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