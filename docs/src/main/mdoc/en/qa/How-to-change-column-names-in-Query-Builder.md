{%
  laika.metadata.language = en
  laika.title = "Q: How to change column names in Query Builder?"
%}

# Q: How to change column names in Query Builder?

## A: In Query Builder, there are mainly two methods available for changing column names in model definitions.

### A: 1. Using annotations
You can specify column names used in queries by adding the `@Column` annotation to model fields.  
For example, if you want to treat the `name` field of the User model as `full_name`, define it as follows:

```scala 3
case class User(
  id: Int,
  @Column("full_name") name: String,
  email: String
) derives Table

val query = TableQuery[User].select(user => user.id *: user.name *: user.email)
// When generating the query, the name field is treated as "full_name"
println(query.statement)
// Output example: "SELECT `id`, `full_name`, `email` FROM user"
```

### A: 2. Using the alias feature of Query Builder
Another method is provided to specify an alias for columns during query construction without modifying the model definition.  
The example below shows how to change the column name during retrieval using the `alias` function or a custom mapping function:

```scala 3
import ldbc.dsl.codec.Codec
import ldbc.query.builder.*

case class User(id: Int, name: String, email: String) derives Table
object User:
  given Codec[User] = Codec.derived[User]

val userTable = TableQuery[User]

// Build a query and specify aliases in the select clause
val queryWithAlias = userTable
  .select(user => user.id *: user.name.as("full_name") *: user.email)
  
println(queryWithAlias.statement)
// Output example: "SELECT `id`, `name` AS `full_name`, email FROM user"
```

As shown above, you can change the format and display of column names in Query Builder by using annotations at model definition time or by specifying aliases during query construction.

## References
- [How to use Query Builder](/en/tutorial/Query-Builder.md)  
- [Schema Definition Details](/en/tutorial/Schema.md)
