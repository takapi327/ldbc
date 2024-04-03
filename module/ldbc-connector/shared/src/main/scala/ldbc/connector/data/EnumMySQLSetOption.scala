/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

/**
 * EnumMySQLSetOption is an enumeration of MySQL set options.
 */
enum EnumMySQLSetOption(val code: Short):
  case MYSQL_OPTION_MULTI_STATEMENTS_ON extends EnumMySQLSetOption(0)
  case MYSQL_OPTION_MULTI_STATEMENTS_OFF extends EnumMySQLSetOption(1)
