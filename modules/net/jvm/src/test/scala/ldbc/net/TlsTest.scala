/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.security.KeyStore
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import javax.net.ssl.{ KeyManagerFactory, SSLContext, SSLServerSocket }

import scala.concurrent.duration.*

import ldbc.fx.concurrentFx
import ldbc.fx.Fx
import ldbc.net.TlsUpgrade

/**
 * JVM TLS integration tests (design §11): a keytool-generated self-signed server (SAN =
 * dns:localhost, ip:127.0.0.1) is spun up locally, then the [[Tls]] client is exercised for the
 * echo round-trip, chain validation (`System` must reject a self-signed peer), and hostname
 * verification (mismatch rejected exactly when `verifyHostname` is on).
 */
class TlsTest extends munit.FunSuite:

  private val engine = ldbc.net.IoEngine.fromRaw[Fx](PlatformRawEngine.global)

  private val password = "changeit"

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 15000): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("timeout")) else ref.get()

  /** Generates a PKCS12 keystore + PEM cert via keytool once for the suite. */
  private lazy val (keystorePath, certPem): (Path, String) =
    val dir      = Files.createTempDirectory("ldbc-tls-test")
    val keystore = dir.resolve("server.p12")
    val certFile = dir.resolve("server.pem")
    val keytool  = Paths.get(System.getProperty("java.home"), "bin", "keytool").toString
    def run(args: String*): Unit =
      val process = new ProcessBuilder((keytool +: args)*).redirectErrorStream(true).start()
      val out     = scala.io.Source.fromInputStream(process.getInputStream).mkString
      assert(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0, s"keytool failed: $out")
    run(
      "-genkeypair",
      "-alias",
      "test",
      "-keyalg",
      "RSA",
      "-keysize",
      "2048",
      "-validity",
      "1",
      "-keystore",
      keystore.toString,
      "-storetype",
      "PKCS12",
      "-storepass",
      password,
      "-keypass",
      password,
      "-dname",
      "CN=ldbc-test",
      "-ext",
      "SAN=dns:localhost,ip:127.0.0.1"
    )
    run(
      "-exportcert",
      "-alias",
      "test",
      "-keystore",
      keystore.toString,
      "-storepass",
      password,
      "-rfc",
      "-file",
      certFile.toString
    )
    (keystore, new String(Files.readAllBytes(certFile), StandardCharsets.US_ASCII))

  /** Starts a TLS echo server backed by the generated keystore; returns its port. */
  private lazy val port: Int =
    val ks = KeyStore.getInstance("PKCS12")
    val in = new FileInputStream(keystorePath.toFile)
    try ks.load(in, password.toCharArray)
    finally in.close()
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(ks, password.toCharArray)
    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, null, null)
    val server    = context.getServerSocketFactory.createServerSocket(0).asInstanceOf[SSLServerSocket]
    val accepting = new Thread(() =>
      while true do
        val socket = server.accept()
        val worker = new Thread(() =>
          try
            val input  = socket.getInputStream
            val output = socket.getOutputStream
            val buffer = new Array[Byte](1024)
            var read   = input.read(buffer)
            while read >= 0 do
              output.write(buffer, 0, read)
              output.flush()
              read = input.read(buffer)
          catch { case _: Throwable => () }
        )
        worker.setDaemon(true)
        worker.start()
    )
    accepting.setDaemon(true)
    accepting.start()
    server.getLocalPort

  private def echoOver(host: String, config: SSL, message: String): Fx[String] =
    for
      plain <- engine.connect("127.0.0.1", port, 5.seconds)
      tls   <- summon[TlsUpgrade[Fx]].client(plain, host, port, config)
      _     <- tls.write(message.getBytes("UTF-8"))
      bytes <- tls.read(1024)
      _     <- tls.close()
    yield new String(bytes.getOrElse(Array.emptyByteArray), "UTF-8")

  test("TLS echo round-trip with Trusted (handshake + wrap/unwrap data path)"):
    assertEquals(runSync(echoOver("localhost", SSL.Trusted, "PING-TLS")), Right("PING-TLS"))

  test("large payload round-trips across TLS record boundaries"):
    val payload = "x" * 50000
    val prog    =
      for
        plain <- engine.connect("127.0.0.1", port, 5.seconds)
        tls   <- summon[TlsUpgrade[Fx]].client(plain, "localhost", port, SSL.Trusted)
        _     <- tls.write(payload.getBytes("UTF-8"))
        got   <- readFully(tls, payload.length)
        _     <- tls.close()
      yield got
    assertEquals(runSync(prog).map(_.length), Right(payload.length))

  private def readFully(socket: ldbc.net.Socket[Fx], total: Int): Fx[String] =
    def loop(acc: Array[Byte]): Fx[Array[Byte]] =
      if acc.length >= total then Fx.pure(acc)
      else
        socket.read(total - acc.length).flatMap {
          case None        => Fx.pure(acc)
          case Some(chunk) => if chunk.isEmpty then Fx.pure(acc) else loop(acc ++ chunk)
        }
    loop(Array.emptyByteArray).map(new String(_, "UTF-8"))

  test("System rejects a self-signed server (chain validation)"):
    val result = runSync(echoOver("localhost", SSL.System, "X"))
    assert(result.isLeft, s"expected chain-validation failure, got $result")

  test("FromPemCerts + matching hostname succeeds with verification on"):
    val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = true)
    assertEquals(runSync(echoOver("localhost", config, "VERIFIED")), Right("VERIFIED"))

  test("hostname mismatch is rejected when verifyHostname is on"):
    val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = true)
    val result = runSync(echoOver("wrong.example.com", config, "X"))
    assert(result.isLeft, s"expected hostname-verification failure, got $result")

  test("the same mismatch is accepted when verifyHostname is off (proves the check is the gate)"):
    val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = false)
    assertEquals(runSync(echoOver("wrong.example.com", config, "UNCHECKED")), Right("UNCHECKED"))
