{%
  laika.title = Error Handling
  laika.metadata.language = en
%}

# Error Handling

This chapter examines a set of combinators for building programs that trap and handle exceptions.

## About Exceptions

Whether an operation succeeds or not depends on unpredictable factors such as the health of the network, the current contents of the table, and the state of the lock. Therefore, we must decide whether to compute everything in a logical OR like `EitherT[Executor, Throwable, A]` or to allow exception propagation until it is explicitly caught. In other words, when an ldbc action (which is converted to a target monad) is executed, an exception may be raised.

There are three main types of exceptions that are likely to occur

1. various types of IOExceptions can occur with all types of I/O, and these exceptions tend to be unrecoverable
2. database exceptions usually occur in common situations such as key violations, as a general SQLException that identifies a specific error in vendor-specific SQLState. Error codes must be communicated as lore or discovered by experimentation; there are XOPEN and SQL:2003 standards, but no vendor seems to adhere to these specifications. Some of these errors are recoverable, some are not.
3. ldbc raises InvariantViolation for invalid type mappings, unknown JDBC constants returned by the driver, observed NULL values, and other violations of immutable conditions assumed by ldbc. These exceptions indicate programmer error or driver incompatibility and are generally unrecoverable.

## Monad errors and derived combinators

All ldbc monads are derived from the `MonadError[?[_], Throwable]` and provide an Async instance that extends it. This means that Executor and others will have the following primitive operations

- raiseError: raise an exception (convert Throwable' to `M[A]`)
- handleErrorWith: handle an exception (convert `M[A]` to `M[B]`)
- attempt: catch exception (convert `M[A]` to `M[Either[Throwable, A]]`)

In other words, any ldbc program can catch an exception simply by adding `attempt`.

```scala
val program = Executor.pure[IO, Int](1)

program.attempt
// Executor[IO, Either[Throwable, Int]]
```

From the `attempt` and `raiseError` combinators, many other operations can be derived, as described in the Cats documentation.
