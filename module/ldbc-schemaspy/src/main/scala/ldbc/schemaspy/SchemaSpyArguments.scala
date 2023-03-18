/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy

import java.io.File

import org.schemaspy.Arguments
import org.schemaspy.output.diagram.graphviz.GraphvizConfig

class SchemaSpyArguments(
  nohtml:                Boolean = false,
  noImplied:             Boolean = false,
  databaseType:          String  = "ora",
  databaseName:          String,
  schemaMeta:            String,
  sso:                   Boolean = false,
  user:                  String,
  schema:                String,
  catalog:               String,
  outputDirectory:       File,
  port:                  Int,
  _useVizJS:             Boolean,
  degreeOfSeparation:    Int     = 2,
  noDbObjectPaging:      Boolean = false,
  dbObjectPageLength:    Int     = 50,
  dbObjectLengthChange:  Boolean = false,
  noTablePaging:         Boolean = false,
  tablePageLength:       Int     = 10,
  tableLengthChange:     Boolean = false,
  noIndexPaging:         Boolean = false,
  indexPageLength:       Int     = 10,
  indexLengthChange:     Boolean = false,
  noCheckPaging:         Boolean = false,
  checkPageLength:       Int     = 10,
  checkLengthChange:     Boolean = false,
  noRoutinePaging:       Boolean = false,
  routinePageLength:     Int     = 50,
  routineLengthChange:   Boolean = false,
  noFkPaging:            Boolean = false,
  fkPageLength:          Int     = 50,
  fkLengthChange:        Boolean = false,
  noColumnPaging:        Boolean = false,
  columnPageLength:      Int     = 50,
  columnLengthChange:    Boolean = false,
  noAnomaliesPaging:     Boolean = false,
  anomaliesPageLength:   Int     = 10,
  anomaliesLengthChange: Boolean = false
) extends Arguments:

  override def isHelpRequired: Boolean = false

  override def isDbHelpRequired: Boolean = false

  override def isPrintLicense: Boolean = false

  override def isDebug: Boolean = false

  override def isHtmlDisabled: Boolean = nohtml

  override def isHtmlEnabled: Boolean = !nohtml

  override def withImpliedRelationships(): Boolean = !noImplied

  override def getDatabaseType: String = databaseType

  override def getOutputDirectory: File = outputDirectory

  override def getSchema: String = schema

  override def isSingleSignOn: Boolean = sso

  override def getUser: String = user

  override def getCatalog: String = catalog

  override def getDatabaseName: String = databaseName

  override def getSchemaMeta: String = schemaMeta

  override def getPort: Integer = port

  override def getGraphVizConfig: GraphvizConfig = new SchemaSpyGraphvizConfig()

  override def useVizJS(): Boolean = _useVizJS

  override def getDegreeOfSeparation: Int = degreeOfSeparation

  override def isNoDbObjectPaging: Boolean = noDbObjectPaging

  override def getDbObjectPageLength: Int = dbObjectPageLength

  override def isDbObjectLengthChange: Boolean = dbObjectLengthChange

  override def isNoTablePaging: Boolean = noTablePaging

  override def getTablePageLength: Int = tablePageLength

  override def isTableLengthChange: Boolean = tableLengthChange

  override def isNoIndexPaging: Boolean = noIndexPaging

  override def getIndexPageLength: Int = indexPageLength

  override def isIndexLengthChange: Boolean = indexLengthChange

  override def isNoCheckPaging: Boolean = noCheckPaging

  override def getCheckPageLength: Int = checkPageLength

  override def isCheckLengthChange: Boolean = checkLengthChange

  override def isNoRoutinePaging: Boolean = noRoutinePaging

  override def getRoutinePageLength: Int = routinePageLength

  override def isRoutineLengthChange: Boolean = routineLengthChange

  override def isNoFkPaging: Boolean = noFkPaging

  override def getFkPageLength: Int = fkPageLength

  override def isFkLengthChange: Boolean = fkLengthChange

  override def isNoColumnPaging: Boolean = noColumnPaging

  override def getColumnPageLength: Int = columnPageLength

  override def isColumnLengthChange: Boolean = columnLengthChange

  override def isNoAnomaliesPaging: Boolean = noAnomaliesPaging

  override def getAnomaliesPageLength: Int = anomaliesPageLength

  override def isAnomaliesLengthChange: Boolean = anomaliesLengthChange
