{%
  laika.title = Schema
  laika.metadata.language = en
%}

# Schema

You've learned how to build type-safe queries with the [Query Builder](/en/tutorial/Query-Builder.md). This page explains how to define database schemas in Scala code and map tables to models.

Schema definitions are a critical element that clearly defines the boundary between your application and the database. ldbc provides functionality to define schemas in Scala code, leveraging the powerful type system to represent database structures.

This chapter explains how to handle database schemas in Scala code, particularly how to manually write schemas, which is useful when starting to write an application without an existing database. If you already have schemas in your database, you can skip this work by using the Code Generator.

## Preparation

You need to set up the following dependency in your project:

```scala
//> using dep "@ORGANIZATION@::ldbc-schema:@VERSION@"
```

The following code examples assume these imports:

```scala 3
import ldbc.schema.*
```

## Basic Table Definition

In ldbc, you create table definitions by extending the `Table` class. This allows you to associate Scala models (such as case classes) with database tables.

### Basic Table Definition

```scala 3
// Model definition
case class User(
  id:   Long,
  name:  String,
  age:   Option[Int] // Use Option for columns that allow NULL
)

// Table definition
class UserTable extends Table[User]("user"): // "user" is the table name
  // Column definitions
  def id: Column[Long] = column[Long]("id")
  def name: Column[String] = column[String]("name")
  def age: Column[Option[Int]] = column[Option[Int]]("age")

  // Mapping with the model
  override def * : Column[User] = (id *: name *: age).to[User]
```

In the example above:
- `Table[User]` indicates that this table is associated with the User model
- `"user"` is the table name in the database
- Each column is defined with the `column` method
- The `*` method defines how to map all columns to the model

### Table Definition with Data Types

You can specify MySQL data types and attributes for columns:

```scala 3
class UserTable extends Table[User]("user"):
  // Column definitions with data types and attributes
  def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
  def name: Column[String] = column[String]("name", VARCHAR(255))
  def age: Column[Option[Int]] = column[Option[Int]]("age", INT)

  override def * : Column[User] = (id *: name *: age).to[User]
```

## Using Dedicated Column Definition Methods

ldbc also provides specialized column definition methods for each data type. Because the variable name is used as the column name, you can write code more simply.

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def name: Column[String] = varchar(255)
  def age: Column[Option[Int]] = int().defaultNull

  override def * : Column[User] = (id *: name *: age).to[User]
```

Using dedicated column definition methods allows you to set attributes appropriate for that data type, enabling you to write more type-safe code.

### Explicitly Specifying Column Names

If you want to explicitly specify column names:

```scala 3
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint("user_id").autoIncrement.primaryKey
  def name: Column[String] = varchar("user_name", 255)
  def age: Column[Option[Int]] = int("user_age").defaultNull

  override def * : Column[User] = (id *: name *: age).to[User]
```

### Setting Column Naming Conventions

You can change the naming convention for columns using `Naming`:

```scala 3
class UserTable extends Table[User]("user"):
  // Convert column names to Pascal case (e.g., userId → UserId)
  given Naming = Naming.PASCAL
  
  def userId: Column[Long] = bigint().autoIncrement.primaryKey
  def userName: Column[String] = varchar(255)
  def userAge: Column[Option[Int]] = int().defaultNull

  override def * : Column[User] = (userId *: userName *: userAge).to[User]
```

Available naming conventions:
- `Naming.SNAKE` (default): Snake case (e.g., user_id)
- `Naming.CAMEL`: Camel case (e.g., userId)
- `Naming.PASCAL`: Pascal case (e.g., UserId)

## Numeric Column Definitions

For numeric columns, the following operations are possible:

### Integer Types

```scala 3
def id: Column[Long] = bigint().autoIncrement.primaryKey
def count: Column[Int] = int().unsigned.default(0) // Set as unsigned, default value 0
def smallValue: Column[Short] = smallint().unsigned
```

### Decimal Types

```scala 3
def price: Column[BigDecimal] = decimal(10, 2) // 10 digits in total, 2 decimal places
def rating: Column[Double] = double(5) // Double-precision floating point
def score: Column[Float] = float(4) // Single-precision floating point
```

## String Column Definitions

For string columns, the following operations are possible:

```scala 3
def name: Column[String] = varchar(255) // Variable-length string (max 255 characters)
def code: Column[String] = char(5) // Fixed-length string (5 characters)
def description: Column[String] = text() // Text type
def content: Column[String] = longtext() // Long text type

