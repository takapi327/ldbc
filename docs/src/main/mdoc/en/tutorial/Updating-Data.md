{%
  laika.title = Updating Data
  laika.metadata.language = en
%}

# Updating Data

Now that we've learned how to retrieve data in [Selecting Data](/en/tutorial/Selecting-Data.md), let's look at how to write data to the database. This page explains the basics of Data Manipulation Language (DML) such as INSERT, UPDATE, and DELETE.

## Basics of Data Updating

Write operations to the database behave somewhat differently from read operations because they change the state of the database. ldbc provides appropriate abstractions to safely perform these operations.

The basic flow for performing write operations is as follows:

1. Create an SQL query using the `sql` interpolator
2. Call the appropriate method (`.update`, `.returning`, etc.) to specify the type of query
3. Execute the query with `.commit()` or `.transaction()`
4. Process the results

## Inserting Data (INSERT)

### Basic INSERT Operation

To insert data, use the SQL `INSERT` statement and call ldbc's `.update` method. Here's an example of inserting a row into the `user` table:

```scala
// Method to insert a new user into the user table
def insertUser(name: String, email: String): DBIO[Int] =
  sql"INSERT INTO user (name, email) VALUES ($name, $email)"
    .update
```

Let's use this method to actually insert data and check the result:

```scala
// Execute the insert operation
insertUser("dave", "dave@example.com").commit(conn).unsafeRunSync()
// The return value is the number of affected rows (in this case, 1)

// Check the inserted data
sql"SELECT id, name, email FROM user WHERE name = 'dave'"
  .query[(Int, String, String)]
  .to[Option]
  .readOnly(conn)
  .unsafeRunSync()
  .foreach { case (id, name, email) =>
    println(s"ID: $id, Name: $name, Email: $email")
  }
```

The `.update` method returns the number of affected rows (in this case, 1).

### Retrieving Auto-generated Keys

In many cases, tables have auto-generated keys such as auto-increment IDs. To retrieve this auto-generated key value at the time of insertion, use the `.returning[T]` method:

```scala
// Method to insert and get the generated ID
def insertUserAndGetId(name: String, email: String): DBIO[Long] =
  sql"INSERT INTO user (name, email) VALUES ($name, $email)"
    .returning[Long]
```

Let's use this method to insert a new user and get the auto-generated ID:

```scala
// Insert and get the auto-generated ID
val newUserId = insertUserAndGetId("frank", "frank@example.com")
  .commit(conn)
  .unsafeRunSync()

println(s"New user ID: $newUserId")
```

**Note**: The `.returning` method only works with columns that have `AUTO_INCREMENT` set in MySQL.

### Retrieving Inserted Data

If you want to retrieve all the information about the inserted data at the same time, you can combine two steps using the auto-generated key:

```scala
// Case class representing a user
case class User(id: Long, name: String, email: String)

// Method to insert a user and return the inserted user's information
def insertAndRetrieveUser(name: String, email: String): DBIO[Option[User]] =
  for
    id   <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".returning[Long]
    user <- sql"SELECT id, name, email FROM user WHERE id = $id".query[User].to[Option]
  yield user
```

Usage example:

```scala
// Insert a user and get the inserted user's information
insertAndRetrieveUser("grace", "grace@example.com")
  .commit(conn)
  .unsafeRunSync()
  .foreach { user =>
    println(s"Inserted user: ID=${user.id}, Name=${user.name}, Email=${user.email}")
  }
```

## Updating Data (UPDATE)

To update data, use the SQL `UPDATE` statement and call the `.update` method similarly:

```scala
// Method to update a user's email address
def updateUserEmail(id: Long, newEmail: String): DBIO[Int] =
  sql"UPDATE user SET email = $newEmail WHERE id = $id"
    .update
```

Usage example:

```scala
// Update a user's email address
updateUserEmail(1, "alice+updated@example.com")
  .commit(conn)
  .unsafeRunSync()

// Check the updated data
sql"SELECT id, name, email FROM user WHERE id = 1"
  .query[User]
  .to[Option]
  .readOnly(conn)
  .unsafeRunSync()
  .foreach { user => 
    println(s"Updated user: ID=${user.id}, Name=${user.name}, Email=${user.email}")
  }
```

### Updating with Multiple Conditions

It's also possible to update with complex conditions:

```scala
// Update email addresses for users matching a specific name
def updateEmailsByName(name: String, newEmail: String): DBIO[Int] =
  sql"""
    UPDATE user 
    SET email = $newEmail 
    WHERE name LIKE ${"%" + name + "%"}
  """.update
```

This example updates the email address of all users who match the specified name pattern.

## Deleting Data (DELETE)

To delete data, use the SQL `DELETE` statement and call the `.update` method:

```scala
// Delete a user by ID
def deleteUser(id: Long): DBIO[Int] =
  sql"DELETE FROM user WHERE id = $id"
    .update
```

Usage example:

