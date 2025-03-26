{%
  laika.title = カスタム データ型
  laika.metadata.language = ja
%}

# カスタム データ型

[ロギング](/ja/tutorial/Logging.md)の設定方法を学んだところで、今度はldbcでより表現力豊かなコードを書くために、独自のデータ型をサポートする方法を見ていきましょう。このページでは、基本型だけでなく、ドメイン固有の型を使ってデータベース操作を行う方法を説明します。

実際のアプリケーションでは、単純な基本型だけでなく、ドメインに特化した型を使用したいことがよくあります。例えば、`Status`や`Currency`などのカスタム型を定義し、それらをデータベースの基本型（文字列や整数）とマッピングしたいことがあります。ldbcはこのような操作を簡単に行うための仕組みを提供しています。

この章では、ldbcで構築したテーブル定義でユーザー独自の型もしくはサポートされていない型を使用するための方法について説明します。

セットアップで作成したテーブル定義に新たにカラムを追加します。

```sql
ALTER TABLE user ADD COLUMN status BOOLEAN NOT NULL DEFAULT TRUE;
```

## Encoder

ldbcではstatementに受け渡す値を`Encoder`で表現しています。`Encoder`はstatementにバインドする値を表現するためのtraitです。

`Encoder`を実装することでstatementに受け渡す値をカスタム型で表現することができます。

### 基本的な使い方

ユーザー情報にそのユーザーのステータスを表す`Status`を追加します。以下は`enum`を使った例です：

```scala 3
enum Status(val done: Boolean, val name: String):
  case Active   extends Status(false, "Active")
  case InActive extends Status(true, "InActive")
```

以下のコード例では、カスタム型の`Encoder`を定義しています。`contramap`メソッドを使用して、カスタム型から基本型（ここでは`Boolean`）への変換方法を指定します：

```scala 3
given Encoder[Status] = Encoder[Boolean].contramap(_.done)
```

これによりstatementにカスタム型をバインドすることができるようになります。具体的な使用例は次のとおりです：

```scala 3
val program1: DBIO[Int] =
  sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update
```

### 複合型のEncoder

Encoderは複数の型を合成して新しい型を作成することもできます。`*:`演算子を使用して型を合成できます：

```scala 3
val encoder: Encoder[(Int, String)] = Encoder[Int] *: Encoder[String]
```

合成した型は任意のクラスに変換することもできます。以下の例では、`to`メソッドを使用してタプルからケースクラスへの変換を行っています：

```scala 3
case class Status(code: Int, name: String)
given Encoder[Status] = (Encoder[Int] *: Encoder[String]).to[Status]
```

この場合、`Status`クラスのフィールドは順番通りに対応する必要があります。つまり、`code`は`Int`型のエンコーダーに、`name`は`String`型のエンコーダーに対応します。

## Decoder

ldbcではパラメーターの他に実行結果から独自の型を取得するための`Decoder`も提供しています。

`Decoder`を実装することでstatementの実行結果から独自の型を取得することができます。

### 基本的な使い方

以下のコード例では、`Decoder`を使用して`Boolean`型の値を`Status`型に変換する方法を示しています：

```scala 3
given Decoder[Status] = Decoder[Boolean].map {
  case true  => Status.InActive
  case false => Status.Active
}
```

これを使って、クエリ結果から`Status`型の値を直接取得できるようになります：

```scala 3
val program2: DBIO[(String, String, Status)] =
  sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe
```

### 複合型のDecoder

Decoderも複数の型を合成して新しい型を作成することができます。`*:`演算子を使用して型を合成します：

```scala 3
val decoder: Decoder[(Int, String)] = Decoder[Int] *: Decoder[String]
```

合成した型は任意のクラスに変換することもできます：

```scala 3
case class Status(code: Int, name: String)
given Decoder[Status] = (Decoder[Int] *: Decoder[String]).to[Status]
```

この定義により、データベースから取得した2つのカラム（整数と文字列）を自動的に`Status`クラスのインスタンスに変換できます。

## Codec

`Encoder`と`Decoder`を組み合わせた`Codec`を使用することで、statementに受け渡す値とstatementの実行結果の両方で独自の型を使用できます。これにより、コードの重複を減らし、一貫した型変換を実現できます。

### 基本的な使い方

以下のコード例では、`Codec`を使用して先ほどの`Encoder`と`Decoder`を統合した方法を示しています：

```scala 3
given Codec[Status] = Codec[Boolean].imap(_.done)(Status(_))
```

### 複合型のCodec

Codecも複数の型を合成して新しい型を作成することができます：

