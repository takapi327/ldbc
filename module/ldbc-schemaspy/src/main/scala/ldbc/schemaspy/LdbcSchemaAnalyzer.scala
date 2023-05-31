/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy

import java.io.{ File, IOException, FileFilter }
import java.nio.file.{ Files, Path, StandardOpenOption }
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.lang.invoke.MethodHandles

import scala.util.Using
import scala.jdk.CollectionConverters.*

import org.slf4j.LoggerFactory

import org.apache.commons.io.filefilter.FileFilterUtils

import org.schemaspy.{ DbAnalyzer, SimpleRuntimeDotConfig, LayoutFolder, TableOrderer, OrderingReport }
import org.schemaspy.model.Database as SchemaspyDatabase
import org.schemaspy.model.Table as SchemaspyTable
import org.schemaspy.model.{
  ProgressListener,
  Tracked,
  Console,
  ForeignKeyConstraint,
}
import org.schemaspy.util.{ Markdown, ManifestUtils, DataTableConfig, DefaultPrintWriter, Jar }
import org.schemaspy.util.naming.FileNameGenerator
import org.schemaspy.view.*
import org.schemaspy.output.OutputProducer
import org.schemaspy.output.dot.schemaspy.{ DefaultFontConfig, DotFormatter, OrphanGraph }
import org.schemaspy.output.diagram.{ SummaryDiagram, TableDiagram }
import org.schemaspy.output.diagram.vizjs.VizJSDot
import org.schemaspy.output.html.mustache.diagrams.{
  MustacheSummaryDiagramFactory,
  OrphanDiagram,
  MustacheTableDiagramFactory
}
import org.schemaspy.analyzer.ImpliedConstraintsFinder
import org.schemaspy.cli.CommandLineArguments

import ldbc.core.*
import ldbc.schemaspy.builder.{ DbmsMetaBuilder, TableBuilder, ImportForeignKeyBuilder }

