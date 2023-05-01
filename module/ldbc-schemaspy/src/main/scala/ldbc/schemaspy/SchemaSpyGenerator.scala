/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy

import java.io.{ File, IOException }
import java.sql.SQLException

import org.schemaspy.{ LayoutFolder, SchemaAnalyzer, TableOrderer }
import org.schemaspy.model.{ EmptySchemaException, InvalidConfigurationException }
import org.schemaspy.view.*
import org.schemaspy.input.dbms.MissingParameterException
import org.schemaspy.input.dbms.service.{ DatabaseServiceFactory, SqlService }
import org.schemaspy.input.dbms.exceptions.ConnectionFailure
import org.schemaspy.output.xml.dom.XmlProducerUsingDOM
import org.schemaspy.cli.{
  CommandLineArgumentParser,
  CommandLineArguments,
  ConfigFileArgumentParser,
  DefaultProviderFactory
}

import ldbc.core.*
import ldbc.schemaspy.result.Status
import ldbc.schemaspy.builder.DbmsMetaBuilder

class SchemaSpyGenerator(analyzer: SchemaAnalyzer | LdbcSchemaAnalyzer):

  def generate(): Status =
    try
      Option(
        analyzer match
          case v: SchemaAnalyzer     => v.analyze()
          case v: LdbcSchemaAnalyzer => v.analyze()
      )
        .fold(Status.Failure)(_ => Status.Success)
    catch
      case connectionFailure: ConnectionFailure =>
        connectionFailure.printStackTrace()
        Status.ConnectionFailure
      case noData: EmptySchemaException =>
        noData.printStackTrace()
        Status.EmptySchema
      case badConfig: InvalidConfigurationException =>
        badConfig.printStackTrace()
        Status.InvalidConfig
      case miss: MissingParameterException =>
        miss.printStackTrace()
        Status.MissingParameter
      case e: SQLException =>
        e.printStackTrace()
        Status.Failure
      case e: IOException =>
        e.printStackTrace()
        Status.Failure

object SchemaSpyGenerator:

  private val layoutFolder             = new LayoutFolder(this.getClass.getClassLoader)
  private val commandLineArguments     = new CommandLineArguments
  private val outputProducer           = new XmlProducerUsingDOM
  private val orderer                  = new TableOrderer()
  private val factory                  = new DefaultProviderFactory
  private val configFileArgumentParser = new ConfigFileArgumentParser

  private def buildDatabaseArguments(database: Database): Seq[String] =
    Seq(
      "-t",
      database.databaseType.toString.toLowerCase(),
      "-db",
      database.name,
      "-s",
      database.schema,
      "-host",
      database.host,
      "-port",
      database.port.toString
    )

  private def buildArguments(arguments: Seq[String]): CommandLineArguments =
    val iDefaultProvider =
      factory.create(configFileArgumentParser.parseConfigFileArgumentValue(arguments: _*).orElse(null))
    val parser = CommandLineArgumentParser(commandLineArguments, iDefaultProvider)

    parser.parse(arguments: _*)

  def default(
    database:        Database,
    outputDirectory: File
  ): SchemaSpyGenerator =

    val builder = new DbmsMetaBuilder(database)

    val analyzer = new LdbcSchemaAnalyzer(
      database,
      layoutFolder,
      builder,
      commandLineArguments,
      outputProducer,
      orderer,
      outputDirectory
    )

    new SchemaSpyGenerator(analyzer)

  def connect(
    database:        Database,
    user:            String,
    password:        Option[String],
    outputDirectory: File
  ): SchemaSpyGenerator =
    val sqlService = new SqlService()

    val databaseArguments: Seq[String] = buildDatabaseArguments(database)
    val userArguments: Seq[String] = Seq(
      "-u",
      user,
      "-o",
      outputDirectory.getPath
    ) ++ password.fold(Seq.empty)(v => Seq("-p", v))

    Class.forName(database.databaseType.driver)

    val arguments: Seq[String] = databaseArguments ++ userArguments

    val analyzer = new SchemaAnalyzer(
      sqlService,
      new DatabaseServiceFactory(sqlService),
      buildArguments(arguments),
      outputProducer,
      layoutFolder
    )

    new SchemaSpyGenerator(analyzer)
