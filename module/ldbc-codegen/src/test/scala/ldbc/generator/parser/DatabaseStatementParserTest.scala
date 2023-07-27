/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import org.scalatest.flatspec.AnyFlatSpec

class DatabaseStatementParserTest extends AnyFlatSpec, DatabaseStatementParser:

  override def fileName: String = "test.sql"

  it should "Database create statement parsing test succeeds." in {
    assert(parseAll(databaseStatements, "CREATE DATABASE `database`;").successful)
    assert(parseAll(databaseStatements, "CREATE DATABASE `database` DEFAULT CHARACTER SET utf8mb4;").successful)
    assert(parseAll(databaseStatements, "CREATE DATABASE `database` COLLATE utf8mb4_bin;").successful)
    assert(
      parseAll(
        databaseStatements,
        "CREATE DATABASE `database` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"
      ).successful
    )
    assert(parseAll(databaseStatements, "CREATE DATABASE IF NOT EXISTS `database`;").successful)
    assert(parseAll(databaseStatements, "CREATE DATABASE `database` ENCRYPTION Y;").successful)
    assert(
      parseAll(
        databaseStatements,
        "/* comment */ CREATE /* comment */ DATABASE /* comment */ IF NOT EXISTS /* comment */ `database`;"
      ).successful
    )
    assert(parseAll(databaseStatements, "CREATE SCHEMA `database`;").successful)
    assert(parseAll(databaseStatements, "CREATE SCHEMA `database` DEFAULT CHARACTER SET utf8mb4;").successful)
    assert(parseAll(databaseStatements, "CREATE SCHEMA `database` COLLATE utf8mb4_bin;").successful)
    assert(
      parseAll(
        databaseStatements,
        "CREATE SCHEMA `database` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"
      ).successful
    )
    assert(parseAll(databaseStatements, "CREATE DATABASE IF EXISTS `database`;").successful)
    assert(parseAll(databaseStatements, "CREATE SCHEMA IF NOT EXISTS `database`;").successful)
    assert(parseAll(databaseStatements, "CREATE SCHEMA `database` ENCRYPTION Y;").successful)
    assert(
      parseAll(
        databaseStatements,
        "/* comment */ CREATE /* comment */ SCHEMA /* comment */ IF NOT EXISTS /* comment */ `database`;"
      ).successful
    )
  }

  it should "Database create statement parsing test fails." in {
    assert(!parseAll(databaseStatements, "CREATE DATABASE;").successful)
    assert(!parseAll(databaseStatements, "CREATE DATABASE /* comment */;").successful)
    assert(!parseAll(databaseStatements, "CREATE DATABASE `database` ENCRYPTION X;").successful)
    assert(!parseAll(databaseStatements, "CREATE SCHEMA;").successful)
    assert(!parseAll(databaseStatements, "CREATE SCHEMA /* comment */;").successful)
    assert(!parseAll(databaseStatements, "CREATE SCHEMA `database` ENCRYPTION X;").successful)
  }

  it should "Database drop statement parsing test succeeds." in {
    assert(parseAll(databaseStatements, "DROP DATABASE `database`;").successful)
    assert(parseAll(databaseStatements, "DROP DATABASE IF EXISTS `database`;").successful)
    assert(parseAll(databaseStatements, "DROP DATABASE IF NOT EXISTS `database`;").successful)
    assert(
      parseAll(databaseStatements, "/* comment */ DROP /* comment */ DATABASE /* comment */ `database`;").successful
    )
    assert(parseAll(databaseStatements, "DROP SCHEMA `database`;").successful)
    assert(parseAll(databaseStatements, "DROP SCHEMA IF EXISTS `database`;").successful)
    assert(parseAll(databaseStatements, "/* comment */ DROP /* comment */ SCHEMA /* comment */ `database`;").successful)
  }

  it should "Database drop statement parsing test fails." in {
    assert(!parseAll(databaseStatements, "DROP DATABASE;").successful)
    assert(!parseAll(databaseStatements, "/* comment */ DROP /* comment */;").successful)
    assert(!parseAll(databaseStatements, "DROP SCHEMA;").successful)
    assert(!parseAll(databaseStatements, "/* comment */ DROP /* comment */;").successful)
  }

  it should "Database use statement parsing test succeeds." in {
    assert(parseAll(databaseStatements, "USE `database`;").successful)
    assert(parseAll(databaseStatements, "/* comment */ USE /* comment */ `database`;").successful)
  }

  it should "Database use statement parsing test fails." in {
    assert(!parseAll(databaseStatements, "USE;").successful)
    assert(!parseAll(databaseStatements, "/* comment */ USE /* comment */;").successful)
  }
