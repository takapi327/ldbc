/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.schemaspy.result

/**
 * Enum representing the status at the time of SchemaSPY execution.
 */
enum Status(val code: Int):
  case Success extends Status(0)
  case Failure extends Status(1)
  case EmptySchema extends Status(2)
  case ConnectionFailure extends Status(3)
  case InvalidConfig extends Status(4)
