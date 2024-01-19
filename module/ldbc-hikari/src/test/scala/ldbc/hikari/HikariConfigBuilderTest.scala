/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.hikari

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

import org.specs2.mutable.Specification

object HikariConfigBuilderTest extends Specification:

  private val hikariConfig = HikariConfigBuilder.default.build()

  "Testing the HikariConfigBuilder" should {

    "The value of the specified key can be retrieved from the conf file" in {
      hikariConfig.getCatalog == "ldbc"
    }

    "The value of the specified key can be retrieved from the conf file" in {
      hikariConfig.getJdbcUrl == "jdbc:mysql://127.0.0.1:3306/ldbc"
    }

    "The connection_timeout setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getConnectionTimeout == Duration(30, TimeUnit.SECONDS).toMillis
    }

    "The idle_timeout setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getIdleTimeout == Duration(10, TimeUnit.MINUTES).toMillis
    }

    "The leak_detection_threshold setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getLeakDetectionThreshold == Duration.Zero.toMillis
    }

    "The maximum_pool_size setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getMaximumPoolSize == 32
    }

    "The max_lifetime setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getMaxLifetime == Duration(30, TimeUnit.MINUTES).toMillis
    }

    "The minimum_idle setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getMinimumIdle == 10
    }

    "The pool_name setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getPoolName == "ldbc-pool"
    }

    "The validation_timeout setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getValidationTimeout == Duration(5, TimeUnit.SECONDS).toMillis
    }

    "The connection_init_sql setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getConnectionInitSql == "select 1"
    }

    "The connection_test_query setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getConnectionTestQuery == "select 1"
    }

    "The connection_test_query setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getDataSourceClassName == "com.mysql.cj.jdbc.Driver"
    }

    "The datasource_jndi setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getDataSourceJNDI == ""
    }

    "The initialization_fail_timeout setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getInitializationFailTimeout == Duration(1, TimeUnit.MILLISECONDS).toMillis
    }

    "The jdbc_url setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getJdbcUrl == "jdbc:mysql://127.0.0.1:3306/ldbc"
    }

    "The schema setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getSchema == "ldbc"
    }

    "The username setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getUsername == "ldbc"
    }

    "The password setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getPassword == "mysql"
    }

    "The transaction_isolation setting in HikariConfig matches the setting in the conf file" in {
      hikariConfig.getTransactionIsolation == "TRANSACTION_NONE"
    }
  }

object HikariConfigBuilderFailureTest extends Specification, HikariConfigBuilder:
  override protected val path: String = "ldbc.hikari.failure"

  "Testing the HikariConfigBuilderFailureTest" should {
    "IllegalArgumentException exception is raised when transaction_isolation is set to a value other than expected" in {
      getTransactionIsolation must throwAn[IllegalArgumentException]
    }
  }
