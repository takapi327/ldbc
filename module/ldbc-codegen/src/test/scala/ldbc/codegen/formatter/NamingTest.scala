/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.formatter

import org.specs2.mutable.Specification

object NamingTest extends Specification:

  "Testing the Naming camelCase" should {

    "COLUMN => column" in {
      Naming.toCamel("COLUMN") === "column"
    }

    "camelCase => camelCase" in {
      Naming.toCamel("camelCase") === "camelCase"
    }

    "PascalCase => pascalCase" in {
      Naming.toCamel("PascalCase") === "pascalCase"
    }

    "snake_case => snakeCase" in {
      Naming.toCamel("snake_case") === "snakeCase"
    }

    "kebab-case => kebabCase" in {
      Naming.toCamel("kebab-case") === "kebabCase"
    }
  }

  "Testing the Naming PascalCase" should {

    "COLUMN => Column" in {
      Naming.toPascal("COLUMN") === "Column"
    }

    "camelCase => CamelCase" in {
      Naming.toPascal("camelCase") === "CamelCase"
    }

    "PascalCase => PascalCase" in {
      Naming.toPascal("PascalCase") === "PascalCase"
    }

    "snake_case => SnakeCase" in {
      Naming.toPascal("snake_case") === "SnakeCase"
    }

    "kebab-case => KebabCase" in {
      Naming.toPascal("kebab-case") === "KebabCase"
    }
  }

  "Testing the Naming snake_case" should {

    "COLUMN => column" in {
      Naming.toSnake("COLUMN") === "column"
    }

    "camelCase => camel_case" in {
      Naming.toSnake("camelCase") === "camel_case"
    }

    "PascalCase => pascal_case" in {
      Naming.toSnake("PascalCase") === "pascal_case"
    }

    "snake_case => snake_case" in {
      Naming.toSnake("snake_case") === "snake_case"
    }

    "kebab-case => kebab_case" in {
      Naming.toSnake("kebab-case") === "kebab_case"
    }
  }

  "Testing the Naming kebab-case" should {

    "COLUMN => column" in {
      Naming.toKebab("COLUMN") === "column"
    }

    "camelCase => camel-case" in {
      Naming.toKebab("camelCase") === "camel-case"
    }

    "PascalCase => pascal-case" in {
      Naming.toKebab("PascalCase") === "pascal-case"
    }

    "snake_case => snake-case" in {
      Naming.toKebab("snake_case") === "snake-case"
    }

    "kebab-case => kebab-case" in {
      Naming.toKebab("kebab-case") === "kebab-case"
    }
  }
