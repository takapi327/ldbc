/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.model

/**
 * Trait for using enum with Ldbc. It will be unnecessary if enums can be identified by type parameters.
 *
 * example:
 * {{{
 *   enum Status extends Enum:
 *     case Active, InActive
 *   object Status extends EnumDataType[Status]
 * }}}
 */
trait Enum:

  /**
   * Index number of the enum value, a value that is standard implemented when the enum is generated.
   */
  def ordinal: Int

/**
 * Trait inherited to access the standard implementation that the generated enum has. It will be unnecessary if enums
 * can be identified by type parameters.
 *
 * @tparam T
 *   Trait for using enum with Ldbc.
 */
trait EnumDataType[T <: Enum]:

  /**
   * List of enumeration values, a value that is standard implemented when the enum is generated.
   *
   * @return
   *   List of enumerated values
   */
  def values: Array[T]

  /**
   * Method to obtain the Enum value at the specified index number, a value that is standard implemented when the enum
   * is generated.
   *
   * @param ordinal
   *   Index number of the enum value
   * @return
   *   enum value
   */
  def fromOrdinal(ordinal: Int): T

  /**
   * Method to retrieve the Enum value with the specified name, a value that is standard implemented when the enum is
   * generated.
   *
   * @param name
   *   enum name
   * @return
   *   enum value
   */
  def valueOf(name: String): T
