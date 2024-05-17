/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * An object that is used to identify a generic SQL type, called a JDBC type or
 * a vendor specific data type.
 */
trait SQLType:

  /**
   * Returns the {@code SQLType} name that represents a SQL data type.
   *
   * @return The name of this {@code SQLType}.
   */
  def getName(): String

  /**
   * Returns the name of the vendor that supports this data type. The value
   * returned typically is the package name for this vendor.
   *
   * @return The name of the vendor for this data type
   */
  def getVendor(): String

  /**
   * Returns the vendor specific type number for the data type.
   *
   * @return An Integer representing the vendor specific data type
   */
  def getVendorTypeNumber(): Int
