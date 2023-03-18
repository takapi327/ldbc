/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.schemaspy

import java.util.regex.Pattern

import scala.jdk.CollectionConverters.*

import org.schemaspy.Config

object ConfigBuilder:

  def build(
    db: String,
    host: String,
    port: Int,
    user: String,
    exportedKeysEnabled: Option[Boolean] = None,
    templateDirectory: Option[String] = None,
    dbType: Option[String] = None,
    singleSignOn: Option[Boolean] = None,
    password: Option[String] = None,
    promptForPassword: Option[Boolean] = None,
    maxDetailedTables: Option[Int] = None,
    driverPath: Option[String] = None,
    css: Option[String] = None,
    font: Option[String] = None,
    fontSize: Option[Int] = None,
    description: Option[String] = None,
    maxDbThreads: Option[Int] = None,
    rankDirBugEnabled: Option[Boolean] = None,
    railsEnabled: Option[Boolean]   = None,
    numRowsEnabled: Option[Boolean] = None,
    viewsEnabled: Option[Boolean]   = None,
    columnExclusions: Option[String] = None,
    indirectColumnExclusions: Option[String] = None,
    tableInclusions: Option[String] = None,
    tableExclusions: Option[String] = None,
    schemas: Option[List[String]] = None,
    evaluateAll: Option[Boolean] = None,
    schemaSpec: Option[String] = None,
    dbProperties: Option[java.util.Properties] = None,
    paginationEnabled: Option[Boolean] = None
  ): Config =
    val config = Config.getInstance()

    config.setDb(db)
    config.setHost(s"$host:$port")
    config.setUser(user)

    exportedKeysEnabled      foreach config.setExportedKeysEnabled
    templateDirectory        foreach config.setTemplateDirectory
    dbType                   foreach config.setDbType
    singleSignOn             foreach config.setSingleSignOn
    password                 foreach config.setPassword
    promptForPassword        foreach config.setPromptForPasswordEnabled
    maxDetailedTables        foreach config.setMaxDetailedTabled
    driverPath               foreach config.setDriverPath
    css                      foreach config.setCss
    font                     foreach config.setFont
    fontSize                 foreach config.setFontSize
    description              foreach config.setDescription
    maxDbThreads             foreach config.setMaxDbThreads
    rankDirBugEnabled        foreach config.setRankDirBugEnabled
    railsEnabled             foreach config.setRailsEnabled
    numRowsEnabled           foreach config.setNumRowsEnabled
    viewsEnabled             foreach config.setViewsEnabled
    columnExclusions         foreach config.setColumnExclusions
    indirectColumnExclusions foreach config.setIndirectColumnExclusions
    tableInclusions          foreach config.setTableInclusions
    tableExclusions          foreach config.setTableExclusions
    schemas.map(_.asJava)    foreach config.setSchemas
    evaluateAll              foreach config.setEvaluateAllEnabled
    schemaSpec               foreach config.setSchemaSpec
    dbProperties             foreach config.setDbProperties
    paginationEnabled        foreach config.setPaginationEnabled

    config

  def build(
    database: Database,
    user:     String,
  ): Config =
    this.build(
      db     = database.databaseName,
      host   = database.host,
      port   = database.port,
      user   = user,
      dbType = Some(database.databaseType),
    )
