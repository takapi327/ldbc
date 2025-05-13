{%
laika.title = "Q: How to use the Enum type?"
laika.metadata.language = ja
%}

# Q: How to use the Enum type?

## A: EEnum types are automatically encoded/decoded using `Codec.derivedEnum`.

Enum types are automatically encoded/decoded using `Codec.derivedEnum`. The following example defines an Enum type named `Color` and encodes/decodes it using `Codec.derivedEnum`.

The Enum type treats the value of the Enum as it is as a string. For example, `Color.Red` is treated as `“Red”`. This can be used with MySQL's Enum type.

Example:

```scala 3
import ldbc.codec.*

enum Color:
  case Red, Blue, Yellow
object Color:
  given Codec[Color] = Codec.derivedEnum[Color]

val query = sql"SELECT 'Red'".query[Color].to[Option]
```

If you want to encode/decode an Enum using the values of the fields it contains instead of strings, you need to extend `Codec` to implement it. The following example defines an Enum type called `Color` and extends `Codec` to encode/decode it.

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

## References
- [Custom Data Types](/en/tutorial/Custom-Data-Type.md)
- [ENUM Type and Special Data Types](/en/tutorial/Schema.md#enum-type-and-special-data-types)
