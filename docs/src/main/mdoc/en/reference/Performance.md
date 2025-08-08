{%
laika.title = Performance
laika.metadata.language = en
%}

# Performance

Currently supported Java versions are 11, 17, and 21+. We compared the performance of ldbc and jdbc for each version.

| Operation | 11 | 17 | 21+ |
|-----------|:--:|:--:|:---:|
| Reading   | 🔺 | 🔺 |  ✅  |
| Writing   | ✅  | ✅  |  ✅  |

- 🔺: Room for performance improvement
- ✅: Stable performance

As the performance results show, JDK 21+, the latest version, demonstrates the highest performance. Particularly, ldbc's prepareStatement and statement maintain high throughput and remain stable even under high load. When using ldbc, we recommend using JDK 21 or higher.

## Query Execution Overhead

### Writing

The benchmark measured operations per second (ops/s) for different operations. The following graphs show the performance of ldbc and jdbc for each JDK version.

**JDK 11**

The performance of ldbc and jdbc is nearly equivalent, with ldbc showing slight advantages in specific operations.

@:image(/img/connector/Insert.svg) {
alt = "Select Benchmark (JDK 11)"
}

**JDK 17**

Compared to JDK 11, we can see that ldbc's performance has improved. In particular, response times for complex queries have been reduced.

While jdbc has also improved in performance, ldbc consistently shows higher performance.

@:image(/img/connector/Insert17.svg) {
alt = "Select Benchmark (JDK 17)"
}

**JDK 21**

The performance of ldbc has further improved, with many results surpassing jdbc.

The advantages of ldbc are particularly noticeable in operations handling large amounts of data.

@:image(/img/connector/Insert21.svg) {
alt = "Select Benchmark (JDK 21)"
}

From the benchmark results, ldbc shows improvement in performance as JDK versions increase and consistently demonstrates higher performance compared to jdbc. Especially when using JDK 21, ldbc delivers excellent performance for large-scale data operations.

Considering these results, using ldbc is likely to dispel performance concerns. In particular, using the latest JDK will allow you to maximize the benefits of ldbc.

### Reading

We compared the performance of ldbc and jdbc based on benchmark results using JDK 21. The following graphs show operations per second (ops/s) for different operations.

- ldbc: prepareStatement - blue line
- ldbc: statement - orange line
- jdbc: prepareStatement - green line
- jdbc: statement - red line

It shows the highest throughput among all versions. Both `ldbc: prepareStatement` and `jdbc: statement` demonstrate excellent performance, indicating that the latest optimizations are effective.

@:image(/img/connector/Select21.svg) {
alt = "Select Benchmark (JDK 21)"
}

#### Performance Comparison

**1. ldbc: prepareStatement vs jdbc: prepareStatement**

ldbc's prepareStatement consistently shows higher performance compared to jdbc's prepareStatement. Especially under high load, ldbc maintains stable operation counts.

**2. ldbc: statement vs jdbc: statement**

ldbc's statement also demonstrates superior performance compared to jdbc's statement. ldbc operates more efficiently than jdbc, particularly for complex queries and processing large volumes of data.

From the benchmark results, ldbc shows superior performance compared to jdbc in the following aspects:

- High throughput: ldbc has higher operations per second than jdbc and remains stable even under high load.
- Efficient resource usage: ldbc operates efficiently even with complex queries and large data processing, optimizing resource usage.

Based on these results, using ldbc can eliminate performance concerns. ldbc is an excellent choice, especially in high-load environments or scenarios requiring large-scale data processing.

#### Other Versions

Here are the benchmark results for other versions:

**JDK 11**

In this version, throughput tends to be lower compared to other versions. In particular, the performance of `ldbc: prepareStatement` is inferior compared to other versions.

@:image(/img/connector/Select.svg) {
alt = "Select Benchmark (JDK 11)"
}

**JDK 17**

Compared to JDK 11, overall throughput has improved. The performance of `jdbc: prepareStatement` has particularly improved, showing stable results.

@:image(/img/connector/Select17.svg) {
alt = "Select Benchmark (JDK 17)"
}
