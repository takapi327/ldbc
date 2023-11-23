/**
 *  This file is part of the ldbc.
 *  For the full copyright and license information,
 *  please view the LICENSE file that was distributed with this source code.
 */

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("org.takapi327" % "ldbc-plugin" % x)
  case _       => sys.error(
    """|The system property 'plugin.version' is not defined.
       |Specify this property using the scriptedLaunchOpts -D.
       |""".stripMargin
  )
}
