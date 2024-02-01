/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

/**
 * Model for storing commented out strings in SQL files.
 *
 * @param message
 *   Commented out string
 */
case class CommentOut(message: String)

/**
 * A model for storing the values of comment attributes to be set on columns and tables.
 *
 * @param message
 *   Comments to be set on columns and tables
 */
case class CommentSet(message: String)
