/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.{ Ref, Sync }
import ldbc.effect.syntax.*

/** Tracks pool metrics, generic over `F`. Ranges from [[PoolMetricsTracker.noop]] to in-memory. */
trait PoolMetricsTracker[F[_]]:
  def recordAcquisition(duration: FiniteDuration): F[Unit]
  def recordUsage(duration: FiniteDuration): F[Unit]
  def recordCreation(duration: FiniteDuration): F[Unit]
  def recordTimeout(): F[Unit]
  def recordLeak(): F[Unit]
  def recordRemoval(): F[Unit]
  def updateGauge(name: String, value: Long): F[Unit]
  def getMetrics: F[PoolMetrics]

object PoolMetricsTracker:

  /** A tracker that discards all metrics. */
  def noop[F[_]](using F: Sync[F]): PoolMetricsTracker[F] = new PoolMetricsTracker[F]:
    override def recordAcquisition(duration: FiniteDuration):      F[Unit]        = F.unit
    override def recordUsage(duration:       FiniteDuration):      F[Unit]        = F.unit
    override def recordCreation(duration:    FiniteDuration):      F[Unit]        = F.unit
    override def recordTimeout():                                  F[Unit]        = F.unit
    override def recordLeak():                                     F[Unit]        = F.unit
    override def recordRemoval():                                  F[Unit]        = F.unit
    override def updateGauge(name:           String, value: Long): F[Unit]        = F.unit
    override def getMetrics:                                       F[PoolMetrics] = F.pure(PoolMetrics.empty)

  /** An in-memory tracker keeping bounded rolling windows of timings. */
  def inMemory[F[_]](using F: Sync[F]): F[PoolMetricsTracker[F]] = for
    acquisitionTimes <- Ref.of[F, Vector[FiniteDuration]](Vector.empty)
    usageTimes       <- Ref.of[F, Vector[FiniteDuration]](Vector.empty)
    creationTimes    <- Ref.of[F, Vector[FiniteDuration]](Vector.empty)
    timeouts         <- Ref.of[F, Long](0L)
    leaks            <- Ref.of[F, Long](0L)
    acquisitions     <- Ref.of[F, Long](0L)
    releases         <- Ref.of[F, Long](0L)
    creations        <- Ref.of[F, Long](0L)
    removals         <- Ref.of[F, Long](0L)
    gauges           <- Ref.of[F, Map[String, Long]](Map.empty)
  yield new PoolMetricsTracker[F]:

    private def recordDuration(ref: Ref[F, Vector[FiniteDuration]], duration: FiniteDuration, maxSize: Int = 100): F[Unit] =
      ref.update { times =>
        val updated = times :+ duration
        if updated.size > maxSize then updated.drop(1) else updated
      }

    private def average(times: Vector[FiniteDuration]): FiniteDuration =
      if times.isEmpty then Duration.Zero else times.foldLeft(Duration.Zero)(_ + _) / times.size

    override def recordAcquisition(duration: FiniteDuration): F[Unit] =
      recordDuration(acquisitionTimes, duration).flatMap(_ => acquisitions.update(_ + 1))

    override def recordUsage(duration: FiniteDuration): F[Unit] =
      recordDuration(usageTimes, duration).flatMap(_ => releases.update(_ + 1))

    override def recordCreation(duration: FiniteDuration): F[Unit] =
      recordDuration(creationTimes, duration).flatMap(_ => creations.update(_ + 1))

    override def recordTimeout(): F[Unit] = timeouts.update(_ + 1)

    override def recordLeak(): F[Unit] = leaks.update(_ + 1)

    override def recordRemoval(): F[Unit] = removals.update(_ + 1)

    override def updateGauge(name: String, value: Long): F[Unit] = gauges.update(_.updated(name, value))

    override def getMetrics: F[PoolMetrics] = for
      acqTimes <- acquisitionTimes.get
      useTimes <- usageTimes.get
      creTimes <- creationTimes.get
      to       <- timeouts.get
      le       <- leaks.get
      acq      <- acquisitions.get
      rel      <- releases.get
      cre      <- creations.get
      rem      <- removals.get
      gs       <- gauges.get
    yield PoolMetrics(
      acquisitionTime   = average(acqTimes),
      usageTime         = average(useTimes),
      creationTime      = average(creTimes),
      timeouts          = to,
      leaks             = le,
      totalAcquisitions = acq,
      totalReleases     = rel,
      totalCreations    = cre,
      totalRemovals     = rem,
      gauges            = gs
    )
