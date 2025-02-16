{%
  laika.title = Schema
  laika.metadata.language = en
%}

# Schema

This chapter describes how to work with database schemas in Scala code, especially how to manually write a schema, which is useful when starting to write an application without an existing database. If you already have a schema in your database, you can skip this step using Code Generator.

The following dependencies must be set up for your project

```scala
//> using dep "@ORGANIZATION@::ldbc-schema:@VERSION@"
```

The following code example assumes the following import

```scala 3
import ldbc.schema.*
import ldbc.schema.attribute.*
```

ldbc maintains a one-to-one mapping between Scala models and database table definitions. The mapping between the properties held by the model and the columns held by the table is done in the order of definition. Table definitions are very similar to the structure of a Create statement. This makes the construction of table definitions intuitive for the user.

ldbc uses these table definitions for a variety of purposes. Generating type-safe queries, generating documents, etc.

```scala 3
case class User(
  id:    Int,
  name:  String,
  email: String,
)

val table = Table[User]("user")(                  // CREATE TABLE `user` (
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY), //   `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  column("name", VARCHAR(50)),                    //   `name` VARCHAR(50) NOT NULL,
  column("email", VARCHAR(100)),                  //   `email` VARCHAR(100) NOT NULL,
)                                                 // );
```

All columns are defined by the column method. Each column has a column name, data type, and attributes. The following primitive types are supported by default and are ready to use

- Numeric types: `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `BigDecimal`, `BigInt`
- LOB types: `java.sql.Blob`, `java.sql.Clob`, `Array[Byte]`
- Date types: `java.sql.Date`, `java.sql.Time`, `java.sql.Timestamp`
- String
- Boolean
- java.time.*

Nullable columns are represented by `Option[T]`, where T is one of the supported primitive types; note that any column that is not of type Option is Not Null.

## Data Type

The mapping of the Scala type of property that a model has to the data type that a column has requires that the defined data type supports the Scala type. Attempting to assign an unsupported type will result in a compile error.

The Scala types supported by the data type are listed in the table below.

| Data Type    | Scala Type                                                                                      |
|--------------|-------------------------------------------------------------------------------------------------|
| `BIT`        | `Byte, Short, Int, Long`                                                                        |
| `TINYINT`    | `Byte, Short`                                                                                   |
| `SMALLINT`   | `Short, Int`                                                                                    |
| `MEDIUMINT`  | `Int`                                                                                           |
| `INT`        | `Int, Long`                                                                                     |
| `BIGINT`     | `Long, BigInt`                                                                                  |
| `DECIMAL`    | `BigDecimal`                                                                                    |
| `FLOAT`      | `Float`                                                                                         |
| `DOUBLE`     | `Double`                                                                                        |
| `CHAR`       | `String`                                                                                        |
| `VARCHAR`    | `String`                                                                                        |
| `BINARY`     | `Array[Byte]`                                                                                   |
| `VARBINARY`  | `Array[Byte]`                                                                                   |
| `TINYBLOB`   | `Array[Byte]`                                                                                   |
| `BLOB`       | `Array[Byte]`                                                                                   |
| `MEDIUMBLOB` | `Array[Byte]`                                                                                   |
| `LONGBLOB`   | `Array[Byte]`                                                                                   |
| `TINYTEXT`   | `String`                                                                                        |
| `TEXT`       | `String`                                                                                        |
| `MEDIUMTEXT` | `String`                                                                                        |
| `DATE`       | `java.time.LocalDate`                                                                           |
| `DATETIME`   | `java.time.Instant, java.time.LocalDateTime, java.time.OffsetTime`                              |
| `TIMESTAMP`  | `java.time.Instant, java.time.LocalDateTime, java.time.OffsetDateTime, java.time.ZonedDateTime` |
| `TIME`       | `java.time.LocalTime`                                                                           |
| `YEAR`       | `java.time.Instant, java.time.LocalDate, java.time.Year`                                        |
| `BOOLEA`     | `Boolean`                                                                                       |

**Note on working with integer types**

It should be noted that the range of data that can be handled, depending on whether it is signed or unsigned, does not fit within the Scala types.

