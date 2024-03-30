/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

lazy val root = (project in file("."))
  .settings(
    name                 := "ldbc-plugin-example",
    scalaVersion         := sys.props.get("scala.version").getOrElse("3.3.0"),
    version              := "0.1",
    run / fork           := true,
    Compile / parseFiles := List(baseDirectory.value / "test.sql"),
    Compile / customYamlFiles := List(
      baseDirectory.value / "custom.yml"
    )
  )
  .enablePlugins(Ldbc)
