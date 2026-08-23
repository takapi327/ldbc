/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration.*
import scala.scalajs.js

import ldbc.fx.Fx
import ldbc.fx.concurrentFx
import ldbc.net.TlsUpgrade

/**
 * Node TLS integration tests (design Phase 2): a node `tls` echo server backed by an embedded
 * self-signed certificate (SAN = dns:localhost, ip:127.0.0.1) exercises the upgrade path, chain
 * validation, node's built-in hostname verification, and a genuine STARTTLS flow (plaintext banner
 * first, TLS upgrade on the same socket).
 */
class TlsTest extends munit.FunSuite:

  private val engine = ldbc.net.IoEngine.fromRaw[Fx](PlatformRawEngine.global)

  private given ExecutionContext = munitExecutionContext

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

  private lazy val tlsModule = js.Dynamic.global.require("tls")
  private lazy val netModule = js.Dynamic.global.require("net")

  private def toFuture[A](fx: Fx[A]): Future[A] =
    val promise = Promise[A]()
    fx.unsafeRun(result => promise.complete(result.toTry))
    promise.future

  /** Starts a TLS echo server with the embedded certificate and runs `body` with its port. */
  private def withTlsEchoServer[A](body: Int => Future[A]): Future[A] =
    val ready  = Promise[Int]()
    val server = tlsModule.createServer(
      js.Dynamic.literal(key = keyPem, cert = certPem),
      ((sock: js.Dynamic) => {
        sock.on("data", ((chunk: js.Dynamic) => { sock.write(chunk); () }): js.Function1[js.Dynamic, Unit])
        ()
      }): js.Function1[js.Dynamic, Unit]
    )
    server.listen(0, (() => ready.success(server.address().port.asInstanceOf[Int])): js.Function0[Unit])
    ready.future.flatMap(port => body(port).andThen { case _ => server.close() })

  /** Starts a STARTTLS-style server: a plaintext banner first, then the same socket upgrades to TLS echo. */
  private def withStartTlsServer[A](banner: String)(body: Int => Future[A]): Future[A] =
    val ready  = Promise[Int]()
    val server = netModule.createServer(((sock: js.Dynamic) => {
      sock.write(banner)
      val context = tlsModule.createSecureContext(js.Dynamic.literal(key = keyPem, cert = certPem))
      val tlsSock = js.Dynamic.newInstance(tlsModule.TLSSocket)(
        sock,
        js.Dynamic.literal(isServer = true, secureContext = context)
      )
      tlsSock.on("data", ((chunk: js.Dynamic) => { tlsSock.write(chunk); () }): js.Function1[js.Dynamic, Unit])
      ()
    }): js.Function1[js.Dynamic, Unit])
    server.listen(0, (() => ready.success(server.address().port.asInstanceOf[Int])): js.Function0[Unit])
    ready.future.flatMap(port => body(port).andThen { case _ => server.close() })

  private def echoOver(port: Int, host: String, config: SSL, message: String): Fx[String] =
    for
      plain <- engine.connect("127.0.0.1", port, 5.seconds)
      tls   <- summon[TlsUpgrade[Fx]].client(plain, host, port, config)
      _     <- tls.write(message.getBytes("UTF-8"))
      bytes <- tls.read(1024)
      _     <- tls.close()
    yield new String(bytes.getOrElse(Array.emptyByteArray), "UTF-8")

  test("TLS echo round-trip with Trusted"):
    withTlsEchoServer { port =>
      toFuture(echoOver(port, "localhost", SSL.Trusted, "PING-JS-TLS")).map(got => assertEquals(got, "PING-JS-TLS"))
    }

  test("FromPemCerts + matching hostname succeeds with verification on"):
    withTlsEchoServer { port =>
      val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = true)
      toFuture(echoOver(port, "localhost", config, "VERIFIED-JS")).map(got => assertEquals(got, "VERIFIED-JS"))
    }

  test("hostname mismatch is rejected when verifyHostname is on"):
    withTlsEchoServer { port =>
      val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = true)
      toFuture(echoOver(port, "wrong.example.com", config, "X")).transform {
        case scala.util.Success(_) => scala.util.Try(fail("expected hostname-verification failure"))
        case scala.util.Failure(_) => scala.util.Try(())
      }
    }

  test("the same mismatch is accepted when verifyHostname is off (proves the check is the gate)"):
    withTlsEchoServer { port =>
      val config = SSL.Custom(trust = TrustSource.FromPemCerts(certPem), verifyHostname = false)
      toFuture(echoOver(port, "wrong.example.com", config, "UNCHECKED-JS")).map(got =>
        assertEquals(got, "UNCHECKED-JS")
      )
    }

  test("System rejects a self-signed server (chain validation)"):
    withTlsEchoServer { port =>
      toFuture(echoOver(port, "localhost", SSL.System, "X")).transform {
        case scala.util.Success(_) => scala.util.Try(fail("expected chain-validation failure"))
        case scala.util.Failure(_) => scala.util.Try(())
      }
    }

  test("the underlying raw socket keeps an 'error' listener after upgrade (unhandled 'error' crashes node)"):
    withTlsEchoServer { port =>
      val prog =
        for
          plain <- engine.connect("127.0.0.1", port, 5.seconds)
          raw = plain.asInstanceOf[ldbc.net.RawBackedSocket].underlying.asInstanceOf[NodeRawSocket].underlying
          tls   <- summon[TlsUpgrade[Fx]].client(plain, "localhost", port, SSL.Trusted)
          count <- Fx.delay(raw.listenerCount("error").asInstanceOf[Int])
          _     <- tls.close()
        yield count
      toFuture(prog).map { listenerCount =>
        assert(
          listenerCount > 0,
          s"raw socket has $listenerCount 'error' listeners; an emitted error would crash the process"
        )
      }
    }

  test("an error emitted on the raw socket after upgrade does not crash the process"):
    withTlsEchoServer { port =>
      val prog =
        for
          plain <- engine.connect("127.0.0.1", port, 5.seconds)
          raw = plain.asInstanceOf[ldbc.net.RawBackedSocket].underlying.asInstanceOf[NodeRawSocket].underlying
          tls <- summon[TlsUpgrade[Fx]].client(plain, "localhost", port, SSL.Trusted)
          _   <- Fx.delay { raw.emit("error", new js.Error("injected raw failure")); () }
        yield ()
      toFuture(prog).map(_ => assert(true))
    }

  test("STARTTLS: plaintext banner then upgrade of the same socket to TLS"):
    withStartTlsServer("BANNER") { port =>
      val prog =
        for
          plain  <- engine.connect("127.0.0.1", port, 5.seconds)
          banner <- plain.read(6)
          tls    <- summon[TlsUpgrade[Fx]].client(plain, "localhost", port, SSL.Trusted)
          _      <- tls.write("AFTER-UPGRADE".getBytes("UTF-8"))
          bytes  <- tls.read(1024)
          _      <- tls.close()
        yield (
          new String(banner.getOrElse(Array.emptyByteArray), "UTF-8"),
          new String(bytes.getOrElse(Array.emptyByteArray), "UTF-8")
        )
      toFuture(prog).map {
        case (banner, echoed) =>
          assertEquals(banner, "BANNER")
          assertEquals(echoed, "AFTER-UPGRADE")
      }
    }
