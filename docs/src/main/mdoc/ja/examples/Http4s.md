{%
  laika.title = Http4s
  laika.metadata.language = ja
%}

# Http4sとldbcの連携

このガイドでは、[Http4s](https://http4s.org/)とldbcを組み合わせてWebアプリケーションを構築する方法を説明します。

## はじめに

Http4sは、Scala用の純粋関数型HTTPサーバーおよびクライアントライブラリです。ldbcと組み合わせることで、
タイプセーフなデータベースアクセスを持つWebアプリケーションを構築することができます。

## 依存関係の追加

まず、以下の依存関係をbuild.sbtに追加する必要があります：

```scala
libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-dsl"          % "0.23.30",
  "org.http4s"    %% "http4s-ember-server" % "0.23.30",
  "org.http4s"    %% "http4s-circe"        % "0.23.30",
  "io.circe"      %% "circe-generic"       % "0.14.10"
)
```

## 基本的な使い方

### 1. テーブル定義

まず、データベーステーブルに対応するモデルとテーブル定義を作成します：

```scala
// モデル定義
case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
)

// JSON エンコーダーの定義
object City:
  given Encoder[City] = Encoder.derived[City]

// テーブル定義
class CityTable extends Table[City]("city"):
  // カラム名の命名規則を設定（オプション）
  given Naming = Naming.PASCAL

  // カラム定義
  def id:          Column[Int]    = int("ID").unsigned.autoIncrement.primaryKey
  def name:        Column[String] = char(35)
  def countryCode: Column[String] = char(3).unique
  def district:    Column[String] = char(20)
  def population:  Column[Int]    = int()

  // マッピング定義
  override def * : Column[City] = 
    (id *: name *: countryCode *: district *: population).to[City]

val cityTable = TableQuery[CityTable]
```

### 2. データベース接続設定

データベース接続の設定を行います：

```scala
private def datasource =
  MySQLDataSource
    .build[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
    .setDatabase("world")
    .setSSL(SSL.Trusted)
```

接続設定で指定可能な主なオプション：
- ホスト名
- ポート番号
- データベース名
- ユーザー名
- パスワード
- SSL設定（Trusted, Verify等）

### 3. HTTPルートの定義

Http4sのルートを定義し、ldbcのクエリを組み込みます：

```scala
private def routes(connector: Connector[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "cities" =>
    for
      cities <- cityTable.selectAll.query.to[List].readOnly(connector)
      result <- Ok(cities.asJson)
    yield result
}
```

### 4. サーバーの起動

最後に、Http4sサーバーを起動します：

```scala
object Main extends ResourceApp.Forever:
  override def run(args: List[String]): Resource[IO, Unit] =
    // Connectorを作成
    val connector = Connector.fromDataSource(datasource)
    
    EmberServerBuilder
      .default[IO]
      .withHttpApp(routes(connector).orNotFound)
      .build
      .void
```

## 応用例

### カスタムクエリの追加

特定の条件での検索や、複雑なクエリを実装する例：

```scala
private def routes(connector: Connector[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "cities" / "search" / name =>
    for
      cities <- cityTable.filter(_.name === name).query.to[List].readOnly(connector)
      result <- Ok(cities.asJson)
    yield result
      
  case GET -> Root / "cities" / "population" / IntVar(minPopulation) =>
    for
      cities <- cityTable.filter(_.population >= minPopulation).query.to[List].readOnly(connector)
      result <- Ok(cities.asJson)
    yield result
}
```

## エラーハンドリング

データベースエラーを適切にハンドリングする例：

```scala
private def handleDatabaseError[A](action: IO[A]): IO[Response[IO]] =
  action.attempt.flatMap {
    case Right(value) => Ok(value.asJson)
    case Left(error) => 
      InternalServerError(s"Database error: ${error.getMessage}")
  }

private def routes(connector: Connector[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "cities" =>
    handleDatabaseError {
      cityTable.selectAll.query.to[List].readOnly(connector)
    }
}
```

## まとめ

Http4sとldbcを組み合わせることで、以下のような利点があります：
- タイプセーフなデータベースアクセス
- 純粋関数型プログラミングの恩恵
- 柔軟なルーティングとエラーハンドリング

実際のアプリケーション開発では、これらの基本的なパターンを組み合わせて、より複雑な機能を実装していくことができます。
