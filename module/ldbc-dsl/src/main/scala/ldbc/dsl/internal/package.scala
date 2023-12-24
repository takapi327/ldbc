/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

trait internalSyntax extends internal.ConnectionSyntax, internal.ResultSetMetaDataSyntax, internal.ParameterMetaDataSyntax
package object internal extends internalSyntax
