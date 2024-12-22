{%
  laika.title = Setup
  laika.metadata.language = en
%}

# Setup

Welcome to the wonderful world of ldbc! In this section we will help you get everything set up.

## Database Setup

First, start the database using Docker. Use the following code to start the database

```yaml
services:
  mysql:
    image: mysql:@MYSQL_VERSION@
    container_name: ldbc
    environment:
      MYSQL_USER: 'ldbc'
      MYSQL_PASSWORD: 'password'
      MYSQL_ROOT_PASSWORD: 'root'
    ports:
      - 13306:3306
    volumes:
      - ./database:/docker-entrypoint-initdb.d
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost" ]
      timeout: 20s
      retries: 10
```

Next, initialize the database.

Create the database as shown in the code below.

```sql
CREATE DATABASE IF NOT EXISTS sandbox_db;
```

Next, tables are created.

```sql
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `product` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `price` DECIMAL(10, 2) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)

CREATE TABLE IF NOT EXISTS `order` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `product_id` INT NOT NULL,
  `order_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `quantity` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES `user` (id),
  FOREIGN KEY (product_id) REFERENCES `product` (id)
)
```

Insert data into each table.

```sql
INSERT INTO user (name, email) VALUES
  ('Alice', 'alice@example.com'),
  ('Bob', 'bob@example.com'),
  ('Charlie', 'charlie@example.com');

INSERT INTO product (name, price) VALUES
  ('Laptop', 999.99),
  ('Mouse', 19.99),
  ('Keyboard', 49.99),
  ('Monitor', 199.99);

INSERT INTO `order` (user_id, product_id, quantity) VALUES
  (1, 1, 1), -- Alice ordered 1 Laptop
  (1, 2, 2), -- Alice ordered 2 Mice
  (2, 3, 1), -- Bob ordered 1 Keyboard
  (3, 4, 1); -- Charlie ordered 1 Monitor
```

## Scala Setup

The tutorial will use [Scala CLI](https://scala-cli.virtuslab.org/). Therefore, you will need to install the Scala CLI.

```bash
brew install Virtuslab/scala-cli/scala-cli
```

**Execute with Scala CLI**

The database setup described earlier can be performed using the Scala CLI. The following commands can be used to perform this setup.

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

### First Program

To begin, create a new project with ldbc as a dependency.

```scala
//> using scala "@SCALA_VERSION@"
//> using dep "@ORGANIZATION@::ldbc-dsl:@VERSION@"
```

Before using ldbc, some symbols need to be imported. For convenience, we will use package import here. This will give us the symbols most commonly used when working with high-level APIs.

```scala
import ldbc.dsl.io.*
```

Let's bring Cats too.

```scala
import cats.syntax.all.*
import cats.effect.*
```

Next, tracers and log handlers are provided. These are used to log applications. Tracers are used to record application traces. The log handler is used to log the application.

The following code provides tracers and log handlers but does nothing with the entities.

```scala 3
given Tracer[IO]     = Tracer.noop[IO]
given LogHandler[IO] = LogHandler.noop[IO]
```

The most common type handled by the ldbc high-level API is of the form `Executor[F, A]`, which specifies a calculation to be performed in a context where `{java | ldbc}.sql.Connection` is available, ultimately producing a value of type A.

Let's start with an Executor program that only returns constants.

```scala
val program: Executor[IO, Int] = Executor.pure[IO, Int](1)
```

Next, create a connector to connect to the database. A connector is a resource for managing connections to a database. A connector provides resources to initiate a connection to the database, execute a query, and close the connection.

Here, ldbc uses a connector created by ldbc on its own. How to select and create a connector will be explained later.

```scala
def connection = Connection[IO](
  host     = "127.0.0.1",
  port     = 13306,
  user     = "ldbc",
  password = Some("password"),
  ssl      = SSL.Trusted
)
```

Executor is a data type that knows how to connect to a database, how to pass connections, and how to clean up connections, and this knowledge allows Executor to be converted to IO to obtain an executable program. Specifically, execution yields an IO that connects to the database and executes a single transaction.

```scala
connection
  .use { conn =>
    program.readOnly(conn).map(println(_))
  }
  .unsafeRunSync()
```

Hooray! I was able to calculate the constants. This is not very interesting, since we will not be asking the database to do the work, but the first step is complete.

> Remember that all the code in this book is pure except for the call to IO.unsafeRunSync. IO.unsafeRunSync is an “end of the world” operation that usually appears only at the entry point of an application. The REPL forces the calculation to to use this to make it “happen.”

**Execute with Scala CLI**

This program can also be run using the Scala CLI. The following command will execute this program.

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/01-Program.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```
