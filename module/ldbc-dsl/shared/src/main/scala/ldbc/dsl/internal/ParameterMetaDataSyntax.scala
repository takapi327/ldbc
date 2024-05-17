/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.internal

import cats.effect.Sync

import ldbc.sql.ParameterMetaData

trait ParameterMetaDataSyntax:

  object ParameterMetaData

  implicit class ParameterMetaDataF(parameterMetaDataObject: ParameterMetaData.type):

    def apply[F[_]: Sync](parameterMetaData: java.sql.ParameterMetaData): ParameterMetaData[F] =
      new ParameterMetaData[F]:

        override def getParameterCount(): F[Int] = Sync[F].blocking(parameterMetaData.getParameterCount)

        override def isNullable(param: Int): F[Int] =
          Sync[F].blocking(parameterMetaData.isNullable(param))

        override def isSigned(param: Int): F[Boolean] = Sync[F].blocking(parameterMetaData.isSigned(param))

        override def getPrecision(param: Int): F[Int] = Sync[F].blocking(parameterMetaData.getPrecision(param))

        override def getScale(param: Int): F[Int] = Sync[F].blocking(parameterMetaData.getScale(param))

        override def getParameterType(param: Int): F[Int] =
          Sync[F].blocking(parameterMetaData.getParameterType(param))

        override def getParameterTypeName(param: Int): F[String] =
          Sync[F].blocking(parameterMetaData.getParameterTypeName(param))

        override def getParameterClassName(param: Int): F[String] =
          Sync[F].blocking(parameterMetaData.getParameterClassName(param))

        override def getParameterMode(param: Int): F[Int] =
          Sync[F].blocking(parameterMetaData.getParameterMode(param))
