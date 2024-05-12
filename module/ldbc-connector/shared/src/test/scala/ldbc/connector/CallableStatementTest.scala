/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.*
import cats.effect.*
import ldbc.connector.exception.SQLException
import munit.CatsEffectSuite
import org.typelevel.otel4s.trace.Tracer

class CallableStatementTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  test("The result of calling an empty procedure matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL proc1()")
          resultSet         <- callableStatement.executeQuery()
          value             <- resultSet.getString(1)
        yield value
      },
      Some("8.0.33")
    )
  }

  test("The result of calling by executeUpdate an empty procedure matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL proc1()")
          resultSet         <- callableStatement.executeUpdate() *> callableStatement.getResultSet()
          value <- resultSet match
                     case Some(rs) => rs.getString(1)
                     case None     => IO.raiseError(new Exception("No result set"))
        yield value
      },
      Some("8.0.33")
    )
  }

  test("The result of calling a procedure that accepts only one IN parameter argument matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL proc2(?)")
          resultSet         <- callableStatement.setInt(1, 1024) *> callableStatement.executeQuery()
          value             <- resultSet.getInt(1)
        yield value
      },
      1024
    )
  }

  test(
    "The result of calling a procedure that accepts one or more IN parameter arguments matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL proc3(?, ?)")
          resultSet <- callableStatement.setInt(1, 1024) *> callableStatement.setString(2, "Hello") *> callableStatement
                         .executeQuery()
          param1 <- resultSet.getInt(1)
          param2 <- resultSet.getString(2)
        yield (param1, param2)
      },
      (1024, Some("Hello"))
    )
  }

  test(
    "The result of calling a procedure that accepts one or more OUT parameter arguments matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL proc4(?, ?)")
          _      <- callableStatement.setInt(1, 1) *> callableStatement.setInt(2, 2) *> callableStatement.executeQuery()
          param1 <- callableStatement.getInt(1)
          param2 <- callableStatement.getString(2)
        yield (param1, param2)
      },
      (-1, Some("hello"))
    )
  }

  test("The result of retrieving the In parameter with index matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          resultSet <- callableStatement.setString(1, "abcdefg") *> callableStatement.setInt(2, 1) *> callableStatement
                         .executeQuery()
          value <- resultSet.getString(1)
        yield value
      },
      Some("abcdefg")
    )
  }

  test("The result of retrieving the Out parameter with index matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          resultSet <- callableStatement.setString(1, "abcdefg") *> callableStatement.setInt(2, 1) *> callableStatement
                         .executeQuery()
          outParam <- callableStatement.getInt(2)
        yield outParam
      },
      2
    )
  }

  test("The result of retrieving the Out parameter with parameter name matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          resultSet <- callableStatement.setString(1, "abcdefg") *> callableStatement.setInt(2, 1) *> callableStatement
                         .executeQuery()
          outParam <- callableStatement.getInt("inOutParam")
        yield outParam
      },
      2
    )
  }

  test("The results retrieved in multiple result sets returned from the procedure match the specified values.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          hasResult <- callableStatement.setString(1, "abcdefg") *> callableStatement.setInt(2, 1) *> callableStatement
                         .execute()
          values <- Monad[IO].whileM[List, Option[String]](callableStatement.getMoreResults()) {
                      for
                        resultSet <- callableStatement.getResultSet().flatMap {
                                       case Some(rs) => IO.pure(rs)
                                       case None     => IO.raiseError(new Exception("No result set"))
                                     }
                        value <- resultSet.getString(1)
                      yield value
                    }
        yield values
      },
      List(Some("abcdefg"), Some("zyxwabcdefg"))
    )
  }

  test("If a query is executed with the Out parameter set in advance, the execution result will match the value that was set.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          _ <- callableStatement.setString(1, "abcdefg")
          _ <- callableStatement.setInt(2, 1)
          _ <- callableStatement.registerOutParameter(2, ldbc.connector.data.Types.INTEGER)
          hasResult <- callableStatement.execute()
          value <- callableStatement.getInt(2)
        yield value
      },
      2
    )
  }

  test("The result of calling a stored function with an empty parameter argument matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("SELECT func1()")
          resultSet         <- callableStatement.executeQuery()
          value             <- resultSet.getInt(1)
        yield value
      },
      -1
    )
  }

  test("The result of calling a stored function with an empty parameter argument matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("SELECT func2()")
          resultSet         <- callableStatement.executeQuery()
          value             <- resultSet.getString(1)
        yield value
      },
      Some("hello, world")
    )
  }

  test("The result of calling a stored function with arguments matches the specified value.") {
    assertIO(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("select getPrice(?)")
          resultSet         <- callableStatement.setInt(1, 100) *> callableStatement.executeQuery()
          value             <- resultSet.getInt(1)
        yield value
      },
      110
    )
  }

  test("The registration process of Out parameter succeeds.") {
    assertIOBoolean(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          _ <- callableStatement.setString(1, "abcdefg") *> callableStatement
                 .registerOutParameter(2, ldbc.connector.data.Types.INTEGER)
        yield true
      }
    )
  }

  test(
    "SQLException occurs if the Out parameter type of the procedure is different from the Out parameter type to be set."
  ) {
    interceptMessageIO[SQLException](
      "Message: The type specified for the parameter does not match the type registered as a procedure."
    )(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL demoSp(?, ?)")
          _ <- callableStatement.setString(1, "abcdefg") *> callableStatement
                 .registerOutParameter(2, ldbc.connector.data.Types.VARCHAR)
        yield true
      }
    )
  }

  test("SQLException occurs if the procedure does not have an Out parameter and is preconfigured for Out.") {
    interceptMessageIO[SQLException]("Message: No output parameters returned by procedure.")(
      connection.use { conn =>
        for
          callableStatement <- conn.prepareCall("CALL proc3(?, ?)")
          _ <- callableStatement.setInt(1, 1024) *> callableStatement
                 .registerOutParameter(2, ldbc.connector.data.Types.VARCHAR)
        yield true
      }
    )
  }
