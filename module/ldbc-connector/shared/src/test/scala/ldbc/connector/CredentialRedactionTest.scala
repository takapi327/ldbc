/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

/**
 * Verification test for the security finding: the default case-class `toString`
 * of [[MySQLDataSource]] and [[MySQLConfig]] exposes the configured password in
 * plaintext. After the fix, `toString` must redact the password while remaining
 * useful for debugging (host / port / user / database are still shown).
 */
class CredentialRedactionTest extends FTestPlatform:

  private val secret = "sup3r-s3cret-passw0rd"

  test("MySQLDataSource.toString should not expose the password") {
    val dataSource = MySQLDataSource[IO, Unit](
      host     = "localhost",
      port     = 3306,
      user     = "myuser",
      password = Some(secret),
      database = Some("mydb")
    )

    val rendered = dataSource.toString

    // Non-sensitive connection fields remain visible for debugging.
    assert(rendered.contains("localhost"))
    assert(rendered.contains("myuser"))

    // The password MUST NOT appear in any string representation.
    assert(!rendered.contains(secret), s"password leaked in toString: $rendered")
  }

  test("MySQLConfig.toString should not expose the password") {
    val config = MySQLConfig.default
      .setUser("myuser")
      .setDatabase("mydb")
      .setPassword(secret)

    val rendered = config.toString

    assert(rendered.contains("myuser"))

    assert(!rendered.contains(secret), s"password leaked in toString: $rendered")
  }
