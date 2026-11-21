/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.exception

import ldbc.sql.SQLFeatureNotSupportedException

/** MySQL-branded constructors for shared [[ldbc.sql]] exceptions that need extra driver context. */
object MySQLErrors:

  def featureNotSupported(message: String, detail: Option[String]): SQLFeatureNotSupportedException =
    SQLFeatureNotSupportedException(
      message,
      detail = detail,
      hint   = Some(
        "Report Issues here: https://github.com/takapi327/ldbc/issues/new?assignees=&labels=&projects=&template=bug_report.md&title="
      ),
      vendor = "MySQL"
    )
