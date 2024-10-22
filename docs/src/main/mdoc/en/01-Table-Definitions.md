{%
laika.title = Table Definitions
laika.metadata.language = en
%}

# Table Definitions

This chapter describes how to work with database schemas in Scala code, especially how to manually write a schema, which is useful when starting to write an application without an existing database. If you already have a schema in your database, you can skip this step using the [code generator](/en/07-Schema-Code-Generation.md).

The following code example assumes the following import

```scala 3
import ldbc.core.*
import ldbc.core.attribute.*
```

LDBC maintains a one-to-one mapping between Scala models and database table definitions. The mapping between the properties held by the model and the columns held by the table is done in the order of definition. Table definitions are very similar to the structure of a Create statement. This makes the construction of table definitions intuitive for the user.

LDBC uses this table definition for a variety of purposes Generating type-safe queries, generating documents, etc.

```scala 3
case class User(
  id: Long,
  name: String,
  age: Option[Int],
)

val table = Table[User]("user")(                     // CREATE TABLE `user` (
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY), //   `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  column("name", VARCHAR(255)),                      //   `name` VARCHAR(255) NOT NULL,
  column("age", INT.UNSIGNED.DEFAULT(None)),         //   `age` INT unsigned DEFAULT NULL
)                                                    // );
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

The mapping of the Scala type of a property that a model has to the data type that a column has requires that the defined data type supports the Scala type. Attempting to assign an unsupported type will result in a compile error.

The following table shows the Scala types supported by the data types.

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

**Points to keep in mind when dealing with integer types**

It should be noted that the range of data that can be handled, depending on whether it is signed or unsigned, does not fit within the Scala types.

| Data Type   | Signed Range                                 | Unsigned Range             | Scala Type       | Range                                                                |
|-------------|----------------------------------------------|----------------------------|------------------|----------------------------------------------------------------------|
| `TINYINT`   | `-128 ~ 127`                                 | `0 ~ 255`                  | `Byte<br>Short`  | `-128 ~ 127<br>-32768～32767`                                         |
| `SMALLINT`  | `-32768 ~ 32767`                             | `0 ~ 65535`                | `Short<br>Int`   | `-32768～32767<br>-2147483648～2147483647`                             |
| `MEDIUMINT` | `-8388608 ~ 8388607`                         | `0 ~ 16777215`             | `Int`            | `-2147483648～2147483647`                                             |
| `INT`       | `-2147483648	~ 2147483647`                   | `0 ~ 4294967295`           | `Int<br>Long`    | `-2147483648～2147483647<br>-9223372036854775808～9223372036854775807` |
| `BIGINT`    | `-9223372036854775808 ~ 9223372036854775807` | `0 ~ 18446744073709551615` | `Long<br>BigInt` | `-9223372036854775808～9223372036854775807<br>...`                    |

To work with user-defined proprietary or unsupported types, see [Custom Types](/en/02-Custom-Data-Type.md).

## Attribute

Various attributes can be assigned to columns.

- `AUTO_INCREMENT`
  Mark columns as auto-increment keys when creating DDL statements and documenting SchemaSPY.
  MySQL cannot return columns that are not AutoInc when inserting data. Therefore, if necessary, LDBC will check to see if the return column is properly marked as AutoInc.
- `PRIMARY_KEY`
  Mark columns as primary keys when creating DDL statements and SchemaSPY documents.
- `UNIQUE_KEY`
  Mark columns as unique keys when creating DDL statements and SchemaSPY documents.
- `COMMENT`
  Set comments on columns when creating DDL statements and SchemaSPY documents.

## Key Settings

MySQL allows you to set various keys for tables, such as Unique keys, Index keys, foreign keys, etc. Let's look at how to set these keys in a table definition built with LDBC.

### PRIMARY KEY

A primary key is an item that uniquely identifies data in MySQL. When a primary key constraint is set on a column, the column can only contain values that do not duplicate the values of other data. It also cannot contain NULLs. As a result, only one piece of data in the table can be identified by searching for a value in a column with a primary key constraint.

