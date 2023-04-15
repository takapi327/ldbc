/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy

import java.io.{ File, IOException, FileFilter }
import java.nio.file.{ Files, Path, StandardOpenOption }
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.util.ArrayList
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import scala.util.Using

import org.apache.commons.io.filefilter.FileFilterUtils

import org.schemaspy.{ DbAnalyzer, SimpleRuntimeDotConfig, LayoutFolder, SchemaAnalyzer }
import org.schemaspy.model.Database as SchemaspyDatabase
import org.schemaspy.model.Table as SchemaspyTable
import org.schemaspy.model.{ TableIndex, ProgressListener, Tracked, Console }
import org.schemaspy.util.{ Markdown, ManifestUtils, DataTableConfig, DefaultPrintWriter, Jar }
import org.schemaspy.util.naming.FileNameGenerator
import org.schemaspy.view.*
import org.schemaspy.output.dot.schemaspy.{ DefaultFontConfig, DotFormatter, OrphanGraph }
import org.schemaspy.output.diagram.{ SummaryDiagram, TableDiagram }
import org.schemaspy.output.diagram.graphviz.GraphvizDot
import org.schemaspy.output.html.mustache.diagrams.{ MustacheSummaryDiagramFactory, OrphanDiagram, MustacheTableDiagramFactory }
import org.schemaspy.analyzer.ImpliedConstraintsFinder
import org.schemaspy.cli.CommandLineArguments

import ldbc.core.*
import ldbc.schemaspy.result.Status
import ldbc.schemaspy.builder.{ DbmsMetaBuilder, TableBuilder }

class SchemaSpyGenerator(database: Database):

  private val DOT_HTML = ".html"
  private val INDEX_DOT_HTML = "index.html"

  private val layoutFolder = new LayoutFolder(this.getClass.getClassLoader)
  private val builder = new DbmsMetaBuilder(database)
  private val commandLineArguments = new CommandLineArguments
  private val progressListener = new Console(commandLineArguments, new Tracked())

  private def writeInfo(key: String, value: String, infoFile: Path): Unit  =
    try {
      Files.write(
        infoFile,
        (key + "=" + "\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND,
        StandardOpenOption.WRITE
      )
    } catch
      case e: IOException =>
        println(s"Failed to write `$key=$value`, to $infoFile")
        e.printStackTrace()

  private def notHtml(): FileFilter =
    val notHtmlFilter = FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(DOT_HTML))
    FileFilterUtils.and(notHtmlFilter)

  private def generateHtmlDoc(
    db: SchemaspyDatabase,
    outputDirectory: File,
    progressListener: ProgressListener
  ): Unit =
    val tables = db.getTables

    Markdown.registryPage(new ArrayList[SchemaspyTable](tables))

    new Jar(layoutFolder.url(), outputDirectory, notHtml()).copyJarResourceToPath()

    val renderer = new GraphvizDot(commandLineArguments.getGraphVizConfig)

    val htmlInfoFile = outputDirectory.toPath.resolve("info-html.txt")
    Files.deleteIfExists(htmlInfoFile)

    writeInfo("date", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ")), htmlInfoFile)
    writeInfo("os", System.getProperty("os.name") + " " + System.getProperty("os.version"), htmlInfoFile)
    writeInfo("schemaspy-version", ManifestUtils.getImplementationVersion, htmlInfoFile)
    writeInfo("schemaspy-build", ManifestUtils.getImplementationBuild, htmlInfoFile)
    writeInfo("renderer", renderer.identifier(), htmlInfoFile)
    progressListener.graphingSummaryProgressed()

    val impliedConstraintsFinder = new ImpliedConstraintsFinder()

    val impliedConstraints = impliedConstraintsFinder.find(tables)

    val runtimeDotConfig = new SimpleRuntimeDotConfig(
      new DefaultFontConfig(commandLineArguments.getDotConfig),
      commandLineArguments.getDotConfig,
      "svg".equalsIgnoreCase(renderer.format()),
      true
    )

    val dotProducer = new DotFormatter(runtimeDotConfig)
    val diagramDir = new File(outputDirectory, "diagrams")
    diagramDir.mkdirs()
    val summaryDir = new File(outputDirectory, "summary")
    summaryDir.mkdirs()
    val summaryDiagram = new SummaryDiagram(renderer, summaryDir)

    val mustacheSummaryDiagramFactory = new MustacheSummaryDiagramFactory(dotProducer, summaryDiagram, true, !impliedConstraints.isEmpty, outputDirectory)
    val results = mustacheSummaryDiagramFactory.generateSummaryDiagrams(db, tables, progressListener)

    val dataTableConfig = new DataTableConfig(commandLineArguments)
    val mustacheCompiler = new MustacheCompiler(
      db.getName,
      db.getSchema.getName,
      commandLineArguments.getHtmlConfig,
      true,
      dataTableConfig
    )

    val htmlRelationshipsPage = new HtmlRelationshipsPage(mustacheCompiler, true, !impliedConstraints.isEmpty)
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
            .toFile)
      ) { writer => htmlRoutinePage.write(routine, writer) }
    })

    val sqlAnalyzer = new SqlAnalyzer(db.getDbmsMeta.getIdentifierQuoteString, db.getDbmsMeta.getAllKeywords, db.getTables, db.getViews)

    val tablesDir = new File(diagramDir, "tables")
    tablesDir.mkdirs()

    val tableDiagram = new TableDiagram(renderer, tablesDir)

    val mustacheTableDiagramFactory = new MustacheTableDiagramFactory(dotProducer, tableDiagram, outputDirectory, commandLineArguments.getDegreeOfSeparation)
    val htmlTablePage = new HtmlTablePage(mustacheCompiler, sqlAnalyzer)

    tables.forEach(table => {
      val mustacheTableDiagrams = mustacheTableDiagramFactory.generateTableDiagrams(table)
      progressListener.graphingDetailsProgressed(table)
      Using(
        new DefaultPrintWriter(
          outputDirectory.toPath
            .resolve("tables")
            .resolve(new FileNameGenerator(table.getName).value() + DOT_HTML).toFile
        )
      ) { writer =>
        htmlTablePage.write(table, mustacheTableDiagrams, writer)
      }
    })

  def generateTo(outputDirectory: File): Unit =

    val dbmsMeta = builder.build
    val db = new SchemaspyDatabase(dbmsMeta, database.name, database.catalog.orNull, database.schema)

    database.tables.map(table => {
      val builder = TableBuilder(db, table)
      builder.build
    })

    generateHtmlDoc(db, outputDirectory, progressListener)

object SchemaSpyGenerator:

  def apply(database: Database): SchemaSpyGenerator =
    new SchemaSpyGenerator(database)
