/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy.builder

import org.schemaspy.model.Database as SchemaspyDatabase
import org.schemaspy.model.Table as SchemaspyTable
import org.schemaspy.model.TableIndex

import ldbc.core.*
import ldbc.core.attribute.AutoInc
import ldbc.core.validator.TableValidator

/** A model for building a SchemaSpy Table model from a Table.
  *
  * If there is a problem with the Table configuration, an exception is raised by TableValidator during instance
  * creation.
  *
  * @param db
  *   A model that stores information on databases handled by SchemaSpy.
  * @param table
  *   Trait for generating SQL table information.
  */
case class TableBuilder(db: SchemaspyDatabase, table: Table[?]) extends TableValidator:

  private val comment = table.options.flatMap {
    case comment: TableOption.Comment => Some(comment.value)
    case _                            => None
  }.headOption

  private val schemaTable = new SchemaspyTable(db, null, db.getSchema.getName, table._name, comment.orNull)

  /** A method to extract columns with PrimaryKey set from all columns and keys set in the Table.
    *
    * @param table
    *   Trait for generating SQL table information.
    */
  private def detectPrimaryKeyColumn(table: Table[?]): Seq[Column[?]] =
    (table.all.flatMap {
      case c: Column[?] if c.attributes.exists(_.isInstanceOf[PrimaryKey]) => Some(c)
      case _                                                               => None
    } ++ table.keyDefinitions.flatMap {
      case key: PrimaryKey with Index => key.keyPart.toList
      case _                          => List.empty
    }).distinct

  /** Method for extracting Index key information from Column.
    *
    * @param column
    *   Trait for representing SQL Column
    */
  private def buildIndexFromColumn(column: Column[?]): Seq[(String, Boolean)] =
    column.attributes.flatMap {
      case _: PrimaryKey  => Some(("PRIMARY", true))
      case key: UniqueKey => Some((key.indexName.getOrElse(column.label), true))
      case key: IndexKey  => Some((key.indexName.getOrElse(column.label), false))
      case v: Constraint =>
        v.key match
          case _: PrimaryKey  => Some(("PRIMARY", true))
          case key: UniqueKey => Some((key.indexName.getOrElse(column.label), true))
          case _              => None
      case _ => None
    }

  /** Method for extracting Index key information from Table Key definitions.
    *
    * @param column
    *   Trait for representing SQL Column
    * @param keyDefinitions
    *   Table Key definitions
    */
  private def buildIndexFromKeyDefinitions(column: Column[?], keyDefinitions: Seq[Key]): Seq[(String, Boolean)] =
    keyDefinitions.flatMap {
      case key: PrimaryKey with Index if key.keyPart.find(_ == column).nonEmpty => Some(("PRIMARY", true))
      case key: UniqueKey with Index if key.keyPart.find(_ == column).nonEmpty =>
        Some((key.indexName.getOrElse(column.label), true))
      case key: IndexKey if key.keyPart.find(_ == column).nonEmpty =>
        Some((key.indexName.getOrElse(column.label), false))
      case constraint: Constraint =>
        constraint.key match
          case key: PrimaryKey with Index if key.keyPart.find(_ == column).nonEmpty => Some(("PRIMARY", true))
          case key: UniqueKey with Index if key.keyPart.find(_ == column).nonEmpty =>
            Some((key.indexName.getOrElse(column.label), true))
          case _ => List.empty
      case _ => List.empty
    }

  /** Methods for building and storing column information in SchemaSpy's Table model.
    *
    * @param column
    *   Trait for representing SQL Column
    * @param index
    *   The index number of the column set in the table.
    */
  private def initColumns(column: Column[?], index: Int): SchemaspyTable =
    if Option(schemaTable.getColumn(column.label)).isEmpty then
      schemaTable.getColumnsMap.put(column.label, TableColumnBuilder.build(schemaTable, column, index))

    val indexedSeq =
      (buildIndexFromColumn(column) ++ buildIndexFromKeyDefinitions(column, table.keyDefinitions)).distinct

    indexedSeq.foreach { (key, isUnique) =>
      if Option(schemaTable.getIndex(key)).isEmpty then
        val index = new TableIndex(key, isUnique)
        schemaTable.getIndexesMap.put(index.getName, index)
        index.addColumn(schemaTable.getColumn(column.label), "DESC")
    }

    column.attributes.foreach {
      case _: AutoInc[?] => schemaTable.getColumn(column.label).setIsAutoUpdated(true)
      case _             =>
    }

    detectPrimaryKeyColumn(table).map(primaryColumn => {
      schemaTable.setPrimaryColumn(TableColumnBuilder.build(schemaTable, primaryColumn, index))
      Option(schemaTable.getIndex(primaryColumn.label)).map(_.setIsPrimaryKey(true))
    })

    schemaTable

  lazy val build: SchemaspyTable =
    table.all.zipWithIndex.map((column: Column[?], index: Int) =>
      val result = initColumns(column, index)
      db.getTablesMap.put(result.getName, result)
    )

    schemaTable
