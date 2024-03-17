/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.slick

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import slick.jdbc.MySQLProfile.api.*

import benchmark.City

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var db: Database = uninitialized

  @volatile
  var query: TableQuery[CityTable] = uninitialized

  @Setup
  def setup(): Unit =
    db = Database.forURL(
      url      = "jdbc:mysql://127.0.0.1:13306/world",
      user     = "ldbc",
      password = "password",
      driver   = "com.mysql.cj.jdbc.Driver"
    )

    query = TableQuery[CityTable]

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def selectN: Seq[(Int, String, String)] =
    Await.result(
      db.run(query.map(city => (city.id, city.name, city.countryCode)).take(len).result),
      Duration.Inf
    )

class CityTable(tag: Tag) extends Table[City](tag, "city"):
  def id          = column[Int]("ID", O.AutoInc, O.PrimaryKey)
  def name        = column[String]("Name")
  def countryCode = column[String]("CountryCode")
  def district    = column[String]("District")
  def population  = column[Int]("Population")

  def * = (id, name, countryCode, district, population).mapTo[City]