LDBC allows this primary key constraint to be set in two ways.

1. set as an attribute of column method
2. set by keySet method of table

**Set as an attribute of the column method**

It is very easy to set a column method as an attribute by simply passing `PRIMARY_KEY` as the third or later argument of the column method. This allows you to set the `id` column as the primary key in the following cases

```scala 3
column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
```

**Set by keySet method of table**

LDBC table definitions have a method called `keySet`, where you can set a column as a primary key by passing `PRIMARY_KEY` as the column to be set as the primary key.

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => PRIMARY_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   PRIMARY KEY (`id`)
// )
```

The `PRIMARY_KEY` method accepts the following parameters in addition to columns

- `Index Type` ldbc.core.Index.Type.BTREE or ldbc.core.Index.Type.HASH
- `Index Option` ldbc.core.Index.IndexOption

#### Compound key (primary key)

Not only one column, but also multiple columns can be set as a combined primary key. You can set multiple columns as primary keys by simply passing multiple columns to `PRIMARY_KEY` as primary keys.

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => PRIMARY_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   PRIMARY KEY (`id`, `name`)
// )
```

Compound keys can only be set with `PRIMARY_KEY` in the `keySet` method. If multiple keys are set as attributes of the column method as shown below, each will be set as a primary key, not as a compound key.

LDBC does not allow multiple `PRIMARY_KEY`s in a table definition to cause a compile error. However, if the table definition is used in query generation, document generation, etc., an error will occur. This is due to the restriction that only one PRIMARY KEY can be set per table.

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255), PRIMARY_KEY),
  column("age", INT.UNSIGNED.DEFAULT(None))
)

// CREATE TABLE `user` (
//   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
// )
```

### UNIQUE KEY

A unique key is an item that uniquely identifies data in MySQL. When a column has a uniqueness constraint, it can only contain values that do not duplicate the values of other data.

LDBC allows this uniqueness constraint to be set in two ways.

1. set as an attribute of column method
2. set by keySet method of table

**Set as an attribute of the column method**

It is very easy to set a column method as an attribute by simply passing `UNIQUE_KEY` as the third or later argument of the column method. This allows you to set the `id` column as a unique key in the following cases

```scala 3
column("id", BIGINT, AUTO_INCREMENT, UNIQUE_KEY)
```

**Set by keySet method of table**

LDBC table definitions have a method called `keySet`, where you can set a column as a unique key by passing the column you want to set as a unique key to `UNIQUE_KEY`.

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => UNIQUE_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   UNIQUE KEY (`id`)
// )
```

The `UNIQUE_KEY` method accepts the following parameters in addition to columns

- `Index Name` String
- `Index Type` ldbc.core.Index.Type.BTREE or ldbc.core.Index.Type.HASH
- `Index Option` ldbc.core.Index.IndexOption

#### Compound key (unique key)

You can set not only one column but also multiple columns as a unique key as a combined unique key. You can set multiple columns as unique keys by simply passing `UNIQUE_KEY` with the columns you want to set as unique keys.

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => UNIQUE_KEY(table.id, table.name))

// CREATE TABLE `user` (
//   ...,
//   UNIQUE KEY (`id`, `name`)
// )
```

Compound keys can only be set with `UNIQUE_KEY` in the `keySet` method. If you set multiple keys as attributes in the column method, each will be set as a unique key, not as a compound key.

### INDEX KEY

An index key is an "index" in MySQL to efficiently retrieve the desired record.

LDBC allows this index to be set in two ways.

1. set as an attribute of column method
2. set by keySet method of table

**Set as an attribute of the column method**

It is very easy to set a column method as an attribute, just pass `INDEX_KEY` as the third argument or later of the column method. This allows you to set the `id` column as an index in the following cases

```scala 3
column("id", BIGINT, AUTO_INCREMENT, INDEX_KEY)
```

**Set by keySet method of table**

LDBC table definitions have a method called `keySet`, where you can set a column as an index key by passing the column you want to set as an index to `INDEX_KEY`.

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
)
  .keySet(table => INDEX_KEY(table.id))

// CREATE TABLE `user` (
//   ...,
//   INDEX KEY (`id`)
// )
```

