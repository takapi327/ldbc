# ldbc (Lepus Database Connectivity)

<div align="center">
  <img alt="ldbc" src="./lepus_logo.png">
</div>

[![Continuous Integration](https://github.com/takapi327/ldbc/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/takapi327/ldbc/actions/workflows/ci.yml)
[![MIT License](https://img.shields.io/badge/license-MIT-green)](https://en.wikipedia.org/wiki/MIT_License)
[![Scala Version](https://img.shields.io/badge/scala-v3.3.x-red)](https://github.com/lampepfl/dotty)
[![Typelevel Affiliate Project](https://img.shields.io/badge/typelevel-affiliate%20project-FF6169.svg)](https://typelevel.org/projects/affiliate/)
[![javadoc](https://javadoc.io/badge2/io.github.takapi327/ldbc-core_3/javadoc.svg)](https://javadoc.io/doc/io.github.takapi327/ldbc-core_3)
[![Maven Central Version](https://maven-badges.herokuapp.com/maven-central/io.github.takapi327/ldbc-core_3/badge.svg?color=blue)](https://search.maven.org/artifact/io.github.takapi327/ldbc-core_3/0.3.0-beta2/jar)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-core/latest-by-scala-version.svg?color=blue)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-core/latest-by-scala-version.svg?color=blue&targetType=js)](https://index.scala-lang.org/takapi327/ldbc)
[![scaladex](https://index.scala-lang.org/takapi327/ldbc/ldbc-core/latest-by-scala-version.svg?color=blue&targetType=native)](https://index.scala-lang.org/takapi327/ldbc)

ldbc (Lepus Database Connectivity) is Pure functional JDBC layer with Cats Effect 3 and Scala 3.

ldbc is Created under the influence of [tapir](https://github.com/softwaremill/tapir), a declarative, type-safe web endpoint library. Using tapir, you can build type-safe endpoints and also generate OpenAPI documentation from the endpoints you build.

ldbc allows the same type-safe construction with Scala at the database layer and document generation using the constructed one.

> [!NOTE]
> **ldbc** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

Please drop a :star: if this project interests you. I need encouragement.

## Modules availability

ldbc is available on the JVM, Scala.js, and ScalaNative

|  Module / Platform   | JVM | Scala Native | Scala.js |  
|:--------------------:|:---:|:------------:|:--------:|
|     `ldbc-core`      |  ✅  |      ✅       |    ✅     |
|      `ldbc-sql`      |  ✅  |      ✅       |    ✅     |
| `ldbc-query-builder` |  ✅  |      ✅       |    ✅     |
|      `ldbc-dsl`      |  ✅  |      ❌       |    ❌     | 
|   `ldbc-schemaSpy`   |  ✅  |      ❌       |    ❌     | 
|    `ldbc-codegen`    |  ✅  |      ✅       |    ✅     |
|    `ldbc-hikari`     |  ✅  |      ❌       |    ❌     | 
|    `ldbc-plugin`     |  ✅  |      ❌       |    ❌     | 
|   `ldbc-connector`   |  ✅  |      ✅       |    ✅     | 

## Documentation

- [English](https://takapi327.github.io/ldbc/en/index.html)
- [Japanese](https://takapi327.github.io/ldbc/ja/index.html)

## Features/Roadmap

Creating a MySQL connector project written in pure Scala3.

JVM, JS and Native platforms are all supported.

> [!IMPORTANT]
> **ldbc** is currently focused on developing connectors written in pure Scala3 to work with JVM, JS and Native.
> In the future, we also plan to rewrite existing functions based on a pure Scala3 connector.

### Implementation of Authentication Function

- [x] Support for mysql_native_password plugin
- [x] Support for sha256_password plugin
- [x] Support for caching_sha2_password plugin
- [x] SSL/TLS support
- [x] RSA authentication method support
- [x] Establish connection specifying database

### Sending SQL statements to a database

- [x] Statement
- [x] PreparedStatement
- [x] CallableStatement
- [ ] ResultSet Insert/Update/Delete

### Transaction function implementation

- [x] Read Only setting function
- [x] Auto Commit setting function
- [x] Transaction isolation level setting function
- [x] Commit function
- [x] Rollback function
- [x] Savepoint function

### Utility Commands

- [x] COM_QUIT
- [x] COM_INIT_DB
- [x] COM_STATISTICS
- [x] COM_PING
- [x] COM_CHANGE_USER
- [x] COM_RESET_CONNECTION
- [x] COM_SET_OPTION

### Connection pooling implementation

- [ ] Failover Countermeasures

### Performance Verification

- [ ] Comparison with JDBC
- [ ] Comparison with other MySQL Scala libraries
- [ ] Verification of operation in AWS and other infrastructure environments

### Other

- [ ] Integration into existing functions
- [ ] Additional streaming implementation
- [ ] Support for other effects systems
- [ ] Integration with java.sql API
- [ ] etc...

## Contributing

All suggestions welcome :)!

If you’d like to contribute, see the list of [issues](https://github.com/takapi327/ldbc/issues) and pick one! Or report your own. If you have an idea you’d like to discuss, that’s always a good option.

If you have any questions about why or how it works, feel free to ask on github. This probably means that the documentation, scaladocs, and code are unclear and can be improved for the benefit of all.

### Testing locally

If you want to build and run the tests for yourself, you'll need a local MySQL database. The easiest way to do this is to run `docker-compose up` from the project root.
