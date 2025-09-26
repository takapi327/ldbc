/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

enum TelemetrySpanName(val name: String):
  case CANCEL_QUERY                              extends TelemetrySpanName("Cancel Query")
  case CHANGE_DATABASE                           extends TelemetrySpanName("Change Database")
  case COMMIT                                    extends TelemetrySpanName("Commit")
  case CONNECTION_CREATE                         extends TelemetrySpanName("Create Connection")
  case CONNECTION_RESET                          extends TelemetrySpanName("Reset Connection")
  case CREATE_DATABASE                           extends TelemetrySpanName("Create Database")
  case EXPLAIN_QUERY                             extends TelemetrySpanName("Explain Query")
  case GET_INNODB_STATUS                         extends TelemetrySpanName("Get InnoDB Status")
  case GET_PROCESS_HOST                          extends TelemetrySpanName("Get Process Host")
  case GET_VARIABLE                              extends TelemetrySpanName("Get Variable")
  case LOAD_COLLATIONS                           extends TelemetrySpanName("Load Collations")
  case LOAD_VARIABLES                            extends TelemetrySpanName("Load Variables")
  case PING                                      extends TelemetrySpanName("Ping")
  case ROLLBACK                                  extends TelemetrySpanName("Rollback")
  case ROUTINE_EXECUTE                           extends TelemetrySpanName("Execute Routine")
  case ROUTINE_EXECUTE_BATCH                     extends TelemetrySpanName("Execute Routine Batch")
  case ROUTINE_PREPARE                           extends TelemetrySpanName("Prepare Routine")
  case SET_CHARSET                               extends TelemetrySpanName("Set Charset")
  case SET_OPTION_MULTI_STATEMENTS(code: Short)  extends TelemetrySpanName(s"Set multi-statements '$code''")
  case SET_TRANSACTION_ACCESS_MODE(mode: String) extends TelemetrySpanName(s"Set transaction access mode '$mode'")
  case SET_VARIABLE(variable: String)            extends TelemetrySpanName(s"Set variable '$variable'")
  case SET_VARIABLES(variables: String)          extends TelemetrySpanName(s"Set variables($variables)")
  case SHOW_WARNINGS                             extends TelemetrySpanName("Show Warnings")
  case SHUTDOWN                                  extends TelemetrySpanName("Shutdown")
  case STMT_DEALLOCATE_PREPARED                  extends TelemetrySpanName("Deallocate Prepared Statement")
  case STMT_EXECUTE                              extends TelemetrySpanName("Execute Statement")
  case STMT_EXECUTE_BATCH                        extends TelemetrySpanName("Execute Statement Batch")
  case STMT_EXECUTE_BATCH_PREPARED               extends TelemetrySpanName("Execute Prepared Statement Batch")
  case STMT_EXECUTE_PREPARED                     extends TelemetrySpanName("Execute Prepared Statement")
  case STMT_FETCH_PREPARED                       extends TelemetrySpanName("Fetch Prepared Statement")
  case STMT_PREPARE                              extends TelemetrySpanName("Prepare Statement")
  case STMT_RESET_PREPARED                       extends TelemetrySpanName("Reset Prepared Statement")
  case STMT_SEND_LONG_DATA                       extends TelemetrySpanName("Send Long Data Statement")
  case STMT_CALLABLE                             extends TelemetrySpanName("Callable Statement")
  case USE_DATABASE                              extends TelemetrySpanName("Use Database")
  case CHANGE_USER                               extends TelemetrySpanName("Change User")
  case COMMAND_QUIT                              extends TelemetrySpanName("Utility Command Quit")
  case COMMAND_STATISTICS                        extends TelemetrySpanName("Utility Command Statistics")
