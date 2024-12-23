/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.interpreter

type ToTuple[T] <: Tuple = T match
  case h *: EmptyTuple => Tuple1[h]
  case h *: t          => h *: ToTuple[t]
  case _               => Tuple1[T]
