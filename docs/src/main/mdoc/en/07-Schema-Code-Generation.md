{%
laika.title = Schema Code Generation
laika.metadata.language = en
%}

# Schema Code Generation

This chapter describes how to automatically generate LDBC table definitions from SQL files.

The following dependencies must be set up for the project

```scala 3
addSbtPlugin("@ORGANIZATION@" % "ldbc-plugin" % "@VERSION@")
```

## Generation

Enable the plugin for the project.

```sbt
lazy val root = (project in file("."))
  .enablePlugins(Ldbc)
```

Specify the SQL file to be analyzed as an array.

```sbt
Compile / parseFiles := List(baseDirectory.value / "test.sql")
```

**List of keys that can be set by enabling the plugin**

| Key                  | Details                                                                |
|----------------------|------------------------------------------------------------------------|
| `parseFiles`         | `List of SQL files to be analyzed`                                     |
| `parseDirectories`   | `Specify SQL files to be parsed by directory`                          |
| `excludeFiles`       | `List of file names to exclude from analysis`                          |
| `customYamlFiles`    | `List of yaml files for customizing Scala types and column data types` |
| `classNameFormat`    | `Value specifying the format of the class name`                        |
| `propertyNameFormat` | `Value specifying the format of the property name in the Scala model`  |
| `ldbcPackage`        | `Value specifying the package name of the generated file`              |

The SQL file to be parsed must always begin with a database Create or Use statement, and LDBC parses the file one file at a time, generating table definitions and storing the list of tables in the database model.
This is because it is necessary to tell which database the table belongs to.

```mysql
CREATE DATABASE `location`;

USE `location`;

DROP TABLE IF EXISTS `country`;
CREATE TABLE country (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `code` INT NOT NULL
);
```

The SQL file to be analyzed should contain only Create/Use statements for the database or Create/Drop statements for table definitions.

## Generation Code

When the sbt project is started and compiled, model classes generated based on the SQL file to be analyzed and table definitions are generated under the target of the sbt project.

```shell
sbt compile
```

The code generated from the above SQL file will look like this.

```scala 3
package ldbc.generated.location

import ldbc.core.*

case class Country(
  id: Long,
  name: String,
  code: Int
)

object Country:
  val table = Table[Country]("country")(
    column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
    column("name", VARCHAR(255)),
    column("code", INT)
  )
```

If the SQL file has been modified or the cache has been removed by running the clean command, Compile will generate the code again. If the SQL file has been modified or the cache has been removed by executing the clean command, the code will be generated again by executing Compile.
If you want to generate code again without using the cache, execute the command `generateBySchema`. This command will always generate code without using the cache.

```shell
sbt generateBySchema
```

## Customize

There may be times when you want to convert the type of code generated from an SQL file to something else. This can be done by passing `customYamlFiles` with the yml files to be customized.

```sbt
Compile / customYamlFiles := List(
  baseDirectory.value / "custom.yml"
)
```

The format of the yml file should be as follows

```yaml
database:
  name: '{Database Name}'
  tables:
    - name: '{table name}'
      columns: # Optional
        - name: '{column name}'
          type: '{Scala type you want to change}'
      class: # Optional
        extends:
          - '{Package paths such as trait that you want model classes to inherit}' // package.trait.name
      object: # Optional
        extends:
          - '{The package path, such as trait, that you want the object to inherit.}'
    - name: '{table name}'
      ...
```

The `database` must be the name of the database listed in the SQL file to be analyzed. The table name must be the name of a table belonging to the database listed in the SQL file to be analyzed.

In the `columns` field, enter the name of the column to be retyped and the Scala type to be changed as a string. You can set multiple values for `columns`, but the column name listed in name must be in the target table.
Also, the Scala type to be converted must be one that is supported by the column's Data type. If you want to specify an unsupported type, you must pass a trait, abstract class, etc. that is configured to do implicit type conversion for `object`.

See [here](/en/01-Table-Definitions.md) for types supported by the Data type and [here](/en/02-Custom-Data-Type.md).

To convert an Int type to the user's own type, CountryCode, implement the following `CustomMapping`trait.

```scala 3
trait CountryCode:
  val code: Int
object Japan extends CountryCode:
  override val code: Int = 1

trait CustomMapping: // Any name
  given Conversion[INT[Int], CountryCode] = DataType.mappingp[INT[Int], CountryCode]
```

Set the `CustomMapping`trait that you have implemented in the yml file for customization, and convert the target column type to CountryCode.

```yaml
database:
  name: 'location'
  tables:
    - name: 'country'
      columns:
        - name: 'code'
          type: 'Country.CountryCode' // CustomMapping is mixed in with the Country object so that it can be retrieved from there.
      object:
        extends:
          - '{package.name.}CustomMapping'
```

The code generated by the above configuration will be as follows, allowing users to generate model and table definitions with their own types.

```scala 3
case class Country(
  id: Long,
  name: String,
  code: Country.CountryCode
)

object Country extends /*{package.name.}*/CustomMapping:
  val table = Table[Country]("country")(
    column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
    column("name", VARCHAR(255)),
    column("code", INT)
  )
```

The database model is also automatically generated from SQL files.

```scala 3
package ldbc.generated.location

import ldbc.core.*

case class LocationDatabase(
  schemaMeta: Option[String] = None,
  catalog: Option[String] = Some("def"),
  host: String = "127.0.0.1",
  port: Int = 3306
) extends Database:

  override val databaseType: Database.Type = Database.Type.MySQL

  override val name: String = "location"

  override val schema: String = "location"

  override val character: Option[Character] = None

  override val collate: Option[Collate] = None

  override val tables = Set(
    Country.table
  )
```
