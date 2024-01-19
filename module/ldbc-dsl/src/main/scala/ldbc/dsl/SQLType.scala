/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.dsl

import cats.effect.Sync

import ldbc.sql.SQLType

object SQLType:

  def apply[F[_]: Sync](sqlType: java.sql.SQLType): SQLType[F] = new SQLType[F]:

    override def getName(): F[String] = Sync[F].blocking(sqlType.getName)

    override def getVendor(): F[String] = Sync[F].blocking(sqlType.getVendor)

    override def getVendorTypeNumber(): F[Int] = Sync[F].blocking(sqlType.getVendorTypeNumber)
