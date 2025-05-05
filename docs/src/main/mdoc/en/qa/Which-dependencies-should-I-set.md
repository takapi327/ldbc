{%
  laika.title = "Q: Which dependencies should I set?"
  laika.metadata.language = en
%}

# Q: Which dependencies should I set?

## A: To use ldbc, you need to set the following dependencies according to your needs.

- Plain DSL
- Query Builder
- Schema Definition and Model Mapping

**Connector**

To perform database connection processing using ldbc, you need to set one of the following dependencies.

**jdbc-connector**

If you want to use the traditional connector written in Java, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "jdbc-connector" % "@VERSION@",
  "com.mysql" % "mysql-connector-j" % "@MYSQL_VERSION@"
)
```

**ldbc-connector**

If you want to use the new connector written in Scala, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@"
)
```

ldbc-connector works not only on JVM but also on JS and Native platforms.

To use ldbc with Scala.js or Scala Native, set the dependencies as follows:

```scala 3
libraryDependencies ++= Seq(
  "com.example" %%% "ldbc-connector" % "@VERSION@"
)
```

### Plain DSL

To use the plain DSL, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-dsl" % "@VERSION@"
)
```

Plain DSL is a method to write simple SQL statements as they are. For example, you can execute queries using SQL literals directly.

```scala
import ldbc.dsl.*

val plainResult = sql"SELECT name FROM user"
  .query[String]
  .to[List]
  .readOnly(conn)
// plainResult is returned as List[String]
```

### Query Builder

To use the query builder, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-query-builder" % "@VERSION@"
)
```

Query builder is a method to construct queries with type-safe API. In the following example, we define a `User` model and build a SELECT statement using `TableQuery`.

```scala 3
import ldbc.dsl.codec.Codec
import ldbc.query.builder.*

case class User(id: Int, name: String, email: String) derives Table
object User:
  given Codec[User] = Codec.derived[User]

val userQuery = TableQuery[User]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")

// userQuery.statement is generated as "SELECT id, name, email FROM user WHERE email = ?"
```

### Schema Definition and Model Mapping

To use schema definition and model mapping, set the following dependencies:

```scala 3
libraryDependencies ++= Seq(
  "@ORGANIZATION@" %% "ldbc-schema" % "@VERSION@"
)
```

Schema definition and model mapping enable one-to-one mapping between table definitions and Scala models. Here's an example of defining a `User` table:

```scala 3
import ldbc.schema.*

case class User(id: Long, name: String, email: String)

class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def name: Column[String] = varchar(255)
  def email: Column[String] = varchar(255)
  
  override def * : Column[User] = (id *: name *: email).to[User]

val userQuery = TableQuery[UserTable]
  .select(user => user.id *: user.name *: user.email)
  .where(_.email === "alice@example.com")

// userQuery.statement is generated as "SELECT id, name, email FROM user WHERE email = ?"
```

## References
- [How to use Query Builder](/en/tutorial/Query-Builder.md)
- [Schema Definition Details](/en/tutorial/Schema.md)
- [Plain DSL Usage Examples](/en/tutorial/Selecting-Data.md)
- [Database Connection](/en/tutorial/Connection.md)
- [Parameterized Queries](/en/tutorial/Parameterized-Queries.md)
