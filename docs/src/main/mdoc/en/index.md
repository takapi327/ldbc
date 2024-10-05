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

ldbc is another library that also wraps JDBC, and ldbc combines aspects of each to provide a type-safe and refactorable SQL interface in the Scala 3 library, allowing SQL expressions to be expressed on MySQL databases.

Unlike other libraries, ldbc also provides its own connector built in Scala.

Scala currently supports multiple platforms: JVM, JS, and Native.

However, if the library uses JDBC, it will only work in a JVM environment.

Therefore, ldbc is being developed to provide a connector written in Scala that is compatible with the MySQL protocol so that it can work on different platforms.
With ldbc, database access can be done regardless of platform while taking advantage of Scala's type safety and functional programming.

Also, the use of ldbc allows development to centralize Scala models, sql schemas, and documentation by managing a single resource.

This concept was inspired by [tapir](https://github.com/softwaremill/tapir), a declarative, type-safe web endpoint library. tapir can be used to build type-safe endpoints, which can then be used to generate OpenAPI documents, OpenAPI documents can also be generated from the constructed endpoints.

ldbc uses Scala at the database layer to allow for the same type-safe construction and document generation using the construction.

### Target Audience

This document is intended for developers who use ldbc, a library for database access using the Scala programming language.

ldbc is designed for those interested in typed, pure functional programming. If you are not a Cats user or are not familiar with functional I/O or the monadic Cats Effect, you may need to proceed slowly.

Nevertheless, if you are confused or frustrated by this documentation or the ldbc API, please submit an issue and ask for help. Because both the library and the documentation are new and rapidly changing, it is inevitable that there will be some unclear points. Therefore, this document will be continually updated to address problems and omissions.

## Quick Start

The current version is **@VERSION@** corresponding to **Scala @SCALA_VERSION@**.

```scala
libraryDependencies ++= Seq(

  // Start with this one.
  "@ORGANIZATION@" %% "ldbc-dsl" % "@VERSION@",
  
  // Select the connector to be used
  "@ORGANIZATION@" %% "jdbc-connector" % "@VERSION@", // Java Connector (Supported platforms: JVM)
  "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@", // Scala connector (supported platforms: JVM, JS, Native)

  // Then add these as needed
  "@ORGANIZATION@" %% "ldbc-query-builder" % "@VERSION@", // Type-safe query construction
  "@ORGANIZATION@" %% "ldbc-schema"        % "@VERSION@", // Building a database schema
)
```

## TODO

- JSON data type support
- SET data type support
- Support for Geometry data type
- Support for CHECK constraints
- Non-MySQL database support
- Streaming Support
- ZIO module support
- Test Kit
- etc...
