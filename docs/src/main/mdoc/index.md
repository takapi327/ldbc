# LDBC

<div align="center">
  <img alt="LDBC" src="./img/lepus_logo.png">
</div>

<div align="center">
  <a href="https://search.maven.org/artifact/io.github.takapi327/ldbc-core_3/0.3.0-alpha2/jar">
    <img alt="Maven Central Version" src="https://img.shields.io/maven-central/v/io.github.takapi327/ldbc-core_3?color=blue">
  </a>
  <a href="https://en.wikipedia.org/wiki/MIT_License">
    <img alt="MIT License" src="https://img.shields.io/badge/license-MIT-green">
  </a>
  <a href="https://github.com/lampepfl/dotty">
    <img alt="Scala Version" src="https://img.shields.io/badge/scala-v3.3.x-red">
  </a>
  <a href="https://typelevel.org/projects/affiliate/">
    <img alt="Typelevel Affiliate Project" src="https://img.shields.io/badge/typelevel-affiliate%20project-FF6169.svg">
  </a>
</div>

ldbc (Lepus Database Connectivity) is Pure functional JDBC layer with Cats Effect 3 and Scala 3.

ldbc is Created under the influence of [tapir](https://github.com/softwaremill/tapir), a declarative, type-safe web endpoint library. Using tapir, you can build type-safe endpoints and also generate OpenAPI documentation from the endpoints you build.

ldbc allows the same type-safe construction with Scala at the database layer and document generation using the constructed one.

Note that **ldbc** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

## Modules availability

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

- [English](/ldbc/en/index.html)
- [Japanese](/ldbc/ja/index.html)

## Contributing

All suggestions welcome :)!

If you’d like to contribute, see the list of [issues](https://github.com/takapi327/ldbc/issues) and pick one! Or report your own. If you have an idea you’d like to discuss, that’s always a good option.

If you have any questions about why or how it works, feel free to ask on github. This probably means that the documentation, scaladocs, and code are unclear and can be improved for the benefit of all.

### Testing locally

If you want to build and run the tests for yourself, you'll need a local MySQL database. The easiest way to do this is to run `docker-compose up` from the project root.
