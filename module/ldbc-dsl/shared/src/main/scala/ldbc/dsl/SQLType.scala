/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import ldbc.sql.SQLType

object SQLType:

  def apply(sqlType: java.sql.SQLType): SQLType = new SQLType:

    override def getName(): String = sqlType.getName

    override def getVendor(): String = sqlType.getVendor

    override def getVendorTypeNumber(): Int = sqlType.getVendorTypeNumber
