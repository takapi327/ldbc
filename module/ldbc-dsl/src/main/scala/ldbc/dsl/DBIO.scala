/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import java.time.*

import scala.concurrent.duration.FiniteDuration

import cats.*
import cats.data.NonEmptyList
import cats.free.Free
import cats.syntax.all.*

import cats.effect.kernel.{ CancelScope, Poll, Sync }

import ldbc.sql.*

import ldbc.dsl.codec.*
import ldbc.dsl.exception.*
import ldbc.dsl.util.FactoryCompat

import ldbc.*
import ldbc.free.*
import ldbc.logging.LogEvent

/**
 * DBIO (Database I/O) provides a set of combinators for building type-safe database operations
 * using the Free monad pattern. It abstracts over JDBC operations and provides a functional
 * interface for executing SQL queries with proper resource management and error handling.
 * 
 * The DBIO type is a type alias for `Free[ConnectionOp, A]`, which represents a computation
 * that produces a value of type `A` when run against a database connection.
 * 
 * @example
 * {{{
 * import ldbc.dsl.*
 * 
 * val query: DBIO[List[User]] = 
 *   sql"SELECT * FROM users WHERE age > ${ 18 }".query[User].to[List]
 *   
 * val result: F[List[User]] = query.run(connector)
 * }}}
 */
