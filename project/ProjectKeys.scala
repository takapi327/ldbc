/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt.settingKey

object ProjectKeys {
  lazy val projectName = settingKey[String]("project name")
}