| Data Type   | Signed Range                                 | Unsigned Range             | Scala Type       | Range                                                                |
|-------------|----------------------------------------------|----------------------------|------------------|----------------------------------------------------------------------|
| `TINYINT`   | `-128 ~ 127`                                 | `0 ~ 255`                  | `Byte<br>Short`  | `-128 ~ 127<br>-32768～32767`                                         |
| `SMALLINT`  | `-32768 ~ 32767`                             | `0 ~ 65535`                | `Short<br>Int`   | `-32768～32767<br>-2147483648～2147483647`                             |
| `MEDIUMINT` | `-8388608 ~ 8388607`                         | `0 ~ 16777215`             | `Int`            | `-2147483648～2147483647`                                             |
| `INT`       | `-2147483648	~ 2147483647`                   | `0 ~ 4294967295`           | `Int<br>Long`    | `-2147483648～2147483647<br>-9223372036854775808～9223372036854775807` |
| `BIGINT`    | `-9223372036854775808 ~ 9223372036854775807` | `0 ~ 18446744073709551615` | `Long<br>BigInt` | `-9223372036854775808～9223372036854775807<br>...`                    |

To work with user-defined proprietary or unsupported types, see Custom Data Types.

## Attributes

Columns can be assigned various attributes.

- `AUTO_INCREMENT`.
  Create a DDL statement to mark a column as an auto-increment key when documenting SchemaSPY.
  MySQL cannot return columns that are not AutoInc when inserting data. Therefore, if necessary, ldbc will check to see if the return column is properly marked as AutoInc.
- `PRIMARY_KEY`.
  Mark the column as a primary key when creating DDL statements or SchemaSPY documents.
- `UNIQUE_KEY`.
  Marks a column as a unique key when creating a DDL statement or SchemaSPY document.
- `COMMENT`.
  Marks a column as a comment when creating a DDL statement or SchemaSPY document.

## Setting Keys

MySQL allows you to set various keys for your tables, such as Unique keys, Index keys, foreign keys, etc. Let's look at how to set these keys in a table definition built with ldbc.

### PRIMARY KEY

A primary key is an item that uniquely identifies data in MySQL. When a column has a primary key constraint, it can only contain values that do not duplicate the values of other data. It also cannot contain NULLs. As a result, only one piece of data in the table can be identified by looking up the value of a column with a primary key constraint set.

In ldbc, this primary key constraint can be set in two ways.

1. set as an attribute of column method
2. set by keySet method of table

**Setting as an attribute of the column method**.

It is very easy to set a column method attribute by simply passing `PRIMARY_KEY` as the third or later argument of the column method. This allows you to set the `id` column as the primary key in the following cases.

```scala 3
column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
```

Set by keySet method of **table**.

ldbc table definitions have a method called `keySet`, where you can set the column you want to set as the primary key by passing `PRIMARY_KEY` as the column name.

```scala 3
val table = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)
  .keySet(table => PRIMARY_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   PRIMARY KEY (`id`)
// )
```

The `PRIMARY_KEY` method can be set to the following parameters in addition to the columns.

- `Index Type` ldbc.schema.Index.Type.BTREE or ldbc.schema.Index.Type.HASH
- `Index Option` ldbc.schema.Index.IndexOption

#### composite key (primary key)

Not only one column, but also multiple columns can be combined as a primary key. You can set up a composite primary key by simply passing `PRIMARY_KEY` with the columns you want as primary keys.

```scala 3
val table = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)
  .keySet(table => PRIMARY_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   PRIMARY KEY (`id`, `name`)
// )
```

Compound keys can only be set with `PRIMARY_KEY` in the `keySet` method. If you set multiple attributes in the column method as shown below, each attribute will be set as a primary key, not as a compound key.

In ldbc, setting multiple `PRIMARY_KEY`s in a table definition does not cause a compile error. However, if the table definition is used in query generation, document generation, etc., an error will occur. This is due to the restriction that only one PRIMARY KEY can be set per table.

```scala 3
val table = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(50), PRIMARY_KEY),
  column("email", VARCHAR(100))
)

// CREATE TABLE `user` (
//   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
// )
```

### UNIQUE KEY

