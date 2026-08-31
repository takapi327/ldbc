/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.{ Deferred, Ref }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

class CircuitBreakerTest extends FxSuite:

  test("CircuitBreaker should start in closed state") {
    CircuitBreaker().flatMap(cb => cb.state.map(s => assertEquals(s, CircuitBreaker.State.Closed)))
  }

  test("CircuitBreaker should allow successful operations in closed state") {
    CircuitBreaker().flatMap { cb =>
      for
        result <- cb.protect(Fx.pure(42))
        state  <- cb.state
      yield
        assertEquals(result, 42)
        assertEquals(state, CircuitBreaker.State.Closed)
    }
  }

  test("CircuitBreaker should transition to open state after maxFailures") {
    val config = CircuitBreaker.Config(maxFailures = 3)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _     <- cb.protect(Fx.raiseError(new Exception("fail 1"))).attempt
        _     <- cb.protect(Fx.raiseError(new Exception("fail 2"))).attempt
        _     <- cb.protect(Fx.raiseError(new Exception("fail 3"))).attempt
        state <- cb.state
      yield assertEquals(state, CircuitBreaker.State.Open)
    }
  }

  test("CircuitBreaker should fail fast when open") {
    val config = CircuitBreaker.Config(maxFailures = 1)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _      <- cb.protect(Fx.raiseError(new Exception("fail"))).attempt
        result <- cb.protect(Fx.pure("should not execute")).attempt
        state  <- cb.state
      yield
        assert(result.isLeft)
        result.left.foreach(error => assert(error.getMessage.contains("Circuit breaker is open")))
        assertEquals(state, CircuitBreaker.State.Open)
    }
  }

  test("CircuitBreaker should transition to half-open after reset timeout") {
    val config = CircuitBreaker.Config(maxFailures = 1, resetTimeout = 100.millis)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _      <- cb.protect(Fx.raiseError(new Exception("fail"))).attempt
        _      <- Fx.sleep(150.millis)
        result <- cb.protect(Fx.pure("success"))
        state  <- cb.state
      yield
        assertEquals(result, "success")
        assertEquals(state, CircuitBreaker.State.Closed)
    }
  }

  test("CircuitBreaker should return to open with increased timeout after half-open failure") {
    val config = CircuitBreaker.Config(maxFailures = 1, resetTimeout = 200.millis, exponentialBackoffFactor = 2.0)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _          <- cb.protect(Fx.raiseError(new Exception("fail 1"))).attempt
        _          <- Fx.sleep(300.millis)
        _          <- cb.protect(Fx.raiseError(new Exception("fail 2"))).attempt
        state1     <- cb.state
        _          <- Fx.sleep(100.millis)
        testResult <- cb.protect(Fx.pure("test")).attempt
        _          <- Fx.delay {
               assert(testResult.isLeft)
               testResult.left.foreach(error => assert(error.getMessage.contains("Circuit breaker is open")))
             }
        _           <- Fx.sleep(500.millis)
        finalResult <- cb.protect(Fx.pure("finally success"))
        finalState  <- cb.state
      yield
        assertEquals(state1, CircuitBreaker.State.Open)
        assertEquals(finalResult, "finally success")
        assertEquals(finalState, CircuitBreaker.State.Closed)
    }
  }

  test("CircuitBreaker reset should reset state and counters") {
    val config = CircuitBreaker.Config(maxFailures = 2)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _     <- cb.protect(Fx.raiseError(new Exception("fail"))).attempt
        _     <- cb.reset
        _     <- cb.protect(Fx.raiseError(new Exception("fail again"))).attempt
        state <- cb.state
      yield assertEquals(state, CircuitBreaker.State.Closed)
    }
  }

  test("CircuitBreaker should handle concurrent operations") {
    val config = CircuitBreaker.Config(maxFailures = 5)
    CircuitBreaker[Fx](config).flatMap { cb =>
      val successOp = cb.protect(Fx.pure(1))
      val failOp    = cb.protect(Fx.raiseError[Int](new Exception("fail")))
      for
        results <- (1 to 10).toList.traverse(i => if i % 2 == 0 then successOp.attempt else failOp.attempt)
        state   <- cb.state
      yield
        val successes = results.count(_.isRight)
        val failures  = results.count(_.isLeft)
        assert(successes > 0)
        assert(failures > 0)
        assert(
          state == CircuitBreaker.State.Open || state == CircuitBreaker.State.Closed,
          s"Expected Open or Closed state, got $state"
        )
    }
  }

  test("CircuitBreaker should respect maxResetTimeout") {
    val config = CircuitBreaker.Config(
      maxFailures              = 1,
      resetTimeout             = 10.millis,
      exponentialBackoffFactor = 10.0,
      maxResetTimeout          = 50.millis
    )
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _      <- cb.protect(Fx.raiseError(new Exception("fail 1"))).attempt
        _      <- Fx.sleep(20.millis)
        _      <- cb.protect(Fx.raiseError(new Exception("fail 2"))).attempt
        _      <- Fx.sleep(60.millis)
        result <- cb.protect(Fx.pure("success"))
      yield assertEquals(result, "success")
    }
  }

  test("CircuitBreaker should count only consecutive failures") {
    val config = CircuitBreaker.Config(maxFailures = 3)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _              <- cb.protect(Fx.raiseError(new Exception("fail 1"))).attempt
        _              <- cb.protect(Fx.pure("success 1"))
        _              <- cb.protect(Fx.raiseError(new Exception("fail 2"))).attempt
        _              <- cb.protect(Fx.pure("success 2"))
        stateBeforeMax <- cb.state
        _              <- cb.protect(Fx.raiseError(new Exception("fail 3"))).attempt
        _              <- cb.protect(Fx.raiseError(new Exception("fail 4"))).attempt
        _              <- cb.protect(Fx.raiseError(new Exception("fail 5"))).attempt
        stateAfterMax  <- cb.state
      yield
        assertEquals(stateBeforeMax, CircuitBreaker.State.Closed)
        assertEquals(stateAfterMax, CircuitBreaker.State.Open)
    }
  }

  test("CircuitBreaker should handle successful operations after failures") {
    val config = CircuitBreaker.Config(maxFailures = 3)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        _     <- cb.protect(Fx.raiseError(new Exception("fail 1"))).attempt
        _     <- cb.protect(Fx.raiseError(new Exception("fail 2"))).attempt
        _     <- cb.protect(Fx.pure("success"))
        _     <- cb.protect(Fx.raiseError(new Exception("fail 3"))).attempt
        state <- cb.state
      yield assertEquals(state, CircuitBreaker.State.Open)
    }
  }

  test("CircuitBreaker with custom config") {
    val config = CircuitBreaker.Config(
      maxFailures              = 10,
      resetTimeout             = 1.minute,
      exponentialBackoffFactor = 1.5,
      maxResetTimeout          = 10.minutes
    )
    CircuitBreaker[Fx](config).flatMap(cb => cb.state.map(s => assertEquals(s, CircuitBreaker.State.Closed)))
  }

  test("CircuitBreaker HalfOpen: only one concurrent fiber should be allowed as test request") {
    val config = CircuitBreaker.Config(maxFailures = 1, resetTimeout = 10.millis)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        counter <- Ref.of(0)
        gate    <- Deferred[Fx, Unit]
        _       <- cb.protect(Fx.raiseError(new Exception("fail"))).attempt
        _       <- Fx.sleep(20.millis)
        fibers  <- (1 to 10).toList.traverse { _ =>
                    (cb.protect(counter.update(_ + 1) >> gate.get).attempt).start
                  }
        _     <- Fx.sleep(50.millis)
        _     <- gate.complete(())
        _     <- fibers.traverse_(_.join)
        count <- counter.get
      yield assertEquals(count, 1, s"Only 1 fiber should enter HalfOpen as test request, but $count did")
    }
  }

  test("CircuitBreaker Probing: concurrent requests should be rejected while one fiber is probing") {
    val config = CircuitBreaker.Config(maxFailures = 1, resetTimeout = 10.millis)
    CircuitBreaker[Fx](config).flatMap { cb =>
      for
        latch        <- Deferred[Fx, Unit]
        started      <- Deferred[Fx, Unit]
        _            <- cb.protect(Fx.raiseError(new Exception("fail"))).attempt
        _            <- Fx.sleep(20.millis)
        f1           <- cb.protect(started.complete(()) >> latch.get >> Fx.pure(())).start
        _            <- started.get
        fiber2Result <- cb.protect(Fx.raiseError(new Exception("concurrent attempt"))).attempt
        _            <- latch.complete(())
        _            <- f1.join
        finalState   <- cb.state
        afterResult  <- cb.protect(Fx.pure("recovered")).attempt
      yield
        assert(fiber2Result.isLeft, "Concurrent request should be rejected while probing")
        fiber2Result.left.foreach { err =>
          assert(err.getMessage.contains("Circuit breaker is open"), s"Unexpected error: ${ err.getMessage }")
        }
        assertEquals(finalState, CircuitBreaker.State.Closed)
        assert(afterResult.isRight, "Requests should succeed after circuit is closed")
    }
  }
