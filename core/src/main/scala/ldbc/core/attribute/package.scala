/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

package object attribute:

  private[ldbc] trait Attribute[T]:

    /** Define an SQL query string for each attribute.
      *
      * @return
      *   SQL query string
      */
    def queryString: String

  private[ldbc] case class Comment[T](message: String) extends Attribute[T]:

    override def queryString: String = s"COMMENT '$message'"