// Specifying character set
def japaneseText: Column[String] = text().charset(Character.utf8mb4)

// Specifying collation
def sortableText: Column[String] = varchar(255)
  .charset(Character.utf8mb4)
  .collate(Collate.utf8mb4_unicode_ci)
```

## Binary Column Definitions

Defining columns for binary data:

```scala 3
def data: Column[Array[Byte]] = binary(255) // Fixed-length binary
def flexData: Column[Array[Byte]] = varbinary(1000) // Variable-length binary
def largeData: Column[Array[Byte]] = blob() // Binary Large Object
```

## Date and Time Column Definitions

Defining columns for dates and times:

```scala 3
def birthDate: Column[LocalDate] = date() // Date only
def createdAt: Column[LocalDateTime] = datetime() // Date and time
def updatedAt: Column[LocalDateTime] = timestamp()
  .defaultCurrentTimestamp(onUpdate = true) // Auto-update on creation and modification
def startTime: Column[LocalTime] = time() // Time only
def fiscalYear: Column[Int] = year() // Year only
```

## ENUM Type and Special Data Types

Example of using ENUM type:

```scala 3
// ENUM definition
enum UserStatus:
  case Active, Inactive, Suspended

// Using ENUM in table definition
class UserTable extends Table[User]("user"):
  // ...
  def status: Column[UserStatus] = `enum`[UserStatus]("status")
```

Other special data types:

```scala 3
def isActive: Column[Boolean] = boolean() // BOOLEAN type
def uniqueId: Column[BigInt] = serial() // SERIAL type (auto-increment BIGINT UNSIGNED)
```

## Setting Default Values

How to set default values for columns:

```scala 3
def score: Column[Int] = int().default(100) // Fixed value
def updatedAt: Column[LocalDateTime] = timestamp()
  .defaultCurrentTimestamp() // Current timestamp
def createdDate: Column[LocalDate] = date()
  .defaultCurrentDate // Current date
def nullableField: Column[Option[String]] = varchar(255)
  .defaultNull // NULL value
```

## Primary Keys, Foreign Keys, and Indexes

### Single-Column Primary Key

```scala 3
def id: Column[Long] = bigint().autoIncrement.primaryKey
```

### Composite Primary Key Definition

```scala 3
class OrderItemTable extends Table[OrderItem]("order_item"):
  def orderId: Column[Int] = int()
  def itemId: Column[Int] = int()
  def quantity: Column[Int] = int().default(1)
  
  // Composite primary key definition
  override def keys = List(
    PRIMARY_KEY(orderId *: itemId)
  )

  override def * : Column[OrderItem] = (orderId *: itemId *: quantity).to[OrderItem]
```

### Index Definition

```scala 3
class UserTable extends Table[User]("user"):
  // ...column definitions...
  
  // Index definitions
  override def keys = List(
    INDEX_KEY("idx_user_name", name), // Named index
    UNIQUE_KEY("idx_user_email", email) // Unique index
  )
```

You can also specify index types:

```scala 3
override def keys = List(
  INDEX_KEY(
    Some("idx_name"), 
    Some(Index.Type.BTREE), // Can specify BTREE or HASH index type
    None, 
    name
  )
)
```

### Foreign Key Definition

To define foreign keys, first create a TableQuery for the referenced table:

```scala 3
// Referenced table
val userTable = TableQuery[UserTable]