```scala 3
val codec: Codec[(Int, String)] = Codec[Int] *: Codec[String]
```

合成した型は任意のクラスに変換することもできます：

```scala 3
case class Status(code: Int, name: String)
given Codec[Status] = (Codec[Int] *: Codec[String]).to[Status]
```

### EncoderとDecoderを個別に取得

Codecは、`Encoder`と`Decoder`を組み合わせたものであるため、それぞれの型への変換処理を個別に取得することもできます：

```scala 3
val encoder: Encoder[Status] = Codec[Status].asEncoder
val decoder: Decoder[Status] = Codec[Status].asDecoder
```

## 複雑なオブジェクト構造の変換

`Codec`, `Encoder`, `Decoder`はそれぞれ合成することができるため、複数の型を組み合わせて複雑なオブジェクト構造を作成することができます。

これにより、ユーザーは取得したレコードをネストした階層データに変換できます：

```scala 3
case class City(id: Int, name: String, countryCode: String)
case class Country(code: String, name: String)
case class CityWithCountry(city: City, country: Country)

// 都市と国の情報を結合して取得する例
val program3: DBIO[List[CityWithCountry]] =
  sql"""
    SELECT c.id, c.name, c.country_code, co.code, co.name 
    FROM city c 
    JOIN country co ON c.country_code = co.code
  """.query[CityWithCountry].list
```

上記の例では、クエリの結果から自動的に`CityWithCountry`オブジェクトが構築されます。ldbcはコンパイル時に型を解決し、適切なエンコーダーとデコーダーを生成します。

## 大きなオブジェクトの扱い

Codecを始め`Encoder`と`Decoder`は暗黙的に解決されるため、通常はユーザーがこれらの型を明示的に指定する必要はありません。

しかし、モデル内に多くのプロパティがある場合、暗黙的な解決処理が複雑になりすぎて失敗する可能性があります：

```shell
[error]    |Implicit search problem too large.
[error]    |an implicit search was terminated with failure after trying 100000 expressions.
[error]    |The root candidate for the search was:
[error]    |
[error]    |  given instance given_Decoder_P in object Decoder  for  ldbc.dsl.codec.Decoder[City]}
```

このような場合は、以下のいずれかの解決策が有効です：

1. コンパイルオプションの検索制限を上げる：

```scala
scalacOptions += "-Ximplicit-search-limit:100000"
```

ただし、この方法はコンパイル時間が長くなる可能性があります。

2. 手動で型変換を明示的に構築する：

```scala 3
// 明示的にDecoderを構築
given Decoder[City] = (
  Decoder[Int] *: 
  Decoder[String] *: 
  Decoder[String]
).to[City]

// 明示的にEncoderを構築
given Encoder[City] = (
  Encoder[Int] *: 
  Encoder[String] *: 
  Encoder[String]
).to[City]
```

3. `Codec`を使用して一度に定義する：

```scala 3
given Codec[City] = (
  Codec[Int] *: 
  Codec[String] *: 
  Codec[String]
).to[City]
```

## 実際の応用例

以下は、より実践的な例として、ドメイン固有の型を使用したコード例を示します：

```scala 3
// メールアドレスを表す値
opaque type Email = String
object Email:
  def apply(value: String): Email = value
  def unapply(email: Email): String = email

// ユーザーID
opaque type UserId = Long
object UserId:
  def apply(value: Long): UserId = value
  def unapply(userId: UserId): Long = userId

// ユーザークラス
case class User(id: UserId, name: String, email: Email, status: Status)
object User:
  // ユーザーID用のCodec
  given Codec[UserId] = Codec[Long].imap(UserId.apply)(_.value)
  
  // メールアドレス用のCodec
  given Codec[Email] = Codec[String].imap(Email.apply)(Email.unapply)

// これでユーザーの取得や更新が型安全に行える
val getUser: DBIO[Option[User]] = 
  sql"SELECT id, name, email, status FROM user WHERE id = ${UserId(1)}".query[User].option

val updateEmail: DBIO[Int] =
  sql"UPDATE user SET email = ${Email("new@example.com")} WHERE id = ${UserId(1)}".update
```

## 次のステップ

これでカスタムデータ型をldbcで使用する方法がわかりました。独自の型を定義することで、より表現力豊かで型安全なコードを書くことができます。また、ドメインの概念を正確にコードで表現できるようになり、バグの発生を減らすことができます。

次は[クエリビルダー](/ja/tutorial/Query-Builder.md)に進み、SQLを直接書かずに型安全なクエリを構築する方法を学びましょう。
