package ldbc.schema.interpreter

/** A type function to pull a type parameter it has from a type with one type parameter. */
type Extract[T] = T match
  case Option[t] => Extract[t]
  case Array[t]  => Extract[t]
  case List[t]   => Extract[t]
  case Seq[t]    => Extract[t]
  case Set[t]    => Extract[t]
  case _         => T

/** A type function that derives its type from the type parameters that Option has. */
type ExtractOption[T] = T match
  case Option[t] => Extract[t]
  case _         => T
