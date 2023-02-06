/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import ScalaVersions._
import JavaVersions._
import BuildSettings._

lazy val ldbc = project.in(file("."))
  .settings(scalaVersion := scala3)
  .settings(publish / skip := true)
  .settings(commonSettings: _*)
