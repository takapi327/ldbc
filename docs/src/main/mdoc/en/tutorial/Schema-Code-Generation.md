{%
  laika.title = Schema Code Generation
  laika.metadata.language = en
%}

# Schema Code Generation

In [Schema](/en/tutorial/Schema.md), we learned how to define schemas using Scala code. However, when working with existing databases, manually defining schemas can be time-consuming and prone to errors. This page explains how to automatically generate Scala code from existing SQL files.

Code generation is a powerful tool for automating repetitive tasks and reducing human errors. ldbc provides functionality to automatically generate model classes and table definitions from SQL files.

## SBT Plugin Setup

### Adding the Plugin

You need to set up the following dependency in your project. Add it to `project/plugins.sbt`.

```scala
addSbtPlugin("@ORGANIZATION@" % "ldbc-plugin" % "@VERSION@")
```

### Enabling the Plugin

Enable the plugin for your project in the `build.sbt` file.

```sbt
lazy val root = (project in file("."))
  .enablePlugins(Ldbc)
```

## Basic Usage

### Specifying SQL Files

Configure the SQL files to be parsed. You can specify a single file or multiple files.

```sbt
// Specifying a single SQL file
Compile / parseFiles := List(
  baseDirectory.value / "sql" / "schema.sql"
)

// Specifying multiple SQL files
Compile / parseFiles := List(
  baseDirectory.value / "sql" / "users.sql",
  baseDirectory.value / "sql" / "products.sql"
)
```

### Specifying Directories

To target all SQL files in a specific directory, use `parseDirectories`.

```sbt
// Specify by directory
Compile / parseDirectories := List(
  baseDirectory.value / "sql"
)
```

### Generated Code

After configuration, code will be automatically generated when you compile with sbt.

```shell
sbt compile
```

The generated files are stored in the `target/scala-X.X/src_managed/main` directory.

### Manual Generation

If you want to force code generation without using cache, use the following command:

```shell
sbt generateBySchema
```

## SQL File Format Requirements

SQL files must include the following elements:

### Database Definition

At the beginning of the file, always include a Create or Use statement for the database. This determines the package name and table ownership in the generated code.

```sql
-- Method 1: Creating a database
CREATE DATABASE `my_app`;

-- Or Method 2: Using an existing database
USE `my_app`;
```

### Table Definitions

After the database definition, include table definitions.

```sql
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Complete SQL file example:

```sql
CREATE DATABASE `my_app`;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Detailed Configuration Options

The ldbc plugin offers the following configuration keys to customize code generation.

### Configuration Key List

| Key                   | Default Value    | Details                                                |
|----------------------|-----------------|--------------------------------------------------------|
| `parseFiles`         | `List.empty`    | List of SQL files to parse                             |
| `parseDirectories`   | `List.empty`    | Directory-based specification of SQL files to parse    |
| `excludeFiles`       | `List.empty`    | List of filenames to exclude from parsing              |
| `customYamlFiles`    | `List.empty`    | List of YAML files for type customization              |
| `classNameFormat`    | `Format.PASCAL` | Format for generated class names (PASCAL, CAMEL, SNAKE)|
| `propertyNameFormat` | `Format.CAMEL`  | Format for generated property names (PASCAL, CAMEL, SNAKE)|
| `ldbcPackage`        | `ldbc.generated`| Package name for generated files                        |

### Example: Detailed Configuration

```sbt
Compile / parseFiles := List(
  baseDirectory.value / "sql" / "schema.sql"
)

Compile / parseDirectories := List(
  baseDirectory.value / "sql" / "tables"
)

Compile / excludeFiles := List(
  "temp_tables.sql", "test_data.sql"
)

Compile / classNameFormat := PASCAL // PascalCase (MyClass)
Compile / propertyNameFormat := CAMEL // camelCase (myProperty)

Compile / ldbcPackage := "com.example.db"
```

## Example of Generated Code

For example, with an SQL file like:

```sql
CREATE DATABASE `shop`;

CREATE TABLE `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `description` TEXT,
  `category_id` INT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

The following Scala code would be generated:

```scala
package com.example.db

import ldbc.schema.*

import java.time.LocalDateTime

// Model class
case class Product(
  id: Long,
  name: String,
  price: BigDecimal,
  description: Option[String],
  categoryId: Int,
  createdAt: LocalDateTime
)

// Table definition and query builder
object Product {
  val table = TableQuery[ProductTable]
  
  class ProductTable extends Table[Product]("products"):
    def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
    def name: Column[String] = column[String]("name", VARCHAR(255), NOT_NULL)
    def price: Column[BigDecimal] = column[BigDecimal]("price", DECIMAL(10, 2), NOT_NULL)
    def description: Column[Option[String]] = column[Option[String]]("description", TEXT)
    def categoryId: Column[Int] = column[Int]("category_id", INT, NOT_NULL)
    def createdAt: Column[LocalDateTime] = column[LocalDateTime]("created_at", TIMESTAMP, NOT_NULL, DEFAULT_CURRENT_TIMESTAMP)
    
    override def * : Column[Product] = (id *: name *: price *: description *: categoryId *: createdAt).to[Product]
}
```

## Customizing Types

If you want to change the types in the automatically generated code to your own types, you can customize them using YAML files.

### YAML File Configuration

First, create a YAML file for customization.

```yaml
# custom_types.yml
database:
  name: 'shop'
  tables:
    - name: 'products'
      columns:
        - name: 'category_id'
          type: 'ProductCategory'
      object:
        extends:
          - 'com.example.ProductTypeMapping'
```

