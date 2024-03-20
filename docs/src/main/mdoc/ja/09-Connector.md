# コネクタ

この章では、LDBC独自のMySQLコネクタを使用したデータベース接続について説明します。

ScalaでMySQLデータベースへの接続を行うためにはJDBCを使用する必要があります。JDBCはJavaの標準APIであり、Scalaでも使用することができます。
JDBCはJavaで実装が行われているためScalaで使用する場合でもJVM環境でのみ動作することができます。

昨今のScalaを取り巻く環境はJSやNativeなどの環境でも動作できるようプラグインの開発が盛んに行われています。
ScalaはJavaの資産を使用できるJVMのみで動作する言語から、マルチプラットフォーム環境でも動作できるよう進化を続けています。

しかし、JDBCはJavaの標準APIでありScalaのマルチプラットフォーム環境での動作をサポートしていません。

そのため、ScalaでアプリケーションをJS, Nativeなどで動作できるように作成を行ったとしてもJDBCを使用できないため、MySQLなどのデータベースへ接続を行うことができません。

Typelevel Projectには[Skunk](https://github.com/typelevel/skunk)と呼ばれる[PostgreSQL](https://www.postgresql.org/)用のScalaライブラリが存在します。
このプロジェクトはJDBCを使用しておらず、純粋なScalaのみでPostgreSQLへの接続を実現しています。そのため、Skunkを使用すればJVM, JS, Native環境を問わずPostgreSQLへの接続を行うことができます。

LDBC コネクタはこのSkunkに影響を受けてJVM, JS, Native環境を問わずMySQLへの接続を行えるようにするために開発が行われてるプロジェクトです。

※ このコネクタは現在実験的な機能となります。そのため本番環境での使用しないでください。

LDBCコネクタは一番低レイヤーのAPIとなります。
今後このコネクタを使用してより高レイヤーのAPIを提供する予定です。また既存の高レイヤーのAPIとの互換性を持たせることも予定しています。

使用するにはプロジェクトに以下の依存関係を設定する必要があります。

**JVM**

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-connector" % "$version$"
```
@@@

**JS/Native**

@@@ vars
```scala
libraryDependencies += "$org$" %%% "ldbc-connector" % "$version$"
```
@@@

**サポートバージョン**

現在のバージョンは以下のバージョンのMySQLをサポートしています。

- MySQL 5.7.x
- MySQL 8.x

メインサポートはMySQL 8.xです。MySQL 5.7.xはサブサポートとなります。そのためMySQL 5.7.xでの動作には注意が必要です。
将来的にはMySQL 5.7.xのサポートは終了する予定です。

## 接続

LDBCコネクタを使用してMySQLへの接続を行うためには、`Connection`を使用します。

また、`Connection`はオブザーバビリティを意識した開発を行えるように`Otel4s`を使用してテレメトリデータを収集できるようにしています。
そのため、`Connection`を使用する際には`Otel4s`の`Tracer`を設定する必要があります。

開発時やトレースを使用したテレメトリデータが不要な場合は`Tracer.noop`を使用することを推奨します。

```scala
import cats.effect.IO
import org.typelevel.otel4s.trace.Tracer
import ldbc.connector.Connection

given Tracer[IO] = Tracer.noop[IO]

val connection = Connection[IO](
  host = "127.0.0.1",
  port = 3306,
  user = "root",
)
```

以下は`Connection`構築時に設定できるプロパティの一覧です。

| プロパティ                   | 型                  | 用途                                                     |
|-------------------------|--------------------|--------------------------------------------------------|
| host                    | String             | MySQLサーバーのホストを指定します                                    |
| port                    | Int                | MySQLサーバーのポート番号を指定します                                  |
| user                    | String             | MySQLサーバーへログインを行うユーザー名を指定します                           |
| password                | Option[String]     | MySQLサーバーへログインを行うユーザーのパスワードを指定します                      |
| database                | Option[String]     | MySQLサーバーへ接続後に使用するデータベース名を指定します                        |
| debug                   | Boolean            | 処理のログを出力します。デフォルトはfalseです                              |
| ssl                     | SSL                | MySQLサーバーとの通知んでSSL/TLSを使用するかを指定します。デフォルトはSSL.Noneです    |
| socketOptions           | List[SocketOption] | TCP/UDP ソケットのソケットオプションを指定します。                          |
| readTimeout             | Duration           | MySQLサーバーへの接続を試みるまでのタイムアウトを指定します。デフォルトはDuration.Infです。 |
| allowPublicKeyRetrieval | Boolean            | MySQLサーバーとの認証時にRSA公開鍵を使用するかを指定します。デフォルトはfalseです。       |

`Connection`は`Resource`を使用してリソース管理を行います。そのためコネクション情報を使用する場合は`use`メソッドを使用してリソースの管理を行います。

```scala
connection.use { conn =>
  // コードを記述
}
```

### 認証

MySQLでの認証は、クライアントがMySQLサーバーへ接続するときにLoginRequestというフェーズでユーザ情報を送信します。そして、サーバー側では送られたユーザが`mysql.user`テーブルに存在するか検索を行い、どの認証プラグインを使用するかを決定します。認証プラグインが決定した後にサーバーはそのプラグインを呼び出してユーザー認証を開始し、その結果をクライアント側に送信します。このようにMySQLでは認証がプラガブル（様々なタイプのプラグインを付け外しできる）になっています。

MySQLでサポートされている認証プラグインは[公式ページ](https://dev.mysql.com/doc/refman/8.0/ja/authentication-plugins.html)に記載されています。

LDBCは現時点で以下の認証プラグインをサポートしています。

- ネイティブプラガブル認証
- SHA-256 プラガブル認証
- SHA-2 プラガブル認証のキャッシュ

※ ネイティブプラガブル認証とSHA-256 プラガブル認証はMySQL 8.xから非推奨となったプラグインです。特段理由がない場合はSHA-2 プラガブル認証のキャッシュを使用することを推奨します。

LDBCのアプリケーションコード上で認証プラグインを意識する必要はありません。ユーザーはMySQLのデータベース上で使用したい認証プラグインで作成されたユーザーを作成し、LDBCのアプリケーションコード上ではそのユーザーを使用してMySQLへの接続を試みるだけで問題ありません。
LDBCが内部で認証プラグインを判断し、適切な認証プラグインを使用してMySQLへの接続を行います。

## 実行

以降の処理では以下テーブルを使用しているものとします。

```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  age INT NULL
);
```

### Statement

`Statement`は動的なパラメーターを使用しないSQLを実行するためのAPIです。

※ `Statement`は動的なパラメーターを使用しないため、使い方によってはSQLインジェクションのリスクがあります。そのため、動的なパラメーターを使用する場合は`PreparedStatement`を使用することを推奨します。

`Connection`の`createStatement`メソッドを使用して`Statement`を構築します。

#### 読み取りクエリ

読み取り専用のSQLを実行する場合は`executeQuery`メソッドを使用します。

クエリを実行した結果MySQLサーバーから返される値は`ResultSet`に格納されて戻り値として返却されます。

```scala
connection.use { conn =>
  val statement: Statement[IO] = conn.createStatement("SELECT * FROM users")
  val result: IO[ResultSet] = statement.executeQuery()
  // ResultSetを使用した処理
}
```

#### 書き込みクエリ

書き込みを行うSQLを実行する場合は`executeUpdate`メソッドを使用します。

クエリを実行した結果MySQLサーバーから返される値は影響を受けた行数が戻り値として返却されます。

```scala
connection.use { conn =>
  val statement: Statement[IO] = conn.createStatement("INSERT INTO users (name, age) VALUES ('Alice', 20)")
  val result: IO[Int] = statement.executeUpdate()
}
```

#### AUTO_INCREMENTの値を取得

`Statement`を使用してクエリ実行後にAUTO_INCREMENTの値を取得する場合は`returningAutoGeneratedKey`メソッドを使用します。

クエリを実行した結果MySQLサーバーから返される値はAUTO_INCREMENTに生成された値が戻り値として返却されます。

```scala
connection.use { conn =>
  val statement: Statement[IO] = conn.createStatement("INSERT INTO users (name, age) VALUES ('Alice', 20)")
  val result: IO[Int] = statement.returningAutoGeneratedKey()
}
```

### Client/Server PreparedStatement

LDBCでは`PreparedStatement`を`Client PreparedStatement`と`Server PreparedStatement`に分けて提供しています。

`Client PreparedStatement`は動的なパラメーターを使用してアプリケーション上でSQLの構築を行い、MySQLサーバーに送信を行うためのAPIです。
そのためMySQLサーバーへのクエリ送信方法は`Statement`と同じになります。

このAPIはJDBCの`PreparedStatement`に相当します。

より安全なMySQLサーバー内でクエリを構築するための`PreparedStatement`は`Server PreparedStatement`で提供されますので、そちらを使用してください。

`Server PreparedStatement`は実行を行うクエリをMySQLサーバー内で事前に準備を行い、アプリケーション上でパラメーターを設定して実行を行うためのAPIです。

`Server PreparedStatement`では実行するクエリの送信とパラメーターの送信が分けて行われるため、クエリの再利用が可能となります。

`Server PreparedStatement`を使用する場合事前にクエリをMySQLサーバーで準備します。格納するためにMySQLサーバーはメモリを使用しますが、クエリの再利用が可能となるため、パフォーマンスの向上が期待できます。

しかし、事前準備されたクエリは解放されるまでメモリを使用し続けるため、メモリリークのリスクがあります。

`Server PreparedStatement`を使用する場合は`close`メソッドを使用して適切にクエリの解放を行う必要があります。

#### Client PreparedStatement

`Connection`の`clientPreparedStatement`メソッドを使用して`Client PreparedStatement`を構築します。

```scala
connection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("SELECT * FROM users WHERE id = ?")
    ...
  yield ...
}
```

#### Server PreparedStatement

`Connection`の`serverPreparedStatement`メソッドを使用して`Server PreparedStatement`を構築します。

```scala
connection.use { conn =>
  for 
    statement <- conn.serverPreparedStatement("SELECT * FROM users WHERE id = ?")
    ...
  yield ...
}
```

#### 読み取りクエリ

読み取り専用のSQLを実行する場合は`executeQuery`メソッドを使用します。

クエリを実行した結果MySQLサーバーから返される値は`ResultSet`に格納されて戻り値として返却されます。

```scala
connection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("SELECT * FROM users WHERE id = ?") // or conn.serverPreparedStatement("SELECT * FROM users WHERE id = ?")
    _ <- statement.setLong(1, 1)
    result <- statement.executeQuery()
  yield
    // ResultSetを使用した処理
}
```

動的なパラメーターを使用する場合は`setXXX`メソッドを使用してパラメーターを設定します。
`setXXX`メソッドは`Option`型を使用することもできます。`None`が渡された場合パラメーターにはNULLがセットされます。

`setXXX`メソッドはパラメーターのインデックスとパラメーターの値を指定します。

```scala
statement.setLong(1, 1)
```

現在のバージョンでは以下のメソッドがサポートされています。

| メソッド          | 型                                   | 備考                                |
|---------------|-------------------------------------|-----------------------------------|
| setNull       |                                     | パラメーターにNULLをセットします                |
| setBoolean    | Boolean/Option[Boolean]             |                                   |
| setByte       | Byte/Option[Byte]                   |                                   |
| setShort      | Short/Option[Short]                 |                                   |
| setInt        | Int/Option[Int]                     |                                   |
| setLong       | Long/Option[Long]                   |                                   |
| setBigInt     | BigInt/Option[BigInt]               |                                   |
| setFloat      | Float/Option[Float]                 |                                   |
| setDouble     | Double/Option[Double]               |                                   |
| setBigDecimal | BigDecimal/Option[BigDecimal]       |                                   |
| setString     | String/Option[String]               |                                   |
| setBytes      | Array[Byte]/Option[Array[Byte]]     |                                   |
| setDate       | LocalDate/Option[LocalDate]         | `java.sql`ではなく`java.time`を直接扱います。 |
| setTime       | LocalTime/Option[LocalTime]         | `java.sql`ではなく`java.time`を直接扱います。 |
| setTimestamp  | LocalDateTime/Option[LocalDateTime] | `java.sql`ではなく`java.time`を直接扱います。 |
| setYear       | Year/Option[Year]                   | `java.sql`ではなく`java.time`を直接扱います。 |

#### 書き込みクエリ

書き込みを行うSQLを実行する場合は`executeUpdate`メソッドを使用します。

クエリを実行した結果MySQLサーバーから返される値は影響を受けた行数が戻り値として返却されます。

```scala
connection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)") // or conn.serverPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
    _ <- statement.setString(1, "Alice")
    _ <- statement.setInt(2, 20)
    result <- statement.executeUpdate()
  yield result
}

