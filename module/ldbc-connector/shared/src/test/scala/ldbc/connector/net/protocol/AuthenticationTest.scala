/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import cats.effect.{ IO, Ref }

import fs2.hashing.Hashing

import ldbc.connector.authenticator.*
import ldbc.connector.exception.*
import ldbc.connector.util.Version
import ldbc.connector.FTestPlatform

class AuthenticationTest extends Authentication[IO], FTestPlatform:

  // Mock state for testing authentication process
  private val authenticationStarted: Ref[IO, Boolean]        = Ref.unsafe[IO, Boolean](false)
  private val authenticatedUser:     Ref[IO, Option[String]] = Ref.unsafe[IO, Option[String]](None)
  private val authenticatedPassword: Ref[IO, Option[String]] = Ref.unsafe[IO, Option[String]](None)
  private val userChanged:           Ref[IO, Boolean]        = Ref.unsafe[IO, Boolean](false)

  // Cache which plugin was selected in determinatePlugin for testing
  private val selectedPluginName: Ref[IO, Option[String]] = Ref.unsafe[IO, Option[String]](None)

  // Implementation of required methods from Authentication
  override def startAuthentication(username: String, password: String): IO[Unit] =
    for {
      _ <- authenticationStarted.set(true)
      _ <- authenticatedUser.set(Some(username))
      _ <- authenticatedPassword.set(Some(password))
    } yield ()

  override def changeUser(user: String, password: String): IO[Unit] =
    for {
      _ <- userChanged.set(true)
      _ <- authenticatedUser.set(Some(user))
      _ <- authenticatedPassword.set(Some(password))
    } yield ()

  // Test helper methods
  private def resetState: IO[Unit] = for {
    _ <- authenticationStarted.set(false)
    _ <- authenticatedUser.set(None)
    _ <- authenticatedPassword.set(None)
    _ <- userChanged.set(false)
    _ <- selectedPluginName.set(None)
  } yield ()

  // Test for determinatePlugin
  test("determinatePlugin should return the correct plugin for mysql_native_password") {
    val version = Version(8, 0, 26)
    val result  = determinatePlugin("mysql_native_password", version)
    assert(result.isRight, "Expected Right but got Left")
    assert(
      result.exists(_.isInstanceOf[MysqlNativePasswordPlugin[IO]]),
      s"Expected MysqlNativePasswordPlugin but got ${ result.map(_.getClass.getSimpleName) }"
    )
  }

  test("determinatePlugin should return the correct plugin for sha256_password") {
    val version = Version(8, 0, 26)
    val result  = determinatePlugin("sha256_password", version)
    assert(result.isRight, "Expected Right but got Left")
    assert(
      result.exists(_.isInstanceOf[Sha256PasswordPlugin[IO]]),
      s"Expected Sha256PasswordPlugin but got ${ result.map(_.getClass.getSimpleName) }"
    )
  }

  test("determinatePlugin should return the correct plugin for caching_sha2_password") {
    val version = Version(8, 0, 26)
    val result  = determinatePlugin("caching_sha2_password", version)
    assert(result.isRight, "Expected Right but got Left")
    assert(
      result.exists(_.isInstanceOf[CachingSha2PasswordPlugin[IO]]),
      s"Expected CachingSha2PasswordPlugin but got ${ result.map(_.getClass.getSimpleName) }"
    )
  }

  test("determinatePlugin should return Left for unknown plugin") {
    val version = Version(8, 0, 26)
    val result  = determinatePlugin("unknown_plugin", version)
    assert(result.isLeft, "Expected Left but got Right")
    val exception = result.left.getOrElse(throw new RuntimeException("Expected Left but got Right"))
    assert(
      exception.isInstanceOf[SQLInvalidAuthorizationSpecException],
      s"Expected SQLInvalidAuthorizationSpecException but got ${ exception.getClass.getSimpleName }"
    )
    assert(
      exception.getMessage.contains("Unknown authentication plugin: unknown_plugin"),
      s"Error message did not match expected. Got: ${ exception.getMessage }"
    )
  }

  test("startAuthentication should set the correct state") {
    for {
      _       <- resetState
      _       <- startAuthentication("testuser", "testpass")
      started <- authenticationStarted.get
      user    <- authenticatedUser.get
      pass    <- authenticatedPassword.get
    } yield {
      assert(started, "Authentication was not started")
      assertEquals(user, Some("testuser"))
      assertEquals(pass, Some("testpass"))
    }
  }

  test("changeUser should set the correct state") {
    for {
      _       <- resetState
      _       <- changeUser("newuser", "newpass")
      changed <- userChanged.get
      user    <- authenticatedUser.get
      pass    <- authenticatedPassword.get
    } yield {
      assert(changed, "User was not changed")
      assertEquals(user, Some("newuser"))
      assertEquals(pass, Some("newpass"))
    }
  }
