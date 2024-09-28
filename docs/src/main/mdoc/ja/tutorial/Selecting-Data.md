{%
  laika.title = データ選択
  laika.metadata.language = ja
%}

# データ選択

この章では、ldbcデータセットを使用してデータを選択する方法を説明します。

## コレクションへの行の読み込み

最初のクエリでは、低レベルのクエリを目指して、いくつかのユーザーをリストに選択し、最初の数件をプリントアウトしてみましょう。ここにはいくつかのステップがあるので、途中のタイプを記しておきます。

```scala
sql"SELECT name FROM user"
  .query[String] // Query[IO, String]
  .to[List] // Executor[IO, List[String]]
  .readOnly(conn) // IO[List[String]]
  .unsafeRunSync() // List[String]
  .foreach(println) // Unit
```

これを少し分解してみよう。

- `sql"SELECT name FROM user".query[String]`は`Query[IO, String]`を定義し、返される各行をStringにマップする1列のクエリです。このクエリは1列のクエリで、返される行をそれぞれStringにマップします。
- `.to[List]`は、行をリストに蓄積する便利なメソッドで、この場合は`Executor[IO, List[String]]`を生成します。このメソッドは、CanBuildFromを持つすべてのコレクション・タイプで動作します。
- `readOnly(conn)`は`IO[List[String]]`を生成し、これを実行すると通常のScala `List[String]`が出力される。
- `unsafeRunSync()`は、IOモナドを実行し、結果を取得する。これは、IOモナドを実行し、結果を取得するために使用される。
- `foreach(println)`は、リストの各要素をプリントアウトする。

## 複数列クエリ

もちろん、複数のカラムを選択してタプルにマッピングすることもできます。

```scala
sql"SELECT name, email FROM user"
  .query[(String, String)] // Query[IO, (String, String)]
  .to[List] // Executor[IO, List[(String, String)]]
  .readOnly(conn) // IO[List[(String, String)]]
  .unsafeRunSync() // List[(String, String)]
  .foreach(println) // Unit
```

ldbcは、複数のカラムを選択してクラスにマッピングすることもできます。

```scala
case class User(name: String, email: String)

sql"SELECT name, email FROM user"
  .query[User] // Query[IO, User]
  .to[List] // Executor[IO, List[User]]
  .readOnly(conn) // IO[List[User]]
  .unsafeRunSync() // List[User]
  .foreach(println) // Unit
```
