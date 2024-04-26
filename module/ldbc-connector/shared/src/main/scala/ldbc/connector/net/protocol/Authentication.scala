/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import ldbc.connector.authenticator.*
import ldbc.connector.util.Version
import ldbc.connector.exception.*

/**
 * Protocol to handle the Authentication Phase
 * 
 * Assume the client wants to log in via user account U and that user account is defined to use authentication method server_method. The fast authentication path is used when:
 * 
 * - the server used server_method to generate authentication data in the Protocol::Handshake packet.
 * - the client used a client_authentication_method in Protocol::HandshakeResponse: that is compatible with the server_method used by the server.
 * 
 * In that case the first round of authentication has been already commenced during the handshake.
 * Now, depending on the authentication method server_method, further authentication can be exchanged until the server either accepts or refuses the authentication.
 *
 * @tparam F
 *   The effect type
 */
trait Authentication[F[_]]:

  /**
   * Determine the authentication plugin.
   *
   * @param pluginName
   *   Plugin name
   * @param version
   *   MySQL Server version
   */
  protected def determinatePlugin(pluginName: String, version: Version): Either[SQLException, AuthenticationPlugin] =
    pluginName match
      case "mysql_native_password" => Right(MysqlNativePasswordPlugin())
      case "sha256_password"       => Right(Sha256PasswordPlugin())
      case "caching_sha2_password" => Right(CachingSha2PasswordPlugin(version))
      case _ => Left(new SQLInvalidAuthorizationSpecException(s"Unknown authentication plugin: $pluginName"))

  /**
   * Start the authentication process.
   *
   * @param username
   *   Username
   * @param password
   *   Password
   */
  def startAuthentication(username: String, password: String): F[Unit]

  /**
   * Change the user.
   *
   * @param user
   *   Username
   * @param password
   *   Password
   */
  def changeUser(user: String, password: String): F[Unit]

object Authentication:

  val FULL_AUTH = "4"
