/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

/**
 * Tests for the DB-agnostic [[PoolConfigValidator]] (pool sizes, timeouts, lifetimes). The connector's
 * connection-specific validation cases (host, port, user, debug warning) belong to the connector's own
 * config validation, not here.
 */
class PoolConfigValidatorTest extends FxSuite:

  private val valid = ConnectionPoolConfig(minConnections = 5, maxConnections = 20)

  private def expectInvalid(config: ConnectionPoolConfig)(check: Throwable => Unit): Fx[Unit] =
    PoolConfigValidator.validate(config).attempt.map {
      case Left(error) => check(error)
      case Right(_)    => fail("expected validation to fail")
    }

  test("valid configuration should pass validation") {
    PoolConfigValidator.validate(valid).map(_ => assert(true))
  }

  test("negative minConnections should fail validation") {
    expectInvalid(valid.copy(minConnections = -1)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("minConnections cannot be less than 0"))
    }
  }

  test("zero maxConnections should fail validation") {
    expectInvalid(valid.copy(minConnections = 0, maxConnections = 0)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("maxConnections cannot be less than 1"))
    }
  }

  test("minConnections > maxConnections should fail validation") {
    expectInvalid(valid.copy(minConnections = 10, maxConnections = 5)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("cannot be greater than maxConnections"))
    }
  }

  test("negative connectionTimeout should fail validation") {
    expectInvalid(valid.copy(connectionTimeout = -1.seconds)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("connectionTimeout cannot be less than 250ms"))
    }
  }

  test("connectionTimeout less than 250ms should fail validation") {
    expectInvalid(valid.copy(connectionTimeout = 100.milliseconds)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("connectionTimeout cannot be less than 250ms"))
    }
  }

  test("validationTimeout less than 250ms should fail validation") {
    expectInvalid(valid.copy(validationTimeout = 100.milliseconds)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("validationTimeout cannot be less than 250ms"))
    }
  }

  test("negative idleTimeout should fail validation") {
    expectInvalid(valid.copy(idleTimeout = -1.seconds)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("idleTimeout cannot be negative"))
    }
  }

  test("zero idleTimeout should pass validation") {
    PoolConfigValidator.validate(valid.copy(idleTimeout = 0.seconds)).map(_ => assert(true))
  }

  test("maxLifetime less than 30 seconds should fail validation") {
    expectInvalid(valid.copy(maxLifetime = 20.seconds)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("maxLifetime cannot be less than 30 seconds"))
    }
  }

  test("idleTimeout greater than maxLifetime should fail validation") {
    expectInvalid(valid.copy(idleTimeout = 40.minutes, maxLifetime = 30.minutes)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("cannot be greater than maxLifetime"))
    }
  }

  test("leakDetectionThreshold less than 2 seconds should fail validation") {
    expectInvalid(valid.copy(leakDetectionThreshold = Some(1.second))) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("leakDetectionThreshold cannot be less than 2 seconds"))
    }
  }

  test("leakDetectionThreshold greater than maxLifetime should fail validation") {
    expectInvalid(valid.copy(maxLifetime = 30.minutes, leakDetectionThreshold = Some(40.minutes))) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("cannot be greater than maxLifetime"))
    }
  }

  test("zero maintenanceInterval should fail validation") {
    expectInvalid(valid.copy(maintenanceInterval = 0.seconds)) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("maintenanceInterval cannot be less than 1 second"))
    }
  }

  test("validation should collect all errors") {
    val config = valid.copy(
      minConnections    = -1,
      maxConnections    = 0,
      connectionTimeout = -1.seconds,
      validationTimeout = 50.milliseconds
    )
    expectInvalid(config) { error =>
      assert(error.getMessage.contains("Configuration validation failed:"))
      assert(error.getMessage.contains("minConnections cannot be less than 0"))
      assert(error.getMessage.contains("maxConnections cannot be less than 1"))
      assert(error.getMessage.contains("connectionTimeout cannot be less than 250ms"))
      assert(error.getMessage.contains("validationTimeout cannot be less than 250ms"))
    }
  }
