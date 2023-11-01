# Custom Data type

This chapter describes how to use user-specific or unsupported types in table definitions built with LDBC.

The following code example assumes the following import

```scala 3
import ldbc.core.*
```

The way to use user-specific or unsupported types is to tell the column data type what type to treat as a data type; DataType provides a `mapping` method that can be used to set this up as an implicit type conversion.

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

LDBC does not allow multiple columns to be merged into a single property of a model, since the purpose of LDBC is to provide a one-to-one mapping between models and tables, and to construct database table definitions in a type-safe manner.

Therefore, it is not allowed to have different number of properties in the table definition and in the model. The following implementation will result in a compile error

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

If you wish to implement the above, please consider the following implementation.

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
