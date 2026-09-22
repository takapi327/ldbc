/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.protocol

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

  /**
   * The byte a client sends to ask the server for its RSA public key, so a password can be encrypted
   * before crossing an unencrypted connection.
   *
   * These are authentication-phase markers, not command ids. The server reads the first payload byte
   * according to the phase the connection is in, so the fact that `0x01` and `0x02` also happen to be
   * `COM_QUIT` and `COM_INIT_DB` means nothing here — the two sets of values are unrelated and free to
   * diverge. Each plugin has its own marker.
   */
  val PUBLIC_KEY_REQUEST_SHA256: Byte = 0x01

  /** See [[PUBLIC_KEY_REQUEST_SHA256]]. `caching_sha2_password` asks with a different byte. */
  val PUBLIC_KEY_REQUEST_CACHING_SHA2: Byte = 0x02
