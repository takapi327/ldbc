/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

@main def setup(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #setupDatabase
  val createDatabase: DBIO[IO, Int] =
    sql"CREATE DATABASE IF NOT EXISTS sandbox_db".update
  // #setupDatabase

  // #setupUser
  val createUser: DBIO[IO, Int] =
    sql"""
      CREATE TABLE IF NOT EXISTS `user` (
        `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        `name` VARCHAR(50) NOT NULL,
        `email` VARCHAR(100) NOT NULL,
        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
      )
    """.update
  // #setupUser

  // #setupProduct
  val createProduct: DBIO[IO, Int] =
    sql"""
      CREATE TABLE IF NOT EXISTS `product` (
        `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        `name` VARCHAR(100) NOT NULL,
        `price` DECIMAL(10, 2) NOT NULL,
        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
      )
    """.update
  // #setupProduct

  // #setupOrder
  val createOrder: DBIO[IO, Int] =
    sql"""
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
    """.update
  // #setupOrder

  // #insertUser
  val insertUser: DBIO[IO, Int] =
    sql"""
      INSERT INTO user (name, email) VALUES
       ('Alice', 'alice@example.com'),
       ('Bob', 'bob@example.com'),
       ('Charlie', 'charlie@example.com')
    """.update
  // #insertUser

  // #insertProduct
  val insertProduct: DBIO[IO, Int] =
    sql"""
      INSERT INTO product (name, price) VALUES
      ('Laptop', 999.99),
      ('Mouse', 19.99),
      ('Keyboard', 49.99),
      ('Monitor', 199.99)
    """.update
  // #insertProduct

  // #insertOrder
  val insertOrder: DBIO[IO, Int] =
    sql"""
      INSERT INTO `order` (user_id, product_id, quantity) VALUES
      (1, 1, 1), -- Alice ordered 1 Laptop
      (1, 2, 2), -- Alice ordered 2 Mice
      (2, 3, 1), -- Bob ordered 1 Keyboard
      (3, 4, 1) -- Charlie ordered 1 Monitor
    """.update
  // #insertOrder

  // #connection
  def connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password")
  )
  // #connection

  // #setupTable
  val setUpTables =
    createUser *> createProduct *> createOrder
  // #setupTable

  // #insertData
  val insertData =
    insertUser *> insertProduct *> insertOrder
  // #insertData

  // #run
  connection
    .use { conn =>
      createDatabase.commit(conn) *>
        conn.setCatalog("sandbox_db") *>
        (setUpTables *> insertData)
          .transaction(conn)
          .as(println("Database setup completed"))
    }
    .unsafeRunSync()
  // #run
