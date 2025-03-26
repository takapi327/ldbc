{%
  laika.title = "Q: ネストしたモデルを使うにはどうすればいいですか？"
  laika.metadata.language = ja
%}

# Q: ネストしたモデルを使うにはどうすればいいですか？

## A: ldbcでは、複数のカラムを組み合わせてネストしたモデルにマッピングすることができます。  
例えば、UserモデルにNameというネストしたモデルを持たせ、データベース上では分割されたカラム（例: first_name、last_name）にマッピングする方法は以下の通りです。

```scala 3
// ネストしたモデルの定義
case class User(id: Long, name: User.Name, email: String)
object User:
  case class Name(firstName: String, lastName: String)

// テーブル定義の例
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def firstName: Column[String] = varchar(255)
  def lastName: Column[String] = varchar(255)
  def email: Column[String] = varchar(255)
  
  // (firstName *: lastName)をUser.Nameに変換し、id、名前、emailをUserにマッピングする
  override def * : Column[User] =
    (id *: (firstName *: lastName).to[User.Name] *: email).to[User]
  
// 使用例:
TableQuery[UserTable].selectAll.query.to[List].foreach(println)
// UserTableのselectで取得されたレコードは、自動的にUser.Name(firstName, lastName)に変換される
```

この定義により、データベースの`first_name`と`last_name`カラムがそれぞれUser.Nameの`firstName`および`lastName`に対応し、ネストしたモデルとして利用できます。

## 参考資料
- [スキーマ定義の詳細](/ja/tutorial/Schema.md)  
- [カスタム データ型](/ja/tutorial/Custom-Data-Type.md)
