{%
  laika.title = Selecting Data
  laika.metadata.language = en
%}

# Selecting Data

Now that we've learned about [Parameterized Queries](/en/tutorial/Parameterized-Queries.md), let's look at how to retrieve data in various formats. This page explains how to efficiently fetch data using SELECT queries and map them to Scala types.

One of ldbc's most powerful features is its ability to easily map database results to Scala types. It can handle various data formats, from simple primitive types to complex case classes.

*Note: In this tutorial, we use a `Connector` to execute database operations. Create it as follows:*

```scala
import ldbc.connector.*

// Create Connector
val connector = Connector.fromDataSource(datasource)
```

## Basic Data Retrieval Workflow

The basic flow for retrieving data with ldbc is as follows:

1. Create an SQL query with the `sql` interpolator
2. Specify the result type with `.query[T]`
3. Convert results to a collection with `.to[Collection]` (optional)
4. Execute the query using `.readOnly(connector)`/`.commit(connector)`/`.transaction(connector)`, etc.
5. Process the results

Let's look at this flow along with the type transformations in code.

## Reading Rows into Collections

In our first query, let's look at an example that retrieves some usernames into a list and outputs them. We'll show how the types change at each step:

```scala
sql"SELECT name FROM user"
  .query[String]                 // Query[String]
  .to[List]                      // DBIO[List[String]]
  .readOnly(connector)                // IO[List[String]]
  .unsafeRunSync()               // List[String]
  .foreach(println)              // Unit
```

Let's explain this code in detail:

- `sql"SELECT name FROM user"` - Defines the SQL query.
- `.query[String]` - Maps each row result to a `String` type. This generates a `Query[String]` type.
- `.to[List]` - Aggregates the results into a `List`. This generates a `DBIO[List[String]]` type. This method can be used with any collection type that implements `FactoryCompat` (such as `List`, `Vector`, `Set`, etc.). Similar methods are:
    - `.unsafe` which returns a single value, raising an exception if there is not exactly one row returned.
    - `.option` which returns an Option, raising an exception if there is more than one row returned.
    - `.nel` which returns an NonEmptyList, raising an exception if there are no rows returned.
- `.readOnly(connector)` - Executes the query using the connection in read-only mode. The return value is `IO[List[String]]`.
- `.unsafeRunSync()` - Executes the IO monad to get the actual result (`List[String]`).
- `.foreach(println)` - Outputs each element of the result.

## Multiple Column Queries

Of course, you can select multiple columns and map them to tuples:

```scala
sql"SELECT name, email FROM user"
  .query[(String, String)]       // Query[(String, String)]
  .to[List]                      // DBIO[List[(String, String)]]
  .readOnly(connector)                // IO[List[(String, String)]]
  .unsafeRunSync()               // List[(String, String)]
  .foreach { case (name, email) => println(s"Name: $name, Email: $email") }
```

