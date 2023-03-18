/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.schemaspy

import org.schemaspy.output.diagram.graphviz.GraphvizConfig

class SchemaSpyGraphvizConfig(
  graphvizDir: Option[String] = None,
  renderer:    Option[String] = None,
  lowQuality:  Boolean        = false,
  imageFormat: String         = "png"
) extends GraphvizConfig:

  override def getGraphvizDir: String = graphvizDir.orNull

  override def getRenderer: String = renderer.orNull

  override def isLowQuality: Boolean = lowQuality

  override def getImageFormat: String = imageFormat
