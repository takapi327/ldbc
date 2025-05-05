{%
  laika.metadata.language = ja
  laika.title = "Q: クエリビルダーでカラム名を変更する方法は？"
%}

# Q: クエリビルダーでカラム名を変更する方法は？

## A: クエリビルダーでは、モデル定義においてカラム名を変更する方法として、主に2つの手法が利用できます。

### A: 1. アノテーションを利用する方法  
モデルのフィールドに`@Column`アノテーションを追加することで、クエリで使用されるカラム名を指定できます。  
例えば、Userモデルの`name`フィールドを`full_name`として扱いたい場合、次のように定義します。

```scala 3
case class User(
  id: Int,
  @Column("full_name") name: String,
  email: String
) derives Table

val query = TableQuery[User].select(user => user.id *: user.name *: user.email)
// クエリ生成時、nameフィールドは "full_name" として扱われる
println(query.statement)
// 出力例: "SELECT `id`, `full_name`, `email` FROM user"
```

### A: 2. クエリビルダーのエイリアス機能を利用する方法  
モデル定義に変更を加えず、クエリ構築時にカラムの別名（エイリアス）を指定する方法も提供されています。  
以下の例では、`alias`関数またはカスタムのマッピング関数を使って、取得時のカラム名を変更する例を示します。

```scala 3
import ldbc.dsl.codec.Codec
import ldbc.query.builder.*

case class User(id: Int, name: String, email: String) derives Table
object User:
  given Codec[User] = Codec.derived[User]

val userTable = TableQuery[User]

// クエリを構築し、select句でエイリアスを指定する
val queryWithAlias = userTable
  .select(user => user.id *: user.name.as("full_name") *: user.email)
  
println(queryWithAlias.statement)
// 出力例: "SELECT `id`, `name` AS `full_name`, email FROM user"
```

以上のように、クエリビルダーではモデル定義時のアノテーションによる方法と、クエリ構築時のエイリアス指定による方法を利用して、カラム名のフォーマットや表示を変更できます。

## 参考資料
- [クエリビルダーの使い方](/ja/tutorial/Query-Builder.md)  
- [スキーマ定義の詳細](/ja/tutorial/Schema.md)
