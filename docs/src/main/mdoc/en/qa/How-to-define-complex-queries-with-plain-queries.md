{%
  laika.title = "Q: How to define complex queries with plain queries?"
  laika.metadata.language = en
%}

# Q: How to define complex queries with plain queries?

### A: You can define IN clauses by using the `in` function.  
For example, you can define an IN clause that embeds values from a list into an SQL statement as follows:

```scala
// Sample: Generating an IN clause
val ids = NonEmptyList.of(1, 2, 3)
val inClause = in(sql"user.id", ids) 
// The generated SQL will be "(user.id IN (?, ?, ?))"
```

### A: Use the `and` function to combine multiple SQL conditions with AND.  
In the example below, multiple conditions are concatenated with AND to build a single WHERE clause.

```scala
// Sample: Generating AND conditions
val cond1: SQL = sql"user.age > ?"       // Example: age filter
val cond2: SQL = sql"user.status = ?"      // Example: status filter
val andClause = and(NonEmptyList.of(cond1, cond2))
// The generated SQL will be "((user.age > ?) AND (user.status = ?))"
```

### A: Use the `or` function to combine multiple conditions with OR.  
In the example below, multiple conditions are concatenated with OR to generate a flexible WHERE clause.

```scala
// Sample: Generating OR conditions
val condA: SQL = sql"user.country = ?"
val condB: SQL = sql"user.region = ?"
val orClause = or(NonEmptyList.of(condA, condB))
// The generated SQL will be "((user.country = ?) OR (user.region = ?))"
```

### A: The `whereAnd` and `whereOr` functions are useful when dynamically building WHERE clauses.  
Using these, you can automatically generate WHERE clauses only when conditions exist.

```scala
// Sample: Generating dynamic WHERE clauses
val conditions: NonEmptyList[SQL] = NonEmptyList.of(sql"user.age > ?", sql"user.status = ?")
val whereClause = whereAnd(conditions)
// The generated SQL will be "WHERE (user.age > ?) AND (user.status = ?)"
```

### A: The `comma` and `parentheses` functions are helpful in complex queries for concatenating multiple columns or conditions.  
These functions allow you to properly separate and group list-format SQL elements.

```scala
// Sample: Concatenating columns and grouping
val colList = comma(NonEmptyList.of(sql"user.id", sql"user.name", sql"user.email"))
val grouped = parentheses(colList)
// The generated SQL will be "(user.id, user.name, user.email)"
```

## References
- [Parameterized Queries](/en/tutorial/Parameterized-Queries.md)
