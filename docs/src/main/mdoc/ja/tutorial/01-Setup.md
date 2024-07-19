{%
  laika.title = セットアップ
  laika.metadata.language = ja
%}

# セットアップ

素晴らしいldbcの世界へようこそ！このセクションでは、すべてのセットアップをお手伝いします。

## データベースセットアップ

まず、データベースを起動します。以下のコードを使用して、データベースを起動します。

```yaml
version: '3'
services:
  mysql:
    image: mysql:@MYSQL_VERSION@
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

次に、データベースの初期化を行います。

以下コードのようにデータベースの作成を行います。

```sql
CREATE DATABASE IF NOT EXISTS sandbox_db;
```

次に、テーブルの作成を行います。

```sql
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `product` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)

CREATE TABLE IF NOT EXISTS `order` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `product_id` INT NOT NULL,
  `order_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `quantity` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES `user` (id),
  FOREIGN KEY (product_id) REFERENCES `product` (id)
)
```

それぞれのテーブルにデータを挿入します。

```sql
INSERT INTO user (name, email) VALUES
  ('Alice', 'alice@example.com'),
  ('Bob', 'bob@example.com'),
  ('Charlie', 'charlie@example.com');

INSERT INTO product (name, price) VALUES
  ('Laptop', 999.99),
  ('Mouse', 19.99),
  ('Keyboard', 49.99),
  ('Monitor', 199.99);

INSERT INTO `order` (user_id, product_id, quantity) VALUES
  (1, 1, 1), -- Alice ordered 1 Laptop
  (1, 2, 2), -- Alice ordered 2 Mice
  (2, 3, 1), -- Bob ordered 1 Keyboard
  (3, 4, 1); -- Charlie ordered 1 Monitor
```

## Scalaセットアップ

チュートリアルでは[Scala CLI](https://scala-cli.virtuslab.org/)を使用します。そのため、Scala CLIをインストールする必要があります。

```bash
brew install Virtuslab/scala-cli/scala-cli
```

**Scala CLIで実行**

先ほどのデータベースセットアップは、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このセットアップを行うことができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

次に、ldbcを依存関係に持つ新しいプロジェクトを作成します。

```scala
//> using scala "@SCALA_VERSION@"
//> using dep "@ORGANIZATION@::ldbc-dsl:@VERSION@"
```

### 最初のプログラム

ldbcを使う前に、いくつかのシンボルをインポートする必要がある。ここでは便宜上、パッケージのインポートを使用する。これにより、高レベルAPIで作業する際に最もよく使用されるシンボルを得ることができる。

```scala
import ldbc.dsl.io.*
```

Catsも連れてこよう。

```scala
import cats.syntax.all.*
import cats.effect.*
```

次に、トレーサーとログハンドラーを提供する。これらは、アプリケーションのログを記録するために使用される。トレーサーは、アプリケーションのトレースを記録するために使用される。ログハンドラーは、アプリケーションのログを記録するために使用される。

以下のコードは、トレーサーとログハンドラーを提供するがその実体は何もしない。

```scala
given Tracer[IO]     = Tracer.noop[IO]
given LogHandler[IO] = LogHandler.noop[IO]
```

ldbc高レベルAPIで扱う最も一般的な型は`Executor[F, A]`という形式で、`{java | ldbc}.sql.Connection`が利用可能なコンテキストで行われる計算を指定し、最終的にA型の値を生成します。

では、定数を返すだけのExecutorプログラムから始めてみよう。

```scala
val program: Executor[IO, Int] = Executor.pure[IO, Int](1)
```

次に、データベースに接続するためのコネクタを作成する。コネクタは、データベースへの接続を管理するためのリソースである。コネクタは、データベースへの接続を開始し、クエリを実行し、接続を閉じるためのリソースを提供する。

※ ここではldbcが独自に作成したコネクタを使用します。コネクタの選択と作成方法は後に説明します。

```scala
def connection = Connection[IO](
  host     = "127.0.0.1",
  port     = 13306,
  user     = "ldbc",
  password = Some("password"),
  ssl      = SSL.Trusted
)
```

Executorは、データベースへの接続方法、接続の受け渡し方法、接続のクリーンアップ方法を知っているデータ型であり、この知識によってExecutorをIOへ変換し、実行可能なプログラムを得ることができる。具体的には、実行するとデータベースに接続し、単一のトランザクションを実行するIOが得られる。

```scala
connection
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

万歳！定数を計算できた。これはデータベースに仕事を依頼することはないので、あまり面白いものではないが、最初の一歩が完了です。

> この本のコードは、IO.unsafeRunSyncの呼び出し以外はすべて純粋なものであることを覚えておいてほしい。IO.unsafeRunSyncは、通常アプリケーションのエントリー・ポイントにのみ現れる「世界の終わり」の操作である。REPLでは、計算を強制的に "happen "させるためにこれを使用する。

**Scala CLIで実行**

このプログラムも、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/01-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```
