{%
  laika.title = "Q: スキーマを使用したカラム名のフォーマットを変更する方法は？"
  laika.metadata.language = ja
%}

# Q: スキーマを使用したカラム名のフォーマットを変更する方法は？

## A: スキーマ定義において、カラム名のフォーマットはNamingの暗黙的なインスタンスを変更することで変更できます。  
たとえば、デフォルトはキャメルケースですが、`Naming.PASCAL`を使用するとすべてのカラム名がパスカルケースに変換されます。  
以下のサンプルコードでは、`given Naming = Naming.PASCAL`をテーブル定義内で設定し、カラム名に自動的に適用される例を示しています。

```scala 3
// Schema定義の例（カラム名のフォーマット変更）
case class User(id: Long, name: String, age: Option[Int])

class UserTable extends Table[User]("user"):
  // カラム名をパスカルケースに変更するためのNamingを設定
  given Naming = Naming.PASCAL
  
  def id: Column[Long] = bigint()         // 自動的に "Id" になる
  def name: Column[String] = varchar(255)  // 自動的に "Name" になる
  def age: Column[Option[Int]] = int().defaultNull // 自動的に "Age" になる
  
  override def * : Column[User] = (id *: name *: age).to[User]
  
// 使用例：select文で変更後のカラム名が適用される
val userTable: TableQuery[UserTable] = TableQuery[UserTable]
val select = userTable.selectAll

println(select.statement)
// 出力例: "SELECT `Id`, `Name`, `Age` FROM user"
```

この方法により、カラム名を一括して変更できるため、プロジェクト全体の命名規則に合わせた整合性の取れたスキーマが実現できます。

## 参考資料
- [スキーマ定義の詳細](/ja/tutorial/Schema.md)  
- [カスタム データ型](/ja/tutorial/Custom-Data-Type.md)
