# Security Policy

ldbc takes security seriously. This document explains how to report a
vulnerability and what you can expect from us in return.

## Supported Versions

ldbc is pre-1.0 software. Releases are not binary compatible with prior
versions.

| Version | Supported                                                       |
| ------- | --------------------------------------------------------------- |
| 0.9.x   | :white_check_mark:                                              |
| 0.8.x   | High / Critical fixes only, until 6 months after 0.9.0 release |
| < 0.8   | :x:                                                             |

We aim to publish security fixes for the latest release line. When a new
release line is published, we intend to also provide fixes for
vulnerabilities rated **High or Critical** in the previous line for **6
months** after the new line's first release; the table above will show the
end date. Lower-severity issues are fixed only in the latest line, and once
the 6 months have passed, upgrading is the remediation path.

## Reporting a Vulnerability

**Please do not report security issues through public GitHub issues, pull
requests, or discussions.**

Report privately through GitHub Security Advisories:

<https://github.com/takapi327/ldbc/security/advisories/new>

### What to include

The more of this you can provide, the faster we can triage:

- ldbc version and module (e.g. `ldbc-mysql` 0.9.0)
- Scala version and platform (JVM / Scala.js / Scala Native)
- MySQL server version and configuration, if relevant
- A description of the vulnerability and its impact
- Steps to reproduce, ideally a minimal reproducer project or test case
- Any proof-of-concept code you have

### What to expect

We aim to provide:

- **Acknowledgement** within 5 business days.
- **Initial assessment** — whether we consider the report a vulnerability, and
  a rough severity — within 10 business days.
- **Status updates** at least every 14 days until the report is resolved or
  closed.

ldbc is maintained by volunteers. We will do our best to move faster than these
windows, but they are goals, not guarantees.

If you have not received an acknowledgement within 5 business days, open a
public GitHub issue saying only that you have submitted a private security
report and are waiting for a response. Do not include any details of the
vulnerability in that issue.

## Coordinated Disclosure

We ask reporters to follow coordinated disclosure:

- Give us **90 days** from acknowledgement before any public disclosure. If a
  fix needs longer, we will ask for an extension in writing and explain why.
- Do not publish details, proof-of-concept code, or a CVE record before the
  fixed release is available and the advisory is published.
- We intend to publish a GitHub Security Advisory once the fix is released.

### CVE assignment

**CVE IDs for vulnerabilities in ldbc are requested by the maintainers through
GitHub, which is a CVE Numbering Authority.** We ask that reporters and
third-party CNAs not assign or publish a CVE for ldbc outside this process
without prior coordination with us.

If we conclude that a report does not describe a vulnerability in ldbc — for
example, because it depends on unsafe usage, or the behaviour is in a
dependency — we will explain our reasoning. If you disagree, say so and we
will reconsider; we would rather argue it out privately than discover a
disputed CVE after publication.

We intend to share the draft advisory with you for review before it is
published.

## Scope

### In scope

Vulnerabilities in ldbc's own code in any artifact we publish, on Maven Central
or npm, except the test-only modules listed under "Out of scope". Areas of
particular interest:

- **MySQL wire protocol implementation and transport** (currently
  `ldbc-mysql`, `ldbc-net`, `ldbc-authentication-plugin`,
  `ldbc-aws-authentication-plugin`, and the legacy `ldbc-connector`) — TLS
  handling, certificate and hostname verification, authentication plugin
  implementations, packet parsing, and anything that could allow a malicious
  or compromised server, or a network attacker on an unencrypted connection,
  to compromise the client. This includes protocol values that cause unbounded
  memory allocation, infinite loops, or crashes in the client.
- **Credentials and query data handling** — passwords, tokens, AWS
  credentials, or bound parameter values leaking through exception messages,
  `toString` output, or the traces and metrics ldbc emits under its default
  telemetry configuration, including literals that slip past query-text
  sanitization (currently `ldbc-sql`, `ldbc-mysql`, `ldbc-telemetry`,
  `ldbc-otel4s`, `ldbc-zio-telemetry`, `ldbc-authentication-plugin`,
  `ldbc-aws-authentication-plugin`, and the legacy `ldbc-connector`).
  Query-text sanitization is best-effort; bypasses are in scope but are
  typically rated Low.
- **Connection pool** (currently `ldbc-pool`, and the legacy pool in
  `ldbc-connector`) — returning a connection that still carries the previous
  borrower's open transaction (including its uncommitted writes and row locks)
  or authenticated identity, or committing a previous borrower's partial work
  when a connection is returned.
- **Query construction APIs** (currently `ldbc-dsl`, `ldbc-statement`,
  `ldbc-query-builder`, `ldbc-schema`) — any way to inject SQL through the
  `sql` interpolator or the type-safe query and schema APIs while using them
  as documented, including any way to break out of the quoting applied by
  `ident` / `enquoteIdentifier`.
- **Code generation** (currently `ldbc-codegen`, `ldbc-plugin`) — code
  generation that produces unsafe output from untrusted schema input.
- **MCP document server** (currently `@ldbc/mcp-document-server` on npm).

### Out of scope

- SQL injection resulting from bypassing the `sql` interpolator, for example by
  concatenating untrusted input into a query string.
- Passing untrusted input to `ident` / `enquoteIdentifier`, which lets that
  input choose which table or column is referenced. Deciding which
  identifiers a user may reference is the application's responsibility.
- Resource exhaustion caused by legitimately large result sets or values
  returned by the server.
- Bound parameter values passed to a user-supplied `LogHandler`, and query text
  emitted after the user has explicitly disabled query-text sanitization. These
  are delivered by design, and handling them is the application's
  responsibility.
- Connection state other than that listed under "Connection pool" not being
  reset between borrowers — connection attributes (read-only, isolation level,
  catalog), session variables, temporary tables, and named locks. These do not
  cross MySQL's privilege boundary; access control is enforced by the database
  user's grants. Please report them as ordinary bugs.
- The test-only modules `ldbc-testkit` and `ldbc-testkit-munit`.
- Vulnerabilities in MySQL itself, in the JDBC driver used by
  `jdbc-connector`, or in other third-party dependencies. Please report these
  upstream; tell us as well if ldbc's usage makes the impact worse, and we will
  look at updating the dependency.
- Findings from automated scanners with no demonstrated exploit path or impact.
- Missing hardening measures or best practices with no concrete attack.
- Vulnerabilities requiring an attacker who already has code execution in the
  application process.
- Issues in ldbc's own CI, documentation site, or example code that do not
  affect users of the published artifacts.

## Safe Harbour

We will not pursue legal action against anyone who reports a vulnerability in
good faith under this policy: who makes a reasonable effort to avoid harm to
users, does not access or modify data belonging to others, and gives us a
reasonable opportunity to fix the issue before disclosure.

This applies only to testing ldbc in environments you own or are otherwise
authorized to test. It does not authorize testing systems operated by others
that use ldbc, and we cannot waive the rights of third parties.

## Recognition

ldbc does not run a bug bounty and offers no monetary reward. If you would like
to be credited, tell us the name or handle to use and we will include it in the
published advisory.

## Third-Party Audits

We welcome security review of ldbc — the source is public and MIT licensed, so
no permission is needed to read it or to test it in your own environment.
Reports from security vendors, audit firms, and bug bounty platforms are
handled under exactly the same policy as any other report, including the CVE
assignment process described above.

## Legal

This policy describes how we intend to handle vulnerability reports. It is not
a contract, creates no obligation or warranty, and does not modify the MIT
License, under which ldbc is provided "as is", without warranty of any kind.
