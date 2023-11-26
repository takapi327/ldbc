/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package benchmark.doobie

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import doobie.*
import doobie.implicits.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var xa: Transactor[IO] = _

  @Setup
  def setup(): Unit =
    xa = Transactor.fromDriverManager[IO](
      "com.mysql.cj.jdbc.Driver",
      "jdbc:mysql://127.0.0.1:13306/world",
      "ldbc", 
      "password",
      None
    )

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = _

  @Benchmark
  def selectN: List[(Int, String, String)] =
    sql"SELECT ID, Name, CountryCode FROM city LIMIT $len"
      .query[(Int, String, String)]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