object DBIO:

  /**
   * Binds dynamic parameters to a PreparedStatement.
   * 
   * This method is the core mechanism for safely binding values to SQL prepared statements.
   * It processes a list of dynamic parameters, extracting their encoded values and binding
   * them to the appropriate positions in the PreparedStatement.
   * 
   * The binding process:
   * 1. Validates all parameters, collecting any encoding failures
   * 2. Maps each encoded value to the correct PreparedStatement setter method
   * 3. Handles null values by calling setNull with the appropriate SQL type
   * 
   * @param params List of dynamic parameters to bind. Each parameter contains either
   *               a successfully encoded value or encoding error information
   * @return A PreparedStatementIO action that performs the binding operations
   * @throws IllegalArgumentException if any parameter contains encoding failures
   * 
   * @note JDBC uses 1-based indexing for parameters, so the index is incremented by 1
   * @note The method uses pattern matching to determine the appropriate setter method
   *       based on the runtime type of each encoded value
   */
  private def paramBind(params: List[Parameter.Dynamic]): PreparedStatementIO[Unit] =
    val encoded = params.foldLeft(PreparedStatementIO.pure(List.empty[Encoder.Supported])) {
      case (acc, param) =>
        for
          acc$  <- acc
          value <- param match
                     case Parameter.Dynamic.Success(value)  => PreparedStatementIO.pure(value)
                     case Parameter.Dynamic.Failure(errors) =>
                       PreparedStatementIO.raiseError(new IllegalArgumentException(errors.mkString(", ")))
        yield acc$ :+ value
    }

    for
      encodes <- encoded
      _       <- encodes.zipWithIndex.foldLeft(PreparedStatementIO.pure[Unit](())) {
             case (acc, (value, index)) =>
               acc *> (value match
                 case value: Boolean       => PreparedStatementIO.setBoolean(index + 1, value)
                 case value: Byte          => PreparedStatementIO.setByte(index + 1, value)
                 case value: Short         => PreparedStatementIO.setShort(index + 1, value)
                 case value: Int           => PreparedStatementIO.setInt(index + 1, value)
                 case value: Long          => PreparedStatementIO.setLong(index + 1, value)
                 case value: Float         => PreparedStatementIO.setFloat(index + 1, value)
                 case value: Double        => PreparedStatementIO.setDouble(index + 1, value)
                 case value: BigDecimal    => PreparedStatementIO.setBigDecimal(index + 1, value)
                 case value: String        => PreparedStatementIO.setString(index + 1, value)
                 case value: Array[Byte]   => PreparedStatementIO.setBytes(index + 1, value)
                 case value: LocalDate     => PreparedStatementIO.setDate(index + 1, value)
                 case value: LocalTime     => PreparedStatementIO.setTime(index + 1, value)
                 case value: LocalDateTime => PreparedStatementIO.setTimestamp(index + 1, value)
                 case None                 => PreparedStatementIO.setNull(index + 1, ldbc.sql.Types.NULL))
           }
    yield ()

  /**
   * Reads a single row from a ResultSet and decodes it using the provided decoder.
   * 
   * This method expects exactly one row in the ResultSet. It will:
   * - Advance to the first row using ResultSet.next()
   * - Decode the row using the provided decoder
   * - Return an error if no rows are found or if decoding fails
   * 
   * @param statement The SQL statement being executed (used for error messages)
   * @param decoder The decoder to convert the row data into type T
   * @tparam T The type to decode the row into
   * @return A ResultSetIO action that produces a value of type T
   * @throws UnexpectedContinuation if the ResultSet is empty
   * @throws DecodeFailureException if the decoder fails to decode the row
   */
  private def unique[T](
    statement: String,
    decoder:   Decoder[T]
  ): ResultSetIO[T] =
    ResultSetIO.next().flatMap {
      case true =>
        decoder.decode(1, statement).flatMap {
          case Right(value) => ResultSetIO.pure(value)
          case Left(error)  =>
            ResultSetIO.raiseError(new DecodeFailureException(error.message, decoder.offset, statement, error.cause))
        }
      case false => ResultSetIO.raiseError(new UnexpectedContinuation("Expected ResultSet to have at least one row."))
    }

  /**
   * Reads all rows from a ResultSet and collects them into a collection of type G[T].
   * 
   * This method iterates through all rows in the ResultSet, decoding each row
   * and accumulating the results into a collection using the provided factory.
   * It continues until ResultSet.next() returns false.
   * 
   * @param statement The SQL statement being executed (used for error messages)
   * @param decoder The decoder to convert each row into type T
   * @param factoryCompat A factory for building the target collection type G[T]
   * @tparam G The collection type constructor (e.g., List, Vector, Set)
   * @tparam T The element type to decode each row into
   * @return A ResultSetIO action that produces a collection of decoded values
   * @throws DecodeFailureException if the decoder fails on any row
   */
  private def whileM[G[_], T](
    statement:     String,
    decoder:       Decoder[T],
    factoryCompat: FactoryCompat[T, G[T]]
  ): ResultSetIO[G[T]] =
    val builder = factoryCompat.newBuilder

    def loop(acc: collection.mutable.Builder[T, G[T]]): ResultSetIO[collection.mutable.Builder[T, G[T]]] =
      ResultSetIO.next().flatMap {
        case true =>
          decoder
            .decode(1, statement)
            .flatMap {
              case Right(value) => ResultSetIO.pure(value)
              case Left(error)  =>
                ResultSetIO.raiseError(
                  new DecodeFailureException(error.message, decoder.offset, statement, error.cause)
                )
            }
            .flatMap(v => loop(acc += v))
        case false => ResultSetIO.pure(acc)
      }

    loop(builder).map(_.result())

  /**
   * Reads all rows from a ResultSet and returns them as a NonEmptyList.
   * 
   * This method ensures that at least one row exists in the ResultSet.
   * If the ResultSet is empty, it raises an UnexpectedEnd error.
   * 
   * @param statement The SQL statement being executed (used for error messages)
   * @param decoder The decoder to convert each row into type A
   * @tparam A The type to decode each row into
   * @return A ResultSetIO action that produces a NonEmptyList of decoded values
   * @throws UnexpectedEnd if the ResultSet contains no rows
   * @throws DecodeFailureException if the decoder fails on any row
   */
  private def nel[A](
    statement: String,
    decoder:   Decoder[A]
  ): ResultSetIO[NonEmptyList[A]] =
    whileM[List, A](statement, decoder, summon[FactoryCompat[A, List[A]]]).flatMap { results =>
      if results.isEmpty then ResultSetIO.raiseError(new UnexpectedEnd("No results found"))
      else ResultSetIO.pure(NonEmptyList.fromListUnsafe(results))
    }

  /**
   * Executes a SQL query that returns exactly one row and decodes it to type A.
   * 
   * This method:
   * 1. Prepares a statement with the given SQL
   * 2. Binds the provided parameters
   * 3. Executes the query
   * 4. Reads exactly one row from the result
   * 5. Closes the prepared statement
   * 6. Logs the operation (success or failure)
   * 
   * @param statement The SQL query to execute
   * @param params Dynamic parameters to bind to the query
   * @param decoder Decoder to convert the result row to type A
   * @tparam A The result type
   * @return A DBIO action that produces a single value of type A
   * @throws UnexpectedContinuation if the ResultSet is empty
   * @throws DecodeFailureException if decoding fails
   * 
   * @example
   * {{{
   * val userId = 42
   * val query = DBIO.queryA(
   *   "SELECT name FROM users WHERE id = ?",
   *   List(Parameter.Dynamic.Success(userId)),
   *   Decoder[String]
   * )
   * }}}
   */
  def queryA[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[A] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, unique(statement, decoder))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  /**
   * Executes a SQL query and collects all results into a collection of type G[A].
   * 
   * This method allows you to specify the target collection type through the
   * factory parameter. Common usage includes collecting to List, Vector, Set, etc.
   * 
   * @param statement The SQL query to execute
   * @param params Dynamic parameters to bind to the query
   * @param decoder Decoder to convert each result row to type A
   * @param factory Factory for building the target collection type
   * @tparam G The collection type constructor (e.g., List, Vector, Set)
   * @tparam A The element type
   * @return A DBIO action that produces a collection of decoded values
   * @throws DecodeFailureException if decoding fails for any row
   * 
   * @example
   * {{{
   * val query = DBIO.queryTo[List, String](
   *   "SELECT name FROM users WHERE age > ?",
   *   List(Parameter.Dynamic.Success(18)),
   *   Decoder[String],
   *   summon[FactoryCompat[String, List[String]]]
   * )
   * }}}
   */
  def queryTo[G[_], A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A],
    factory:   FactoryCompat[A, G[A]]
  ): DBIO[G[A]] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, whileM(statement, decoder, factory))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  /**
   * Executes a SQL query that returns at most one row as an Option[A].
   * 
   * This method is useful when a query might return zero or one row.
   * It returns:
   * - Some(a) if exactly one row is found and successfully decoded
   * - None if no rows are found
   * - An error if more than one row is found
   * 
   * @param statement The SQL query to execute
   * @param params Dynamic parameters to bind to the query
   * @param decoder Decoder to convert the result row to type A
   * @tparam A The result type
   * @return A DBIO action that produces an Option[A]
   * @throws UnexpectedContinuation if more than one row is found
   * @throws DecodeFailureException if decoding fails
   * 
   * @example
   * {{{
   * val query = DBIO.queryOption(
   *   "SELECT name FROM users WHERE email = ?",
   *   List(Parameter.Dynamic.Success("user@example.com")),
   *   Decoder[String]
   * )
   * // Returns Some(name) if user exists, None otherwise
   * }}}
   */
  def queryOption[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[Option[A]] =
    val decoded: ResultSetIO[Option[A]] =
      for
        data <- ResultSetIO.next().flatMap {
                  case true =>
                    decoder.decode(1, statement).flatMap {
                      case Right(value) => ResultSetIO.pure(Option(value))
                      case Left(error)  =>
                        ResultSetIO.raiseError(
                          new DecodeFailureException(error.message, decoder.offset, statement, error.cause)
                        )
                    }
                  case false => ResultSetIO.pure(None)
                }
        next   <- ResultSetIO.next()
        result <- if next then {
                    ResultSetIO.raiseError(
                      new UnexpectedContinuation(
                        "Expected ResultSet exhaustion, but more rows were available."
                      )
                    )
                  } else ResultSetIO.pure(data)
      yield result
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, decoded)
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  /**
   * Executes a SQL query that returns at least one row as a NonEmptyList[A].
   * 
   * This method ensures that the query returns at least one result.
   * It's useful when you need compile-time guarantees that your result
   * set is not empty.
   * 
   * @param statement The SQL query to execute
   * @param params Dynamic parameters to bind to the query
   * @param decoder Decoder to convert each result row to type A
   * @tparam A The result type
   * @return A DBIO action that produces a NonEmptyList[A]
   * @throws UnexpectedEnd if no rows are found
   * @throws DecodeFailureException if decoding fails for any row
   * 
   * @example
   * {{{
   * val query = DBIO.queryNel(
   *   "SELECT id FROM users WHERE role = ?",
   *   List(Parameter.Dynamic.Success("admin")),
   *   Decoder[Int]
   * )
   * // Fails at runtime if no admin users exist
   * }}}
   */
  def queryNel[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[NonEmptyList[A]] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeQuery()
                   )
      result <- ConnectionIO.embed(resultSet, nel(statement, decoder))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  /**
   * Executes a SQL update statement (INSERT, UPDATE, DELETE) with parameters.
   * 
   * This method prepares the statement, binds parameters, executes the update,
   * and returns the number of affected rows.
   * 
   * @param statement The SQL update statement to execute
   * @param params Dynamic parameters to bind to the statement
   * @return A DBIO action that produces the number of affected rows
   * 
   * @example
   * {{{
   * val update = DBIO.update(
   *   "UPDATE users SET name = ? WHERE id = ?",
   *   List(
   *     Parameter.Dynamic.Success("New Name"),
   *     Parameter.Dynamic.Success(42)
   *   )
   * )
   * // Returns the number of updated rows
   * }}}
   */
  def update(
    statement: String,
    params:    List[Parameter.Dynamic]
  ): DBIO[Int] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement)
      result           <- ConnectionIO.embed(
                  prepareStatement,
                  paramBind(params) *> PreparedStatementIO.executeUpdate()
                )
      _ <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  /**
   * Executes a raw SQL update statement without parameters.
   * 
   * This method uses a Statement instead of PreparedStatement and is suitable
   * for DDL operations or updates that don't require parameter binding.
   * 
   * @param statement The SQL statement to execute
   * @return A DBIO action that produces the number of affected rows
   * 
   * @example
   * {{{
   * val create = DBIO.updateRaw(
   *   "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(255))"
   * )
   * }}}
   * 
   * @note Use parameterized queries (update method) when dealing with user input
   *       to prevent SQL injection attacks
   */
  def updateRaw(statement: String): DBIO[Int] =
    (for
      stmt   <- ConnectionIO.createStatement()
      result <- ConnectionIO.embed(stmt, StatementIO.executeUpdate(statement))
      _      <- ConnectionIO.embed(stmt, StatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, List.empty, ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, List.empty))

  /**
   * Executes multiple SQL statements separated by semicolons as a batch.
   * 
   * This method splits the input by semicolons and executes each statement
   * as part of a batch operation. It's useful for executing multiple DDL
   * or DML statements in a single round trip to the database.
   * 
   * @param statement Multiple SQL statements separated by semicolons
   * @return A DBIO action that produces an array of update counts
   * 
   * @example
   * {{{
   * val batch = DBIO.updateRaws("""
   *   CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255));
   *   CREATE INDEX idx_name ON users(name);
   *   INSERT INTO users VALUES (1, 'Alice'), (2, 'Bob');
   * """)
   * // Returns an array with the result of each statement
   * }}}
   * 
   * @note Each statement is executed independently; failure of one doesn't
   *       necessarily rollback others unless in a transaction
   */
  def updateRaws(statement: String): DBIO[Array[Int]] =
    (for
      stmt <- ConnectionIO.createStatement()
      statements = statement.trim.split(";").toList
      _      <- ConnectionIO.embed(stmt, statements.map(statement => StatementIO.addBatch(statement)).sequence)
      result <- ConnectionIO.embed(stmt, StatementIO.executeBatch())
      _      <- ConnectionIO.embed(stmt, StatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, List.empty, ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, List.empty))

  /**
   * Executes an INSERT statement and returns generated keys.
   * 
   * This method is typically used for INSERT statements where you want to
   * retrieve auto-generated keys (like auto-increment IDs). The statement
   * is prepared with RETURN_GENERATED_KEYS flag.
   * 
   * @param statement The INSERT statement to execute
   * @param params Dynamic parameters to bind to the statement
   * @param decoder Decoder to convert the generated key to type A
   * @tparam A The type of the generated key
   * @return A DBIO action that produces the generated key
   * @throws UnexpectedContinuation if no keys are generated
   * @throws DecodeFailureException if decoding the key fails
   * 
   * @example
   * {{{
   * val insert = DBIO.returning[Long](
   *   "INSERT INTO users (name, email) VALUES (?, ?)",
   *   List(
   *     Parameter.Dynamic.Success("Alice"),
   *     Parameter.Dynamic.Success("alice@example.com")
   *   ),
   *   Decoder[Long] // For auto-increment ID
   * )
   * // Returns the generated user ID
   * }}}
   */
  def returning[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A]
  ): DBIO[A] =
    (for
      prepareStatement <- ConnectionIO.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
      resultSet        <- ConnectionIO.embed(
                     prepareStatement,
                     paramBind(params) *> PreparedStatementIO.executeUpdate() *> PreparedStatementIO
                       .getGeneratedKeys()
                   )
      result <- ConnectionIO.embed(resultSet, unique(statement, decoder))
      _      <- ConnectionIO.embed(prepareStatement, PreparedStatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value)))

  /**
   * Executes a batch of SQL statements.
   * 
   * This method executes multiple statements as a batch operation,
   * which is more efficient than executing them individually.
   * Unlike updateRaws, this takes a list of complete statements.
   * 
   * @param statements List of SQL statements to execute
   * @return A DBIO action that produces an array of update counts
   * 
   * @example
   * {{{
   * val statements = List(
   *   "INSERT INTO users (name) VALUES ('Alice')",
   *   "INSERT INTO users (name) VALUES ('Bob')",
   *   "UPDATE users SET active = true WHERE name = 'Alice'"
   * )
   * val batch = DBIO.sequence(statements)
   * // Returns update counts for each statement
   * }}}
   */
  def sequence(
    statements: List[String]
  ): DBIO[Array[Int]] =
    (for
      statement <- ConnectionIO.createStatement()
      _         <- ConnectionIO.embed(statement, statements.map(statement => StatementIO.addBatch(statement)).sequence)
      result    <- ConnectionIO.embed(statement, StatementIO.executeBatch())
      _         <- ConnectionIO.embed(statement, StatementIO.close())
    yield result).onError { ex =>
      ConnectionIO.performLogging(LogEvent.ProcessingFailure(statements.mkString("\n"), List.empty, ex))
    } <*
      ConnectionIO.performLogging(LogEvent.Success(statements.mkString("\n"), List.empty))

  /**
   * Creates a streaming query that processes results incrementally.
   * 
   * This method is ideal for processing large result sets without loading
   * all data into memory at once. It uses JDBC fetch size to control
   * how many rows are fetched from the database in each round trip.
   * 
   * The stream is resource-safe and will automatically close the
   * PreparedStatement when the stream terminates (normally or via error).
   * 
   * @param statement The SQL query to execute
   * @param params Dynamic parameters to bind to the query
   * @param decoder Decoder to convert each result row to type A
   * @param fetchSize Number of rows to fetch in each batch from the database
   * @tparam A The result type
   * @return An fs2.Stream that emits decoded values
   * 
   * @example
   * {{{
   * val stream = DBIO.stream(
   *   "SELECT * FROM large_table WHERE category = ?",
   *   List(Parameter.Dynamic.Success("books")),
   *   Decoder[Product],
   *   fetchSize = 1000
   * )
   * 
   * // Process results as they arrive
   * stream
   *   .evalMap(product => processProduct(product))
   *   .compile
   *   .drain
   *   .run(connector)
   * }}}
   * 
   * @note The fetch size is a hint to the JDBC driver and may be ignored
   */
  def stream[A](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[A],
    fetchSize: Int
  ): fs2.Stream[DBIO, A] =
    (for
      preparedStatement              <- fs2.Stream.eval(ConnectionIO.prepareStatement(statement))
      (preparedStatement, resultSet) <- fs2.Stream.bracket {
                                          ConnectionIO.embed(
                                            preparedStatement,
                                            for
                                              _         <- PreparedStatementIO.setFetchSize(fetchSize)
                                              _         <- paramBind(params)
                                              resultSet <- PreparedStatementIO.executeQuery()
                                            yield (preparedStatement, resultSet)
                                          )
                                        }((preparedStatement, _) =>
                                          ConnectionIO.embed(preparedStatement, PreparedStatementIO.close())
                                        )
      result <- fs2.Stream.unfoldEval(resultSet) { rs =>
                  ConnectionIO.embed(
                    rs,
                    ResultSetIO.next().flatMap {
                      case true =>
                        decoder
                          .decode(1, statement)
                          .flatMap {
                            case Right(value) => ResultSetIO.pure(value)
                            case Left(error)  =>
                              ResultSetIO.raiseError(
                                new DecodeFailureException(error.message, decoder.offset, statement, error.cause)
                              )
                          }
                          .map(name => Some((name, rs)))
                      case false => ResultSetIO.pure(None)
                    }
                  )
                }
    yield result).onError { ex =>
      fs2.Stream.eval(ConnectionIO.performLogging(LogEvent.ProcessingFailure(statement, params.map(_.value), ex)))
    } <*
      fs2.Stream.eval(ConnectionIO.performLogging(LogEvent.Success(statement, params.map(_.value))))

  /**
   * Combines multiple DBIO actions into a single action that produces a list of results.
   * 
   * This is equivalent to the traverse operation, executing each DBIO in sequence
   * and collecting all results.
   * 
   * @param dbios Variable number of DBIO actions to sequence
   * @tparam A The result type of each DBIO
   * @return A DBIO action that produces a List[A] containing all results
   * 
   * @example
   * {{{
   * val combined = DBIO.sequence(
   *   sql"SELECT COUNT(*) FROM users".query[Int].to[Option],
   *   sql"SELECT COUNT(*) FROM products".query[Int].to[Option]
   * )
   * // Returns List(userCount, productCount)
   * }}}
   */
  def sequence[A](dbios: DBIO[A]*): DBIO[List[A]] =
    dbios.toList.sequence

  /**
   * Lifts a pure value into the DBIO context.
   * 
   * @param value The value to lift
   * @tparam A The type of the value
   * @return A DBIO action that produces the given value
   */
  def pure[A](value: A): DBIO[A] = Free.pure(value)

  /**
   * Creates a failed DBIO action with the given error.
   * 
   * @param e The error to raise
   * @tparam A The expected result type (never produced)
   * @return A DBIO action that fails with the given error
   */
  def raiseError[A](e: Throwable): DBIO[A] = ConnectionIO.raiseError(e)

  /**
   * Provides a Sync type class instance for DBIO.
   * 
   * This enables DBIO to be used with any Cats Effect operations that
   * require a Sync constraint, including error handling, resource management,
   * and cancellation support.
   * 
   * The implementation delegates to the underlying ConnectionIO Free monad
   * operations, ensuring proper effect handling throughout the DBIO lifecycle.
   */
  implicit val syncDBIO: Sync[DBIO] =
    new Sync[DBIO]:
      val monad = Free.catsFreeMonadForFree[ConnectionOp]
      override val applicative:                                            Applicative[DBIO] = monad
      override val rootCancelScope:                                        CancelScope       = CancelScope.Cancelable
      override def pure[A](x:        A):                                   DBIO[A]           = monad.pure(x)
      override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]):            DBIO[B]           = monad.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f:       A => DBIO[Either[A, B]]): DBIO[B]           = monad.tailRecM(a)(f)
      override def raiseError[A](e: Throwable):                              DBIO[A] = ConnectionIO.raiseError(e)
      override def handleErrorWith[A](fa: DBIO[A])(f: Throwable => DBIO[A]): DBIO[A] =
        ConnectionIO.handleErrorWith(fa)(f)
      override def monotonic:                                   DBIO[FiniteDuration] = ConnectionIO.monotonic
      override def realTime:                                    DBIO[FiniteDuration] = ConnectionIO.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A):    DBIO[A]              = ConnectionIO.suspend(hint)(thunk)
      override def forceR[A, B](fa: DBIO[A])(fb:      DBIO[B]): DBIO[B]              = ConnectionIO.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[DBIO] => DBIO[A]):    DBIO[A]    = ConnectionIO.uncancelable(body)
      override def canceled:                                        DBIO[Unit] = ConnectionIO.canceled
      override def onCancel[A](fa:       DBIO[A], fin: DBIO[Unit]): DBIO[A]    = ConnectionIO.onCancel(fa, fin)

  /**
   * Extension methods for DBIO values that provide various execution strategies.
   * 
   * This class is made available through an implicit conversion and provides
   * methods to run DBIO actions with different transaction semantics.
   * 
   * @param dbio The DBIO action to execute
   * @tparam A The result type of the DBIO action
   */
  private[ldbc] class Ops[A](dbio: DBIO[A]):

    /**
     * Runs the DBIO action using the provided connector.
     * 
     * This is the basic execution method that runs the action
     * with whatever transaction settings are currently active.
     * 
     * @param connector The database connector to use
     * @tparam F The effect type (e.g., IO, Task)
     * @return The result wrapped in the effect type F
     */
    def run[F[_]](connector: Connector[F]): F[A] = connector.run(dbio)

    /**
     * Runs the DBIO action in read-only mode.
     * 
     * This ensures that the connection is set to read-only before executing
     * the action and restored afterwards. Useful for ensuring queries don't
     * accidentally modify data.
     * 
     * @param connector The database connector to use
     * @tparam F The effect type
     * @return The result wrapped in the effect type F
     */
    def readOnly[F[_]](connector: Connector[F]): F[A] =
      connector.run(ConnectionIO.setReadOnly(true) *> dbio <* ConnectionIO.setReadOnly(false))

    /**
     * Runs the DBIO action with auto-commit enabled.
     * 
     * Each statement is automatically committed after execution.
     * This is typically used for single statement operations that
     * should be immediately persisted.
     * 
     * @param connector The database connector to use
     * @tparam F The effect type
     * @return The result wrapped in the effect type F
     */
    def commit[F[_]](connector: Connector[F]): F[A] =
      connector.run(ConnectionIO.setReadOnly(false) *> ConnectionIO.setAutoCommit(true) *> dbio)

    /**
     * Runs the DBIO action and rolls back all changes at the end.
     * 
     * This is useful for testing or dry-run scenarios where you want
     * to execute operations but not persist any changes.
     * 
     * @param connector The database connector to use
     * @tparam F The effect type
     * @return The result wrapped in the effect type F
     */
    def rollback[F[_]](connector: Connector[F]): F[A] =
      connector.run(
        ConnectionIO.setReadOnly(false) *>
          ConnectionIO.setAutoCommit(false) *>
          dbio <*
          ConnectionIO.rollback() <*
          ConnectionIO.setAutoCommit(true)
      )

    /**
     * Runs the DBIO action within a transaction.
     * 
     * The action is executed with auto-commit disabled. If the action
     * completes successfully, changes are committed. If an error occurs,
     * all changes are rolled back.
     * 
     * @param connector The database connector to use
     * @tparam F The effect type
     * @return The result wrapped in the effect type F
     * 
     * @example
     * {{{
     * val transfer = for {
     *   _ <- sql"UPDATE accounts SET balance = balance - 100 WHERE id = 1".update
     *   _ <- sql"UPDATE accounts SET balance = balance + 100 WHERE id = 2".update
     * } yield ()
     * 
     * transfer.transaction(connector) // Both updates succeed or both fail
     * }}}
     */
    def transaction[F[_]](connector: Connector[F]): F[A] =
      connector.run(
        (ConnectionIO.setReadOnly(false) *> ConnectionIO.setAutoCommit(false) *> dbio)
          .onError { _ =>
            ConnectionIO.rollback()
          } <* ConnectionIO.commit() <* ConnectionIO.setAutoCommit(true)
      )
