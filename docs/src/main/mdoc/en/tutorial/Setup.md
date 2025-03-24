{%
  laika.title = Setup
  laika.metadata.language = en
%}

# Setup

Welcome to the first step in getting started with ldbc! This page explains how to prepare your development environment and database.

## Requirements

- JDK 21 or higher
- Scala 3
- Docker (for database environment)
- [Scala CLI](https://scala-cli.virtuslab.org/) (recommended)

## Database Setup

First, start a MySQL database using Docker. Create the following docker-compose.yml file:

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

Next, create the following SQL file in the `database` directory to set up initial data:

```sql
-- 01-create-database.sql
CREATE DATABASE IF NOT EXISTS sandbox_db;
USE sandbox_db;

-- Create tables
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
);

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
);

-- Insert initial data
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

Start the database using Docker Compose:

```bash
docker compose up -d
```

## Setting up a Scala Project

This tutorial uses [Scala CLI](https://scala-cli.virtuslab.org/) to get started easily. If you haven't installed it yet, you can do so with the following command:

```bash
# For macOS
brew install Virtuslab/scala-cli/scala-cli

# For other OS, please refer to Scala CLI's official website
```

### Your First ldbc Project

Create a new directory and set up your first ldbc project:

```bash
mkdir ldbc-tutorial
cd ldbc-tutorial
touch FirstSteps.scala
```

Add the following code to `FirstSteps.scala`:

```scala
//> using scala "@SCALA_VERSION@"
//> using dep "@ORGANIZATION@::ldbc-dsl:@VERSION@"
//> using dep "@ORGANIZATION@::ldbc-connector:@VERSION@"

import cats.effect.*
import cats.syntax.all.*
import ldbc.connector.*
import ldbc.dsl.*

object FirstSteps extends IOApp.Simple:

  // A program that returns a simple constant
  val simpleProgram: DBIO[Int] = DBIO.pure(0)
  
  // Database connection configuration
  val provider =
    ConnectionProvider
      .default[IO]("127.0.0.1", 13306, "ldbc", "password", "sandbox_db")
      .setSSL(SSL.Trusted)
  
  def run: IO[Unit] =
    // Execute the program
    provider.use { conn =>
      simpleProgram.readOnly(conn).flatMap { result =>
        IO.println(s"Value retrieved from database: $result")
      }
    }
```

Run the program using Scala CLI:

```bash
scala-cli FirstSteps.scala
```

If you see "Value retrieved from database: 42", you've succeeded! This doesn't actually query the database yet, but it confirms that you have the basic ldbc structure and connection setup working.

## Automatic Setup Script (Optional)

We also provide a Scala CLI script that automatically handles all the setup:

```bash
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala --dependency io.github.takapi327::ldbc-dsl:@VERSION@ --dependency io.github.takapi327::ldbc-connector:@VERSION@
```

## Next Steps

You're now ready to use ldbc! Proceed to [Connection](/en/tutorial/Connection.md) to learn more about database connections.
