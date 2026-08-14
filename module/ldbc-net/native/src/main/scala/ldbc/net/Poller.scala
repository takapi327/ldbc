/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * Readiness multiplexer abstraction over `epoll` (Linux) and `kqueue` (macOS). All methods are
 * invoked only from the single poller thread except [[wakeup]], which is thread-safe.
 */
private[net] trait Poller:
  /** Adds `fd` to the interest set (idempotent). */
  def add(fd: Int): Unit

  /** Arms one-shot readiness for `fd` in the given direction (auto-disabled after one event). */
  def arm(fd: Int, read: Boolean, write: Boolean): Unit

  /** Removes `fd` from the interest set. */
  def remove(fd: Int): Unit

  /**
   * Blocks until events are available, invoking `onEvent(fd, readable, writable, error)` for each.
   * The wakeup fd is handled internally and never surfaced.
   */
  def poll(onEvent: (Int, Boolean, Boolean, Boolean) => Unit): Unit

  /** Wakes a thread blocked in [[poll]]. Thread-safe. */
  def wakeup(): Unit
