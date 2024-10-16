/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

addSbtPlugin("com.github.sbt"     % "sbt-site-paradox"                            % "1.7.0")
addSbtPlugin("com.github.sbt"     % "sbt-ghpages"                                 % "0.8.0")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"                                    % "2.6.1")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"                                     % "0.4.7")
addSbtPlugin("org.typelevel"      % "sbt-typelevel"                               % "0.7.4")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                                 % "1.17.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"                            % "0.4.17")
addSbtPlugin("com.armanbilge"     % "sbt-scala-native-config-brew-github-actions" % "0.3.0")

// TODO: Remove this line when the following issue is resolved:
// https://github.com/typelevel/sbt-typelevel/issues/750
addSbtPlugin("org.typelevel" % "sbt-typelevel-github-actions" % "0.7.4")
