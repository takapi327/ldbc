/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

import sbt.*

import laika.ast.Path.Root
import laika.config.{ Version, Versions }
import laika.helium.config.*

import org.typelevel.sbt.TypelevelSitePlugin.autoImport.*

object LaikaSettings {

  object versions {

    private def version(version: String, label: String = "EOL"): Version = {
      val (pathSegment, canonical) = version match {
        case LdbcVersions.latest => ("latest", true)
        case _     => (version, false)
      }

      val v =
        Version(version, pathSegment)
          .withFallbackLink("/index.html")
          .withLabel(label)
      if (canonical) v.setCanonical else v
    }

    val v03:     Version      = version("0.3", "Stable")
    val current: Version      = v03
    val all:     Seq[Version] = Seq(v03)

    val config: Versions = Versions
      .forCurrentVersion(current)
      .withOlderVersions(all.dropWhile(_ != current).drop(1) *)
      .withNewerVersions(all.takeWhile(_ != current) *)
  }

  private object paths {
    val apiLink = "https://javadoc.io/doc/io.github.takapi327/ldbc-dsl_3/latest/index.html"
  }

  val helium = Def.setting(
    tlSiteHelium.value.site
      .internalCSS(Root / "css" / "site.css")
      .site
      .topNavigationBar(
        navLinks = Seq(
          IconLink.external(paths.apiLink, HeliumIcon.api)
        ),
        versionMenu = VersionMenu.create(
          "Version",
          "Choose Version",
          additionalLinks = Seq(TextLink.internal(Root / "olderVersions" / "index.md", "Older Versions"))
        )
      )
      .site
      .versions(versions.config)
      .build
  )
}
