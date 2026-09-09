/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/** Scala Native platform default [[RawIoEngine]]: the epoll/kqueue [[FdRawEngine]]. */
private[net] object PlatformRawEngine:
  def global: RawIoEngine = FdRawEngine.global