A unique key is an item that uniquely identifies data in MySQL. When a uniqueness constraint is set on a column, the column can only contain values that do not duplicate the values of other data.

In ldbc, this uniqueness constraint can be set in two ways. 1.

1. as an attribute of the column method
2. in the keySet method of table

**Setting as an attribute of the column method**.

It is very easy to set a column method as an attribute by simply passing `UNIQUE_KEY` as the third or later argument of the column method. This allows you to set the `id` column as a unique key in the following cases.

```scala 3
column("id", BIGINT, AUTO_INCREMENT, UNIQUE_KEY)
```

**Set by keySet method of table**

The ldbc table definition has a method called `keySet` where you can set a column as a unique key by passing `UNIQUE_KEY` as the column name you want to set as a unique key.

```scala 3
val table = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)
  .keySet(table => UNIQUE_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   UNIQUE KEY (`id`)
// )
```

The `UNIQUE_KEY` method accepts the following parameters in addition to the columns.

- `Index Name` String
- `Index Type` ldbc.schema.Index.Type.BTREE or ldbc.schema.Index.Type.HASH
- `Index Option` ldbc.schema.Index.IndexOption

#### composite key (unique key)

You can set not only one column as a unique key, but also multiple columns as a combined unique key. You can set up a composite unique key by simply passing `UNIQUE_KEY` with multiple columns that you want to set as unique keys.

```scala 3
val table = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)
  .keySet(table => UNIQUE_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   UNIQUE KEY (`id`, `name`)
// )
```

Compound keys can only be set with `UNIQUE_KEY` in the `keySet` method. If you set multiple keys as attributes in the column method, each will be set as a unique key, not as a compound key.

### INDEX KEY

An index key is an “index” in MySQL to efficiently retrieve the desired record.

In ldbc, this index can be set in two ways.

1. as an attribute of the column method
2. by using the keySet method of table.

**Set as an attribute of the column method**

It is very easy to set a column method as an attribute, just pass `INDEX_KEY` as the third argument or later of the column method. This allows you to set the `id` column as an index in the following cases

```scala 3
column("id", BIGINT, AUTO_INCREMENT, INDEX_KEY)
```

**Set by keySet method of table**

The ldbc table definition has a method called `keySet`, where you can set a column as an index key by passing the column you want to set as an index to `INDEX_KEY`.

```scala 3
val table = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)
  .keySet(table => INDEX_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   INDEX KEY (`id`)
// )
```

The `INDEX_KEY` method accepts the following parameters in addition to the columns.

- `Index Name` String
- Index Type` ldbc.schema.Index.Type.BTREE or ldbc.schema.Index.Type.HASH
- `Index Option` ldbc.schema.Index.IndexOption

#### composite key (index key)

You can set not only one column but also multiple columns as index keys as a combined index key. You can set up a composite index by simply passing `INDEX_KEY` with multiple columns that you want to set as index keys.

```scala 3
val table = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)
  .keySet(table => INDEX_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   INDEX KEY (`id`, `name`)
// )
```

Compound keys can only be set with `INDEX_KEY` in the `keySet` method. If you set multiple columns as attributes of the `column` method, they will each be set as an index key, not as a composite index.

### FOREIGN KEY

A foreign key is a data integrity constraint (referential integrity constraint) in MySQL.  A column set to a foreign key can only have values that exist in the columns of the referenced table.

In ldbc, this foreign key constraint can be set by using the keySet method of table.

```scala 3
val user = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)

val order = Table[Order]("order")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("user_id", VARCHAR(50))
  ...
)
  .keySet(table => FOREIGN_KEY(table.userId, REFERENCE(user, user.id)))

// CREATE TABLE `order` (
//   ...,
//   FOREIGN KEY (user_id) REFERENCES `user` (id),
// )
```

The `FOREIGN_KEY` method accepts the following parameters in addition to column and reference values.

- `Index Name` String

Foreign key constraints can be used to set the behavior of the parent table on delete and update. The `REFERENCE` method provides `onDelete` and `onUpdate` methods that can be used to set these parameters.

Values that can be set can be obtained from `ldbc.schema.Reference.ReferenceOption`.

```scala 3
val order = Table[Order]("order")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("user_id", VARCHAR(50))
  ...
)
  .keySet(table => FOREIGN_KEY(table.userId, REFERENCE(user, user.id).onDelete(Reference.ReferenceOption.RESTRICT)))

