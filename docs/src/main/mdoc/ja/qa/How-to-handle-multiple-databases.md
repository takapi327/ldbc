{%
laika.title = "Q: 複数のデータベース（マルチテナント環境）を扱う方法は？"
laika.metadata.language = ja
%}

# Q: 複数のデータベース（マルチテナント環境）を扱う方法は？

## A: 複数のデータベースを扱う場合は、それぞれのデータベースに対して別々のConnectionProviderを作成します。

複数のデータベースを扱う場合は、それぞれのデータベースに対して別々の`ConnectionProvider`を作成します。例えば、次のように異なるデータベースに対して異なるプロバイダーを作成し、必要に応じてプロバイダーを切り替えて使用します。

```scala 3
val provider1 = ConnectionProvider
  .default[IO]("host", 3306, "user", "password", "database1")

val provider2 = ConnectionProvider
  .default[IO]("host", 3306, "user", "password", "database2")

// 必要に応じてプロバイダーを切り替えて使用
val program1 = provider1.use { conn => /* database1に対する操作 */ }
val program2 = provider2.use { conn => /* database2に対する操作 */ }
```
