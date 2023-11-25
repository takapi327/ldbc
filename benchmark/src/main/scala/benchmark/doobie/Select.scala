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
class Select:

  def selectN(num: Int): Int =
    sql"SELECT ID, Name, CountryCode FROM city LIMIT $num"
      .query[(String, String, String)]
      .to[List]
      .transact(Select.xa)
      .map(_.length)
      .unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(10)
  def select10: Int = selectN(10)

  @Benchmark
  @OperationsPerInvocation(100)
  def select100: Int = selectN(100)

  @Benchmark
  @OperationsPerInvocation(1000)
  def select1000: Int = selectN(1000)

  @Benchmark
  @OperationsPerInvocation(2000)
  def select2000: Int = selectN(2000)

  @Benchmark
  @OperationsPerInvocation(4000)
  def select4000: Int = selectN(4000)

object Select:

  @State(Scope.Benchmark)
  val xa = Transactor.fromDriverManager[IO]("com.mysql.cj.jdbc.Driver", "jdbc:mysql://127.0.0.1:13306/world", "ldbc", "password", None)
