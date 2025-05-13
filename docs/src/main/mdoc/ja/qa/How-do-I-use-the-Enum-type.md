{%
laika.title = "Q: Enum型の使い方は？"
laika.metadata.language = ja
%}

# Q: Enum型の使い方は？

## A: Enum型は、`Codec.derivedEnum`を使用して自動的にエンコード/デコードされます。

Enum型は`Codec.derivedEnum`を使用して自動的にエンコード/デコードされます。以下の例では、`Color`というEnum型を定義し、`Codec.derivedEnum`を使用してエンコード/デコードを行っています。

Enum型はEnumが持つ値をそのまま文字列として扱います。例えば、`Color.Red`は`"Red"`として扱われます。これはMySQLのEnum型で使用することができます。

Example:

```scala 3
import ldbc.codec.*

enum Color:
  case Red, Blue, Yellow
object Color:
  given Codec[Color] = Codec.derivedEnum[Color]

val query = sql"SELECT 'Red'".query[Color].to[Option]
```

Enumを文字列ではなくEnumが持つフィールドの値を使用してエンコード/デコードする場合は、`Codec`を拡張して実装する必要があります。以下の例では、`Color`というEnum型を定義し、`Codec`を拡張してエンコード/デコードを行っています。

```scala 3
import ldbc.codec.*

enum Color(val colorCode: String):
  case Red extends Color("FF0000")
  case Blue extends Color("0000FF")
  case Yellow extends Color("FFFF00")
object Color:
  given Codec[Color] = Codec[String].eimap { str =>
    Color.values.find(_.colorCode == str) match
      case Some(color) => Right(color)
      case None        => Left(s"Invalid color code: $str")
  } (_.colorCode)

val query = sql"SELECT 'FF0000'".query[Color].to[Option]
```

## 参考資料
- [カスタムデータ型](/ja/tutorial/Custom-Data-Type.md)
- [ENUM型と特殊データ型](/ja/tutorial/Schema.md#enum型と特殊データ型)
