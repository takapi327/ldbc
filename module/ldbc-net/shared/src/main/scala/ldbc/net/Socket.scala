/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import ldbc.fx.Fx

/**
 * Non-blocking byte transport. `read`/`write` suspend an [[ldbc.fx.Fx]] rather than blocking a thread.
 * DB-agnostic: this is "read/write N bytes"; per-database packet framing lives in the driver modules.
 */
trait Socket:
  /**
   * Reads up to `n` bytes. End-of-stream is signalled out of band, mirroring fs2's `Socket.read`:
   * `Some(bytes)` carries received data, `Some` of an empty array is the (unambiguous) result of a
   * `n <= 0` request, and `None` means the peer closed the stream.
   *
   * @param n the maximum number of bytes to read
   * @return `Some` of up to `n` bytes, or `None` at end of stream
   */
  def read(n: Int): Fx[Option[Array[Byte]]]

  /** Write all `bytes`. */
  def write(bytes: Array[Byte]): Fx[Unit]

  /** Closes the socket and releases its underlying channel. Safe to call more than once. */
  def close(): Fx[Unit]
