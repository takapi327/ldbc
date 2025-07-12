{%
laika.title = "Q: Is there a way to stream query results asynchronously?"
laika.metadata.language = en
%}

# Q: Is there a way to stream query results asynchronously?

## A: Streaming API is supported.

Streaming processing can be done using the `stream` method. This returns an Fs2 `Stream`.

```scala 3
val stream: fs2.Stream[DBIO, String] = sql"SELECT * FROM `table`".query[String].stream
```

In MySQL, the behavior of streaming changes significantly depending on the `UseCursorFetch` setting:

- **UseCursorFetch=true**: True streaming processing using server-side cursors
- **UseCursorFetch=false**: Limited streaming processing (with memory constraints)

For handling large datasets, we recommend setting `UseCursorFetch=true`.

## Reference
- [Selecting Data - Efficient Processing of Large Data with Streaming](/en/tutorial/Selecting-Data.md#efficient-processing-of-large-data-with-streaming)