Then, add this YAML file to your project configuration.

```sbt
Compile / customYamlFiles := List(
  baseDirectory.value / "config" / "custom_types.yml"
)
```

### Custom Type Implementation

Next, implement the custom type conversion referenced in the YAML file.

```scala
// com/example/ProductTypeMapping.scala
package com.example

import ldbc.dsl.Codec

sealed trait ProductCategory {
  def id: Int
}

object ProductCategory {
  case object Electronics extends ProductCategory { val id = 1 }
  case object Books extends ProductCategory { val id = 2 }
  case object Clothing extends ProductCategory { val id = 3 }
  
  def fromId(id: Int): ProductCategory = id match {
    case 1 => Electronics
    case 2 => Books
    case 3 => Clothing
    case _ => throw new IllegalArgumentException(s"Unknown category ID: $id")
  }
}

trait ProductTypeMapping {
  given Codec[ProductCategory] = Codec[Int].imap(ProductCategory.fromId)(_.id)
}
```

### Generated Code After Customization

With the above configuration, code like the following would be generated:

```scala
package ldbc.generated.shop

import ldbc.schema.*
import java.time.LocalDateTime
import com.example.ProductCategory

case class Product(
  id: Long,
  name: String,
  price: BigDecimal,
  description: Option[String],
  categoryId: ProductCategory, // Changed to custom type
  createdAt: LocalDateTime
)

object Product extends com.example.ProductTypeMapping {
  val table = TableQuery[ProductTable]
  
  class ProductTable extends Table[Product]("products"):
    def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
    def name: Column[String] = column[String]("name", VARCHAR(255), NOT_NULL)
    def price: Column[BigDecimal] = column[BigDecimal]("price", DECIMAL(10, 2), NOT_NULL)
    def description: Column[Option[String]] = column[Option[String]]("description", TEXT)
    def categoryId: Column[Int] = column[Int]("category_id", INT, NOT_NULL) // Actual column type doesn't change
    def createdAt: Column[LocalDateTime] = column[LocalDateTime]("created_at", TIMESTAMP, NOT_NULL, DEFAULT_CURRENT_TIMESTAMP)
    
    override def * : Column[Product] = (id *: name *: price *: description *: categoryId *: createdAt).to[Product]
}
```

## Detailed YAML Customization Syntax

In the customization YAML file, the following configurations are possible:

```yaml
database:
  name: '{database_name}'
  tables:
    - name: '{table_name}'
      columns: # Optional
        - name: '{column_name}'
          type: '{desired_Scala_type}'
      class: # Optional
        extends:
          - '{package_path_of_trait_to_extend_the_model_class}'
      object: # Optional
        extends:
          - '{package_path_of_trait_to_extend_the_object}'
```

### Example: Adding Traits to a Model Class

```yaml
database:
  name: 'shop'
  tables:
    - name: 'products'
      class:
        extends:
          - 'com.example.JsonSerializable'
          - 'com.example.Validatable'
```

### Example: Customizing Multiple Tables and Columns

```yaml
database:
  name: 'shop'
  tables:
    - name: 'products'
      columns:
        - name: 'price'
          type: 'Money'
      object:
        extends:
          - 'com.example.MoneyTypeMapping'
    - name: 'orders'
      columns:
        - name: 'status'
          type: 'OrderStatus'
      object:
        extends:
          - 'com.example.OrderStatusMapping'
```

## Using Generated Code

Generated code can be used like any other ldbc code.

```scala
import ldbc.dsl.*
import ldbc.generated.shop.Product

val provider = MySQLConnectionProvider(...)

// Referencing table queries
val products = Product.table

// Executing queries
val allProducts = provider.use { conn =>
  products.filter(_.price > 100).all.run(conn)
}
```

## Best Practices for Code Generation

### 1. Clear SQL File Structure

- Group related tables in the same file
- Always include database definition at the beginning of each file
- Add appropriate comments to explain SQL

### 2. Consistent Naming Conventions

- Use consistent naming conventions for tables and columns in SQL
- Explicitly configure naming rules for the generated Scala code

### 3. Smart Use of Custom Types

- Use custom types for domain-specific concepts
- Leverage custom types to encapsulate complex business logic

### 4. Automate Regeneration

Consider integrating into CI/CD pipelines for regular schema updates.

## Troubleshooting

### When Code Is Not Generated

- Verify SQL file paths are correct
- Ensure database definition is at the beginning of the SQL files
- Check for SQL syntax errors

### When Type Conversion Errors Occur

- Verify custom YAML configurations are correct
- Ensure referenced packages and classes exist in the classpath
- Check that implicit type conversions (given/using) are properly defined

### When Generated Code Has Issues

- Don't manually modify; instead, fix the SQL or YAML files and regenerate
- Check for unsupported SQL features or special types

## Tutorial Completion

Congratulations! You've completed all sections of the ldbc tutorial. Now you have the basic skills and knowledge to develop database applications using ldbc.

Throughout this journey, you've learned:
- Basic usage and setup of ldbc
- Database connections and query execution
- Reading and writing data with type-safe mapping
- Transaction management and error handling
- Advanced features (logging, custom data types, query builders)
- Schema definition and code generation

Use this knowledge to build type-safe and efficient database applications. For more information and updates, refer to the official documentation and GitHub repository.

Happy coding with ldbc!
