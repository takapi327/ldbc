{%
  laika.title = セットアップ
  laika.metadata.language = ja
%}

# セットアップ

ldbcを使い始めるための最初のステップへようこそ！このページでは、開発環境とデータベースを準備する方法を説明します。

## 必要なもの

- JDK 21以上
- Scala 3
- Docker（データベース環境のため）
- [Scala CLI](https://scala-cli.virtuslab.org/)（推奨）

## データベースセットアップ

まず、Dockerを使用してMySQLデータベースを起動します。以下のdocker-compose.ymlファイルを作成してください：

```yaml
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

次に、`database`ディレクトリに以下のSQLファイルを作成して、初期データを設定します：

```sql
-- 01-create-database.sql
CREATE DATABASE IF NOT EXISTS sandbox_db;
USE sandbox_db;

-- テーブル作成
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
);

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
);

-- 初期データ投入
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

Docker Composeを使ってデータベースを起動します：

```bash
docker-compose up -d
```

## Scalaプロジェクトのセットアップ

このチュートリアルでは[Scala CLI](https://scala-cli.virtuslab.org/)を使用して簡単に始められるようにしています。まだインストールしていない場合は、以下のコマンドでインストールできます：

```bash
# macOSの場合
brew install Virtuslab/scala-cli/scala-cli

# その他のOSはScala CLIの公式サイトを参照してください
```

### はじめてのldbcプロジェクト

新しいディレクトリを作成し、最初のldbcプロジェクトを設定します：

```bash
mkdir ldbc-tutorial
cd ldbc-tutorial
touch FirstSteps.scala
```

`FirstSteps.scala`に以下のコードを記述します：

```scala
//> using scala "@SCALA_VERSION@"
//> using dep "@ORGANIZATION@::ldbc-dsl:@VERSION@"
//> using dep "@ORGANIZATION@::ldbc-connector:@VERSION@"

import cats.effect._
import cats.syntax.all._
import ldbc.dsl.io._

object FirstSteps extends IOApp.Simple {
  
  // トレーサー設定（ログ記録用）
  given Tracer[IO] = Tracer.noop[IO]
  
  // 単純な定数を返すプログラム
  val simpleProgram: DBIO[IO, Int] = DBIO.pure[IO, Int](42)
  
  // データベース接続設定
  val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("sandbox_db")
  )
  
  def run: IO[Unit] = {
    // プログラムの実行
    connection.use { conn =>
      simpleProgram.readOnly(conn).flatMap { result =>
        IO.println(s"データベースから取得した値: $result")
      }
    }
  }
}
```

Scala CLIを使ってプログラムを実行します：

```bash
scala-cli FirstSteps.scala
```

「データベースから取得した値: 42」と表示されれば成功です！これは実際にはデータベースに問い合わせていませんが、ldbcの基本的な構造と接続の設定ができていることを確認できました。

## 自動セットアップスクリプト（オプション）

すべてのセットアップを自動で行うScala CLIスクリプトも用意しています：

```bash
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## 次のステップ

これでldbcを使用する準備が整いました！次は[コネクション](/ja/tutorial/Connection.md)に進み、データベース接続の詳細について学びましょう。
