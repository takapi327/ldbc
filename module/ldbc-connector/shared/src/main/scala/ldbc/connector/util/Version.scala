/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import scala.util.Try

/**
 * A version number is a sequence of non-negative integers separated by dots.
 * It is used to represent the version of a software product.
 * A version number has three parts: major, minor, and patch.
 * 
 * The version number must be a non-negative integer.
 * 
 * @param major
 * The major version number is incremented when there are significant changes to the software.
 * @param minor
 * The minor version number is incremented when there are minor changes to the software.
 * @param patch
 * The patch version number is incremented when there are bug fixes to the software.
 */
case class Version(
  major: Int,
  minor: Int,
  patch: Int
):

  /**
   * This version is compared to the specified version to determine if they are the same series.
   * @param that
   * Version to be compared
   * @return
   * Return true if major version and minor version are the same.
   */
  def isSameSeries(that: Version): Boolean =
    this.major == that.major && this.minor == that.minor

  /**
   * Compare versions and determine if the version being compared is higher or lower.
   * @param that
   * Version to be compared
   * @return
   * Return 1 if the version is above the compared version, -1 if below, and 0 if equal.
   */
  def compare(that: Version): Int =
    val x = this.major.compare(that.major)
    if x != 0 then return x
    val y = this.minor.compare(that.minor)
    if y != 0 then return y
    val z = this.patch.compare(that.patch)
    if z != 0 then return z
    -1

  override def toString: String = s"$major.$minor.$patch"

object Version:

  // The version number must be in the format of "major.minor.patch".
  private val versionRegex = """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)?$""".r

  def apply(version: String): Option[Version] = Version.unapply(version)

  def unapply(version: String): Option[Version] = version match
    case versionRegex(major, minor, patch) =>
      Try(Version(major.toInt, minor.toInt, patch.toInt)).toOption
    case _ => None
