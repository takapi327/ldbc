/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sbt

import sbt._

object Ldbc extends AutoPlugin {

  val autoImport = AutoImport

  override def projectSettings = Settings.projectSettings
}