// CREATE TABLE `order` (
//   ...,
//   FOREIGN KEY (`user_id`)  REFERENCES `user` (`id`) ON DELETE RESTRICT
// )
```

Possible values are

- `RESTRICT`: deny delete or update operations on the parent table.
- `CASCADE`: delete or update a row from the parent table and automatically delete or update the matching row in the child table.
- `SET_NULL`: deletes or updates a row from the parent table and sets a foreign key column in the child table to NULL.
- `NO_ACTION`: Standard SQL keyword. In MySQL, equivalent to RESTRICT.
- `SET_DEFAULT`: This action is recognized by the MySQL parser, but both InnoDB and NDB will reject table definitions containing an ON DELETE SET DEFAULT or ON UPDATE SET DEFAULT clause.

#### composite key (foreign key)

Not only one column, but also multiple columns can be combined as a foreign key. Simply pass multiple columns to `FOREIGN_KEY` to be set as foreign keys as a compound foreign key.

```scala 3
val user = Table[User]("user")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(50)),
  column("email", VARCHAR(100))
)

val order = Table[Order]("order")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("user_id", VARCHAR(50))
  column("user_email", VARCHAR(100))
  ...
)
  .keySet(table => FOREIGN_KEY((table.userId, table.userEmail), REFERENCE(user, (user.id, user.email))))

// CREATE TABLE `user` (
//   ...,
//   FOREIGN KEY (`user_id`, `user_email`)  REFERENCES `user` (`id`, `email`)
// )
```

### constraint name

MySQL allows you to give arbitrary names to constraints by using CONSTRAINT. The constraint name must be unique per database.

Since ldbc provides the CONSTRAINT method, you can set constraints such as key constraints by simply passing them to the CONSTRAINT method.

```scala 3
val order = Table[Order]("order")(
  column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
  column("user_id", VARCHAR(50))
  ...
)
  .keySet(table => CONSTRAINT("fk_user_id", FOREIGN_KEY(table.userId, REFERENCE(user, user.id))))

// CREATE TABLE `order` (
//   ...,
//   CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`)  REFERENCES `user` (`id`)
// )
```

## Custom data types

The way to use user-specific or unsupported types is to tell them what type to treat the column data type as; DataType provides a `mapping` method that can be used to set this up as an implicit type conversion.

```scala 3
case class User(
  id:    Int,
  name:  User.Name,
  email: String,
)

object User:

  case class Name(firstName: String, lastName: String)

  given Conversion[VARCHAR[String], DataType[Name]] = DataType.mapping[VARCHAR[String], Name]

  val table = Table[User]("user")(
    column("id", INT, AUTO_INCREMENT),
    column("name", VARCHAR(50)),
    column("email", VARCHAR(100))
  )
```

ldbc does not allow multiple columns to be merged into a single property of the model, since the purpose of ldbc is to provide a one-to-one mapping between model and table, and to type-safe the table definitions in the database.

Therefore, it is not allowed to have different number of properties in a table definition and in a model. The following implementation will result in a compile error

```scala 3
case class User(
  id:    Int,
  name:  User.Name,
  email: String,
)

object User:

  case class Name(firstName: String, lastName: String)

  val table = Table[User]("user")(
    column("id", INT, AUTO_INCREMENT),
    column("first_name", VARCHAR(50)),
    column("last_name", VARCHAR(50)),
    column("email", VARCHAR(100))
  )
```

If you wish to implement the above, please consider the following implementation.

```scala 3
case class User(
  id:        Int,
  firstName: String, 
  lastName:  String,
  email:     String,
):
  
  val name: User.Name = User.Name(firstName, lastName)

object User:

  case class Name(firstName: String, lastName: String)

  val table = Table[User]("user")(
    column("id", INT, AUTO_INCREMENT),
    column("first_name", VARCHAR(50)),
    column("last_name", VARCHAR(50)),
    column("email", VARCHAR(100))
  )
```
