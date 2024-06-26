package ldbc.schema.attribute

/**
 * Model for specifying an additional attribute AUTO_INCREMENT for DataType.
 */
private[ldbc] case class AutoInc[T <: Byte | Short | Int | Long | BigInt | Option[Byte | Short | Int | Long | BigInt]]()
  extends Attribute[T]:

  override def queryString: String = "AUTO_INCREMENT"

  override def toString: String = queryString
