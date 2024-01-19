/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schemaspy.result

/** Enum representing the status at the time of SchemaSPY execution.
  */
enum Status(val code: Int):
  case Success           extends Status(0)
  case Failure           extends Status(1)
  case EmptySchema       extends Status(2)
  case ConnectionFailure extends Status(3)
  case InvalidConfig     extends Status(4)
  case MissingParameter  extends Status(5)
