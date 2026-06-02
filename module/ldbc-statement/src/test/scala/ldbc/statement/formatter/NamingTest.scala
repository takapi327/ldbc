/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.formatter

class NamingTest extends munit.FunSuite:

  test("Naming.toCamel should convert COLUMN to column") {
    assertEquals(Naming.toCamel("COLUMN"), "column")
  }

  test("Naming.toCamel should convert camelCase to camelCase") {
    assertEquals(Naming.toCamel("camelCase"), "camelCase")
  }

  test("Naming.toCamel should convert PascalCase to pascalCase") {
    assertEquals(Naming.toCamel("PascalCase"), "pascalCase")
  }

  test("Naming.toCamel should convert snake_case to snakeCase") {
    assertEquals(Naming.toCamel("snake_case"), "snakeCase")
  }

  test("Naming.toCamel should convert kebab-case to kebabCase") {
    assertEquals(Naming.toCamel("kebab-case"), "kebabCase")
  }

  test("Naming.toPascal should convert COLUMN to Column") {
    assertEquals(Naming.toPascal("COLUMN"), "Column")
  }

  test("Naming.toPascal should convert camelCase to CamelCase") {
    assertEquals(Naming.toPascal("camelCase"), "CamelCase")
  }

  test("Naming.toPascal should convert PascalCase to PascalCase") {
    assertEquals(Naming.toPascal("PascalCase"), "PascalCase")
  }

  test("Naming.toPascal should convert snake_case to SnakeCase") {
    assertEquals(Naming.toPascal("snake_case"), "SnakeCase")
  }

  test("Naming.toPascal should convert kebab-case to KebabCase") {
    assertEquals(Naming.toPascal("kebab-case"), "KebabCase")
  }

  test("Naming.toSnake should convert COLUMN to column") {
    assertEquals(Naming.toSnake("COLUMN"), "column")
  }

  test("Naming.toSnake should convert camelCase to camel_case") {
    assertEquals(Naming.toSnake("camelCase"), "camel_case")
  }

  test("Naming.toSnake should convert PascalCase to pascal_case") {
    assertEquals(Naming.toSnake("PascalCase"), "pascal_case")
  }

  test("Naming.toSnake should convert snake_case to snake_case") {
    assertEquals(Naming.toSnake("snake_case"), "snake_case")
  }

  test("Naming.toSnake should convert kebab-case to snake_case") {
    assertEquals(Naming.toSnake("kebab-case"), "kebab_case")
  }

  test("Naming.toSnake should handle acronyms correctly") {
    assertEquals(Naming.toSnake("HTTPRequest"), "http_request")
    assertEquals(Naming.toSnake("APIResponse"), "api_response")
    assertEquals(Naming.toSnake("JSONParser"), "json_parser")
  }

  test("Naming.toSnake should handle empty strings") {
    assertEquals(Naming.toSnake(""), "")
  }

  test("Naming.format should format with CAMEL case") {
    assertEquals(Naming.CAMEL.format("snake_case"), "snakeCase")
    assertEquals(Naming.CAMEL.format("PascalCase"), "pascalCase")
    assertEquals(Naming.CAMEL.format("kebab-case"), "kebabCase")
  }

  test("Naming.format should format with PASCAL case") {
    assertEquals(Naming.PASCAL.format("snake_case"), "SnakeCase")
    assertEquals(Naming.PASCAL.format("camelCase"), "CamelCase")
    assertEquals(Naming.PASCAL.format("kebab-case"), "KebabCase")
  }

  test("Naming.format should format with SNAKE case") {
    assertEquals(Naming.SNAKE.format("PascalCase"), "pascal_case")
    assertEquals(Naming.SNAKE.format("camelCase"), "camel_case")
    assertEquals(Naming.SNAKE.format("kebab-case"), "kebab_case")
  }

  test("Naming.fromString should return correct enum value for valid strings") {
    assertEquals(Naming.fromString("CAMEL"), Naming.CAMEL)
    assertEquals(Naming.fromString("PASCAL"), Naming.PASCAL)
    assertEquals(Naming.fromString("SNAKE"), Naming.SNAKE)
  }

  test("Naming.fromString should throw IllegalArgumentException for invalid strings") {
    val exception = intercept[IllegalArgumentException] {
      Naming.fromString("KEBAB")
    }
    assert(exception.getMessage.contains("KEBAB does not match any of the Naming"))
  }

  test("Naming should handle consecutive uppercase letters correctly") {
    assertEquals(Naming.toSnake("ABCTest"), "abc_test")
    assertEquals(Naming.toSnake("UserURLMapping"), "user_url_mapping")
  }
