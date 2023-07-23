/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator.builder

import org.specs2.mutable.Specification

import ldbc.generator.formatter.Naming
import ldbc.generator.model.*
import ldbc.generator.model.ColumnDefinition.*

object ColumnCodeBuilderTest extends Specification:

  private val builder = ColumnCodeBuilder(Naming.PASCAL)

  "Testing the ColumnCodeBuilder" should {
    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition("p1", DataType.VARCHAR(255, None, None), None)
      builder.build(column, None) === "column(\"p1\", VARCHAR[Option[String]](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition("p1", DataType.VARCHAR(255, None, None), Some(List(Attribute.Condition(false))))
      builder.build(column, None) === "column(\"p1\", VARCHAR[String](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition(
        "p1",
        DataType.BIGINT(None, false, false),
        Some(List(
          Attribute.Condition(false),
          Attribute.Key("AUTO_INCREMENT"),
          Attribute.Key("PRIMARY_KEY")
        ))
      )
      builder.build(column, None) === "column(\"p1\", BIGINT[Long], AUTO_INCREMENT, PRIMARY_KEY)"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition(
        "p1",
        DataType.BIGINT(None, false, false),
        Some(List(
          Attribute.Condition(false),
          CommentSet("identifier")
        ))
      )
      builder.build(column, None) === "column(\"p1\", BIGINT[Long], \"identifier\")"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition(
        "p1",
        DataType.VARCHAR(255, None, None),
        Some(List(
          Attribute.Condition(false),
          Attribute.Collate("utf8mb4_bin")
        ))
      )
      builder.build(column, None) === "column(\"p1\", VARCHAR[String](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition(
        "p1",
        DataType.VARCHAR(255, None, None),
        Some(List(
          Attribute.Condition(false),
          Attribute.Visible("VISIBLE")
        ))
      )
      builder.build(column, None) === "column(\"p1\", VARCHAR[String](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition(
        "p1",
        DataType.VARCHAR(255, None, None),
        Some(List(
          Attribute.Condition(false),
          Attribute.ColumnFormat("FIXED")
        ))
      )
      builder.build(column, None) === "column(\"p1\", VARCHAR[String](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition(
        "p1",
        DataType.VARCHAR(255, None, None),
        Some(List(
          Attribute.Condition(false),
          Attribute.Storage("DISK")
        ))
      )
      builder.build(column, None) === "column(\"p1\", VARCHAR[String](255))"
    }

    "The construction of Column into a code string matches the specified string." in {
      val column = ColumnDefinition("p1", DataType.SERIAL(), None)
      builder.build(column, None) === "column(\"p1\", SERIAL[BigInt])"
    }
  }
