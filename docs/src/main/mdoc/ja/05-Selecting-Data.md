# データ選択

この章では、ldbcデータセットを使用してデータを選択する方法を説明します。まず、データベースをセットアップします。以下のコードを使用して、MySQLデータベースをセットアップします。

@@@ vars
```yaml
version: '3'
services:
  mysql:
    image: mysql:"$mysqlVersion$"
    container_name: ldbc
    environment:
      MYSQL_USER: 'ldbc'
      MYSQL_PASSWORD: 'password'
      MYSQL_ROOT_PASSWORD: 'root'
    ports:
      - 13306:3306
    volumes:
      - ./database:/docker-entrypoint-initdb.d
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost" ]
      timeout: 20s
      retries: 10
```
@@@

次に、データベースの初期化を行います。以下のコードを使用して、データベースに接続し必要なテーブルを作成します。

@@snip [00-Setup.scala](/docs/src/main/scala/00-Setup.scala) { #setup }

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala
```

## コレクションへの行の読み込み

最初のクエリでは、低レベルのクエリを目指して、いくつかの国名をリストに選択し、最初の数件をプリントアウトしてみましょう。ここにはいくつかのステップがあるので、途中のタイプを記しておきます。

```scala
sql"SELECT name FROM country"
  .query[String] // Query[IO, String]
  .to[List] // Executor[IO, List[String]]
  .readOnly(conn) // IO[List[String]]
  .unsafeRunSync() // List[String]
  .foreach(println) // Unit
```

これを少し分解してみよう。

- `sql"SELECT name FROM country".query[String]`は`Query[IO, String]`を定義し、返される各行をStringにマップする1列のクエリです。このクエリは1列のクエリで、返される行をそれぞれStringにマップします。
- `.to[List]`は、行をリストに蓄積する便利なメソッドで、この場合は`Executor[IO, List[String]]`を生成します。このメソッドは、CanBuildFromを持つすべてのコレクション・タイプで動作します。
- `readOnly(conn)`は`IO[List[String]]`を生成し、これを実行すると通常のScala List[String]が出力される。
- `unsafeRunSync()`は、IOモナドを実行し、結果を取得する。これは、IOモナドを実行し、結果を取得するために使用される。
- `foreach(println)`は、リストの各要素をプリントアウトする。

## 複数列クエリ

もちろん、複数のカラムを選択してタプルにマッピングすることもできます。

```scala
sql"SELECT name, population FROM country"
  .query[(String, Int)] // Query[IO, (String, Int)]
  .to[List] // Executor[IO, List[(String, Int)]]
  .readOnly(conn) // IO[List[(String, Int)]]
  .unsafeRunSync() // List[(String, Int)]
  .foreach(println) // Unit
```

ldbcは、複数のカラムを選択してクラスにマッピングすることもできます。

```scala
case class Country(name: String, population: Int)

sql"SELECT name, population FROM country"
  .query[Country] // Query[IO, Country]
  .to[List] // Executor[IO, List[Country]]
  .readOnly(conn) // IO[List[Country]]
  .unsafeRunSync() // List[Country]
  .foreach(println) // Unit
```
