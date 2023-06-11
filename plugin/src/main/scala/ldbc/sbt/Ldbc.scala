/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package ldbc.sbt

import sbt._

object Ldbc extends AutoPlugin {

  val autoImport = AutoImport

  override def projectSettings = Settings.projectSettings
}
