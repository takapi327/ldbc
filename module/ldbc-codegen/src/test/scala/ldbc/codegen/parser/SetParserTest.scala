/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.codegen.parser

import org.scalatest.flatspec.AnyFlatSpec

class SetParserTest extends AnyFlatSpec, SetParser:

  override def fileName: String = "test.sql"

  it should "SET statement parsing test succeeds." in {
    assert(
      parseAll(
        setStatements,
        "SET @name = 43;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET GLOBAL max_connections = 1000;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@GLOBAL.max_connections = 1000;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET SESSION sql_mode = 'TRADITIONAL';"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET LOCAL sql_mode = 'TRADITIONAL';"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@SESSION.sql_mode = 'TRADITIONAL';"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@LOCAL.sql_mode = 'TRADITIONAL';"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@sql_mode = 'TRADITIONAL';"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET sql_mode = 'TRADITIONAL';"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET PERSIST max_connections = 1000;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@PERSIST.max_connections = 1000;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET PERSIST_ONLY back_log = 100;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@PERSIST_ONLY.back_log = 100;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@SESSION.max_join_size = DEFAULT;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@SESSION.max_join_size = @@GLOBAL.max_join_size;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET GLOBAL sort_buffer_size = 1000000, SESSION sort_buffer_size = 1000000;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET @@GLOBAL.sort_buffer_size = 1000000, @@LOCAL.sort_buffer_size = 1000000;"
      ).successful
    )
    assert(
      parseAll(
        setStatements,
        "SET GLOBAL max_connections = 1000, sort_buffer_size = 1000000;"
      ).successful
    )
  }
