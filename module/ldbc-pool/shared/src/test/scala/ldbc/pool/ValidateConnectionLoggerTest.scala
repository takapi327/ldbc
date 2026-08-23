/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.fx.FxSuite

import ldbc.fx.Fx
import ldbc.fx.concurrentFx
import ldbc.effect.{ Ref, Resource }

/**
 * Regression tests for bug #710: `validateConnection` used `handleError`, silently dropping the debug
 * log effect. These call `validateConnection` directly on a real [[PooledDataSource]] (built with
 * `minConnections = 0` so no physical connection is created) and verify the debug log is actually
 * emitted when validation fails.
 *
 *   - buggy code (`handleError`): FAILS
 *   - fixed code (`handleErrorWith`): PASSES
 *
 * @see https://github.com/takapi327/ldbc/issues/710
 */
class ValidateConnectionLoggerTest extends FxSuite:

  /** A [[PoolLogger]] that records every message it emits, for assertion. */
  private class RecordingPoolLogger(ref: Ref[Fx, List[String]]) extends PoolLogger[Fx]:
    override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): Fx[Unit] = Fx.unit
    override def debug(message: String): Fx[Unit] = ref.update(_ :+ s"[DEBUG] $message")
    override def info(message:  String): Fx[Unit] = ref.update(_ :+ s"[INFO] $message")
    override def warn(message:  String): Fx[Unit] = ref.update(_ :+ s"[WARN] $message")
    override def error(message: String, error: Option[Throwable]): Fx[Unit] = ref.update(_ :+ s"[ERROR] $message")
    override def isDebugEnabled: Fx[Boolean] = Fx.pure(true)

  /** minConnections = 0 so the factory is never invoked; the factory itself must therefore never run. */
  private val poolConfig = ConnectionPoolConfig(minConnections = 0, maxConnections = 1, debug = true)

  private val neverCreate: Resource[Fx, ldbc.sql.Connection[Fx]] =
    Resource.eval(Fx.raiseError(new RuntimeException("factory should not be called with minConnections = 0")))

  test("Bug #710: validateConnection emits a debug log when isValid() fails") {
    Ref.of(List.empty[String]).flatMap { logs =>
      val logger = new RecordingPoolLogger(logs)
      for
        conn <- MockConnection(isValidError = Some(new RuntimeException("connection broken")))
        _ <- PooledDataSource
               .fromConfig(poolConfig, neverCreate, poolLogger = Some(logger))
               .use { ds =>
                 for
                   result   <- ds.validateConnection(conn)
                   recorded <- logs.get
                   debugLogs = recorded.filter(_.startsWith("[DEBUG]"))
                 yield
                   assertEquals(result, false)
                   assert(
                     debugLogs.exists(_.contains("validation failed")),
                     s"Expected a [DEBUG] log about validation failure, but none was captured.\nCaptured: $recorded"
                   )
               }
      yield ()
    }
  }
