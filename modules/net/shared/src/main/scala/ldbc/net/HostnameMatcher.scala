/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/** A subjectAltName entry extracted from an X.509 certificate. */
enum SanEntry:
  /** A dNSName entry (may contain a leftmost-label wildcard such as `*.example.com`). */
  case Dns(name: String)

  /** An iPAddress entry, as its textual representation. */
  case Ip(address: String)

/**
 * Pure hostname-to-certificate matching, fixed to the written specification (RFC 6125): SAN entries
 * take priority (with a DNS SAN present the CN is never consulted), DNS wildcards match only the
 * leftmost label, and an IP-literal host matches only an exact iPAddress SAN. Used by the Native
 * TLS engine's verify-host callback and by the cross-platform parity tests; the JVM and JS engines
 * use their runtimes' built-in checkers.
 */
object HostnameMatcher:

  /**
   * Decides whether a certificate identified by `sans`/`cn` matches `host`.
   *
   * @param sans the certificate's subjectAltName entries
   * @param cn   the certificate subject's common name, if any
   * @param host the hostname or IP literal the client intended to connect to
   * @return `true` if the certificate matches the host under RFC 6125 rules
   */
  def matches(sans: List[SanEntry], cn: Option[String], host: String): Boolean =
    if isIpLiteral(host) then
      sans.exists {
        case SanEntry.Ip(address) => address.equalsIgnoreCase(host)
        case _                    => false
      }
    else
      val dnsSans = sans.collect { case SanEntry.Dns(name) => name }
      if dnsSans.nonEmpty then dnsSans.exists(matchesDns(_, host))
      else cn.exists(matchesDns(_, host))

  /**
   * Matches one certificate-presented name against the intended host, for per-name verify callbacks
   * (s2n's `verify_host` delivers names one at a time rather than as a full SAN list). An IP-literal
   * host matches only by exact textual equality; a DNS host follows the same wildcard rules as
   * [[matches]].
   *
   * @param certName a single name presented by the certificate
   * @param host     the hostname or IP literal the client intended to connect to
   */
  def matchesName(certName: String, host: String): Boolean =
    if isIpLiteral(host) then certName.equalsIgnoreCase(host)
    else matchesDns(certName, host)

  /**
   * Whether `host` is an IP literal (IPv4 dotted-quad or IPv6). IP literals never match DNS names
   * and must not be sent as SNI (RFC 6066).
   *
   * @param host the host string to classify
   */
  def isIpLiteral(host: String): Boolean =
    host.contains(':') || host.nonEmpty && host.split('.').length == 4 &&
      host.split('.').forall(part => part.nonEmpty && part.length <= 3 && part.forall(_.isDigit) && part.toInt <= 255)

  /**
   * Matches one certificate DNS name (possibly wildcarded) against a hostname, case-insensitively.
   * A wildcard is honoured only as the entire leftmost label and matches exactly one label.
   */
  private def matchesDns(pattern: String, host: String): Boolean =
    val p = pattern.toLowerCase
    val h = host.toLowerCase
    if p.startsWith("*.") then
      val suffix     = p.drop(1)
      val firstDotAt = h.indexOf('.')
      firstDotAt > 0 && h.substring(firstDotAt) == suffix
    else p == h
