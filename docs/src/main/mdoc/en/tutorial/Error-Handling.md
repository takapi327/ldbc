{%
  laika.title = Error Handling
  laika.metadata.language = en
%}

# Error Handling

In [Database Operations](/en/tutorial/Database-Operations.md), we learned about basic transaction management. However, in real applications, errors can occur for various reasons, such as database connection issues, SQL syntax errors, unique constraint violations, and more. This page explains how to properly handle errors in ldbc.

## Basics of Error Handling

ldbc follows functional programming principles and integrates with Cats Effect's error handling capabilities. The `DBIO` monad has a `MonadThrow` instance, providing a standard functional interface for raising and handling exceptions.

There are three main error handling methods you'll primarily use:

- `raiseError`: Raise an exception - converts a `Throwable` to a `DBIO[A]`
- `handleErrorWith`: Handle an exception - transforms a `DBIO[A]` to another `DBIO[A]` with an error handling function
- `attempt`: Capture an exception - transforms a `DBIO[A]` to a `DBIO[Either[Throwable, A]]`

## Types of Exceptions in Database Operations

Here are the main types of exceptions you might encounter when using ldbc:

1. **Connection Errors**: Occur when unable to connect to the database server (e.g., `ConnectException`)
2. **SQL Exceptions**: Errors during SQL execution (e.g., `SQLException`)
   - Syntax errors
   - Unique key constraint violations
   - Foreign key constraint violations
   - Timeouts
3. **Type Conversion Errors**: When database results can't be converted to the expected type (e.g., `ldbc.dsl.exception.DecodeFailureException`)

## Basic Usage of Error Handling

### Raising Exceptions (raiseError)

To explicitly raise an exception under specific conditions, use `raiseError`:

```scala
import cats.syntax.all.*
import ldbc.dsl.*

// Example of raising an error under specific conditions
def validateUserId(id: Int): DBIO[Int] = 
  if id <= 0 then 
    DBIO.raiseError[Int](new IllegalArgumentException("ID must be a positive value"))
  else 
    DBIO.pure(id)

// Usage example
val program: DBIO[String] = for
  id <- validateUserId(0)
  result <- sql"SELECT name FROM users WHERE id = $id".query[String].unsafe
yield result

// In this example, an error occurs because id is 0, and subsequent SQL isn't executed
```

### Handling Errors (handleErrorWith)

To handle errors that occur, use the `handleErrorWith` method:

```scala
import ldbc.dsl.*
import java.sql.SQLException

// Error handling example
val findUserById: DBIO[String] = for
  userId <- DBIO.pure(123)
  result <- sql"SELECT name FROM users WHERE id = $userId".query[String].unsafe.handleErrorWith {
    case e: SQLException if e.getMessage.contains("table 'users' doesn't exist") =>
      // Fallback when table doesn't exist
      DBIO.pure("User table has not been created yet")
    case e: SQLException =>
      // Handling other SQL errors
      DBIO.pure(s"Database error: ${e.getMessage}")
    case e: Throwable =>
      // Handling other errors
      DBIO.pure(s"Unexpected error: ${e.getMessage}")
  }
yield result
```

### Capturing Exceptions (attempt)

To capture errors as an `Either` type, use the `attempt` method:

```scala
import cats.syntax.all.*
import ldbc.dsl.*

// Example of capturing exceptions using attempt
val safeOperation: DBIO[String] = {
  val riskyOperation = sql"SELECT * FROM potentially_missing_table".query[String].unsafe
  
  riskyOperation.attempt.flatMap {
    case Right(result) => 
      DBIO.pure(s"Operation successful: $result")
    case Left(error) => 
      DBIO.pure(s"An error occurred: ${error.getMessage}")
  }
}
```

## Practical Error Handling Patterns

Here are some error handling patterns that are useful in real applications.

### Implementing Retry Functionality

An example of automatically retrying on temporary database connection errors:

```scala
import scala.concurrent.duration.*
import cats.effect.{IO, Sync}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import ldbc.dsl.*

// Retry implementation example - combining IO and DBIO
def retryWithBackoff[F[_]: Sync, A](
  dbioOperation: DBIO[A], 
  connection: Connection[F],
  maxRetries: Int = 3, 
  initialDelay: FiniteDuration = 100.millis,
  maxDelay: FiniteDuration = 2.seconds
): F[A] =
  def retryLoop(remainingRetries: Int, delay: FiniteDuration): F[A] =
    // Convert DBIO to F type (e.g., IO) and execute
    dbioOperation.run(connection).handleErrorWith { error =>
      if remainingRetries > 0 && isTransientError(error) then
        // For temporary errors, delay and retry
        val nextDelay = (delay * 2).min(maxDelay)
        Sync[F].sleep(delay) >> retryLoop(remainingRetries - 1, nextDelay)
      else
        // If max retries exceeded or permanent error, rethrow
        Sync[F].raiseError[A](error)
    }
  
  retryLoop(maxRetries, initialDelay)

// Example with concrete IO type
def retryDatabaseOperation[A](
  operation: DBIO[A],
  connection: Connection[IO],
  maxRetries: Int = 3
): IO[A] =
  retryWithBackoff(operation, connection, maxRetries)

// Helper method to determine if an error is transient
def isTransientError(error: Throwable): Boolean =
  error match
    case e: SQLException if e.getSQLState == "40001" => true // For deadlocks
    case e: SQLException if e.getSQLState == "08006" => true // For connection loss
    case e: Exception if e.getMessage.contains("connection reset") => true
    case _ => false

// Usage example
val query = sql"SELECT * FROM users".query[User].unsafe
val result = retryDatabaseOperation(query, myConnection)
```

