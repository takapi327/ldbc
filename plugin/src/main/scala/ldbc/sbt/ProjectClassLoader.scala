/** This file is part of the Lepus Framework. For the full copyright and license information, please view the LICENSE
  * file that was distributed with this source code.
  */

package ldbc.sbt

import java.net.{ URL, URLClassLoader }

class ProjectClassLoader(
  urls:   Array[URL],
  parent: ClassLoader
) extends URLClassLoader(urls, parent)
