/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * A curated, cross-platform set of TCP client socket options. Only the options that are meaningful for
 * a database client and expressible on every platform (JVM / JS / Native) are exposed; multicast,
 * Unix-domain, and server-only options are deliberately omitted.
 *
 * @param noDelay           `TCP_NODELAY` — disables Nagle's algorithm so small request/response packets
 *                          are sent immediately (on by default, the right choice for query latency)
 * @param keepAlive         `SO_KEEPALIVE` — enables TCP keep-alive probes, useful for detecting dead
 *                          peers on long-lived pooled connections
 * @param sendBufferSize    `SO_SNDBUF` — the send buffer size in bytes, if set (not applied on JS)
 * @param receiveBufferSize `SO_RCVBUF` — the receive buffer size in bytes, if set (not applied on JS)
 */
final case class SocketOptions(
  noDelay:           Boolean     = true,
  keepAlive:         Boolean     = false,
  sendBufferSize:    Option[Int] = None,
  receiveBufferSize: Option[Int] = None
)

object SocketOptions:

  /** The default options: `TCP_NODELAY` on, everything else at the OS default. */
  val default: SocketOptions = SocketOptions()
