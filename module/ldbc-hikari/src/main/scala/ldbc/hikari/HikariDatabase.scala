/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.hikari

import com.zaxxer.hikari.HikariConfig

import cats.effect.{ Resource, Sync }

import ldbc.core.Database as CoreDatabase
import ldbc.dsl.*

object HikariDatabase:

  /** Methods for building a Database using the HikariCP connection pool.
    *
    * @param hikariConfig
    *   User-generated HikariCP Config file
    * @tparam F
    *   the effect type.
    */
  def fromHikariConfig[F[_]: Sync](hikariConfig: HikariConfig): Resource[F, Database[F]] =
    val builder = new HikariDataSourceBuilder[F] {}
    builder
      .buildFromConfig(hikariConfig)
      .map(dataSource =>
        Database[F](
          CoreDatabase.Type.MySQL,
          hikariConfig.getUsername,
          hikariConfig.getJdbcUrl,
          None,
          () => DataSource[F](dataSource).getConnection
        )
      )