```scala
// Delete a user
deleteUser(5)
  .commit(conn)
  .unsafeRunSync()

// Confirm deletion
sql"SELECT COUNT(*) FROM user WHERE id = 5"
  .query[Int]
  .unsafe
  .readOnly(conn)
  .unsafeRunSync() match {
    case 0 => println("User with ID 5 has been deleted")
    case n => println(s"User with ID 5 still exists (count: $n)")
  }
```

### Deleting Multiple Rows

It's also possible to delete multiple rows at once that match a condition:

```scala
// Delete all users with email addresses from a specific domain
def deleteUsersByEmailDomain(domain: String): DBIO[Int] =
  sql"DELETE FROM user WHERE email LIKE ${"%@" + domain}"
    .update
```

## Batch Processing (Bulk Operations)

### Bulk Insertion of Multiple Rows

To efficiently insert many rows, you can specify multiple value sets in the `VALUES` clause:

```scala
import cats.data.NonEmptyList

// Bulk insert multiple users
def insertManyUsers(users: NonEmptyList[(String, String)]): DBIO[Int] =
  val values = users.map { case (name, email) => sql"($name, $email)" }
  (sql"INSERT INTO user (name, email) VALUES " ++ Fragments.values(values)).update
```

Usage example:

```scala
// Define multiple users
val newUsers = NonEmptyList.of(
  ("greg", "greg@example.com"),
  ("henry", "henry@example.com"),
  ("irene", "irene@example.com")
)

// Execute bulk insertion
val insertedCount = insertManyUsers(newUsers).commit(conn).unsafeRunSync()
println(s"Number of rows inserted: $insertedCount") // Should output "Number of rows inserted: 3"
```

### Transactions for Multiple Operations

To atomically execute multiple operations, use transactions. This ensures that either all operations succeed or all operations fail (rollback):

```scala
// Example of inserting a user and related information for that user
def createUserWithProfile(name: String, email: String, bio: String): DBIO[Long] =
  for
    userId    <- sql"INSERT INTO user (name, email) VALUES ($name, $email)".returning[Long]
    profileId <- sql"INSERT INTO user_profile (user_id, bio) VALUES ($userId, $bio)".returning[Long]
  yield userId
```

By executing this method using `.transaction`, both user insertion and profile insertion are processed as a single transaction:

```scala
// Execute within a transaction
val userId = createUserWithProfile("julia", "julia@example.com", "Programmer")
  .transaction(conn)
  .unsafeRunSync()

println(s"Created user ID: $userId")
```

If the insertion into the `user_profile` table fails, the insertion into the `user` table will also automatically roll back.

## Selecting Query Execution Methods

ldbc provides the following query execution methods for data update operations:

- `.commit(conn)` - Executes a write operation in auto-commit mode (suitable for simple, single update operations)
- `.rollback(conn)` - Executes a write operation and always rolls back (for testing and verification)
- `.transaction(conn)` - Executes operations within a transaction, committing only upon success (suitable when you want to treat multiple operations as a single unit)

```scala
// Execution in auto-commit mode (simple single operation)
updateUserEmail(1, "new@example.com").commit(conn)

// Execution for testing (changes are not saved)
insertUser("test", "test@example.com").rollback(conn)

// Multiple operations within a transaction (all succeed or all fail)
(for {
  userId <- insertUser("kate", "kate@example.com").returning[Long]
  _      <- sql"INSERT INTO user_roles (user_id, role_id) VALUES ($userId, 2)".update
} yield userId).transaction(conn)
```

## Error Handling

It's also important to handle errors that may occur during data update operations. In ldbc, you can use the `IO` monad from `cats-effect` to process errors:

```scala
import cats.effect.unsafe.IORuntime
import cats.effect.IO

implicit val runtime: IORuntime = IORuntime.global

// Example of error handling
def safeUpdateUser(id: Long, newEmail: String): Unit = {
  val result = updateUserEmail(id, newEmail)
    .commit(conn)
    .attempt // Convert IO result to Either[Throwable, Int]
    .unsafeRunSync()
    
  result match {
    case Right(count) => println(s"Number of rows updated: $count")
    case Left(error)  => println(s"An error occurred: ${error.getMessage}")
  }
}

// Execution example (updating with a non-existent ID)
safeUpdateUser(9999, "nonexistent@example.com")
```

## Summary

When performing data update operations with ldbc, it's good to remember the following points:

1. Use `.update` or `.returning[T]` (to retrieve auto-generated keys) for **insert operations**
2. Use `.update` for **update operations**, specifying target rows with a WHERE clause
3. Also use `.update` for **delete operations** (though it's a DELETE statement, the operation method is common)
4. Use `.transaction()` to execute **multiple operations** atomically
5. Use `.commit()` for simple **single operations**
6. Use `.rollback()` for **testing purposes** to discard changes

By appropriately combining these data update operations, you can efficiently and safely manipulate your database.

## Next Steps

Now you understand how to insert, update, and delete data in your database. At this point, you've learned all the basics of using ldbc. You've acquired the knowledge necessary for everyday database operations, including database connection, query execution, data reading and writing, and transaction management.

From here, we'll move on to more advanced topics. Let's start with [Error Handling](/en/tutorial/Error-Handling.md) to learn how to properly handle exceptions that can occur in database operations.