class LdbcSchemaAnalyzer(
  database:             Database,
  layoutFolder:         LayoutFolder,
  builder:              DbmsMetaBuilder,
  commandLineArguments: CommandLineArguments,
  outputProducer:       OutputProducer,
  orderer:              TableOrderer,
  outputDirectory:      File
):

  private val DOT_HTML       = ".html"
  private val INDEX_DOT_HTML = "index.html"
  private val SECONDS_IN_MS  = 1000

  private val isOneOfMultipleSchemas = false

  private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

  private def writeInfo(key: String, value: String, infoFile: Path): Unit =
    try
      Files.write(
        infoFile,
        (key + "=" + "\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND,
        StandardOpenOption.WRITE
      )
    catch
      case e: IOException =>
        logger.error(s"Failed to write `$key=$value`, to $infoFile")
        e.printStackTrace()

  private def notHtml(): FileFilter =
    val notHtmlFilter = FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(DOT_HTML))
    FileFilterUtils.and(notHtmlFilter)

  private def generateHtmlDoc(
    db:               SchemaspyDatabase,
    outputDirectory:  File,
    progressListener: ProgressListener
  ): Unit =

    logger.info(s"Gathered schema details in ${ progressListener.startedGraphingSummaries() / SECONDS_IN_MS } seconds")
    logger.info("Writing/graphing summary")

    val tables = db.getTables

    Markdown.registryPage(new java.util.ArrayList[SchemaspyTable](tables))

    new Jar(layoutFolder.url(), outputDirectory, notHtml()).copyJarResourceToPath()

    val renderer = new VizJSDot()

    val htmlInfoFile = outputDirectory.toPath.resolve("info-html.txt")
    Files.deleteIfExists(htmlInfoFile)

    writeInfo("date", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ")), htmlInfoFile)
    writeInfo("os", System.getProperty("os.name") + " " + System.getProperty("os.version"), htmlInfoFile)
    writeInfo("schemaspy-version", ManifestUtils.getImplementationVersion, htmlInfoFile)
    writeInfo("schemaspy-build", ManifestUtils.getImplementationBuild, htmlInfoFile)
    writeInfo("renderer", renderer.identifier(), htmlInfoFile)
    progressListener.graphingSummaryProgressed()

    val hasRealConstraints = !db.getRemoteTables.isEmpty || tables.stream().anyMatch(table => !table.isOrphan(false))

    val impliedConstraintsFinder = new ImpliedConstraintsFinder()

    val impliedConstraints = impliedConstraintsFinder.find(tables)

    val runtimeDotConfig = new SimpleRuntimeDotConfig(
      new DefaultFontConfig(commandLineArguments.getDotConfig),
      commandLineArguments.getDotConfig,
      "svg".equalsIgnoreCase(renderer.format()),
      isOneOfMultipleSchemas
    )

    val dotProducer = new DotFormatter(runtimeDotConfig)
    val diagramDir  = new File(outputDirectory, "diagrams")
    diagramDir.mkdirs()
    val summaryDir = new File(diagramDir, "summary")
    summaryDir.mkdirs()
    val summaryDiagram = new SummaryDiagram(renderer, summaryDir)

    val mustacheSummaryDiagramFactory = new MustacheSummaryDiagramFactory(
      dotProducer,
      summaryDiagram,
      hasRealConstraints,
      !impliedConstraints.isEmpty,
      outputDirectory
    )
    val results = mustacheSummaryDiagramFactory.generateSummaryDiagrams(db, tables, progressListener)
    results.getOutputExceptions.stream().forEachOrdered(_.printStackTrace())

    val dataTableConfig = new DataTableConfig(commandLineArguments)
    val mustacheCompiler = new MustacheCompiler(
      db.getName,
      db.getSchema.getName,
      commandLineArguments.getHtmlConfig,
      isOneOfMultipleSchemas,
      dataTableConfig
    )

    val htmlRelationshipsPage =
      new HtmlRelationshipsPage(mustacheCompiler, hasRealConstraints, !impliedConstraints.isEmpty)
    Using(new DefaultPrintWriter(outputDirectory.toPath.resolve("relationships.html").toFile)) { writer =>
      htmlRelationshipsPage.write(results, writer)
    }

    progressListener.graphingSummaryProgressed()

    val htmlOrphansPage = new HtmlOrphansPage(
      mustacheCompiler,
      new OrphanDiagram(
        new OrphanGraph(runtimeDotConfig, tables),
        renderer,
        outputDirectory
      )
    )

    Using(new DefaultPrintWriter(outputDirectory.toPath.resolve("orphans.html").toFile)) { writer =>
      htmlOrphansPage.write(writer)
    }

    progressListener.graphingSummaryProgressed()

    val htmlMainIndexPage = new HtmlMainIndexPage(
      mustacheCompiler,
      commandLineArguments.getHtmlConfig.getDescription
    )
    Using(new DefaultPrintWriter(outputDirectory.toPath.resolve(INDEX_DOT_HTML).toFile)) { writer =>
      htmlMainIndexPage.write(db, tables, impliedConstraints, writer)
    }

    progressListener.graphingSummaryProgressed()

    val constraints = DbAnalyzer.getForeignKeyConstraints(tables)

    val htmlConstraintsPage = new HtmlConstraintsPage(mustacheCompiler)
    Using(new DefaultPrintWriter(outputDirectory.toPath.resolve("constraints.html").toFile)) { writer =>
      htmlConstraintsPage.write(constraints, tables, writer)
    }

    progressListener.graphingSummaryProgressed()

    val htmlAnomaliesPage = new HtmlAnomaliesPage(mustacheCompiler)
    Using(new DefaultPrintWriter(outputDirectory.toPath.resolve("anomalies.html").toFile)) { writer =>
      htmlAnomaliesPage.write(tables, impliedConstraints, writer)
    }

    progressListener.graphingSummaryProgressed()

    val htmlColumnsPage = new HtmlColumnsPage(mustacheCompiler)
    Using(new DefaultPrintWriter(outputDirectory.toPath.resolve("columns.html").toFile)) { writer =>
      htmlColumnsPage.write(tables, writer)
    }

    progressListener.graphingSummaryProgressed()

    val htmlRoutinesPage = new HtmlRoutinesPage(mustacheCompiler)
    Using(new DefaultPrintWriter(outputDirectory.toPath.resolve("routines.html").toFile)) { writer =>
      htmlRoutinesPage.write(db.getRoutines, writer)
    }

    val htmlRoutinePage = new HtmlRoutinePage(mustacheCompiler)
    db.getRoutines.forEach(routine => {
      Using(
        new DefaultPrintWriter(
          outputDirectory.toPath
            .resolve("routines")
            .resolve(new FileNameGenerator(routine.getName).value() + DOT_HTML)
            .toFile
        )
      ) { writer => htmlRoutinePage.write(routine, writer) }
    })

    logger.info(s"Completed summary in ${ progressListener.startedGraphingDetails() / SECONDS_IN_MS } seconds")
    logger.info("Writing/diagramming details")

    val sqlAnalyzer =
      new SqlAnalyzer(db.getDbmsMeta.getIdentifierQuoteString, db.getDbmsMeta.getAllKeywords, db.getTables, db.getViews)

    val tablesDir = new File(diagramDir, "tables")
    tablesDir.mkdirs()

    val tableDiagram = new TableDiagram(renderer, tablesDir)

    val mustacheTableDiagramFactory = new MustacheTableDiagramFactory(
      dotProducer,
      tableDiagram,
      outputDirectory,
      commandLineArguments.getDegreeOfSeparation
    )
    val htmlTablePage = new HtmlTablePage(mustacheCompiler, sqlAnalyzer)

    tables.forEach(table => {
      val mustacheTableDiagrams = mustacheTableDiagramFactory.generateTableDiagrams(table)
      progressListener.graphingDetailsProgressed(table)
      logger.debug(s"Writing details of ${ table.getName }")
      Using(
        new DefaultPrintWriter(
          outputDirectory.toPath
            .resolve("tables")
            .resolve(new FileNameGenerator(table.getName).value() + DOT_HTML)
            .toFile
        )
      ) { writer =>
        htmlTablePage.write(table, mustacheTableDiagrams, writer)
      }
    })

  def analyze(): SchemaspyDatabase =

    logger.info("Starting schema analysis")

    val dbmsMeta = builder.build
    val db       = new SchemaspyDatabase(dbmsMeta, database.name, database.catalog.orNull, database.schema)

    val progressListener = new Console(commandLineArguments, new Tracked())

    database.tables.foreach(table => {
      val builder        = TableBuilder(db, table)
      val schemaSpyTable = builder.build

      val importedKeys = table.keyDefinitions.flatMap {
        case v: ForeignKey => ImportForeignKeyBuilder.build(v, db.getCatalog.getName, db.getSchema.getName, None)
        case constraint: Constraint =>
          constraint.key match
            case v: ForeignKey =>
              ImportForeignKeyBuilder.build(v, db.getCatalog.getName, db.getSchema.getName, Some(constraint.symbol))
            case _ => Nil
        case _ => Nil
      }

      val tables = db.getLocals

      importedKeys.foreach(key => {
        val foreignKeyConstraint = Option(schemaSpyTable.getForeignKeysMap.get(key.getFkName))
          .getOrElse {
            val fkc = new ForeignKeyConstraint(
              schemaSpyTable,
              key.getFkName,
              key.getUpdateRule,
              key.getDeleteRule
            )
            schemaSpyTable.getForeignKeysMap.put(key.getFkName, fkc)
            fkc
          }

        val childColumn = Option(schemaSpyTable.getColumn(key.getFkColumnName))
        childColumn.foreach(v => {
          foreignKeyConstraint.addChildColumn(v)
          val parentTable  = tables.get(key.getPkTableName)
          val parentColumn = Option(parentTable.getColumn(key.getPkColumnName))
          parentColumn.foreach(p => {
            foreignKeyConstraint.addParentColumn(p)
            v.addParent(p, foreignKeyConstraint)
            p.addChild(v, foreignKeyConstraint)
          })
        })
      })
    })

    generateHtmlDoc(db, outputDirectory, progressListener)

    outputProducer.generate(db, outputDirectory)

    val orderedTables = orderer.getTablesOrderedByRI(db.getTables, List.empty.asJava)

    new OrderingReport(outputDirectory, orderedTables).write()

    val wroteRelationshipDuration = progressListener.finished(db.getTables)

    logger.info(s"Wrote table details in ${ progressListener.finishedGatheringDetails() / SECONDS_IN_MS } seconds")
    logger.info(
      s"Wrote relationship details of ${ db.getTables.size } tables/views to directory '$outputDirectory' in ${ wroteRelationshipDuration / SECONDS_IN_MS } seconds."
    )
    logger.info(s"View the results by opening ${ new File(outputDirectory, INDEX_DOT_HTML) }")

    db
