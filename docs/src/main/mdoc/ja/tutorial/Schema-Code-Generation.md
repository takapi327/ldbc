{%
  laika.title = スキーマコード生成
  laika.metadata.language = ja
%}

# スキーマコード生成

[スキーマ](/ja/tutorial/Schema.md)でScalaコードでスキーマを定義する方法を学びました。しかし、既存のデータベースがある場合は、手動でスキーマを定義するのは時間がかかりミスも発生しやすくなります。このページでは、既存のSQLファイルからScalaコードを自動生成する方法を説明します。

コード生成は、反復的な作業を自動化し、人的ミスを減らすための強力なツールです。ldbcは、SQLファイルからモデルクラスとテーブル定義を自動生成するための機能を提供しています。

## SBTプラグインの設定

### プラグインの追加

プロジェクトに以下の依存関係を設定する必要があります。`project/plugins.sbt`に追加してください。

```scala
addSbtPlugin("@ORGANIZATION@" % "ldbc-plugin" % "@VERSION@")
```

### プラグインの有効化

`build.sbt`ファイルでプロジェクトに対してプラグインを有効にします。

```sbt
lazy val root = (project in file("."))
  .enablePlugins(Ldbc)
```

## 基本的な使い方

### SQLファイルの指定

解析対象のSQLファイルを設定します。単一または複数のSQLファイルを指定できます。

```sbt
// 単一のSQLファイルを指定
Compile / parseFiles := List(
  baseDirectory.value / "sql" / "schema.sql"
)

// 複数のSQLファイルを指定
Compile / parseFiles := List(
  baseDirectory.value / "sql" / "users.sql",
  baseDirectory.value / "sql" / "products.sql"
)
```

### ディレクトリの指定

特定のディレクトリ内のすべてのSQLファイルを対象にする場合は、`parseDirectories`を使用します。

```sbt
// ディレクトリ単位で指定
Compile / parseDirectories := List(
  baseDirectory.value / "sql"
)
```

### 生成コード

設定後、sbtでコンパイルを実行すると自動的にコードが生成されます。

```shell
sbt compile
```

生成されたファイルは`target/scala-X.X/src_managed/main`ディレクトリに保存されます。

### 手動での生成

キャッシュを使用せず強制的にコード生成を実行したい場合は、以下のコマンドを使用します。

```shell
sbt generateBySchema
```

## SQLファイル形式の要件

SQLファイルには必ず以下の要素を含める必要があります。

### データベース定義

ファイルの先頭には、必ずデータベースのCreate文またはUse文を記述してください。これにより、生成されるコードのパッケージ名とテーブルの所属先を決定します。

```sql
-- 方法1: データベース作成
CREATE DATABASE `my_app`;

-- または方法2: 既存のデータベースを使用
USE `my_app`;
```

### テーブル定義

データベース定義の後に、テーブル定義を記述します。

```sql
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

完全なSQLファイルの例:

```sql
CREATE DATABASE `my_app`;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 詳細な設定オプション

ldbcプラグインでは、以下の設定キーを使用してコード生成をカスタマイズできます。

### 設定キー一覧

| キー                   | デフォルト値         | 詳細                                                  |
|----------------------|----------------|-----------------------------------------------------|
| `parseFiles`         | `List.empty`   | 解析対象のSQLファイルのリスト                                   |
| `parseDirectories`   | `List.empty`   | 解析対象のSQLファイルをディレクトリ単位で指定                         |
| `excludeFiles`       | `List.empty`   | 解析から除外するファイル名のリスト                                |
| `customYamlFiles`    | `List.empty`   | 型をカスタマイズするためのYAMLファイルのリスト                        |
| `classNameFormat`    | `Format.PASCAL`| 生成されるクラス名の形式（PASCAL、CAMEL、SNAKEから選択）             |
| `propertyNameFormat` | `Format.CAMEL` | 生成されるプロパティ名の形式（PASCAL、CAMEL、SNAKEから選択）           |
| `ldbcPackage`        | `ldbc.generated`| 生成されるファイルのパッケージ名                                |

### 例: 詳細な設定

