# カスタム データ型

この章では、LDBCで構築したテーブル定義でユーザー独自の型もしくはサポートされていない型を使用するための方法について説明します。

以下のコード例では、以下のimportを想定しています。

```scala 3
import ldbc.core.*
```

ユーザー独自の型もしくはサポートされていない型を使用するための方法はカラムのデータ型をどのような型として扱うかを教えてあげることです。DataTypeには`mapping`メソッドが提供されているのでこのメソッドを使用して暗黙の型変換として設定します。

```scala 3
case class User(
  id: Long,
  name: User.Name,
  age: Option[Int],
)

object User:

  case class Name(firstName: String, lastName: String)

  given Conversion[VARCHAR[String], DataType[Name]] = DataType.mapping[VARCHAR[String], Name]

  val table = Table[User]("user")(
    column("id", BIGINT[Long], AUTO_INCREMENT),
    column("name", VARCHAR(255)),
    column("age", INT.UNSIGNED.DEFAULT(None))
  )
```

LDBCでは複数のカラムをモデルが持つ1つのプロパティに統合することはできません。LDBCの目的はモデルとテーブルを1対1でマッピングを行い、データベースのテーブル定義を型安全に構築することにあるからです。

そのためテーブル定義とモデルで異なった数のプロパティを持つようなことは許可していません。以下のような実装はコンパイルエラーとなります。

```scala 3
case class User(
  id: Long,
  name: User.Name,
  age: Option[Int],
)

object User:

  case class Name(firstName: String, lastName: String)

  val table = Table[User]("user")(
    column("id", BIGINT[Long], AUTO_INCREMENT),
    column("first_name", VARCHAR(255)),
    column("last_name", VARCHAR(255)),
    column("age", INT.UNSIGNED.DEFAULT(None))
  )
```

上記のような実装を行いたい場合は以下のような実装を検討してください。

```scala 3
case class User(
  id: Long,
  firstName: String, 
  lastName: String,
  age: Option[Int],
):
  
  val name: User.Name = User.Name(firstName, lastName)

object User:

  case class Name(firstName: String, lastName: String)

  val table = Table[User]("user")(
    column("id", BIGINT[Long], AUTO_INCREMENT),
    column("first_name", VARCHAR(255)),
    column("last_name", VARCHAR(255)),
    column("age", INT.UNSIGNED.DEFAULT(None))
  )
```
