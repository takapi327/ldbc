/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl

import ldbc.core.{DataTypes, Alias as CoreAlias}

trait Alias extends CoreAlias, DataTypes:

  type Table[P <: Product] = ldbc.core.Table[P]
  val Table: ldbc.core.Table.type = ldbc.core.Table

  type DataType[T] = ldbc.core.DataType[T]
  val DataType: ldbc.core.DataType.type = ldbc.core.DataType
  
  type DataSource[F[_]] = ldbc.sql.DataSource[F]
  val DataSource: ldbc.dsl.DataSource.type = ldbc.dsl.DataSource

  type Connection[F[_]] = ldbc.sql.Connection[F]
  type Statement[F[_]] = ldbc.sql.Statement[F]
  type PreparedStatement[F[_]] = ldbc.sql.PreparedStatement[F]
  type ResultSet[F[_]] = ldbc.sql.ResultSet[F]
  type ResultSetMetaData[F[_]] = ldbc.sql.ResultSetMetaData[F]
