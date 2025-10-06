{%
laika.title = ldbc
laika.metadata.language = en
%}

# ldbc (Lepus Database Connectivity)

Please note that **ldbc** is pre-1.0 software and is still under active development. Newer versions may no longer be binary compatible with earlier versions.

ldbc is a library for building pure functional JDBC layers by [Cats Effect 3](https://typelevel.org/cats-effect/) and [Scala 3](https://github.com/scala/scala3).

ldbc is a [Typelevel](http://typelevel.org/) project. It embraces pure, unconventional, functional programming as described in Scala's [Code of Conduct](http://scala-lang.org/conduct.html) and is meant to provide a safe and friendly environment for teaching, learning, and contributing.

## Introduction

Most of our application development involves the use of databases.

One way to access databases in Scala is to use JDBC, and there are several libraries in Scala that wrap this JDBC.

- Functional DSLs (Slick, quill, zio-sql)
- SQL string interpolators (Anorm, doobie)

ldbc is also a library that wraps JDBC, combining aspects of both approaches. It's a Scala 3 library that provides a type-safe and refactorable SQL interface, capable of expressing SQL expressions on MySQL databases.

Unlike other libraries, ldbc also offers its own connector built in Scala.

Scala currently supports multiple platforms: JVM, JS, and Native.

However, libraries using JDBC can only run in JVM environments.

Therefore, ldbc is being developed to provide a connector written in Scala that supports the MySQL protocol, allowing it to work across different platforms.
By using ldbc, you can access databases across platforms while leveraging Scala's type safety and the benefits of functional programming.

### Target Audience

This documentation is intended for developers who use ldbc, a library for database access using the Scala programming language.

ldbc is designed for those interested in typed pure functional programming. If you're not a Cats user or are unfamiliar with functional I/O and monadic Cats Effect, you may need to proceed at a slower pace.

That said, if you find yourself confused or frustrated by this documentation or the ldbc API, please open an issue and ask for help. Both the library and documentation are young and rapidly evolving, so some lack of clarity is inevitable. This document will therefore be continuously updated to address issues and omissions.

## Quick Start

The current version is **@VERSION@**, compatible with **Scala @SCALA_VERSION@**.

```scala
libraryDependencies ++= Seq(

  // Start with this one
  "@ORGANIZATION@" %% "ldbc-dsl" % "@VERSION@",
  
  // Choose the connector you want to use
  "@ORGANIZATION@" %% "jdbc-connector" % "@VERSION@", // Java connector (supported platform: JVM)
  "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@", // Scala connector (supported platforms: JVM, JS, Native)

  // And add these as needed
  "@ORGANIZATION@" %% "ldbc-query-builder" % "@VERSION@", // Type-safe query building
  "@ORGANIZATION@" %% "ldbc-schema"        % "@VERSION@", // Database schema construction
)
```

## TODO

- JSON data type support
- SET data type support
- Geometry data type support
- CHECK constraint support
- Support for databases other than MySQL
- Test kit
- etc...
