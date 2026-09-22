/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

class PollOutcomeTest extends munit.FunSuite:

  private val eintr = 4

  test("a non-negative result is the event count"):
    assertEquals(PollOutcome.classify(0, 0, eintr), PollOutcome.Events(0))
    assertEquals(PollOutcome.classify(7, 0, eintr), PollOutcome.Events(7))

  test("a negative result with EINTR is retried"):
    assertEquals(PollOutcome.classify(-1, eintr, eintr), PollOutcome.Retry)

  test("a negative result with any other errno is a failure"):
    assertEquals(PollOutcome.classify(-1, 9, eintr), PollOutcome.Failed(9))
    assertEquals(PollOutcome.classify(-1, 22, eintr), PollOutcome.Failed(22))

  test("errno is ignored when the call succeeded"):
    assertEquals(PollOutcome.classify(3, 9, eintr), PollOutcome.Events(3))
