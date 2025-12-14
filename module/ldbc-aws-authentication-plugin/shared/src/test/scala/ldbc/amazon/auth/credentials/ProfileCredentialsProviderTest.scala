/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.time.Instant

import cats.effect.std.SystemProperties
import cats.effect.IO

import fs2.io.file.Files

import munit.CatsEffectSuite

import ProfileCredentialsProvider.*

class ProfileCredentialsProviderTest extends CatsEffectSuite:

  // Test fixtures
  private val testHomeDir = "/home/test"

  // Mock system properties
  private def mockSystemProperties(homeDir: Option[String] = Some(testHomeDir)): SystemProperties[IO] =
    new SystemProperties[IO]:
      override def get(name: String): IO[Option[String]] =
        name match
          case "user.home" => IO.pure(homeDir)
          case _           => IO.pure(None)
      override def clear(key: String): IO[Option[String]] =
        IO.raiseError(new UnsupportedOperationException("clear not supported in mock"))
      override def set(key: String, value: String): IO[Option[String]] =
        IO.raiseError(new UnsupportedOperationException("set not supported in mock"))

  test("ProfileCredentialsProvider fails when user.home is missing") {
    given SystemProperties[IO] = mockSystemProperties(homeDir = None)

    assertIOBoolean(
      for
        provider <- ProfileCredentialsProvider.default[IO]()
        result <- provider.resolveCredentials().attempt
      yield result.isLeft
    )
  }

  test("ProfileCredentialsProvider companion object methods work") {
    val profile = ProfileCredentialsProvider.Profile("test", Map("key" -> "value"))
    assertEquals(profile.name, "test")
    assertEquals(profile.properties, Map("key" -> "value"))
  }

  test("ProfileFile case class creation works") {

    val profiles    = Map("default" -> Profile("default", Map("aws_access_key_id" -> "test")))
    val instant     = Instant.now()
    val profileFile = ProfileFile(profiles, instant)

    assertEquals(profileFile.profiles, profiles)
    assertEquals(profileFile.lastModified, instant)
  }

  test("Profile case class creation works") {

    val properties = Map(
      "aws_access_key_id"     -> "AKIAIOSFODNN7EXAMPLE",
      "aws_secret_access_key" -> "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
    )
    val profile = Profile("production", properties)

    assertEquals(profile.name, "production")
    assertEquals(profile.properties, properties)
  }

  test("ProfileCredentialsProvider handles various profile names correctly") {
    given SystemProperties[IO] = mockSystemProperties()

    val profileNames = List("default", "dev", "staging", "production", "test-profile", "profile_with_underscores")

    assertIO(
      IO.traverse(profileNames)(ProfileCredentialsProvider.default[IO](_)).map(_.length),
      profileNames.length
    )
  }
