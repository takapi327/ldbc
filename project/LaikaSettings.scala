/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import laika.ast.Path.Root
import laika.theme.ThemeProvider
import laika.config.{ Version, Versions }
import laika.helium.config.*

import org. typelevel. sbt. TypelevelSitePlugin. autoImport.*

object LaikaSettings {

  object versions {

    val latestRelease = "0.3.0"

    private def version(version: String, label: String = "EOL"): Version = {
      val (pathSegment, canonical) = version match {
        case "0.3" => ("latest", true)
        case _     => (version, false)
      }

      val v =
        Version(version, pathSegment)
          .withFallbackLink("/index.html")
          .withLabel(label)
      if (canonical) v.setCanonical else v
    }

    val v03: Version      = version("0.3", "Dev")
    val current: Version = v03
    val all: Seq[Version]     = Seq(v03)

    val config: Versions = Versions
      .forCurrentVersion(current)
      .withOlderVersions(all.dropWhile(_ != current).drop(1) *)
      .withNewerVersions(all.takeWhile(_ != current) *)
  }

  import sbt.*
  val helium = Def.setting(
    tlSiteHelium.value
      .site.internalCSS(Root / "css" / "site.css")
      .site.topNavigationBar(
        versionMenu = VersionMenu.create(
          "Version",
          "Choose Version",
          additionalLinks = Seq(TextLink.internal(Root / "olderVersions" / "index.md", "Older Versions"))
        )
      )
      .site.versions(versions.config)
      .build
  )
}
