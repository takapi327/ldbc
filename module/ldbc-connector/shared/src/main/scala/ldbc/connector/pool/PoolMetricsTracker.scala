/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.*
import cats.effect.*
import cats.syntax.all.*

/**
 * Trait for tracking pool metrics.
 * 
 * @tparam F the effect type
 */
trait PoolMetricsTracker[F[_]]:
  
  /**
   * Record the time taken to acquire a connection.
   * 
   * @param duration the duration of the acquisition
   */
  def recordAcquisition(duration: FiniteDuration): F[Unit]
  
  /**
   * Record the time a connection was used.
   * 
   * @param duration the duration of usage
   */
  def recordUsage(duration: FiniteDuration): F[Unit]
  
  /**
   * Record the time taken to create a connection.
   * 
   * @param duration the duration of creation
   */
  def recordCreation(duration: FiniteDuration): F[Unit]
  
  /**
   * Record a timeout event.
   */
  def recordTimeout(): F[Unit]
  
  /**
   * Record a leak detection.
   */
  def recordLeak(): F[Unit]
  
  /**
   * Update a gauge metric.
   * 
   * @param name the name of the gauge
   * @param value the current value
   */
  def updateGauge(name: String, value: Long): F[Unit]
  
  /**
   * Get current metrics snapshot.
   * 
   * @return current metrics
   */
  def getMetrics: F[PoolMetrics]

object PoolMetricsTracker:
  
  /**
   * A no-op metrics tracker.
   */
  def noop[F[_]: Applicative]: PoolMetricsTracker[F] = new PoolMetricsTracker[F]:
    def recordAcquisition(duration: FiniteDuration): F[Unit] = Applicative[F].unit
    def recordUsage(duration: FiniteDuration): F[Unit] = Applicative[F].unit
    def recordCreation(duration: FiniteDuration): F[Unit] = Applicative[F].unit
    def recordTimeout(): F[Unit] = Applicative[F].unit
    def recordLeak(): F[Unit] = Applicative[F].unit
    def updateGauge(name: String, value: Long): F[Unit] = Applicative[F].unit
    def getMetrics: F[PoolMetrics] = Applicative[F].pure(PoolMetrics.empty)
  
  /**
   * Default in-memory metrics tracker.
   */
  def inMemory[F[_]: Sync]: F[PoolMetricsTracker[F]] = for
    acquisitionTimes <- Ref[F].of(Vector.empty[FiniteDuration])
    usageTimes       <- Ref[F].of(Vector.empty[FiniteDuration])
    creationTimes    <- Ref[F].of(Vector.empty[FiniteDuration])
    timeouts         <- Ref[F].of(0L)
    leaks            <- Ref[F].of(0L)
    acquisitions     <- Ref[F].of(0L)
    releases         <- Ref[F].of(0L)
    creations        <- Ref[F].of(0L)
    removals         <- Ref[F].of(0L)
    gauges           <- Ref[F].of(Map.empty[String, Long])
  yield new PoolMetricsTracker[F]:
    
    private def recordDuration(ref: Ref[F, Vector[FiniteDuration]], duration: FiniteDuration, maxSize: Int = 100): F[Unit] =
      ref.update { times =>
        val updated = times :+ duration
        if updated.size > maxSize then updated.drop(1) else updated
      }
    
    private def average(times: Vector[FiniteDuration]): FiniteDuration =
      if times.isEmpty then Duration.Zero
      else times.foldLeft(Duration.Zero)(_ + _) / times.size
    
    def recordAcquisition(duration: FiniteDuration): F[Unit] =
      recordDuration(acquisitionTimes, duration) *> acquisitions.update(_ + 1)
    
    def recordUsage(duration: FiniteDuration): F[Unit] =
      recordDuration(usageTimes, duration) *> releases.update(_ + 1)
    
    def recordCreation(duration: FiniteDuration): F[Unit] =
      recordDuration(creationTimes, duration) *> creations.update(_ + 1)
    
    def recordTimeout(): F[Unit] = timeouts.update(_ + 1)
    
    def recordLeak(): F[Unit] = leaks.update(_ + 1)
    
    def updateGauge(name: String, value: Long): F[Unit] =
      gauges.update(_.updated(name, value))
    
    def getMetrics: F[PoolMetrics] = for
      acqTimes <- acquisitionTimes.get
      useTimes <- usageTimes.get
      creTimes <- creationTimes.get
      to       <- timeouts.get
      le       <- leaks.get
      acq      <- acquisitions.get
      rel      <- releases.get
      cre      <- creations.get
      rem      <- removals.get
    yield PoolMetrics(
      acquisitionTime   = average(acqTimes),
      usageTime         = average(useTimes),
      creationTime      = average(creTimes),
      timeouts          = to,
      leaks             = le,
      totalAcquisitions = acq,
      totalReleases     = rel,
      totalCreations    = cre,
      totalRemovals     = rem
    )
