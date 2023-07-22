/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

/**
 * Model for storing commented out strings in SQL files.
 *
 * @param message
 *   Commented out string
 */
case class CommentOut(message: String)

/**
 * A model for storing the values of comment attributes to be set on columns and tables.
 *
 * @param message
 *   Comments to be set on columns and tables
 */
case class CommentSet(message: String)
