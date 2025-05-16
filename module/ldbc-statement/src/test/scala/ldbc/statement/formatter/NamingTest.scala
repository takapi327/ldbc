/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.formatter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NamingTest extends AnyFlatSpec, Matchers:

  "Naming.toCamel" should "convert COLUMN to column" in {
    Naming.toCamel("COLUMN") shouldEqual "column"
  }

  it should "convert camelCase to camelCase" in {
    Naming.toCamel("camelCase") shouldEqual "camelCase"
  }

  it should "convert PascalCase to pascalCase" in {
    Naming.toCamel("PascalCase") shouldEqual "pascalCase"
  }

  it should "convert snake_case to snakeCase" in {
    Naming.toCamel("snake_case") shouldEqual "snakeCase"
  }

  it should "convert kebab-case to kebabCase" in {
    Naming.toCamel("kebab-case") shouldEqual "kebabCase"
  }

  "Naming.toPascal" should "convert COLUMN to Column" in {
    Naming.toPascal("COLUMN") shouldEqual "Column"
  }

  it should "convert camelCase to CamelCase" in {
    Naming.toPascal("camelCase") shouldEqual "CamelCase"
  }

  it should "convert PascalCase to PascalCase" in {
    Naming.toPascal("PascalCase") shouldEqual "PascalCase"
  }

  it should "convert snake_case to SnakeCase" in {
    Naming.toPascal("snake_case") shouldEqual "SnakeCase"
  }

  it should "convert kebab-case to KebabCase" in {
    Naming.toPascal("kebab-case") shouldEqual "KebabCase"
  }

  "Naming.toSnake" should "convert COLUMN to column" in {
    Naming.toSnake("COLUMN") shouldEqual "column"
  }

  it should "convert camelCase to camel_case" in {
    Naming.toSnake("camelCase") shouldEqual "camel_case"
  }

  it should "convert PascalCase to pascal_case" in {
    Naming.toSnake("PascalCase") shouldEqual "pascal_case"
  }

  it should "convert snake_case to snake_case" in {
    Naming.toSnake("snake_case") shouldEqual "snake_case"
  }

  it should "convert kebab-case to snake_case" in {
    Naming.toSnake("kebab-case") shouldEqual "kebab_case"
  }
  
  it should "handle acronyms correctly" in {
    Naming.toSnake("HTTPRequest") shouldEqual "http_request"
    Naming.toSnake("APIResponse") shouldEqual "api_response"
    Naming.toSnake("JSONParser") shouldEqual "json_parser"
  }
  
  it should "handle empty strings" in {
    Naming.toSnake("") shouldEqual ""
  }
  
  "Naming.format" should "format with CAMEL case" in {
    Naming.CAMEL.format("snake_case") shouldEqual "snakeCase"
    Naming.CAMEL.format("PascalCase") shouldEqual "pascalCase"
    Naming.CAMEL.format("kebab-case") shouldEqual "kebabCase"
  }
  
  it should "format with PASCAL case" in {
    Naming.PASCAL.format("snake_case") shouldEqual "SnakeCase"
    Naming.PASCAL.format("camelCase") shouldEqual "CamelCase"
    Naming.PASCAL.format("kebab-case") shouldEqual "KebabCase"
  }
  
  it should "format with SNAKE case" in {
    Naming.SNAKE.format("PascalCase") shouldEqual "pascal_case"
    Naming.SNAKE.format("camelCase") shouldEqual "camel_case"
    Naming.SNAKE.format("kebab-case") shouldEqual "kebab_case"
  }
  
  "Naming.fromString" should "return correct enum value for valid strings" in {
    Naming.fromString("CAMEL") shouldBe Naming.CAMEL
    Naming.fromString("PASCAL") shouldBe Naming.PASCAL
    Naming.fromString("SNAKE") shouldBe Naming.SNAKE
  }
  
  it should "throw IllegalArgumentException for invalid strings" in {
    val exception = intercept[IllegalArgumentException] {
      Naming.fromString("KEBAB")
    }
    exception.getMessage should include("KEBAB does not match any of the Naming")
  }
  
  "Naming" should "handle consecutive uppercase letters correctly" in {
    Naming.toSnake("ABCTest") shouldEqual "abc_test"
    Naming.toSnake("UserURLMapping") shouldEqual "user_url_mapping"
  }
