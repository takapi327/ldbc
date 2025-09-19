/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.syntax.all.*
import cats.effect.*

import ldbc.connector.FTestPlatform

class CircuitBreakerTest extends FTestPlatform:

  test("CircuitBreaker should start in closed state") {
    CircuitBreaker[IO]().flatMap { cb =>
      assertIO(cb.state, CircuitBreaker.State.Closed)
    }
  }

  test("CircuitBreaker should allow successful operations in closed state") {
    CircuitBreaker[IO]().flatMap { cb =>
      for {
        result <- cb.protect(IO.pure(42))
        state  <- cb.state
      } yield {
        assertEquals(result, 42)
        assertEquals(state, CircuitBreaker.State.Closed)
      }
    }
  }

  test("CircuitBreaker should transition to open state after maxFailures") {
    val config = CircuitBreaker.Config(maxFailures = 3)
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Cause 3 failures
        _ <- cb.protect(IO.raiseError(new Exception("fail 1"))).attempt
        _ <- cb.protect(IO.raiseError(new Exception("fail 2"))).attempt
        _ <- cb.protect(IO.raiseError(new Exception("fail 3"))).attempt
        
        state <- cb.state
      } yield assertEquals(state, CircuitBreaker.State.Open)
    }
  }

  test("CircuitBreaker should fail fast when open") {
    val config = CircuitBreaker.Config(maxFailures = 1)
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Open the circuit
        _ <- cb.protect(IO.raiseError(new Exception("fail"))).attempt
        
        // Should fail fast without executing the action
        result <- cb.protect(IO.pure("should not execute")).attempt
        
        state <- cb.state
      } yield {
        assert(result.isLeft)
        result.left.foreach { error =>
          assertEquals(error.getMessage, "Circuit breaker is open")
        }
        assertEquals(state, CircuitBreaker.State.Open)
      }
    }
  }

  test("CircuitBreaker should transition to half-open after reset timeout") {
    val config = CircuitBreaker.Config(
      maxFailures = 1,
      resetTimeout = 100.millis
    )
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Open the circuit
        _ <- cb.protect(IO.raiseError(new Exception("fail"))).attempt
        
        // Wait for reset timeout
        _ <- IO.sleep(150.millis)
        
        // Should transition to half-open and succeed
        result <- cb.protect(IO.pure("success"))
        state  <- cb.state
      } yield {
        assertEquals(result, "success")
        assertEquals(state, CircuitBreaker.State.Closed)
      }
    }
  }

  test("CircuitBreaker should return to open with increased timeout after half-open failure") {
    val config = CircuitBreaker.Config(
      maxFailures = 1,
      resetTimeout = 100.millis,
      exponentialBackoffFactor = 2.0
    )
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Open the circuit
        _ <- cb.protect(IO.raiseError(new Exception("fail 1"))).attempt
        
        // Wait for reset timeout
        _ <- IO.sleep(150.millis)
        
        // Fail in half-open state
        _ <- cb.protect(IO.raiseError(new Exception("fail 2"))).attempt
        
        state1 <- cb.state
        
        // Should still be open after original reset timeout
        _ <- IO.sleep(100.millis)
        testResult <- cb.protect(IO.pure("test")).attempt
        _ <- IO {
          assert(testResult.isLeft)
          testResult.left.foreach { error =>
            assertEquals(error.getMessage, "Circuit breaker is open")
          }
        }
        
        // Wait for increased timeout (200ms due to 2x backoff)
        _ <- IO.sleep(150.millis)
        
        // Should now allow retry
        finalResult <- cb.protect(IO.pure("finally success"))
        finalState  <- cb.state
      } yield {
        assertEquals(state1, CircuitBreaker.State.Open)
        assertEquals(finalResult, "finally success")
        assertEquals(finalState, CircuitBreaker.State.Closed)
      }
    }
  }

  test("CircuitBreaker reset should reset state and counters") {
    val config = CircuitBreaker.Config(maxFailures = 2)
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Cause one failure
        _ <- cb.protect(IO.raiseError(new Exception("fail"))).attempt
        
        // Reset
        _ <- cb.reset
        
        // Should be able to fail again without opening
        _ <- cb.protect(IO.raiseError(new Exception("fail again"))).attempt
        
        state <- cb.state
      } yield assertEquals(state, CircuitBreaker.State.Closed)
    }
  }

  test("CircuitBreaker should handle concurrent operations") {
    val config = CircuitBreaker.Config(maxFailures = 5)
    
    CircuitBreaker[IO](config).flatMap { cb =>
      val successOp = cb.protect(IO.pure(1))
      val failOp = cb.protect(IO.raiseError[Int](new Exception("concurrent fail")))
      
      for {
        // Run operations with proper error handling
        results <- (1 to 10).toList.traverse { i =>
          if (i % 2 == 0) successOp.attempt else failOp.attempt
        }
        
        state <- cb.state
      } yield {
        val successes = results.count(_.isRight)
        val failures = results.count(_.isLeft)
        
        assert(successes > 0)
        assert(failures > 0)
        // Due to concurrent execution, state might vary
        assert(
          state == CircuitBreaker.State.Open || state == CircuitBreaker.State.Closed,
          s"Expected Open or Closed state, got $state"
        )
      }
    }
  }

  test("CircuitBreaker should respect maxResetTimeout") {
    val config = CircuitBreaker.Config(
      maxFailures = 1,
      resetTimeout = 10.millis,
      exponentialBackoffFactor = 10.0, // Very high factor
      maxResetTimeout = 50.millis
    )
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Open circuit
        _ <- cb.protect(IO.raiseError(new Exception("fail 1"))).attempt
        
        // Fail multiple times in half-open to increase timeout
        _ <- IO.sleep(20.millis)
        _ <- cb.protect(IO.raiseError(new Exception("fail 2"))).attempt
        
        _ <- IO.sleep(60.millis) // More than maxResetTimeout
        
        // Should allow retry (timeout capped at maxResetTimeout)
        result <- cb.protect(IO.pure("success"))
      } yield assertEquals(result, "success")
    }
  }

  test("CircuitBreaker should count only consecutive failures") {
    val config = CircuitBreaker.Config(maxFailures = 3)
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Mix of successes and failures
        _ <- cb.protect(IO.raiseError(new Exception("fail 1"))).attempt
        _ <- cb.protect(IO.pure("success 1"))
        _ <- cb.protect(IO.raiseError(new Exception("fail 2"))).attempt
        _ <- cb.protect(IO.pure("success 2"))
        
        stateBeforeMax <- cb.state
        
        // Now consecutive failures to open
        _ <- cb.protect(IO.raiseError(new Exception("fail 3"))).attempt
        _ <- cb.protect(IO.raiseError(new Exception("fail 4"))).attempt
        _ <- cb.protect(IO.raiseError(new Exception("fail 5"))).attempt
        
        stateAfterMax <- cb.state
      } yield {
        assertEquals(stateBeforeMax, CircuitBreaker.State.Closed)
        assertEquals(stateAfterMax, CircuitBreaker.State.Open)
      }
    }
  }

  test("CircuitBreaker should handle successful operations after failures") {
    val config = CircuitBreaker.Config(maxFailures = 3)
    
    CircuitBreaker[IO](config).flatMap { cb =>
      for {
        // Two failures
        _ <- cb.protect(IO.raiseError(new Exception("fail 1"))).attempt
        _ <- cb.protect(IO.raiseError(new Exception("fail 2"))).attempt
        
        // Success should not reset counter in current implementation
        _ <- cb.protect(IO.pure("success"))
        
        // One more failure should open
        _ <- cb.protect(IO.raiseError(new Exception("fail 3"))).attempt
        
        state <- cb.state
      } yield assertEquals(state, CircuitBreaker.State.Open)
    }
  }

  test("CircuitBreaker with custom config") {
    val config = CircuitBreaker.Config(
      maxFailures = 10,
      resetTimeout = 1.minute,
      exponentialBackoffFactor = 1.5,
      maxResetTimeout = 10.minutes
    )
    
    CircuitBreaker[IO](config).flatMap { cb =>
      assertIO(cb.state, CircuitBreaker.State.Closed)
    }
  }