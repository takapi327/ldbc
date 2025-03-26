{%
  laika.title = "Q: How do I use my own type?"
  laika.metadata.language = en
%}

# Q: How do I use my own type?

## A: To use your own custom type, first define an Encoder and Decoder for that type. This allows you to bind values to the database and extract values from query results as custom types. For example, you can define a Status type representing user status as follows:

```scala 3
// Definition of custom Status type
enum Status(val active: Boolean, val label: String):
  case Active   extends Status(true, "Active")
  case Inactive extends Status(false, "Inactive")

// Define an Encoder to bind Status as a Boolean
given Encoder[Status] = Encoder[Boolean].contramap(_.active)

// Define a Decoder to convert from Boolean to Status
given Decoder[Status] = Decoder[Boolean].map {
  case true  => Status.Active
  case false => Status.Inactive
}

// Example of using Codec to integrate Encoder and Decoder
given Codec[Status] =
  Codec[Boolean].imap(b => if b then Status.Active else Status.Inactive)(_.active)
```

In the sample above, the Status type is actually converted to a Boolean value and used in database INSERTs and query result decoding. This maintains type safety in database interactions and allows for easy integration of custom logic.

Additionally, when creating a new type by combining multiple types, you can compose Encoders and Decoders as follows:

```scala 3
// Example of combining two values and converting from tuple to custom type
case class CustomStatus(code: Int, label: String)
given Encoder[CustomStatus] = (Encoder[Int] *: Encoder[String]).to[CustomStatus]
given Decoder[CustomStatus] = (Decoder[Int] *: Decoder[String]).to[CustomStatus]
// or
given Codec[CustomStatus] = (Codec[Int] *: Codec[String]).to[CustomStatus]
```

By defining Encoders, Decoders, and Codecs for your custom types, you can naturally handle custom types in data operations through ldbc.

## References
- [Custom Data Types](/en/tutorial/Custom-Data-Type.md)
- [How to use Codec](/en/tutorial/Custom-Data-Type.md#codec)
