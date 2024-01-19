/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.sbt

import java.net.{ URL, URLClassLoader }

class ProjectClassLoader(
  urls:   Array[URL],
  parent: ClassLoader
) extends URLClassLoader(urls, parent)