```sbt
Compile / parseFiles := List(
  baseDirectory.value / "sql" / "schema.sql"
)

Compile / parseDirectories := List(
  baseDirectory.value / "sql" / "tables"
)

Compile / excludeFiles := List(
  "temp_tables.sql", "test_data.sql"
)

Compile / classNameFormat := PASCAL // PascalCase (MyClass)
Compile / propertyNameFormat := CAMEL // camelCase (myProperty)

Compile / ldbcPackage := "com.example.db"
```

## 生成されるコードの例

例として、以下のようなSQLファイルがある場合：

```sql
CREATE DATABASE `shop`;

CREATE TABLE `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `description` TEXT,
  `category_id` INT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

以下のようなScalaコードが生成されます：

```scala
package com.example.db

import ldbc.schema.*

import java.time.LocalDateTime

// モデルクラス
case class Product(
  id: Long,
  name: String,
  price: BigDecimal,
  description: Option[String],
  categoryId: Int,
  createdAt: LocalDateTime
)

// テーブル定義とクエリビルダー
object Product {
  val table = TableQuery[ProductTable]
  
  class ProductTable extends Table[Product]("products"):
    def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
    def name: Column[String] = column[String]("name", VARCHAR(255), NOT_NULL)
    def price: Column[BigDecimal] = column[BigDecimal]("price", DECIMAL(10, 2), NOT_NULL)
    def description: Column[Option[String]] = column[Option[String]]("description", TEXT)
    def categoryId: Column[Int] = column[Int]("category_id", INT, NOT_NULL)
    def createdAt: Column[LocalDateTime] = column[LocalDateTime]("created_at", TIMESTAMP, NOT_NULL, DEFAULT_CURRENT_TIMESTAMP)
    
    override def * : Column[Product] = (id *: name *: price *: description *: categoryId *: createdAt).to[Product]
}
```

## 型のカスタマイズ

自動生成されるコードの型を独自の型に変更したい場合は、YAMLファイルを使用してカスタマイズできます。

### YAMLファイルの設定

まず、カスタマイズ用のYAMLファイルを作成します。

```yaml
# custom_types.yml
database:
  name: 'shop'
  tables:
    - name: 'products'
      columns:
        - name: 'category_id'
          type: 'ProductCategory'
      object:
        extends:
          - 'com.example.ProductTypeMapping'
```

そして、このYAMLファイルをプロジェクト設定に追加します。

```sbt
Compile / customYamlFiles := List(
  baseDirectory.value / "config" / "custom_types.yml"
)
```

### カスタム型の実装

次に、YAMLファイルで参照している独自の型変換を実装します。

```scala
// com/example/ProductTypeMapping.scala
package com.example

import ldbc.dsl.Codec

sealed trait ProductCategory {
  def id: Int
}

object ProductCategory {
  case object Electronics extends ProductCategory { val id = 1 }
  case object Books extends ProductCategory { val id = 2 }
  case object Clothing extends ProductCategory { val id = 3 }
  
  def fromId(id: Int): ProductCategory = id match {
    case 1 => Electronics
    case 2 => Books
    case 3 => Clothing
    case _ => throw new IllegalArgumentException(s"Unknown category ID: $id")
  }
}

trait ProductTypeMapping {
  given Codec[ProductCategory] = Codec[Int].imap(ProductCategory.fromId)(_.id)
}
```

### カスタマイズ後の生成コード

上記の設定により、以下のようなコードが生成されます：

```scala
package ldbc.generated.shop

import ldbc.schema.*
import java.time.LocalDateTime
import com.example.ProductCategory

case class Product(
  id: Long,
  name: String,
  price: BigDecimal,
  description: Option[String],
  categoryId: ProductCategory, // カスタム型に変更
  createdAt: LocalDateTime
)

object Product extends com.example.ProductTypeMapping {
  val table = TableQuery[ProductTable]
  
  class ProductTable extends Table[Product]("products"):
    def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
    def name: Column[String] = column[String]("name", VARCHAR(255), NOT_NULL)
    def price: Column[BigDecimal] = column[BigDecimal]("price", DECIMAL(10, 2), NOT_NULL)
    def description: Column[Option[String]] = column[Option[String]]("description", TEXT)
    def categoryId: Column[Int] = column[Int]("category_id", INT, NOT_NULL) // 実際のカラムの型は変わらない
    def createdAt: Column[LocalDateTime] = column[LocalDateTime]("created_at", TIMESTAMP, NOT_NULL, DEFAULT_CURRENT_TIMESTAMP)
    
    override def * : Column[Product] = (id *: name *: price *: description *: categoryId *: createdAt).to[Product]
}
```

