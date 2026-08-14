/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js

import ldbc.fx.Fx

/**
 * Scala.js engine over node's async `net` module (event loop, non-blocking): `connect` opens a
 * node socket, bounds the TCP handshake with a timer, and wraps the result in a [[NodeSocket]].
 * NOTE: compile-verified; runtime testing on node is a follow-up (requires a JS test run).
 */
private[net] object PlatformIoEngine:
  private lazy val netModule = js.Dynamic.global.require("net")

  lazy val global: IoEngine = new IoEngine:
    override def connect(host: String, port: Int, timeout: FiniteDuration, options: SocketOptions): Fx[Socket] = Fx.async[Socket] { cb =>
      val sock = netModule.connect(port.asInstanceOf[js.Any], host.asInstanceOf[js.Any])
      sock.setNoDelay(options.noDelay.asInstanceOf[js.Any])
      if options.keepAlive then sock.setKeepAlive(true.asInstanceOf[js.Any])
      val done = new AtomicBoolean(false)
      val timer = Fx.sleep(timeout).unsafeRun { _ =>
        if done.compareAndSet(false, true) then
          sock.destroy()
          cb(Left(new ConnectTimeoutException(s"connect to $host:$port timed out after $timeout")))
      }
      sock.on(
        "connect",
        ((() => if done.compareAndSet(false, true) then { timer.cancel(); cb(Right(new NodeSocket(sock))) })): js.Function0[Unit]
      )
      sock.on(
        "error",
        ((_: js.Dynamic) =>
          if done.compareAndSet(false, true) then { timer.cancel(); cb(Left(new RuntimeException("connect error"))) }
        ): js.Function1[js.Dynamic, Unit]
      )
      new Fx.Canceler { override def cancel(): Unit = { timer.cancel(); sock.destroy(); () } }
    }.flatMap(SerializedSocket.apply)
