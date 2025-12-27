/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.authentication.plugin

import scodec.bits.ByteVector

/**
 * A trait representing a MySQL authentication plugin for database connections.
 * 
 * This trait defines the contract for various authentication mechanisms supported by MySQL,
 * including traditional password-based authentication (mysql_native_password) and 
 * modern authentication methods like mysql_clear_password for IAM authentication.
 * 
 * Authentication plugins are used during the MySQL handshake process to validate
 * client credentials and establish secure database connections.
 * 
 * @tparam F The effect type that wraps the authentication operations
 */
trait AuthenticationPlugin[F[_]]:

  /**
   * The name of the authentication plugin as recognized by the MySQL server.
   * 
   * Common plugin names include:
   * - "mysql_native_password" for traditional SHA1-based password authentication
   * - "mysql_clear_password" for plaintext password transmission over SSL
   * - "caching_sha2_password" for SHA256-based password authentication
   * - "mysql_old_password" for legacy MySQL authentication (deprecated)
   * 
   * @return The plugin name string that identifies this authentication method
   */
  def name: PluginName

  /**
   * Indicates whether this authentication plugin requires a secure (encrypted) connection.
   * 
   * Some authentication plugins, particularly those that transmit passwords in cleartext
   * (like mysql_clear_password), require SSL/TLS encryption to ensure data security.
   * Traditional hashing-based plugins may optionally use encryption but don't strictly require it.
   * 
   * @return true if SSL/TLS connection is mandatory for this plugin, false otherwise
   */
  def requiresConfidentiality: Boolean

  /**
   * Processes the password according to the authentication plugin's requirements.
   * 
   * Different authentication plugins handle passwords differently:
   * - mysql_native_password: Performs SHA1-based hashing with the server's scramble
   * - mysql_clear_password: Returns the password as plaintext bytes (requires SSL)
   * - caching_sha2_password: Performs SHA256-based hashing with salt
   * 
   * @param password The user's password in plaintext
   * @param scramble The random challenge bytes sent by the MySQL server during handshake.
   *                 Used as salt/seed for cryptographic hashing in most authentication methods.
   *                 May be ignored by plugins that don't use server-side challenges.
   * @return The processed password data wrapped in the effect type F, ready for transmission
   *         to the MySQL server during authentication
   */
  def hashPassword(password: String, scramble: Array[Byte]): F[ByteVector]
