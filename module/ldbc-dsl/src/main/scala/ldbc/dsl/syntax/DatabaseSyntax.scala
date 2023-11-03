/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.syntax

import javax.sql.DataSource

import ldbc.core.Database as CoreDatabase
import ldbc.dsl.Database

/**
 * Trait to provide a way to convert from Database model to Database model with connection information to db.
 *
 * @tparam F
 *   The effect type
 */
trait DatabaseSyntax[F[_]]:

  extension (database: CoreDatabase)
    def fromDriverManager(
      databaseType: CoreDatabase.Type,
      user: Option[String] = None,
      password: Option[String] = None
    ): Database[F]

    def mysqlDriver: Database[F] = fromDriverManager(CoreDatabase.Type.MySQL)
    def mysqlDriver(user: String, password: String): Database[F] = fromDriverManager(CoreDatabase.Type.MySQL, Some(user), Some(password))

    def awsDriver: Database[F] = fromDriverManager(CoreDatabase.Type.AWSMySQL)
    def awsDriver(user: String, password: String): Database[F] = fromDriverManager(CoreDatabase.Type.AWSMySQL, Some(user), Some(password))

    def fromDataSource(
      databaseType: CoreDatabase.Type,
      dataSource: DataSource
    ): Database[F]

    def mysqlDataSource(dataSource: DataSource): Database[F] = fromDataSource(CoreDatabase.Type.MySQL, dataSource)

    def awsDataSource(dataSource: DataSource): Database[F] = fromDataSource(CoreDatabase.Type.AWSMySQL, dataSource)
