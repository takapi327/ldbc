/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.*

import scodec.bits.ByteVector

import munit.*

import cats.effect.IO

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.RdsUtilities
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest

import java.nio.charset.StandardCharsets

import ldbc.dsl.*

import ldbc.connector.*
import ldbc.authentication.plugin.*

import ldbc.Connector

class AwsSdkIamAuthenticationPlugin(
                                     host: String,
                                     port: Int,
                                     user: String,
                                     region: Region
                                   ) extends AuthenticationPlugin[IO]:

  override def name: PluginName = MYSQL_CLEAR_PASSWORD

  override def requiresConfidentiality: Boolean = true

  override def hashPassword(password: String, scramble: Array[Byte]): IO[ByteVector] =
    IO.blocking {
      val credentialsProvider = DefaultCredentialsProvider.builder().build()

      val rdsUtilities = RdsUtilities.builder()
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build()

      val request = GenerateAuthenticationTokenRequest.builder()
        .hostname(host)
        .port(port)
        .username(user)
        .build()

      val authToken = rdsUtilities.generateAuthenticationToken(request)

      ByteVector(authToken.getBytes(StandardCharsets.UTF_8)) :+ 0.toByte
    }

class AwsTest extends CatsEffectSuite:
  private val datasource = MySQLDataSource
    .build[IO]("127.0.0.1", 3308, "iam_user")
    .setSSL(SSL.Trusted)
    .setPlugins(new AwsSdkIamAuthenticationPlugin(
      "aurora-cluster-stg.cluster-cr8aoa2mozlh.ap-northeast-1.rds.amazonaws.com",
      3306,
      "iam_user",
      Region.AP_NORTHEAST_1)
    )

  def connector: Connector[IO] = Connector.fromDataSource(datasource)

  test("When the table is created, the number of records is 0.") {
    assertIO(
      sql"SELECT 1".query[Int].unsafe.readOnly(connector),
      1
    )
  }
