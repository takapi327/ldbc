/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql.dsl

import javax.sql.DataSource

import org.specs2.mutable.Specification

import org.mockito.Mockito.*

import cats.Id
import cats.data.Kleisli

import ldbc.sql.*

object ParameterBinderTest extends Specification:

  given SQLSyntax[Id] = new SQLSyntax[Id]:
    extension (sql: SQL[Id])
      def query[T](using consumer: ResultSetConsumer[Id, T]): Kleisli[Id, DataSource, T] =
        throw new IllegalStateException("This method is never called in this test.")
      def update(): Kleisli[Id, DataSource, Int] = throw new IllegalStateException(
        "This method is never called in this test."
      )

  "ParameterBinder Test" should {

    "If a parameter of type Boolean is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val bool = true
      val sql  = sql"SELECT bool FROM test WHERE bool = $bool"

      sql.statement === "SELECT bool FROM test WHERE bool = ?" and sql.params.size === 1
    }

    "If a parameter of type Boolean is passed, the bind method of the ParameterBinder calls setBoolean of PreparedStatement." in {
      val bool = true
      val sql  = sql"SELECT bool FROM test WHERE bool = $bool"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setBoolean(1, true)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setBoolean(1, true)
    }

    "If a parameter of type Byte is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val byte = Byte.MinValue
      val sql  = sql"SELECT byte FROM test WHERE byte = $byte"

      sql.statement === "SELECT byte FROM test WHERE byte = ?" and sql.params.size === 1
    }

    "If a parameter of type Byte is passed, the bind method of the ParameterBinder calls setByte of PreparedStatement." in {
      val byte = Byte.MinValue
      val sql  = sql"SELECT byte FROM test WHERE byte = $byte"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setByte(1, byte)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setByte(1, byte)
    }

    "If a parameter of type Int is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val int = 1
      val sql = sql"SELECT id FROM test WHERE id = $int"

      sql.statement === "SELECT id FROM test WHERE id = ?" and sql.params.size === 1
    }

    "If a parameter of type Int is passed, the bind method of the ParameterBinder calls setInt of PreparedStatement." in {
      val int = 1
      val sql = sql"SELECT id FROM test WHERE id = $int"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setInt(1, 1)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setInt(1, 1)
    }

    "If a parameter of type Short is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val short = 1.toShort
      val sql   = sql"SELECT short FROM test WHERE short = $short"

      sql.statement === "SELECT short FROM test WHERE short = ?" and sql.params.size === 1
    }

    "If a parameter of type Short is passed, the bind method of the ParameterBinder calls setShort of PreparedStatement." in {
      val short = 1.toShort
      val sql   = sql"SELECT short FROM test WHERE short = $short"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setShort(1, 1.toShort)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setShort(1, 1.toShort)
    }

    "If a parameter of type Long is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val long = 1L
      val sql  = sql"SELECT long FROM test WHERE long = $long"

      sql.statement === "SELECT long FROM test WHERE long = ?" and sql.params.size === 1
    }

    "If a parameter of type Long is passed, the bind method of the ParameterBinder calls setLong of PreparedStatement." in {
      val long = 1L
      val sql  = sql"SELECT long FROM test WHERE long = $long"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setLong(1, 1L)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setLong(1, 1L)
    }

    "If a parameter of type Float is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val float = 0.1f
      val sql   = sql"SELECT float FROM test WHERE float = $float"

      sql.statement === "SELECT float FROM test WHERE float = ?" and sql.params.size === 1
    }

    "If a parameter of type Float is passed, the bind method of the ParameterBinder calls setFloat of PreparedStatement." in {
      val float = 0.1f
      val sql   = sql"SELECT float FROM test WHERE float = $float"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setFloat(1, 0.1f)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setFloat(1, 0.1f)
    }

    "If a parameter of type Double is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val double = 0.1
      val sql    = sql"SELECT double FROM test WHERE double = $double"

      sql.statement === "SELECT double FROM test WHERE double = ?" and sql.params.size === 1
    }

    "If a parameter of type Double is passed, the bind method of the ParameterBinder calls setDouble of PreparedStatement." in {
      val double = 0.1
      val sql    = sql"SELECT double FROM test WHERE double = $double"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setDouble(1, 0.1)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setDouble(1, 0.1)
    }

    "If a parameter of type BigDecimal is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val bigDecimal = BigDecimal(0.1)
      val sql        = sql"SELECT bigDecimal FROM test WHERE bigDecimal = $bigDecimal"

      sql.statement === "SELECT bigDecimal FROM test WHERE bigDecimal = ?" and sql.params.size === 1
    }

    "If a parameter of type BigDecimal is passed, the bind method of the ParameterBinder calls setBigDecimal of PreparedStatement." in {
      val bigDecimal = BigDecimal(0.1)
      val sql        = sql"SELECT bigDecimal FROM test WHERE bigDecimal = $bigDecimal"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setBigDecimal(1, BigDecimal(0.1))).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setBigDecimal(1, BigDecimal(0.1))
    }

    "If a parameter of type String is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val string = "string"
      val sql    = sql"SELECT string FROM test WHERE string = $string"

      sql.statement === "SELECT string FROM test WHERE string = ?" and sql.params.size === 1
    }

    "If a parameter of type String is passed, the bind method of the ParameterBinder calls setString of PreparedStatement." in {
      val string = "string"
      val sql    = sql"SELECT string FROM test WHERE string = $string"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setString(1, "string")).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setString(1, "string")
    }

    "If a parameter of type Array[Byte] is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val bytes = "string".getBytes
      val sql   = sql"SELECT bytes FROM test WHERE bytes = $bytes"

      sql.statement === "SELECT bytes FROM test WHERE bytes = ?" and sql.params.size === 1
    }

    "If a parameter of type Array[Byte] is passed, the bind method of the ParameterBinder calls setBytes of PreparedStatement." in {
      val bytes = "string".getBytes
      val sql   = sql"SELECT bytes FROM test WHERE bytes = $bytes"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setBytes(1, "string".getBytes)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setBytes(1, "string".getBytes)
    }

    "If a parameter of type Date is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val date = java.sql.Date.valueOf(java.time.LocalDate.now())
      val sql  = sql"SELECT date FROM test WHERE date = $date"

      sql.statement === "SELECT date FROM test WHERE date = ?" and sql.params.size === 1
    }

    "If a parameter of type Date is passed, the bind method of the ParameterBinder calls setDate of PreparedStatement." in {
      val date = java.sql.Date.valueOf(java.time.LocalDate.now())
      val sql  = sql"SELECT date FROM test WHERE date = $date"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setDate(1, date)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setDate(1, date)
    }

    "If a parameter of type Time is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val time = java.sql.Time.valueOf(java.time.LocalTime.now())
      val sql  = sql"SELECT time FROM test WHERE time = $time"

      sql.statement === "SELECT time FROM test WHERE time = ?" and sql.params.size === 1
    }

    "If a parameter of type Time is passed, the bind method of the ParameterBinder calls setTime of PreparedStatement." in {
      val time = java.sql.Time.valueOf(java.time.LocalTime.now())
      val sql  = sql"SELECT time FROM test WHERE time = $time"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setTime(1, time)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setTime(1, time)
    }

    "If a parameter of type Timestamp is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val timestamp = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())
      val sql       = sql"SELECT timestamp FROM test WHERE timestamp = $timestamp"

      sql.statement === "SELECT timestamp FROM test WHERE timestamp = ?" and sql.params.size === 1
    }

    "If a parameter of type Timestamp is passed, the bind method of the ParameterBinder calls setTimestamp of PreparedStatement." in {
      val timestamp = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())
      val sql       = sql"SELECT timestamp FROM test WHERE timestamp = $timestamp"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setTimestamp(1, timestamp)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setTimestamp(1, timestamp)
    }

    "If a parameter of type Object is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val javaObject: Object = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())
      val sql = sql"SELECT object FROM test WHERE object = $javaObject"

      sql.statement === "SELECT object FROM test WHERE object = ?" and sql.params.size === 1
    }

    "If a parameter of type Object is passed, the bind method of the ParameterBinder calls setObject of PreparedStatement." in {
      val javaObject: Object = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())
      val sql = sql"SELECT object FROM test WHERE object = $javaObject"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setObject(1, javaObject)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setObject(1, javaObject)
    }

    "If a parameter of type URL is passed, it will be equal to the value specified by statements and params in the generated SQL model." in {
      val url = new java.net.URL("http", "localhost", 5555, "")
      val sql = sql"SELECT url FROM test WHERE url = $url"

      sql.statement === "SELECT url FROM test WHERE url = ?" and sql.params.size === 1
    }

    "If a parameter of type URL is passed, the bind method of the ParameterBinder calls setURL of PreparedStatement." in {
      val url = new java.net.URL("http", "localhost", 5555, "")
      val sql = sql"SELECT url FROM test WHERE url = $url"

      val statement = mock(classOf[PreparedStatement[Id]])
      when(statement.setURL(1, url)).thenReturn(Id(()))

      sql.params.head.bind(statement, 1)

      verify(statement, org.mockito.Mockito.times(1)).setURL(1, url)
    }
  }
