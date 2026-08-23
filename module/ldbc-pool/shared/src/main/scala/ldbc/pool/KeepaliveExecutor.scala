/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.annotation.unused
import scala.concurrent.duration.*
import scala.util.Random

import ldbc.effect.{ Concurrent, Ref, Resource }
import ldbc.effect.syntax.*

/**
 * A background task that keepalive-validates idle connections. The interval carries up to 20% random
 * variance to avoid synchronised validation. The loop is a `sleep`-recursion started as a fiber and
 * cancelled when its [[ldbc.effect.Resource]] is released.
 */
trait KeepaliveExecutor[F[_]]:
  def start(pool: PooledDataSource[F]): Resource[F, Unit]

object KeepaliveExecutor:

  def apply[F[_]](keepaliveTime: FiniteDuration, metricsTracker: PoolMetricsTracker[F])(using
    F: Concurrent[F]
  ): KeepaliveExecutor[F] =
    new Impl(keepaliveTime, metricsTracker)

  private final class Impl[F[_]](keepaliveTime: FiniteDuration, @unused metricsTracker: PoolMetricsTracker[F])(using
    F: Concurrent[F]
  ) extends KeepaliveExecutor[F]:

    override def start(pool: PooledDataSource[F]): Resource[F, Unit] =
      val effectiveDelay = F.delay {
        val variance        = (keepaliveTime.toMillis * 0.2).toLong
        val randomOffset    = if variance > 0 then Random.nextLong(variance) - (variance / 2) else 0L
        val randomizedDelay = keepaliveTime.toMillis + randomOffset
        FiniteDuration(randomizedDelay.max(keepaliveTime.toMillis / 2), MILLISECONDS)
      }
      def loop(delay: FiniteDuration): F[Unit] =
        F.sleep(delay).flatMap(_ => performKeepalive(pool)).flatMap(_ => loop(delay))
      Resource
        .make(effectiveDelay.flatMap(delay => loop(delay).start))(fiber => fiber.cancel)
        .map(_ => ())

    private def nowMillis: F[Long] = F.delay(System.currentTimeMillis())

    private def performKeepalive(pool: PooledDataSource[F]): F[Unit] =
      pool.poolState.get.flatMap { state =>
        if state.closed then F.unit
        else
          val idleConnections = state.connections.filter(pooled => state.idleConnections.contains(pooled.id))
          idleConnections.traverse_(pooled => validateIdleConnection(pool, pooled))
      }

    private def validateIdleConnection(pool: PooledDataSource[F], pooled: PooledConnection[F]): F[Unit] =
      pooled.state.get.flatMap {
        case ConnectionState.Idle =>
          compareAndSetConnectionState(pooled.state, ConnectionState.Idle, ConnectionState.Reserved).flatMap {
            case true =>
              pool.poolState.update(s => s.copy(idleConnections = s.idleConnections - pooled.id)) >>
                (for
                  now   <- nowMillis
                  valid <- pool.validateConnection(pooled.connection)
                  _ <-
                    if valid then
                      pooled.lastValidatedAt.set(now) >>
                        pooled.state.set(ConnectionState.Idle) >>
                        pool.poolState.update(s => s.copy(idleConnections = s.idleConnections + pooled.id))
                    else pool.removeConnection(pooled)
                yield ())
            case false => F.unit
          }
        case _ => F.unit
      }

    private def compareAndSetConnectionState(
      ref:    Ref[F, ConnectionState],
      expect: ConnectionState,
      update: ConnectionState
    ): F[Boolean] =
      ref.modify(current => if current == expect then (update, true) else (current, false))
