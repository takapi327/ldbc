/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * What one [[Poller.poll]] call did.
 *
 * `epoll_wait` and `kevent` report failure by returning `-1` and setting `errno`, not by throwing, so
 * a `catch` around the poller loop cannot see these at all. The distinction that matters is between
 * `EINTR` — the call was merely interrupted by a signal and retrying immediately is correct — and a
 * real error, where retrying immediately would spin the poller thread at full speed.
 */
private[net] enum PollOutcome:
  /** The call succeeded and delivered `n` events (possibly zero). */
  case Events(n: Int)

  /** The call was interrupted by a signal (`EINTR`). Retry immediately; this is not an error. */
  case Retry

  /** The call failed. Retrying immediately would spin, so the caller backs off. */
  case Failed(errno: Int)

private[net] object PollOutcome:

  /**
   * Classifies the raw return value of `epoll_wait` / `kevent`.
   *
   * Kept separate from the pollers so the branch that decides "spin or back off" can be tested
   * without a live multiplexer.
   */
  def classify(result: Int, errno: Int, eintr: Int): PollOutcome =
    if result >= 0 then Events(result)
    else if errno == eintr then Retry
    else Failed(errno)
