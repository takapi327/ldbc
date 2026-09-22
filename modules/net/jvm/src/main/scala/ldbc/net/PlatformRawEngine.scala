/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/** JVM platform default [[RawIoEngine]]: the shared daemon-selector [[NioRawEngine]]. */
private[net] object PlatformRawEngine:
  def global: RawIoEngine = NioRawEngine.global

  /** An engine of its own, so a test can drive failure paths without disturbing [[global]]. */
  private[net] def startIsolated(name: String): RawIoEngine & PollerDiagnostics = NioRawEngine.start(name)
