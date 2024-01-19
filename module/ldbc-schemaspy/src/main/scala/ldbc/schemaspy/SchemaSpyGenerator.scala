/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schemaspy

import java.io.{ File, IOException }
import java.sql.SQLException

import org.schemaspy.{ LayoutFolder, SchemaAnalyzer }
import org.schemaspy.model.{ EmptySchemaException, InvalidConfigurationException }
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

/**
 * Class for generating SchemaSpy documents.
 *
 * @param analyzer
 *   Classes for analyzing database models or actual databases.
 */
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
      database.port.getOrElse(3306).toString
    )

  private def buildArguments(arguments: Seq[String]): CommandLineArguments =
    val iDefaultProvider =
      factory.create(configFileArgumentParser.parseConfigFileArgumentValue(arguments*).orElse(null))
    val parser = CommandLineArgumentParser(commandLineArguments, iDefaultProvider)

    parser.parse(arguments*)

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