// Referencing table
class ProfileTable extends Table[Profile]("profile"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def userId: Column[Long] = bigint()
  // ...other columns...
  
  // Foreign key definition
  def fkUser = FOREIGN_KEY(
    "fk_profile_user", // Foreign key name
    userId, // Referencing column
    REFERENCE(userTable)(_.id) // Referenced table and column
      .onDelete(Reference.ReferenceOption.CASCADE) // Action on delete
      .onUpdate(Reference.ReferenceOption.RESTRICT) // Action on update
  )
  
  override def keys = List(
    PRIMARY_KEY(id),
    fkUser // Add foreign key
  )
```

Reference constraint options (`ReferenceOption`):
- `RESTRICT`: Prevents changes to parent record as long as child records exist
- `CASCADE`: Changes child records along with parent record changes
- `SET_NULL`: Sets relevant columns in child records to NULL when parent record changes
- `NO_ACTION`: Delays constraint checking (basically the same as RESTRICT)
- `SET_DEFAULT`: Sets relevant columns in child records to default values when parent record changes

## Setting Constraints

If you want to define constraints with specific naming conventions, you can use `CONSTRAINT`:

```scala 3
override def keys = List(
  CONSTRAINT(
    "pk_user", // Constraint name
    PRIMARY_KEY(id) // Constraint type
  ),
  CONSTRAINT(
    "fk_user_department",
    FOREIGN_KEY(departmentId, REFERENCE(departmentTable)(_.id))
  )
)
```

## Complex Mapping with Models

### Mapping Nested Models

```scala 3
case class User(
  id: Long, 
  name: UserName, // Nested type
  contact: Contact // Nested type
)

case class UserName(first: String, last: String)
case class Contact(email: String, phone: Option[String])

class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def firstName: Column[String] = varchar(50)
  def lastName: Column[String] = varchar(50)
  def email: Column[String] = varchar(100)
  def phone: Column[Option[String]] = varchar(20).defaultNull
  
  // Nested value mapping
  def userName: Column[UserName] = (firstName *: lastName).to[UserName]
  def contact: Column[Contact] = (email *: phone).to[Contact]
  
  override def * : Column[User] = (id *: userName *: contact).to[User]
```

This configuration will generate the following SQL:

```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `firstName` VARCHAR(50) NOT NULL,
  `lastName` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(20) NULL,
  PRIMARY KEY (`id`)
)
```

## Schema Generation and DDL Execution

Generate DDL (Data Definition Language) from table definitions and create schemas in the database.

### Generating TableQuery

```scala 3
val users = TableQuery[UserTable]
val profiles = TableQuery[ProfileTable]
val orders = TableQuery[OrderTable]
```

### Generating and Executing Schema

```scala 3
import ldbc.dsl.*

// Combining schemas
val schema = users.schema ++ profiles.schema ++ orders.schema

// Apply schema using database connection
datasource.getConnection.use { conn =>
  DBIO.sequence(
    // Create tables (only if they don't exist)
    schema.createIfNotExists,
    // Other operations such as data insertion...
  ).commit(connector)
}
```

### DDL Operations

```scala 3
val userSchema = users.schema

// Various DDL operations
userSchema.create            // Create table
userSchema.createIfNotExists // Create table only if it doesn't exist
userSchema.drop              // Drop table
userSchema.dropIfExists      // Drop table only if it exists
userSchema.truncate          // Delete all data in the table
```

### Checking DDL Statements

How to check the actual SQL that will be executed:

```scala 3
// Check creation query
userSchema.create.statements.foreach(println)

// Check conditional creation query
userSchema.createIfNotExists.statements.foreach(println)

// Check drop query
userSchema.drop.statements.foreach(println)

// Check conditional drop query
userSchema.dropIfExists.statements.foreach(println)

// Check truncate query
userSchema.truncate.statements.foreach(println)
```

## Setting Column Attributes

Various attributes can be set for columns:

```scala 3
def id: Column[Long] = bigint()
  .autoIncrement    // Auto increment
  .primaryKey       // Primary key
  .comment("User ID") // Comment

def email: Column[String] = varchar(255)
  .unique           // Unique constraint
  .comment("Email address")

def status: Column[String] = varchar(20)
  .charset(Character.utf8mb4)  // Character set
  .collate(Collate.utf8mb4_unicode_ci)  // Collation

def hiddenField: Column[String] = varchar(100)
  .invisible        // Invisible attribute (not retrieved with SELECT *)

def formatField: Column[String] = varchar(100)
  .setAttributes(COLUMN_FORMAT.DYNAMIC[String]) // Column storage format

def storageField: Column[Array[Byte]] = blob()
  .setAttributes(STORAGE.DISK[Array[Byte]]) // Storage type
```

## Summary

Using the schema module of ldbc allows you to define safe and expressive database schemas by leveraging Scala's type system.

Key features:
- Strong type safety: Detect schema issues at compile time
- Rich data type support: Supports all MySQL data types
- Flexible model mapping: Handles everything from simple case classes to complex nested models
- DDL generation: Generate SQL directly from table definitions
- Extensibility: Supports custom data types and mapping functions

## Next Steps

Now you understand how to define schemas in Scala code. Manually defining schemas allows you to closely integrate your application and database structures.

Next, proceed to [Schema Code Generation](/en/tutorial/Schema-Code-Generation.md) to learn how to automatically generate schema code from existing SQL files.