The `INDEX_KEY` method accepts the following parameters in addition to columns

- `Index Name` String
- `Index Type` ldbc.core.Index.Type.BTREE or ldbc.core.Index.Type.HASH
- `Index Option` ldbc.core.Index.IndexOption

#### Compound key (index key)

You can set not only one column but also multiple columns as index keys as a combined index key. You can set up a composite index by simply passing multiple columns as index keys to `INDEX_KEY`.

```scala 3
val table = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None))
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

In LDBC, this foreign key constraint can be set by using the keySet method of table.

```scala 3
val post = Table[Post]("post")(
  column("id", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255))
)

val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long])
)
  .keySet(table => FOREIGN_KEY(table.postId, REFERENCE(post, post.id)))

// CREATE TABLE `user` (
//   ...,
//   FOREIGN KEY (`post_id`)  REFERENCES `post` (`id`)
// )
```

The `FOREIGN_KEY` method accepts the following parameters in addition to column and reference values.

- `Index Name` String

Foreign key constraints can be used to set the behavior of the parent table on delete and update. The `REFERENCE` method provides the `onDelete` and `onUpdate` methods, which can be used to set the respective behavior.

Values that can be set can be obtained from `ldbc.core.Reference.ReferenceOption`.

```scala 3
val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long])
)
  .keySet(table => FOREIGN_KEY(table.postId, REFERENCE(post, post.id).onDelete(Reference.ReferenceOption.RESTRICT)))

// CREATE TABLE `user` (
//   ...,
//   FOREIGN KEY (`post_id`)  REFERENCES `post` (`id`) ON DELETE RESTRICT
// )
```

The values that can be set are as follows

- `RESTRICT`: Deny delete or update operations on parent tables.
- `CASCADE`: Deletes or updates rows from the parent table and automatically deletes or updates matching rows in the child tables.
- `SET_NULL`: Deletes or updates rows from the parent table and sets foreign key columns in the child table to NULL.
- `NO_ACTION`: Standard SQL keywords. In MySQL, equivalent to RESTRICT.
- `SET_DEFAULT`: This action is recognized by the MySQL parser, but both InnoDB and NDB will reject table definitions containing an ON DELETE SET DEFAULT or ON UPDATE SET DEFAULT clause.

#### Compound key (foreign key)

Not only one column, but also multiple columns can be combined as a foreign key. Simply pass multiple columns to `FOREIGN_KEY` to be set as foreign keys as a compound foreign key.

```scala 3
val post = Table[Post]("post")(
  column("id", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255)),
  column("category", SMALLINT[Short])
)

val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long]),
  column("post_category", SMALLINT[Short])
)
  .keySet(table => FOREIGN_KEY((table.postId, table.postCategory), REFERENCE(post, (post.id, post.category))))

// CREATE TABLE `user` (
//   ...,
//   FOREIGN KEY (`post_id`, `post_category`)  REFERENCES `post` (`id`, `category`)
// )
```

### Constraint Name

MySQL allows you to give arbitrary names to constraints by using CONSTRAINT. The constraint name must be unique on a per-database basis.

LDBC provides the CONSTRAINT method, so the process of setting constraints such as key constraints can be set by simply passing the process to the CONSTRAINT method.

```scala 3
val user = Table[User]("user")(
  column("id", BIGINT[Long], AUTO_INCREMENT),
  column("name", VARCHAR(255)),
  column("age", INT.UNSIGNED.DEFAULT(None)),
  column("post_id", BIGINT[Long])
)
  .keySet(table => CONSTRAINT("fk_post_id", FOREIGN_KEY(table.postId, REFERENCE(post, post.id))))

// CREATE TABLE `user` (
//   ...,
//   CONSTRAINT `fk_post_id` FOREIGN KEY (`post_id`)  REFERENCES `post` (`id`)
// )
```
