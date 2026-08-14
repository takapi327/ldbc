/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import ldbc.net.SanEntry.{ Dns, Ip }

/**
 * Specification tests for [[HostnameMatcher]] (RFC 6125 rules, design §7): SAN priority over CN,
 * leftmost-label-only wildcards, and strict iPAddress matching for IP-literal hosts.
 */
class HostnameMatcherTest extends munit.FunSuite:

  test("exact DNS SAN match, case-insensitively"):
    assert(HostnameMatcher.matches(List(Dns("Example.COM")), None, "example.com"))
    assert(!HostnameMatcher.matches(List(Dns("example.org")), None, "example.com"))

  test("wildcard matches exactly one leftmost label"):
    assert(HostnameMatcher.matches(List(Dns("*.example.com")), None, "a.example.com"))
    assert(!HostnameMatcher.matches(List(Dns("*.example.com")), None, "a.b.example.com"))
    assert(!HostnameMatcher.matches(List(Dns("*.example.com")), None, "example.com"))

  test("a present DNS SAN suppresses CN fallback"):
    assert(!HostnameMatcher.matches(List(Dns("other.com")), Some("example.com"), "example.com"))

  test("CN fallback applies only when no DNS SAN exists"):
    assert(HostnameMatcher.matches(Nil, Some("example.com"), "example.com"))
    assert(!HostnameMatcher.matches(Nil, Some("other.com"), "example.com"))
    assert(HostnameMatcher.matches(List(Ip("10.0.0.1")), Some("example.com"), "example.com"))

  test("an IP-literal host matches only an exact iPAddress SAN"):
    assert(HostnameMatcher.matches(List(Ip("127.0.0.1")), None, "127.0.0.1"))
    assert(!HostnameMatcher.matches(List(Dns("127.0.0.1")), Some("127.0.0.1"), "127.0.0.1"))
    assert(!HostnameMatcher.matches(List(Ip("10.0.0.1")), None, "127.0.0.1"))

  test("isIpLiteral classifies IPv4/IPv6 but not hostnames"):
    assert(HostnameMatcher.isIpLiteral("127.0.0.1"))
    assert(HostnameMatcher.isIpLiteral("::1"))
    assert(!HostnameMatcher.isIpLiteral("example.com"))
    assert(!HostnameMatcher.isIpLiteral("1.2.3.4.5"))
    assert(!HostnameMatcher.isIpLiteral("999.0.0.300"))

  test("isIpLiteral returns false (does not throw) for numeric labels beyond Int range"):
    assert(!HostnameMatcher.isIpLiteral("99999999999.1.1.1"))
    assert(!HostnameMatcher.isIpLiteral("1.1.1.99999999999999999999"))
    assert(!HostnameMatcher.matches(List(SanEntry.Ip("127.0.0.1")), None, "99999999999.1.1.1"))
