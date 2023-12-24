/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
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
