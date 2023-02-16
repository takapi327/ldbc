/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql

import cats.effect.Sync

/**
 * An object that is used to identify a generic SQL type, called a JDBC type or a vendor specific data type.
 *
 * @tparam F
 * The effect type
 */
trait SQLType[F[_]]:

  /**
   * Returns the SQLType name that represents a SQL data type.
   *
   * @return
   * The name of this SQLType.
   */
  def getName(): F[String]

  /**
   * Returns the name of the vendor that supports this data type. The value
   * returned typically is the package name for this vendor.
   *
   * @return
   * The name of the vendor for this data type
   */
  def getVendor(): F[String]

  /**
   * Returns the vendor specific type number for the data type.
   *
   * @return
   * An Integer representing the vendor specific data type
   */
  def getVendorTypeNumber(): F[Int]

object SQLType:

  def apply[F[_]: Sync](sqlType: java.sql.SQLType): SQLType[F] = new SQLType[F]:

    override def getName(): F[String] = Sync[F].blocking(sqlType.getName)

    override def getVendor(): F[String] = Sync[F].blocking(sqlType.getVendor)

    override def getVendorTypeNumber(): F[Int] = Sync[F].blocking(sqlType.getVendorTypeNumber)
