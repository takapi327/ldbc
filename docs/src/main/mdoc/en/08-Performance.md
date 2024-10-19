{%
laika.title = Performance
laika.metadata.language = en
%}

# Performance

## Compile-time Overhead

Compilation time for table definitions increases with the number of columns

@:image(../img/compile_create.png) {}

Compile time for query construction increases with the number of columns to select

@:image(../img/compile_create_query.png) {}

## Runtime Overhead

Since ldbc uses Tuple internally, it is much slower than pure class definition.

@:image(../img/runtime_create.png) {}

ldbc is much slower than the others with table definitions.

@:image(../img/runtime_create_query.png) {}

## Query execution Overhead

Throughput of select query execution decreases as the number of records to retrieve increases.

@:image(../img/select_throughput.png) {}

Throughput of insert query execution decreases as the number of records to insert increases.

※ Not accurate because the query performed is not an exact match.

@:image(../img/insert_throughput.png) {}
