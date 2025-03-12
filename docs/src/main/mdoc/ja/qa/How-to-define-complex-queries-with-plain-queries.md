{%
  laika.title = "Q: プレーンクエリで複雑なクエリを定義する方法は？"
  laika.metadata.language = ja
%}

# Q: プレーンクエリで複雑なクエリを定義する方法は？

### A: IN句の構築方法は `in` 関数を使用することで定義できます。  
例えば、`in` 関数を用いると、次のようにSQL文にリストから値を埋め込むIN句を定義できます。

```scala
// サンプル: IN句の生成
val ids = NonEmptyList.of(1, 2, 3)
val inClause = in(sql"user.id", ids) 
// 生成されたSQLは "(user.id IN (?, ?, ?))" となる
```

### A: 複数のSQL条件をANDで結合する場合は `and` 関数を使用します。  
以下の例では、複数の条件をANDで連結して、1つのWHERE句を構築しています。

```scala
// サンプル: AND条件の生成
val cond1: SQL = sql"user.age > ?"       // 例: 年齢フィルター
val cond2: SQL = sql"user.status = ?"      // 例: ステータスフィルター
val andClause = and(NonEmptyList.of(cond1, cond2))
// 生成されたSQLは "((user.age > ?) AND (user.status = ?))" となる
```

### A: 複数の条件をORで結合する場合は `or` 関数を使用します。  
以下の例では、複数の条件をORで連結して、柔軟なWHERE句を生成します。

```scala
// サンプル: OR条件の生成
val condA: SQL = sql"user.country = ?"
val condB: SQL = sql"user.region = ?"
val orClause = or(NonEmptyList.of(condA, condB))
// 生成されたSQLは "((user.country = ?) OR (user.region = ?))" となる
```

### A: WHERE句を動的に組み立てる場合は、`whereAnd` や `whereOr` 関数が有用です。  
これらを使うことで、条件が存在する場合のみWHERE句を自動生成できます。

```scala
// サンプル: 動的WHERE句の生成
val conditions: NonEmptyList[SQL] = NonEmptyList.of(sql"user.age > ?", sql"user.status = ?")
val whereClause = whereAnd(conditions)
// 生成されたSQLは "WHERE (user.age > ?) AND (user.status = ?)" となる
```

### A: 複雑なクエリで、複数のカラムや条件を連結する場合は `comma` や `parentheses` 関数が役立ちます。  
これらの関数を使うことで、リスト形式のSQL要素を適切に区切り、グループ化できます。

```scala
// サンプル: カラムの連結とグルーピング
val colList = comma(NonEmptyList.of(sql"user.id", sql"user.name", sql"user.email"))
val grouped = parentheses(colList)
// 生成されたSQLは "(user.id, user.name, user.email)" となる
```

## 参考資料
- [パラメータ化されたクエリ](/ja/tutorial/Parameterized-Queries.md)