```

#### AUTO_INCREMENTの値を取得

クエリ実行後にAUTO_INCREMENTの値を取得する場合は`returningAutoGeneratedKey`メソッドを使用します。

クエリを実行した結果MySQLサーバーから返される値はAUTO_INCREMENTに生成された値が戻り値として返却されます。

```scala
connection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)") // or conn.serverPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
    _ <- statement.setString(1, "Alice")
    _ <- statement.setInt(2, 20)
    result <- statement.returningAutoGeneratedKey()
  yield result
}
```

### ResultSet

`ResultSet`はクエリ実行後にMySQLサーバーから返された値を格納するためのAPIです。

SQLを実行して取得したレコードを`ResultSet`から取得するには`decode`メソッドを使用します。

`decode`メソッドは`ResultSet`から取得した値をScalaの型に変換して取得するためのAPIです。

取得するカラムの数に応じて`*:`演算子を使用して変換する型を指定します。

例では、usersテーブルのid, name, ageカラムを取得する場合を示しておりそれぞれのカラムの型を指定しています。

```scala
result.decode(bigint *: varchar *: int.opt)
```

NULL許容のカラムを取得する場合は`Option`型に変換するために`opt`メソッドを使用します。
これによりレコードがNULLの場合はNoneとして取得することができます。

クエリ実行からレコード取得までの一連の流れは以下のようになります。

```scala
connection.use { conn =>
  for 
    statement <- conn.clientPreparedStatement("SELECT * FROM users WHERE id = ?") // or conn.serverPreparedStatement("SELECT * FROM users WHERE id = ?")
    _ <- statement.setLong(1, 1)
    result <- statement.executeQuery()
  yield 
    val decodes: List[(Long, String, Option[Int])] = result.decode(bigint *: varchar *: int.opt)
    ...
}
```

`ResultSet`から取得するレコードは常に配列になります。
これはMySQLで実行するクエリの結果が常に複数のレコードを返す可能性があるからです。

単一のレコードを取得する場合は`decode`処理後に、`head`や`headOption`メソッドを使用して取得を行なってください。

現在のバージョンでは以下のデータ型がサポートされています。

| Codec       | データ型              | Scala 型        |
|-------------|-------------------|----------------|
| boolean     | BOOLEAN           | Boolean        |
| tinyint     | TINYINT           | Byte           |
| utinyint    | unsigned TINYINT  | Short          |
| smallint    | SMALLINT          | Short          |
| usmallint   | unsigned SMALLINT | Int            |
| int         | INT               | Int            |
| uint        | unsigned INT      | Long           |
| bigint      | BIGINT            | Long           |
| ubigint     | unsigned BIGINT   | BigInt         |
| float       | FLOAT             | Float          |
| double      | DOUBLE            | Double         |
| decimal     | DECIMAL           | BigDecimal     |
| char        | CHAR              | String         |
| varchar     | VARCHAR           | String         |
| binary      | BINARY            | Array[Byte]    |
| varbinary   | VARBINARY         | String         |
| tinyblob    | TINYBLOB          | String         |
| blob        | BLOB              | String         |
| mediumblob  | MEDIUMBLOB        | String         |
| longblob    | LONGBLOB          | String         |
| tinytext    | TINYTEXT          | String         |
| text        | TEXT              | String         |
| mediumtext  | MEDIUMTEXT        | String         |
| longtext    | LONGTEXT          | String         |
| enum        | ENUM              | String         |
| set         | SET               | List[String]   |
| json        | JSON              | String         |
| date        | DATE              | LocalDate      |
| time        | TIME              | LocalTime      |
| timetz      | TIME              | OffsetTime     |
| datetime    | DATETIME          | LocalDateTime  |
| timestamp   | TIMESTAMP         | LocalDateTime  |
| timestamptz | TIMESTAMP         | OffsetDateTime |
| year        | YEAR              | Year           |

※ 現在MySQLのデータ型を指定して値を取得するような作りとなっていますが、将来的にはより簡潔にScalaの型を指定して値を取得するような作りに変更する可能性があります。

以下サポートされていないデータ型があります。

- GEOMETRY
- POINT
- LINESTRING
- POLYGON
- MULTIPOINT
- MULTILINESTRING
- MULTIPOLYGON
- GEOMETRYCOLLECTION

## トランザクション

`Connection`を使用してトランザクションを実行するためには`setAutoCommit`メソッドと`commit`メソッド、`rollback`メソッドを組み合わせて使用します。

まず、`setAutoCommit`メソッドを使用してトランザクションの自動コミットを無効にします。

```scala
conn.setAutoCommit(false)
```

何かしらの処理を行った後に`commit`メソッドを使用してトランザクションをコミットします。

```scala
for
  statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
  _ <- statement.setString(1, "Alice")
  _ <- statement.setInt(2, 20)
  result <- statement.executeUpdate()
  _ <- conn.commit()
