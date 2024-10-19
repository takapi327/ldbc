{%
laika.title = スキーマコード生成
laika.metadata.language = ja
%}

# スキーマコード生成

この章では、LDBCのテーブル定義をSQLファイルから自動生成する方法について説明します。

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

| キー                 | 詳細                                       |
|--------------------|------------------------------------------|
| parseFiles         | 解析対象のSQLファイルのリスト                         |
| parseDirectories   | 解析対象のSQLファイルをディレクトリ単位で指定する               |
| excludeFiles       | 解析から除外するファイル名のリスト                        |
| customYamlFiles    | Scala型やカラムのデータ型をカスタマイズするためのyamlファイルのリスト。 |
| classNameFormat    | クラス名の書式を指定する値。                           |
| propertyNameFormat | Scalaモデルのプロパティ名の形式を指定する値。                |
| ldbcPackage        | 生成されるファイルのパッケージ名を指定する値。                  |

解析対象のSQLファイルの先頭には必ずデータベースのCreate文もしくはUse文を定義する必要があります。LDBCはファイルの解析を1ファイルずつ行い、テーブル定義を生成しデータベースモデルにテーブルのリストを格納させます。
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

import ldbc.core.*

case class Country(
  id: Long,
  name: String,
  code: Int
)

object Country:
  val table = Table[Country]("country")(
    column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
    column("name", VARCHAR(255)),
    column("code", INT)
  )
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

Data型がサポートしている型に関しては[こちら](/ja/01-Table-Definitions.md)を、サポート対象外の型を設定する方法は[こちら](/ja/02-Custom-Data-Type.md)を参照してください。

Int型をユーザー独自の型であるCountryCodeに変換する場合は、以下のような`CustomMapping`traitを実装します。

```scala 3
trait CountryCode:
  val code: Int
object Japan extends CountryCode:
  override val code: Int = 1

trait CustomMapping: // 任意の名前
  given Conversion[INT[Int], CountryCode] = DataType.mappingp[INT[Int], CountryCode]
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
  val table = Table[Country]("country")(
    column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
    column("name", VARCHAR(255)),
    column("code", INT)
  )
```

データベースモデルに関してもSQLファイルから自動生成が行われています。

```scala 3
package ldbc.generated.location

import ldbc.core.*

case class LocationDatabase(
  schemaMeta: Option[String] = None,
  catalog: Option[String] = Some("def"),
  host: String = "127.0.0.1",
  port: Int = 3306
) extends Database:

  override val databaseType: Database.Type = Database.Type.MySQL

  override val name: String = "location"

  override val schema: String = "location"

  override val character: Option[Character] = None

  override val collate: Option[Collate] = None

  override val tables = Set(
    Country.table
  )
```
