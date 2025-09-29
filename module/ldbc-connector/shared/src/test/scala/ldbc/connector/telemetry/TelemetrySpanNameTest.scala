/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import ldbc.connector.*

class TelemetrySpanNameTest extends FTestPlatform:

  test("CANCEL_QUERY should have correct name") {
    assertEquals(TelemetrySpanName.CANCEL_QUERY.name, "Cancel Query")
  }

  test("CHANGE_DATABASE should have correct name") {
    assertEquals(TelemetrySpanName.CHANGE_DATABASE.name, "Change Database")
  }

  test("COMMIT should have correct name") {
    assertEquals(TelemetrySpanName.COMMIT.name, "Commit")
  }

  test("CONNECTION_CREATE should have correct name") {
    assertEquals(TelemetrySpanName.CONNECTION_CREATE.name, "Create Connection")
  }

  test("CONNECTION_CLOSE should have correct name") {
    assertEquals(TelemetrySpanName.CONNECTION_CLOSE.name, "Close Connection")
  }

  test("CONNECTION_RESET should have correct name") {
    assertEquals(TelemetrySpanName.CONNECTION_RESET.name, "Reset Connection")
  }

  test("CREATE_DATABASE should have correct name") {
    assertEquals(TelemetrySpanName.CREATE_DATABASE.name, "Create Database")
  }

  test("EXPLAIN_QUERY should have correct name") {
    assertEquals(TelemetrySpanName.EXPLAIN_QUERY.name, "Explain Query")
  }

  test("GET_INNODB_STATUS should have correct name") {
    assertEquals(TelemetrySpanName.GET_INNODB_STATUS.name, "Get InnoDB Status")
  }

  test("GET_PROCESS_HOST should have correct name") {
    assertEquals(TelemetrySpanName.GET_PROCESS_HOST.name, "Get Process Host")
  }

  test("GET_VARIABLE should have correct name") {
    assertEquals(TelemetrySpanName.GET_VARIABLE.name, "Get Variable")
  }

  test("LOAD_COLLATIONS should have correct name") {
    assertEquals(TelemetrySpanName.LOAD_COLLATIONS.name, "Load Collations")
  }

  test("LOAD_VARIABLES should have correct name") {
    assertEquals(TelemetrySpanName.LOAD_VARIABLES.name, "Load Variables")
  }

  test("PING should have correct name") {
    assertEquals(TelemetrySpanName.PING.name, "Ping")
  }

  test("ROLLBACK should have correct name") {
    assertEquals(TelemetrySpanName.ROLLBACK.name, "Rollback")
  }

  test("ROUTINE_EXECUTE should have correct name") {
    assertEquals(TelemetrySpanName.ROUTINE_EXECUTE.name, "Execute Routine")
  }

  test("ROUTINE_EXECUTE_BATCH should have correct name") {
    assertEquals(TelemetrySpanName.ROUTINE_EXECUTE_BATCH.name, "Execute Routine Batch")
  }

  test("ROUTINE_PREPARE should have correct name") {
    assertEquals(TelemetrySpanName.ROUTINE_PREPARE.name, "Prepare Routine")
  }

  test("SET_CHARSET should have correct name") {
    assertEquals(TelemetrySpanName.SET_CHARSET.name, "Set Charset")
  }

  test("SET_OPTION_MULTI_STATEMENTS should have correct name with code") {
    val code: Short = 1
    assertEquals(TelemetrySpanName.SET_OPTION_MULTI_STATEMENTS(code).name, s"Set multi-statements '$code'")
  }

  test("SET_TRANSACTION_ACCESS_MODE should have correct name with mode") {
    val mode = "READ ONLY"
    assertEquals(TelemetrySpanName.SET_TRANSACTION_ACCESS_MODE(mode).name, s"Set transaction access mode '$mode'")
  }

  test("SET_VARIABLE should have correct name with variable") {
    val variable = "autocommit"
    assertEquals(TelemetrySpanName.SET_VARIABLE(variable).name, s"Set variable '$variable'")
  }

  test("SET_VARIABLES should have correct name with variables") {
    val variables = "autocommit=1,max_connections=100"
    assertEquals(TelemetrySpanName.SET_VARIABLES(variables).name, s"Set variables($variables)")
  }

  test("SHOW_WARNINGS should have correct name") {
    assertEquals(TelemetrySpanName.SHOW_WARNINGS.name, "Show Warnings")
  }

  test("SHUTDOWN should have correct name") {
    assertEquals(TelemetrySpanName.SHUTDOWN.name, "Shutdown")
  }

  test("STMT_DEALLOCATE_PREPARED should have correct name") {
    assertEquals(TelemetrySpanName.STMT_DEALLOCATE_PREPARED.name, "Deallocate Prepared Statement")
  }

  test("STMT_EXECUTE should have correct name") {
    assertEquals(TelemetrySpanName.STMT_EXECUTE.name, "Execute Statement")
  }

  test("STMT_EXECUTE_BATCH should have correct name") {
    assertEquals(TelemetrySpanName.STMT_EXECUTE_BATCH.name, "Execute Statement Batch")
  }

  test("STMT_EXECUTE_BATCH_PREPARED should have correct name") {
    assertEquals(TelemetrySpanName.STMT_EXECUTE_BATCH_PREPARED.name, "Execute Prepared Statement Batch")
  }

  test("STMT_EXECUTE_PREPARED should have correct name") {
    assertEquals(TelemetrySpanName.STMT_EXECUTE_PREPARED.name, "Execute Prepared Statement")
  }

  test("STMT_FETCH_PREPARED should have correct name") {
    assertEquals(TelemetrySpanName.STMT_FETCH_PREPARED.name, "Fetch Prepared Statement")
  }

  test("STMT_PREPARE should have correct name") {
    assertEquals(TelemetrySpanName.STMT_PREPARE.name, "Prepare Statement")
  }

  test("STMT_RESET_PREPARED should have correct name") {
    assertEquals(TelemetrySpanName.STMT_RESET_PREPARED.name, "Reset Prepared Statement")
  }

  test("STMT_SEND_LONG_DATA should have correct name") {
    assertEquals(TelemetrySpanName.STMT_SEND_LONG_DATA.name, "Send Long Data Statement")
  }

  test("STMT_CALLABLE should have correct name") {
    assertEquals(TelemetrySpanName.STMT_CALLABLE.name, "Callable Statement")
  }

  test("USE_DATABASE should have correct name") {
    assertEquals(TelemetrySpanName.USE_DATABASE.name, "Use Database")
  }

  test("CHANGE_USER should have correct name") {
    assertEquals(TelemetrySpanName.CHANGE_USER.name, "Change User")
  }

  test("COMMAND_STATISTICS should have correct name") {
    assertEquals(TelemetrySpanName.COMMAND_STATISTICS.name, "Utility Command Statistics")
  }

  test("Enum case instances should be distinct") {
    // Test that different enum cases are not equal
    assert(TelemetrySpanName.COMMIT != TelemetrySpanName.ROLLBACK)
    assert(TelemetrySpanName.PING != TelemetrySpanName.COMMIT)

    // Test that same enum cases are equal
    assertEquals(TelemetrySpanName.COMMIT, TelemetrySpanName.COMMIT)
    assertEquals(TelemetrySpanName.ROLLBACK, TelemetrySpanName.ROLLBACK)
  }

  test("Parameterized span names should work correctly") {
    // SET_OPTION_MULTI_STATEMENTS with different codes
    val code1: Short = 0
    val code2: Short = 1
    assertEquals(TelemetrySpanName.SET_OPTION_MULTI_STATEMENTS(code1).name, "Set multi-statements '0'")
    assertEquals(TelemetrySpanName.SET_OPTION_MULTI_STATEMENTS(code2).name, "Set multi-statements '1'")

    // SET_TRANSACTION_ACCESS_MODE with different modes
    assertEquals(
      TelemetrySpanName.SET_TRANSACTION_ACCESS_MODE("READ WRITE").name,
      "Set transaction access mode 'READ WRITE'"
    )
    assertEquals(
      TelemetrySpanName.SET_TRANSACTION_ACCESS_MODE("READ ONLY").name,
      "Set transaction access mode 'READ ONLY'"
    )

    // SET_VARIABLE with different variables
    assertEquals(TelemetrySpanName.SET_VARIABLE("max_connections").name, "Set variable 'max_connections'")
    assertEquals(TelemetrySpanName.SET_VARIABLE("wait_timeout").name, "Set variable 'wait_timeout'")

    // SET_VARIABLES with different variable lists
    assertEquals(TelemetrySpanName.SET_VARIABLES("var1=1").name, "Set variables(var1=1)")
    assertEquals(TelemetrySpanName.SET_VARIABLES("var1=1,var2=2").name, "Set variables(var1=1,var2=2)")
  }
