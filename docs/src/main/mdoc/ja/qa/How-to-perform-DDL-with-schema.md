{%
  laika.title = "Q: スキーマを使用したDDLを実行する方法は？"
  laika.metadata.language = ja
%}

# Q: スキーマを使用したDDLを実行する方法は？

## A: スキーマからDDLを生成・実行するには、TableQueryの`schema`メソッドを利用します。  
`schema`メソッドは、テーブル定義からCREATE、DROP、TRUNCATEなどのDDL文を自動生成する便利な仕組みです。  
以下のサンプルコードは、`UserTable`のDDLを生成し、テーブルの作成、データの削除、テーブルの削除を順に実行する方法を示しています。

```scala 3
// ...existing code...
// 例: UserTableのDDL操作

// UserTableが定義されていると仮定
val userSchema = TableQuery[UserTable].schema

// DDL文の生成例
val createDDL   = userSchema.createIfNotExists.statements
val dropDDL     = userSchema.dropIfExists.statements
val truncateDDL = userSchema.truncate.statements

// 生成されたDDL文を確認する場合
createDDL.foreach(println)    // CREATE TABLE文が出力される
dropDDL.foreach(println)      // DROP TABLE文が出力される
truncateDDL.foreach(println)  // TRUNCATE TABLE文が出力される

// DDL操作の実行例
provider
  .use { conn =>
    DBIO.sequence(
        userSchema.createIfNotExists,
        userSchema.truncate,
        userSchema.dropIfExists
      )
      .commit(conn)
  }
// ...existing code...
```

上記のコードは、テーブルの存在確認を行いながら作成し、必要に応じてテーブル内のデータをリセットし、その後テーブル自体を削除する操作を実現します。

## 参考資料
- [スキーマ定義の詳細](/ja/tutorial/Schema.md)  
- [データベース操作](/ja/tutorial/Database-Operations.md)
