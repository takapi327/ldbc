/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import scala.sys.process.Process
import scala.util.Try

import sbt.*
import sbt.Keys.*

import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport.*

/**
 * Homebrew integration for the Scala Native builds.
 *
 * fs2-io declares `@link("s2n")` for its TLS implementation, so the Scala Native test binaries link
 * against s2n-tls. Homebrew installs it outside clang's default search path, which means the include
 * and library directories have to be passed explicitly.
 *
 * This replaces `sbt-scala-native-config-brew`, of which ldbc only used this single formula. The
 * upstream plugin parses `brew info --json` to expand a formula's runtime dependencies; `brew deps`
 * gives the same list without needing a JSON parser on the build classpath.
 */
object Brew {

  /** Formulae required by the Scala Native builds. Their dependencies are resolved by `brew deps`. */
  val formulas: Seq[String] = Seq("s2n")

  val linuxBin: String = "/home/linuxbrew/.linuxbrew/bin/brew"

  /**
   * Points the Scala Native test build at the Homebrew installation of [[formulas]].
   *
   * `LD_LIBRARY_PATH` is extended as well, because the test binary resolves s2n-tls dynamically at
   * run time. When Homebrew or a formula cannot be found the settings are left untouched and a
   * warning is logged, so that a checkout without them still loads and builds on the other platforms.
   */
  def nativeSettings: Seq[Setting[?]] = Seq(
    Test / nativeConfig := {
      val config = (Test / nativeConfig).value
      val dirs   = prefixes(streams.value.log)
      config
        .withCompileOptions(config.compileOptions ++ subDirs(dirs, "include").map(dir => s"-I$dir"))
        .withLinkingOptions(config.linkingOptions ++ subDirs(dirs, "lib").map(dir => s"-L$dir"))
    },
    Test / envVars := {
      val env = (Test / envVars).value
      subDirs(prefixes(streams.value.log), "lib") match {
        case Nil  => env
        case libs => env.updated("LD_LIBRARY_PATH", (env.get("LD_LIBRARY_PATH").toList ++ libs).mkString(":"))
      }
    }
  )

  private def bin: Option[String] = {
    val os = sys.props.getOrElse("os.name", "").toLowerCase
    if (os.contains("mac")) {
      val isArm = sys.props.getOrElse("os.arch", "").toLowerCase.contains("aarch64")
      Some(if (isArm) "/opt/homebrew/bin/brew" else "/usr/local/bin/brew")
    } else if (os.contains("linux")) Some(linuxBin)
    else None
  }

  private def run(brew: String, args: String*): Option[String] =
    Try(Process(brew +: args).!!.trim).toOption

  private def prefixes(log: Logger): List[File] = bin match {
    case None =>
      log.warn("Homebrew is not available on this platform, Scala Native options will not be auto-configured.")
      Nil
    case Some(brew) =>
      val withDeps = formulas.toList.flatMap { formula =>
        formula :: run(brew, "deps", formula).toList.flatMap(_.linesIterator.map(_.trim).filter(_.nonEmpty))
      }
      val dirs = withDeps.distinct.flatMap(run(brew, "--prefix", _)).map(file).filter(_.isDirectory)
      if (dirs.isEmpty) {
        log.warn(s"Cannot find brew-installed ${ formulas.mkString(", ") }.")
        log.warn("nativeConfig and LD_LIBRARY_PATH will not be auto-configured.")
      }
      dirs
  }

  private def subDirs(prefixes: List[File], name: String): List[String] =
    prefixes.map(_ / name).filter(_.isDirectory).map(_.toString)
}
