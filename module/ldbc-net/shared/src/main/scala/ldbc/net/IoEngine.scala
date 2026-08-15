/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.concurrent.duration.FiniteDuration

import ldbc.fx.Fx

/** Opens [[Socket]]s. One implementation per platform (JVM=NIO selector, JS=node net, Native=sockets). */
trait IoEngine:
  /**
   * Opens a non-blocking connection, failing with a [[ConnectTimeoutException]] if the TCP handshake
   * is not established within `timeout`, so an unreachable host cannot hang on the connect attempt.
   *
   * `timeout` bounds the TCP handshake only. When `host` is a DNS name, name resolution runs first
   * and is governed by the OS resolver (`resolv.conf`), not by `timeout`; a slow resolver can
   * therefore exceed it. An IP literal skips resolution entirely.
   *
   * @param host    the remote host to connect to
   * @param port    the remote port to connect to
   * @param timeout the maximum time to wait for the TCP handshake (DNS resolution is not bounded by it)
   * @param options the TCP socket options to apply to the connection
   * @return an effect that produces a connected [[Socket]]
   */
  def connect(
    host:    String,
    port:    Int,
    timeout: FiniteDuration,
    options: SocketOptions = SocketOptions.default
  ): Fx[Socket]

object IoEngine:
  /** The platform-default engine. */
  lazy val global: IoEngine = PlatformIoEngine.global

/** Raised when [[IoEngine.connect]] does not establish a connection within its timeout. */
final class ConnectTimeoutException(message: String) extends RuntimeException(message)
