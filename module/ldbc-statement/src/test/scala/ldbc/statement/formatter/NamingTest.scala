/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
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
