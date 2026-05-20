/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.syntax.all.*

import ldbc.connector.exception.SQLException

/**
 * Circuit breaker pattern implementation for connection creation.
 *
 * Prevents thundering herd problem when database is down by failing fast
 * instead of attempting to create connections that will likely fail.
 *
 * States:
 * - Closed: Normal operation, connections are created
 * - Open: Failures exceeded threshold, fail fast
 * - HalfOpen: Reset timeout elapsed, ready to accept one test request
 * - Probing: HalfOpen and one fiber is actively verifying service recovery
 */
trait CircuitBreaker[F[_]]:

  /**
   * Execute an action through the circuit breaker.
   *
   * @param action the action to protect
   * @return the result or failure if circuit is open
   */
  def protect[A](action: F[A]): F[A]

  /**
   * Get the current state of the circuit breaker.
   */
  def state: F[CircuitBreaker.State]

  /**
   * Reset the circuit breaker to closed state.
   */
  def reset: F[Unit]

object CircuitBreaker:

  enum State:
    case Closed
    case Open
    case HalfOpen
    case Probing

  case class Config(
    maxFailures:              Int            = 5,
    resetTimeout:             FiniteDuration = 60.seconds,
    exponentialBackoffFactor: Double         = 2.0,
    maxResetTimeout:          FiniteDuration = 5.minutes
  )

  /**
   * Create a new circuit breaker.
   * 
   * @param config circuit breaker configuration
   * @tparam F the effect type
   * @return a new circuit breaker instance
   */
  def apply[F[_]: Temporal](
    config: Config = Config()
  ): F[CircuitBreaker[F]] =
    for
      stateRef            <- Ref[F].of[State](State.Closed)
      failures            <- Ref[F].of(0)
      lastFailureTime     <- Ref[F].of(0L)
      currentResetTimeout <- Ref[F].of(config.resetTimeout)
    yield new CircuitBreakerImpl[F](
      config,
      stateRef,
      failures,
      lastFailureTime,
      currentResetTimeout
    )

  private class CircuitBreakerImpl[F[_]: Temporal](
    config:                 Config,
    stateRef:               Ref[F, State],
    failuresRef:            Ref[F, Int],
    lastFailureTimeRef:     Ref[F, Long],
    currentResetTimeoutRef: Ref[F, FiniteDuration]
  ) extends CircuitBreaker[F]:

    override def protect[A](action: F[A]): F[A] =
      stateRef.get.flatMap {
        case State.Closed =>
          action.handleErrorWith { error =>
            recordFailure >> Temporal[F].raiseError(error)
          }

        case State.Open =>
          checkIfShouldTransitionToHalfOpen.flatMap { shouldTransition =>
            if shouldTransition then
              // Atomically claim the HalfOpen transition: only the first fiber wins
              stateRef.modify {
                case State.Open => (State.HalfOpen, true)
                case other      => (other, false)
              }.flatMap {
                case true  => protect(action)
                case false => Temporal[F].raiseError(new SQLException("Circuit breaker is open"))
              }
            else
              // Still open, fail fast
              Temporal[F].raiseError(
                new SQLException("Circuit breaker is open")
              )
          }

        case State.HalfOpen | State.Probing =>
          // Atomically claim the single test slot: only one fiber transitions to Probing
          stateRef.modify {
            case State.HalfOpen => (State.Probing, true)
            case other          => (other, false)
          }.flatMap {
            case false =>
              // Another fiber is already probing, fail fast
              Temporal[F].raiseError(new SQLException("Circuit breaker is open"))
            case true =>
              action
                .handleErrorWith { error =>
                  // Failed again, back to open with increased timeout
                  stateRef.set(State.Open) *>
                    failuresRef.set(0) *>
                    currentResetTimeoutRef.get.flatMap { currentTimeout =>
                      val newTimeout = FiniteDuration(
                        (currentTimeout.toNanos * config.exponentialBackoffFactor).toLong
                          .min(config.maxResetTimeout.toNanos),
                        TimeUnit.NANOSECONDS
                      )
                      currentResetTimeoutRef.set(newTimeout)
                    } *>
                    Clock[F].realTime.flatMap(now => lastFailureTimeRef.set(now.toMillis)) *>
                    Temporal[F].raiseError[A](error)
                }
                .flatMap { result =>
                  // Success, close the circuit
                  reset.as(result)
                }
                .guarantee {
                  // On cancellation, restore Probing -> HalfOpen so the next fiber can retry.
                  // On success or failure, state is already Closed / Open, so this is a no-op.
                  stateRef.modify {
                    case State.Probing => (State.HalfOpen, ())
                    case other         => (other, ())
                  }.void
                }
          }
      }

    private def recordFailure: F[Unit] =
      failuresRef.updateAndGet(_ + 1).flatMap { failures =>
        if failures >= config.maxFailures then
          for
            _ <- stateRef.set(State.Open)
            _ <- Clock[F].realTime.flatMap(now => lastFailureTimeRef.set(now.toMillis))
          yield ()
        else Temporal[F].unit
      }

    private def checkIfShouldTransitionToHalfOpen: F[Boolean] =
      for
        lastFailure  <- lastFailureTimeRef.get
        now          <- Clock[F].realTime.map(_.toMillis)
        resetTimeout <- currentResetTimeoutRef.get
        elapsed = now - lastFailure
      yield elapsed >= resetTimeout.toMillis

    override def state: F[State] = stateRef.get

    override def reset: F[Unit] =
      stateRef.set(State.Closed) >>
        failuresRef.set(0) >>
        currentResetTimeoutRef.set(config.resetTimeout)