yield
```
もしくは、`rollback`メソッドを使用してトランザクションをロールバックします。

```scala
for
  statement <- conn.clientPreparedStatement("INSERT INTO users (name, age) VALUES (?, ?)")
  _ <- statement.setString(1, "Alice")
  _ <- statement.setInt(2, 20)
  result <- statement.executeUpdate()
  _ <- conn.rollback()
yield
```

`setAutoCommit`メソッドを使用してトランザクションの自動コミットを無効にした場合、コネクションのResourceを解放する際に自動的にロールバックが行われます。

### トランザクション分離レベル

LDBCではトランザクション分離レベルの設定を行うことができます。

トランザクション分離レベルは`setTransactionIsolation`メソッドを使用して設定を行います。

MySQLでは以下のトランザクション分離レベルがサポートされています。

- READ UNCOMMITTED
- READ COMMITTED
- REPEATABLE READ
- SERIALIZABLE

MySQLのトランザクション分離レベルについては[公式ドキュメント](https://dev.mysql.com/doc/refman/8.0/ja/innodb-transaction-isolation-levels.html)を参照してください。

```scala
import ldbc.connector.Connection.TransactionIsolationLevel

conn.setTransactionIsolation(TransactionIsolationLevel.REPEATABLE_READ)
```

現在設定されているトランザクション分離レベルを取得するには`getTransactionIsolation`メソッドを使用します。

```scala
for
  isolationLevel <- conn.getTransactionIsolation()
yield
```

## 未対応機能

LDBCコネクタは現在実験的な機能となります。そのため、以下の機能はサポートされていません。
機能提供は順次行っていく予定です。

- コネクションプーリング
- トランザクション
- バッチ処理
- セーブポイント
- フェイルオーバー対策
- etc...
