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

case class TableBuilder(db: SchemaspyDatabase, table: Table[?]) extends TableValidator:
  
  private val schemaTable = new SchemaspyTable(db, "def", db.getSchema.getName, table.name, "table comment")

  private def detectPrimaryKeyColumn(table: Table[?]): Seq[Column[?]] =
    (table.*.flatMap {
      case c: Column[?] if c.attributes.exists(_.isInstanceOf[PrimaryKey]) => Some(c)
      case _ => None
    } ++ table.keyDefinitions.flatMap {
      case key: PrimaryKey with Index => key.keyPart.toList
      case _ => List.empty
    }).distinct

  private def initColumns(column: Column[?], index: Int): SchemaspyTable =
    if Option(schemaTable.getColumn(column.label)).isEmpty then
      schemaTable.getColumnsMap.put(column.label, TableColumnBuilder.build(schemaTable, column, index))

    val indexedSeq: Seq[(String, Boolean)] = column.attributes.flatMap {
      case _: PrimaryKey => Some(("PRIMARY", true))
      case key: UniqueKey => Some((key.indexName.getOrElse(column.label), true))
      case key: IndexKey => Some((key.indexName.getOrElse(column.label), false))
      case v: Constraint => v.key match
        case _: PrimaryKey => Some(("PRIMARY", true))
        case key: UniqueKey => Some((key.indexName.getOrElse(column.label), true))
        case _ => None
      case _ => None
    }

    indexedSeq.foreach {
      (key, isUnique) =>
        if Option(schemaTable.getIndex(key)).isEmpty then
          val index = new TableIndex(key, isUnique)
          schemaTable.getIndexesMap.put(index.getName, index)
          index.addColumn(schemaTable.getColumn(column.label), "DESC")
    }

    column.attributes.foreach {
      case _: AutoInc[?] => schemaTable.getColumn(column.label).setIsAutoUpdated(true)
      case _ =>
    }

    detectPrimaryKeyColumn(table).map(primaryColumn => {
      schemaTable.setPrimaryColumn(TableColumnBuilder.build(schemaTable, primaryColumn, index))
      Option(schemaTable.getIndex(primaryColumn.label)).map(_.setIsPrimaryKey(true))
    })

    schemaTable

  lazy val build: SchemaspyTable =
    table.*.zipWithIndex.map {
      case (column: Column[?], index: Int) =>
        val result = initColumns(column, index)
        db.getTablesMap.put(result.getName, result)
      case unknown => throw new IllegalStateException(s"$unknown is not a Column.")
    }

    schemaTable
    