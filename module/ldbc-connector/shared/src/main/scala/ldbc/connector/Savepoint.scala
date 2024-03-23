package ldbc.connector

/**
 * The representation of a savepoint, which is a point within the current transaction that can be referenced from the Connection.rollback method.
 * When a transaction is rolled back to a savepoint all changes made after that savepoint are undone.
 * 
 * Savepoints can be either named or unnamed. Unnamed savepoints are identified by an ID generated by the underlying data source.
 */
trait Savepoint:

  /**
   * Retrieves the name of the savepoint that this Savepoint object represents.
   * 
   * @return
   *   the name of this savepoint
   */
  def getSavepointName: String
