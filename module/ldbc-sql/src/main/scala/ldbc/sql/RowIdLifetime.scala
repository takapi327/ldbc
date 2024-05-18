/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * Enumeration for RowId life-time values.
 */
enum RowIdLifetime:

  /**
   Indicates that this data source does not support the ROWID type.
   */
  case ROWID_UNSUPPORTED

  /**
   * Indicates that the lifetime of a RowId from this data source is indeterminate;
   * but not one of ROWID_VALID_TRANSACTION, ROWID_VALID_SESSION, or,
   * ROWID_VALID_FOREVER.
   */
  case ROWID_VALID_OTHER

  /**
   * Indicates that the lifetime of a RowId from this data source is at least the
   * containing session.
   */
  case ROWID_VALID_SESSION

  /**
   * Indicates that the lifetime of a RowId from this data source is at least the
   * containing transaction.
   */
  case ROWID_VALID_TRANSACTION

  /**
   * Indicates that the lifetime of a RowId from this data source is, effectively,
   * unlimited.
   */
  case ROWID_VALID_FOREVER
