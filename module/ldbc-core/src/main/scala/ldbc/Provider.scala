/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.effect.*

import ldbc.sql.logging.LogHandler
import ldbc.sql.Connection

/**
 * Trait for aggregating settings for building connections and providing connections to users.
 *
 * @tparam F
 *   the effect type
 */
trait Provider[F[_]]:

  /**
   *  Handler for outputting logs of process execution using connections.
   */
  def logHandler: Option[LogHandler[F]]

  /**
   * Allocates a resource and supplies it to the given function. The resource is released as soon as the resulting F[A] is completed, whether normally or as a raised error.
   *
   * @param f
   *   the function to apply to the allocated resource
   * @tparam A
   *   the result of applying [F] to
   */
  def use[A](f: Connector[F] => F[A]): F[A]

  /**
   * Create a connection managed by Resource.
   *
   * {{{
   *   provider.createConnection().use { connection =>
   *     ???
   *   }
   * }}}
   */
  def createConnection(): Resource[F, Connection[F]]

  /**
   * Create a connector managed by Resource.
   *
   * {{{
   *   provider.createConnector().use { connector =>
   *     ???
   *   }
   * }}}
   */
  def createConnector(): Resource[F, Connector[F]]
