/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package example

import cats.effect.IO

import ldbc.dsl.*

import ldbc.Connector

trait UserRepository[F[_]]:
  def findAll():                           F[List[User]]
  def findById(id: Long):                  F[Option[User]]
  def create(name: String, email: String): F[Long]
  def delete(id:   Long):                  F[Unit]

class UserRepositoryImpl(connector: Connector[IO]) extends UserRepository[IO]:

  override def findAll(): IO[List[User]] =
    sql"SELECT id, name, email FROM users"
      .query[User]
      .to[List]
      .readOnly(connector)

  override def findById(id: Long): IO[Option[User]] =
    sql"SELECT id, name, email FROM users WHERE id = $id"
      .query[User]
      .to[Option]
      .readOnly(connector)

  override def create(name: String, email: String): IO[Long] =
    sql"INSERT INTO users (name, email) VALUES ($name, $email)"
      .returning[Long]
      .commit(connector)

  override def delete(id: Long): IO[Unit] =
    sql"DELETE FROM users WHERE id = $id".update
      .commit(connector)
      .void
