/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.effect.Sync

/** A structured logger for pool lifecycle events, generic over the effect `F`. */
trait PoolLogger[F[_]]:
  def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics] = None): F[Unit]
  def debug(message:         String):                                                          F[Unit]
  def info(message:          String):                                                          F[Unit]
  def warn(message:          String):                                                          F[Unit]
  def error(message:         String, error:  Option[Throwable] = None):                        F[Unit]
  def isDebugEnabled:                                                                          F[Boolean]

object PoolLogger:

  /** A logger that prints to stdout/stderr. */
  def console[F[_]](logDebug: Boolean = false)(using F: Sync[F]): PoolLogger[F] = new PoolLogger[F]:

    override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): F[Unit] =
      val baseStats =
        s"[$poolName] - stats (total=${ status.total }, idle=${ status.idle }, active=${ status.active }, waiting=${ status.waiting })"
      val fullMessage = metrics match
        case Some(m) =>
          s"$baseStats [avgAcquisition=${ m.acquisitionTime.toMillis }ms, timeouts=${ m.timeouts }, leaks=${ m.leaks }]"
        case None => baseStats
      if logDebug then debug(fullMessage) else F.unit

    override def debug(message: String): F[Unit] =
      if logDebug then F.delay(println(s"[DEBUG] $message")) else F.unit

    override def info(message: String): F[Unit] = F.delay(println(s"[INFO] $message"))

    override def warn(message: String): F[Unit] = F.delay(System.err.println(s"[WARN] $message"))

    override def error(message: String, error: Option[Throwable]): F[Unit] =
      val errorMessage = error match
        case Some(e) => s"$message: ${ e.getMessage }"
        case None    => message
      F.delay(System.err.println(s"[ERROR] $errorMessage"))

    override def isDebugEnabled: F[Boolean] = F.pure(logDebug)

  /** A logger that discards all messages. */
  def noop[F[_]](using F: Sync[F]): PoolLogger[F] = new PoolLogger[F]:
    override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): F[Unit] = F.unit
    override def debug(message:         String):                                                   F[Unit] = F.unit
    override def info(message:          String):                                                   F[Unit] = F.unit
    override def warn(message:          String):                                                   F[Unit] = F.unit
    override def error(message:         String, error:  Option[Throwable]):                        F[Unit] = F.unit
    override def isDebugEnabled: F[Boolean] = F.pure(false)
