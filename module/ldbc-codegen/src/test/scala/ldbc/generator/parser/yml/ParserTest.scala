/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.codegen.parser.yml

import org.specs2.mutable.Specification

object ParserTest extends Specification:

  "Testing the Custom" should {

    "The model generated from the Yaml string matches the specified model." in {
      val parser = Parser(
        Parser.Database(
          "test",
          Seq(
            Parser.Table(
              "test",
              Some(Seq(Parser.Column("id", "Long"))),
              Some(Parser.Extend(Seq("ldbc.custom.extend"))),
              Some(Parser.Extend(Seq("ldbc.custom.extend")))
            )
          )
        )
      )
      val parsed = Parser.parse("""
          |database:
          |  name: 'test'
          |  tables:
          |    - name: 'test'
          |      columns:
          |        - name: 'id'
          |          type: 'Long'
          |      object:
          |        extends:
          |          - ldbc.custom.extend
          |      class:
          |        extends:
          |          - ldbc.custom.extend
          |""".stripMargin)
      parsed === parser
    }

    "The model generated from the Yaml string matches the specified model." in {
      val parser = Parser(
        Parser.Database(
          "test",
          Seq(
            Parser.Table(
              "test",
              Some(Seq(Parser.Column("id", "Long"))),
              None,
              None
            )
          )
        )
      )
      val parsed = Parser.parse("""
          |database:
          |  name: 'test'
          |  tables:
          |    - name: 'test'
          |      columns:
          |        - name: 'id'
          |          type: 'Long'
          |""".stripMargin)
      parsed === parser
    }

    "The model generated from the Yaml string matches the specified model." in {
      val parser = Parser(
        Parser.Database(
          "test",
          Seq(
            Parser.Table(
              "test",
              None,
              Some(Parser.Extend(Seq("ldbc.custom.extend"))),
              Some(Parser.Extend(Seq("ldbc.custom.extend")))
            )
          )
        )
      )
      val parsed = Parser.parse("""
          |database:
          |  name: 'test'
          |  tables:
          |    - name: 'test'
          |      object:
          |        extends:
          |          - ldbc.custom.extend
          |      class:
          |        extends:
          |          - ldbc.custom.extend
          |""".stripMargin)
      parsed === parser
    }
  }
