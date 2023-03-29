/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy

import java.io.{ File, IOException }
import java.sql.SQLException

import org.schemaspy.{ Arguments, LayoutFolder, SchemaAnalyzer }
import org.schemaspy.model.{ EmptySchemaException, InvalidConfigurationException }
import org.schemaspy.input.dbms.service.{ DatabaseServiceFactory, SqlService }
import org.schemaspy.input.dbms.exceptions.ConnectionFailure
import org.schemaspy.output.OutputProducer
import org.schemaspy.output.xml.dom.XmlProducerUsingDOM

import ldbc.core.Database
import ldbc.schemaspy.result.Status

object SchemaSpyGenerator:

  def generate(
    database:        Database,
    outputDirectory: File,
    user:            String,
    password:        Option[String]
  ): Status =
    val sqlService:             SqlService             = new SqlService()
    val databaseServiceFactory: DatabaseServiceFactory = new DatabaseServiceFactory(sqlService)
    val outputProducer:         OutputProducer         = new XmlProducerUsingDOM()
    val layoutFolder:           LayoutFolder           = new LayoutFolder(classOf[SchemaAnalyzer].getClassLoader)

    val arguments: Arguments = new SchemaSpyArguments(
      nohtml          = false,
      noImplied       = false,
      databaseType    = database.databaseType,
      databaseName    = database.name,
      schemaMeta      = database.schemaMeta.orNull,
      sso             = false,
      user            = user,
      schema          = database.schema,
      catalog         = database.catalog.orNull,
      outputDirectory = outputDirectory,
      port            = database.port,
      _useVizJS       = false
    )

    val analyzer: SchemaAnalyzer = new SchemaAnalyzer(
      sqlService,
      databaseServiceFactory,
      arguments,
      outputProducer,
      layoutFolder
    )

    val config = ConfigBuilder.build(
      db       = database.name,
      host     = database.host,
      port     = database.port,
      user     = user,
      dbType   = Some(database.databaseType),
      password = password
    )

    try
      val result = Option(analyzer.analyze(config))
      if result.nonEmpty then Status.Success else Status.Failure
    catch
      case ex: ConnectionFailure =>
        ex.printStackTrace()
        Status.ConnectionFailure
      case ex: EmptySchemaException =>
        ex.printStackTrace()
        Status.EmptySchema
      case ex: InvalidConfigurationException =>
        ex.printStackTrace()
        Status.InvalidConfig
      case ex: SQLException =>
        ex.printStackTrace()
        Status.Failure
      case ex: IOException =>
        ex.printStackTrace()
        Status.Failure