In multiple column queries, it's important that the order of selected columns matches the order of type parameters in the tuple. In the above example, `name` corresponds to the 1st column (tuple's `_1`), and `email` to the 2nd column (tuple's `_2`).

## Mapping to Case Classes

While tuples are convenient, we recommend using case classes to improve code readability. ldbc can automatically map query results to case classes:

```scala
// Case class representing user information
case class User(id: Long, name: String, email: String)

// Query execution and mapping
sql"SELECT id, name, email FROM user"
  .query[User]                   // Query[User]
  .to[List]                      // DBIO[List[User]]
  .readOnly(connector)                // IO[List[User]]
  .unsafeRunSync()               // List[User]
  .foreach(user => println(s"ID: ${user.id}, Name: ${user.name}, Email: ${user.email}"))
```

**Important**: The field names in the case class need to match the column names selected in the SQL query. The order also needs to match, but ldbc will map appropriately if the names match exactly.

### How Mapping Works

The following diagram shows how SQL query results are mapped to the User model:

![Simple Mapping Mechanism](../../../img/select-mapping-simple-EN.svg)

As shown in the diagram:

1. **SQL Execution**: Create a query with `sql"SELECT id, name, email FROM user"` string interpolation and specify the result type with `.query[User]`
2. **ResultSet**: The result set returned from the database (values stored sequentially starting from column number 1)
3. **Decoder Resolution**: At compile time, compose basic Decoders like `Decoder[Long]` and `Decoder[String]` to build `Decoder[User]`
4. **Mapping Process**: At runtime, convert each column value to the appropriate type with the decode method and finally generate a User instance

This type-safe mapping allows errors to be detected at compile time, preventing runtime type errors.

## Joining Multiple Tables and Nested Case Classes

When retrieving data from multiple tables using `JOIN`, you can map to nested case class structures. In the following example, we join the `city` and `country` tables and map the result to a `CityWithCountry` class:

```scala
// Case class representing a city
case class City(id: Long, name: String)

// Case class representing a country
case class Country(code: String, name: String, region: String)

// Case class combining city and country information
case class CityWithCountry(city: City, country: Country)

// Executing a join query
sql"""
  SELECT
    city.id,
    city.name,
    country.code,
    country.name,
    country.region
  FROM city
  JOIN country ON city.country_code = country.code
"""
  .query[CityWithCountry]        // Query[CityWithCountry]
  .to[List]                      // DBIO[List[CityWithCountry]]
  .readOnly(connector)                // IO[List[CityWithCountry]]
  .unsafeRunSync()               // List[CityWithCountry]
  .foreach(cityWithCountry => println(
    s"City: ${cityWithCountry.city.name}, Country: ${cityWithCountry.country.name}"
  ))
```

A feature of ldbc is that columns specified in the format `table_name.column_name` are automatically mapped to `class_name.field_name`. This enables the following mappings in the above example:

- `city.id` → `CityWithCountry.city.id`
- `city.name` → `CityWithCountry.city.name`
- `country.code` → `CityWithCountry.country.code`
- `country.name` → `CityWithCountry.country.name`
- `country.region` → `CityWithCountry.country.region`

### How Complex Mapping Works

The following diagram shows how JOIN results are mapped to nested case classes (CityWithCountry):

![Complex Mapping Mechanism (JOIN Results)](../../../img/select-mapping-complex-EN.svg)

As shown in the diagram:

1. **SQL Execution**: Execute a SQL query with JOIN clause and specify the result type with `.query[CityWithCountry]`
2. **ResultSet**: Result set with 5 columns (city.id, city.name, country.code, country.name, country.region)
3. **Decoder Construction**: 
   - Decoder for City: Compose `Decoder[Long]` and `Decoder[String]` to create `Decoder[City]`
   - Decoder for Country: Compose three `Decoder[String]`s to create `Decoder[Country]`
   - Finally compose both to build `Decoder[CityWithCountry]`
4. **Mapping Process**: 
   - Generate City object from columns 1,2
   - Generate Country object from columns 3,4,5
   - Combine both to create CityWithCountry instance

In this way, ldbc performs type-safe mapping through Decoder composition even for complex nested structures.

## Using Tuples for Join Queries

Instead of nested case classes, you can also use tuples to retrieve data from multiple tables:

```scala
case class City(id: Long, name: String)
case class Country(code: String, name: String, region: String)

sql"""
  SELECT
    city.id,
    city.name,
    country.code,
    country.name,
    country.region
  FROM city
  JOIN country ON city.country_code = country.code
"""
  .query[(City, Country)]        // Query[(City, Country)]
  .to[List]                      // DBIO[List[(City, Country)]]
  .readOnly(connector)                // IO[List[(City, Country)]]
  .unsafeRunSync()               // List[(City, Country)]
  .foreach { case (city, country) => 
    println(s"City: ${city.name}, Country: ${country.name}")
  }
```

It's important to note that when using tuples, the table name and the case class name need to match. That is, the `city` table is mapped to the `City` class, and the `country` table is mapped to the `Country` class.

## Table Aliases and Mapping

When using aliases for tables in SQL statements, you need to match the case class name with that alias:

```scala
// Case class names matching alias names
case class C(id: Long, name: String)
case class CT(code: String, name: String, region: String)

sql"""
  SELECT
    c.id,
    c.name,
    ct.code,
    ct.name,
    ct.region
  FROM city AS c
  JOIN country AS ct ON c.country_code = ct.code
"""
  .query[(C, CT)]                // Query[(C, CT)]
  .to[List]                      // DBIO[List[(C, CT)]]
  .readOnly(connector)                // IO[List[(C, CT)]]
  .unsafeRunSync()               // List[(C, CT)]
  .foreach { case (city, country) => 
    println(s"City: ${city.name}, Country: ${country.name}")
  }
```

## Getting a Single Result (Option Type)

If you want to get a single result or an optional result (0 or 1 record) instead of a list, you can use `.to[Option]`:

```scala
case class User(id: Long, name: String, email: String)

// Searching for a single user by ID
sql"SELECT id, name, email FROM user WHERE id = ${userId}"
  .query[User]                   // Query[User]
  .to[Option]                    // DBIO[Option[User]]
  .readOnly(connector)                // IO[Option[User]]
  .unsafeRunSync()               // Option[User]
  .foreach(user => println(s"Found user: ${user.name}"))
```

If no result is found, `None` is returned, and if one is found, `Some(User(...))` is returned.

## Choosing Query Execution Methods

ldbc provides different query execution methods depending on the purpose:

- `.readOnly(connector)` - Used for read-only operations (such as SELECT statements)
- `.commit(connector)` - Executes write operations in auto-commit mode
- `.rollback(connector)` - Executes write operations and always rolls back (for testing)
- `.transaction(connector)` - Executes operations within a transaction and commits only on success

```scala
// Example of a read-only operation
sql"SELECT * FROM users"
  .query[User]
  .to[List]
  .readOnly(connector)

// Example of a write operation (auto-commit)
sql"UPDATE users SET name = ${newName} WHERE id = ${userId}"
  .update
  .commit(connector)

// Multiple operations in a transaction
(for {
  userId <- sql"INSERT INTO users (name, email) VALUES (${name}, ${email})".returning[Long]
  _      <- sql"INSERT INTO user_roles (user_id, role_id) VALUES (${userId}, ${roleId})".update
} yield userId).transaction(connector)
```

## Combining Collection Operations with Queries

By applying Scala collection operations to retrieved data, you can concisely describe more complex data processing:

```scala
// Example of grouping users
sql"SELECT id, name, department FROM employees"
  .query[(Long, String, String)] // ID, name, department
  .to[List]
  .readOnly(connector)
  .unsafeRunSync()
  .groupBy(_._3) // Group by department
  .map { case (department, employees) => 
    (department, employees.map(_._2)) // Map to department name and list of employee names
  }
  .foreach { case (department, names) =>
    println(s"Department: $department, Employees: ${names.mkString(", ")}")
  }
```

## Efficient Processing of Large Data with Streaming

When processing large amounts of data, loading all data into memory at once can cause memory exhaustion. ldbc allows you to process data efficiently using **streaming**.

### Basic Usage of Streaming

Streaming allows you to fetch and process data incrementally, significantly reducing memory usage:

```scala
import fs2.Stream
import cats.effect.*

// Basic streaming
val cityStream: Stream[DBIO, String] = 
  sql"SELECT name FROM city"
    .query[String]
    .stream                    // Stream[DBIO, String]

// Create Connector
val connector = Connector.fromDataSource(datasource)

// Fetch and process only the first 5 records
val firstFiveCities: IO[List[String]] = 
  cityStream
    .take(5)                   // Only the first 5 records
    .compile.toList            // Convert Stream to List
    .readOnly(connector)       // IO[List[String]]
```

### Specifying Fetch Size

You can control the number of rows fetched at once using the `stream(fetchSize: Int)` method:

```scala
// Fetch 10 rows at a time
val efficientStream: Stream[DBIO, String] = 
  sql"SELECT name FROM city"
    .query[String]
    .stream(10)                // fetchSize = 10

// Efficiently process large data
val processLargeData: IO[Int] = 
  sql"SELECT id, name, population FROM city"
    .query[(Long, String, Int)]
    .stream(100)               // Fetch 100 rows at a time
    .filter(_._3 > 1000000)    // Cities with population over 1 million
    .map(_._2)                 // Extract only city names
    .compile.toList
    .readOnly(connector)
    .map(_.size)
```

### Practical Data Processing with Streaming

Since streaming returns Fs2 `Stream`, you can use rich functional operations:

```scala
// Process large user data incrementally
// Create Connector
val connector = Connector.fromDataSource(datasource)

val processUsers: IO[Unit] = 
  sql"SELECT id, name, email, created_at FROM users"
    .query[(Long, String, String, java.time.LocalDateTime)]
    .stream(50)                // Fetch 50 rows at a time
    .filter(_._4.isAfter(lastWeek))  // Users created since last week
    .map { case (id, name, email, _) => 
      s"New user: $name ($email)"
    }
    .evalMap(IO.println)       // Output results sequentially
    .compile.drain             // Execute the stream
    .readOnly(connector)
```

### Optimizing Behavior with UseCursorFetch

In MySQL, the efficiency of streaming changes significantly depending on the `UseCursorFetch` setting:

```scala
// UseCursorFetch=true (recommended) - True streaming
val efficientProvider = MySQLDataSource
  .build[IO](host, port, user)
  .setPassword(password)
  .setDatabase(database)
  .setUseCursorFetch(true)    // Enable server-side cursors
  .setSSL(SSL.None)

// UseCursorFetch=false (default) - Limited streaming
val standardProvider = MySQLDataSource
  .build[IO](host, port, user)
  .setPassword(password)
  .setDatabase(database)
  .setSSL(SSL.None)
```

**When UseCursorFetch=true:**
- Uses server-side cursors to fetch data incrementally as needed
- Significantly reduces memory usage (safe even with millions of rows)
- Enables true streaming processing

**When UseCursorFetch=false:**
- Loads all results into memory when executing the query
- Fast for small datasets but risky for large data
- Limited effectiveness of streaming

### Example of Large Data Processing

Here's an example of safely processing one million rows:

```scala
// Efficient large data processing
val datasource = MySQLDataSource
  .build[IO](host, port, user)
  .setPassword(password)
  .setDatabase(database)
  .setUseCursorFetch(true)   // Important: Enable server-side cursors

// Create Connector
val connector = Connector.fromDataSource(datasource)

val processMillionRecords: IO[Long] = 
  sql"SELECT id, amount FROM transactions WHERE year = 2024"
    .query[(Long, BigDecimal)]
    .stream(1000)          // Process 1000 rows at a time
    .filter(_._2 > 100)    // Transactions over 100 yen only
    .map(_._2)             // Extract amounts only
    .fold(BigDecimal(0))(_ + _)  // Calculate sum
    .compile.lastOrError   // Get final result
    .readOnly(connector)
```

### Benefits of Streaming

1. **Memory Efficiency**: Keep memory usage constant even with large data
2. **Early Processing**: Process data while receiving it simultaneously
3. **Interruptible**: Stop processing midway based on conditions
4. **Functional Operations**: Rich operations like `filter`, `map`, `take`

```scala
// Example of early termination based on conditions
// Create Connector
val connector = Connector.fromDataSource(datasource)

val findFirstLargeCity: IO[Option[String]] = 
  sql"SELECT name, population FROM city ORDER BY population DESC"
    .query[(String, Int)]
    .stream(10)
    .find(_._2 > 5000000)      // First city with population over 5 million
    .map(_.map(_._1))          // Extract only city name
    .compile.last
    .readOnly(connector)
```

## How Mapping Works in Detail

### What is a Decoder?

In ldbc, a `Decoder` is a crucial component responsible for converting from `ResultSet` to Scala types. Decoders have the following characteristics:

1. **Type Safety**: Verify type consistency at compile time
2. **Composable**: Create Decoders for complex structures by combining smaller Decoders
3. **Auto-derivation**: Often generated automatically without explicit definitions

### Basic Decoder Operations

```scala
// Basic type Decoders (implicitly provided)
val longDecoder: Decoder[Long] = Decoder.long
val stringDecoder: Decoder[String] = Decoder.string

// Composing multiple Decoders
val tupleDecoder: Decoder[(Long, String)] = 
  longDecoder *: stringDecoder

// Converting to case class
case class User(id: Long, name: String)
val userDecoder: Decoder[User] = tupleDecoder.to[User]
```

### Reading by Column Number

When Decoders read values from a `ResultSet`, they use column numbers (starting from 1):

```scala
// How decode(columnIndex, resultSet) method works
decoder.decode(1, resultSet) // Read the 1st column
decoder.decode(2, resultSet) // Read the 2nd column
```

### Error Handling

During the decoding process, the following types of errors may occur:

- **Type mismatch**: SQL type and Scala type are incompatible
- **NULL values**: Mapping to types that don't allow NULL
- **Column count mismatch**: Expected column count differs from actual

These errors are represented as `Either[Decoder.Error, A]` and appropriate error messages are provided at runtime.

## Summary

ldbc provides features for retrieving data from databases in a type-safe and intuitive manner. In this tutorial, we covered:

- The basic workflow for data retrieval
- Single-column and multi-column queries
- Mapping to case classes and **how the internal mechanism works**
- **Type-safe conversion processing with Decoders**
- Joining multiple tables and nested data structures
- Getting single and multiple results
- **Efficient processing of large data with streaming**
- Various execution methods

By understanding how mapping works through Decoder composition, you can safely handle even more complex data structures. Additionally, by leveraging streaming capabilities, you can process large amounts of data efficiently in terms of memory usage. When dealing with large datasets, consider setting `UseCursorFetch=true`.

## Next Steps

Now that you understand how to retrieve data from databases in various formats, you've seen how type-safe mapping allows you to map database results directly to Scala data structures.

Next, let's move on to [Updating Data](/en/tutorial/Updating-Data.md) to learn how to insert, update, and delete data.
