/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.internal

import cats.effect.Sync

import ldbc.core.JdbcType
import ldbc.sql.ParameterMetaData
import ParameterMetaData.*

trait ParameterMetaDataSyntax:

  implicit class ParameterMetaDataF(parameterMetaDataObject: ParameterMetaData.type):

    def apply[F[_]: Sync](parameterMetaData: java.sql.ParameterMetaData): ParameterMetaData[F] =
      new ParameterMetaData[F]:

        override def getParameterCount(): F[Int] = Sync[F].blocking(parameterMetaData.getParameterCount)

        override def isNullable(param: Int): F[Option[Parameter]] =
          Sync[F].blocking(Parameter.values.find(_.code == parameterMetaData.isNullable(param)))

        override def isSigned(param: Int): F[Boolean] = Sync[F].blocking(parameterMetaData.isSigned(param))

        override def getPrecision(param: Int): F[Int] = Sync[F].blocking(parameterMetaData.getPrecision(param))

        override def getScale(param: Int): F[Int] = Sync[F].blocking(parameterMetaData.getScale(param))

        override def getParameterType(param: Int): F[JdbcType] =
          Sync[F].blocking(JdbcType.fromCode(parameterMetaData.getParameterType(param)))

        override def getParameterTypeName(param: Int): F[String] =
          Sync[F].blocking(parameterMetaData.getParameterTypeName(param))

        override def getParameterClassName(param: Int): F[String] =
          Sync[F].blocking(parameterMetaData.getParameterClassName(param))

        override def getParameterMode(param: Int): F[Option[Mode]] =
          Sync[F].blocking(Mode.values.find(_.code == parameterMetaData.getParameterMode(param)))
