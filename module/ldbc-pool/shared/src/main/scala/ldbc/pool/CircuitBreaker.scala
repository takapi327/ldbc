/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.*

import ldbc.effect.{ Async, Ref }
import ldbc.effect.syntax.*

import ldbc.sql.SQLException

/** Guards connection creation with a circuit breaker (closed/open/half-open), generic over `F`. */
trait CircuitBreaker[F[_]]:
  def protect[A](action: F[A]): F[A]
  def state: F[CircuitBreaker.State]
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

  def apply[F[_]](config: Config = Config())(using F: Async[F]): F[CircuitBreaker[F]] =
    for
      stateRef            <- Ref.of[F, State](State.Closed)
      failures            <- Ref.of[F, Int](0)
      lastFailureTime     <- Ref.of[F, Long](0L)
      currentResetTimeout <- Ref.of[F, FiniteDuration](config.resetTimeout)
    yield new CircuitBreakerImpl(config, stateRef, failures, lastFailureTime, currentResetTimeout)

  private class CircuitBreakerImpl[F[_]](
    config:                 Config,
    stateRef:               Ref[F, State],
    failuresRef:            Ref[F, Int],
    lastFailureTimeRef:     Ref[F, Long],
    currentResetTimeoutRef: Ref[F, FiniteDuration]
  )(using F: Async[F])
    extends CircuitBreaker[F]:

    private def guarantee[A](action: F[A])(fin: F[Unit]): F[A] =
      F.bracket(F.unit)((_: Unit) => action)((_: Unit) => fin)

    private def nowMillis: F[Long] = F.delay(System.currentTimeMillis())

    override def protect[A](action: F[A]): F[A] =
      stateRef.get.flatMap {
        case State.Closed =>
          action.handleErrorWith(error => recordFailure.flatMap(_ => F.raiseError(error)))

        case State.Open =>
          checkIfShouldTransitionToHalfOpen.flatMap { shouldTransition =>
            if shouldTransition then
              stateRef
                .modify {
                  case State.Open => (State.HalfOpen, true)
                  case other      => (other, false)
                }
                .flatMap {
                  case true  => protect(action)
                  case false => F.raiseError(new SQLException("Circuit breaker is open"))
                }
            else F.raiseError(new SQLException("Circuit breaker is open"))
          }

        case State.HalfOpen | State.Probing =>
          stateRef
            .modify {
              case State.HalfOpen => (State.Probing, true)
              case other          => (other, false)
            }
            .flatMap {
              case false => F.raiseError(new SQLException("Circuit breaker is open"))
              case true =>
                guarantee(
                  action
                    .handleErrorWith { error =>
                      stateRef
                        .set(State.Open)
                        .flatMap(_ => failuresRef.set(0))
                        .flatMap(_ => currentResetTimeoutRef.get)
                        .flatMap { currentTimeout =>
                          val newTimeout = FiniteDuration(
                            (currentTimeout.toNanos * config.exponentialBackoffFactor).toLong
                              .min(config.maxResetTimeout.toNanos),
                            TimeUnit.NANOSECONDS
                          )
                          currentResetTimeoutRef.set(newTimeout)
                        }
                        .flatMap(_ => nowMillis)
                        .flatMap(now => lastFailureTimeRef.set(now))
                        .flatMap(_ => F.raiseError[A](error))
                    }
                    .flatMap(result => reset.map(_ => result))
                )(
                  stateRef.modify {
                    case State.Probing => (State.HalfOpen, ())
                    case other         => (other, ())
                  }
                )
            }
      }

    private def recordFailure: F[Unit] =
      failuresRef.updateAndGet(_ + 1).flatMap { failures =>
        if failures >= config.maxFailures then
          stateRef.set(State.Open).flatMap(_ => nowMillis).flatMap(now => lastFailureTimeRef.set(now))
        else F.unit
      }

    private def checkIfShouldTransitionToHalfOpen: F[Boolean] =
      for
        lastFailure  <- lastFailureTimeRef.get
        now          <- nowMillis
        resetTimeout <- currentResetTimeoutRef.get
      yield (now - lastFailure) >= resetTimeout.toMillis

    override def state: F[State] = stateRef.get

    override def reset: F[Unit] =
      stateRef
        .set(State.Closed)
        .flatMap(_ => failuresRef.set(0))
        .flatMap(_ => currentResetTimeoutRef.set(config.resetTimeout))
