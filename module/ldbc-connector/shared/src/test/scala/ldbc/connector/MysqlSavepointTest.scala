/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import ldbc.connector.exception.SQLFeatureNotSupportedException

class MysqlSavepointTest extends FTestPlatform:

  test("getSavepointName should return the name provided in the constructor") {
    val savePointName = "test_savepoint"
    val savepoint = MysqlSavepoint(savePointName)
    
    assertEquals(savepoint.getSavepointName(), savePointName)
  }
  
  test("getSavepointId should throw SQLFeatureNotSupportedException") {
    val savepoint = MysqlSavepoint("test_savepoint")
    
    val exception = intercept[SQLFeatureNotSupportedException] {
      savepoint.getSavepointId()
    }
    
    assert(exception.getMessage.contains("Only named savepoints are supported"))
  }
  
  test("different savepoints with same name should be distinct objects") {
    val name = "same_name_savepoint"
    val savepoint1 = MysqlSavepoint(name)
    val savepoint2 = MysqlSavepoint(name)
    
    assertNotEquals(savepoint1, savepoint2)
  }
  
  test("different savepoints with different names should have different names") {
    val savepoint1 = MysqlSavepoint("first_savepoint")
    val savepoint2 = MysqlSavepoint("second_savepoint")
    
    assertNotEquals(savepoint1.getSavepointName(), savepoint2.getSavepointName())
  }
