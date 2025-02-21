{%
  laika.title = カスタム データ型
  laika.metadata.language = ja
%}

# カスタム データ型

この章では、ldbcで構築したテーブル定義でユーザー独自の型もしくはサポートされていない型を使用するための方法について説明します。

セットアップで作成したテーブル定義に新たにカラムを追加します。

```sql
ALTER TABLE user ADD COLUMN status BOOLEAN NOT NULL DEFAULT TRUE;
```

## Encoder

ldbcではstatementに受け渡す値を`Encoder`で表現しています。`Encoder`はstatementへのバインドする値を表現するためのtraitです。

`Encoder`を実装することでstatementに受け渡す値をカスタム型で表現することができます。

ユーザー情報にそのユーザーのステータスを表す`Status`を追加します。

```scala 3
enum Status(val done: Boolean, val name: String):
  case Active   extends Status(false, "Active")
  case InActive extends Status(true, "InActive")
```

以下のコード例では、カスタム型の`Encoder`を定義しています。

これによりstatementにカスタム型をバインドすることができるようになります。

```scala 3
given Encoder[Status] = Encoder[Boolean].contramap(_.done)
```

カスタム型は他のパラメーターと同じようにstatementにバインドすることができます。

```scala
val program1: DBIO[Int] =
  sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update
```

これでstatementにカスタム型をバインドすることができるようになりました。

また、Encoderは複数の型を合成して新しい型を作成することができます。

```scala 3
val encoder: Encoder[(Int, String)] = Encoder[Int] *: Encoder[String]
```

合成した型は任意のクラスに変換することもできます。

```scala 3
case class Status(code: Int, name: String)
given Encoder[Status] = (Encoder[Int] *: Encoder[String]).to[Status]
```

## Decoder

ldbcではパラメーターの他に実行結果から独自の型を取得するための`Decoder`も提供しています。

`Decoder`を実装することでstatementの実行結果から独自の型を取得することができます。

以下のコード例では、`Decoder`を使用して単一のデータ型を取得する方法を示しています。

```scala 3
given Decoder[Status] = Decoder[Boolean].map {
  case true  => Status.Active
  case false => Status.InActive
}
```

```scala 3
val program2: DBIO[(String, String, Status)] =
  sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe
```

これでstatementの実行結果からカスタム型を取得することができるようになりました。

Decoderも複数の型を合成して新しい型を作成することができます。

```scala 3
val decoder: Decoder[(Int, String)] = Decoder[Int] *: Decoder[String]
```

合成した型は任意のクラスに変換することもできます。

```scala 3
case class Status(code: Int, name: String)
given Decoder[Status] = (Decoder[Int] *: Decoder[String]).to[Status]
```

## Codec

`Encoder`と`Decoder`を組み合わせた`Codec`を使用することでstatementに受け渡す値とstatementの実行結果から独自の型を取得することができます。

以下のコード例では、`Codec`を使用して先ほどの`Encoder`と`Decoder`を組み合わせた方法を示しています。

```scala 3
given Codec[Status] = Codec[Boolean].imap(_.done)(Status(_))
```

Codecも複数の型を合成して新しい型を作成することができます。

```scala 3
val codec: Codec[(Int, String)] = Codec[Int] *: Codec[String]
```

合成した型は任意のクラスに変換することもできます。

```scala 3
case class Status(code: Int, name: String)
given Codec[Status] = (Codec[Int] *: Codec[String]).to[Status]
```

Codecは、`Encoder`と`Decoder`を組み合わせたものであるため、それぞれの型への変換処理を行うことができます。

```scala 3
val encoder: Encoder[Status] = Codec[Status].asEncoder
val decoder: Decoder[Status] = Codec[Status].asDecoder
```

`Codec`, `Encoder`, `Decoder`はそれぞれ合成することができるため、複数の型を組み合わせて新しい型を作成することができます。

これにより、ユーザーは取得したレコードをネストした階層データに変換できます。

```scala
case class City(id: Int, name: String, countryCode: String)
case class Country(code: String, name: String)
case class CityWithCountry(city: City, country: Country)

sql"SELECT city.Id, city.Name, city.CountryCode, country.Code, country.Name FROM city JOIN country ON city.CountryCode = country.Code".query[CityWithCountry]
```

Codecを始め`Encoder`と`Decoder`は暗黙的に解決されるため、ユーザーはこれらの型を明示的に指定する必要はありません。

しかし、モデル内に多くのプロパティがある場合、暗黙的な検索は失敗する可能性があります。

```shell
[error]    |Implicit search problem too large.
[error]    |an implicit search was terminated with failure after trying 100000 expressions.
[error]    |The root candidate for the search was:
[error]    |
[error]    |  given instance given_Decoder_P in object Decoder  for  ldbc.dsl.codec.Decoder[City]}
```

このような場合は、コンパイルオプションの検索制限を上げると問題が解決することがあります。

```scala
scalacOptions += "-Ximplicit-search-limit:100000"
```

しかし、オプションでの制限拡張はコンパイル時間の増幅につながる可能性があります。その場合は、以下のように手動で任意の型を構築することで解決することもできます。

```scala 3
given Decoder[City] = (Decoder[Int] *: Decoder[String] *: Decoder[Int] *: ....).to[City]
given Encoder[City] = (Encoder[Int] *: Encoder[String] *: Encoder[Int] *: ....).to[City]
```

もしくは、`Codec`を使用して`Encoder`と`Decoder`を組み合わせることで解決することもできます。

```scala 3
given Codec[City] = (Codec[Int] *: Codec[String] *: Codec[Int] *: ....).to[City]
```