## YAMLカスタマイズの詳細な構文

カスタマイズYAMLファイルでは、以下の設定が可能です。

```yaml
database:
  name: '{データベース名}'
  tables:
    - name: '{テーブル名}'
      columns: # オプション
        - name: '{カラム名}'
          type: '{変更したいScala型}'
      class: # オプション
        extends:
          - '{モデルクラスに継承させたいtraitなどのパッケージパス}'
      object: # オプション
        extends:
          - '{オブジェクトに継承させたいtraitなどのパッケージパス}'
```

### 例: モデルクラスへのtraitの追加

```yaml
database:
  name: 'shop'
  tables:
    - name: 'products'
      class:
        extends:
          - 'com.example.JsonSerializable'
          - 'com.example.Validatable'
```

### 例: 複数のテーブル・カラムのカスタマイズ

```yaml
database:
  name: 'shop'
  tables:
    - name: 'products'
      columns:
        - name: 'price'
          type: 'Money'
      object:
        extends:
          - 'com.example.MoneyTypeMapping'
    - name: 'orders'
      columns:
        - name: 'status'
          type: 'OrderStatus'
      object:
        extends:
          - 'com.example.OrderStatusMapping'
```

## 生成されたコードの使用方法

生成されたコードは、他のldbcコードと同様に使用できます。

```scala
import ldbc.dsl.*
import ldbc.generated.shop.Product

val provider = MySQLConnectionProvider(...)

// テーブルクエリの参照
val products = Product.table

// クエリの実行
val allProducts = provider.use { conn =>
  products.filter(_.price > 100).all.run(conn)
}
```

## コード生成のベストプラクティス

### 1. 明確なSQLファイル構成

- 関連するテーブルを同じファイルにまとめる
- 各ファイルの先頭にデータベース定義を必ず含める
- 適切なコメントでSQLを説明する

### 2. 命名規則の一貫性

- SQL内のテーブル・カラム名に一貫した命名規則を使用する
- 生成されるScalaコードの命名規則を明示的に設定する

### 3. カスタム型の賢い使用

- ドメイン特有の概念には独自の型を使用する
- 複雑なビジネスロジックをカプセル化するためにカスタム型を活用する

### 4. 再生成の自動化

定期的なスキーマ更新のためにCI/CDパイプラインに組み込むことを検討する。

## トラブルシューティング

### コードが生成されない場合

- SQLファイルのパスが正しいか確認する
- SQLファイルの先頭にデータベース定義があるか確認する
- SQLの構文エラーがないか確認する

### 型変換エラーが発生する場合

- カスタムYAMLの設定が正しいか確認する
- 参照しているパッケージやクラスがクラスパスに存在するか確認する
- 暗黙の型変換（given/using）が正しく定義されているか確認する

### 生成されたコードに問題がある場合

- 手動で修正せず、SQLまたはYAMLファイルを修正してから再生成する
- サポートされていないSQLの機能や特殊な型がないか確認する

## チュートリアルの完了

おめでとうございます！ldbcチュートリアルのすべてのセクションを完了しました。これで、ldbcを使ってデータベースアプリケーションを開発するための基本的なスキルと知識を身につけました。

この旅を通じて、以下のことを学びました：
- ldbcの基本的な使い方とセットアップ
- データベース接続とクエリ実行
- データの読み書きと型安全なマッピング
- トランザクション管理とエラーハンドリング
- 高度な機能（ロギング、カスタムデータ型、クエリビルダー）
- スキーマ定義とコード生成

これらの知識を活かして、型安全で効率的なデータベースアプリケーションを構築してください。さらに詳しい情報やアップデートは、公式ドキュメントやGitHubリポジトリを参照してください。

Happy coding with ldbc!
