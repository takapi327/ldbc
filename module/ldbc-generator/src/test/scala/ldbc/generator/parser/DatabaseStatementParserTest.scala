/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import org.scalatest.flatspec.AnyFlatSpec

class DatabaseStatementParserTest extends AnyFlatSpec, DatabaseStatementParser:

  it should "Database create statement parsing test succeeds." in {
    assert(parseAll(databaseStatement, "CREATE DATABASE `database`;").successful)
    assert(parseAll(databaseStatement, "CREATE DATABASE `database` DEFAULT CHARACTER SET utf8mb4;").successful)
    assert(
      parseAll(
        databaseStatement,
        "CREATE DATABASE `database` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"
      ).successful
    )
    assert(parseAll(databaseStatement, "CREATE DATABASE IF NOT EXISTS `database`;").successful)
    assert(parseAll(databaseStatement, "CREATE DATABASE `database` ENCRYPTION Y;").successful)
    assert(
      parseAll(
        databaseStatement,
        "/* comment */ CREATE /* comment */ DATABASE /* comment */ IF NOT EXISTS /* comment */ `database`;"
      ).successful
    )
    assert(parseAll(databaseStatement, "CREATE SCHEMA `database`;").successful)
    assert(parseAll(databaseStatement, "CREATE SCHEMA `database` DEFAULT CHARACTER SET utf8mb4;").successful)
    assert(
      parseAll(
        databaseStatement,
        "CREATE SCHEMA `database` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"
      ).successful
    )
    assert(parseAll(databaseStatement, "CREATE SCHEMA IF NOT EXISTS `database`;").successful)
    assert(parseAll(databaseStatement, "CREATE SCHEMA `database` ENCRYPTION Y;").successful)
    assert(
      parseAll(
        databaseStatement,
        "/* comment */ CREATE /* comment */ SCHEMA /* comment */ IF NOT EXISTS /* comment */ `database`;"
      ).successful
    )
  }

  it should "Database create statement parsing test fails." in {
    assert(!parseAll(databaseStatement, "CREATE DATABASE;").successful)
    assert(!parseAll(databaseStatement, "CREATE DATABASE /* comment */;").successful)
    assert(!parseAll(databaseStatement, "CREATE DATABASE `database` COLLATE utf8mb4_bin;").successful)
    assert(!parseAll(databaseStatement, "CREATE DATABASE IF EXISTS `database`;").successful)
    assert(!parseAll(databaseStatement, "CREATE DATABASE `database` ENCRYPTION X;").successful)
    assert(!parseAll(databaseStatement, "CREATE SCHEMA;").successful)
    assert(!parseAll(databaseStatement, "CREATE SCHEMA /* comment */;").successful)
    assert(!parseAll(databaseStatement, "CREATE SCHEMA `database` COLLATE utf8mb4_bin;").successful)
    assert(!parseAll(databaseStatement, "CREATE SCHEMA IF EXISTS `database`;").successful)
    assert(!parseAll(databaseStatement, "CREATE SCHEMA `database` ENCRYPTION X;").successful)
  }

  it should "Database drop statement parsing test succeeds." in {
    assert(parseAll(databaseStatement, "DROP DATABASE `database`;").successful)
    assert(parseAll(databaseStatement, "DROP DATABASE IF EXISTS `database`;").successful)
    assert(
      parseAll(databaseStatement, "/* comment */ DROP /* comment */ DATABASE /* comment */ `database`;").successful
    )
    assert(parseAll(databaseStatement, "DROP SCHEMA `database`;").successful)
    assert(parseAll(databaseStatement, "DROP SCHEMA IF EXISTS `database`;").successful)
    assert(parseAll(databaseStatement, "/* comment */ DROP /* comment */ SCHEMA /* comment */ `database`;").successful)
  }

  it should "Database drop statement parsing test fails." in {
    assert(!parseAll(databaseStatement, "DROP DATABASE;").successful)
    assert(!parseAll(databaseStatement, "DROP DATABASE IF NOT EXISTS `database`;").successful)
    assert(!parseAll(databaseStatement, "/* comment */ DROP /* comment */;").successful)
    assert(!parseAll(databaseStatement, "DROP SCHEMA;").successful)
    assert(!parseAll(databaseStatement, "DROP SCHEMA IF NOT EXISTS `database`;").successful)
    assert(!parseAll(databaseStatement, "/* comment */ DROP /* comment */;").successful)
  }

  it should "Database use statement parsing test succeeds." in {
    assert(parseAll(databaseStatement, "USE `database`;").successful)
    assert(parseAll(databaseStatement, "/* comment */ USE /* comment */ `database`;").successful)
  }

  it should "Database use statement parsing test fails." in {
    assert(!parseAll(databaseStatement, "USE;").successful)
    assert(!parseAll(databaseStatement, "/* comment */ USE /* comment */;").successful)
  }
