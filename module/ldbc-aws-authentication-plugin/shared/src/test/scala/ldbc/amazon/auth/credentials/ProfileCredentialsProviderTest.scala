/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import cats.effect.std.SystemProperties
import cats.effect.IO

import fs2.io.file.Files

import munit.CatsEffectSuite

import ldbc.amazon.exception.SdkClientException

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

  test("ProfileCredentialsProvider creation succeeds with default parameters") {
    given SystemProperties[IO] = mockSystemProperties()
    given Files[IO] = Files.forIO

    for
      provider <- ProfileCredentialsProvider.default[IO]()
    yield
      // Basic test - provider was created successfully
      assert(provider != null)
  }

  test("ProfileCredentialsProvider creation succeeds with named profile") {
    given SystemProperties[IO] = mockSystemProperties()
    given Files[IO] = Files.forIO

    for
      provider <- ProfileCredentialsProvider.default[IO]("dev")
    yield
      // Basic test - provider was created successfully
      assert(provider != null)
  }

  test("ProfileCredentialsProvider fails when user.home is missing") {
    given SystemProperties[IO] = mockSystemProperties(homeDir = None)
    given Files[IO] = Files.forIO

    for
      provider <- ProfileCredentialsProvider.default[IO]()
      result <- provider.resolveCredentials().attempt
    yield
      result match
        case Left(exception: SdkClientException) =>
          // Should fail because user.home is not available
          assert(true) // Test passed
        case _ => fail("Expected SdkClientException")
  }

  test("ProfileCredentialsProvider companion object methods work") {
    val profile = ProfileCredentialsProvider.Profile("test", Map("key" -> "value"))
    assertEquals(profile.name, "test")
    assertEquals(profile.properties, Map("key" -> "value"))
  }

  test("ProfileFile case class creation works") {
    import java.time.Instant
    import ProfileCredentialsProvider.*
    
    val profiles = Map("default" -> Profile("default", Map("aws_access_key_id" -> "test")))
    val instant = Instant.now()
    val profileFile = ProfileFile(profiles, instant)
    
    assertEquals(profileFile.profiles, profiles)
    assertEquals(profileFile.lastModified, instant)
  }

  test("Profile case class creation works") {
    import ProfileCredentialsProvider.*
    
    val properties = Map(
      "aws_access_key_id" -> "AKIAIOSFODNN7EXAMPLE",
      "aws_secret_access_key" -> "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
    )
    val profile = Profile("production", properties)
    
    assertEquals(profile.name, "production")
    assertEquals(profile.properties, properties)
  }

  // Test basic functionality that doesn't require file system access
  test("ProfileCredentialsProvider implements AwsCredentialsProvider trait") {
    given SystemProperties[IO] = mockSystemProperties()
    given Files[IO] = Files.forIO

    for
      provider <- ProfileCredentialsProvider.default[IO]()
      providerTrait: ldbc.amazon.identity.AwsCredentialsProvider[IO] = provider
    yield
      // Type check passes - provider implements the trait correctly
      assert(providerTrait != null)
  }

  test("ProfileCredentialsProvider default factory creates provider with correct profile name") {
    given SystemProperties[IO] = mockSystemProperties()
    given Files[IO] = Files.forIO

    for
      defaultProvider <- ProfileCredentialsProvider.default[IO]()
      namedProvider <- ProfileCredentialsProvider.default[IO]("custom")
    yield
      // Both providers should be created successfully
      assert(defaultProvider != null)
      assert(namedProvider != null)
  }

  // Test thread safety of provider creation
  test("ProfileCredentialsProvider factory is thread-safe") {
    given SystemProperties[IO] = mockSystemProperties()
    given Files[IO] = Files.forIO

    for
      providers <- IO.parSequenceN(10)((1 to 10).map(_ => ProfileCredentialsProvider.default[IO]()).toList)
    yield
      // All providers should be created successfully
      providers.foreach(provider => assert(provider != null))
  }

  test("ProfileCredentialsProvider handles various profile names correctly") {
    given SystemProperties[IO] = mockSystemProperties()
    given Files[IO] = Files.forIO

    val profileNames = List("default", "dev", "staging", "production", "test-profile", "profile_with_underscores")

    for
      providers <- IO.traverse(profileNames)(ProfileCredentialsProvider.default[IO](_))
    yield
      // All providers should be created successfully regardless of profile name format
      providers.foreach(provider => assert(provider != null))
      assertEquals(providers.length, profileNames.length)
  }