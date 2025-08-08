{%
  laika.title = "Q: How do I use nested models?"
  laika.metadata.language = en
%}

# Q: How do I use nested models?

## A: In ldbc, you can map multiple columns to nested models.
For example, here's how to give a User model a nested Name model, which maps to separate columns (e.g., first_name, last_name) in the database:

```scala 3
// Definition of nested model
case class User(id: Long, name: User.Name, email: String)
object User:
  case class Name(firstName: String, lastName: String)

// Table definition example
class UserTable extends Table[User]("user"):
  def id: Column[Long] = bigint().autoIncrement.primaryKey
  def firstName: Column[String] = varchar(255)
  def lastName: Column[String] = varchar(255)
  def email: Column[String] = varchar(255)
  
  // Convert (firstName *: lastName) to User.Name, and map id, name, and email to User
  override def * : Column[User] =
    (id *: (firstName *: lastName).to[User.Name] *: email).to[User]
  
// Usage example:
TableQuery[UserTable].selectAll.query.to[List].foreach(println)
// Records retrieved from UserTable's select are automatically converted to User.Name(firstName, lastName)
```

With this definition, the database columns `first_name` and `last_name` correspond to `firstName` and `lastName` in User.Name, allowing you to use them as a nested model.

## References
- [Schema Definition Details](/en/tutorial/Schema.md)
- [Custom Data Types](/en/tutorial/Custom-Data-Type.md)
