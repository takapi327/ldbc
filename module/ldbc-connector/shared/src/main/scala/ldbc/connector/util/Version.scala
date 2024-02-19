/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import scala.util.Try

case class Version(
  major: Int,
  minor: Int,
  patch: Int
):

  override def toString: String = s"$major.$minor.$patch"

  def isSameSeries(that: Version): Boolean =
    this.major == that.major && this.minor == that.minor

  def compare(that: Version): Int =
    val x = this.major.compare(that.major)
    if x != 0 then return x
    val y = this.minor.compare(that.minor)
    if y != 0 then return y
    val z = this.patch.compare(that.patch)
    if z != 0 then return z
    0

object Version:

  private val versionRegex = """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)?$""".r

  def apply(version: String): Option[Version] = Version.unapply(version)

  def unapply(version: String): Option[Version] = version match
    case versionRegex(major, minor, patch) =>
      Try(Version(major.toInt, minor.toInt, patch.toInt)).toOption
    case _ => None
