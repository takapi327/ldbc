/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

trait Join[A, B, AB, OO]:

  def left: TableQuery[A, ?]

  def right: TableQuery[B, ?]

  def on(expression: AB => Expression): TableQuery[AB, OO]
