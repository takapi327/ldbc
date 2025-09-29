/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import cats.*

import cats.effect.*
import cats.effect.std.Console

/**
 * A logger for connection pool events and state information.
 * 
 * This trait abstracts the logging mechanism for connection pools, allowing
 * different implementations (console, file, external logging frameworks, etc.)
 * to be plugged in based on the application's needs.
 * 
 * The logger provides different log levels (debug, info, warn, error) to
 * control the verbosity of output and help with debugging and monitoring
 * pool behavior.
 * 
 * @tparam F the effect type
 */
trait PoolLogger[F[_]]:

  /**
   * Log the current state of the connection pool.
   * 
   * This typically includes information like:
   * - Total connections (current/max)
   * - Active connections
   * - Idle connections (current/min)
   * - Waiting threads
   * 
   * @param poolName the name of the pool
   * @param status the current pool status
   * @param metrics optional metrics for additional information
   */
  def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics] = None): F[Unit]

  /**
   * Log a debug level message.
   * 
   * Debug messages are typically used for detailed information useful
   * during development or troubleshooting.
   * 
   * @param message the message to log
   */
  def debug(message: String): F[Unit]

  /**
   * Log an info level message.
   * 
   * Info messages are for general informational events that highlight
   * the progress of the application at a coarse-grained level.
   * 
   * @param message the message to log
   */
  def info(message: String): F[Unit]

  /**
   * Log a warning level message.
   * 
   * Warning messages indicate potentially harmful situations that should
   * be investigated but don't prevent the application from functioning.
   * 
   * @param message the message to log
   */
  def warn(message: String): F[Unit]

  /**
   * Log an error level message with an optional exception.
   * 
   * Error messages indicate serious problems that have occurred and
   * typically require immediate attention.
   * 
   * @param message the error message
   * @param error optional exception that caused the error
   */
  def error(message: String, error: Option[Throwable] = None): F[Unit]

  /**
   * Check if debug logging is enabled.
   * 
   * This can be used to avoid expensive message construction when
   * debug logging is disabled.
   * 
   * @return true if debug level is enabled
   */
  def isDebugEnabled: F[Boolean]

object PoolLogger:

  /**
   * Creates a PoolLogger that outputs to the console using Cats Effect's Console.
   * 
   * This implementation writes all log messages to stdout/stderr based on the log level:
   * - Debug and Info messages go to stdout
   * - Warn and Error messages go to stderr
   * 
   * The pool state is formatted similar to HikariCP's output:
   * `[poolName] - stats (total=X/maxSize, idle=Y/minSize, active=Z, waiting=W)`
   * 
   * @param logDebug whether debug messages should be logged
   * @tparam F the effect type (must have Console and Applicative instances)
   * @return a new console-based PoolLogger
   */
  def console[F[_]: Console: Applicative](logDebug: Boolean = false): PoolLogger[F] = new PoolLogger[F]:

    override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): F[Unit] =
      val baseStats =
        s"[$poolName] - stats (total=${ status.total }, idle=${ status.idle }, active=${ status.active }, waiting=${ status.waiting })"

      val fullMessage = metrics match
        case Some(m) =>
          val avgAcquisition = m.acquisitionTime.toMillis
          val timeouts       = m.timeouts
          val leaks          = m.leaks
          s"$baseStats [avgAcquisition=${ avgAcquisition }ms, timeouts=$timeouts, leaks=$leaks]"
        case None =>
          baseStats

      if logDebug then debug(fullMessage)
      else Applicative[F].unit

    override def debug(message: String): F[Unit] =
      if logDebug then Console[F].println(s"[DEBUG] $message")
      else Applicative[F].unit

    override def info(message: String): F[Unit] =
      Console[F].println(s"[INFO] $message")

    override def warn(message: String): F[Unit] =
      Console[F].errorln(s"[WARN] $message")

    override def error(message: String, error: Option[Throwable]): F[Unit] =
      val errorMessage = error match
        case Some(e) => s"$message: ${ e.getMessage }"
        case None    => message
      Console[F].errorln(s"[ERROR] $errorMessage")

    override def isDebugEnabled: F[Boolean] =
      Applicative[F].pure(logDebug)

  /**
   * Creates a no-op PoolLogger that discards all log messages.
   * 
   * This implementation is useful for testing or when logging is not desired.
   * All methods return immediately without performing any operations.
   * 
   * @tparam F the effect type (must have an Applicative instance)
   * @return a PoolLogger that performs no operations
   */
  def noop[F[_]: Applicative]: PoolLogger[F] = new PoolLogger[F]:
    override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): F[Unit] =
      Applicative[F].unit
    override def debug(message: String): F[Unit] =
      Applicative[F].unit
    override def info(message: String): F[Unit] =
      Applicative[F].unit
    override def warn(message: String): F[Unit] =
      Applicative[F].unit
    override def error(message: String, error: Option[Throwable]): F[Unit] =
      Applicative[F].unit
    override def isDebugEnabled: F[Boolean] =
      Applicative[F].pure(false)
