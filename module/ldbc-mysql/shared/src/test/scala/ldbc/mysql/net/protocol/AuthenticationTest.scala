/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.protocol

import ldbc.fx.{ Fx, Ref }
import ldbc.mysql.FTestPlatform

class AuthenticationTest extends Authentication, FTestPlatform:

  // Mock state for testing authentication process
  private val authenticationStarted: Ref[Boolean]        = Ref.unsafe[Boolean](false)
  private val authenticatedUser:     Ref[Option[String]] = Ref.unsafe[Option[String]](None)
  private val authenticatedPassword: Ref[Option[String]] = Ref.unsafe[Option[String]](None)
  private val userChanged:           Ref[Boolean]        = Ref.unsafe[Boolean](false)

  // Cache which plugin was selected in determinatePlugin for testing
  private val selectedPluginName: Ref[Option[String]] = Ref.unsafe[Option[String]](None)

  // Implementation of required methods from Authentication
  override def startAuthentication(username: String, password: String): Fx[Unit] =
    for {
      _ <- authenticationStarted.set(true)
      _ <- authenticatedUser.set(Some(username))
      _ <- authenticatedPassword.set(Some(password))
    } yield ()

  override def changeUser(user: String, password: String): Fx[Unit] =
    for {
      _ <- userChanged.set(true)
      _ <- authenticatedUser.set(Some(user))
      _ <- authenticatedPassword.set(Some(password))
    } yield ()

  // Test helper methods
  private def resetState: Fx[Unit] = for {
    _ <- authenticationStarted.set(false)
    _ <- authenticatedUser.set(None)
    _ <- authenticatedPassword.set(None)
    _ <- userChanged.set(false)
    _ <- selectedPluginName.set(None)
  } yield ()

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
