/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.hikari

import cats.effect.*
import cats.effect.implicits.*

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }

/**
 * A model for building a database. HikariCP construction, thread pool generation for database connection, test
 * connection, etc. are performed via the method.
 *
 * @tparam F
 *   the effect type.
 */
trait HikariDataSourceBuilder[F[_]: Sync] extends HikariConfigBuilder:

  /**
   * Method for generating HikariDataSource with Resource.
   *
   * @param factory
   *   Process to generate HikariDataSource
   */
  private def createDataSourceResource(factory: => HikariDataSource): Resource[F, HikariDataSource] =
    Resource.fromAutoCloseable(Sync[F].delay(factory))

  /**
   * Method to generate Config for HikariCP.
   */
  private def buildConfig(): Resource[F, HikariConfig] =
    Sync[F].delay {
      val hikariConfig = build()
      hikariConfig.validate()
      hikariConfig
    }.toResource

  /**
   * Method to generate DataSource from HikariCPConfig generation.
   */
  def buildDataSource(): Resource[F, HikariDataSource] =
    for
      hikariConfig     <- buildConfig()
      hikariDataSource <- createDataSourceResource(new HikariDataSource(hikariConfig))
    yield hikariDataSource

  /**
   * Methods for generating DataSource from user-generated HikariCPConfig.
   *
   * @param hikariConfig
   *   User-generated HikariCP Config file
   */
  def buildFromConfig(hikariConfig: HikariConfig): Resource[F, HikariDataSource] =
    createDataSourceResource(new HikariDataSource(hikariConfig))

object HikariDataSourceBuilder:

  def default[F[_]: Sync]: HikariDataSourceBuilder[F] = new HikariDataSourceBuilder[F] {}
