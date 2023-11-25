/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package benchmark.jdbc

import java.util.concurrent.TimeUnit

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class Select:

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.While"))
  def selectN(num: Int): Int =
    val connection = Select.dataSource.getConnection
    try {
      connection.setAutoCommit(false)
      val statement = connection.prepareStatement("SELECT ID, Name, CountryCode FROM city LIMIT ?")
      try {
        statement.setInt(1, num)
        val resultSet = statement.executeQuery()
        try {
          val records = List.newBuilder[(Int, String, String)]
          while (resultSet.next()) {
            val code = resultSet.getInt(1)
            resultSet.wasNull()
            val name = resultSet.getString(2)
            resultSet.wasNull()
            val region = resultSet.getString(3)
            resultSet.wasNull()
            records += ((code, name, region))
          }
          records.result().length
        } finally resultSet.close()
      } finally statement.close()
    } finally {
      connection.commit()
      connection.close()
    }

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
  val dataSource = new MysqlDataSource()
  dataSource.setServerName("127.0.0.1")
  dataSource.setPortNumber(13306)
  dataSource.setDatabaseName("world")
  dataSource.setUser("ldbc")
  dataSource.setPassword("password")
