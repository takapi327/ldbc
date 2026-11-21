/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * Mutable per-fd interest/continuation state used by the Scala Native raw engine ([[FdRawEngine]]) and
 * its TLS layer. Each callback is one-shot: the poller nulls it after firing (the interest is registered
 * with ONESHOT semantics so it is auto-disabled after one event).
 */
private[net] final class ChannelState:
  @volatile var connectReady: () => Unit = null
  @volatile var readReady:    () => Unit = null
  @volatile var writeReady:   () => Unit = null

  /**
   * How to report that a continuation will never run.
   *
   * Set alongside the continuation it belongs to, and cleared with it. The engine needs these when it
   * gives up its poller thread: the continuations already armed at that moment have no other way of
   * hearing about it, and whoever is waiting on them is not necessarily the socket — the TLS layer
   * arms readiness with continuations of its own while driving a handshake.
   */
  @volatile var readFailed:  Throwable => Unit = null
  @volatile var writeFailed: Throwable => Unit = null
