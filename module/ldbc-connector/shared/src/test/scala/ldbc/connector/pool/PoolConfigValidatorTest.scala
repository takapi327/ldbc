/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*
import cats.effect.std.Console

import munit.CatsEffectSuite

import ldbc.connector.MySQLConfig

class PoolConfigValidatorTest extends CatsEffectSuite:

  test("valid configuration should pass validation") {
    val config = MySQLConfig.default
      .setHost("localhost")
      .setPort(3306)
      .setUser("testuser")
      .setMinConnections(5)
      .setMaxConnections(20)
      .setConnectionTimeout(30.seconds)
      .setIdleTimeout(10.minutes)
      .setMaxLifetime(30.minutes)
      .setValidationTimeout(5.seconds)
      .setMaintenanceInterval(30.seconds)

    PoolConfigValidator.validate[IO](config).map(_ => assert(true))
  }

  test("negative minConnections should fail validation") {
    val config = MySQLConfig.default.setMinConnections(-1)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("minConnections cannot be less than 0"))
    }
  }

  test("zero maxConnections should fail validation") {
    val config = MySQLConfig.default
      .setMinConnections(0)
      .setMaxConnections(0)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("maxConnections cannot be less than 1"))
    }
  }

  test("minConnections > maxConnections should fail validation") {
    val config = MySQLConfig.default
      .setMinConnections(10)
      .setMaxConnections(5)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("cannot be greater than maxConnections"))
    }
  }

  test("negative connectionTimeout should fail validation") {
    val config = MySQLConfig.default
      .setConnectionTimeout(-1.seconds)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("connectionTimeout cannot be less than 250ms"))
    }
  }

  test("connectionTimeout less than 250ms should fail validation") {
    val config = MySQLConfig.default
      .setConnectionTimeout(100.milliseconds)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("connectionTimeout cannot be less than 250ms"))
    }
  }

  test("validationTimeout less than 250ms should fail validation") {
    val config = MySQLConfig.default
      .setValidationTimeout(100.milliseconds)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("validationTimeout cannot be less than 250ms"))
    }
  }

  test("negative idleTimeout should fail validation") {
    val config = MySQLConfig.default
      .setIdleTimeout(-1.seconds)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("idleTimeout cannot be negative"))
    }
  }

  test("zero idleTimeout should pass validation") {
    val config = MySQLConfig.default
      .setIdleTimeout(0.seconds) // 0 means connections never idle out

    PoolConfigValidator.validate[IO](config).map(_ => assert(true))
  }

  test("maxLifetime less than 30 seconds should fail validation") {
    val config = MySQLConfig.default
      .setMaxLifetime(20.seconds)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("maxLifetime cannot be less than 30 seconds"))
    }
  }

  test("idleTimeout greater than maxLifetime should fail validation") {
    val config = MySQLConfig.default
      .setIdleTimeout(40.minutes)
      .setMaxLifetime(30.minutes)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("cannot be greater than maxLifetime"))
    }
  }

  test("leakDetectionThreshold less than 2 seconds should fail validation") {
    val config = MySQLConfig.default
      .setLeakDetectionThreshold(1.second)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("leakDetectionThreshold cannot be less than 2 seconds"))
    }
  }

  test("leakDetectionThreshold greater than maxLifetime should fail validation") {
    val config = MySQLConfig.default
      .setMaxLifetime(30.minutes)
      .setLeakDetectionThreshold(40.minutes)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("cannot be greater than maxLifetime"))
    }
  }

  test("empty user should fail validation") {
    val config = MySQLConfig.default
      .setUser("")

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("user cannot be null or empty"))
    }
  }

  test("empty host should fail validation") {
    val config = MySQLConfig.default
      .setHost("")

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("host cannot be null or empty"))
    }
  }

  test("invalid port should fail validation") {
    val config = MySQLConfig.default.setPort(0)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("port must be between 1 and 65535"))
    }
  }

  test("port over 65535 should fail validation") {
    val config = MySQLConfig.default.setPort(70000)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("port must be between 1 and 65535"))
    }
  }

  test("zero maintenanceInterval should fail validation") {
    val config = MySQLConfig.default
      .setMaintenanceInterval(0.seconds)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("maintenanceInterval cannot be less than 1 second"))
    }
  }

  test("large pool size should pass validation without debug mode") {
    val config = MySQLConfig.default
      .setMaxConnections(200)
      .setDebug(false)

    // Should pass without warning since debug is false
    PoolConfigValidator.validate[IO](config).map(_ => assert(true))
  }

  test("large pool size with debug mode should log warning") {
    val config = MySQLConfig.default
      .setMaxConnections(150)
      .setDebug(true)

    // Create a test console to capture output
    val testConsole = new TestConsole

    given Console[IO] = testConsole

    PoolConfigValidator.validate[IO](config).map { _ =>
      val output = testConsole.getOutput
      assert(output.exists(_.contains("[WARN]")))
      assert(output.exists(_.contains("maxConnections (150) is unusually high")))
    }
  }

  test("validation should fail fast on first error") {
    val config = MySQLConfig.default
      .setMinConnections(-1)            // This will fail first
      .setMaxConnections(0)             // This would also fail
      .setConnectionTimeout(-1.seconds) // This would also fail

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validate[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("minConnections cannot be less than 0"))
    }
  }

  test("individual validation methods should work correctly") {
    val config = MySQLConfig.default.setMinConnections(-1)

    interceptIO[IllegalArgumentException] {
      PoolConfigValidator.validateMinConnections[IO](config)
    }.map { error =>
      assert(error.getMessage.contains("minConnections cannot be less than 0"))
    }
  }

  test("validateMaxConnections should pass for normal values") {
    val config = MySQLConfig.default.setMaxConnections(50)

    given Console[IO] = new TestConsole

    PoolConfigValidator.validateMaxConnections[IO](config).map(_ => assert(true))
  }

  test("validateLeakDetectionThreshold should pass when not set") {
    val config = MySQLConfig.default // leakDetectionThreshold is None by default

    PoolConfigValidator.validateLeakDetectionThreshold[IO](config).map(_ => assert(true))
  }

  // Helper class to capture console output for testing
  private class TestConsole extends Console[IO]:
    private var output: List[String] = List.empty

    def getOutput: List[String] = output

    override def print[A](a: A)(implicit S: cats.Show[A]): IO[Unit] =
      IO { output = output :+ S.show(a) }

    override def println[A](a: A)(implicit S: cats.Show[A]): IO[Unit] =
      IO { output = output :+ S.show(a) }

    override def error[A](a: A)(implicit S: cats.Show[A]): IO[Unit] =
      IO { output = output :+ s"ERROR: ${ S.show(a) }" }

    override def errorln[A](a: A)(implicit S: cats.Show[A]): IO[Unit] =
      IO { output = output :+ s"ERROR: ${ S.show(a) }" }

    override def readLine: IO[String] = IO.pure("")

    override def readLineWithCharset(charset: java.nio.charset.Charset): IO[String] = IO.pure("")
