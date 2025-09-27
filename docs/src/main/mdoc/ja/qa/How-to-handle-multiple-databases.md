{%
laika.title = "Q: 複数のデータベース（マルチテナント環境）を扱う方法は？"
laika.metadata.language = ja
%}

# Q: 複数のデータベース（マルチテナント環境）を扱う方法は？

## A: 複数のデータベースを扱う場合は、それぞれのデータベースに対して別々のDataSourceを作成します。

複数のデータベースを扱う場合は、それぞれのデータベースに対して別々の`DataSource`を作成します。例えば、次のように異なるデータベースに対して異なるデータソースを作成し、必要に応じてデータソースを切り替えて使用します。

```scala 3
val datasource1 = MySQLDataSource
  .build[IO]("host", 3306, "user")
  .setPassword("password")
  .setDatabase("database1")

val datasource2 = MySQLDataSource
  .build[IO]("host", 3306, "user")
  .setPassword("password")
  .setDatabase("database2")

// 必要に応じてデータソースを切り替えて使用
val program1 = datasource1.getConnection.use { conn => /* database1に対する操作 */ }
val program2 = datasource2.getConnection.use { conn => /* database2に対する操作 */ }
```
