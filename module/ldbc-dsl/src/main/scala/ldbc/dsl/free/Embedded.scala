/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.free

import cats.free.Free

/**
 * This code is based on doobie's code.
 *
 * @see https://github.com/typelevel/doobie/blob/main/modules/free/src/main/scala/doobie/free/embedded.scala
 */
sealed trait Embedded[A]
object Embedded:
  final case class Connection[F[_], A](c: ldbc.sql.Connection[F], ci: ConnectionIO[A]) extends Embedded[A]
  final case class PreparedStatement[F[_], A](p: ldbc.sql.PreparedStatement[F], pi: PreparedStatementIO[A]) extends Embedded[A]
  final case class ResultSet[F[_], A](r: ldbc.sql.ResultSet[F], ri: ResultSetIO[A]) extends Embedded[A]

trait Embeddable[F[_], J]:
  def embed[A](j: J, fa: Free[F, A]): Embedded[A]
