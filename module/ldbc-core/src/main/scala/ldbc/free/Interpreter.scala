/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.free

import cats.~>
import cats.data.Kleisli

import ldbc.sql.*

trait Interpreter[F[_]]:

  def ConnectionInterpreter:        ConnectionOp ~> ([A] =>> Kleisli[F, Connection[F], A])
  def StatementInterpreter:         StatementOp ~> ([A] =>> Kleisli[F, Statement[F], A])
  def PreparedStatementInterpreter: PreparedStatementOp ~> ([A] =>> Kleisli[F, PreparedStatement[F], A])
  def ResultSetInterpreter:         ResultSetOp ~> ([A] =>> Kleisli[F, ResultSet[F], A])
