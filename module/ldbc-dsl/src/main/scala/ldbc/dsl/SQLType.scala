/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import cats.effect.Sync

import ldbc.sql.SQLType

object SQLType:

  def apply[F[_]: Sync](sqlType: java.sql.SQLType): SQLType[F] = new SQLType[F]:

    override def getName(): F[String] = Sync[F].blocking(sqlType.getName)

    override def getVendor(): F[String] = Sync[F].blocking(sqlType.getVendor)

    override def getVendorTypeNumber(): F[Int] = Sync[F].blocking(sqlType.getVendorTypeNumber)
