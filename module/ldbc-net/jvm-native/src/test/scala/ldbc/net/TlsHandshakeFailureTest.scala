/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.ServerSocket
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import ldbc.fx.Fx

/**
 * Abnormal-case coverage shared by the JVM (JSSE) and Native (s2n) TLS clients: a TLS handshake
 * against a peer that speaks plaintext (e.g. an accidental connection to a non-TLS port) must fail
 * with an error rather than hang. The server sends a plaintext banner instead of a `ServerHello`,
 * so the client's TLS record parser rejects it during the handshake.
 */
class TlsHandshakeFailureTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("timeout")) else ref.get()

  /** Starts a plaintext server that greets every client with a non-TLS banner, then closes. */
  private def startPlaintextServer(): Int =
    val ss = new ServerSocket(0)
    val th = new Thread(() =>
      while true do
        val s = ss.accept()
        val worker = new Thread(() =>
          try
            s.getOutputStream.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes("UTF-8"))
            s.getOutputStream.flush()
            s.close() // hang up so the client's handshake read sees EOF rather than waiting for more bytes
          catch { case _: Throwable => () }
        )
        worker.setDaemon(true)
        worker.start()
    )
    th.setDaemon(true)
    th.start()
    ss.getLocalPort

  private lazy val port = startPlaintextServer()

  test("a TLS handshake against a plaintext peer fails and does not hang"):
    val startNanos = System.nanoTime()
    val prog =
      for
        plain <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
        tls   <- Tls.client(plain, "localhost", port, SSL.Trusted)
      yield tls
    val result    = runSync(prog, 8000)
    val elapsedMs = (System.nanoTime() - startNanos) / 1000000L
    assert(result.isLeft, s"a TLS handshake against a plaintext peer must fail, got $result")
    assert(elapsedMs < 6000, s"a failed handshake must surface promptly, not hang (took ${ elapsedMs }ms)")
