{%
  laika.title = Custom Data Types
  laika.metadata.language = en
%}

# Custom Data Types

Now that we've learned how to configure [Logging](/en/tutorial/Logging.md), let's look at how to support your own data types in ldbc to write more expressive code. This page explains how to perform database operations using not only basic types but also domain-specific types.

In real applications, you often want to use domain-specific types rather than just simple basic types. For example, you might want to define custom types like `Status` or `Currency` and map them to database basic types (strings or integers). ldbc provides mechanisms to do this easily.

This chapter explains how to use custom types or unsupported types in table definitions built with ldbc.

Let's add a new column to the table definition created during setup.

```sql
ALTER TABLE user ADD COLUMN status BOOLEAN NOT NULL DEFAULT TRUE;
```

## Encoder

In ldbc, values passed to statements are represented by `Encoder`. `Encoder` is a trait for representing values to be bound to statements.

By implementing `Encoder`, you can represent values passed to statements with custom types.

### Basic Usage

Let's add a `Status` to represent the user's status in the user information. Here's an example using an `enum`:

```scala 3
enum Status(val done: Boolean, val name: String):
  case Active   extends Status(false, "Active")
  case InActive extends Status(true, "InActive")
```

In the code example below, we define an `Encoder` for a custom type. The `contramap` method is used to specify how to convert from the custom type to a basic type (in this case, `Boolean`):

```scala 3
given Encoder[Status] = Encoder[Boolean].contramap(_.done)
```

This allows you to bind custom types to statements. Here's a specific example:

```scala 3
val program1: DBIO[Int] =
  sql"INSERT INTO user (name, email, status) VALUES (${ "user 1" }, ${ "user@example.com" }, ${ Status.Active })".update
```

### Composite Type Encoder

Encoders can also create new types by composing multiple types. You can compose types using the `*:` operator:

```scala 3
val encoder: Encoder[(Int, String)] = Encoder[Int] *: Encoder[String]
```

Composed types can also be converted to arbitrary classes. In the example below, the `to` method is used to convert from a tuple to a case class:

```scala 3
case class Status(code: Int, name: String)
given Encoder[Status] = (Encoder[Int] *: Encoder[String]).to[Status]
```

In this case, the fields of the `Status` class must correspond in order. That is, `code` corresponds to the `Int` encoder, and `name` corresponds to the `String` encoder.

## Decoder

In addition to parameters, ldbc also provides `Decoder` for retrieving custom types from execution results.

By implementing `Decoder`, you can retrieve custom types from statement execution results.

### Basic Usage

The code example below shows how to use `Decoder` to convert a `Boolean` value to a `Status` type:

```scala 3
given Decoder[Status] = Decoder[Boolean].map {
  case true  => Status.InActive
  case false => Status.Active
}
```

This allows you to directly retrieve `Status` type values from query results:

```scala 3
val program2: DBIO[(String, String, Status)] =
  sql"SELECT name, email, status FROM user WHERE id = 1".query[(String, String, Status)].unsafe
```

### Composite Type Decoder

Decoders can also create new types by composing multiple types. Use the `*:` operator to compose types:

```scala 3
val decoder: Decoder[(Int, String)] = Decoder[Int] *: Decoder[String]
```

Composed types can also be converted to arbitrary classes:

```scala 3
case class Status(code: Int, name: String)
given Decoder[Status] = (Decoder[Int] *: Decoder[String]).to[Status]
```

This definition allows two columns (integer and string) retrieved from the database to be automatically converted to instances of the `Status` class.

## Codec

By using `Codec`, which combines `Encoder` and `Decoder`, you can use custom types for both values passed to statements and statement execution results. This reduces code duplication and achieves consistent type conversions.

### Basic Usage

The code example below shows how to use `Codec` to integrate the `Encoder` and `Decoder` from earlier:

```scala 3
given Codec[Status] = Codec[Boolean].imap(_.done)(Status(_))
```

### Composite Type Codec

Codecs can also create new types by composing multiple types:

