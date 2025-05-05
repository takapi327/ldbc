{%
  laika.title = "Q: How to change the format of column names using the schema?"
  laika.metadata.language = en
%}

# Q: How to change the format of column names using the schema?

## A: In schema definitions, the format of column names can be changed by modifying the implicit instance of Naming.
For example, the default is camel case, but using `Naming.PASCAL` converts all column names to Pascal case.
The sample code below demonstrates setting `given Naming = Naming.PASCAL` within a table definition, which is automatically applied to column names.

```scala 3
// Schema definition example (changing column name format)
case class User(id: Long, name: String, age: Option[Int])

class UserTable extends Table[User]("user"):
  // Set Naming to change column names to Pascal case
  given Naming = Naming.PASCAL
  
  def id: Column[Long] = bigint()         // Automatically becomes "Id"
  def name: Column[String] = varchar(255)  // Automatically becomes "Name"
  def age: Column[Option[Int]] = int().defaultNull // Automatically becomes "Age"
  
  override def * : Column[User] = (id *: name *: age).to[User]
  
// Usage example: The changed column names are applied in select statements
val userTable: TableQuery[UserTable] = TableQuery[UserTable]
val select = userTable.selectAll

println(select.statement)
// Output example: "SELECT `Id`, `Name`, `Age` FROM user"
```

This method allows you to change column names in bulk, enabling consistent schema definitions aligned with your project's naming conventions.

## References
- [Schema Definition Details](/en/tutorial/Schema.md)  
- [Custom Data Types](/en/tutorial/Custom-Data-Type.md)
