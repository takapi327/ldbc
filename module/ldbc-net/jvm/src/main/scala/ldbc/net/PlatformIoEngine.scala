/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.channels.Selector

/** JVM platform entry point: starts the daemon selector thread and exposes the [[SelectorEngine]]. */
private[net] object PlatformIoEngine:
  lazy val global: IoEngine =
    val engine = new SelectorEngine(Selector.open())
    val th     = new Thread(() => engine.loop(), "fx-io-engine")
    th.setDaemon(true)
    th.start()
    engine
