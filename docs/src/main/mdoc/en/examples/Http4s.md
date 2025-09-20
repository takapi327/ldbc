{%
  laika.title = Http4s
  laika.metadata.language = en
%}

# Integration of Http4s and ldbc

This guide explains how to build a web application by combining [Http4s](https://http4s.org/) and ldbc.

## Introduction

Http4s is a pure functional HTTP server and client library for Scala. By combining it with ldbc,
you can build web applications with type-safe database access.

## Adding Dependencies

First, you need to add the following dependencies to your build.sbt:

```scala
libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-dsl"          % "0.23.30",
  "org.http4s"    %% "http4s-ember-server" % "0.23.30",
  "org.http4s"    %% "http4s-circe"        % "0.23.30",
  "io.circe"      %% "circe-generic"       % "0.14.10"
)
```

## Basic Usage

### 1. Table Definition

First, create models and table definitions corresponding to your database tables:

```scala
// Model definition
case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
)

// JSON encoder definition
object City:
  given Encoder[City] = Encoder.derived[City]

// Table definition
class CityTable extends Table[City]("city"):
  // Set column naming convention (optional)
  given Naming = Naming.PASCAL

  // Column definitions
  def id:          Column[Int]    = int("ID").unsigned.autoIncrement.primaryKey
  def name:        Column[String] = char(35)
  def countryCode: Column[String] = char(3).unique
  def district:    Column[String] = char(20)
  def population:  Column[Int]    = int()

  // Mapping definition
  override def * : Column[City] = 
    (id *: name *: countryCode *: district *: population).to[City]

val cityTable = TableQuery[CityTable]
```

### 2. Database Connection Configuration

Configure the database connection:

```scala
private def datasource =
  MySQLDataSource
    .build[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
    .setDatabase("world")
    .setSSL(SSL.Trusted)
```

Main options available in connection configuration:
- Hostname
- Port number
- Database name
- Username
- Password
- SSL settings (Trusted, Verify, etc.)

### 3. HTTP Route Definition

Define Http4s routes and incorporate ldbc queries:

```scala
private def routes(connector: Connector[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "cities" =>
    for
      cities <- cityTable.selectAll.query.to[List].readOnly(connector)
      result <- Ok(cities.asJson)
    yield result
}
```

### 4. Server Startup

Finally, start the Http4s server:

```scala
object Main extends ResourceApp.Forever:
  override def run(args: List[String]): Resource[IO, Unit] =
    // Create Connector
    val connector = Connector.fromDataSource(datasource)
    
    EmberServerBuilder
      .default[IO]
      .withHttpApp(routes(connector).orNotFound)
      .build
      .void
```

## Advanced Examples

### Adding Custom Queries

Example of implementing searches with specific conditions or complex queries:

```scala
private def routes(connector: Connector[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "cities" / "search" / name =>
    for
      cities <- cityTable.filter(_.name === name).query.to[List].readOnly(connector)
      result <- Ok(cities.asJson)
    yield result
      
  case GET -> Root / "cities" / "population" / IntVar(minPopulation) =>
    for
      cities <- cityTable.filter(_.population >= minPopulation).query.to[List].readOnly(connector)
      result <- Ok(cities.asJson)
    yield result
}
```

## Error Handling

Example of properly handling database errors:

```scala
private def handleDatabaseError[A](action: IO[A]): IO[Response[IO]] =
  action.attempt.flatMap {
    case Right(value) => Ok(value.asJson)
    case Left(error) => 
      InternalServerError(s"Database error: ${error.getMessage}")
  }

private def routes(connector: Connector[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
  case GET -> Root / "cities" =>
    handleDatabaseError {
      cityTable.selectAll.query.to[List].readOnly(connector)
    }
}
```

## Summary

Combining Http4s and ldbc offers the following advantages:
- Type-safe database access
- Benefits of pure functional programming
- Flexible routing and error handling

In actual application development, you can combine these basic patterns to implement more complex functionality.
