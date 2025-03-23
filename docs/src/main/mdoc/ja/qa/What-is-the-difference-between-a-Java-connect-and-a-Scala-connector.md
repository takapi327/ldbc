{%
  laika.title = "Q: JavaのコネクトとScalaのコネクタの違いは何ですか？"
  laika.metadata.language = ja
%}

# Q: JavaのコネクトとScalaのコネクタの違いは何ですか？

## A: Javaのコネクト（jdbc-connector）とScalaのコネクタ（ldbc-connector）は、どちらもデータベースへの接続を提供しますが、以下の点で異なります。

### A: Javaのコネクト（jdbc-connector）
Javaのコネクトは、従来のJDBC APIを利用してデータベースに接続します。  
- JDBCドライバ（例：MySQLの場合は`mysql-connector-j`）に依存し、低レベルな設定が必要です。  
- 接続の確立、クエリの実行、結果の取得は、従来の手続き型のAPIを使って実装されます。  

```scala
import com.mysql.cj.jdbc.MysqlDataSource
import cats.effect.IO
import ldbc.dsl.DBIO

// データソースの設定とJDBCコネクタの利用例
val ds = new com.mysql.cj.jdbc.MysqlDataSource()
ds.setServerName("127.0.0.1")
ds.setPortNumber(13306)
ds.setDatabaseName("world")
ds.setUser("ldbc")
ds.setPassword("password")

val provider = ConnectionProvider.fromDataSource[IO](ex, ExecutionContexts.synchronous)

// JavaのJDBC APIに基づく接続の利用例
provider.use { conn =>
  // PreparedStatementなどを利用してSQLを実行
  DBIO.pure(()).commit(conn)
}
```

### A: Scalaのコネクタ（ldbc-connector）
Scalaのコネクタは、型安全性と関数型プログラミングを活かしてデータベース接続を管理します。  
- Cats Effectの`Resource`や`IO`を利用し、接続の獲得・解放が安全に行えます。  
- DSLやクエリビルダーと組み合わせることで、直感的なコードでデータ操作が可能です。  
- また、ldbc-connectorはJVMだけでなく、Scala.jsやScala NativeといったJVM以外のプラットフォームでも動作します。  
  これにより、クロスプラットフォームな開発環境でのデータベース接続が容易に実現できます。

```scala
import cats.effect.IO
import ldbc.connector.*
import ldbc.dsl.DBIO

// ldbc-connectorを利用してコネクションを作成する例（JVM, Scala.js, Scala Native対応）
val provider =
  ConnectionProvider
    .default[IO]("127.0.0.1", 3306, "ldbc", "password", "ldbc")
    .setSSL(SSL.Trusted)

// Scalaコネクタの利用例：内部でResourceを使って接続後に自動でクローズを保証
provider.use { conn =>
  // ldbc DSLやDBIOを使ってSQLを実行できる
  DBIO.pure(()).commit(conn)
}
```

### A: 主な違い
- **APIの設計思想**:  
  Javaコネクトは従来のJDBCの手続き型APIをそのまま使用するのに対し、Scalaコネクタは関数型プログラミングを前提とし、型安全かつ宣言的な接続管理を実現しています。
- **エラーハンドリングとリソース管理**:  
  ScalaコネクタではCats Effectの`Resource`および`IO`を使うことで、接続の取得と解放が安全に行え、エラーハンドリングが簡潔に記述できます。
- **統合性**:  
  Scalaコネクタは、ldbc DSLやクエリビルダーとシームレスに連携し、クエリの定義から実行まで統一された型安全なAPIを提供します。

## 参考資料
- [コネクション](/ja/tutorial/Connection.md)  
- [ldbcとは何ですか？](/ja/qa/What-is-ldbc.md)
