/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.formatter

import munit.CatsEffectSuite

class NamingTest extends CatsEffectSuite:

  test("Naming.toCamel: convert COLUMN to column") {
    assertEquals(Naming.toCamel("COLUMN"), "column")
  }

  test("Naming.toCamel: convert camelCase to camelCase") {
    assertEquals(Naming.toCamel("camelCase"), "camelCase")
  }

  test("Naming.toCamel: convert PascalCase to pascalCase") {
    assertEquals(Naming.toCamel("PascalCase"), "pascalCase")
  }

  test("Naming.toCamel: convert snake_case to snakeCase") {
    assertEquals(Naming.toCamel("snake_case"), "snakeCase")
  }

  test("Naming.toCamel: convert kebab-case to kebabCase") {
    assertEquals(Naming.toCamel("kebab-case"), "kebabCase")
  }

  test("Naming.toPascal: convert COLUMN to Column") {
    assertEquals(Naming.toPascal("COLUMN"), "Column")
  }

  test("Naming.toPascal: convert camelCase to CamelCase") {
    assertEquals(Naming.toPascal("camelCase"), "CamelCase")
  }

  test("Naming.toPascal: convert PascalCase to PascalCase") {
    assertEquals(Naming.toPascal("PascalCase"), "PascalCase")
  }

  test("Naming.toPascal: convert snake_case to SnakeCase") {
    assertEquals(Naming.toPascal("snake_case"), "SnakeCase")
  }

  test("Naming.toPascal: convert kebab-case to KebabCase") {
    assertEquals(Naming.toPascal("kebab-case"), "KebabCase")
  }

  test("Naming.toSnake: convert COLUMN to column") {
    assertEquals(Naming.toSnake("COLUMN"), "column")
  }

  test("Naming.toSnake: convert camelCase to camel_case") {
    assertEquals(Naming.toSnake("camelCase"), "camel_case")
  }

  test("Naming.toSnake: convert PascalCase to pascal_case") {
    assertEquals(Naming.toSnake("PascalCase"), "pascal_case")
  }

  test("Naming.toSnake: convert snake_case to snake_case") {
    assertEquals(Naming.toSnake("snake_case"), "snake_case")
  }

  test("Naming.toSnake: convert kebab-case to kebab_case") {
    assertEquals(Naming.toSnake("kebab-case"), "kebab_case")
  }
