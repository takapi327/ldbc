/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mcp

import scala.scalajs.js
import scala.scalajs.js.annotation.*

// Import Node.js path module
@js.native
@JSImport("path", JSImport.Namespace)
object NodePath extends js.Object:
  def resolve(paths: String*): String = js.native

// Accessing Node.js process objects
@js.native
@JSGlobal("process")
object Process extends js.Object:
  val cwd: js.Function0[String] = js.native

object PathUtils:
  // How to get __dirname (ES module requires a different method)
  // Different module types have different implementations
  private def getCurrentDir: String =
    // For the CommonJS module
    try js.Dynamic.global.__dirname.asInstanceOf[String]
    catch
      // Fallback to ES module or cwd if __dirname is not available
      case _: Throwable => Process.cwd()

  def fromPackageRoot(relative: String): String =
    val currentDir = getCurrentDir
    // Move up one level from the current directory and merge relative
    NodePath.resolve(currentDir, "..", relative)