```scala 3
val codec: Codec[(Int, String)] = Codec[Int] *: Codec[String]
```

Composed types can also be converted to arbitrary classes:

```scala 3
case class Status(code: Int, name: String)
given Codec[Status] = (Codec[Int] *: Codec[String]).to[Status]
```

### Retrieving Encoder and Decoder Individually

Since Codec is a combination of `Encoder` and `Decoder`, you can also get the conversion process for each type individually:

```scala 3
val encoder: Encoder[Status] = Codec[Status].asEncoder
val decoder: Decoder[Status] = Codec[Status].asDecoder
```

## Converting Complex Object Structures

Since `Codec`, `Encoder`, and `Decoder` can each be composed, complex object structures can be created by combining multiple types.

This allows users to convert retrieved records into nested hierarchical data:

```scala 3
case class City(id: Int, name: String, countryCode: String)
case class Country(code: String, name: String)
case class CityWithCountry(city: City, country: Country)

// Example of retrieving joined city and country information
val program3: DBIO[List[CityWithCountry]] =
  sql"""
    SELECT c.id, c.name, c.country_code, co.code, co.name 
    FROM city c 
    JOIN country co ON c.country_code = co.code
  """.query[CityWithCountry].list
```

In the example above, `CityWithCountry` objects are automatically constructed from the query results. ldbc resolves types at compile time and generates appropriate encoders and decoders.

## Handling Large Objects

Since Codec, along with `Encoder` and `Decoder`, is implicitly resolved, users do not usually need to explicitly specify these types.

However, if there are many properties in a model, the implicit resolution process might become too complex and fail:

```shell
[error]    |Implicit search problem too large.
[error]    |an implicit search was terminated with failure after trying 100000 expressions.
[error]    |The root candidate for the search was:
[error]    |
[error]    |  given instance given_Decoder_P in object Decoder  for  ldbc.dsl.codec.Decoder[City]}
```

In such cases, one of the following solutions is effective:

1. Increase the search limit in compile options:

```scala
scalacOptions += "-Ximplicit-search-limit:100000"
```

However, this method may increase compilation time.

2. Manually construct explicit type conversions:

```scala 3
// Explicitly build Decoder
given Decoder[City] = (
  Decoder[Int] *: 
  Decoder[String] *: 
  Decoder[String]
).to[City]

// Explicitly build Encoder
given Encoder[City] = (
  Encoder[Int] *: 
  Encoder[String] *: 
  Encoder[String]
).to[City]
```

3. Use `Codec` to define both at once:

```scala 3
given Codec[City] = (
  Codec[Int] *: 
  Codec[String] *: 
  Codec[String]
).to[City]
```

## Practical Application Examples

Below is an example of code using domain-specific types as a more practical example:

```scala 3
// Value representing an email address
opaque type Email = String
object Email:
  def apply(value: String): Email = value
  def unapply(email: Email): String = email

// User ID
opaque type UserId = Long
object UserId:
  def apply(value: Long): UserId = value
  def unapply(userId: UserId): Long = userId

// User class
case class User(id: UserId, name: String, email: Email, status: Status)
object User:
  // Codec for User ID
  given Codec[UserId] = Codec[Long].imap(UserId.apply)(_.value)
  
  // Codec for Email
  given Codec[Email] = Codec[String].imap(Email.apply)(Email.unapply)

// Now you can retrieve and update users in a type-safe manner
val getUser: DBIO[Option[User]] = 
  sql"SELECT id, name, email, status FROM user WHERE id = ${UserId(1)}".query[User].option

val updateEmail: DBIO[Int] =
  sql"UPDATE user SET email = ${Email("new@example.com")} WHERE id = ${UserId(1)}".update
```

## Next Steps

Now you know how to use custom data types with ldbc. By defining your own types, you can write more expressive and type-safe code. You can also accurately represent domain concepts in code, reducing the occurrence of bugs.

Next, let's move on to [Query Builder](/en/tutorial/Query-Builder.md) to learn how to build type-safe queries without writing SQL directly.
