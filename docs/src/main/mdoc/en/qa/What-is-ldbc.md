{%
  laika.title = "Q: What is ldbc?"
  laika.metadata.language = en
%}

# Q: What is ldbc?

## A: ldbc is an OSS library that enables type-safe database access, query building, and schema definition.
ldbc leverages Scala's power to prevent errors at compile time while allowing intuitive database operations.

For example, let's start with how to build a schema. Here's an example of schema (table) definition:

```scala 3
// Schema definition example
case class User(id: Long, name: String, email: String)

class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def name: Column[String] = varchar(255)
  def email: Column[String] = varchar(255)
  
  override def * : Column[User] = (id *: name *: email).to[User]
```

In ldbc, after defining such a schema, you can abstract tables using TableQuery.

```scala
// UserTable abstraction using TableQuery
val userTable: TableQuery[UserTable] = TableQuery[UserTable]
// This allows you to use the schema with QueryBuilder API
```

Next, here's an example of how to use the query builder with the schema definition above. We'll show how to insert and retrieve data using TableQuery based on the schema.

### Query Builder Example Using Schema

```scala 3
// Using UserTable and TableQuery defined in the schema
val userTable: TableQuery[UserTable] = TableQuery[UserTable]

// Data insertion using schema
val schemaInsert: DBIO[Int] =
  (userTable += User(1, "Charlie", "charlie@example.com")).update

// Data retrieval using schema (mapping to User)
val schemaSelect = userTable.selectAll.query.to[List]

// Execution example
for
  _     <- schemaInsert.commit(conn)
  users <- schemaSelect.readOnly(conn)
yield users.foreach(println)
```

You can also perform data operations using plain queries directly. For example, here's how to insert and retrieve data using plain SQL:

```scala 3
// Data insertion using plain query
val plainInsert: DBIO[Int] =
  sql"INSERT INTO user (id, name, email) VALUES (2, 'Dave', 'dave@example.com')".update

// Data retrieval using plain query (mapping to User)
val plainSelect: DBIO[List[User]] =
  sql"SELECT id, name, email FROM user".query[User].to[List]

// Execution example
for
  _     <- plainInsert.commit(conn)
  users <- plainSelect.readOnly(conn)
yield users.foreach(println)
```

As shown above, ldbc is an attractive library that enables intuitive data operations from schema construction to query building using those schemas through its simple yet powerful API.

## References
- [How to Use Query Builder](/en/tutorial/Query-Builder.md)
- [Schema Definition Details](/en/tutorial/Schema.md)
