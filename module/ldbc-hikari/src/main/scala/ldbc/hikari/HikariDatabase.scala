/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.hikari

import com.zaxxer.hikari.HikariConfig

import cats.effect.{ Resource, Sync }
import cats.implicits.*

import ldbc.core.Database as CoreDatabase
import ldbc.dsl.*
import ldbc.sql.Connection

object HikariDatabase:

  /**
   * Methods for building a Database using the HikariCP connection pool.
   * 
   * @param hikariConfig
   *   User-generated HikariCP Config file
   * @tparam F
   *   the effect type.
   */
  def fromHikariConfig[F[_]: Sync](hikariConfig: HikariConfig): Resource[F, Database[F]] =
    val builder = new HikariDataSourceBuilder[F] {}
    builder.buildFromConfig(hikariConfig).map(dataSource =>
      val connection: F[Connection[F]] = Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
      Database[F](
        CoreDatabase.Type.MySQL,
        hikariConfig.getUsername,
        hikariConfig.getJdbcUrl,
        None,
        () => connection
      )
    )
