{%
  laika.title = スキーマコード生成
  laika.metadata.language = ja
%}

# スキーマコード生成

[スキーマ](/ja/tutorial/Schema.md)でScalaコードでスキーマを定義する方法を学びました。しかし、既存のデータベースがある場合は、手動でスキーマを定義するのは時間がかかりミスも発生しやすくなります。このページでは、既存のSQLファイルからScalaコードを自動生成する方法を説明します。

コード生成は、反復的な作業を自動化し、人的ミスを減らすための強力なツールです。ldbcは、SQLファイルからモデルクラスとテーブル定義を自動生成するための機能を提供しています。

この章では、ldbcのテーブル定義をSQLファイルから自動生成する方法について説明します。

プロジェクトに以下の依存関係を設定する必要があります。

```scala 3
addSbtPlugin("@ORGANIZATION@" % "ldbc-plugin" % "@VERSION@")
```

## 生成

プロジェクトに対してプラグインを有効にします。

```sbt
lazy val root = (project in file("."))
  .enablePlugins(Ldbc)
```

解析対象のSQLファイルを配列で指定します。

```sbt
Compile / parseFiles := List(baseDirectory.value / "test.sql")
```

**プラグインを有効にすることで設定できるキーの一覧**

| キー                   | 詳細                                        |
|----------------------|-------------------------------------------|
| `parseFiles`         | `解析対象のSQLファイルのリスト`                        |
| `parseDirectories`   | `解析対象のSQLファイルをディレクトリ単位で指定する`              |
| `excludeFiles`       | `解析から除外するファイル名のリスト`                       |
| `customYamlFiles`    | `Scala型やカラムのデータ型をカスタマイズするためのyamlファイルのリスト` |
| `classNameFormat`    | `クラス名の書式を指定する値`                           |
| `propertyNameFormat` | `Scalaモデルのプロパティ名の形式を指定する値`                |
| `ldbcPackage`        | `生成されるファイルのパッケージ名を指定する値`                  |

解析対象のSQLファイルの先頭には必ずデータベースのCreate文もしくはUse文を定義する必要があります。ldbcはファイルの解析を1ファイルずつ行い、テーブル定義を生成しデータベースモデルにテーブルのリストを格納させます。
そのためテーブルがどのデータベースに所属しているかを教えてあげる必要があるからです。

```sql
CREATE DATABASE `location`;

USE `location`;

DROP TABLE IF EXISTS `country`;
CREATE TABLE country (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `code` INT NOT NULL
);
```

解析対象のSQLファイルにはデータベースのCreate/Use文もしくはテーブル定義のCreate/Drop文のみ記載するようにしなければいけません。

## 生成コード

sbtプロジェクトを起動してコンパイルを実行すると、解析対象のSQLファイルを元に生成されたモデルクラスと、テーブル定義がsbtプロジェクトのtarget配下に生成されます。

```shell
sbt compile
```

上記SQLファイルから生成されるコードは以下のようなものになります。

```scala 3
package ldbc.generated.location

import ldbc.schema.*

case class Country(
  id: Long,
  name: String,
  code: Int
)

object Country:
  val table = TableQuery[CountryTable]
  
  class CountryTable extends Table[Country]("country"):
    def id: Column[Long] = column[Long]("id")
    def name: Column[String] = column[String]("name")
    def code: Column[Int] = column[Int]("code")
    
    override def * : Column[Country] = (id *: name *: code).to[Country]
```

Compileでコードを生成した場合、その生成されたファイルはキャッシュされるので、SQLファイルを変更していない場合再度生成されることはありません。SQLファイルを変更した場合もしくは、cleanコマンドを実行してキャッシュを削除した場合はCompileを実行すると再度コードが生成されます。
キャッシュを利用せず再度コード生成を行いたい場合は、`generateBySchema`コマンドを実行してください。このコマンドはキャッシュを使用せず常にコード生成を行います。

```shell
sbt generateBySchema
```

## カスタマイズ

SQLファイルから生成されるコードの型を別のものに変換したい時があるかもしれません。その場合は`customYamlFiles`にカスタマイズを行うymlファイルを渡してあげることで行うことができます。

```sbt
Compile / customYamlFiles := List(
  baseDirectory.value / "custom.yml"
)
```

ymlファイルの形式は以下のようなものである必要があります。

```yaml
database:
  name: '{データベース名}'
  tables:
    - name: '{テーブル名}'
      columns: # Optional
        - name: '{カラム名}'
          type: '{変更したいScalaの型}'
      class: # Optional
        extends:
          - '{モデルクラスに継承させたいtraitなどのpackageパス}' // package.trait.name
      object: # Optional
        extends:
          - '{オブジェクトに継承させたいtraitなどのpackageパス}'
    - name: '{テーブル名}'
      ...
```

`database`は解析対象のSQLファイルに記載されているデータベース名である必要があります。またテーブル名は解析対象のSQLファイルに記載されているデータベースに所属しているテーブル名である必要があります。

`columns`には型を変更したいカラム名と変更したいScalaの型を文字列で記載を行います。`columns`には複数の値を設定できますが、nameに記載されたカラム名が対象のテーブルに含まれいてなければなりません。
また、変換を行うScalaの型はカラムのData型がサポートしている型である必要があります。もしサポート対象外の型を指定したい場合は、`object`に対して暗黙の型変換を行う設定を持ったtraitやabstract classなどを渡してあげる必要があります。

Data型がサポートしている型に関しては[こちら](/ja/tutorial/Custom-Data-Type.md)を、サポート対象外の型を設定する方法は[こちら](/ja/tutorial/Custom-Data-Type.md)を参照してください。

Int型をユーザー独自の型であるCountryCodeに変換する場合は、以下のような`CustomMapping`traitを実装します。

```scala 3
trait CountryCode:
  val code: Int
object Japan extends CountryCode:
  override val code: Int = 1

trait CustomMapping: // 任意の名前
  given Codec[CountryCode] = Codec[Int].imap {
    case 1 => Japan
    case _ => throw new Exception("Not found")
  }(_.code)
```

カスタマイズを行うためのymlファイルに実装を行なった`CustomMapping`traitを設定し、対象のカラムの型をCountryCodeに変換してあげます。

```yaml
database:
  name: 'location'
  tables:
    - name: 'country'
      columns:
        - name: 'code'
          type: 'Country.CountryCode' // CustomMappingをCountryオブジェクトにミックスインさせるのでそこから取得できるように記載
      object:
        extends:
          - '{package.name.}CustomMapping'
```

上記設定で生成されるコードは以下のようになり、ユーザー独自の型でモデルとテーブル定義を生成できるようになります。

```scala 3
case class Country(
  id: Long,
  name: String,
  code: Country.CountryCode
)

object Country extends /*{package.name.}*/CustomMapping:
  
  val table = TableQuery[CountryTable]

  class CountryTable extends Table[Country]("country"):
    def id: Column[Long] = column[Long]("id")
    def name: Column[String] = column[String]("name")
    def code: Column[Int] = column[Int]("code")

    override def * : Column[Country] = (id *: name *: code).to[Country]
```

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
