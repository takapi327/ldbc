/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.{ File, FileOutputStream }
import java.net.{ InetSocketAddress, ServerSocket, Socket as JSocket }
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import ldbc.fx.Fx

/**
 * Native s2n TLS integration tests (design Phase 3): an `openssl s_server -www` process backed by an
 * embedded self-signed certificate (SAN = dns:localhost, ip:127.0.0.1) exercises the s2n handshake,
 * chain validation, the per-name `verify_host` callback (shared [[HostnameMatcher]]), and the
 * encrypted data path.
 */
class TlsTest extends munit.FunSuite:

  private val certPem =
    """-----BEGIN CERTIFICATE-----
      |MIIDJTCCAg2gAwIBAgIUJcfNTLKrj9TUWjS5YN4GoDYEN+0wDQYJKoZIhvcNAQEL
      |BQAwFDESMBAGA1UEAwwJbGRiYy10ZXN0MB4XDTI2MDgwOTE0NTgyMloXDTM2MDgw
      |NjE0NTgyMlowFDESMBAGA1UEAwwJbGRiYy10ZXN0MIIBIjANBgkqhkiG9w0BAQEF
      |AAOCAQ8AMIIBCgKCAQEA9qTiEBtUUnvpmpD6Jd9AbiB/Xyveq81KTeaXHKKgaFhM
      |r05DT2zsvBPgR7AVt9hKgcRFG/WGJjMoW4ygGn9j7B2yvlxx101U58xnjrFRg/+3
      |VOfIY3iKns2UUIBR1TZSDwq5iGWT3Xq2j8zAlpI5GXKmqMuGxI56zLu10yPSFaHY
      |qZt6XREFnmi7ExtAsLJx1xrWRDkTSd4c2BQhE/VO3w2vJNcFUb4Zhc5HbavoTQ47
      |UxsURvEtXCl5GI2+mCE8GIMOpbgsWtv8a+4pdWRAjgywaUNQUd4HbTkf6SEiLeRl
      |t7XHjpB6NvqOlHN4RnTVffKODLwNG5Qd7L2sU33/SwIDAQABo28wbTAdBgNVHQ4E
      |FgQUA5h2aRDWWRj3iknxvmXx4eQARScwHwYDVR0jBBgwFoAUA5h2aRDWWRj3iknx
      |vmXx4eQARScwDwYDVR0TAQH/BAUwAwEB/zAaBgNVHREEEzARgglsb2NhbGhvc3SH
      |BH8AAAEwDQYJKoZIhvcNAQELBQADggEBAGk9BYFmrOeDHyt6Gd8viDXtGKgnU3zi
      |B3MZ/kkaKPxLWLPS1MjpxaAWwZgEPjBenapXU3BU/16UBAPeUc94xqj29qAPLOdB
      |jqfwyq1mU2gwto+bo88OQv3BTxm1jD6yWGoc5lh1A2reRLGzLzoDvJhRz+D9pJXE
      |I53/25D7csRQkAxwgiMJl8vFGPJoR4aCpOLmUoZY7DIqNvZU7kwv0N5cCGPjikpa
      |WlbS3jsGQ96s9jkgWRT4HOKDNzSmEmctQROi2ZEKk6qb1Y0SmB39kuqFoeeZA1uA
      |1SjbHWCj+osyU/bo83CqdfHJyBqsfN/XDVqBl2gdLGozzZ7UFreptHw=
      |-----END CERTIFICATE-----
      |""".stripMargin

  private val keyPem =
    """-----BEGIN PRIVATE KEY-----
      |MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQD2pOIQG1RSe+ma
      |kPol30BuIH9fK96rzUpN5pccoqBoWEyvTkNPbOy8E+BHsBW32EqBxEUb9YYmMyhb
      |jKAaf2PsHbK+XHHXTVTnzGeOsVGD/7dU58hjeIqezZRQgFHVNlIPCrmIZZPderaP
      |zMCWkjkZcqaoy4bEjnrMu7XTI9IVodipm3pdEQWeaLsTG0CwsnHXGtZEORNJ3hzY
      |FCET9U7fDa8k1wVRvhmFzkdtq+hNDjtTGxRG8S1cKXkYjb6YITwYgw6luCxa2/xr
      |7il1ZECODLBpQ1BR3gdtOR/pISIt5GW3tceOkHo2+o6Uc3hGdNV98o4MvA0blB3s
      |vaxTff9LAgMBAAECggEAA8EHtdIMqwK60ADz5b6rpuF0vtiWl4vU3TGszCFHir8J
      |T8PDr4JLaiNtTvoFopo2rBn2KVJH2+38Z8RQrYaR2UascUxL6UiCidKpItldEcLD
      |IhYzDRyJE5iycLU7SaD9h8AUidht8zmKbM7jgYcxOktt8PTEFNrMULBsuI17hZu8
      |34I9giaLUqMLl2pSyLBPRRsEhQjJiFjYuXIhPDw7ZMgB5HFVYQKTb5J5d4lkbNL1
      |oLrGNB+p4+iPZAVjnllL2Q+oatU6V1tpPtNgl9BJKc1ptyzmmfLTIYh7QQsNif/i
      |WKQj+2695H6CNn7nsviWW9Kaz9hsHFkq6kZK5Dsc5QKBgQD/fLi4rxfqIV88crDU
      |1uBauTULTcodfFbnw07u7iSrGWGQeM8iL19xFMEA41UHotTLllWUyvPvjsTmOYAR
      |oCwEG/zSKCqyY6yNmKO+Eh+0dDg7QWv/EES0x8EXo+1QFvzepl4aKC/nAR5GifdD
      |TlxuKyzhkvQJzVItims0WeT3lQKBgQD3I54ZvFTdE/nowg6/xQI4oVpGpooEyCtw
      |jASKNd0NwwDEWNIqpiqzsiIuiqHTpiXQ0+T0J3W7Jzrrxtt3ZdKFh12xQ8VhLYdO
      |bYA07kUx3mXXK4hUPw0tFbiBA409OVBnwD9Dxes01YRUGhwsRm0Cvi+h1n9EKqQq
      |RYZlXMnjXwKBgAx/h3HRbvQPKd+FJM1krZkCkmu5JHgIyx+PWF8r2zNbjIBKyKc9
      |dgfMIhzvAgvblVlYQ1uCCijf7RXuHiu+kCirTdTiDiSn55sNqoTz0gU74jxT2Pqj
      |Sxkk5HdJl6RhW4Fw1g2lhaJhVZ/Rw/zQ6oihKBLo43NPTgPYT6JB+4d5AoGAOGM3
      |CnYvArYxGgtU5CpmJFekSmRrL0YgqzA3RPWfNymb/jCp2zNIoPFu3SGiZLEPBcOj
      |BJYQRaBg0DTyFB72VqMZHH3zBgYmTh2r1+fZf9RXdi2nkFlGf1fqXf1ad3KhRtrV
      |VHhO/yIMbEf8z5lN/Ac6xPk01m/IPyDX1j7CjY8CgYEAnv/oquBnYcvIh0V5+HPF
      |4h1XDXxM0FeO6xyHUo6zzImqSUDdJrrIllaJAqaPlB1Sd0qDKeFLfJ1kl4kfwLau
      |0M1fjiywSpAyVKJHoRM+aN6QskuOtSBzhp8xidihx42vlQHWwLXZECyU8IGmm25R
      |BBVmEVwIIzf8yiH/+3cDpp8=
      |-----END PRIVATE KEY-----
      |""".stripMargin

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 20000): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("timeout")) else ref.get()

  private def writeTemp(name: String, content: String): String =
    val file = File.createTempFile(name, ".pem")
    file.deleteOnExit()
    val out = new FileOutputStream(file)
    try out.write(content.getBytes("US-ASCII"))
    finally out.close()
    file.getAbsolutePath

  private var serverProcess: Process = null

  /** Starts `openssl s_server -www` with the embedded certificate and waits until it accepts. */
  private lazy val port: Int =
    val certPath = writeTemp("ldbc-tls-cert", certPem)
    val keyPath  = writeTemp("ldbc-tls-key", keyPem)
    val probe    = new ServerSocket(0)
    val chosen   = probe.getLocalPort
    probe.close()
    serverProcess = new ProcessBuilder(
      "openssl",
      "s_server",
      "-accept",
      chosen.toString,
      "-cert",
      certPath,
      "-key",
      keyPath,
      "-www"
    ).redirectErrorStream(true).start()
    var ready    = false
    var attempts = 0
    while !ready && attempts < 100 do
      try
        val socket = new JSocket()
        socket.connect(new InetSocketAddress("127.0.0.1", chosen), 200)
        socket.close()
        ready = true
      catch
        case _: Throwable =>
          attempts += 1
          Thread.sleep(100)
    assert(ready, "openssl s_server did not start")
    chosen

  override def afterAll(): Unit =
    if serverProcess != null then
      serverProcess.destroy()
      ()

  private def handshakeAndFetch(host: String, config: SSL): Fx[Array[Byte]] =
    for
      plain    <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
      tls      <- Tls.client(plain, host, port, config)
      _        <- tls.write("GET / HTTP/1.0\r\n\r\n".getBytes("UTF-8"))
      response <- tls.read(4096)
      _        <- tls.close()
    yield response.getOrElse(Array.emptyByteArray)

  test("TLS handshake + encrypted data round-trip with Trusted"):
    val result = runSync(handshakeAndFetch("localhost", SSL.Trusted))
    assert(result.exists(_.nonEmpty), s"expected response bytes, got $result")

  test("FromPemCerts + matching hostname succeeds with verification on"):
    val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = true)
    val result = runSync(handshakeAndFetch("localhost", config))
    assert(result.exists(_.nonEmpty), s"expected response bytes, got $result")

  test("hostname mismatch is rejected when verifyHostname is on"):
    val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = true)
    val result = runSync(handshakeAndFetch("wrong.example.com", config))
    assert(result.isLeft, s"expected hostname-verification failure, got $result")

  test("the same mismatch is accepted when verifyHostname is off (proves the check is the gate)"):
    val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = false)
    val result = runSync(handshakeAndFetch("wrong.example.com", config))
    assert(result.exists(_.nonEmpty), s"expected response bytes, got $result")

  test("System rejects a self-signed server (chain validation)"):
    val result = runSync(handshakeAndFetch("localhost", SSL.System))
    assert(result.isLeft, s"expected chain-validation failure, got $result")

  test("the expected-host registry entry is released once the handshake completes, not held until close"):
    val config   = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = true)
    val baseline = S2nBridge.registeredHosts
    val prog     =
      for
        plain <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
        tls   <- Tls.client(plain, "localhost", port, config)
        after <- Fx.delay(S2nBridge.registeredHosts)
        _     <- tls.close()
      yield after
    val result = runSync(prog)
    assertEquals(result, Right(baseline), "the verify-host entry is only needed during the handshake")

  test("close() releases the io registry entry"):
    val baseline = S2nBridge.registeredIo
    val prog     =
      for
        plain <- IoEngine.global.connect("127.0.0.1", port, 5.seconds)
        tls   <- Tls.client(plain, "localhost", port, SSL.Trusted)
        _     <- tls.close()
      yield ()
    assert(runSync(prog).isRight)
    assertEquals(S2nBridge.registeredIo, baseline, "close() must remove the io registry entry")