### Error Handling with Custom Error Types and EitherT

An example of defining application-specific error types for more detailed error handling:

```scala
import cats.data.EitherT
import cats.syntax.all.*
import ldbc.dsl.*

// Application-specific error types
sealed trait AppDatabaseError
case class UserNotFoundError(id: Int) extends AppDatabaseError
case class DuplicateUserError(email: String) extends AppDatabaseError
case class DatabaseConnectionError(cause: Throwable) extends AppDatabaseError
case class UnexpectedDatabaseError(message: String, cause: Throwable) extends AppDatabaseError

// User model
case class User(id: Int, name: String, email: String)

// Example of error handling with EitherT
def findUserById(id: Int): EitherT[DBIO, AppDatabaseError, User] =
  val query = sql"SELECT id, name, email FROM users WHERE id = $id".query[User].to[Option]

  EitherT(
    query.attempt.map {
      case Right(user) => user.toRight(UserNotFoundError(id))
      case Left(e: SQLException) if e.getMessage.contains("Connection refused") =>
        Left(DatabaseConnectionError(e))
      case Left(e) =>
        Left(UnexpectedDatabaseError(e.getMessage, e))
    }
  )

// Usage example
val program = for
  user <- findUserById(123)
  // Other operations...
yield user

// Processing the result
val result: DBIO[Either[AppDatabaseError, User]] = program.value

// Final processing
val finalResult: DBIO[String] = result.flatMap {
  case Right(user) => DBIO.pure(s"User found: ${user.name}")
  case Left(UserNotFoundError(id)) => DBIO.pure(s"User with ID ${id} does not exist")
  case Left(DatabaseConnectionError(_)) => DBIO.pure("A database connection error occurred")
  case Left(error) => DBIO.pure(s"Error: $error")
}
```

### Combining Transactions with Error Handling

Example of error handling within a transaction:

```scala
import cats.effect.IO
import cats.syntax.all.*
import ldbc.dsl.*
import java.sql.Connection

// Error handling within transactions
def transferMoney(fromAccount: Int, toAccount: Int, amount: BigDecimal): DBIO[String] =
  val operation = for
    // Check balance of source account
    balance <- sql"SELECT balance FROM accounts WHERE id = $fromAccount".query[BigDecimal].unsafe
    
    _ <- if balance < amount then
           DBIO.raiseError[Unit](new IllegalStateException(s"Insufficient account balance: $balance < $amount"))
         else
           DBIO.pure(())
           
    // Withdraw from source account
    _ <- sql"UPDATE accounts SET balance = balance - $amount WHERE id = $fromAccount".update.unsafe
    
    // Deposit to target account
    _ <- sql"UPDATE accounts SET balance = balance + $amount WHERE id = $toAccount".update.unsafe
    
    // Create transaction record
    _ <- sql"""INSERT INTO transactions (from_account, to_account, amount, timestamp) 
         VALUES ($fromAccount, $toAccount, $amount, NOW())""".update.unsafe
  yield "Money transfer completed"
  
  // Wrap as transaction (automatically rolled back on error)
  operation.handleErrorWith { error =>
    DBIO.pure(s"Transfer error: ${error.getMessage}")
  }

// Usage example
val fromAccount: Int = ???
val toAccount: Int = ???
val amount: BigDecimal = ???

// Create Connector
val connector = Connector.fromDataSource(datasource)

transferMoney(fromAccount, toAccount, amount).transaction(connector)
```

## Summary

Error handling is an important aspect of robust database applications. In ldbc, you can handle errors explicitly based on functional programming principles. The main points are:

- Use `raiseError` to raise exceptions
- Use `handleErrorWith` to handle exceptions
- Use `attempt` to capture exceptions as `Either`
- Define appropriate error types for application-specific error handling
- Combine transactions with error handling to maintain data integrity

By utilizing these techniques, you can implement database operations that are resilient to unexpected errors and easy to maintain.

## Next Steps

Now that you understand error handling, move on to [Logging](/en/tutorial/Logging.md) to learn how to log query executions and errors. Logging is important for debugging and monitoring.
