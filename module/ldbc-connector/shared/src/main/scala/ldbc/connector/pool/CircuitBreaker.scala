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

/**
 * Circuit breaker pattern implementation for connection creation.
 * 
 * Prevents thundering herd problem when database is down by failing fast
 * instead of attempting to create connections that will likely fail.
 * 
 * States:
 * - Closed: Normal operation, connections are created
 * - Open: Failures exceeded threshold, fail fast
 * - HalfOpen: Testing if service recovered
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
  
  sealed trait State
  case object Closed extends State
  case object Open extends State
  case object HalfOpen extends State
  
  case class Config(
    maxFailures: Int = 5,
    resetTimeout: FiniteDuration = 60.seconds,
    exponentialBackoffFactor: Double = 2.0,
    maxResetTimeout: FiniteDuration = 5.minutes
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
      stateRef <- Ref[F].of[State](Closed)
      failures <- Ref[F].of(0)
      lastFailureTime <- Ref[F].of(0L)
      currentResetTimeout <- Ref[F].of(config.resetTimeout)
    yield new CircuitBreakerImpl[F](
      config,
      stateRef,
      failures,
      lastFailureTime,
      currentResetTimeout
    )
  
  private class CircuitBreakerImpl[F[_]: Temporal](
    config: Config,
    stateRef: Ref[F, State],
    failuresRef: Ref[F, Int],
    lastFailureTimeRef: Ref[F, Long],
    currentResetTimeoutRef: Ref[F, FiniteDuration]
  ) extends CircuitBreaker[F]:
    
    override def protect[A](action: F[A]): F[A] =
      stateRef.get.flatMap {
        case Closed =>
          action.handleErrorWith { error =>
            recordFailure >> Temporal[F].raiseError(error)
          }
          
        case Open =>
          checkIfShouldTransitionToHalfOpen.flatMap { shouldTransition =>
            if shouldTransition then
              // Transition to half-open and try
              stateRef.set(HalfOpen) >> protect(action)
            else
              // Still open, fail fast
              Temporal[F].raiseError(
                new Exception("Circuit breaker is open")
              )
          }
          
        case HalfOpen =>
          // Single test request
          action.handleErrorWith { error =>
            // Failed again, back to open with increased timeout
            for
              _ <- stateRef.set(Open)
              _ <- failuresRef.set(0)
              currentTimeout <- currentResetTimeoutRef.get
              newTimeout = FiniteDuration(
                (currentTimeout.toNanos * config.exponentialBackoffFactor).toLong
                  .min(config.maxResetTimeout.toNanos),
                TimeUnit.NANOSECONDS
              )
              _ <- currentResetTimeoutRef.set(newTimeout)
              _ <- Clock[F].realTime.flatMap(now => 
                lastFailureTimeRef.set(now.toMillis)
              )
              _ <- Temporal[F].raiseError[A](error)
            yield ???  // Never reached due to raiseError
          }.flatMap { result =>
            // Success, close the circuit
            reset.as(result)
          }
      }
    
    private def recordFailure: F[Unit] =
      failuresRef.updateAndGet(_ + 1).flatMap { failures =>
        if failures >= config.maxFailures then
          for
            _ <- stateRef.set(Open)
            _ <- Clock[F].realTime.flatMap(now => 
              lastFailureTimeRef.set(now.toMillis)
            )
          yield ()
        else
          Temporal[F].unit
      }
    
    private def checkIfShouldTransitionToHalfOpen: F[Boolean] =
      for
        lastFailure <- lastFailureTimeRef.get
        now <- Clock[F].realTime.map(_.toMillis)
        resetTimeout <- currentResetTimeoutRef.get
        elapsed = now - lastFailure
      yield elapsed >= resetTimeout.toMillis
    
    override def state: F[State] = stateRef.get
    
    override def reset: F[Unit] =
      stateRef.set(Closed) >>
      failuresRef.set(0) >>
      currentResetTimeoutRef.set(config.resetTimeout)