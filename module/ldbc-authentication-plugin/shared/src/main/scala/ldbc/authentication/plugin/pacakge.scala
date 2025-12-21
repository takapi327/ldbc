/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.authentication

package object plugin:

  opaque type PluginName = String

  // Standard MySQL Authentication Plugins
  val MYSQL_CLEAR_PASSWORD:  PluginName = "mysql_clear_password"
  val MYSQL_NATIVE_PASSWORD: PluginName = "mysql_native_password"
  val SHA256_PASSWORD:       PluginName = "sha256_password"
  val CACHING_SHA2_PASSWORD: PluginName = "caching_sha2_password"

  // Legacy Authentication Plugin (deprecated)
  val MYSQL_OLD_PASSWORD: PluginName = "mysql_old_password"

  // Authentication plugins for external authentication
  val AUTHENTICATION_WINDOWS:     PluginName = "authentication_windows"
  val AUTHENTICATION_PAM:         PluginName = "authentication_pam"
  val AUTHENTICATION_LDAP_SIMPLE: PluginName = "authentication_ldap_simple"
  val AUTHENTICATION_LDAP_SASL:   PluginName = "authentication_ldap_sasl"

  // Kerberos authentication
  val AUTHENTICATION_KERBEROS: PluginName = "authentication_kerberos"

  // FIDO authentication (MySQL 8.0.27+)
  val AUTHENTICATION_FIDO: PluginName = "authentication_fido"

  // Multi-factor authentication (MySQL 8.0.27+)
  val AUTHENTICATION_WEBAUTHN: PluginName = "authentication_webauthn"

  // No login authentication plugin
  val MYSQL_NO_LOGIN: PluginName = "mysql_no_login"

  // Test plugins (for testing purposes)
  val TEST_PLUGIN_SERVER: PluginName = "test_plugin_server"
  val DAEMON_EXAMPLE:     PluginName = "daemon_example"

  // Socket peer-credential authentication (Unix socket)
  val AUTH_SOCKET: PluginName = "auth_socket"
