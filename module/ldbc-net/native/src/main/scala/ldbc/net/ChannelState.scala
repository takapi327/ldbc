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
